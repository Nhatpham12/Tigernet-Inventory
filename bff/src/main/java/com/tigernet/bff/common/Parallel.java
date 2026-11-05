package com.tigernet.bff.common;

import com.tigernet.bff.client.BackendException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

// gọi song song nhiều API backend bằng virtual thread; lỗi backend được ném lại nguyên vẹn
public final class Parallel {
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private Parallel() {
    }

    public static <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, EXECUTOR);
    }

    public static <T> T await(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof BackendException backendException) {
                throw backendException;
            }
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }
}
