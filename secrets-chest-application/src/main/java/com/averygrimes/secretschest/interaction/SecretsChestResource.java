package com.averygrimes.secretschest.interaction;

import com.averygrimes.secretschest.model.SecretsChestConstants;
import com.averygrimes.secretschest.model.SecretsChestResponse;
import com.averygrimes.secretschest.service.SecretsChestBaseService;
import com.averygrimes.secretschest.utils.ResponseBuilder;
import com.averygrimes.secretschest.utils.UUIDUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

@RestController("/secretsChestBase")
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
    public ResponseEntity<Object> uploadSecrets(byte[] dataToUpload){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = chestBaseService.uploadAsset(dataToUpload, requestId);
        return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/uploadSecrets/format/{format}",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> uploadSecrets(@RequestParam("format") String format, String dataToUpload){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = null;
        if(StringUtils.equalsIgnoreCase(format, SecretsChestConstants.PLAIN_TEXT_DATA)){
            secretsChestResponse =  chestBaseService.uploadPlainTextAsset(dataToUpload, requestId);
        }else{
            secretsChestResponse = chestBaseService.uploadAsset(dataToUpload.getBytes(), requestId);
        }
        return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
    }

    @RequestMapping(
            method = RequestMethod.PUT,
            value = "/updateSecrets/{secretsReference}",
            consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> updateSecrets(@RequestParam("secretsReference") String secretsReference, byte[] data){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = chestBaseService.updateAsset(secretsReference, data, requestId);
        return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/retrieveSecrets/{secretsReference}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> retrieveSecrets(@RequestParam("secretsReference") String secretReference){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = chestBaseService.retrieveAsset(secretReference, requestId);
        return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
    }
}