package com.averygrimes.secretschest.model;

import lombok.Data;

@Data
public class SecretsChestData {

    private String keyId;
    private byte[] encryptedData;
    private String hexEncodedEncryptedData;
}