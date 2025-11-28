package com.detection.platform.utils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用户名/密码规则检测工具类
 * 智能检测网站对用户名和密码的要求，并自适应生成符合规则的测试数据
 */
public class UsernamePasswordRuleDetector {

    private static final Logger log = LoggerFactory.getLogger(UsernamePasswordRuleDetector.class);

    /**
     * 用户名规则
     */
    public static class UsernameRule {
        private Integer minLength = 6;
        private Integer maxLength = 16;
        private Boolean allowLetter = true;
        private Boolean allowDigit = true;
        private Boolean allowUnderscore = false;
        private Boolean mustStartWithLetter = true;
        private String pattern = null;

        public Integer getMinLength() { return minLength; }
        public void setMinLength(Integer minLength) { this.minLength = minLength; }
        public Integer getMaxLength() { return maxLength; }
        public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
        public Boolean getAllowLetter() { return allowLetter; }
        public void setAllowLetter(Boolean allowLetter) { this.allowLetter = allowLetter; }
        public Boolean getAllowDigit() { return allowDigit; }
        public void setAllowDigit(Boolean allowDigit) { this.allowDigit = allowDigit; }
        public Boolean getAllowUnderscore() { return allowUnderscore; }
        public void setAllowUnderscore(Boolean allowUnderscore) { this.allowUnderscore = allowUnderscore; }
        public Boolean getMustStartWithLetter() { return mustStartWithLetter; }
        public void setMustStartWithLetter(Boolean mustStartWithLetter) { this.mustStartWithLetter = mustStartWithLetter; }
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
    }

    /**
     * 密码规则
     */
    public static class PasswordRule {
        private Integer minLength = 8;
        private Integer maxLength = 20;
        private Boolean requireLowerCase = false;
        private Boolean requireUpperCase = false;
        private Boolean requireDigit = false;
        private Boolean requireSpecialChar = false;
        private String allowedSpecialChars = "@#$%^&*()_+-=";
        private String pattern = null;

        public Integer getMinLength() { return minLength; }
        public void setMinLength(Integer minLength) { this.minLength = minLength; }
        public Integer getMaxLength() { return maxLength; }
        public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
        public Boolean getRequireLowerCase() { return requireLowerCase; }
        public void setRequireLowerCase(Boolean requireLowerCase) { this.requireLowerCase = requireLowerCase; }
        public Boolean getRequireUpperCase() { return requireUpperCase; }
        public void setRequireUpperCase(Boolean requireUpperCase) { this.requireUpperCase = requireUpperCase; }
        public Boolean getRequireDigit() { return requireDigit; }
        public void setRequireDigit(Boolean requireDigit) { this.requireDigit = requireDigit; }
        public Boolean getRequireSpecialChar() { return requireSpecialChar; }
        public void setRequireSpecialChar(Boolean requireSpecialChar) { this.requireSpecialChar = requireSpecialChar; }
        public String getAllowedSpecialChars() { return allowedSpecialChars; }
        public void setAllowedSpecialChars(String allowedSpecialChars) { this.allowedSpecialChars = allowedSpecialChars; }
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
    }

    /**
     * 从HTML中检测用户名规则
     */
    public static UsernameRule detectUsernameRule(Document doc, String jsCode) {
        UsernameRule rule = new UsernameRule();
        
        try {
            // 1. 从HTML input标签检测
            Element usernameInput = findUsernameInput(doc);
            if (usernameInput != null) {
                detectFromHtmlAttributes(usernameInput, rule);
            }
            
            // 2. 从JS代码检测
            if (jsCode != null && !jsCode.isEmpty()) {
                detectFromJavaScript(jsCode, "username", rule);
            }
            
            log.info("检测到用户名规则: 长度[{}-{}], 字母:{}, 数字:{}, 下划线:{}, 必须字母开头:{}", 
                    rule.getMinLength(), rule.getMaxLength(), 
                    rule.getAllowLetter(), rule.getAllowDigit(), 
                    rule.getAllowUnderscore(), rule.getMustStartWithLetter());
            
        } catch (Exception e) {
            log.warn("检测用户名规则时出错: {}", e.getMessage());
        }
        
        return rule;
    }

    /**
     * 从HTML中检测密码规则
     */
    public static PasswordRule detectPasswordRule(Document doc, String jsCode) {
        PasswordRule rule = new PasswordRule();
        
        try {
            // 1. 从HTML input标签检测
            Element passwordInput = findPasswordInput(doc);
            if (passwordInput != null) {
                detectPasswordFromHtmlAttributes(passwordInput, rule);
            }
            
            // 2. 从JS代码检测
            if (jsCode != null && !jsCode.isEmpty()) {
                detectPasswordFromJavaScript(jsCode, rule);
            }
            
            log.info("检测到密码规则: 长度[{}-{}], 小写:{}, 大写:{}, 数字:{}, 特殊字符:{}", 
                    rule.getMinLength(), rule.getMaxLength(),
                    rule.getRequireLowerCase(), rule.getRequireUpperCase(),
                    rule.getRequireDigit(), rule.getRequireSpecialChar());
            
        } catch (Exception e) {
            log.warn("检测密码规则时出错: {}", e.getMessage());
        }
        
        return rule;
    }

    /**
     * 根据规则生成用户名
     */
    public static String generateUsername(UsernameRule rule) {
        Random random = new Random();
        
        // 确定生成长度（倾向于中间值）
        int minLen = rule.getMinLength() != null ? rule.getMinLength() : 6;
        int maxLen = rule.getMaxLength() != null ? rule.getMaxLength() : 16;
        int targetLen = minLen + (maxLen - minLen) / 2; // 取中间值
        if (targetLen < minLen) targetLen = minLen;
        if (targetLen > maxLen) targetLen = maxLen;
        
        // 构建字符池
        String letters = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        StringBuilder charPool = new StringBuilder();
        
        if (rule.getAllowLetter() == null || rule.getAllowLetter()) {
            charPool.append(letters);
        }
        if (rule.getAllowDigit() == null || rule.getAllowDigit()) {
            charPool.append(digits);
        }
        if (Boolean.TRUE.equals(rule.getAllowUnderscore())) {
            charPool.append("_");
        }
        
        if (charPool.length() == 0) {
            charPool.append(letters).append(digits); // 默认字母+数字
        }
        
        StringBuilder username = new StringBuilder();
        
        // 第一位字符
        if (rule.getMustStartWithLetter() == null || rule.getMustStartWithLetter()) {
            username.append(letters.charAt(random.nextInt(letters.length())));
            targetLen = Math.max(targetLen, 2); // 至少2位
        } else {
            username.append(charPool.charAt(random.nextInt(charPool.length())));
        }
        
        // 填充剩余字符
        while (username.length() < targetLen) {
            char nextChar = charPool.charAt(random.nextInt(charPool.length()));
            // 避免连续相同字符
            if (username.length() == 0 || username.charAt(username.length() - 1) != nextChar) {
                username.append(nextChar);
            }
        }
        
        String result = username.toString();
        log.debug("生成用户名: {} (长度:{})", result, result.length());
        return result;
    }

    /**
     * 根据规则生成密码
     */
    public static String generatePassword(PasswordRule rule) {
        Random random = new Random();
        
        // 确定生成长度
        int minLen = rule.getMinLength() != null ? rule.getMinLength() : 8;
        int maxLen = rule.getMaxLength() != null ? rule.getMaxLength() : 20;
        int targetLen = minLen + (maxLen - minLen) / 2; // 取中间值
        if (targetLen < minLen) targetLen = minLen;
        if (targetLen > maxLen) targetLen = maxLen;
        
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digits = "0123456789";
        String special = rule.getAllowedSpecialChars() != null ? 
                rule.getAllowedSpecialChars() : "@#$%^&*";
        
        StringBuilder password = new StringBuilder();
        StringBuilder charPool = new StringBuilder();
        
        // 确保满足必需要求
        if (Boolean.TRUE.equals(rule.getRequireLowerCase())) {
            password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
            charPool.append(lowerCase);
        }
        if (Boolean.TRUE.equals(rule.getRequireUpperCase())) {
            password.append(upperCase.charAt(random.nextInt(upperCase.length())));
            charPool.append(upperCase);
        }
        if (Boolean.TRUE.equals(rule.getRequireDigit())) {
            password.append(digits.charAt(random.nextInt(digits.length())));
            charPool.append(digits);
        }
        if (Boolean.TRUE.equals(rule.getRequireSpecialChar())) {
            password.append(special.charAt(random.nextInt(special.length())));
            charPool.append(special);
        }
        
        // 如果没有任何要求，使用默认组合（字母+数字）
        if (charPool.length() == 0) {
            charPool.append(lowerCase).append(upperCase).append(digits);
            // 默认至少包含一个大写、小写、数字
            password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
            password.append(upperCase.charAt(random.nextInt(upperCase.length())));
            password.append(digits.charAt(random.nextInt(digits.length())));
        }
        
        // 填充到目标长度
        while (password.length() < targetLen) {
            password.append(charPool.charAt(random.nextInt(charPool.length())));
        }
        
        // 打乱顺序
        String result = shuffleString(password.toString(), random);
        log.debug("生成密码: {} (长度:{})", result, result.length());
        return result;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 查找用户名输入框
     */
    private static Element findUsernameInput(Document doc) {
        // 优先级查找
        String[] selectors = {
            "input[name*=username i]",
            "input[name*=user i]",
            "input[name*=account i]",
            "input[id*=username i]",
            "input[id*=user i]",
            "input[placeholder*=用户名]",
            "input[placeholder*=账号]"
        };
        
        for (String selector : selectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    /**
     * 查找密码输入框
     */
    private static Element findPasswordInput(Document doc) {
        String[] selectors = {
            "input[type=password]",
            "input[name*=password i]",
            "input[name*=pwd i]",
            "input[id*=password i]",
            "input[placeholder*=密码]"
        };
        
        for (String selector : selectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    /**
     * 从HTML属性检测用户名规则
     */
    private static void detectFromHtmlAttributes(Element input, UsernameRule rule) {
        // minlength/maxlength 属性
        String minLenStr = input.attr("minlength");
        String maxLenStr = input.attr("maxlength");
        if (!minLenStr.isEmpty()) {
            try {
                rule.setMinLength(Integer.parseInt(minLenStr));
            } catch (NumberFormatException ignored) {}
        }
        if (!maxLenStr.isEmpty()) {
            try {
                rule.setMaxLength(Integer.parseInt(maxLenStr));
            } catch (NumberFormatException ignored) {}
        }
        
        // pattern 属性
        String pattern = input.attr("pattern");
        if (!pattern.isEmpty()) {
            rule.setPattern(pattern);
            analyzeUsernamePattern(pattern, rule);
        }
    }

    /**
     * 从HTML属性检测密码规则
     */
    private static void detectPasswordFromHtmlAttributes(Element input, PasswordRule rule) {
        String minLenStr = input.attr("minlength");
        String maxLenStr = input.attr("maxlength");
        if (!minLenStr.isEmpty()) {
            try {
                rule.setMinLength(Integer.parseInt(minLenStr));
            } catch (NumberFormatException ignored) {}
        }
        if (!maxLenStr.isEmpty()) {
            try {
                rule.setMaxLength(Integer.parseInt(maxLenStr));
            } catch (NumberFormatException ignored) {}
        }
        
        String pattern = input.attr("pattern");
        if (!pattern.isEmpty()) {
            rule.setPattern(pattern);
            analyzePasswordPattern(pattern, rule);
        }
    }

    /**
     * 从JavaScript代码检测用户名规则
     */
    private static void detectFromJavaScript(String jsCode, String fieldName, UsernameRule rule) {
        // 查找长度验证：username.length >= 5, username.length <= 12
        Pattern lengthPattern = Pattern.compile(
            fieldName + "\\.length\\s*([><=!]+)\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = lengthPattern.matcher(jsCode);
        while (matcher.find()) {
            String operator = matcher.group(1);
            int value = Integer.parseInt(matcher.group(2));
            
            if (operator.contains(">")) {
                rule.setMinLength(Math.max(rule.getMinLength(), value));
            } else if (operator.contains("<")) {
                rule.setMaxLength(Math.min(rule.getMaxLength(), value));
            }
        }
        
        // 查找正则验证：/^[a-zA-Z][a-zA-Z0-9_]{4,11}$/
        Pattern regexPattern = Pattern.compile(
            "/\\^?\\[?([a-zA-Z0-9_\\-\\\\]+)\\]?.*?\\{(\\d+),(\\d+)\\}.*?\\$/",
            Pattern.CASE_INSENSITIVE
        );
        matcher = regexPattern.matcher(jsCode);
        if (matcher.find()) {
            String chars = matcher.group(1);
            int min = Integer.parseInt(matcher.group(2));
            int max = Integer.parseInt(matcher.group(3));
            
            rule.setMinLength(min);
            rule.setMaxLength(max);
            
            if (chars.contains("_")) {
                rule.setAllowUnderscore(true);
            }
        }
    }

    /**
     * 从JavaScript代码检测密码规则
     */
    private static void detectPasswordFromJavaScript(String jsCode, PasswordRule rule) {
        // 查找长度要求
        Pattern lengthPattern = Pattern.compile(
            "(password|pwd)\\.length\\s*([><=!]+)\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = lengthPattern.matcher(jsCode);
        while (matcher.find()) {
            String operator = matcher.group(2);
            int value = Integer.parseInt(matcher.group(3));
            
            if (operator.contains(">")) {
                rule.setMinLength(Math.max(rule.getMinLength(), value));
            } else if (operator.contains("<")) {
                rule.setMaxLength(Math.min(rule.getMaxLength(), value));
            }
        }
        
        // 查找必需字符要求
        if (jsCode.contains("(?=.*[a-z])") || jsCode.contains("小写")) {
            rule.setRequireLowerCase(true);
        }
        if (jsCode.contains("(?=.*[A-Z])") || jsCode.contains("大写")) {
            rule.setRequireUpperCase(true);
        }
        if (jsCode.contains("(?=.*\\d)") || jsCode.contains("(?=.*[0-9])") || jsCode.contains("数字")) {
            rule.setRequireDigit(true);
        }
        if (jsCode.contains("特殊字符") || jsCode.contains("special")) {
            rule.setRequireSpecialChar(true);
        }
    }

    /**
     * 分析用户名正则表达式
     */
    private static void analyzeUsernamePattern(String pattern, UsernameRule rule) {
        // 分析长度：{5,12}
        Pattern lenPattern = Pattern.compile("\\{(\\d+),(\\d+)\\}");
        Matcher matcher = lenPattern.matcher(pattern);
        if (matcher.find()) {
            rule.setMinLength(Integer.parseInt(matcher.group(1)));
            rule.setMaxLength(Integer.parseInt(matcher.group(2)));
        }
        
        // 分析必须字母开头：^[a-zA-Z]
        if (pattern.matches(".*\\^\\[?[a-zA-Z].*")) {
            rule.setMustStartWithLetter(true);
        }
        
        // 分析允许的字符
        if (pattern.contains("_") || pattern.contains("\\w")) {
            rule.setAllowUnderscore(true);
        }
        if (pattern.contains("[a-z]") || pattern.contains("[A-Z]")) {
            rule.setAllowLetter(true);
        }
        if (pattern.contains("\\d") || pattern.contains("[0-9]")) {
            rule.setAllowDigit(true);
        }
    }

    /**
     * 分析密码正则表达式
     */
    private static void analyzePasswordPattern(String pattern, PasswordRule rule) {
        // 分析长度
        Pattern lenPattern = Pattern.compile("\\{(\\d+),(\\d+)\\}");
        Matcher matcher = lenPattern.matcher(pattern);
        if (matcher.find()) {
            rule.setMinLength(Integer.parseInt(matcher.group(1)));
            rule.setMaxLength(Integer.parseInt(matcher.group(2)));
        }
        
        // 分析必需字符
        if (pattern.contains("(?=.*[a-z])")) {
            rule.setRequireLowerCase(true);
        }
        if (pattern.contains("(?=.*[A-Z])")) {
            rule.setRequireUpperCase(true);
        }
        if (pattern.contains("(?=.*\\d)") || pattern.contains("(?=.*[0-9])")) {
            rule.setRequireDigit(true);
        }
        if (pattern.contains("[!@#$%^&*]") || pattern.contains("\\W")) {
            rule.setRequireSpecialChar(true);
        }
    }

    /**
     * 打乱字符串顺序
     */
    private static String shuffleString(String input, Random random) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
