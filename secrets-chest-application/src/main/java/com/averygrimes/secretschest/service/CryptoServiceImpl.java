package com.averygrimes.secretschest.service;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.secretschest.exceptions.SecretsChestCryptoException;
import com.averygrimes.secretschest.model.SecretsChestData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

@Service
@Slf4j
public class CryptoServiceImpl implements CryptoService {

    private KmsClient kmsClient;

    @Autowired
    public void setKmsClient(KmsClient kmsClient) {
        this.kmsClient = kmsClient;
    }

    @Override
    public void generateDataKeyAndEncryptData(SecretsChestData secretsChestData){
        try{
            CreateKeyResponse dataKeyResult = createDataKey(secretsChestData.getRequestId());
            String keyId = dataKeyResult.keyMetadata().keyId();

            byte[] encryptedData = encryptData(keyId, SdkBytes.fromByteArray(secretsChestData.getUnencryptedData()));
            String hexEncodedEncryptedData = Hex.encodeHexString(encryptedData);

            secretsChestData.setKeyId(keyId);
            secretsChestData.setEncryptedData(encryptedData);
            secretsChestData.setHexEncodedEncryptedData(hexEncodedEncryptedData);
        }
        catch(Exception e){
            log.error("Error encrypting secrets to be uploaded", e);
            throw new SecretsChestCryptoException("Error encrypting secrets to be uploaded", e);
        }
    }

    @Override
    public SecretsChestData encryptDataWithoutGeneratingDataKey(String keyId, byte[] dataToUpload){
        try{
            SecretsChestData secretsChestData = new SecretsChestData();

            byte[] encryptedData =  encryptData(keyId, SdkBytes.fromByteArray(dataToUpload));
            String hexEncodedEncryptedData = Hex.encodeHexString(encryptedData);

            secretsChestData.setKeyId(keyId);
            secretsChestData.setEncryptedData(encryptedData);
            secretsChestData.setHexEncodedEncryptedData(hexEncodedEncryptedData);
            return secretsChestData;
        }
        catch(Exception e){
            log.error("Error encrypting secrets to be uploaded", e);
            throw new SecretsChestCryptoException("Error encrypting secrets to be uploaded", e);
        }
    }

    @Override
    public byte[] decryptData(String keyId, String hexEncodedEncryptedData){
        try{
            byte[] encryptedData = Hex.decodeHex(hexEncodedEncryptedData);
            return performDecryptData(keyId, SdkBytes.fromByteArray(encryptedData));
        }
        catch(Exception e){
            log.error("Error decrypting secrets", e);
            throw new SecretsChestCryptoException("Error decrypting secrets: " + e.getMessage());
        }
    }

    private CreateKeyResponse createDataKey(String requestId){
        CreateKeyRequest keyRequest = CreateKeyRequest.builder()
                .description(requestId)
                .keySpec(KeySpec.RSA_4096)
                .keyUsage(KeyUsageType.ENCRYPT_DECRYPT)
                .build();
        return kmsClient.createKey(keyRequest);
    }

    private byte[] encryptData(String keyId, SdkBytes dataToEncrypt) {
        try {
            log.info("Encrypting retrieved secrets");
            EncryptRequest encryptRequest = EncryptRequest.builder()
                    .keyId(keyId)
                    .encryptionAlgorithm(EncryptionAlgorithmSpec.RSAES_OAEP_SHA_256)
                    .plaintext(dataToEncrypt)
                    .build();
            EncryptResponse encryptResponse = kmsClient.encrypt(encryptRequest);
            return encryptResponse.ciphertextBlob().asByteArray();
        } catch (Exception e) {
            log.error("Error encrypting retrieved secrets", e);
            throw new SecretsChestCryptoException("Error encrypted retrieved secrets", e);
        }
    }

    private byte[] performDecryptData(String keyId, SdkBytes encryptedData){
        try{
            log.info("decrypting secrets");
            DecryptRequest decryptRequest = DecryptRequest.builder()
                    .ciphertextBlob(encryptedData)
                    .keyId(keyId)
                    .encryptionAlgorithm(EncryptionAlgorithmSpec.RSAES_OAEP_SHA_256)
                    .build();
            DecryptResponse decryptResponse = kmsClient.decrypt(decryptRequest);
            return decryptResponse.plaintext().asByteArray();
        }
        catch(Exception e){
            log.error("Error decrypting secrets", e);
            throw new SecretsChestCryptoException("Error decrypting retrieved secrets: " + e.getMessage());
        }
    }
}