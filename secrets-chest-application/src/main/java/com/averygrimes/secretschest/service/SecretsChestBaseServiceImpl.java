package com.averygrimes.secretschest.service;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.secretschest.cache.CacheBase;
import com.averygrimes.secretschest.exceptions.SecretsChestException;
import com.averygrimes.secretschest.external.AWSService;
import com.averygrimes.secretschest.model.SecretsChestData;
import com.averygrimes.secretschest.model.SecretsChestResponse;
import com.averygrimes.secretschest.utils.UUIDUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Service
@Slf4j
public class SecretsChestBaseServiceImpl implements SecretsChestBaseService {

    private CryptoService cryptoService;
    private CacheBase cacheService;
    private Executor executor;
    private final Lock lock = new ReentrantLock(true);
    private AWSService awsService;

    @Value("${aws.s3.keyBucket}")
    private String awsS3KeyBucket;
    @Value("${aws.s3.dataBucket}")
    private String awsS3DataBucket;

    @Autowired
    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Autowired
    @Qualifier("s3UploadAsyncThreadPoolTaskExecutor")
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Autowired
    public void setCacheService(CacheBase cacheService) {
        this.cacheService = cacheService;
    }

    @Autowired
    public void setAwsService(AWSService awsService) {
        this.awsService = awsService;
    }


    @Override
    public SecretsChestResponse uploadAsset(byte[] dataToUpload, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            String bucketObjectReference = UUIDUtils.generateRandomId();
            secretsChestResponse.setSecretReference(bucketObjectReference);
            boolean isUploadSuccessful = performEncryptedUpload(dataToUpload, bucketObjectReference, requestId);
            secretsChestResponse.setSuccessful(isUploadSuccessful);
        } catch (Exception e) {
            log.warn("Thread has been interrupted while acquiring lock");
        }
        return secretsChestResponse;
    }

    @Override
    public SecretsChestResponse uploadPlainTextAsset(String dataToUpload, boolean isEncryptionDisabled, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            String bucketObjectReference = UUIDUtils.generateRandomId();
            if(isEncryptionDisabled){
                awsService.sendUploadBucketObjectRequest(awsS3DataBucket, bucketObjectReference, dataToUpload, requestId);
                secretsChestResponse.setSuccessful(true);
            }else {
                boolean isUploadSuccessful = performEncryptedUpload(dataToUpload.getBytes(StandardCharsets.UTF_8), bucketObjectReference, requestId);
                secretsChestResponse.setSuccessful(isUploadSuccessful);
            }
            secretsChestResponse.setSecretReference(bucketObjectReference);
        }
        catch (Exception e) {
            log.error("Error uploading new secrets for bucket {} for requestId {}", "dataToUpload", requestId, e);
            throw new SecretsChestException("Error uploading secrets data for request id: " + requestId);
        }
        return secretsChestResponse;
    }

    @Override
    public SecretsChestResponse updateAsset(String secretsReference, byte[] dataToUpload, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            String keyId = getKeyIdFromCacheOrBucket(secretsReference, requestId);
            SecretsChestData encryptedKeyAndData = cryptoService.encryptDataWithoutGeneratingDataKey(keyId, dataToUpload);
            awsService.sendUploadBucketObjectRequest(awsS3DataBucket, secretsReference, encryptedKeyAndData.getHexEncodedEncryptedData(), requestId);
            secretsChestResponse.setSecretReference(secretsReference);
            secretsChestResponse.setSuccessful(true);
        }
        catch (Exception e) {
            log.error("Error updating secrets for bucket {} for requestId {}", "dataToUpload", requestId, e);
            throw new SecretsChestException("Error updating secrets data for request id: " + requestId);
        }
        return secretsChestResponse;
    }

    @Override
    public SecretsChestResponse retrieveAsset(String secretReference, boolean isEncryptionDisabled, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            if(lock.tryLock(3500, TimeUnit.MILLISECONDS)){
                try{
                    String s3ObjectOutput = (String) awsService.sendRetrieveBucketObjectResponse(awsS3DataBucket, secretReference, requestId, true);
                    if(!isEncryptionDisabled){
                        String keyId = getKeyIdFromCacheOrBucket(secretReference, requestId);
                        byte[] decryptedData = cryptoService.decryptData(keyId, s3ObjectOutput);
                        secretsChestResponse.setData(decryptedData);
                        secretsChestResponse.setPlainTextData(new String(decryptedData));
                    }else{
                        secretsChestResponse.setPlainTextData(s3ObjectOutput);
                    }
                    secretsChestResponse.setSecretReference(secretReference);
                    secretsChestResponse.setSuccessful(true);
                }catch(Exception e){
                    log.error("Error fetching secrets for object reference {}", secretReference, e);
                    throw new SecretsChestException("Error fetching secrets for secret" + secretReference + " requestId: " + requestId);
                }finally{
                    lock.unlock();
                }
            }
        } catch(InterruptedException e) {
            log.warn("Thread has been interrupted while acquiring lock");
        }
        return secretsChestResponse;
    }

    private boolean performEncryptedUpload(byte[] dataToUpload, String bucketObjectReference, String requestId){
        boolean isOperationSuccessful = true;
        try {
            if (lock.tryLock(1500, TimeUnit.MILLISECONDS)) {
                try {
                    SecretsChestData encryptionKeyAndData = cryptoService.generateDataKeyAndEncryptData(dataToUpload, requestId);
                    List<CompletableFuture<SecretsChestResponse>> completableFutures = new ArrayList<>();
                    sendBucketUploadTask(completableFutures, awsS3KeyBucket, encryptionKeyAndData.getKeyId(), bucketObjectReference, requestId);
                    sendBucketUploadTask(completableFutures, awsS3DataBucket, encryptionKeyAndData.getHexEncodedEncryptedData(), bucketObjectReference, requestId);
                    CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
                    for(CompletableFuture<SecretsChestResponse> result : completableFutures){
                        if(!result.get().isSuccessful()){
                            isOperationSuccessful = false;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error uploading new secrets for bucket {} for requestId {}", "dataToUpload", requestId, e);
                    throw new SecretsChestException("Error uploading secrets data for request id: " + requestId);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            log.warn("Thread has been interrupted while acquiring lock");
        }
        return isOperationSuccessful;
    }

    private void sendBucketUploadTask(List<CompletableFuture<SecretsChestResponse>> completableFutures, String bucket, String dataToUpload, String bucketObjectReference, String requestId){
        CompletableFuture<SecretsChestResponse> completableFuture = CompletableFuture.supplyAsync(() -> {
            SecretsChestResponse secretsChestResponse = null;
            try{
                secretsChestResponse = new SecretsChestResponse();
                awsService.sendUploadBucketObjectRequest(bucket, bucketObjectReference, dataToUpload, requestId);
                if(bucket.equalsIgnoreCase(awsS3KeyBucket)){
                    putItemInCache(bucketObjectReference, dataToUpload);
                }
                secretsChestResponse.setSuccessful(true);
            }
            catch(Exception e){
                log.error("Error completing upload data task", e);
            }
            return secretsChestResponse;
        }, executor).orTimeout(10000, TimeUnit.MILLISECONDS);
        completableFutures.add(completableFuture);
    }

    private void putItemInCache(String bucketObjectReference, String encryptedKey){
        try{
            cacheService.putItemInCache(bucketObjectReference, encryptedKey);
        }
        catch(Exception e){
            log.warn("Error putting encrypted key in key cache", e);
        }
    }

    private String getKeyIdFromCacheOrBucket(String secretReference, String requestId){
        try{
            String keyId;
            if(cacheService.getItemFromCache(secretReference) != null){
                keyId = (String) cacheService.getItemFromCache(secretReference);
            }else{
                keyId  = (String) awsService.sendRetrieveBucketObjectResponse(awsS3KeyBucket, secretReference, requestId, true);
            }
            return keyId;
        } catch (Exception e) {
            log.error("Error decoding hex encrypted key for request id {}", requestId, e);
            throw new SecretsChestException("Error decoding hex encrypted key for request id: " + requestId);
        }
    }
}