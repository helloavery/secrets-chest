package com.averygrimes.s3gateway.service;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.s3gateway.dto.S3GatewayDTO;

import java.security.PublicKey;

public interface CryptoService {

    PublicKey getPublicKey();

    S3GatewayDTO cryptoPrepareSendSecrets(String data, byte[] publicKey);
}