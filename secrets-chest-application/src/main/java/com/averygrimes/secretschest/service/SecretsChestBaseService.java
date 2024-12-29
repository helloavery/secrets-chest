package com.averygrimes.secretschest.service;


import com.averygrimes.secretschest.model.SecretsChestResponse;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

public interface SecretsChestBaseService {

    SecretsChestResponse uploadAsset(byte[] dataToUpload, String requestId);

    SecretsChestResponse uploadPlainTextAsset(String dataToUpload, boolean isEncryptionDisabled, String requestId);

    SecretsChestResponse updateAsset(String secretsReference, byte[] dataToUpload, String requestId);

    SecretsChestResponse retrieveAsset(String secretReference, boolean isEncryptionDisabled, String requestId);
}
