package com.matchbar.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MinimalPdfGenerator {

    private MinimalPdfGenerator() {}

    public static byte[] generate(String title, List<String> lines) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<>();

            write(out, "%PDF-1.4\n%âãÏÓ\n");

            offsets.add(out.size());
            write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            offsets.add(out.size());
            write(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            offsets.add(out.size());
            write(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
                       "/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");

            StringBuilder stream = new StringBuilder();
            stream.append("BT\n");
            stream.append("/F1 18 Tf\n");
            stream.append("72 780 Td\n");
            stream.append("(").append(escape(title)).append(") Tj\n");
            stream.append("/F1 11 Tf\n");
            stream.append("0 -30 TD\n");
            stream.append("16 TL\n");
            boolean first = true;
            for (String line : lines) {
                if (first) {
                    stream.append("(").append(escape(line)).append(") Tj\n");
                    first = false;
                } else {
                    stream.append("T*\n(").append(escape(line)).append(") Tj\n");
                }
            }
            stream.append("ET\n");
            byte[] streamBytes = stream.toString().getBytes(StandardCharsets.ISO_8859_1);

            offsets.add(out.size());
            write(out, "4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
            out.write(streamBytes);
            write(out, "\nendstream\nendobj\n");

            offsets.add(out.size());
            write(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            int xrefStart = out.size();
            StringBuilder xref = new StringBuilder();
            xref.append("xref\n0 ").append(offsets.size() + 1).append("\n");
            xref.append("0000000000 65535 f \n");
            for (int off : offsets) {
                xref.append(String.format("%010d 00000 n \n", off));
            }
            xref.append("trailer\n<< /Size ").append(offsets.size() + 1).append(" /Root 1 0 R >>\n");
            xref.append("startxref\n").append(xrefStart).append("\n%%EOF");
            write(out, xref.toString());

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de muestra", e);
        }
    }

    private static void write(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(bytes, 0, bytes.length);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
