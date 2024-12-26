package com.averygrimes.secretschest.cache;

import com.hazelcast.config.Config;
import com.hazelcast.config.NativeMemoryConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.memory.MemorySize;
import com.hazelcast.memory.MemoryUnit;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class ClusterImpl implements Cluster {

    private HazelcastInstance hzInstance;
    private int backupCount;
    private int maxIdleSeconds;
    private int ttlSeconds;

    @PostConstruct
    public void init(){
        Config config = new Config();
        config.setClusterName("secrets-chest");
        MemorySize memorySize = new MemorySize(512, MemoryUnit.MEGABYTES);
        NativeMemoryConfig nativeMemoryConfig =
                new NativeMemoryConfig()
                        .setAllocatorType(NativeMemoryConfig.MemoryAllocatorType.POOLED)
                        .setSize(memorySize)
                        .setEnabled(true)
                        .setMinBlockSize(16)
                        .setPageSize(1 << 20);
        config.setNativeMemoryConfig(nativeMemoryConfig);
        hzInstance = Hazelcast.newHazelcastInstance(config);
    }

    public Object getHzInstance(){
        return this.hzInstance;
    }

    public int getBackupCount(){
        return this.backupCount;
    }

    public int getMaxIdleSeconds() {
        return maxIdleSeconds;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }
}