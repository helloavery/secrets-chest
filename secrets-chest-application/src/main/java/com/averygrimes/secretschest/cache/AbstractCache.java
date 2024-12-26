package com.averygrimes.secretschest.cache;

import com.averygrimes.secretschest.exceptions.CacheException;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Service
public class AbstractCache implements CacheBase {

    protected Cluster cluster;
    protected HazelcastInstance hzInstance;
    protected String name;
    protected IMap<String, CacheObject> cacheMap;
    protected Long refreshMillis = 0L;

    @Autowired
    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    @PostConstruct
    public void init(){
        this.hzInstance = (HazelcastInstance) this.cluster.getHzInstance();
        this.name = "secrets-chest";
        if (!cacheExists(hzInstance, name)) {
            MapConfig mapConfig = getMapConfig();
            mapConfig.setTimeToLiveSeconds(cluster.getTtlSeconds());
            this.hzInstance.getConfig().addMapConfig(mapConfig);
        }
        this.cacheMap = hzInstance.getMap(name);
    }

    @Override
    public Object getItemFromCache(String key) {
        if(key == null){
            throw new CacheException("");
        }
        if(cacheMap.containsKey(key)){
            CacheObject cacheObject = cacheMap.get(key);
            if(cacheObject != null){
                return cacheObject.getValueObject();
            }
        }
        return null;
    }

    @Override
    public void putItemInCache(String key, Object value) {
        putItemInCache(key, value, (long) cluster.getTtlSeconds(), TimeUnit.SECONDS);
    }

    public void putItemInCache(String key, Object value, Long ttl, TimeUnit timeUnit) {
        cacheMap.putIfAbsent(key, createCacheObject(key, value), ttl, timeUnit);
    }

    @Override
    public void updateItemInCache(String key, Object value) {
        updateItemInCache(key, value, (long) cluster.getTtlSeconds(), TimeUnit.SECONDS);
    }

    public void updateItemInCache(String key, Object value, Long ttl, TimeUnit timeUnit){
        cacheMap.put(key, createCacheObject(key, value), ttl, timeUnit);
    }

    @Override
    public void removeItemFromCache(String key) {
        cacheMap.remove(key);
    }

    @Override
    public void clearCache() {
        cacheMap.clear();
    }

    private CacheObject createCacheObject(String key, Object value){
        CacheObject cacheObject = new CacheObject();
        cacheObject.setKey(key);
        cacheObject.setValueObject((Serializable) value);
        cacheObject.setCreatedTimestamp(System.currentTimeMillis());
        return cacheObject;
    }

    private MapConfig getMapConfig(){
        MapConfig mapConfig = new MapConfig();
        mapConfig.setName(name);
        mapConfig.setBackupCount(cluster.getBackupCount());
        mapConfig.setMaxIdleSeconds(cluster.getMaxIdleSeconds());
        mapConfig.setEvictionConfig(new EvictionConfig().setEvictionPolicy(EvictionPolicy.LFU).setMaxSizePolicy(MaxSizePolicy.USED_HEAP_PERCENTAGE).setSize(90));
        return mapConfig;
    }

    private boolean cacheExists(HazelcastInstance hzInstance, String name){
        for(DistributedObject distributedObject : hzInstance.getDistributedObjects()){
            if(name.equals(distributedObject.getName())){
                return true;
            }
        }
        return false;
    }
}