package com.averygrimes.secretschest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Slf4j
public class AppConfig {

    @Bean
    public S3Client amazonS3Client(){
        return S3Client.builder().region(Region.US_EAST_2).build();
    }

    @Bean
    public KmsClient kmsClient(){
        log.info("Retrieving IAM credentials");
        AwsCredentials awsCredentials = ProfileCredentialsProvider.create("kmsUser").resolveCredentials();
        StaticCredentialsProvider staticCredentialsProvider =  StaticCredentialsProvider.create(awsCredentials);
        return KmsClient.builder().region(Region.US_EAST_2).credentialsProvider(staticCredentialsProvider).build();
    }
}