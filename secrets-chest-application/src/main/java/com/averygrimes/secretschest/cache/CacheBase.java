package com.averygrimes.secretschest.cache;

import java.util.Optional;

public interface CacheBase {

    Optional<Object> getItemFromCache(String key);

    void putItemInCache(String key, Object value);

    void updateItemInCache(String key, Object value);

    void removeItemFromCache(String key);

    void clearCache();
}