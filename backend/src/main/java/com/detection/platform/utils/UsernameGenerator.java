package com.detection.platform.utils;

import java.util.Random;

/**
 * 简单的用户名生成器，生成符合6-10位要求的用户名
 */
public class UsernameGenerator {
    private static final Random random = new Random();
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
    
    /**
     * 生成一个8位的用户名（字母开头，后跟7位数字）
     * 格式：xNNNNNNN（符合6-10位要求）
     */
    public static String generate() {
        StringBuilder username = new StringBuilder();
        // 首字母
        username.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        // 7位数字
        for (int i = 0; i < 7; i++) {
            username.append(random.nextInt(10));
        }
        return username.toString();
    }
    
    /**
     * 生成指定长度的用户名
     */
    public static String generate(int length) {
        if (length < 1) length = 8;
        StringBuilder username = new StringBuilder();
        // 首字母
        username.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        // 其余为数字
        for (int i = 1; i < length; i++) {
            username.append(random.nextInt(10));
        }
        return username.toString();
    }
}
