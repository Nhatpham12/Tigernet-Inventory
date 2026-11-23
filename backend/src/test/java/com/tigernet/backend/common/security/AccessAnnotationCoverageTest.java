package com.tigernet.backend.common.security;

import com.tigernet.backend.user.service.PageAccessPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mọi API (kể cả GET) phải ghi rõ quyền: @RequiresPage, @RequiresRole hoặc @NoPageCheck.
 * API mới quên ghi sẽ làm test này đỏ (và AccessInterceptor từ chối khi chạy).
 */
class AccessAnnotationCoverageTest {

    @Test
    void everyHandlerDeclaresAccessRule() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> missing = new ArrayList<>();
        List<String> badPages = new ArrayList<>();
        int handlers = 0;
        for (BeanDefinition bean : scanner.findCandidateComponents("com.tigernet.backend")) {
            Class<?> type = Class.forName(bean.getBeanClassName());
            for (Method method : type.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    continue;
                }
                handlers++;
                RequiresPage pages = find(method, type, RequiresPage.class);
                boolean declared = pages != null || find(method, type, RequiresRole.class) != null
                        || find(method, type, NoPageCheck.class) != null;
                if (!declared) {
                    missing.add(type.getSimpleName() + "." + method.getName());
                }
                if (pages != null) {
                    Arrays.stream(pages.value()).filter(p -> !PageAccessPolicy.allPages().contains(p))
                            .forEach(p -> badPages.add(type.getSimpleName() + "." + method.getName() + " -> " + p));
                }
            }
        }
        assertTrue(handlers > 90, "quét được quá ít handler: " + handlers);
        assertTrue(missing.isEmpty(), "API chưa ghi quyền: " + missing);
        assertTrue(badPages.isEmpty(), "page key không có trong ma trận quyền: " + badPages);
    }

    private static <A extends java.lang.annotation.Annotation> A find(Method method, Class<?> type, Class<A> ann) {
        A onMethod = AnnotatedElementUtils.findMergedAnnotation(method, ann);
        return onMethod != null ? onMethod : AnnotatedElementUtils.findMergedAnnotation(type, ann);
    }
}
