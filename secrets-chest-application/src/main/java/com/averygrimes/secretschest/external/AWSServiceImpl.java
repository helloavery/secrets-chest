package com.averygrimes.secretschest.external;

import com.averygrimes.secretschest.exceptions.AWSOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * @author Avery Grimes-Farrow
 * Created on: 6/13/20
 * https://github.com/helloavery
 */

@Service
@Slf4j
public class AWSServiceImpl implements AWSService{

    private S3Client amazonS3Client;

    @Autowired
    public void setAmazonS3Client(S3Client amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    @Override
    public void sendUploadBucketObjectRequest(String bucket, String bucketObjectReference, String dataToUpload, String requestId) {
        try{
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(bucketObjectReference)
                            .build();
            log.info("Uploading data to bucket {} for requestId {}", bucket, requestId);
            amazonS3Client.putObject(objectRequest, RequestBody.fromBytes(dataToUpload.getBytes()));
        }
        catch(Exception e){
            throw new AWSOperationException("Error uploading object to bucket " + bucket + " for request id " + requestId, e);
        }
    }

    @Override
    public Object sendRetrieveBucketObjectResponse(String bucket, String secretReference, String requestId, boolean returnAsString) {
        try{
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(secretReference)
                    .build();
            log.info("Retrieving data from bucket {} for requestId {}", bucket, requestId);
            return returnAsString ? amazonS3Client.getObject(getObjectRequest, ResponseTransformer.toBytes()).asUtf8String()
                    : amazonS3Client.getObject(getObjectRequest, ResponseTransformer.toBytes());
        }
        catch(Exception e){
            throw new AWSOperationException("Error retrieving object from bucket " + bucket + " for request id " + requestId, e);
        }
    }
}