package com.averygrimes.secretschest.interaction;

import com.averygrimes.secretschest.exceptions.SecretsChestException;
import com.averygrimes.secretschest.model.SecretsChestData;
import com.averygrimes.secretschest.model.SecretsChestRequest;
import com.averygrimes.secretschest.model.SecretsChestResponse;
import com.averygrimes.secretschest.service.SecretsChestBaseService;
import com.averygrimes.secretschest.utils.RequestStatCollector;
import com.averygrimes.secretschest.utils.RequestValidator;
import com.averygrimes.secretschest.utils.ResponseBuilder;
import com.averygrimes.secretschest.utils.UUIDUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import static com.averygrimes.secretschest.utils.SecretsChestConstants.ERROR_LOG_FORMAT;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

@RestController
@RequestMapping("/secretsChestBase")
@Slf4j
public class SecretsChestResource {

    private RequestValidator requestValidator;
    private SecretsChestBaseService chestBaseService;
    private RequestStatCollector requestStatCollector;

    @Autowired
    public void setRequestValidator(RequestValidator requestValidator) {
        this.requestValidator = requestValidator;
    }

    @Autowired
    public void setChestBaseService(SecretsChestBaseService chestBaseService) {
        this.chestBaseService = chestBaseService;
    }

    @Autowired
    public void setRequestStatCollector(RequestStatCollector requestStatCollector) {
        this.requestStatCollector = requestStatCollector;
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/uploadSecrets",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> uploadSecrets(@RequestBody @Valid SecretsChestRequest secretsChestRequest){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try{
            log.info("SecretsChestResource - uploadSecrets: retrieved request to start secret upload");
            SecretsChestData secretsChestData = requestValidator.validateAndTransformIncomingRequest(secretsChestRequest);
            SecretsChestResponse secretsChestResponse = chestBaseService.uploadAsset(secretsChestData);
            requestStatCollector.recordSuccess(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(), secretsChestResponse, stopWatch);
            return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
        } catch (SecretsChestException e) {
            log.error(ERROR_LOG_FORMAT, e.getStatusCode(), e.getMessage(), e.getErrors());
            requestStatCollector.recordError(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(),e, stopWatch);
            throw e;
        } catch (Exception e) {
            log.error("Exception occurred while uploading secrets {}", e.getMessage(), e);
            SecretsChestException secretsChestException = new SecretsChestException(500, "Exception occurred while uploading secrets", e);
            requestStatCollector.recordError(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(),secretsChestException, stopWatch);
            throw secretsChestException;
        }
    }

    @RequestMapping(
            method = RequestMethod.PUT,
            value = "/updateSecrets/{secretsReference}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> updateSecrets(@PathVariable("secretsReference") String secretsReference, @RequestBody @Valid SecretsChestRequest secretsChestRequest){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try{
            log.info("SecretsChestResource - updateSecrets: retrieved request to start secret update");
            SecretsChestData secretsChestData = requestValidator.validateAndTransformIncomingRequest(secretsChestRequest);
            SecretsChestResponse secretsChestResponse = chestBaseService.updateAsset(secretsReference, secretsChestData);
            requestStatCollector.recordSuccess(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(),secretsChestResponse, stopWatch);
            return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
        } catch (SecretsChestException e) {
            log.error(ERROR_LOG_FORMAT, e.getStatusCode(), e.getMessage(), e.getErrors());
            requestStatCollector.recordError(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(),e, stopWatch);
            throw e;
        } catch (Exception e) {
            log.error("Exception occurred while updating secrets {}", e.getMessage(), e);
            SecretsChestException secretsChestException = new SecretsChestException(500, "Exception occurred while updating secrets", e);
            requestStatCollector.recordError(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(),secretsChestException, stopWatch);
            throw secretsChestException;
        }
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/retrieveSecrets/groupId/{groupId}/secretsReference/{secretsReference}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> retrieveSecrets(@PathVariable("groupId") String groupId, @PathVariable("secretsReference") String secretReference) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try{
            log.info("SecretsChestResource - retrieveSecrets: retrieved request to start secret retrieval");
            String requestId = UUIDUtils.generateRandomId();
            SecretsChestResponse secretsChestResponse = chestBaseService.retrieveAsset(groupId, secretReference, requestId);
            requestStatCollector.recordSuccess(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(),secretsChestResponse, stopWatch);
            return ResponseBuilder.buildAndReturnResponse(secretsChestResponse);
        } catch (SecretsChestException e) {
            log.error(ERROR_LOG_FORMAT, e.getStatusCode(), e.getMessage(), e.getErrors());
            requestStatCollector.recordError(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(),e, stopWatch);
            throw e;
        } catch (Exception e) {
            log.error("Exception occurred while retrieving secrets {}", e.getMessage(), e);
            SecretsChestException secretsChestException = new SecretsChestException(500, "Exception occurred while retrieving secrets", e);
            requestStatCollector.recordError(StackWalker.getInstance()
                    .walk(s -> s.skip(1).findFirst())
                    .get()
                    .getMethodName(),secretsChestException, stopWatch);
            throw secretsChestException;
        }
    }
}