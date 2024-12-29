package com.averygrimes.secretschest.interaction;

import com.averygrimes.secretschest.model.SecretsChestResponse;
import com.averygrimes.secretschest.service.SecretsChestBaseService;
import com.averygrimes.secretschest.utils.ResponseBuilder;
import com.averygrimes.secretschest.utils.UUIDUtils;
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

    private SecretsChestBaseService chestBaseService;

    @Autowired
    public void setChestBaseService(SecretsChestBaseService chestBaseService) {
        this.chestBaseService = chestBaseService;
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/uploadSecrets",
            consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> uploadSecrets(@RequestBody byte[] dataToUpload){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = chestBaseService.uploadAsset(dataToUpload, requestId);
        return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/uploadSecrets/plaintext",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> uploadSecretsPlainText(@RequestBody String dataToUpload, @RequestParam(value = "disableEncryption", required = false) boolean isEncryptionDisabled){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse =  chestBaseService.uploadPlainTextAsset(dataToUpload, isEncryptionDisabled, requestId);
        return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
    }

    @RequestMapping(
            method = RequestMethod.PUT,
            value = "/updateSecrets/{secretsReference}",
            consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> updateSecrets(@PathVariable("secretsReference") String secretsReference, @RequestBody byte[] data){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = chestBaseService.updateAsset(secretsReference, data, requestId);
        return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/retrieveSecrets/{secretsReference}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> retrieveSecrets(@PathVariable("secretsReference") String secretReference, @RequestParam(value = "encryptionDisabled", required = false) boolean isEncryptionDisabled){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = chestBaseService.retrieveAsset(secretReference, isEncryptionDisabled, requestId);
        return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
    }
}