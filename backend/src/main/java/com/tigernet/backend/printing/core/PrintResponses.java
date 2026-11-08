package com.tigernet.backend.printing.core;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Map;

/**
 * Trả biểu mẫu về client: mặc định PDF mở trực tiếp trên trình duyệt (inline),
 * ?format=html trả HTML để xem nhanh, ?download=true để tải file.
 */
public final class PrintResponses {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private PrintResponses() {
    }

    /**
     * Tên file tải về = tên nghiệp vụ + thời gian thực hiện nghiệp vụ, VD "Phieu_nhap_kho_20261103_0930".
     * Nghiệp vụ chỉ có ngày thì bỏ phần giờ; không có thời gian thì lấy lúc in.
     */
    public static String fileName(String business, TemporalAccessor time) {
        String stamp;
        if (time == null) {
            stamp = LocalDateTime.now().format(DATE_TIME);
        } else if (time.isSupported(ChronoField.HOUR_OF_DAY)) {
            stamp = DATE_TIME.format(time);
        } else {
            stamp = DATE.format(time);
        }
        return business + "_" + stamp;
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
