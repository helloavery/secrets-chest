package com.averygrimes.secretschest.utils;

/**
 * @author Avery Grimes-Farrow
 * Created on: 9/2/19
 * https://github.com/helloavery
 */

public interface SecretsChestConstants {

    /** SecretsChestsBase Constants **/
    String S3_KEY_NAMING_PATTERN = "%s_%s_%s";
    String GROUP_METADATA = "Group ID";
    String APP_METADATA = "Application ID";
    String DESC_METADATA = "Secret Description";

    String ERROR_LOG_FORMAT = "status={}, message={}, errors={}";
}