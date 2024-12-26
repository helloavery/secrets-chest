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
import com.averygrimes.secretschest.utils.SecretsChestCredUtils;
import com.averygrimes.secretschest.utils.UUIDUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@Service
@Slf4j
public class SecretsChestBaseServiceImpl implements SecretsChestBaseService {

    private Environment environment;
    private CryptoService cryptoService;
    private CacheBase cacheService;
    private ExecutorService executorService;
    private SecretsChestCredUtils chestCredUtils;
    private Lock lock;
    private AWSService awsService;
    private static String AWS_S3_DATA_BUCKET;
    private static String AWS_S3_KEY_BUCKET;

    @Autowired
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Autowired
    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Autowired
    public void setCacheService(CacheBase cacheService) {
        this.cacheService = cacheService;
    }

    @Autowired
    public void setChestCredUtils(SecretsChestCredUtils chestCredUtils) {
        this.chestCredUtils = chestCredUtils;
    }

    @Autowired
    public void setAwsService(AWSService awsService) {
        this.awsService = awsService;
    }

    @PostConstruct
    public void init(){
        AWS_S3_DATA_BUCKET = environment.getProperty("AWSS3DataBucket");
        AWS_S3_KEY_BUCKET = environment.getProperty("AWSS3KeyBucket");
        this.executorService = Executors.newFixedThreadPool(100);
        this.lock = new ReentrantLock(true);
    }

    @Override
    public SecretsChestResponse uploadAsset(byte[] dataToUpload, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            if (lock.tryLock(1500, TimeUnit.MILLISECONDS)) {
                try {
                    CountDownLatch countDownLatch = new CountDownLatch(2);
                    SecretsChestData encryptedDataMap = cryptoService.generateDataKeyAndEncryptData(dataToUpload);
                    String bucketObjectReference = UUIDUtils.generateRandomId();

                    CompletableFuture<SecretsChestResponse> encryptedKeyTask = sendEncryptedUploadTasks(AWS_S3_KEY_BUCKET, encryptedDataMap.getEncryptedKey(), bucketObjectReference, requestId);
                    CompletableFuture<SecretsChestResponse> encryptedDataTask = sendEncryptedUploadTasks(AWS_S3_DATA_BUCKET, encryptedDataMap.getEncryptedData(), bucketObjectReference, requestId);
                    waitForCompletableFutureTasksToComplete(encryptedKeyTask, countDownLatch, requestId);
                    waitForCompletableFutureTasksToComplete(encryptedDataTask, countDownLatch, requestId);

                    secretsChestResponse.setSecretReference(bucketObjectReference);
                    secretsChestResponse.setSuccessful(true);
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
            awsService.sendUploadBucketObjectRequest(AWS_S3_DATA_BUCKET, bucketObjectReference, dataToUpload, requestId);
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
            awsService.sendUploadBucketObjectRequest(AWS_S3_DATA_BUCKET, secretsReference, hexEncodedEncryptedData, requestId);
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
                    String s3ObjectOutput = (String) awsService.sendRetrieveBucketObjectResponse(AWS_S3_DATA_BUCKET, secretReference, requestId, true);
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

    private CompletableFuture<SecretsChestResponse> sendEncryptedUploadTasks(String bucket, byte[] dataToUpload, String bucketObjectReference, String requestId){
        CompletableFuture<SecretsChestResponse> completableFuture = new CompletableFuture<>();
        CompletableFuture.supplyAsync(() -> {
            SecretsChestResponse secretsChestResponse = null;
            try{
                secretsChestResponse = new SecretsChestResponse();
                String hexEncodedBytes = Hex.encodeHexString(dataToUpload);
                awsService.sendUploadBucketObjectRequest(bucket, bucketObjectReference, hexEncodedBytes, requestId);
                if(bucket.equals(AWS_S3_KEY_BUCKET)){
                    putEncryptedKeyInCache(bucketObjectReference, dataToUpload);
                }
                secretsChestResponse.setSuccessful(true);
                completableFuture.complete(secretsChestResponse);
            }
            catch(Exception e){
                log.error("Error completing upload data task", e);
            }
            return secretsChestResponse;
        }, executorService).applyToEither(chestCredUtils.timeoutRetrieveInvocationResponse(completableFuture, 10, TimeUnit.SECONDS), Function.identity());
        return completableFuture;
    }

    private void waitForCompletableFutureTasksToComplete(CompletableFuture<SecretsChestResponse> completableFuture, CountDownLatch countDownLatch, String requestId){
        try {
            completableFuture.whenComplete((uploadResponse, exception) -> {
                if (uploadResponse == null || exception != null) {
                    log.error("Exception occurred while uploading data to S3 bucket", exception);
                    throw new SecretsChestException("Error uploading secrets data for request id: " + requestId);
                }
                if (!uploadResponse.isSuccessful()) {
                    log.error("Operation uploading data to S3 bucket unsuccessful");
                    throw new SecretsChestException("Error uploading secrets data for request id: " + requestId);
                }
                countDownLatch.countDown();
            });
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.warn("Thread has been interrupted");
        }
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
                String s3ObjectOutput = (String) awsService.sendRetrieveBucketObjectResponse(AWS_S3_KEY_BUCKET, secretReference, requestId, true);
                byte[] hexDecodedKey = Hex.decodeHex(s3ObjectOutput);
                return SdkBytes.fromByteArray(hexDecodedKey);
            }
            catch(DecoderException e){
                log.error("Error decoding hex encrypted key for request id {}", requestId, e);
                throw new SecretsChestException("Error decoding hex encrypted key for request id: " + requestId);
            }
        }
    }

    @PreDestroy
    public void preDestroy(){
        executorService.shutdown();
    }
}