package com.averygrimes.secretschest.cache;

public interface Cluster {

    Object getHzInstance();

    int getBackupCount();

    int getMaxIdleSeconds();

    int getTtlSeconds();
}