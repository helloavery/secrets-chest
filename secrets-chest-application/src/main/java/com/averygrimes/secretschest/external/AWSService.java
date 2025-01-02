package com.averygrimes.secretschest.external;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Avery Grimes-Farrow
 * Created on: 6/13/20
 * https://github.com/helloavery
 */

public interface AWSService {

    void sendUploadBucketObjectRequest(String bucket, String bucketObjectReference, String dataToUpload, ConcurrentHashMap<String, String> metadata, String requestId);

    Object sendRetrieveBucketObjectResponse(String bucket, String secretReference, String requestId, boolean returnAsString);
}