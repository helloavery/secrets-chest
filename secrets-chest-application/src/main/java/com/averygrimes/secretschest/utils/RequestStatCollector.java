package com.averygrimes.secretschest.utils;

import com.averygrimes.secretschest.exceptions.SecretsChestException;
import com.averygrimes.secretschest.model.SecretsChestResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.MDC;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.annotation.RequestScope;

import java.util.concurrent.TimeUnit;

@Component
@RequestScope
@Slf4j
public class RequestStatCollector {

    public void recordSuccess(String methodName, StopWatch stopWatch){
        try{
            if(stopWatch.isRunning()){
                stopWatch.stop();
            }
            MDC.put("methodName", methodName);
            MDC.put("timeTaken", stopWatch.getTotalTime(TimeUnit.MILLISECONDS) + " millis");

            log.info(MarkerFactory.getMarker("RequestStatCollector"), "RequestStatCollector");

            MDC.remove("methodName");
            MDC.remove("timeTaken");
        } catch (Exception e) {
            log.error("Error recording success within request stat collector");
        }

    }

    public void recordSuccess(String methodName, SecretsChestResponse secretsChestResponse, StopWatch stopWatch){
        try {
            if(stopWatch.isRunning()){
                stopWatch.stop();
            }
            MDC.put("methodName", methodName);
            MDC.put("timeTaken", stopWatch.getTotalTime(TimeUnit.MILLISECONDS) + " millis");
            MDC.put("status", String.valueOf(secretsChestResponse.getStatusCode()));
            MDC.put("secretsReference", secretsChestResponse.getSecretReference());

            log.info(MarkerFactory.getMarker("RequestStatCollector"), "RequestStatCollector");

            MDC.remove("methodName");
            MDC.remove("timeTaken");
            MDC.remove("status");
            MDC.remove("secretsReference");
        } catch (Exception e) {
            log.error("Error recording success within request stat collector");
        }
    }

    public void recordError(String methodName, SecretsChestException secretsChestException, StopWatch stopWatch){
        try {
            if(stopWatch.isRunning()){
                stopWatch.stop();
            }
            MDC.put("methodName", methodName);
            MDC.put("timeTaken", stopWatch.getTotalTime(TimeUnit.MILLISECONDS) + " millis");
            MDC.put("status", String.valueOf(secretsChestException.getStatusCode()));
            if(CollectionUtils.isNotEmpty(secretsChestException.getErrors())){
                MDC.put("errors", secretsChestException.getErrors().toString());
            }
            MDC.put("message", secretsChestException.getMessage());
            MDC.put("exception", secretsChestException.getException().toString());

            log.error(MarkerFactory.getMarker("RequestStatCollector"), "RequestStatCollector");

            MDC.remove("methodName");
            MDC.remove("timeTaken");
            MDC.remove("status");
            MDC.remove("errors");
            MDC.remove("message");
            MDC.remove("exception");
        } catch (Exception e) {
            log.error("Error recording error within request stat collector");
        }
    }
}