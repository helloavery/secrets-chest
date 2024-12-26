package com.averygrimes.secretschest.model;

import lombok.Data;

@Data
public class SecretsChestData {

    private byte[] encryptedKey;
    private byte[] encryptedData;
}