package com.detection.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 自动化数据检测平台主应用类
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableScheduling
@EnableAsync
@MapperScan({"com.detection.platform.dao", "com.detection.platform.mapper"})
public class DetectionPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(DetectionPlatformApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  自动化数据检测平台启动成功！");
        System.out.println("  API文档地址: http://localhost:8080/doc.html");
        System.out.println("========================================\n");
    }
}
