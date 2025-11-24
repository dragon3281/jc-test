package com.detection.platform.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存配置
 * 
 * @author Detection Platform
 * @since 2024-11-12
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, 
                ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        // key采用String的序列化方式
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // value采用Jackson的序列化方式
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置缓存管理器
     * 支持多级缓存策略
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);

        // 默认缓存配置 - 30分钟
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .disableCachingNullValues();

        // 为不同的缓存设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 服务器信息缓存 - 5分钟
        cacheConfigurations.put("server", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 代理池信息缓存 - 10分钟
        cacheConfigurations.put("proxy-pool", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 代理节点缓存 - 3分钟(代理状态变化较快)
        cacheConfigurations.put("proxy-node", defaultConfig.entryTtl(Duration.ofMinutes(3)));
        
        // POST模板缓存 - 1小时(模板相对稳定)
        cacheConfigurations.put("post-template", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 用户信息缓存 - 30分钟
        cacheConfigurations.put("user", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 任务信息缓存 - 1分钟(任务状态变化频繁)
        cacheConfigurations.put("task", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        
        // 基础数据缓存 - 1小时
        cacheConfigurations.put("base-data", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 检测结果缓存 - 2小时
        cacheConfigurations.put("detection-result", defaultConfig.entryTtl(Duration.ofHours(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
