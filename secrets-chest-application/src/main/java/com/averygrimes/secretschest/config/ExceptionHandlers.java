package com.averygrimes.secretschest.config;

import com.averygrimes.secretschest.exceptions.SecretsChestException;
import com.averygrimes.secretschest.model.SecretsChestResponse;
import io.micrometer.common.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler(SecretsChestException.class)
    @ResponseBody
    public ResponseEntity<Object> handleAuthServiceException(final SecretsChestException secretsChestException){
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        try{
            status = HttpStatus.valueOf(secretsChestException.getStatusCode());
        } catch (Exception ignored) {
        }
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        secretsChestResponse.setStatusCode(status.value());
        secretsChestResponse.setErrors(secretsChestException.getErrors());
        if(StringUtils.isNotBlank(secretsChestException.getMessage())){
            secretsChestResponse.getErrors().add(secretsChestException.getMessage());
        }
        secretsChestResponse.setSuccessful(false);
        return new ResponseEntity<>(secretsChestResponse, status);
    }
}