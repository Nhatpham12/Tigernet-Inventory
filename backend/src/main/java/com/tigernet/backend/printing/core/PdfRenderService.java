package com.tigernet.backend.printing.core;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Dựng biểu mẫu: template Thymeleaf trong resources/print/*.html → HTML → PDF (openhtmltopdf).
 * Template viết CSS khổ giấy bằng @page; font Times New Roman được thay bằng Liberation Serif
 * (cùng kích thước chữ, giấy phép SIL OFL) nhúng trong resources/fonts để chạy được trên server Linux.
 */
@Slf4j
@Service
@EnableConfigurationProperties(CompanyProperties.class)
public class PdfRenderService {
    private static final String[][] FONTS = {
            {"LiberationSerif-Regular.ttf", "400", "NORMAL"},
            {"LiberationSerif-Bold.ttf", "700", "NORMAL"},
            {"LiberationSerif-Italic.ttf", "400", "ITALIC"},
            {"LiberationSerif-BoldItalic.ttf", "700", "ITALIC"},
    };

    private final TemplateEngine engine;
    private final CompanyProperties company;
    private final Map<String, File> fontFiles = new HashMap<>();
    private volatile String logoDataUri;

    public PdfRenderService(CompanyProperties company) {
        this.company = company;
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("print/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(resolver);
    }

    /** HTML đã điền dữ liệu (dùng cho ?format=html để xem nhanh/debug). */
    public String renderHtml(String template, Map<String, Object> model) {
        Context ctx = new Context();
        ctx.setVariables(model);
        ctx.setVariable("f", PrintFormatter.INSTANCE);
        ctx.setVariable("company", company);
        ctx.setVariable("logo", logoDataUri());
        return engine.process(template, ctx);
    }

    public byte[] renderPdf(String template, Map<String, Object> model) {
        String html = renderHtml(template, model);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            for (String[] font : FONTS) {
                File file = fontFile(font[0]);
                PdfRendererBuilder.FontStyle style = PdfRendererBuilder.FontStyle.valueOf(font[2]);
                int weight = Integer.parseInt(font[1]);
                // khai báo cùng 1 file dưới các tên mà biểu mẫu Word hay dùng
                builder.useFont(file, "Times New Roman", weight, style, true);
                builder.useFont(file, "Liberation Serif", weight, style, true);
                builder.useFont(file, "serif", weight, style, true);
            }
            builder.withW3cDocument(new W3CDom().fromJsoup(Jsoup.parse(html)), "/");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException | RuntimeException e) {
            log.error("Render PDF failed for template {}", template, e);
            throw new AppException(ErrorCode.PRINT_RENDER_FAILED);
        }
    }

    // openhtmltopdf cần File; copy font từ classpath ra file tạm một lần
    private synchronized File fontFile(String name) throws IOException {
        File cached = fontFiles.get(name);
        if (cached != null && cached.exists()) {
            return cached;
        }
        Path tmp = Files.createTempFile("tigernet-font-", "-" + name);
        tmp.toFile().deleteOnExit();
        try (InputStream in = new ClassPathResource("fonts/" + name).getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        fontFiles.put(name, tmp.toFile());
        return tmp.toFile();
    }

    // logo nhúng thẳng vào HTML dạng data URI; không cấu hình hoặc lỗi đọc file thì bỏ logo
    private String logoDataUri() {
        if (logoDataUri != null) {
            return logoDataUri.isEmpty() ? null : logoDataUri;
        }
        String value = "";
        if (company.logo() != null && !company.logo().isBlank()) {
            try {
                Resource res = new DefaultResourceLoader().getResource(company.logo());
                byte[] bytes = res.getInputStream().readAllBytes();
                String lower = company.logo().toLowerCase();
                String mime = lower.endsWith(".jpg") || lower.endsWith(".jpeg") ? "image/jpeg" : "image/png";
                value = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
            } catch (IOException e) {
                log.warn("Cannot read company logo {}", company.logo());
            }
        }
        logoDataUri = value;
        return value.isEmpty() ? null : value;
    }
}
