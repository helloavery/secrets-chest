package com.averygrimes.secretschest.external;

import com.averygrimes.secretschest.exceptions.SecretsChestException;
import com.averygrimes.secretschest.utils.RequestStatCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Avery Grimes-Farrow
 * Created on: 6/13/20
 * https://github.com/helloavery
 */

@Service
@Slf4j
public class AWSServiceImpl implements AWSService{

    private S3Client amazonS3Client;
    private RequestStatCollector requestStatCollector;

    @Autowired
    public void setAmazonS3Client(S3Client amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    @Autowired
    public void setRequestStatCollector(RequestStatCollector requestStatCollector) {
        this.requestStatCollector = requestStatCollector;
    }

    @Override
    public void sendUploadBucketObjectRequest(String bucket, String bucketObjectReference, String dataToUpload, ConcurrentHashMap<String, String> metadata, String requestId) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("AWSServiceImpl - sendUploadBucketObjectRequest: starting task to upload object to bucket");
        try{
            List<Tag> tags = new ArrayList<>();
            metadata.forEach((key, value) -> tags.add(Tag.builder().key(key).value(value).build()));
            Tagging tagging = Tagging.builder().tagSet(tags).build();
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(bucketObjectReference)
                            .tagging(tagging)
                            .build();
            log.info("Uploading data to bucket {} for requestId {}", bucket, requestId);
            amazonS3Client.putObject(objectRequest, RequestBody.fromBytes(dataToUpload.getBytes()));
        }
        catch(Exception e){
            log.error("Error occurred while uploading object to s3 bucket for bucket={}, bucketObjectReference={}", bucket, bucketObjectReference);
            SecretsChestException secretsChestException = new SecretsChestException(500, "Error occurred while uploading object to s3 bucket for bucket", e);
            requestStatCollector.recordError(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(), secretsChestException, stopWatch);
            throw secretsChestException;
        }
        log.info("AWSServiceImpl - sendUploadBucketObjectRequest: successfully uploading object to bucket");
        requestStatCollector.recordSuccess(StackWalker.getInstance()
                .walk(s -> s.skip(1).findFirst())
                .get()
                .getMethodName(), stopWatch);
    }

    @Override
    public Object sendRetrieveBucketObjectResponse(String bucket, String secretReference, String requestId, boolean returnAsString) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("AWSServiceImpl - sendRetrieveBucketObjectResponse: starting task to retrieve object from bucket");
        try{
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(secretReference)
                    .build();
            log.info("Retrieving data from bucket {} for requestId {}", bucket, requestId);
            Object retrievedBucketObject =  returnAsString ? amazonS3Client.getObject(getObjectRequest, ResponseTransformer.toBytes()).asUtf8String()
                    : amazonS3Client.getObject(getObjectRequest, ResponseTransformer.toBytes());
            log.info("AWSServiceImpl - sendRetrieveBucketObjectResponse: successfully retrieved object from bucket");
            requestStatCollector.recordSuccess(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(), stopWatch);
            return retrievedBucketObject;
        }
        catch(Exception e){
            log.error("Error occurred while retrieving object to s3 bucket for bucket={}, secretReference={}", bucket, secretReference);
            SecretsChestException secretsChestException = new SecretsChestException(500, "Error retrieving object from bucket " + bucket + " for request id " + requestId, e);
            requestStatCollector.recordError(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(), secretsChestException, stopWatch);
            throw secretsChestException;
        }
    }
}