package com.averygrimes.secretschest.model;

import lombok.Data;

@Data
public class SecretsChestData {

    private String keyId;
    private String dataFromRequest;
    private byte[] unencryptedData;
    private byte[] encryptedData;
    private String hexEncodedEncryptedData;
    private String groupId;
    private String appId;
    private String dataDescription;
    private String secretReference;
    private String bucketName;
    private String bucketObjectReference;
    private SecretsChestRequest.DataRequestType dataRequestType;
    private String requestId;
}