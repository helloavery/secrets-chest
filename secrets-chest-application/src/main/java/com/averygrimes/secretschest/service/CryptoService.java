package com.averygrimes.secretschest.service;

import com.averygrimes.secretschest.model.SecretsChestData;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */


public interface CryptoService {

    void generateDataKeyAndEncryptData(SecretsChestData secretsChestData);

    byte[] decryptData(String keyId, String hexEncodedEncryptedData);

    SecretsChestData encryptDataWithoutGeneratingDataKey(String keyId, byte[] dataToUpload);
}