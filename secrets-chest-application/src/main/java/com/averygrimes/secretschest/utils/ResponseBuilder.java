package com.averygrimes.secretschest.utils;

import com.averygrimes.secretschest.model.SecretsChestResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

public class ResponseBuilder {

    public static ResponseEntity<Object> buildAndReturnResponse(SecretsChestResponse secretsChestResponse){
        secretsChestResponse.setSuccessful(true);
        return new ResponseEntity<>(secretsChestResponse, HttpStatus.OK);
    }
}