package com.averygrimes.secretschest.utils;

import com.averygrimes.secretschest.exceptions.SecretsChestException;
import com.averygrimes.secretschest.model.SecretsChestData;
import com.averygrimes.secretschest.model.SecretsChestRequest;
import io.micrometer.common.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RequestValidator {

    public SecretsChestData validateAndTransformIncomingRequest(SecretsChestRequest secretsChestRequest){
        if(secretsChestRequest == null){
            throw new SecretsChestException(400, "Incoming request is null or empty");
        }

        List<String> errors = new ArrayList<>();
        if(StringUtils.isBlank(secretsChestRequest.getGroupId())){
            errors.add("Group id is null or blank");
        }
        if(StringUtils.isBlank(secretsChestRequest.getAppId())){
            errors.add("App id is null or blank");
        }
        if(StringUtils.isBlank(secretsChestRequest.getDataDescription())){
            errors.add("Description id is null or blank");
        }

        if(CollectionUtils.isNotEmpty(errors)){
            throw new SecretsChestException(400, errors);
        }

        SecretsChestData secretsChestData = new SecretsChestData();
        secretsChestData.setDataFromRequest(secretsChestRequest.getDataToUpload());
        secretsChestData.setGroupId(secretsChestRequest.getGroupId());
        secretsChestData.setAppId(secretsChestRequest.getAppId());
        secretsChestData.setDataDescription(secretsChestRequest.getDataDescription());
        secretsChestData.setDataRequestType(secretsChestRequest.getDataRequestType());
        String requestId = secretsChestRequest.getRequestId();
        if(StringUtils.isBlank(requestId)){
            requestId = UUIDUtils.generateRandomId();
        }
        secretsChestData.setRequestId(requestId);
        return secretsChestData;
    }
}