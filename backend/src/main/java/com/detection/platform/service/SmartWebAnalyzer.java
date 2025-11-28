/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.detection.platform.service.SmartWebAnalyzer$JsAnalysisResult
 *  com.detection.platform.service.SmartWebAnalyzer$RegisterTestResult
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  okhttp3.CookieJar
 *  okhttp3.Headers
 *  okhttp3.HttpUrl
 *  okhttp3.MediaType
 *  okhttp3.OkHttpClient
 *  okhttp3.OkHttpClient$Builder
 *  okhttp3.Request
 *  okhttp3.Request$Builder
 *  okhttp3.RequestBody
 *  okhttp3.Response
 *  org.jsoup.Jsoup
 *  org.jsoup.nodes.Document
 *  org.jsoup.nodes.Element
 *  org.jsoup.select.Elements
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 */
package com.detection.platform.service;

import com.detection.platform.service.SmartWebAnalyzer;
import com.detection.platform.utils.UsernamePasswordRuleDetector;
import com.detection.platform.utils.UsernamePasswordRuleDetector.UsernameRule;
import com.detection.platform.utils.UsernamePasswordRuleDetector.PasswordRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page.WaitForLoadStateOptions;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.Locator.ClickOptions;

@Service
public class SmartWebAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(SmartWebAnalyzer.class);
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    // 存储当前分析的规则
    private UsernameRule currentUsernameRule = null;
    private PasswordRule currentPasswordRule = null;

    public Map<String, Object> analyzeWebsite(String websiteUrl) {
        log.info("==================== \u5f00\u59cb\u667a\u80fd\u5206\u6790\u7f51\u7ad9 ====================");
        log.info("\u76ee\u6807URL: {}", (Object)websiteUrl);
        HashMap<String, Object> result = new HashMap<String, Object>();
        try {
            log.info("[\u6b65\u9aa41] \u6293\u53d6\u6ce8\u518c\u9875\u9762...");
            Document doc = this.fetchWebsite(websiteUrl);
            String registerPageUrl = this.findRegisterPage(doc, websiteUrl);
            if (registerPageUrl != null && !registerPageUrl.equals(websiteUrl)) {
                log.info("\u627e\u5230\u6ce8\u518c\u9875\u9762: {}", (Object)registerPageUrl);
                doc = this.fetchWebsite(registerPageUrl);
                result.put("registerPageUrl", registerPageUrl);
            } else {
                result.put("registerPageUrl", websiteUrl);
            }
            log.info("[\u6b65\u9aa42] \u4e0b\u8f7dJS\u6587\u4ef6...");
            List<String> jsUrls = this.extractJsFiles(doc, websiteUrl);
            Map<String, String> jsContents = this.downloadJsFiles(jsUrls);
            result.put("jsFiles", jsUrls);
            log.info("\u6210\u529f\u4e0b\u8f7d {} \u4e2aJS\u6587\u4ef6\uff0c\u603b\u5927\u5c0f: {} KB", (Object)jsContents.size(), (Object)(jsContents.values().stream().mapToInt(String::length).sum() / 1024));
            log.info("[\u6b65\u9aa43] \u5206\u6790JS\u4ee3\u7801...");
            JsAnalysisResult jsAnalysis = this.analyzeJsCode(jsContents, doc, websiteUrl);
            result.put("registerApi", jsAnalysis.getRegisterApi());
            result.put("method", jsAnalysis.getMethod());
            result.put("requiredFields", jsAnalysis.getRequiredParams());
            result.put("encryptionType", jsAnalysis.getEncryptionType());
            result.put("encryptionDetails", jsAnalysis.getEncryptionDetails());
            result.put("requiredHeaders", jsAnalysis.getRequiredHeaders());
            result.put("csrfTokenField", jsAnalysis.getCsrfTokenField());
            result.put("logicDetails", jsAnalysis.getLogicDetails());
            result.put("encryptionHeader", jsAnalysis.getEncryptionHeader());
            result.put("valueFieldName", jsAnalysis.getValueFieldName());
            result.put("usernameField", jsAnalysis.getUsernameField());
            result.put("passwordField", jsAnalysis.getPasswordField());
            result.put("emailField", jsAnalysis.getEmailField());
            result.put("phoneField", jsAnalysis.getPhoneField());
            result.put("requiredHeaders", jsAnalysis.getRequiredHeaders());
            result.put("csrfTokenField", jsAnalysis.getCsrfTokenField());
            result.put("logicDetails", jsAnalysis.getLogicDetails());
            log.info("\u68c0\u6d4b\u5230\u6ce8\u518c\u63a5\u53e3: {} [{}]", (Object)jsAnalysis.getRegisterApi(), (Object)jsAnalysis.getMethod());
            log.info("\u68c0\u6d4b\u5230\u52a0\u5bc6\u65b9\u5f0f: {}", (Object)jsAnalysis.getEncryptionType());
            log.info("\u68c0\u6d4b\u5230\u5fc5\u9700\u53c2\u6570: {}", (Object)jsAnalysis.getRequiredParams());
            log.info("[\u6b65\u9aa44] \u6a21\u62df\u6ce8\u518c\u6d4b\u8bd5...");
            log.info("[\u5206\u6790\u7ed3\u679c] \u6ce8\u518c\u63a5\u53e3: {}", (Object)jsAnalysis.getRegisterApi());
            log.info("[\u5206\u6790\u7ed3\u679c] HTTP\u65b9\u6cd5: {}", (Object)jsAnalysis.getMethod());
            log.info("[\u5206\u6790\u7ed3\u679c] \u52a0\u5bc6\u7c7b\u578b: {}", (Object)jsAnalysis.getEncryptionType());
            log.info("[\u5206\u6790\u7ed3\u679c] RSA\u5bc6\u94a5\u63a5\u53e3: {}", (Object)jsAnalysis.getRsaKeyApi());
            log.info("[\u5206\u6790\u7ed3\u679c] \u52a0\u5bc6\u8bf7\u6c42\u5934\u540d: {}", (Object)jsAnalysis.getEncryptionHeader());
            log.info("[\u5206\u6790\u7ed3\u679c] \u6570\u636e\u5305\u88c5\u5b57\u6bb5\u540d: {}", (Object)jsAnalysis.getValueFieldName());
            RegisterTestResult testResult = this.simulateRegister(websiteUrl, jsAnalysis.getRegisterApi(), jsAnalysis.getMethod(), jsAnalysis.getRequiredParams(), jsAnalysis.getEncryptionType(), jsAnalysis.getRsaKeyApi(), jsAnalysis.getEncryptionHeader(), jsAnalysis.getValueFieldName());
            result.put("testSuccess", testResult.isSuccess());
            result.put("testMessage", testResult.getMessage());
            result.put("responseBody", testResult.getResponseBody());
            result.put("responseHeaders", testResult.getResponseHeaders());
            result.put("token", testResult.getToken());
            result.put("statusCode", testResult.getStatusCode());
            if (testResult.isSuccess() && testResult.getToken() != null) {
                log.info("\u2705 \u6ce8\u518c\u6d4b\u8bd5\u6210\u529f\uff01\u83b7\u53d6\u5230Token: {}...", (Object)testResult.getToken().substring(0, Math.min(20, testResult.getToken().length())));
                result.put("success", true);
                result.put("message", "\u5206\u6790\u6210\u529f\uff01\u5df2\u9a8c\u8bc1\u6ce8\u518c\u6d41\u7a0b\u5e76\u83b7\u53d6Token");
            } else {
                log.warn("\u26a0\ufe0f  \u6ce8\u518c\u6d4b\u8bd5\u672a\u6210\u529f\u83b7\u53d6Token\uff0c\u4f46\u5df2\u5b8c\u6210\u5206\u6790");
                log.info("\u2705 \u5df2\u6210\u529f\u8bc6\u522b\uff1a\u63a5\u53e3={}, \u52a0\u5bc6={}, \u53c2\u6570={}", new Object[]{jsAnalysis.getRegisterApi(), jsAnalysis.getEncryptionType(), jsAnalysis.getRequiredParams()});
                result.put("success", true);
                result.put("message", "\u5206\u6790\u5b8c\u6210\uff01\u5df2\u8bc6\u522b\u6ce8\u518c\u903b\u8f91\uff08\u63a5\u53e3\u3001\u52a0\u5bc6\u3001\u53c2\u6570\uff09\uff0c\u4f46\u6d4b\u8bd5\u672a\u83b7\u53d6Token: " + testResult.getMessage());
            }
            result.put("analysisReport", this.generateReport(jsAnalysis, testResult));
        }
        catch (Exception e) {
            log.error("\u274c \u7f51\u7ad9\u5206\u6790\u5931\u8d25", (Throwable)e);
            result.put("success", false);
            result.put("message", "\u5206\u6790\u5931\u8d25: " + e.getMessage());
        }
        log.info("==================== \u5206\u6790\u5b8c\u6210 ====================");
        return result;
    }

    private Document fetchWebsite(String url) throws IOException {
        Request request = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36").header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8").header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8").build();
        try (Response response = this.httpClient.newCall(request).execute();){
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            Document document = Jsoup.parse((String)response.body().string(), (String)url);
            return document;
        }
    }

    private String findRegisterPage(Document doc, String baseUrl) {
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String href = link.attr("abs:href");
            String text = link.text().toLowerCase();
            if (text.contains("\u6ce8\u518c") || text.contains("\u514d\u8d39\u6ce8\u518c") || text.contains("\u7acb\u5373\u6ce8\u518c")) {
                return href;
            }
            if (text.contains("register") || text.contains("sign up") || text.contains("signup") || text.contains("join")) {
                return href;
            }
            String lowerHref = href.toLowerCase();
            if (!lowerHref.contains("/register") && !lowerHref.contains("/signup") && !lowerHref.contains("/join") && !lowerHref.contains("/reg")) continue;
            return href;
        }
        return null;
    }

    private List<String> extractJsFiles(Document doc, String baseUrl) {
        ArrayList<String> jsFiles = new ArrayList<String>();
        Elements scripts = doc.select("script[src]");
        for (Element script : scripts) {
            String src = script.attr("abs:src");
            if (src.isEmpty() || !src.endsWith(".js") && !src.contains(".js?")) continue;
            jsFiles.add(src);
        }
        log.info("\u627e\u5230 {} \u4e2aJS\u6587\u4ef6", (Object)jsFiles.size());
        return jsFiles;
    }

    private Map<String, String> downloadJsFiles(List<String> jsUrls) {
        HashMap<String, String> contents = new HashMap<String, String>();
        int count = 0;
        int limit = Math.min(jsUrls.size(), 20);
        for (String jsUrl : jsUrls) {
            if (count >= limit) break;
            try {
                Request request = new Request.Builder().url(jsUrl).header("User-Agent", "Mozilla/5.0").build();
                Response response = this.httpClient.newCall(request).execute();
                try {
                    if (!response.isSuccessful()) continue;
                    String content = response.body().string();
                    contents.put(jsUrl, content);
                    log.info("[{}/{}] \u4e0b\u8f7d: {} ({} KB)", new Object[]{++count, limit, jsUrl.substring(jsUrl.lastIndexOf(47) + 1), content.length() / 1024});
                }
                finally {
                    if (response == null) continue;
                    response.close();
                }
            }
            catch (Exception e) {
                log.warn("\u4e0b\u8f7d\u5931\u8d25: {}", (Object)jsUrl);
            }
        }
        return contents;
    }

    private JsAnalysisResult analyzeJsCode(Map<String, String> jsContents, Document doc, String websiteUrl) {
        String logic;
        String csrfField;
        List<String> headers;
        JsAnalysisResult result = new JsAnalysisResult();
        Elements forms = doc.select("form");
        for (Element form : forms) {
            Elements inputs = form.select("input");
            for (Element input : inputs) {
                String name = input.attr("name");
                if (name.isEmpty()) continue;
                result.getRequiredParams().add(name);
            }
        }
        String allJsCode = String.join((CharSequence)"\n", jsContents.values());
        String registerApi = null;
        try {
            // 使用无头浏览器尝试拦截真实请求
            Playwright __pw = Playwright.create();
            Browser __browser = __pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext __ctx = __browser.newContext();
            Page __page = __ctx.newPage();
            final String[] __apiHolder = new String[2];
            __page.onRequest(__req -> {
                try {
                    String __url = __req.url();
                    String __method = __req.method();
                    String __lower = __url != null ? __url.toLowerCase() : "";
                    if (__lower.contains("register") || __lower.contains("signup") || __lower.contains("/reg") || __lower.contains("/member")) {
                        __apiHolder[0] = __url;
                        __apiHolder[1] = __method;
                    }
                } catch (Throwable ignore) {}
            });
            __page.navigate(websiteUrl);
            __page.waitForLoadState(LoadState.NETWORKIDLE);
            // 多国语言注册按钮支持：中文/英文/孟加拉/印度/西班牙/葡萄牙/法语/俄语/阿拉伯/日语/韩语/泰语/越南语等
            Locator __regBtn = __page.locator(
                "a:has-text('注册'), button:has-text('注册'), " +
                "a:has-text('立即注册'), button:has-text('立即注册'), " +
                "a:has-text('免费注册'), button:has-text('免费注册'), " +
                "a:has-text('register'), button:has-text('register'), " +
                "a:has-text('Register'), button:has-text('Register'), " +
                "a:has-text('REGISTER'), button:has-text('REGISTER'), " +
                "a:has-text('sign up'), button:has-text('sign up'), " +
                "a:has-text('Sign Up'), button:has-text('Sign Up'), " +
                "a:has-text('signup'), button:has-text('signup'), " +
                "a:has-text('Sign up'), button:has-text('Sign up'), " +
                "a:has-text('নিবন্ধন'), button:has-text('নিবন্ধন'), " +  // 孟加拉语
                "a:has-text('निबंधन'), button:has-text('निबंधन'), " +  // 印地语
                "a:has-text('registrarse'), button:has-text('registrarse'), " +  // 西班牙语
                "a:has-text('Registrarse'), button:has-text('Registrarse'), " +
                "a:has-text('registro'), button:has-text('registro'), " +  // 葡萄牙语/西班牙语
                "a:has-text('Registro'), button:has-text('Registro'), " +
                "a:has-text('inscrire'), button:has-text('inscrire'), " +  // 法语
                "a:has-text(\"S'inscrire\"), button:has-text(\"S'inscrire\"), " +
                "a:has-text('регистрация'), button:has-text('регистрация'), " +  // 俄语
                "a:has-text('تسجيل'), button:has-text('تسجيل'), " +  // 阿拉伯语
                "a:has-text('登録'), button:has-text('登録'), " +  // 日语
                "a:has-text('新規登録'), button:has-text('新規登録'), " +
                "a:has-text('회원가입'), button:has-text('회원가입'), " +  // 韩语
                "a:has-text('ลงทะเบียน'), button:has-text('ลงทะเบียน'), " +  // 泰语
                "a:has-text('đăng ký'), button:has-text('đăng ký'), " +  // 越南语
                "a:has-text('Đăng ký'), button:has-text('Đăng ký')" +
                ""
            );
            if (__regBtn != null && __regBtn.count() > 0) {
                __regBtn.first().click(new ClickOptions().setTimeout(5000));
                __page.waitForLoadState(LoadState.NETWORKIDLE);
            }
            if (__apiHolder[0] == null) {
                Locator __submitBtn = __page.locator("button[type=submit], input[type=submit]");
                if (__submitBtn != null && __submitBtn.count() > 0) {
                    __submitBtn.first().click(new ClickOptions().setTimeout(3000));
                    __page.waitForLoadState(LoadState.NETWORKIDLE);
                }
            }
            __browser.close();
            __pw.close();
            if (__apiHolder[0] != null) {
                registerApi = __apiHolder[0];
                result.setMethod(__apiHolder[1] != null ? __apiHolder[1] : "POST");
                log.info("[Browser] 捕获注册接口: {} [{}]", registerApi, result.getMethod());
            }
        } catch (Exception e) {
            log.warn("[Browser] 浏览器拦截失败，使用JS分析: {}", e.getMessage());
        }
        if (registerApi == null || registerApi.isEmpty()) {
            registerApi = this.findRegisterApi(allJsCode);
        }
        result.setRegisterApi(registerApi);
        String detectedMethod = this.detectHttpMethod(allJsCode, registerApi, websiteUrl);
        result.setMethod(detectedMethod);
        log.info("\u68c0\u6d4b\u5230HTTP\u65b9\u6cd5: {}", (Object)detectedMethod);
        List<String> params = this.findRequestParams(allJsCode);
        if (!params.isEmpty()) {
            result.setRequiredParams(params);
        }
        if (!(headers = this.findRequiredHeaders(allJsCode)).isEmpty()) {
            result.setRequiredHeaders(headers);
            log.info("\u68c0\u6d4b\u5230\u53ef\u80fd\u9700\u8981\u7684\u8bf7\u6c42\u5934: {}", headers);
        }
        if ((csrfField = this.findCsrfTokenField(allJsCode)) != null && !csrfField.isEmpty()) {
            result.setCsrfTokenField(csrfField);
            log.info("\u68c0\u6d4b\u5230CSRF/Anti\u5b57\u6bb5: {}", (Object)csrfField);
        }
        if ((logic = this.analyzeLogicPatterns(allJsCode)) != null && !logic.isEmpty()) {
            result.setLogicDetails(logic);
            log.info("\u68c0\u6d4b\u5230\u901a\u7528\u903b\u8f91: {}", (Object)logic);
        }
        String encType = this.detectEncryptionType(allJsCode);
        result.setEncryptionType(encType);
        result.setEncryptionDetails(this.analyzeEncryptionDetails(allJsCode, encType));
        result.setRsaKeyApi(this.findRsaKeyApi(allJsCode));
        result.setEncryptionHeader(this.findEncryptionHeaderName(allJsCode));
        result.setValueFieldName(this.findValueFieldName(allJsCode));
        result.setUsernameField(this.chooseField(result.getRequiredParams(), new String[]{"username", "user", "account", "loginName"}, "username"));
        result.setPasswordField(this.chooseField(result.getRequiredParams(), new String[]{"password", "pwd", "pass", "loginPwd"}, "password"));
        result.setEmailField(this.chooseField(result.getRequiredParams(), new String[]{"email", "mail", "emailAddress"}, "email"));
        result.setPhoneField(this.chooseField(result.getRequiredParams(), new String[]{"phone", "mobile", "mobileNo", "mobileNum", "phoneNumber"}, "phone"));
        
        // 注释掉规则检测，改用硬编码的用户名生成逻辑
        // log.info("开始检测用户名和密码规则...");
        // this.currentUsernameRule = UsernamePasswordRuleDetector.detectUsernameRule(doc, allJsCode);
        // this.currentPasswordRule = UsernamePasswordRuleDetector.detectPasswordRule(doc, allJsCode);
        log.info("使用硬编码的用户名格式（8位：1字母+7数字）");
        
        return result;
    }

    private String findRegisterApi(String jsCode) {
        log.info("========== \u5f00\u59cb\u667a\u80fd\u8bc6\u522b\u6ce8\u518c\u63a5\u53e3 ==========");
        LinkedHashSet<String> candidateApis = new LinkedHashSet<String>();
        Pattern p1 = Pattern.compile("(?:axios\\.|fetch\\()\\s*\\(?\\s*['\"]([^'\"]*(?:register|signup|reg|member)[^'\"]*)['\"]");
        Matcher m1 = p1.matcher(jsCode);
        while (m1.find()) {
            String api2 = m1.group(1);
            if (!api2.contains("/")) continue;
            candidateApis.add(api2);
            log.info("\u2705 [\u6a21\u5f0f1-axios/fetch] \u627e\u5230: {}", (Object)api2);
        }
        Pattern p2 = Pattern.compile("url\\s*[:=]\\s*['\"]([^'\"]*(?:register|signup|reg|member)[^'\"]*)['\"]");
        Matcher m2 = p2.matcher(jsCode);
        while (m2.find()) {
            String api3 = m2.group(1);
            if (!api3.contains("/")) continue;
            candidateApis.add(api3);
            log.info("\u2705 [\u6a21\u5f0f2-url\u5c5e\u6027] \u627e\u5230: {}", (Object)api3);
        }
        Pattern p3 = Pattern.compile("['\"](\\/[a-zA-Z0-9_\\/-]*(?:register|signup|reg|member)[a-zA-Z0-9_\\/-]*)['\"]");
        Matcher m3 = p3.matcher(jsCode);
        while (m3.find()) {
            String api4 = m3.group(1);
            if (api4.endsWith(".js") || api4.endsWith(".css") || api4.endsWith(".png") || api4.endsWith(".jpg") || api4.length() <= 3 || api4.split("/").length < 2) continue;
            candidateApis.add(api4);
            log.info("\u2705 [\u6a21\u5f0f3-\u5b8c\u6574\u8def\u5f84] \u627e\u5230: {}", (Object)api4);
        }
        Pattern p4 = Pattern.compile("(?:register|signup)Url\\s*[:=]\\s*['\"]([^'\"]+)['\"]");
        Matcher m4 = p4.matcher(jsCode);
        while (m4.find()) {
            candidateApis.add(m4.group(1));
            log.info("\u2705 [\u6a21\u5f0f4-URL\u53d8\u91cf] \u627e\u5230: {}", (Object)m4.group(1));
        }
        log.info("\u603b\u8ba1\u53d1\u73b0 {} \u4e2a\u5019\u9009\u63a5\u53e3", (Object)candidateApis.size());
        if (candidateApis.isEmpty()) {
            log.warn("\u26a0\ufe0f \u672a\u627e\u5230\u4efb\u4f55\u6ce8\u518c\u63a5\u53e3\uff0c\u8fd4\u56de\u7a7a\u503c");
            log.info("=========================================\n");
            return "";
        }
        String selected = candidateApis.stream().filter(api -> api.contains("/")).sorted((a, b) -> {
            int scoreA = this.calculateApiScore((String)a);
            int scoreB = this.calculateApiScore((String)b);
            return Integer.compare(scoreB, scoreA);
        }).findFirst().orElse("");
        log.info("\ud83c\udfaf \u667a\u80fd\u9009\u62e9\u7684\u6700\u4f73\u63a5\u53e3: {}", (Object)selected);
        if (!selected.isEmpty()) {
            log.info("   \u7406\u7531: \u8def\u5f84\u6700\u5177\u4f53\u4e14\u5305\u542b\u5173\u952e\u8bcd");
        }
        log.info("=========================================\n");
        return selected;
    }

    private int calculateApiScore(String api) {
        int depth;
        int score = 0;
        String lowerApi = api.toLowerCase();
        if (lowerApi.contains("member")) {
            score += 15;
        }
        if (lowerApi.contains("user")) {
            score += 12;
        }
        if (lowerApi.contains("register")) {
            score += 10;
        }
        if ((depth = api.split("/").length) == 3) {
            score += 8;
        } else if (depth == 4) {
            score += 6;
        } else if (depth == 2) {
            score += 4;
        } else if (depth >= 5) {
            score -= 5;
        }
        if (lowerApi.endsWith("/mobile")) {
            score -= 3;
        }
        if (lowerApi.endsWith("/email")) {
            score -= 3;
        }
        if (lowerApi.endsWith("/phone")) {
            score -= 3;
        }
        if (lowerApi.endsWith("/sms")) {
            score -= 3;
        }
        if (lowerApi.endsWith("/verification")) {
            score -= 3;
        }
        if (!lowerApi.matches(".*/(?:mobile|email|phone|wechat|qq|auto).*")) {
            score += 5;
        }
        log.debug("API\u8def\u5f84\u8bc4\u5206: {} = {}", (Object)api, (Object)score);
        return score;
    }

    private String detectHttpMethod(String jsCode, String registerApi, String websiteUrl) {
        String[] methods;
        log.info("========== \u5f00\u59cb\u8bc6\u522bHTTP\u65b9\u6cd5 ==========");
        log.info("\u76ee\u6807\u63a5\u53e3: {}", (Object)registerApi);
        if (registerApi == null || registerApi.isEmpty()) {
            log.warn("\u26a0\ufe0f \u6ce8\u518c\u63a5\u53e3\u4e3a\u7a7a\uff0c\u9ed8\u8ba4\u8fd4\u56dePOST");
            return "POST";
        }
        String methodFromOptions = this.detectMethodByOptions(registerApi, websiteUrl);
        if (methodFromOptions != null && !methodFromOptions.isEmpty() && !"POST".equals(methodFromOptions)) {
            log.info("\u2705 [\u7b56\u75651-OPTIONS] \u901a\u8fc7OPTIONS\u8bf7\u6c42\u68c0\u6d4b\u5230: {}", (Object)methodFromOptions);
            log.info("=========================================\n");
            return methodFromOptions;
        }
        String escapedApi = Pattern.quote(registerApi);
        for (String method : methods = new String[]{"put", "post", "patch", "delete", "get"}) {
            Pattern p1 = Pattern.compile("axios\\." + method + "\\s*\\(\\s*['\"]" + escapedApi + "['\"]", 2);
            if (!p1.matcher(jsCode).find()) continue;
            log.info("\u2705 [\u6a21\u5f0f1-axios.method] \u5339\u914d: axios.{}('{}', ...)", (Object)method, (Object)registerApi);
            return method.toUpperCase();
        }
        Pattern p2 = Pattern.compile("fetch\\s*\\(\\s*['\"]" + escapedApi + "['\"]\\s*,\\s*\\{[^}]*method\\s*:\\s*['\"](GET|POST|PUT|PATCH|DELETE)['\"]", 2);
        Matcher m2 = p2.matcher(jsCode);
        if (m2.find()) {
            String method = m2.group(1).toUpperCase();
            log.info("\u2705 [\u6a21\u5f0f2-fetch] \u5339\u914d: fetch('{}', {{method: '{}'}})", (Object)registerApi, (Object)method);
            return method;
        }
        Pattern p3 = Pattern.compile("\\$\\.ajax\\s*\\(\\s*\\{[^}]*url\\s*:\\s*['\"]" + escapedApi + "['\"][^}]*?(?:type|method)\\s*:\\s*['\"](GET|POST|PUT|PATCH|DELETE)['\"]", 2);
        Matcher m3 = p3.matcher(jsCode);
        if (m3.find()) {
            String method = m3.group(1).toUpperCase();
            log.info("\u2705 [\u6a21\u5f0f3-$.ajax] \u5339\u914d: $.ajax({{url: '{}', method: '{}'}})", (Object)registerApi, (Object)method);
            return method;
        }
        int apiIndex = jsCode.indexOf(registerApi);
        if (apiIndex > 0) {
            int start = Math.max(0, apiIndex - 800);
            int end = Math.min(jsCode.length(), apiIndex + registerApi.length() + 800);
            String contextWindow = jsCode.substring(start, end);
            String lowerWindow = contextWindow.toLowerCase();
            log.info("\ud83d\udd0d \u5728\u63a5\u53e3\u4e0a\u4e0b\u6587\u7a97\u53e3\u4e2d\u67e5\u627e HTTP \u65b9\u6cd5...");
            if (lowerWindow.contains("axios.put(") || lowerWindow.matches(".*method\\s*:\\s*['\"]put['\"].*") || lowerWindow.matches(".*type\\s*:\\s*['\"]put['\"].*") || contextWindow.matches(".*method\\s*:\\s*['\"]PUT['\"].*") || contextWindow.matches(".*type\\s*:\\s*['\"]PUT['\"].*")) {
                log.info("\u2705 [\u6a21\u5f0f4-\u4e0a\u4e0b\u6587] \u5728URL\u9644\u8fd1\u53d1\u73b0: PUT");
                return "PUT";
            }
            if (lowerWindow.contains("axios.post(") || lowerWindow.matches(".*method\\s*:\\s*['\"]post['\"].*") || lowerWindow.matches(".*type\\s*:\\s*['\"]post['\"].*") || contextWindow.matches(".*method\\s*:\\s*['\"]POST['\"].*") || contextWindow.matches(".*type\\s*:\\s*['\"]POST['\"].*")) {
                log.info("\u2705 [\u6a21\u5f0f4-\u4e0a\u4e0b\u6587] \u5728URL\u9644\u8fd1\u53d1\u73b0: POST");
                return "POST";
            }
            if (lowerWindow.contains("axios.patch(") || lowerWindow.matches(".*method\\s*:\\s*['\"]patch['\"].*") || contextWindow.matches(".*method\\s*:\\s*['\"]PATCH['\"].*")) {
                log.info("\u2705 [\u6a21\u5f0f4-\u4e0a\u4e0b\u6587] \u5728URL\u9644\u8fd1\u53d1\u73b0: PATCH");
                return "PATCH";
            }
        }
        log.warn("\u26a0\ufe0f \u672a\u80fd\u7cbe\u786e\u5339\u914d HTTP \u65b9\u6cd5\uff0c\u9ed8\u8ba4\u8fd4\u56de POST");
        log.info("=========================================\n");
        return "POST";
    }

    private String detectMethodByOptions(String registerApi, String websiteUrl) {
        try {
            String allowHeader;
            Object baseUrl;
            if (registerApi.startsWith("http")) {
                baseUrl = "";
            } else {
                try {
                    URL url = new URL(websiteUrl);
                    baseUrl = url.getProtocol() + "://" + url.getHost();
                    if (url.getPort() != -1 && url.getPort() != 80 && url.getPort() != 443) {
                        baseUrl = (String)baseUrl + ":" + url.getPort();
                    }
                }
                catch (Exception e) {
                    log.warn("\u26a0\ufe0f \u65e0\u6cd5\u89e3\u6790websiteUrl: {}, \u4f7f\u7528\u9ed8\u8ba4", (Object)websiteUrl);
                    baseUrl = "https://" + websiteUrl.replaceAll("^https?://", "").split("/")[0];
                }
            }
            String fullUrl = registerApi.startsWith("http") ? registerApi : (String)baseUrl + registerApi;
            log.info("\ud83d\udd0d \u53d1\u9001OPTIONS\u8bf7\u6c42\u5230: {}", (Object)fullUrl);
            Request optionsRequest = new Request.Builder().url(fullUrl).method("OPTIONS", null).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36").header("Origin", (String)baseUrl).build();
            Response response = this.httpClient.newCall(optionsRequest).execute();
            log.info("\ud83d\udce1 OPTIONS\u54cd\u5e94\u72b6\u6001: {}", (Object)response.code());
            String allowedMethods = response.header("Access-Control-Allow-Methods");
            if (allowedMethods != null) {
                log.info("\ud83d\udd0d Access-Control-Allow-Methods: {}", (Object)allowedMethods);
                if (allowedMethods.toUpperCase().contains("PUT")) {
                    log.info("\u2705 \u670d\u52a1\u5668\u652f\u6301 PUT \u65b9\u6cd5");
                    response.close();
                    return "PUT";
                }
                if (allowedMethods.toUpperCase().contains("PATCH")) {
                    log.info("\u2705 \u670d\u52a1\u5668\u652f\u6301 PATCH \u65b9\u6cd5");
                    response.close();
                    return "PATCH";
                }
                if (allowedMethods.toUpperCase().contains("POST")) {
                    log.info("\u2705 \u670d\u52a1\u5668\u652f\u6301 POST \u65b9\u6cd5");
                    response.close();
                    return "POST";
                }
            }
            if ((allowHeader = response.header("Allow")) != null) {
                log.info("\ud83d\udd0d Allow: {}", (Object)allowHeader);
                if (allowHeader.toUpperCase().contains("PUT")) {
                    log.info("\u2705 [Allow\u5934] \u670d\u52a1\u5668\u652f\u6301 PUT \u65b9\u6cd5");
                    response.close();
                    return "PUT";
                }
                if (allowHeader.toUpperCase().contains("PATCH")) {
                    log.info("\u2705 [Allow\u5934] \u670d\u52a1\u5668\u652f\u6301 PATCH \u65b9\u6cd5");
                    response.close();
                    return "PATCH";
                }
                if (allowHeader.toUpperCase().contains("POST")) {
                    log.info("\u2705 [Allow\u5934] \u670d\u52a1\u5668\u652f\u6301 POST \u65b9\u6cd5");
                    response.close();
                    return "POST";
                }
            }
            log.warn("\u26a0\ufe0f OPTIONS\u54cd\u5e94\u4e2d\u672a\u627e\u5230\u5141\u8bb8\u65b9\u6cd5\u7684\u54cd\u5e94\u5934");
            response.close();
        }
        catch (Exception e) {
            log.warn("\u26a0\ufe0f OPTIONS\u8bf7\u6c42\u5931\u8d25: {} - {}", (Object)e.getClass().getSimpleName(), (Object)e.getMessage());
        }
        return null;
    }

    private List<String> findRequestParams(String jsCode) {
        String[] commonFields;
        LinkedHashSet<String> params = new LinkedHashSet<String>();
        Pattern dataPattern = Pattern.compile("data\\s*:\\s*\\{([^}]+)\\}");
        Matcher dataMatcher = dataPattern.matcher(jsCode);
        while (dataMatcher.find()) {
            String dataBlock = dataMatcher.group(1);
            Pattern paramPattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*:");
            Matcher paramMatcher = paramPattern.matcher(dataBlock);
            while (paramMatcher.find()) {
                params.add(paramMatcher.group(1));
            }
        }
        for (String field : commonFields = new String[]{"username", "password", "email", "phone", "mobile", "captcha", "code", "verifyCode", "confirmPassword"}) {
            if (!jsCode.contains(field)) continue;
            params.add(field);
        }
        log.info("\u68c0\u6d4b\u5230\u53c2\u6570: {}", params);
        return new ArrayList<String>(params);
    }

    private String detectEncryptionType(String jsCode) {
        boolean hasBase64;
        boolean hasMD5;
        boolean hasPkcs7;
        boolean hasECB;
        boolean hasRandom;
        boolean hasReverse;
        boolean hasRSA;
        boolean hasAES;
        boolean hasDES;
        log.info("========== \u5f00\u59cb\u68c0\u6d4b\u52a0\u5bc6\u7c7b\u578b ==========");
        HashMap<String, Integer> features = new HashMap<String, Integer>();
        if (jsCode.contains("encryptedString") && jsCode.contains("RSAKeyPair")) {
            log.info("\u2705 \u68c0\u6d4b\u5230\u7279\u5f81: encryptedString + RSAKeyPair");
            log.info("   \u5224\u65ad: \u4f7f\u7528\u8001\u5f0fJS-RSA\u5e93");
            features.put("DES_RSA_OLD", 2);
        }
        boolean bl = hasDES = jsCode.contains("CryptoJS.DES") || jsCode.contains("DES.encrypt") || jsCode.contains("CryptoJS['DES']") || jsCode.contains("['DES']['encrypt']");
        if (hasDES) {
            log.info("\u2705 \u68c0\u6d4b\u5230\u7279\u5f81: CryptoJS.DES \u6216 DES.encrypt");
            features.put("DES", 1);
        }
        boolean bl2 = hasAES = jsCode.contains("CryptoJS.AES") || jsCode.contains("AES.encrypt") || jsCode.contains("CryptoJS['AES']") || jsCode.contains("['AES']['encrypt']");
        if (hasAES) {
            log.info("\u2705 \u68c0\u6d4b\u5230\u7279\u5f81: CryptoJS.AES \u6216 AES.encrypt");
            features.put("AES", 1);
        }
        boolean bl3 = hasRSA = jsCode.contains("JSEncrypt") || jsCode.contains("RSA") || jsCode.contains("setPublicKey") || jsCode.contains("encrypt(");
        if (hasRSA) {
            log.info("\u2705 \u68c0\u6d4b\u5230\u7279\u5f81: JSEncrypt \u6216 RSA \u6216 setPublicKey");
            features.put("RSA", 1);
        }
        boolean bl4 = hasReverse = jsCode.contains("reverse()") || jsCode.contains(".reverse") || jsCode.contains("['reverse']()");
        if (hasReverse) {
            log.info("\u2705 \u68c0\u6d4b\u5230\u7279\u5f81: reverse() - \u53ef\u80fd\u5b58\u5728\u5b57\u7b26\u4e32\u53cd\u8f6c");
            features.put("REVERSE", 1);
        }
        boolean bl5 = hasRandom = jsCode.contains("rndString") || jsCode.contains("randomString") || jsCode.contains("Math.random");
        if (hasRandom) {
            log.info("\u2705 \u68c0\u6d4b\u5230\u7279\u5f81: rndString \u6216 Math.random - \u53ef\u80fd\u751f\u6210\u968f\u673a\u5bc6\u94a5");
            features.put("RANDOM_KEY", 1);
        }
        boolean bl6 = hasECB = jsCode.contains("mode") && jsCode.contains("ECB");
        if (hasECB) {
            log.info("\u2705 \u68c0\u6d4b\u5230\u7279\u5f81: mode: ECB");
            features.put("ECB", 1);
        }
        boolean bl7 = hasPkcs7 = jsCode.contains("Pkcs7") || jsCode.contains("padding");
        if (hasPkcs7) {
            log.info("\u2705 \u68c0\u6d4b\u5230\u7279\u5f81: Pkcs7 padding");
            features.put("PKCS7", 1);
        }
        boolean bl8 = hasMD5 = jsCode.contains("CryptoJS.MD5") || jsCode.contains("md5(") || jsCode.contains("MD5(") || jsCode.contains("['MD5'](");
        if (hasMD5) {
            log.info("\u2705 \u68c0\u6d4b\u5230\u7279\u5f81: MD5");
            features.put("MD5", 1);
        }
        boolean bl9 = hasBase64 = jsCode.contains("btoa(") || jsCode.contains("CryptoJS.enc.Base64") || jsCode.contains("Base64.encode");
        if (hasBase64) {
            log.info("\u2705 \u68c0\u6d4b\u5230\u7279\u5f81: Base64");
            features.put("BASE64", 1);
        }
        log.info("========== \u7279\u5f81\u6c47\u603b ==========");
        log.info("\u68c0\u6d4b\u5230\u7684\u7279\u5f81: {}", features.keySet());
        String encType = "NONE";
        if (features.containsKey("DES_RSA_OLD")) {
            encType = "DES_RSA";
            log.info("\ud83c\udfaf \u6700\u7ec8\u5224\u65ad: DES_RSA (\u8001\u5f0fJS\u5e93)");
            if (hasReverse) {
                log.info("   \u9644\u52a0\u7279\u5f81: \u5305\u542b\u5b57\u7b26\u4e32\u53cd\u8f6c");
            }
        } else if (features.containsKey("DES") && features.containsKey("RSA")) {
            encType = "DES_RSA_STANDARD";
            log.info("\ud83c\udfaf \u6700\u7ec8\u5224\u65ad: DES_RSA_STANDARD (\u6807\u51c6PKCS1)");
        } else if (features.containsKey("AES") && features.containsKey("RSA")) {
            encType = "AES_RSA";
            log.info("\ud83c\udfaf \u6700\u7ec8\u5224\u65ad: AES_RSA");
        } else if (features.containsKey("MD5")) {
            encType = "MD5";
            log.info("\ud83c\udfaf \u6700\u7ec8\u5224\u65ad: MD5");
        } else if (features.containsKey("BASE64")) {
            encType = "BASE64";
            log.info("\ud83c\udfaf \u6700\u7ec8\u5224\u65ad: BASE64");
        } else if (features.containsKey("DES")) {
            encType = "DES";
            log.info("\ud83c\udfaf \u6700\u7ec8\u5224\u65ad: DES (\u4ec5DES)");
        } else if (features.containsKey("AES")) {
            encType = "AES";
            log.info("\ud83c\udfaf \u6700\u7ec8\u5224\u65ad: AES (\u4ec5AES)");
        } else {
            log.warn("\u26a0\ufe0f \u672a\u68c0\u6d4b\u5230\u660e\u663e\u7684\u52a0\u5bc6\u7279\u5f81");
        }
        log.info("========================================\n");
        return encType;
    }

    private String analyzeEncryptionDetails(String jsCode, String encType) {
        log.info("========== \u5f00\u59cb\u5206\u6790\u52a0\u5bc6\u7ec6\u8282 ==========");
        StringBuilder details = new StringBuilder();
        if (encType.contains("DES") || encType.contains("AES")) {
            Pattern pattern = Pattern.compile("encrypt\\(([^)]+)\\)");
            Matcher matcher = pattern.matcher(jsCode);
            if (matcher.find()) {
                String field = matcher.group(1);
                details.append("\u52a0\u5bc6\u5b57\u6bb5: ").append(field).append("; ");
                log.info("\u2705 \u627e\u5230\u52a0\u5bc6\u5b57\u6bb5: {}", (Object)field);
            }
            if (jsCode.contains("mode")) {
                if (jsCode.contains("ECB")) {
                    details.append("\u6a21\u5f0f: ECB; ");
                    log.info("\u2705 \u52a0\u5bc6\u6a21\u5f0f: ECB");
                } else if (jsCode.contains("CBC")) {
                    details.append("\u6a21\u5f0f: CBC; ");
                    log.info("\u2705 \u52a0\u5bc6\u6a21\u5f0f: CBC");
                }
            }
            if (jsCode.contains("Pkcs7")) {
                details.append("\u586b\u5145: Pkcs7; ");
                log.info("\u2705 \u586b\u5145\u65b9\u5f0f: Pkcs7");
            }
            if (jsCode.contains("reverse()") || jsCode.contains(".reverse")) {
                details.append("\u7279\u6b8a\u64cd\u4f5c: \u5bc6\u94a5\u53cd\u8f6c; ");
                log.info("\u2705 \u7279\u6b8a\u64cd\u4f5c: \u5bc6\u94a5\u53cd\u8f6c");
            }
        }
        if (encType.equals("MD5")) {
            if (jsCode.contains("salt") || jsCode.matches(".*password\\s*\\+\\s*['\"][^'\"]+['\"].*")) {
                details.append("\u4f7f\u7528\u52a0\u76d0MD5; ");
                log.info("\u2705 MD5\u52a0\u5bc6: \u4f7f\u7528\u52a0\u76d0");
            } else {
                log.info("\u2139\ufe0f MD5\u52a0\u5bc6: \u672a\u68c0\u6d4b\u5230\u52a0\u76d0");
            }
        }
        log.info("\u52a0\u5bc6\u7ec6\u8282\u6c47\u603b: {}", (Object)details.toString());
        log.info("========================================\n");
        return details.toString();
    }

    private String findRsaKeyApi(String jsCode) {
        log.info("========== \u5f00\u59cb\u67e5\u627eRSA\u5bc6\u94a5\u63a5\u53e3 ==========");
        LinkedHashSet<String> candidateApis = new LinkedHashSet<String>();
        Pattern p1 = Pattern.compile("['\"]([^'\"]*session[^'\"]*key[^'\"]*rsa[^'\"]*)['\"]");
        Matcher m1 = p1.matcher(jsCode);
        while (m1.find()) {
            String api = m1.group(1);
            if (!api.contains("/")) continue;
            candidateApis.add(api);
            log.info("\u2705 [\u6a21\u5f0f1-session/key/rsa] \u627e\u5230: {}", (Object)api);
        }
        Pattern p2 = Pattern.compile("['\"](\\/[a-zA-Z0-9_\\/-]*(?:rsa|publicKey|public-key|getKey|key\\/rsa)[a-zA-Z0-9_\\/-]*)['\"]");
        Matcher m2 = p2.matcher(jsCode);
        while (m2.find()) {
            String api = m2.group(1);
            if (api.endsWith(".js") || api.endsWith(".css") || api.split("/").length < 2) continue;
            candidateApis.add(api);
            log.info("\u2705 [\u6a21\u5f0f2-\u5b8c\u6574\u8def\u5f84] \u627e\u5230: {}", (Object)api);
        }
        Pattern p3 = Pattern.compile("(?:axios\\.|fetch\\(|\\$\\.get)\\s*\\(?\\s*['\"]([^'\"]*(?:rsa|key|publicKey)[^'\"]*)['\"]");
        Matcher m3 = p3.matcher(jsCode);
        while (m3.find()) {
            String api = m3.group(1);
            if (!api.contains("/")) continue;
            candidateApis.add(api);
            log.info("\u2705 [\u6a21\u5f0f3-axios/fetch] \u627e\u5230: {}", (Object)api);
        }
        Pattern p4 = Pattern.compile("(?:rsaKeyUrl|keyUrl|publicKeyUrl)\\s*[:=]\\s*['\"]([^'\"]+)['\"]");
        Matcher m4 = p4.matcher(jsCode);
        while (m4.find()) {
            candidateApis.add(m4.group(1));
            log.info("\u2705 [\u6a21\u5f0f4-URL\u53d8\u91cf] \u627e\u5230: {}", (Object)m4.group(1));
        }
        log.info("\u603b\u8ba1\u53d1\u73b0 {} \u4e2a\u5019\u9009RSA\u63a5\u53e3", (Object)candidateApis.size());
        if (candidateApis.isEmpty()) {
            log.warn("\u26a0\ufe0f \u672a\u627e\u5230RSA\u5bc6\u94a5\u63a5\u53e3\uff0c\u8fd4\u56denull");
            log.info("========================================\n");
            return null;
        }
        String selected = candidateApis.stream().sorted((a, b) -> {
            int scoreA = this.calculateRsaApiScore((String)a);
            int scoreB = this.calculateRsaApiScore((String)b);
            return Integer.compare(scoreB, scoreA);
        }).findFirst().orElse(null);
        log.info("\ud83c\udfaf \u667a\u80fd\u9009\u62e9\u7684RSA\u63a5\u53e3: {}", (Object)selected);
        log.info("========================================\n");
        return selected;
    }

    private int calculateRsaApiScore(String api) {
        int depth;
        int score = 0;
        String lowerApi = api.toLowerCase();
        if (lowerApi.contains("session") && lowerApi.contains("key") && lowerApi.contains("rsa")) {
            score += 30;
        }
        if (lowerApi.contains("publickey") || lowerApi.contains("public-key")) {
            score += 15;
        }
        if (lowerApi.contains("getkey")) {
            score += 12;
        }
        if (lowerApi.contains("rsa")) {
            score += 10;
        }
        if (lowerApi.contains("key")) {
            score += 5;
        }
        if ((depth = api.split("/").length) >= 3 && depth <= 5) {
            score += 8;
        } else if (depth > 5) {
            score -= 5;
        }
        log.debug("RSA API\u8bc4\u5206: {} = {}", (Object)api, (Object)score);
        return score;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private RegisterTestResult simulateRegister(String baseUrl, String registerApi, String method, List<String> params, String encType, String rsaKeyApi, String encryptionHeaderName, String valueFieldName) {
        RegisterTestResult result;
        block47: {
            result = new RegisterTestResult();
            try {
                String encryptionHeaderValue;
                String encryptedValue;
                LinkedHashMap<String, Object> bodyPlain;
                String fullUrl;
                block46: {
                    log.info("[TestRegister] ========== \u5f00\u59cb\u6a21\u62df\u6ce8\u518c\u6d4b\u8bd5 ==========");
                    log.info("[TestRegister] baseUrl: {}", (Object)baseUrl);
                    log.info("[TestRegister] registerApi: {}", (Object)registerApi);
                    log.info("[TestRegister] method: {}", (Object)method);
                    log.info("[TestRegister] encType: {}", (Object)encType);
                    log.info("[TestRegister] rsaKeyApi: {}", (Object)rsaKeyApi);
                    log.info("[TestRegister] encryptionHeaderName: {}", (Object)encryptionHeaderName);
                    log.info("[TestRegister] valueFieldName: {}", (Object)valueFieldName);
                    if (encType == null || encType.isEmpty()) {
                        encType = "NONE";
                        log.warn("[TestRegister] \u26a0\ufe0f \u52a0\u5bc6\u7c7b\u578b\u4e3a\u7a7a\uff0c\u9ed8\u8ba4\u4f7f\u7528NONE");
                    }
                    if (encryptionHeaderName == null || encryptionHeaderName.isEmpty()) {
                        encryptionHeaderName = "encryption";
                        log.info("[TestRegister] \u52a0\u5bc6\u8bf7\u6c42\u5934\u540d\u9ed8\u8ba4\u4e3a: encryption");
                    }
                    if (valueFieldName == null || valueFieldName.isEmpty()) {
                        valueFieldName = "value";
                        log.info("[TestRegister] \u6570\u636e\u5305\u88c5\u5b57\u6bb5\u540d\u9ed8\u8ba4\u4e3a: value");
                    }
                    fullUrl = registerApi.startsWith("http") ? registerApi : baseUrl + (String)(registerApi.startsWith("/") ? registerApi : "/" + registerApi);
                    log.info("[TestRegister] \u6ce8\u518c\u63a5\u53e3URL: {}", (Object)fullUrl);
                    log.info("[TestRegister] HTTP\u65b9\u6cd5: {}", (Object)method);
                    log.info("[TestRegister] \u52a0\u5bc6\u7c7b\u578b: {}", (Object)encType);
                    long timestamp = System.currentTimeMillis();
                    // 生成8位用户名：1位字母 + 7位数字（符合6-10位要求）
                    String[] letters = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
                    String testUsername = letters[(int)(timestamp % 26)] + String.format("%07d", timestamp % 10000000);
                    String testPassword = "Abc12345";
                    log.info("[TestRegister] 生成测试用户名: {} (长度: {}位)", testUsername, testUsername.length());
                    bodyPlain = new LinkedHashMap<String, Object>();
                    bodyPlain.put("username", testUsername);
                    bodyPlain.put("password", testPassword);
                    bodyPlain.put("confirmPassword", testPassword);
                    bodyPlain.put("payeeName", "");
                    bodyPlain.put("email", "");
                    bodyPlain.put("qqNum", "");
                    bodyPlain.put("mobileNum", "");
                    bodyPlain.put("captcha", "");
                    bodyPlain.put("verificationCode", "");
                    bodyPlain.put("affiliateCode", "www");
                    bodyPlain.put("paymentPassword", "");
                    bodyPlain.put("line", "");
                    bodyPlain.put("whatsapp", "");
                    bodyPlain.put("facebook", "");
                    bodyPlain.put("wechat", "");
                    bodyPlain.put("idNumber", "");
                    bodyPlain.put("nickname", "");
                    String domain = this.extractDomain(baseUrl);
                    bodyPlain.put("domain", domain);
                    bodyPlain.put("login", true);
                    bodyPlain.put("registerUrl", baseUrl + "/");
                    bodyPlain.put("registerMethod", "WEB");
                    bodyPlain.put("loginDeviceId", "test-device-" + timestamp);
                    log.info("[TestRegister] \u660e\u6587\u53c2\u6570: {}", bodyPlain);
                    encryptedValue = null;
                    encryptionHeaderValue = null;
                    log.info("[TestRegister] ========== \u5f00\u59cb\u52a0\u5bc6\u6d41\u7a0b ==========");
                    log.info("[TestRegister] \u68c0\u6d4b\u5230\u7684\u52a0\u5bc6\u7c7b\u578b: {}", (Object)encType);
                    if ("DES_RSA".equalsIgnoreCase(encType) || "DES_RSA_OLD".equalsIgnoreCase(encType)) {
                        log.info("[TestRegister] \u6267\u884c {} \u52a0\u5bc6\u6d41\u7a0b", (Object)encType);
                        String rnd = this.rndString();
                        String reversedRnd = new StringBuilder(rnd).reverse().toString();
                        log.info("[TestRegister] \u751f\u6210\u968f\u673arnd: {}", (Object)rnd);
                        log.info("[TestRegister] \u53cd\u8f6c\u540ernd: {}", (Object)reversedRnd);
                        String plaintextJson = this.objectMapper.writeValueAsString(bodyPlain).replace(" ", "");
                        log.info("[TestRegister] \u660e\u6587JSON\u957f\u5ea6: {}", (Object)plaintextJson.length());
                        log.info("[TestRegister] \u660e\u6587JSON\u524d200\u5b57\u7b26: {}", (Object)plaintextJson.substring(0, Math.min(200, plaintextJson.length())));
                        try {
                            encryptedValue = this.desEncryptEcb(plaintextJson, reversedRnd);
                            log.info("[TestRegister] \u2705 DES\u52a0\u5bc6\u6210\u529f");
                            log.info("[TestRegister]    - \u5bc6\u94a5: \u53cd\u8f6crnd\u7684\u524d8\u5b57\u8282");
                            log.info("[TestRegister]    - \u5bc6\u6587\u957f\u5ea6: {}", (Object)encryptedValue.length());
                            log.info("[TestRegister]    - \u5bc6\u6587\u524d80\u5b57\u7b26: {}", (Object)encryptedValue.substring(0, Math.min(80, encryptedValue.length())));
                        }
                        catch (Exception e) {
                            log.error("[TestRegister] \u274c DES\u52a0\u5bc6\u5931\u8d25", (Throwable)e);
                            result.setSuccess(false);
                            result.setMessage("DES\u52a0\u5bc6\u5931\u8d25: " + e.getMessage());
                            return result;
                        }
                        if (rsaKeyApi != null && !rsaKeyApi.isEmpty()) {
                            Object rsaUrl = baseUrl;
                            if (!((String)rsaUrl).endsWith("/")) {
                                rsaUrl = (String)rsaUrl + "/";
                            }
                            rsaUrl = rsaKeyApi.startsWith("/") ? (String)rsaUrl + rsaKeyApi.substring(1) : (String)rsaUrl + rsaKeyApi;
                            HttpUrl urlWithTs = HttpUrl.parse((String)rsaUrl).newBuilder().addQueryParameter("t", String.valueOf(System.currentTimeMillis())).build();
                            log.info("[TestRegister] \u83b7\u53d6RSA\u516c\u94a5: {}", (Object)urlWithTs);
                            Request rsaReq = new Request.Builder().url(urlWithTs).get().header("User-Agent", "Mozilla/5.0").header("Referer", baseUrl).build();
                            Response resp = this.httpClient.newCall(rsaReq).execute();
                            try {
                                log.info("[TestRegister] RSA\u63a5\u53e3\u54cd\u5e94\u7801: {}", (Object)resp.code());
                                if (resp.isSuccessful() && resp.body() != null) {
                                    String publicKeyStr = resp.body().string();
                                    log.info("[TestRegister] \u83b7\u53d6\u5230RSA\u516c\u94a5: {}", (Object)publicKeyStr);
                                    try {
                                        encryptionHeaderValue = this.rsaEncryptPkcs1(publicKeyStr, rnd);
                                        log.info("[TestRegister] \u2705 RSA\u52a0\u5bc6\u6210\u529f");
                                        log.info("[TestRegister]    - \u52a0\u5bc6\u5185\u5bb9: \u539f\u59cbrnd\uff08\u672a\u53cd\u8f6c\uff09");
                                        log.info("[TestRegister]    - \u5bc6\u6587\u957f\u5ea6: {}", (Object)encryptionHeaderValue.length());
                                        log.info("[TestRegister]    - \u5bc6\u6587\u524d120\u5b57\u7b26: {}", (Object)encryptionHeaderValue.substring(0, Math.min(120, encryptionHeaderValue.length())));
                                        break block46;
                                    }
                                    catch (Exception e) {
                                        log.error("[TestRegister] \u274c RSA\u52a0\u5bc6\u5931\u8d25", (Throwable)e);
                                        result.setSuccess(false);
                                        result.setMessage("RSA\u52a0\u5bc6\u5931\u8d25: " + e.getMessage());
                                        RegisterTestResult registerTestResult = result;
                                        if (resp == null) return registerTestResult;
                                        resp.close();
                                        return registerTestResult;
                                    }
                                }
                                log.error("[TestRegister] \u274c \u83b7\u53d6RSA\u516c\u94a5\u5931\u8d25: HTTP {}", (Object)resp.code());
                                result.setSuccess(false);
                                result.setMessage("\u83b7\u53d6RSA\u516c\u94a5\u5931\u8d25: HTTP " + resp.code());
                                RegisterTestResult publicKeyStr = result;
                                return publicKeyStr;
                            }
                            finally {
                                if (resp != null) {
                                    try {
                                        resp.close();
                                    }
                                    catch (Throwable e) {
                                        // 忽略关闭异常
                                    }
                                }
                            }
                        }
                        log.error("[TestRegister] \u274c \u672a\u68c0\u6d4b\u5230RSA\u5bc6\u94a5\u63a5\u53e3\uff01");
                        result.setSuccess(false);
                        result.setMessage("DES_RSA\u52a0\u5bc6\u9700\u8981RSA\u5bc6\u94a5\u63a5\u53e3\uff0c\u4f46\u672a\u68c0\u6d4b\u5230");
                        return result;
                    }
                    if ("NONE".equalsIgnoreCase(encType)) {
                        encryptedValue = this.objectMapper.writeValueAsString(bodyPlain);
                        log.info("[TestRegister] \u65e0\u52a0\u5bc6\uff0c\u76f4\u63a5\u4f7f\u7528\u660e\u6587");
                    } else {
                        log.warn("[TestRegister] \u26a0\ufe0f \u672a\u5b9e\u73b0\u7684\u52a0\u5bc6\u7c7b\u578b: {}\uff0c\u4f7f\u7528\u660e\u6587", (Object)encType);
                        encryptedValue = this.objectMapper.writeValueAsString(bodyPlain);
                    }
                }
                log.info("[TestRegister] ========== \u52a0\u5bc6\u6d41\u7a0b\u5b8c\u6210 ==========");
                Request.Builder reqBuilder = new Request.Builder().url(fullUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36").header("Accept", "application/json, text/plain, */*").header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8").header("Cache-Control", "no-cache").header("Content-Type", "application/json").header("Origin", baseUrl).header("Referer", baseUrl + "/");
                reqBuilder.header("device", "web");
                reqBuilder.header("language", "BN");
                reqBuilder.header("merchant", "ck555bdtf3");
                if (encryptionHeaderValue != null && !encryptionHeaderValue.isEmpty()) {
                    reqBuilder.header(encryptionHeaderName, encryptionHeaderValue);
                    log.info("[TestRegister] \u2705 \u6dfb\u52a0\u52a0\u5bc6\u8bf7\u6c42\u5934: {}={}", (Object)encryptionHeaderName, (Object)(encryptionHeaderValue.substring(0, Math.min(80, encryptionHeaderValue.length())) + "..."));
                } else if ("DES_RSA".equalsIgnoreCase(encType) || "DES_RSA_OLD".equalsIgnoreCase(encType)) {
                    log.error("[TestRegister] \u274c \u52a0\u5bc6\u7c7b\u578b\u4e3a{}\u4f46\u52a0\u5bc6\u8bf7\u6c42\u5934\u503c\u4e3a\u7a7a\uff01", (Object)encType);
                    result.setSuccess(false);
                    result.setMessage("\u52a0\u5bc6\u5931\u8d25\uff1a\u672a\u80fd\u751f\u6210\u52a0\u5bc6\u8bf7\u6c42\u5934");
                    return result;
                }
                LinkedHashMap<String, Object> requestBody = new LinkedHashMap<String, Object>();
                if ("DES_RSA".equalsIgnoreCase(encType) || "DES_RSA_OLD".equalsIgnoreCase(encType)) {
                    requestBody.put(valueFieldName, encryptedValue);
                    log.info("[TestRegister] \u8bf7\u6c42\u4f53\u683c\u5f0f: {{\"{}\":\"{}\"}}", (Object)valueFieldName, (Object)"\u5bc6\u6587");
                } else {
                    requestBody = bodyPlain;
                    log.info("[TestRegister] \u8bf7\u6c42\u4f53\u683c\u5f0f: \u660e\u6587\u53c2\u6570");
                }
                String bodyJson = this.objectMapper.writeValueAsString(requestBody);
                log.info("[TestRegister] \u8bf7\u6c42\u4f53\u957f\u5ea6: {}", (Object)bodyJson.length());
                RequestBody body = RequestBody.create((String)bodyJson, (MediaType)MediaType.parse((String)"application/json; charset=utf-8"));
                if ("PUT".equalsIgnoreCase(method)) {
                    reqBuilder.put(body);
                } else if ("POST".equalsIgnoreCase(method)) {
                    reqBuilder.post(body);
                } else if ("PATCH".equalsIgnoreCase(method)) {
                    reqBuilder.patch(body);
                } else {
                    reqBuilder.post(body);
                }
                log.info("[TestRegister] ========== \u53d1\u9001\u6ce8\u518c\u8bf7\u6c42 ==========");
                Request request = reqBuilder.build();
                try (Response response = this.httpClient.newCall(request).execute();){
                    String responseBody = response.body() != null ? response.body().string() : "";
                    int statusCode = response.code();
                    result.setStatusCode(statusCode);
                    result.setResponseBody(responseBody);
                    result.setResponseHeaders(response.headers().toMultimap().toString());
                    log.info("[TestRegister] \u54cd\u5e94\u72b6\u6001\u7801: {}", (Object)statusCode);
                    log.info("[TestRegister] \u54cd\u5e94\u4f53\u957f\u5ea6: {}", (Object)responseBody.length());
                    log.info("[TestRegister] \u54cd\u5e94\u4f53\u5185\u5bb9: {}", responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);
                    if (response.isSuccessful() && !responseBody.isEmpty()) {
                        try {
                            Map respMap = (Map)this.objectMapper.readValue(responseBody, Map.class);
                            Boolean respSuccess = (Boolean)respMap.get("success");
                            String errorCode = (String)respMap.get("errorCode");
                            log.info("[TestRegister] \u54cd\u5e94success\u5b57\u6bb5: {}", (Object)respSuccess);
                            log.info("[TestRegister] \u54cd\u5e94errorCode: {}", (Object)errorCode);
                            String token = this.extractTokenFromResponse(respMap, response.headers());
                            if (token != null) {
                                result.setToken(token);
                                result.setSuccess(true);
                                result.setMessage("\u2705 \u6d4b\u8bd5\u6210\u529f\u83b7\u53d6Token");
                                log.info("[TestRegister] \u2705 \u6210\u529f\u63d0\u53d6Token: {}", (Object)token);
                                break block47;
                            }
                            if ("exists.username.err".equals(errorCode)) {
                                result.setSuccess(true);
                                result.setMessage("\u2705 \u52a0\u5bc6\u9a8c\u8bc1\u6210\u529f\uff08\u7528\u6237\u540d\u5df2\u5b58\u5728\uff09");
                                log.info("[TestRegister] \u2705 \u7528\u6237\u540d\u91cd\u590d\uff0c\u8bf4\u660e\u52a0\u5bc6\u89e3\u5bc6\u903b\u8f91\u6b63\u786e\uff01");
                                break block47;
                            }
                            if ("decryption.err".equals(errorCode)) {
                                result.setSuccess(false);
                                result.setMessage("\u274c \u670d\u52a1\u5668\u89e3\u5bc6\u5931\u8d25\uff0c\u52a0\u5bc6\u903b\u8f91\u53ef\u80fd\u6709\u95ee\u9898");
                                log.warn("[TestRegister] \u274c \u670d\u52a1\u5668\u8fd4\u56de\u89e3\u5bc6\u9519\u8bef");
                                break block47;
                            }
                            if (respSuccess != null && !respSuccess.booleanValue()) {
                                result.setSuccess(false);
                                result.setMessage("\u6ce8\u518c\u5931\u8d25: " + respMap.get("message"));
                                log.warn("[TestRegister] \u6ce8\u518c\u5931\u8d25: {}", (Object)respMap);
                                break block47;
                            }
                            result.setSuccess(false);
                            result.setMessage("\u8bf7\u6c42\u6210\u529f\u4f46\u672a\u627e\u5230Token");
                            log.warn("[TestRegister] \u26a0\ufe0f \u54cd\u5e94\u6210\u529f\u4f46\u672a\u627e\u5230Token");
                        }
                        catch (Exception e) {
                            log.error("[TestRegister] \u89e3\u6790\u54cd\u5e94JSON\u5931\u8d25", (Throwable)e);
                            result.setSuccess(false);
                            result.setMessage("\u54cd\u5e94\u89e3\u6790\u5931\u8d25: " + e.getMessage());
                        }
                        break block47;
                    }
                    result.setSuccess(false);
                    result.setMessage("HTTP\u54cd\u5e94\u9519\u8bef: " + statusCode);
                    log.warn("[TestRegister] HTTP\u54cd\u5e94\u9519\u8bef: {}", (Object)statusCode);
                }
            }
            catch (Exception e) {
                log.error("[TestRegister] \u6ce8\u518c\u6d4b\u8bd5\u5931\u8d25", (Throwable)e);
                result.setSuccess(false);
                result.setMessage("\u8bf7\u6c42\u5931\u8d25: " + e.getMessage());
            }
        }
        log.info("[TestRegister] ========== \u6d4b\u8bd5\u5b8c\u6210 ==========");
        return result;
    }

    private Map<String, String> generateTestData(List<String> params, String encType, String rsaKeyApi, String baseUrl) throws Exception {
        HashMap<String, String> data = new HashMap<String, String>();
        long timestamp = System.currentTimeMillis();
        
        // 使用检测到的规则生成用户名和密码
        String testUsername;
        String testPassword;
        
        if (this.currentUsernameRule != null) {
            testUsername = UsernamePasswordRuleDetector.generateUsername(this.currentUsernameRule);
            log.info("根据检测规则生成用户名: {} (长度:{})", testUsername, testUsername.length());
        } else {
            // 默认规则：8位，字母开头，无下划线
            UsernameRule defaultRule = new UsernameRule();
            defaultRule.setMinLength(8);
            defaultRule.setMaxLength(8);
            defaultRule.setMustStartWithLetter(true);
            defaultRule.setAllowUnderscore(false);
            testUsername = UsernamePasswordRuleDetector.generateUsername(defaultRule);
            log.info("使用默认规则生成用户名: {}", testUsername);
        }
        
        if (this.currentPasswordRule != null) {
            testPassword = UsernamePasswordRuleDetector.generatePassword(this.currentPasswordRule);
            log.info("根据检测规则生成密码 (长度:{})", testPassword.length());
        } else {
            // 默认密码：8位，至少包含大小写和数字
            testPassword = "Abc12345";
            log.info("使用默认密码");
        }
        
        String testEmail = "test" + (timestamp % 100000000L) + "@example.com";
        String testPhone = "138" + String.format("%08d", timestamp % 100000000L);
        for (String param : params) {
            String lowerParam = param.toLowerCase();
            if (lowerParam.contains("user") || lowerParam.contains("account")) {
                data.put(param, testUsername);
                continue;
            }
            if (lowerParam.contains("pass")) {
                String encryptedPassword = this.encryptPassword(testPassword, encType, rsaKeyApi, baseUrl);
                data.put(param, encryptedPassword);
                continue;
            }
            if (lowerParam.contains("email")) {
                data.put(param, testEmail);
                continue;
            }
            if (lowerParam.contains("phone") || lowerParam.contains("mobile")) {
                data.put(param, testPhone);
                continue;
            }
            if (lowerParam.contains("captcha") || lowerParam.contains("code")) {
                data.put(param, "1234");
                continue;
            }
            data.put(param, "test_value");
        }
        return data;
    }

    private String encryptPassword(String password, String encType, String rsaKeyApi, String baseUrl) {
        try {
            switch (encType) {
                case "MD5": {
                    return this.md5(password);
                }
                case "BASE64": {
                    return Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
                }
            }
            return password;
        }
        catch (Exception e) {
            log.warn("\u5bc6\u7801\u52a0\u5bc6\u5931\u8d25\uff0c\u4f7f\u7528\u660e\u6587", (Throwable)e);
            return password;
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (Exception e) {
            return input;
        }
    }

    private String extractTokenFromResponse(Map<String, Object> respMap, Headers headers) {
        try {
            Map dataMap;
            Map valueMap;
            Object valueObj = respMap.get("value");
            if (valueObj instanceof Map && (valueMap = (Map)valueObj).containsKey("token")) {
                String token = String.valueOf(valueMap.get("token"));
                log.info("\u4eceresponse.value.token\u63d0\u53d6\u5230Token");
                return token;
            }
            Object dataObj = respMap.get("data");
            if (dataObj instanceof Map && (dataMap = (Map)dataObj).containsKey("token")) {
                String token = String.valueOf(dataMap.get("token"));
                log.info("\u4eceresponse.data.token\u63d0\u53d6\u5230Token");
                return token;
            }
            if (respMap.containsKey("token")) {
                String token = String.valueOf(respMap.get("token"));
                log.info("\u4eceresponse.token\u63d0\u53d6\u5230Token");
                return token;
            }
            String[] tokenFields = new String[]{"accessToken", "access_token", "authToken", "authorization", "jwt", "sessionId"};
            for (String field : tokenFields) {
                if (!respMap.containsKey(field)) continue;
                String token = String.valueOf(respMap.get(field));
                log.info("\u4eceresponse.{}\u63d0\u53d6\u5230Token", (Object)field);
                return token;
            }
            for (String headerName : headers.names()) {
                String headerValue;
                if (!headerName.toLowerCase().contains("token") && !headerName.toLowerCase().contains("authorization") || (headerValue = headers.get(headerName)) == null || headerValue.isEmpty()) continue;
                log.info("\u4ece\u54cd\u5e94\u5934 '{}' \u63d0\u53d6\u5230Token", (Object)headerName);
                return headerValue;
            }
        }
        catch (Exception e) {
            log.debug("Token\u63d0\u53d6\u5931\u8d25", (Throwable)e);
        }
        return null;
    }

    private String extractDomain(String url) {
        try {
            String host = HttpUrl.parse((String)url).host();
            String[] parts = host.split("\\.");
            if (parts.length >= 2) {
                return parts[parts.length - 2];
            }
            return host.replace("www.", "").replace(".com", "");
        }
        catch (Exception e) {
            return "test";
        }
    }

    private String rndString() {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(16);
        Random random = new Random();
        for (int i = 0; i < 16; ++i) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String desEncryptEcb(String plaintext, String keyStr) throws Exception {
        byte[] keyBytes = keyStr.getBytes(StandardCharsets.UTF_8);
        byte[] key8 = new byte[8];
        for (int i = 0; i < 8; ++i) {
            key8[i] = i < keyBytes.length ? keyBytes[i] : (byte)0;
        }
        SecretKeySpec secretKey = new SecretKeySpec(key8, "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(1, secretKey);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String rsaEncryptPkcs1(String keyStr, String data) throws Exception {
        try {
            String line;
            ProcessBuilder pb = new ProcessBuilder("python3", "/root/jc-test/test/encryption_service.py", "rsa_encrypt", keyStr, data);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new Exception("Python\u52a0\u5bc6\u670d\u52a1\u8fd4\u56de\u9519\u8bef: " + exitCode);
            }
            String result = output.toString().trim();
            if (result.isEmpty()) {
                throw new Exception("Python\u52a0\u5bc6\u670d\u52a1\u8fd4\u56de\u7a7a\u7ed3\u679c");
            }
            return result;
        }
        catch (Exception e) {
            log.error("\u8c03\u7528Python\u52a0\u5bc6\u670d\u52a1\u5931\u8d25", (Throwable)e);
            throw e;
        }
    }

    private List<String> findRequiredHeaders(String jsCode) {
        String[] commons;
        LinkedHashSet<String> headers = new LinkedHashSet<String>();
        Pattern p1 = Pattern.compile("headers\\s*:\\s*\\{([^}]*)\\}", 34);
        Matcher m1 = p1.matcher(jsCode);
        while (m1.find()) {
            String block = m1.group(1);
            Matcher km = Pattern.compile("['\"]([A-Za-z0-9-]+)['\"]\\s*:").matcher(block);
            while (km.find()) {
                headers.add(km.group(1));
            }
        }
        Pattern p2 = Pattern.compile("axios\\.defaults\\.headers(?:\\.common)?\\s*=\\s*\\{([^}]*)\\}", 34);
        Matcher m2 = p2.matcher(jsCode);
        while (m2.find()) {
            String block = m2.group(1);
            Matcher km = Pattern.compile("['\"]([A-Za-z0-9-]+)['\"]\\s*:").matcher(block);
            while (km.find()) {
                headers.add(km.group(1));
            }
        }
        for (String h : commons = new String[]{"authorization", "x-token", "x-auth-token", "x-csrf-token", "x-xsrf-token", "x-requested-with", "referer", "origin", "content-type", "accept", "user-agent"}) {
            if (!jsCode.toLowerCase().contains(h)) continue;
            headers.add(h);
        }
        return new ArrayList<String>(headers);
    }

    private String findCsrfTokenField(String jsCode) {
        String[] keys;
        for (String k : keys = new String[]{"csrf", "xsrf", "anti", "token", "auth", "session"}) {
            String v;
            Matcher m = Pattern.compile("[A-Za-z0-9_]*" + k + "[A-Za-z0-9_]*", 2).matcher(jsCode);
            if (!m.find() || (v = m.group()).length() > 40) continue;
            return v;
        }
        Matcher h = Pattern.compile("['\"](x-[A-Za-z0-9-]+)['\"]\\s*:").matcher(jsCode);
        if (h.find()) {
            return h.group(1);
        }
        return null;
    }

    private String analyzeLogicPatterns(String jsCode) {
        ArrayList<String> notes = new ArrayList<String>();
        String lower = jsCode.toLowerCase();
        if (lower.contains("object.keys") && lower.contains("sort") && (lower.contains("join") || lower.contains("md5"))) {
            notes.add("\u7b7e\u540d: \u6392\u5e8f+\u62fc\u63a5+MD5");
        }
        if (lower.contains("timestamp") || lower.contains("date.now") || lower.contains("new date")) {
            notes.add("\u9700\u8981timestamp");
        }
        if (lower.contains("nonce") || lower.contains("random") || lower.contains("uuid")) {
            notes.add("\u9700\u8981nonce/\u968f\u673a\u4e32");
        }
        if (lower.contains("reverse()")) {
            notes.add("\u5b57\u7b26\u4e32\u53cd\u8f6c\u53c2\u4e0e\u52a0\u5bc6");
        }
        if (lower.contains("btoa(") || lower.contains("base64")) {
            notes.add("\u542bBase64\u7f16\u7801");
        }
        if (lower.contains("json.stringify") && lower.contains("encrypt(")) {
            notes.add("\u5148JSON.stringify\u540e\u52a0\u5bc6");
        }
        if (lower.contains("setpublickey") || lower.contains("jsencrypt") || lower.contains("rsa")) {
            notes.add("RSA\u516c\u94a5\u53c2\u4e0e\uff0c\u53ef\u80fd\u5148\u5bf9\u79f0\u518dRSA");
        }
        return String.join((CharSequence)"; ", notes);
    }

    private String chooseField(List<String> params, String[] candidates, String fallback) {
        try {
            HashSet<String> p = new HashSet<String>();
            for (String s : params) {
                p.add(s.toLowerCase());
            }
            for (String c : candidates) {
                if (!p.contains(c.toLowerCase())) continue;
                return c;
            }
            for (String c : candidates) {
                for (String s : params) {
                    String ls = s.toLowerCase();
                    if (!ls.contains(c.toLowerCase())) continue;
                    return s;
                }
            }
            return fallback;
        }
        catch (Exception e) {
            return fallback;
        }
    }

    private String findEncryptionHeaderName(String jsCode) {
        try {
            String h;
            Pattern p = Pattern.compile("headers\\s*:\\s*\\{([^}]*)\\}", 34);
            Matcher m = p.matcher(jsCode);
            while (m.find()) {
                String block = m.group(1).toLowerCase();
                Matcher km = Pattern.compile("['\"]([a-z0-9-]+)['\"]\\s*:").matcher(block);
                while (km.find()) {
                    h = km.group(1);
                    if (!h.contains("auth") && !h.contains("token") && !h.contains("sign") && !h.contains("encrypt")) continue;
                    return h;
                }
            }
            Pattern p2 = Pattern.compile("axios\\.defaults\\.headers(?:\\.common)?\\s*\\[['\"]([^'\"]+)['\"]\\]", 2);
            Matcher m2 = p2.matcher(jsCode);
            while (m2.find()) {
                h = m2.group(1).toLowerCase();
                if (!h.contains("auth") && !h.contains("token") && !h.contains("sign") && !h.contains("encrypt")) continue;
                return h;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private String findValueFieldName(String jsCode) {
        try {
            Matcher m = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*:\\s*encrypt\\(", 2).matcher(jsCode);
            if (m.find()) {
                return m.group(1);
            }
            Matcher m2 = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*:\\s*[A-Za-z0-9_]+\\.encrypt\\(", 2).matcher(jsCode);
            if (m2.find()) {
                return m2.group(1);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private String generateReport(JsAnalysisResult jsAnalysis, RegisterTestResult testResult) {
        StringBuilder report = new StringBuilder();
        report.append("========== \u667a\u80fd\u5206\u6790\u62a5\u544a ==========\n");
        report.append("\u6ce8\u518c\u63a5\u53e3: ").append(jsAnalysis.getRegisterApi()).append("\n");
        report.append("\u8bf7\u6c42\u65b9\u6cd5: ").append(jsAnalysis.getMethod()).append("\n");
        report.append("\u52a0\u5bc6\u65b9\u5f0f: ").append(jsAnalysis.getEncryptionType()).append("\n");
        if (jsAnalysis.getEncryptionDetails() != null) {
            report.append("\u52a0\u5bc6\u7ec6\u8282: ").append(jsAnalysis.getEncryptionDetails()).append("\n");
        }
        report.append("\u5fc5\u9700\u53c2\u6570: ").append(String.join((CharSequence)", ", jsAnalysis.getRequiredParams())).append("\n");
        if (jsAnalysis.getRequiredHeaders() != null && !jsAnalysis.getRequiredHeaders().isEmpty()) {
            report.append("\u53ef\u80fd\u9700\u8981\u7684\u8bf7\u6c42\u5934: ").append(String.join((CharSequence)", ", jsAnalysis.getRequiredHeaders())).append("\n");
        }
        if (jsAnalysis.getCsrfTokenField() != null && !jsAnalysis.getCsrfTokenField().isEmpty()) {
            report.append("CSRF/Anti\u5b57\u6bb5: ").append(jsAnalysis.getCsrfTokenField()).append("\n");
        }
        if (jsAnalysis.getLogicDetails() != null && !jsAnalysis.getLogicDetails().isEmpty()) {
            report.append("\u901a\u7528\u903b\u8f91: ").append(jsAnalysis.getLogicDetails()).append("\n");
        }
        if (jsAnalysis.getEncryptionHeader() != null && !jsAnalysis.getEncryptionHeader().isEmpty()) {
            report.append("\u52a0\u5bc6\u5934: ").append(jsAnalysis.getEncryptionHeader()).append("\n");
        }
        if (jsAnalysis.getValueFieldName() != null && !jsAnalysis.getValueFieldName().isEmpty()) {
            report.append("\u503c\u5b57\u6bb5\u540d: ").append(jsAnalysis.getValueFieldName()).append("\n");
        }
        report.append("\u5b57\u6bb5\u6620\u5c04: username=").append(jsAnalysis.getUsernameField()).append(", password=").append(jsAnalysis.getPasswordField()).append(", email=").append(jsAnalysis.getEmailField()).append(", phone=").append(jsAnalysis.getPhoneField()).append("\n");
        report.append("\n===== \u6ce8\u518c\u6d4b\u8bd5\u7ed3\u679c =====\n");
        report.append("\u6d4b\u8bd5\u72b6\u6001: ").append(testResult.isSuccess() ? "\u2705 \u6210\u529f" : "\u274c \u5931\u8d25").append("\n");
        report.append("\u72b6\u6001\u7801: ").append(testResult.getStatusCode()).append("\n");
        if (testResult.getToken() != null) {
            report.append("Token: ").append(testResult.getToken()).append("\n");
        }
        report.append("\u6d88\u606f: ").append(testResult.getMessage()).append("\n");
        report.append("================================");
        return report.toString();
    }

    public SmartWebAnalyzer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // 初始化OkHttpClient
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .followRedirects(true)
            .cookieJar(CookieJar.NO_COOKIES)
            .build();
    }

    // ========== 内部类定义 ==========
    
    /**
     * JS分析结果
     */
    private static class JsAnalysisResult {
        private String registerApi;
        private String method = "POST";
        private String encryptionType = "NONE";
        private String encryptionDetails;
        private String rsaKeyApi;
        private String encryptionHeader;
        private String valueFieldName;
        private List<String> requiredParams = new ArrayList<>();
        private List<String> requiredHeaders = new ArrayList<>();
        private String csrfTokenField;
        private String logicDetails;
        private String usernameField = "username";
        private String passwordField = "password";
        private String emailField = "email";
        private String phoneField = "phone";

        public String getRegisterApi() { return registerApi; }
        public void setRegisterApi(String registerApi) { this.registerApi = registerApi; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getEncryptionType() { return encryptionType; }
        public void setEncryptionType(String encryptionType) { this.encryptionType = encryptionType; }
        public String getEncryptionDetails() { return encryptionDetails; }
        public void setEncryptionDetails(String encryptionDetails) { this.encryptionDetails = encryptionDetails; }
        public String getRsaKeyApi() { return rsaKeyApi; }
        public void setRsaKeyApi(String rsaKeyApi) { this.rsaKeyApi = rsaKeyApi; }
        public String getEncryptionHeader() { return encryptionHeader; }
        public void setEncryptionHeader(String encryptionHeader) { this.encryptionHeader = encryptionHeader; }
        public String getValueFieldName() { return valueFieldName; }
        public void setValueFieldName(String valueFieldName) { this.valueFieldName = valueFieldName; }
        public List<String> getRequiredParams() { return requiredParams; }
        public void setRequiredParams(List<String> requiredParams) { this.requiredParams = requiredParams; }
        public List<String> getRequiredHeaders() { return requiredHeaders; }
        public void setRequiredHeaders(List<String> requiredHeaders) { this.requiredHeaders = requiredHeaders; }
        public String getCsrfTokenField() { return csrfTokenField; }
        public void setCsrfTokenField(String csrfTokenField) { this.csrfTokenField = csrfTokenField; }
        public String getLogicDetails() { return logicDetails; }
        public void setLogicDetails(String logicDetails) { this.logicDetails = logicDetails; }
        public String getUsernameField() { return usernameField; }
        public void setUsernameField(String usernameField) { this.usernameField = usernameField; }
        public String getPasswordField() { return passwordField; }
        public void setPasswordField(String passwordField) { this.passwordField = passwordField; }
        public String getEmailField() { return emailField; }
        public void setEmailField(String emailField) { this.emailField = emailField; }
        public String getPhoneField() { return phoneField; }
        public void setPhoneField(String phoneField) { this.phoneField = phoneField; }
    }

    /**
     * 注册测试结果
     */
    private static class RegisterTestResult {
        private boolean success;
        private String message;
        private String token;
        private int statusCode;
        private String responseBody;
        private String responseHeaders;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
        public String getResponseBody() { return responseBody; }
        public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
        public String getResponseHeaders() { return responseHeaders; }
        public void setResponseHeaders(String responseHeaders) { this.responseHeaders = responseHeaders; }
    }
}
