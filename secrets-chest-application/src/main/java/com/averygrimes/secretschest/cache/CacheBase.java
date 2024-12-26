package com.averygrimes.secretschest.cache;

public interface CacheBase {

    Object getItemFromCache(String key);

    void putItemInCache(String key, Object value);

    void updateItemInCache(String key, Object value);

    void removeItemFromCache(String key);

    void clearCache();
}