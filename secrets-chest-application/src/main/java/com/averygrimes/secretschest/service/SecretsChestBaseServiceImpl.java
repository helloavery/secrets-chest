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
import com.averygrimes.secretschest.model.SecretsChestRequest;
import com.averygrimes.secretschest.model.SecretsChestResponse;
import com.averygrimes.secretschest.utils.UUIDUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.averygrimes.secretschest.utils.SecretsChestConstants.*;


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
    public SecretsChestResponse uploadAsset(SecretsChestData secretsChestData){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            String secretReference = String.format(S3_KEY_NAMING_PATTERN, secretsChestData.getGroupId(), secretsChestData.getAppId(), UUIDUtils.generateRandomId());
            secretsChestData.setSecretReference(secretReference);
            byte[] dataToUpload;
            if(secretsChestData.getDataRequestType() == SecretsChestRequest.DataRequestType.BYTE_ARRAY){
                dataToUpload = Base64.getDecoder().decode(secretsChestData.getDataFromRequest());
            }else{
                dataToUpload = secretsChestData.getDataFromRequest().getBytes();
            }
            secretsChestData.setUnencryptedData(dataToUpload);

            boolean isUploadSuccessful = performEncryptedUpload(secretsChestData);
            secretsChestResponse.setSecretReference(secretReference);
            secretsChestResponse.setStatusCode(200);
            secretsChestResponse.setSuccessful(isUploadSuccessful);
        } catch (Exception e) {
            log.warn("Thread has been interrupted while acquiring lock");
        }
        return secretsChestResponse;
    }

    @Override
    public SecretsChestResponse updateAsset(String secretsReference, SecretsChestData secretsChestData){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            String keyId = getKeyIdFromCacheOrBucket(secretsReference, secretsChestData.getRequestId());
            SecretsChestData encryptedKeyAndData = cryptoService.encryptDataWithoutGeneratingDataKey(keyId, secretsChestData.getUnencryptedData());
            awsService.sendUploadBucketObjectRequest(awsS3DataBucket, secretsReference, encryptedKeyAndData.getHexEncodedEncryptedData(), populateBucketMetadata(secretsChestData), secretsChestData.getRequestId());
            secretsChestResponse.setSecretReference(secretsReference);
            secretsChestResponse.setStatusCode(200);
            secretsChestResponse.setSuccessful(true);
        }
        catch (Exception e) {
            log.error("Error updating secrets for bucket {} for requestId {}", "dataToUpload", secretsChestData.getRequestId(), e);
            throw new SecretsChestException("Error updating secrets data for request id: " + secretsChestData.getRequestId());
        }
        return secretsChestResponse;
    }

    @Override
    public SecretsChestResponse retrieveAsset(String groupId, String secretReference, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            if(lock.tryLock(3500, TimeUnit.MILLISECONDS)){
                try{
                    String bucketObjectReference = groupId + "/" + secretReference;
                    String s3ObjectOutput = (String) awsService.sendRetrieveBucketObjectResponse(awsS3DataBucket, bucketObjectReference, requestId, true);
                    String keyId = getKeyIdFromCacheOrBucket(bucketObjectReference, requestId);
                    byte[] decryptedData = cryptoService.decryptData(keyId, s3ObjectOutput);
                    secretsChestResponse.setData(decryptedData);
                    secretsChestResponse.setPlainTextData(new String(decryptedData));
                    secretsChestResponse.setSecretReference(secretReference);
                    secretsChestResponse.setStatusCode(200);
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

    private boolean performEncryptedUpload(SecretsChestData secretsChestData){
        boolean isOperationSuccessful = true;
        try {
            if (lock.tryLock(1500, TimeUnit.MILLISECONDS)) {
                try {
                    cryptoService.generateDataKeyAndEncryptData(secretsChestData);
                    List<CompletableFuture<SecretsChestResponse>> completableFutures = new ArrayList<>();
                    sendBucketUploadTask(completableFutures, awsS3DataBucket, secretsChestData.getHexEncodedEncryptedData(), secretsChestData);
                    sendBucketUploadTask(completableFutures, awsS3KeyBucket, secretsChestData.getKeyId(), secretsChestData);
                    String bucketObjectReference = secretsChestData.getGroupId() + "/" + secretsChestData.getBucketObjectReference();
                    putItemInCache(bucketObjectReference, secretsChestData.getKeyId());
                    CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
                    for(CompletableFuture<SecretsChestResponse> result : completableFutures){
                        if(!result.get().isSuccessful()){
                            isOperationSuccessful = false;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error uploading new secrets for bucket {} for requestId {}", "dataToUpload", secretsChestData.getRequestId(), e);
                    throw new SecretsChestException("Error uploading secrets data for request id: " + secretsChestData.getRequestId());
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            log.warn("Thread has been interrupted while acquiring lock");
        }
        return isOperationSuccessful;
    }

    private void sendBucketUploadTask(List<CompletableFuture<SecretsChestResponse>> completableFutures, String bucket, String finalDataToUpload, SecretsChestData secretsChestData){
        CompletableFuture<SecretsChestResponse> completableFuture = CompletableFuture.supplyAsync(() -> {
            SecretsChestResponse secretsChestResponse = null;
            try{
                secretsChestResponse = new SecretsChestResponse();
                String bucketObjectReference = secretsChestData.getGroupId() + "/" + secretsChestData.getSecretReference();
                awsService.sendUploadBucketObjectRequest(
                        bucket,
                        bucketObjectReference,
                        finalDataToUpload,
                        populateBucketMetadata(secretsChestData),
                        secretsChestData.getRequestId());
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

    private ConcurrentHashMap<String, String> populateBucketMetadata(SecretsChestData secretsChestData){
        ConcurrentHashMap<String, String> metadata = new ConcurrentHashMap<>();
        metadata.put(GROUP_METADATA, secretsChestData.getGroupId());
        metadata.put(APP_METADATA, secretsChestData.getAppId());
        metadata.put(DESC_METADATA, secretsChestData.getDataDescription());
        return metadata;
    }
}