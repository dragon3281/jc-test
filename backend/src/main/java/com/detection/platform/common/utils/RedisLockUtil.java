package com.detection.platform.common.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private final StringRedisTemplate redisTemplate;
    
    // 锁过期时间(秒)
    private static final long DEFAULT_EXPIRE_TIME = 30;
    
    /**
     * 尝试获取锁
     *
     * @param lockKey 锁的键
     * @param requestId 请求ID(用于释放锁时验证)
     * @param expireTime 锁过期时间(秒)
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String requestId, long expireTime) {
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, requestId, expireTime, TimeUnit.SECONDS);
            
            if (Boolean.TRUE.equals(result)) {
                log.debug("获取分布式锁成功, lockKey: {}, requestId: {}", lockKey, requestId);
                return true;
            } else {
                log.debug("获取分布式锁失败, lockKey: {}, requestId: {}", lockKey, requestId);
                return false;
            }
        } catch (Exception e) {
            log.error("获取分布式锁异常, lockKey: {}, error: {}", lockKey, e.getMessage());
            return false;
        }
    }
    
    /**
     * 尝试获取锁(使用默认过期时间)
     */
    public boolean tryLock(String lockKey, String requestId) {
        return tryLock(lockKey, requestId, DEFAULT_EXPIRE_TIME);
    }
    
    /**
     * 释放锁
     *
     * @param lockKey 锁的键
     * @param requestId 请求ID(必须与获取锁时的requestId一致)
     * @return 是否释放成功
     */
    public boolean releaseLock(String lockKey, String requestId) {
        try {
            String currentValue = redisTemplate.opsForValue().get(lockKey);
            
            // 验证是否是当前请求持有的锁
            if (requestId.equals(currentValue)) {
                Boolean result = redisTemplate.delete(lockKey);
                log.debug("释放分布式锁成功, lockKey: {}, requestId: {}", lockKey, requestId);
                return Boolean.TRUE.equals(result);
            } else {
                log.warn("释放分布式锁失败, 锁已被其他请求持有, lockKey: {}, requestId: {}", 
                        lockKey, requestId);
                return false;
            }
        } catch (Exception e) {
            log.error("释放分布式锁异常, lockKey: {}, error: {}", lockKey, e.getMessage());
            return false;
        }
    }
    
    /**
     * 强制释放锁(不验证requestId)
     */
    public boolean forceReleaseLock(String lockKey) {
        try {
            Boolean result = redisTemplate.delete(lockKey);
            log.warn("强制释放分布式锁, lockKey: {}", lockKey);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("强制释放分布式锁异常, lockKey: {}, error: {}", lockKey, e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查锁是否存在
     */
    public boolean isLocked(String lockKey) {
        try {
            Boolean result = redisTemplate.hasKey(lockKey);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查锁是否存在异常, lockKey: {}, error: {}", lockKey, e.getMessage());
            return false;
        }
    }
    
    /**
     * 延长锁的过期时间
     */
    public boolean renewLock(String lockKey, String requestId, long expireTime) {
        try {
            String currentValue = redisTemplate.opsForValue().get(lockKey);
            
            // 验证是否是当前请求持有的锁
            if (requestId.equals(currentValue)) {
                Boolean result = redisTemplate.expire(lockKey, expireTime, TimeUnit.SECONDS);
                log.debug("延长锁过期时间成功, lockKey: {}, expireTime: {}秒", lockKey, expireTime);
                return Boolean.TRUE.equals(result);
            } else {
                log.warn("延长锁过期时间失败, 锁已被其他请求持有, lockKey: {}", lockKey);
                return false;
            }
        } catch (Exception e) {
            log.error("延长锁过期时间异常, lockKey: {}, error: {}", lockKey, e.getMessage());
            return false;
        }
    }
}
