package com.detection.platform.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP请求包解析器测试
 */
@SpringBootTest
class RequestPackageParserTest {

    @Autowired
    private RequestPackageParser parser;

    @Test
    void testParseSimplePostRequest() {
        String rawRequest = """
                POST /api/check HTTP/1.1
                Host: example.com
                Authorization: Bearer {{token}}
                Content-Type: application/json
                
                {"mobile":"{{phone}}","type":"verify"}
                """;

        RequestPackageParser.ParseResult result = parser.parseRawRequest(rawRequest);

        assertTrue(result.isSuccess());
        assertEquals("https://example.com/api/check", result.getUrl());
        assertEquals("POST", result.getMethod());
        assertEquals(3, result.getHeaders().size());
        assertEquals(2, result.getVariables().size());
        
        // 验证变量识别
        RequestPackageParser.VariableInfo tokenVar = result.getVariables().stream()
                .filter(v -> "token".equals(v.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(tokenVar);
        assertEquals("令牌", tokenVar.getSuggestedType());
        
        RequestPackageParser.VariableInfo phoneVar = result.getVariables().stream()
                .filter(v -> "phone".equals(v.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(phoneVar);
        assertEquals("手机号", phoneVar.getSuggestedType());
        assertEquals("mobile", phoneVar.getFieldName());
    }

    @Test
    void testParseGetRequest() {
        String rawRequest = """
                GET /api/users?id={{userId}} HTTP/1.1
                Host: api.example.com
                Authorization: Bearer {{token}}
                """;

        RequestPackageParser.ParseResult result = parser.parseRawRequest(rawRequest);

        assertTrue(result.isSuccess());
        assertEquals("https://api.example.com/api/users?id={{userId}}", result.getUrl());
        assertEquals("GET", result.getMethod());
    }

    @Test
    void testParseWithoutPlaceholders() {
        String rawRequest = """
                POST /api/login HTTP/1.1
                Host: example.com
                Content-Type: application/json
                
                {"username":"test","password":"123456"}
                """;

        RequestPackageParser.ParseResult result = parser.parseRawRequest(rawRequest);

        assertTrue(result.isSuccess());
        assertEquals("https://example.com/api/login", result.getUrl());
        // 应该通过智能检测找到用户名字段
        assertNotNull(result.getVariables());
    }

    @Test
    void testParseInvalidRequest() {
        String rawRequest = "INVALID REQUEST";

        RequestPackageParser.ParseResult result = parser.parseRawRequest(rawRequest);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }
}
