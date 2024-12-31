package com.averygrimes.secretschest.interaction;

import com.averygrimes.secretschest.model.SecretsChestData;
import com.averygrimes.secretschest.model.SecretsChestRequest;
import com.averygrimes.secretschest.model.SecretsChestResponse;
import com.averygrimes.secretschest.service.SecretsChestBaseService;
import com.averygrimes.secretschest.utils.RequestValidator;
import com.averygrimes.secretschest.utils.ResponseBuilder;
import com.averygrimes.secretschest.utils.UUIDUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

@RestController
@RequestMapping("/secretsChestBase")
public class SecretsChestResource {

    private RequestValidator requestValidator;
    private SecretsChestBaseService chestBaseService;

    @Autowired
    public void setRequestValidator(RequestValidator requestValidator) {
        this.requestValidator = requestValidator;
    }

    @Autowired
    public void setChestBaseService(SecretsChestBaseService chestBaseService) {
        this.chestBaseService = chestBaseService;
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/uploadSecrets",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> uploadSecrets(@RequestBody @Valid SecretsChestRequest secretsChestRequest){
        SecretsChestData secretsChestData = requestValidator.validateAndTransformIncomingRequest(secretsChestRequest);
        SecretsChestResponse secretsChestResponse = chestBaseService.uploadAsset(secretsChestData);
        return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
    }

    @RequestMapping(
            method = RequestMethod.PUT,
            value = "/updateSecrets/{secretsReference}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> updateSecrets(@PathVariable("secretsReference") String secretsReference, @RequestBody @Valid SecretsChestRequest secretsChestRequest){
        SecretsChestData secretsChestData = requestValidator.validateAndTransformIncomingRequest(secretsChestRequest);
        SecretsChestResponse secretsChestResponse = chestBaseService.updateAsset(secretsReference, secretsChestData);
        return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/retrieveSecrets/groupId/{groupId}/secretsReference/{secretsReference}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> retrieveSecrets(@PathVariable("groupId") String groupId, @PathVariable("secretsReference") String secretReference) {
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = chestBaseService.retrieveAsset(groupId, secretReference, requestId);
        return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
    }
}