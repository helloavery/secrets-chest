package com.averygrimes.secretschest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author Avery Grimes-Farrow
 * Created on: 4/16/20
 * https://github.com/helloavery
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class SecretsChestResponse {

    private boolean isSuccessful;
    private byte[] data;
    private String secretReference;
    private List<String> errors;
}
