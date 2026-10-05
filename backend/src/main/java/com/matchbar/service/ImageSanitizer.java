package com.matchbar.service;

import com.matchbar.exception.ApiException;
import com.matchbar.util.FileSignatures;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Limpia las imágenes subidas antes de guardarlas:
 * <ul>
 *   <li>Comprueba el tipo real por su contenido (JPEG, PNG o WEBP).</li>
 *   <li>Elimina los metadatos (EXIF, XMP...), que en las fotos del móvil
 *       incluyen la ubicación GPS de quien la hizo.</li>
 *   <li>JPEG/PNG: se recodifican (aplicando antes la rotación EXIF para que no
 *       salgan giradas) y se reducen a {@value #MAX_DIMENSION} px de lado.</li>
 *   <li>WEBP: Java no lo decodifica, así que se retiran sus bloques EXIF/XMP.</li>
 * </ul>
 */
@Component
public class ImageSanitizer {

    /** Lado mayor máximo tras redimensionar: suficiente para verse a pantalla completa. */
    static final int MAX_DIMENSION = 2560;
    /** Por encima, decodificarla podría agotar la memoria ("bomba" de descompresión). */
    static final long MAX_PIXELS = 40_000_000L;
    private static final float JPEG_QUALITY = 0.85f;

    public record SanitizedImage(byte[] data, String contentType) {}

    public SanitizedImage sanitize(byte[] input) {
        if (FileSignatures.isJpeg(input)) {
            BufferedImage img = applyOrientation(decode(input), readJpegOrientation(input));
            return new SanitizedImage(writeJpeg(toRgb(downscale(img))), "image/jpeg");
        }
        if (FileSignatures.isPng(input)) {
            return new SanitizedImage(writePng(downscale(decode(input))), "image/png");
        }
        if (FileSignatures.isWebp(input)) {
            return new SanitizedImage(stripWebpMetadata(input), "image/webp");
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "La imagen debe ser JPG, PNG o WEBP");
    }

    // ── Decodificación / codificación ───────────────────────────────────────

    private static BufferedImage decode(byte[] input) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) throw unreadable();
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                // Comprobamos el tamaño leyendo solo la cabecera, antes de reservar memoria.
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "La imagen es demasiado grande (máximo " + MAX_PIXELS / 1_000_000 + " megapíxeles)");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException apiException) throw apiException;
            throw unreadable();
        }
    }

    private static byte[] writeJpeg(BufferedImage img) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            // Sin metadatos de origen: solo la cabecera JFIF mínima.
            writer.write(null, new IIOImage(img, null, null), param);
        } catch (IOException e) {
            throw unreadable();
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static byte[] writePng(BufferedImage img) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", out);
        } catch (IOException e) {
            throw unreadable();
        }
        return out.toByteArray();
    }

    // ── Transformaciones ────────────────────────────────────────────────────

    private static BufferedImage downscale(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int longest = Math.max(w, h);
        if (longest <= MAX_DIMENSION) return img;
        double scale = (double) MAX_DIMENSION / longest;
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        BufferedImage out = new BufferedImage(nw, nh, img.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    /** JPEG no admite transparencia: pasamos a RGB para poder escribirla. */
    private static BufferedImage toRgb(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_INT_RGB) return img;
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return out;
    }

    /**
     * Aplica la orientación EXIF (1-8) a los píxeles: al quitar los metadatos
     * se pierde esa indicación y la foto se vería tumbada o en espejo.
     */
    static BufferedImage applyOrientation(BufferedImage img, int orientation) {
        int w = img.getWidth();
        int h = img.getHeight();
        // AffineTransform(m00, m10, m01, m11, m02, m12): x' = m00·x + m01·y + m02 ; y' = m10·x + m11·y + m12
        AffineTransform tx = switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);   // espejo horizontal
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);  // 180°
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);   // espejo vertical
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);    // transpuesta
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);   // 90° horario
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);  // transversa
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);   // 90° antihorario
            default -> null;
        };
        if (tx == null) return img;
        boolean swapsSides = orientation >= 5;
        BufferedImage out = new BufferedImage(swapsSides ? h : w, swapsSides ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, tx, null);
        g.dispose();
        return out;
    }

    // ── JPEG: orientación EXIF ──────────────────────────────────────────────

    /** Lee la etiqueta Orientation (0x0112) del bloque EXIF; 1 si no existe o no se entiende. */
    static int readJpegOrientation(byte[] jpeg) {
        try {
            int pos = 2; // tras el marcador SOI (FF D8)
            while (pos + 4 <= jpeg.length) {
                if ((jpeg[pos] & 0xFF) != 0xFF) return 1;
                int marker = jpeg[pos + 1] & 0xFF;
                if (marker == 0xFF) { pos++; continue; }          // relleno
                if (marker == 0xDA || marker == 0xD9) return 1;   // empiezan los datos de imagen
                int length = ((jpeg[pos + 2] & 0xFF) << 8) | (jpeg[pos + 3] & 0xFF);
                int payload = pos + 4;
                if (marker == 0xE1 && length >= 8 && isExifHeader(jpeg, payload)) {
                    return readTiffOrientation(jpeg, payload + 6, length - 8);
                }
                pos += 2 + length;
            }
        } catch (IndexOutOfBoundsException e) {
            // EXIF corrupto: lo tratamos como sin orientación.
        }
        return 1;
    }

    private static boolean isExifHeader(byte[] b, int pos) {
        return pos + 6 <= b.length
                && new String(b, pos, 4, StandardCharsets.US_ASCII).equals("Exif") && b[pos + 4] == 0 && b[pos + 5] == 0;
    }

    private static int readTiffOrientation(byte[] b, int tiffStart, int tiffLength) {
        ByteBuffer buf = ByteBuffer.wrap(b, tiffStart, tiffLength).slice();
        buf.order(buf.get(0) == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        if (buf.getShort(2) != 42) return 1;
        int ifd = buf.getInt(4);
        int entries = buf.getShort(ifd) & 0xFFFF;
        for (int i = 0; i < entries; i++) {
            int entry = ifd + 2 + i * 12;
            if ((buf.getShort(entry) & 0xFFFF) == 0x0112) {
                int value = buf.getShort(entry + 8) & 0xFFFF;
                return value >= 1 && value <= 8 ? value : 1;
            }
        }
        return 1;
    }

    // ── WEBP: retirada de metadatos ─────────────────────────────────────────

    /**
     * Reconstruye el contenedor RIFF sin los bloques "EXIF" y "XMP " y apaga
     * sus indicadores en la cabecera VP8X.
     */
    static byte[] stripWebpMetadata(byte[] webp) {
        ByteArrayOutputStream chunks = new ByteArrayOutputStream();
        int pos = 12; // "RIFF" + tamaño + "WEBP"
        boolean hasImageData = false;
        while (pos + 8 <= webp.length) {
            String fourcc = new String(webp, pos, 4, StandardCharsets.US_ASCII);
            long size = Integer.toUnsignedLong(ByteBuffer.wrap(webp, pos + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
            long padded = size + (size & 1);
            if (pos + 8 + padded > webp.length) throw unreadable();
            if (fourcc.equals("VP8 ") || fourcc.equals("VP8L") || fourcc.equals("VP8X")) hasImageData = true;
            if (!fourcc.equals("EXIF") && !fourcc.equals("XMP ")) {
                byte[] chunk = new byte[(int) (8 + padded)];
                System.arraycopy(webp, pos, chunk, 0, chunk.length);
                if (fourcc.equals("VP8X") && size > 0) {
                    chunk[8] &= (byte) ~(0x08 | 0x04); // bits EXIF y XMP
                }
                chunks.writeBytes(chunk);
            }
            pos += (int) (8 + padded);
        }
        if (!hasImageData) throw unreadable();

        byte[] body = chunks.toByteArray();
        ByteBuffer out = ByteBuffer.allocate(12 + body.length).order(ByteOrder.LITTLE_ENDIAN);
        out.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(4 + body.length)
                .put("WEBP".getBytes(StandardCharsets.US_ASCII)).put(body);
        return out.array();
    }

    private static ApiException unreadable() {
        return new ApiException(HttpStatus.BAD_REQUEST, "No se ha podido procesar la imagen. Prueba con otra foto.");
    }
}
