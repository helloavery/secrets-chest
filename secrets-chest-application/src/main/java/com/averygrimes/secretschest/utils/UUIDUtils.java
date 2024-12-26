package com.averygrimes.secretschest.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomStringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

@Slf4j
public class UUIDUtils {

    public static String generateUUID() {
        try {
            MessageDigest salt = MessageDigest.getInstance("SHA-256");
            salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(salt.digest());
        } catch (Exception e) {
            log.error("Error generating new UUID", e);
            throw new RuntimeException("Error generating new UUID", e);
        }
    }

    public static String generateRandomId(){
        return RandomStringUtils.random(24, true, true);
    }
}