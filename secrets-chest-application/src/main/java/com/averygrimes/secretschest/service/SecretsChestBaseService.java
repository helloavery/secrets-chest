package com.averygrimes.secretschest.service;


import com.averygrimes.secretschest.model.SecretsChestData;
import com.averygrimes.secretschest.model.SecretsChestRequest;
import com.averygrimes.secretschest.model.SecretsChestResponse;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

public interface SecretsChestBaseService {

    SecretsChestResponse uploadAsset(SecretsChestData secretsChestData);

    SecretsChestResponse updateAsset(String secretsReference, SecretsChestData secretsChestData);

    SecretsChestResponse retrieveAsset(String groupId, String secretReference, String requestId);
}