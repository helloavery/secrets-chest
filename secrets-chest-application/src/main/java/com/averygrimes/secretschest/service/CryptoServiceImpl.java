package com.averygrimes.secretschest.service;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.secretschest.exceptions.SecretsChestException;
import com.averygrimes.secretschest.model.SecretsChestData;
import com.averygrimes.secretschest.utils.RequestStatCollector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

@Service
@Slf4j
public class CryptoServiceImpl implements CryptoService {

    private KmsClient kmsClient;
    private RequestStatCollector requestStatCollector;

    @Autowired
    public void setKmsClient(KmsClient kmsClient) {
        this.kmsClient = kmsClient;
    }

    @Autowired
    public void setRequestStatCollector(RequestStatCollector requestStatCollector) {
        this.requestStatCollector = requestStatCollector;
    }

    @Override
    public void generateDataKeyAndEncryptData(SecretsChestData secretsChestData){
        CreateKeyResponse dataKeyResult = createDataKey(secretsChestData.getRequestId());
        String keyId = dataKeyResult.keyMetadata().keyId();

        byte[] encryptedData = encryptData(keyId, SdkBytes.fromByteArray(secretsChestData.getUnencryptedData()));
        String hexEncodedEncryptedData = Hex.encodeHexString(encryptedData);

        secretsChestData.setKeyId(keyId);
        secretsChestData.setEncryptedData(encryptedData);
        secretsChestData.setHexEncodedEncryptedData(hexEncodedEncryptedData);
    }

    @Override
    public SecretsChestData encryptDataWithoutGeneratingDataKey(String keyId, byte[] dataToUpload){
        SecretsChestData secretsChestData = new SecretsChestData();

        byte[] encryptedData =  encryptData(keyId, SdkBytes.fromByteArray(dataToUpload));
        String hexEncodedEncryptedData = Hex.encodeHexString(encryptedData);

        secretsChestData.setKeyId(keyId);
        secretsChestData.setEncryptedData(encryptedData);
        secretsChestData.setHexEncodedEncryptedData(hexEncodedEncryptedData);
        return secretsChestData;
    }

    @Override
    public byte[] decryptData(String keyId, String hexEncodedEncryptedData){
        try{
            byte[] encryptedData = Hex.decodeHex(hexEncodedEncryptedData);
            return performDecryptData(keyId, SdkBytes.fromByteArray(encryptedData));
        }catch(DecoderException e){
            log.error("Error decoding encrypted data", e);
            throw new SecretsChestException(e);
        }
    }

    private CreateKeyResponse createDataKey(String requestId){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try{
            CreateKeyRequest keyRequest = CreateKeyRequest.builder()
                    .description(requestId)
                    .keySpec(KeySpec.RSA_4096)
                    .keyUsage(KeyUsageType.ENCRYPT_DECRYPT)
                    .build();
            CreateKeyResponse createKeyResponse = kmsClient.createKey(keyRequest);
            log.info("Successfully generated key within kms and returned create key response");
            requestStatCollector.recordSuccess(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(), stopWatch);
            return createKeyResponse;
        } catch (Exception e) {
            log.error("Error while creating key within kms", e);
            SecretsChestException secretsChestException = new SecretsChestException(500, "Error while creating key within kms", e);
            requestStatCollector.recordError(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(), secretsChestException, stopWatch);
            throw secretsChestException;
        }
    }

    private byte[] encryptData(String keyId, SdkBytes dataToEncrypt) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            log.info("Encrypting retrieved secrets");
            EncryptRequest encryptRequest = EncryptRequest.builder()
                    .keyId(keyId)
                    .encryptionAlgorithm(EncryptionAlgorithmSpec.RSAES_OAEP_SHA_256)
                    .plaintext(dataToEncrypt)
                    .build();
            EncryptResponse encryptResponse = kmsClient.encrypt(encryptRequest);
            requestStatCollector.recordSuccess(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(), stopWatch);
            return encryptResponse.ciphertextBlob().asByteArray();
        } catch (Exception e) {
            log.error("Error encrypting data", e);
            SecretsChestException secretsChestException = new SecretsChestException(500, "Error encrypting data", e);
            requestStatCollector.recordError(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(), secretsChestException, stopWatch);
            throw secretsChestException;
        }
    }

    private byte[] performDecryptData(String keyId, SdkBytes encryptedData){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try{
            log.info("decrypting secrets");
            DecryptRequest decryptRequest = DecryptRequest.builder()
                    .ciphertextBlob(encryptedData)
                    .keyId(keyId)
                    .encryptionAlgorithm(EncryptionAlgorithmSpec.RSAES_OAEP_SHA_256)
                    .build();
            DecryptResponse decryptResponse = kmsClient.decrypt(decryptRequest);
            requestStatCollector.recordSuccess(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(), stopWatch);
            return decryptResponse.plaintext().asByteArray();
        }
        catch(Exception e){
            log.error("Error decrypting secrets", e);
            SecretsChestException secretsChestException = new SecretsChestException(500, "Error decrypting secrets", e);
            requestStatCollector.recordError(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(), secretsChestException, stopWatch);
            throw secretsChestException;
        }
    }
}