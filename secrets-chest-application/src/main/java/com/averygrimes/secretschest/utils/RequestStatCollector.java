package com.averygrimes.secretschest.utils;

import com.averygrimes.secretschest.exceptions.SecretsChestException;
import com.averygrimes.secretschest.model.SecretsChestResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.concurrent.TimeUnit;

@Component
public class RequestStatCollector {

    public void recordSuccess(String methodName, StopWatch stopWatch){
        if(stopWatch.isRunning()){
            stopWatch.stop();
        }
        MDC.put("methodName", methodName);
        MDC.put("timeTaken", String.valueOf(stopWatch.getTotalTime(TimeUnit.MILLISECONDS)));
    }

    public void recordSuccess(String methodName, SecretsChestResponse secretsChestResponse, StopWatch stopWatch){
        if(stopWatch.isRunning()){
            stopWatch.stop();
        }
        MDC.put("methodName", methodName);
        MDC.put("timeTaken", String.valueOf(stopWatch.getTotalTime(TimeUnit.MILLISECONDS)));
        MDC.put("status", String.valueOf(secretsChestResponse.getStatusCode()));
        MDC.put("secretsReference", secretsChestResponse.getSecretReference());
    }

    public void recordError(String methodName, SecretsChestException secretsChestException, StopWatch stopWatch){
        if(stopWatch.isRunning()){
            stopWatch.stop();
        }
        MDC.put("methodName", methodName);
        MDC.put("timeTaken", String.valueOf(stopWatch.getTotalTime(TimeUnit.MILLISECONDS)));
        MDC.put("status", String.valueOf(secretsChestException.getStatusCode()));
        if(CollectionUtils.isNotEmpty(secretsChestException.getErrors())){
            MDC.put("errors", secretsChestException.getErrors().toString());
        }
        MDC.put("message", secretsChestException.getMessage());
        MDC.put("exception", secretsChestException.getException().toString());
    }
}