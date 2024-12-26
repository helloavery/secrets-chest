package com.averygrimes.secretschest.service;

import com.averygrimes.secretschest.model.SecretsChestData;
import software.amazon.awssdk.core.SdkBytes;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */


public interface CryptoService {

    SecretsChestData generateDataKeyAndEncryptData(byte[] dataToUpload);

    byte[] decryptData(byte[] encryptedData, ByteBuffer encryptedKey);

    byte[] encryptDataWithoutGeneratingDataKey(byte[] dataToUpload, SdkBytes encryptedKey);

}