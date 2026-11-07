package com.tigernet.backend.printing.core;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Trả biểu mẫu về client: mặc định PDF mở trực tiếp trên trình duyệt (inline),
 * ?format=html trả HTML để xem nhanh, ?download=true để tải file.
 */
public final class PrintResponses {
    private PrintResponses() {
    }

    public static ResponseEntity<byte[]> of(PdfRenderService renderer, String template, Map<String, Object> model,
                                            String fileName, String format, boolean download) {
        if ("html".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                    .body(renderer.renderHtml(template, model).getBytes(StandardCharsets.UTF_8));
        }
        byte[] pdf = renderer.renderPdf(template, model);
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(fileName + ".pdf", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf);
    }
}
