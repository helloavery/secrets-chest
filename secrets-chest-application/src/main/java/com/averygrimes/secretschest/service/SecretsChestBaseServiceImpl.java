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
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.averygrimes.secretschest.utils.SecretsChestConstants.ERROR_UPLOAD;

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
            if (lock.tryLock(1500, TimeUnit.MILLISECONDS)) {
                try {
                    SecretsChestData encryptedDataMap = cryptoService.generateDataKeyAndEncryptData(dataToUpload);
                    String bucketObjectReference = UUIDUtils.generateRandomId();
                    secretsChestResponse.setSecretReference(bucketObjectReference);
                    secretsChestResponse.setSuccessful(true);

                    List<CompletableFuture<SecretsChestResponse>> completableFutures = new ArrayList<>();
                    sendEncryptedUploadTask(completableFutures, awsS3KeyBucket, encryptedDataMap.getEncryptedKey(), bucketObjectReference, requestId);
                    sendEncryptedUploadTask(completableFutures, awsS3DataBucket, encryptedDataMap.getEncryptedData(), bucketObjectReference, requestId);

                    CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
                    for(CompletableFuture<SecretsChestResponse> result : completableFutures){
                        if(!result.get().isSuccessful()){
                            secretsChestResponse.setSecretReference(ERROR_UPLOAD);
                            secretsChestResponse.setSuccessful(false);
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
        return secretsChestResponse;
    }

    @Override
    public SecretsChestResponse uploadPlainTextAsset(String dataToUpload, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            String bucketObjectReference = UUIDUtils.generateRandomId();
            awsService.sendUploadBucketObjectRequest(awsS3DataBucket, bucketObjectReference, dataToUpload, requestId);
            secretsChestResponse.setSecretReference(bucketObjectReference);
            secretsChestResponse.setSuccessful(true);
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
            ByteBuffer encryptedKey = attemptToGetKeyFromCacheThenBucket(secretsReference, requestId).asByteBuffer();
            byte[] encryptedData = cryptoService.encryptDataWithoutGeneratingDataKey(dataToUpload, SdkBytes.fromByteBuffer(encryptedKey));
            String hexEncodedEncryptedData = Hex.encodeHexString(encryptedData);
            awsService.sendUploadBucketObjectRequest(awsS3DataBucket, secretsReference, hexEncodedEncryptedData, requestId);
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
    public SecretsChestResponse retrieveAsset(String secretReference, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            if(lock.tryLock(3500, TimeUnit.MILLISECONDS)){
                try{
                    String s3ObjectOutput = (String) awsService.sendRetrieveBucketObjectResponse(awsS3DataBucket, secretReference, requestId, true);
                    byte[] hexDecodedBucketObject = Hex.decodeHex(s3ObjectOutput);
                    SdkBytes encryptedKey = attemptToGetKeyFromCacheThenBucket(secretReference, requestId);
                    byte[] decryptedData = cryptoService.decryptData(hexDecodedBucketObject, encryptedKey.asByteBuffer());
                    secretsChestResponse.setData(decryptedData);
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

    private void sendEncryptedUploadTask(List<CompletableFuture<SecretsChestResponse>> completableFutures, String bucket, byte[] dataToUpload, String bucketObjectReference, String requestId){
        CompletableFuture<SecretsChestResponse> completableFuture = CompletableFuture.supplyAsync(() -> {
            SecretsChestResponse secretsChestResponse = null;
            try{
                secretsChestResponse = new SecretsChestResponse();
                String hexEncodedBytes = Hex.encodeHexString(dataToUpload);
                awsService.sendUploadBucketObjectRequest(bucket, bucketObjectReference, hexEncodedBytes, requestId);
                if(bucket.equalsIgnoreCase(awsS3KeyBucket)){
                    putEncryptedKeyInCache(bucketObjectReference, dataToUpload);
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

    private void putEncryptedKeyInCache(String bucketObjectReference, byte[] encryptedKey){
        try{
            cacheService.putItemInCache(bucketObjectReference, encryptedKey);
        }
        catch(Exception e){
            log.warn("Error putting encrypted key in key cache", e);
        }
    }

    private SdkBytes attemptToGetKeyFromCacheThenBucket(String secretReference, String requestId){
        if(cacheService.getItemFromCache(secretReference) != null){
            byte[] encryptedKeyByteArray = (byte[]) cacheService.getItemFromCache(secretReference);
            return SdkBytes.fromByteArray(encryptedKeyByteArray);
        }
        else{
            try{
                String s3ObjectOutput = (String) awsService.sendRetrieveBucketObjectResponse(awsS3KeyBucket, secretReference, requestId, true);
                byte[] hexDecodedKey = Hex.decodeHex(s3ObjectOutput);
                return SdkBytes.fromByteArray(hexDecodedKey);
            }
            catch(DecoderException e){
                log.error("Error decoding hex encrypted key for request id {}", requestId, e);
                throw new SecretsChestException("Error decoding hex encrypted key for request id: " + requestId);
            }
        }
    }
}