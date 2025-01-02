package com.averygrimes.secretschest.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SecretsChestRequest {

    @NotNull
    @Size(min = 5)
    private String groupId;

    @NotNull
    private String appId;

    @NotNull
    private String dataDescription;

    @NotNull
    private String dataToUpload;

    @NotNull
    private DataRequestType dataRequestType;

    private String requestId;

    public enum DataRequestType {
        BYTE_ARRAY, PLAIN_TEXT
    }
}