package com.averygrimes.secretschest.config;

import com.averygrimes.secretschest.exceptions.SecretsChestException;
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
        return new ResponseEntity<>(secretsChestException, status);
    }
}