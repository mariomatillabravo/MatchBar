package com.matchbar.service;

import com.matchbar.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageSanitizerTest {

    private final ImageSanitizer sanitizer = new ImageSanitizer();

    @Test
    void rechazaUnFicheroQueNoEsUnaImagenAunqueSeLlameAsi() {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);

        ApiException ex = assertThrows(ApiException.class, () -> sanitizer.sanitize(html));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void elJpegResultanteNoConservaElExifConLaUbicacion() throws IOException {
        byte[] withGps = jpegWithExif(redCornerImage(40, 20), 1);
        assertTrue(contains(withGps, "GPSLatitude"), "precondición: el original lleva GPS");

        ImageSanitizer.SanitizedImage clean = sanitizer.sanitize(withGps);

        assertEquals("image/jpeg", clean.contentType());
        assertFalse(contains(clean.data(), "Exif"));
        assertFalse(contains(clean.data(), "GPSLatitude"));
    }

    @Test
    void aplicaLaRotacionExifAntesDeQuitarLosMetadatos() throws IOException {
        // Orientación 6: la foto debe girarse 90° en sentido horario para verse bien.
        byte[] rotated = jpegWithExif(redCornerImage(40, 20), 6);

        BufferedImage out = read(sanitizer.sanitize(rotated).data());

        assertEquals(20, out.getWidth());
        assertEquals(40, out.getHeight());
        // La esquina roja (arriba-izquierda) pasa a arriba-derecha.
        assertTrue(isRed(out.getRGB(15, 5)), "esquina superior derecha roja");
        assertFalse(isRed(out.getRGB(5, 5)), "esquina superior izquierda blanca");
    }

    @Test
    void reduceLasImagenesMuyGrandes() throws IOException {
        byte[] big = png(new BufferedImage(3000, 1000, BufferedImage.TYPE_INT_RGB));

        BufferedImage out = read(sanitizer.sanitize(big).data());

        assertEquals(ImageSanitizer.MAX_DIMENSION, out.getWidth());
        assertEquals(853, out.getHeight());
    }

    @Test
    void rechazaImagenesConDemasiadosPixelesSinDecodificarlas() {
        // Cabecera PNG que declara 10 000 x 10 000 px (100 MP) sin datos detrás.
        ApiException ex = assertThrows(ApiException.class, () -> sanitizer.sanitize(pngHeader(10_000, 10_000)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("demasiado grande"));
    }

    @Test
    void delWebpSeRetiranLosBloquesExifYXmp() {
        byte[] webp = webp(
                chunk("VP8X", new byte[]{0x0C, 0, 0, 0, 0, 0, 0, 0, 0, 0}), // indicadores EXIF + XMP
                chunk("VP8L", new byte[]{1, 2, 3, 4, 5}),                     // tamaño impar: lleva relleno
                chunk("EXIF", "GPSLatitude=40.41".getBytes(StandardCharsets.US_ASCII)),
                chunk("XMP ", "<x:xmpmeta/>".getBytes(StandardCharsets.US_ASCII)));

        ImageSanitizer.SanitizedImage clean = sanitizer.sanitize(webp);
        byte[] out = clean.data();

        assertEquals("image/webp", clean.contentType());
        assertFalse(contains(out, "EXIF"));
        assertFalse(contains(out, "XMP "));
        assertFalse(contains(out, "GPSLatitude"));
        assertTrue(contains(out, "VP8L"));
        assertEquals(0, out[20] & 0x0C, "indicadores EXIF/XMP apagados en VP8X");
        assertEquals(out.length - 8, ByteBuffer.wrap(out, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
    }

    @Test
    void unWebpSinDatosDeImagenSeRechaza() {
        byte[] onlyMetadata = webp(chunk("EXIF", "GPS".getBytes(StandardCharsets.US_ASCII)));

        assertThrows(ApiException.class, () -> sanitizer.sanitize(onlyMetadata));
    }

    // ── Construcción de ficheros de prueba ──────────────────────────────────

    private static BufferedImage redCornerImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.RED);
        g.fillRect(0, 0, 10, 10);
        g.dispose();
        return img;
    }

    /** JPEG con un segmento APP1/EXIF (orientación + un texto que simula el GPS). */
    private static byte[] jpegWithExif(BufferedImage img, int orientation) throws IOException {
        ByteArrayOutputStream base = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", base);
        byte[] jpeg = base.toByteArray();

        byte[] gps = "GPSLatitude=40.4168".getBytes(StandardCharsets.US_ASCII);
        ByteBuffer tiff = ByteBuffer.allocate(26 + gps.length).order(ByteOrder.BIG_ENDIAN);
        tiff.put((byte) 'M').put((byte) 'M').putShort((short) 42).putInt(8)
                .putShort((short) 1)                                        // 1 entrada en IFD0
                .putShort((short) 0x0112).putShort((short) 3).putInt(1)    // Orientation, SHORT, 1 valor
                .putShort((short) orientation).putShort((short) 0)
                .putInt(0)                                                  // sin más IFDs
                .put(gps);
        byte[] payload = concat("Exif\0\0".getBytes(StandardCharsets.US_ASCII), tiff.array());

        int app0End = 4 + (((jpeg[4] & 0xFF) << 8) | (jpeg[5] & 0xFF)); // SOI + APP0 (JFIF)
        int length = payload.length + 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, app0End);
        out.write(new byte[]{(byte) 0xFF, (byte) 0xE1, (byte) (length >> 8), (byte) length});
        out.write(payload);
        out.write(jpeg, app0End, jpeg.length - app0End);
        return out.toByteArray();
    }

    private static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static byte[] pngHeader(int width, int height) {
        ByteBuffer ihdr = ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN)
                .putInt(width).putInt(height).put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
        byte[] type = "IHDR".getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(type);
        crc.update(ihdr.array());
        ByteBuffer out = ByteBuffer.allocate(8 + 4 + 4 + 13 + 4).order(ByteOrder.BIG_ENDIAN);
        out.put(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})
                .putInt(13).put(type).put(ihdr.array()).putInt((int) crc.getValue());
        return out.array();
    }

    private static byte[] chunk(String fourcc, byte[] payload) {
        int padded = payload.length + (payload.length & 1);
        ByteBuffer b = ByteBuffer.allocate(8 + padded).order(ByteOrder.LITTLE_ENDIAN);
        b.put(fourcc.getBytes(StandardCharsets.US_ASCII)).putInt(payload.length).put(payload);
        return b.array();
    }

    private static byte[] webp(byte[]... chunks) {
        byte[] body = concat(chunks);
        ByteBuffer b = ByteBuffer.allocate(12 + body.length).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(4 + body.length)
                .put("WEBP".getBytes(StandardCharsets.US_ASCII)).put(body);
        return b.array();
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) out.writeBytes(p);
        return out.toByteArray();
    }

    private static boolean contains(byte[] data, String text) {
        byte[] needle = text.getBytes(StandardCharsets.US_ASCII);
        outer:
        for (int i = 0; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static BufferedImage read(byte[] data) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(data));
    }

    private static boolean isRed(int rgb) {
        Color c = new Color(rgb);
        return c.getRed() > 200 && c.getGreen() < 80 && c.getBlue() < 80;
    }
}
