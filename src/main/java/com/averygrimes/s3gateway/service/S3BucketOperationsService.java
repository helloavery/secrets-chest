package com.averygrimes.s3gateway.service;
 
 /*=============================================================================
 |                Forecaster V1.0
 |
 |       File created by: Avery Grimes-Farrow
 |
 |       Created On:  2018-12-14            
 |            
 *===========================================================================*/

public interface S3BucketOperationsService {

    void uploadAsset();

    void fetchAsset();
}
