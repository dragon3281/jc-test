package com.detection.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 字符编码配置
 * 确保HTTP请求响应都使用UTF-8编码
 */
@Configuration
public class EncodingConfig implements WebMvcConfigurer {
    
    /**
     * 字符编码过滤器
     * 确保请求和响应都使用UTF-8编码
     */
    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        filter.setForceRequestEncoding(true);
        filter.setForceResponseEncoding(true);
        return filter;
    }
    
    /**
     * 配置HTTP消息转换器
     * 确保JSON和字符串都使用UTF-8编码
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 字符串转换器
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false); // 禁用Accept-Charset
        converters.add(stringConverter);
        
        // Jackson JSON转换器
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
        converters.add(jsonConverter);
    }
}
