package com.averygrimes.secretschest.exceptions;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SecretsChestException extends RuntimeException {

    private int statusCode;
    private String message;
    private Exception exception;
    private List<String> errors = new ArrayList<>();

    public SecretsChestException(String message){
        super(message);
    }

    public SecretsChestException(Throwable e){
        super(e);
    }

    public SecretsChestException(Exception e){
        super(e);
    }

    public SecretsChestException(int statusCode, Exception e){
        this.statusCode = statusCode;
        this.exception = e;
    }

    public SecretsChestException(int statusCode, List<String> errors){
        this.statusCode = statusCode;
        this.errors = errors;
    }

    public SecretsChestException(int statusCode, String message){
        this.statusCode = statusCode;
        this.message = message;
    }

    public SecretsChestException(int statusCode, String message, Exception e){
        this.statusCode = statusCode;
        this.message = message;
        this.exception = e;
    }

    public SecretsChestException(int statusCode, String message, List<String> errors){
        this.statusCode = statusCode;
        this.message = message;
        this.errors = errors;
    }

    public SecretsChestException(int statusCode, String message, Exception exception, List<String> errors){
        this.statusCode = statusCode;
        this.message = message;
        this.exception = exception;
        this.errors = errors;
    }
}