package com.averygrimes.secretschest.cache;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class CacheObject implements Serializable{


    private String key;
    private Serializable valueObject;
    private Long createdTimestamp;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Serializable getValueObject() {
        return valueObject;
    }

    public void setValueObject(Serializable valueObject) {
        this.valueObject = valueObject;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj  == this) return true;
        if (!(obj instanceof CacheObject)){
            return false;
        }
        CacheObject cacheObject = (CacheObject) obj;
        return new EqualsBuilder()
                .append(key, cacheObject.key)
                .append(valueObject, cacheObject.valueObject)
                .append(createdTimestamp, cacheObject.createdTimestamp)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(valueObject.hashCode(), 37)
                .append(key)
                .append(createdTimestamp)
                .toHashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}