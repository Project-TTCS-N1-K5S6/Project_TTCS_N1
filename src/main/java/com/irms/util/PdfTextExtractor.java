package com.irms.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * ==============================================================================
 * CÔNG CỤ TRÍCH XUẤT VĂN BẢN TỪ TỆP PDF (PdfTextExtractor)
 * ==============================================================================
 * Kiến trúc 2 tầng (Dual-Engine Architecture):
 * 1. Tầng ưu tiên: Sử dụng Apache PDFBox (qua Reflection nếu thư viện có sẵn trong classpath).
 * 2. Tầng độc lập: Bộ giải nén và phân tích cú pháp luồng PDF thuần Java (Pure Java Engine):
 *    - Phân tích indirect objects và streams.
 *    - Giải nén bộ lọc /FlateDecode bằng java.util.zip.Inflater.
 *    - Bóc tách các toán tử văn bản chuẩn PDF: BT...ET, Tj, TJ, ', ".
 *    - Giải mã chuỗi UTF-16BE (BOM 0xFEFF hoặc hex <0048...>), Octal escapes (\ooo), ASCII/UTF-8.
 *    - Trích xuất metadata bổ trợ (/Title, /Author).
 * ==============================================================================
 */
public class PdfTextExtractor {

    private static final Logger LOGGER = Logger.getLogger(PdfTextExtractor.class.getName());

    private static final Pattern STREAM_PATTERN = Pattern.compile("stream[\\r\\n]+([\\s\\S]*?)[\\r\\n]+endstream", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEXT_BLOCK_PATTERN = Pattern.compile("BT([\\s\\S]*?)ET");
    private static final Pattern TJ_PATTERN = Pattern.compile("\\((.*?)\\)\\s*Tj", Pattern.DOTALL);
    private static final Pattern TJ_ARRAY_PATTERN = Pattern.compile("\\[([\\s\\S]*?)\\]\\s*TJ");
    private static final Pattern HEX_STRING_PATTERN = Pattern.compile("<([0-9a-fA-F\\s]+)>\\s*Tj");
    private static final Pattern AUTHOR_METADATA_PATTERN = Pattern.compile("/Author\\s*\\((.*?)\\)");
    private static final Pattern TITLE_METADATA_PATTERN = Pattern.compile("/Title\\s*\\((.*?)\\)");

    /**
     * Trích xuất toàn bộ văn bản từ mảng byte PDF
     */
    public static String extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return "";
        }

        // 1. Thử dùng Apache PDFBox trước nếu có trên Classpath
        String pdfboxText = tryExtractWithPdfBox(pdfBytes);
        if (pdfboxText != null && !pdfboxText.trim().isEmpty()) {
            return pdfboxText.trim();
        }

        // 2. Dự phòng: Dùng bộ phân tích cú pháp PDF tích hợp sẵn thuần Java
        return extractWithBuiltinParser(pdfBytes).trim();
    }

    /**
     * Thử bóc tách qua Apache PDFBox bằng reflection để tránh lỗi compile/runtime nếu chưa có jar
     */
    private static String tryExtractWithPdfBox(byte[] pdfBytes) {
        try {
            Class<?> pdDocClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Method loadMethod = pdDocClass.getMethod("load", byte[].class);
            Object doc = loadMethod.invoke(null, pdfBytes);

            Class<?> stripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
            Object stripper = stripperClass.getDeclaredConstructor().newInstance();
            Method getTextMethod = stripperClass.getMethod("getText", pdDocClass);
            String text = (String) getTextMethod.invoke(stripper, doc);

            Method closeMethod = pdDocClass.getMethod("close");
            closeMethod.invoke(doc);

            if (text != null && !text.trim().isEmpty()) {
                return text;
            }
        } catch (Throwable ignored) {
            // PDFBox không khả dụng hoặc phát sinh lỗi -> tiếp tục với builtin engine
        }
        return null;
    }

    /**
     * Bộ trích xuất thuần Java phân tích nội dung PDF binary
     */
    public static String extractWithBuiltinParser(byte[] pdfBytes) {
        StringBuilder sb = new StringBuilder();

        // 1. Quét tìm và giải mã các luồng stream trong file PDF
        extractStreams(pdfBytes, sb);

        // 2. Nếu luồng stream chưa cho nhiều kết quả, quét tìm văn bản không nén trong toàn bộ file
        if (sb.length() < 20) {
            String rawAscii = new String(pdfBytes, StandardCharsets.ISO_8859_1);
            extractTextFromPdfContent(rawAscii, sb);

            // Bổ sung Author/Title nếu có
            Matcher authorMatcher = AUTHOR_METADATA_PATTERN.matcher(rawAscii);
            if (authorMatcher.find()) {
                sb.append("\n").append(decodePdfString(authorMatcher.group(1)));
            }
            Matcher titleMatcher = TITLE_METADATA_PATTERN.matcher(rawAscii);
            if (titleMatcher.find()) {
                sb.append("\n").append(decodePdfString(titleMatcher.group(1)));
            }
        }

        return cleanExtractedText(sb.toString());
    }

    /**
     * Quét và giải nén các luồng /FlateDecode trong tệp PDF
     */
    private static void extractStreams(byte[] data, StringBuilder out) {
        String dataStr = new String(data, StandardCharsets.ISO_8859_1);
        int streamIndex = 0;

        while ((streamIndex = dataStr.indexOf("stream", streamIndex)) != -1) {
            // Tìm đầu luồng dữ liệu (bỏ qua ký tự xuống dòng \r, \n)
            int start = streamIndex + 6;
            if (start < data.length && data[start] == '\r') start++;
            if (start < data.length && data[start] == '\n') start++;

            int end = dataStr.indexOf("endstream", start);
            if (end == -1) break;

            // Kiểm tra xem stream này có thuộc FlateDecode không
            int objStart = Math.max(0, streamIndex - 200);
            String dictHeader = dataStr.substring(objStart, streamIndex);
            boolean isFlate = dictHeader.contains("/FlateDecode") || dictHeader.contains("/Fl");

            if (start < end) {
                byte[] streamBytes = new byte[end - start];
                System.arraycopy(data, start, streamBytes, 0, end - start);

                if (isFlate) {
                    byte[] decompressed = decompressFlate(streamBytes);
                    if (decompressed != null && decompressed.length > 0) {
                        String content = new String(decompressed, StandardCharsets.ISO_8859_1);
                        extractTextFromPdfContent(content, out);
                    }
                } else {
                    String content = new String(streamBytes, StandardCharsets.ISO_8859_1);
                    extractTextFromPdfContent(content, out);
                }
            }

            streamIndex = end + 9;
        }
    }

    /**
     * Giải nén Flate (zlib deflate) an toàn
     */
    private static byte[] decompressFlate(byte[] compressed) {
        // Thử zlib thông thường
        byte[] result = tryInflate(compressed, false);
        if (result == null) {
            // Thử raw deflate (nowrap = true)
            result = tryInflate(compressed, true);
        }
        return result;
    }

    private static byte[] tryInflate(byte[] compressed, boolean nowrap) {
        Inflater inflater = new Inflater(nowrap);
        inflater.setInput(compressed);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break;
                }
                baos.write(buffer, 0, count);
            }
            return baos.toByteArray();
        } catch (DataFormatException ignored) {
            return null;
        } finally {
            inflater.end();
        }
    }

    /**
     * Bóc tách các toán tử văn bản từ nội dung content stream (BT ... ET)
     */
    private static void extractTextFromPdfContent(String content, StringBuilder out) {
        if (content == null || content.isEmpty()) return;

        Matcher btMatcher = TEXT_BLOCK_PATTERN.matcher(content);
        boolean foundBt = false;
        while (btMatcher.find()) {
            foundBt = true;
            String textBlock = btMatcher.group(1);
            parseTextOperators(textBlock, out);
            out.append("\n");
        }

        if (!foundBt) {
            // Nếu không có BT..ET rõ ràng, parse trực tiếp toán tử Tj và TJ
            parseTextOperators(content, out);
        }
    }

    private static void parseTextOperators(String block, StringBuilder out) {
        // 1. Phân tích toán tử Tj: (chuỗi văn bản) Tj
        Matcher tjMatcher = TJ_PATTERN.matcher(block);
        while (tjMatcher.find()) {
            String decoded = decodePdfString(tjMatcher.group(1));
            if (!decoded.trim().isEmpty()) {
                out.append(decoded).append(" ");
            }
        }

        // 2. Phân tích toán tử hex: <48656C6C6F> Tj
        Matcher hexMatcher = HEX_STRING_PATTERN.matcher(block);
        while (hexMatcher.find()) {
            String decoded = decodeHexString(hexMatcher.group(1));
            if (!decoded.trim().isEmpty()) {
                out.append(decoded).append(" ");
            }
        }

        // 3. Phân tích toán tử mảng TJ: [(chuỗi 1) 120 (chuỗi 2)] TJ
        Matcher tjArrMatcher = TJ_ARRAY_PATTERN.matcher(block);
        while (tjArrMatcher.find()) {
            String arrayContent = tjArrMatcher.group(1);
            Matcher innerString = Pattern.compile("\\((.*?)\\)|<([0-9a-fA-F\\s]+)>").matcher(arrayContent);
            while (innerString.find()) {
                if (innerString.group(1) != null) {
                    String decoded = decodePdfString(innerString.group(1));
                    out.append(decoded);
                } else if (innerString.group(2) != null) {
                    String decoded = decodeHexString(innerString.group(2));
                    out.append(decoded);
                }
            }
            out.append(" ");
        }
    }

    /**
     * Giải mã chuỗi PDF dạng (string) xử lý ký tự escape và UTF-16
     */
    private static String decodePdfString(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = raw.length();
        for (int i = 0; i < len; i++) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = raw.charAt(i + 1);
                if (next == 'n') { sb.append('\n'); i++; }
                else if (next == 'r') { sb.append('\r'); i++; }
                else if (next == 't') { sb.append('\t'); i++; }
                else if (next == 'b') { sb.append('\b'); i++; }
                else if (next == 'f') { sb.append('\f'); i++; }
                else if (next == '(' || next == ')' || next == '\\') { sb.append(next); i++; }
                else if (Character.isDigit(next) && i + 2 < len && Character.isDigit(raw.charAt(i + 2))) {
                    // Octal escape \ddd
                    int end = Math.min(len, i + 4);
                    String oct = raw.substring(i + 1, end);
                    try {
                        int code = Integer.parseInt(oct, 8);
                        sb.append((char) code);
                        i += oct.length();
                    } catch (NumberFormatException e) {
                        sb.append(next);
                        i++;
                    }
                } else {
                    sb.append(next);
                    i++;
                }
            } else {
                sb.append(c);
            }
        }

        String res = sb.toString();
        // Kiểm tra UTF-16BE BOM
        if (res.startsWith("\uFEFF") || (res.length() >= 2 && res.charAt(0) == '\u00FE' && res.charAt(1) == '\u00FF')) {
            try {
                byte[] bytes = res.getBytes(StandardCharsets.ISO_8859_1);
                return new String(bytes, StandardCharsets.UTF_16BE);
            } catch (Exception ignored) {}
        }
        return res;
    }

    /**
     * Giải mã chuỗi Hex <00480065...>
     */
    private static String decodeHexString(String hex) {
        if (hex == null) return "";
        String clean = hex.replaceAll("\\s+", "");
        if (clean.length() % 2 != 0) clean += "0";

        byte[] bytes = new byte[clean.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            try {
                bytes[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
            } catch (NumberFormatException e) {
                return "";
            }
        }

        if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        } else if (bytes.length >= 2 && bytes[0] == 0 && bytes[1] != 0) {
            return new String(bytes, StandardCharsets.UTF_16BE);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Dọn dẹp khoảng trắng dư thừa trong chuỗi văn bản trích xuất
     */
    private static String cleanExtractedText(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\t ]+", " ")
                .replaceAll("(?m)^ +| +$", "")
                .replaceAll("(\\r?\\n){3,}", "\n\n")
                .trim();
    }
}
