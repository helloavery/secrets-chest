package com.averygrimes.credentials;

import com.averygrimes.credentials.pojo.RetrieveDataResponse;

import java.util.concurrent.*;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/14/20
 * https://github.com/helloavery
 */

class CredentialsUtils {

    public <T> CompletableFuture<T> timeoutRetrieveInvocationResponse(CompletableFuture<T> completableFuture,
                                                                                long timeOutDuration, TimeUnit timeUnit){
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        Callable<RetrieveDataResponse> callable = () -> {
            completableFuture.completeExceptionally(new TimeoutException());
            return null;
        };
        scheduledThreadPoolExecutor.schedule(callable, timeOutDuration, timeUnit);
        scheduledThreadPoolExecutor.shutdown();
        return completableFuture;
    }
}
