package com.ss;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.log.dialect.log4j2.Log4j2LogFactory;
import com.alibaba.fastjson.JSON;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Main {
    static {
        LogFactory.setCurrentLogFactory(new Log4j2LogFactory());
    }
    private static final Logger log = LogManager.getLogger();

    private final String SITE = "maya";
    private int POOL_SIZE = 50;

    private static final Charset UTF8 = CharsetUtil.CHARSET_UTF_8;

    private List<String> proxyList = new ArrayList<>();
    private ConcurrentLinkedQueue<String> proxyIps;
    private String proxyFileName = "proxy.txt";

    private ConcurrentLinkedQueue<String> phoneNumbers;
    private LinkedBlockingQueue<String> successPhoneNumbers;
    private LinkedBlockingQueue<String> requestPhoneNumbers;
    private LinkedBlockingQueue<String> otherPhoneNumbers;

    private String sitePhoneFilePathSuccess = null;
    private String sitePhoneFilePathRequest = null;
    private String sitePhoneFilePathOther = null;

    private String sitePhoneFilePath = null;
    private String siteRunPhonesPath = null;
    private String siteDlPhoneFilePath = "dlphone.txt";
    private final AtomicInteger currentThreadCount = new AtomicInteger(0);
    private final ThreadPoolExecutor executor;
    /**停止标志*/
    private volatile boolean stopFlag = false;
    /**是否第一次更新号码状态*/
    private boolean isFirstUpdateStatus = false;
    protected final int QUEUE_CHECK_INTERVAL = 10000;
    /**定时任务线程池*/
    private final ScheduledExecutorService scheduledExecutorService;
    /**数据保存时间间隔*/
    private final int SAVE_INTERVAL = 2;

    public static void main(String[] args) {
        new Main().startProcess(args);
    }


    public Main() {
        initConfigPath();

        this.executor = new ThreadPoolExecutor(
                POOL_SIZE * 2, POOL_SIZE * 2,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(POOL_SIZE * 6),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.scheduledExecutorService = Executors.newScheduledThreadPool(3);
        this.phoneNumbers = new ConcurrentLinkedQueue<>();
        this.successPhoneNumbers = new LinkedBlockingQueue<>();
        this.requestPhoneNumbers = new LinkedBlockingQueue<>();
        this.otherPhoneNumbers = new LinkedBlockingQueue<>();
        this.proxyIps = new ConcurrentLinkedQueue<>();

        scheduleTasks();
    }

    private void scheduleTasks() {

        this.scheduledExecutorService.scheduleAtFixedRate(this::saveSuccessResultsToFile, 0, 1, TimeUnit.SECONDS);
        this.scheduledExecutorService.scheduleAtFixedRate(this::saveRequestResultsToFile, 0, SAVE_INTERVAL, TimeUnit.SECONDS);
        // saveOtherResultsToFile
        this.scheduledExecutorService.scheduleAtFixedRate(this::saveOtherResultsToFile, 0, SAVE_INTERVAL, TimeUnit.SECONDS);
    }

    private void startProcess(String[] args) {
        // 打印站点
        log.info("站点: {}", SITE);
        // 打印线程数
        log.info("线程数: {}", POOL_SIZE);
        // 底料文件路径
        log.info("底料文件路径: {}", sitePhoneFilePath);
        isFirstUpdateStatus = true;
        fetchStaticProxyIps();
        fetchFirstTasksFromFile();
        fetchTasksFromRunPhones();
        System.gc();
        // 运行任务
        for (int i = 0; i < POOL_SIZE; i++) {
            currentThreadCount.incrementAndGet();
            submitTask(this::runTask);
        }
    }

    private void submitTask(Runnable task) {
        executor.submit(task);
    }

    private void runTask() {
        String proxyIp = proxyIps.poll();
        if (proxyIp == null || !proxyIp.contains(":")) {
            log.warn("没有可用的代理IP。等待后重试。");
            ThreadUtil.sleep(QUEUE_CHECK_INTERVAL);
            return;
        }
        while (!stopFlag) {
            if (StrUtil.isBlank(proxyIp)) {
                proxyIp = proxyIps.poll();
            }
            String phoneNumberStr = getPhoneNumber();
            if (phoneNumberStr == null) {
                log.warn("没有可用的号码。等待后重试。");
                ThreadUtil.sleep(QUEUE_CHECK_INTERVAL);
                continue;
            }
            try {
                String result = processPhoneNumber(SITE, proxyIp, phoneNumberStr);
                if (StrUtil.isNotBlank(result)) {
                    if ("exitThread".equals(result)) {
                        break;
                    }
                }
            } catch (Throwable e) {
                addPhoneNumber(phoneNumberStr);
                log.error("Error executing task", e);
            } finally {
                // 归还代理IP
                if (StrUtil.isNotBlank(proxyIp)) {
                    proxyIps.add(proxyIp);
                    // 置为空 以便重新获取
                    proxyIp = null;
                }
            }
        }
        currentThreadCount.decrementAndGet();
    }

    private String processPhoneNumber(String site, String proxyIp, String phoneNumberStr) {
        try {
            // 解析代理配置
            Proxy proxy = parseProxy(proxyIp);
            
            // 构建 OkHttpClient
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS);
            
            if (proxy != null) {
                clientBuilder.proxy(proxy);
            }
            
            OkHttpClient client = clientBuilder.build();
            
            // 先尝试访问注册页面，分析其表单提交逻辑
            // 方案1: 直接POST到注册接口（通常会返回号码已存在的错误）
            // 方案2: 可能有专门的检测接口
            
            // 尝试多个可能的API端点
            String[] possibleUrls = {
                "https://ppvip6.com/register",
                "https://ppvip6.com/api/register",
                "https://ppvip6.com/api/check-phone",
                "https://ppvip6.com/api/user/register",
                "https://ppvip6.com/signup"
            };
            
            String url = possibleUrls[0]; // 先尝试注册接口
            
            // 构建完整的注册表单数据（模拟真实注册）
            String username = "test" + (phoneNumberStr.length() > 6 ? phoneNumberStr.substring(phoneNumberStr.length()-6) : phoneNumberStr);
            RequestBody formBody = new FormBody.Builder()
                .add("mobile", phoneNumberStr)
                .add("phone", phoneNumberStr)
                .add("username", username)
                .add("password", "Test123456")
                .add("password_confirmation", "Test123456")
                .add("confirm_password", "Test123456")
                .build();
            
            Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Accept-Language", "bn-BD,bn;q=0.9,en-US;q=0.8,en;q=0.7")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Origin", "https://ppvip6.com")
                .addHeader("Referer", "https://ppvip6.com/")
                .build();
            
            // 发送请求
            Response response = client.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            log.info("手机号: {} HTTP状态: {} 响应: {}", phoneNumberStr, response.code(), responseBody);
            
            // 解析响应判断是否已注册
            // 常见的注册失败响应模式：
            // 1. HTTP 422/409 状态码表示验证失败/冲突
            // 2. 响应JSON中包含错误信息
            // 3. 孟加拉语错误提示
            
            String lowerBody = responseBody.toLowerCase();
            
            // 判断号码已注册的条件
            boolean isRegistered = false;
            
            // 检查关键字（英文）
            if (lowerBody.contains("already") || 
                lowerBody.contains("exist") || 
                lowerBody.contains("registered") ||
                lowerBody.contains("taken") ||
                lowerBody.contains("duplicate")) {
                isRegistered = true;
            }
            
            // 检查孟加拉语关键字
            if (responseBody.contains("বিদ্যমান") ||  // 存在
                responseBody.contains("নিবন্ধন") ||  // 注册
                responseBody.contains("ইতিমধ্যে")) {  // 已经
                isRegistered = true;
            }
            
            // 检查HTTP状态码
            if (response.code() == 409 || response.code() == 422) {
                isRegistered = true;
            }
            
            // 检查是否注册成功（表示之前未注册）
            boolean registrationSuccess = false;
            if (response.code() == 200 || response.code() == 201) {
                if (lowerBody.contains("success") || 
                    lowerBody.contains("created") ||
                    lowerBody.contains("welcome")) {
                    registrationSuccess = true;
                }
            }
            
            if (isRegistered) {
                addSuccessPhoneNumber(phoneNumberStr);
                log.info("✓ 号码已注册: {}", phoneNumberStr);
                return "success";
            } else if (registrationSuccess) {
                addRequestPhoneNumber(phoneNumberStr);
                log.info("✗ 号码未注册（注册成功）: {}", phoneNumberStr);
                return "request";
            } else {
                // 无法确定，记录详细信息用于分析
                log.warn("无法判断 - 手机号: {} 状态码: {} 响应: {}", phoneNumberStr, response.code(), responseBody);
                addOtherPhoneNumber(phoneNumberStr);
                return "other";
            }
            
        } catch (IOException e) {
            log.error("网络请求失败: {}", phoneNumberStr, e);
            addOtherPhoneNumber(phoneNumberStr);
            return "error";
        } catch (Exception e) {
            log.error("处理异常: {}", phoneNumberStr, e);
            addPhoneNumber(phoneNumberStr); // 重新加入队列
            return null;
        }
    }
    
    /**
     * 解析代理配置
     * 支持格式：
     * - socks5://user:pass@host:port
     * - host:port (HTTP代理)
     */
    private Proxy parseProxy(String proxyStr) {
        if (StrUtil.isBlank(proxyStr)) {
            return null;
        }
        
        try {
            // 移除BOM字符和空白
            proxyStr = proxyStr.trim().replace("﻿", "").replace("\u200b", "");
            
            if (proxyStr.startsWith("socks5://")) {
                // 解析 SOCKS5 代理
                String config = proxyStr.substring(9); // 移除 socks5://
                String auth = null;
                String hostPort = config;
                
                if (config.contains("@")) {
                    String[] parts = config.split("@");
                    auth = parts[0];
                    hostPort = parts[1];
                    
                    // 设置认证
                    String[] authParts = auth.split(":");
                    final String username = authParts[0];
                    final String password = authParts.length > 1 ? authParts[1] : "";
                    
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password.toCharArray());
                        }
                    });
                }
                
                String[] hostPortParts = hostPort.split(":");
                String host = hostPortParts[0];
                int port = Integer.parseInt(hostPortParts[1]);
                
                return new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
                
            } else if (proxyStr.contains(":")) {
                // 解析 HTTP 代理
                String[] parts = proxyStr.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            }
        } catch (Exception e) {
            log.error("代理解析失败: {}", proxyStr, e);
        }
        
        return null;
    }


    private void initConfigPath() {
        String userDir = System.getProperty("user.dir");
        String configDir = userDir + "/config.json";

        sitePhoneFilePathSuccess = userDir + "/" + SITE + "_" + "success.txt";
        sitePhoneFilePathRequest = userDir + "/" + SITE + "_" + "request.txt";
        sitePhoneFilePathOther = userDir + "/" + SITE + "_" + "other.txt";
        sitePhoneFilePath = userDir + "/" + SITE + ".txt";
        if (!FileUtil.exist(sitePhoneFilePath)) {
            // 如果不存在就加载根目录dlphone.txt
            sitePhoneFilePath = userDir + "/" + siteDlPhoneFilePath;
        }
        siteRunPhonesPath = userDir + "/" + SITE + "_run_phones.txt";
        if (FileUtil.exist(siteRunPhonesPath)) {
            FileUtil.del(siteRunPhonesPath);
        }
    }

    private void fetchStaticProxyIps() {
        String userDir = System.getProperty("user.dir");
        String proxyFilePath = userDir + "/" + proxyFileName;
        if (FileUtil.exist(proxyFilePath)) {
            proxyList = FileUtil.readUtf8Lines(proxyFilePath);
            if (proxyList != null) {
                proxyIps.addAll(proxyList);
                log.info("代理数: {}", proxyIps.size());
            }
        }
    }

    private void fetchFirstTasksFromFile() {
        String userDir = System.getProperty("user.dir");

        Set<String> requestPhoneSet  = new HashSet<>();
        Set<String> successPhoneSet = new HashSet<>();
        Set<String> otherPhoneSet = new HashSet<>();

        // 首先看有没有站点名专用的号码文件
        if (FileUtil.exist(sitePhoneFilePath)) {
            // 读取已经成功的
            if (FileUtil.exist(sitePhoneFilePathSuccess)) {
                List<String> successPhones = FileUtil.readLines(new File(sitePhoneFilePathSuccess), UTF8);
                successPhoneSet.addAll(successPhones);
                log.info("成功号码库: {}", successPhoneSet.size());
            }
            // 读取已经请求过的
            if (FileUtil.exist(sitePhoneFilePathRequest)) {
                List<String> requestPhones = FileUtil.readLines(new File(sitePhoneFilePathRequest), UTF8);
                requestPhoneSet.addAll(requestPhones);
                log.info("请求号码库: {}", requestPhoneSet.size());
            }
            // 读取错误的号码
            if (FileUtil.exist(sitePhoneFilePathOther)) {
                List<String> otherPhones = FileUtil.readLines(new File(sitePhoneFilePathOther), UTF8);
                otherPhoneSet.addAll(otherPhones);
                log.info("错误号码库: {}", otherPhones.size());
            }

            Set<String> phoneSet = new HashSet<>();
            List<String> phones = FileUtil.readLines(new File(sitePhoneFilePath), UTF8);
            phoneSet.addAll(phones);
            log.info("底料号码库: {}", phoneSet.size());

            if (phoneSet != null) {

                List<String> phoneList = phoneSet.stream()
                        .filter(phone -> !successPhoneSet.contains(phone))
                        .filter(phone -> !requestPhoneSet.contains(phone))
                        .filter(phone -> !otherPhoneSet.contains(phone))
                        .collect(Collectors.toList());

                log.info("Fetched {} new phone numbers", phoneList.size());
                // phoneNumbers.addAll(phoneList);
                // 保存到本地文件
                log.info("开始保存到本地文件: {}", siteRunPhonesPath);
                FileUtil.writeUtf8Lines(phoneList, siteRunPhonesPath);
                log.info("完成保存到本地文件: {}", siteRunPhonesPath);
                releaseStrList(phoneList);
                releaseSet(phoneSet);
            }
            // 清空requestPhoneSet占用内存
            if (!requestPhoneSet.isEmpty()) {
                releaseSet(requestPhoneSet);
            }
            // 清空successPhoneSet占用内存
            if (!successPhoneSet.isEmpty()) {
                releaseSet(successPhoneSet);
            }
        } else {
            log.info("No phone number file found for site: {}", SITE);
        }
        System.gc();
    }


    private synchronized void fetchTasksFromRunPhones() {
        if (FileUtil.exist(siteRunPhonesPath)) {
            List<String> phoneList = FileUtil.readLines(new File(siteRunPhonesPath), UTF8);
            if (phoneList != null && !phoneList.isEmpty()) {
                log.info("Run Fetched {} new phone numbers", phoneList.size());
                phoneNumbers.addAll(phoneList);
                releaseStrList(phoneList);
                // System.gc();
            } else {
                log.info("Run Fetched 0 new phone numbers");
                //siteRunPhonesLimit.set(0);
                //stopFlag = true;
            }
        }
    }

    public String getPhoneNumber() {
        // 从队列中取出一个号码
        return phoneNumbers.poll();
    }

    // 出现错误用来重试的
    public void addPhoneNumber(String phoneNumber) {
        // 添加号码到队列
        phoneNumbers.add(phoneNumber);
    }

    // 已请求过的, 没有存在于系统的
    private void addRequestPhoneNumber(String phoneNumber) {
        requestPhoneNumbers.add(phoneNumber);
    }

    // 成功的
    private void addSuccessPhoneNumber(String phoneNumber) {
        successPhoneNumbers.add(phoneNumber);
    }

    // 其他错误的
    private void addOtherPhoneNumber(String phoneNumber) {
        otherPhoneNumbers.add(phoneNumber);
    }

    private void saveSuccessResultsToFile() {
        try {
            // Batch update phone statuses
            if (!successPhoneNumbers.isEmpty()) {
                List<String> successPhoneList = new ArrayList<>();
                successPhoneNumbers.drainTo(successPhoneList);
                if (!successPhoneList.isEmpty()) {
                    FileUtil.appendUtf8Lines(successPhoneList, sitePhoneFilePathSuccess);

                    // 可以再这里写入后台Spring+数据库

                    // 清空successPhoneList占用内存
                    releaseStrList(successPhoneList);
                }
            }
        } catch (Exception e) {
            log.error("Error saving saveSuccessResultsToFile 3", e);
        }
    }

    private void saveOtherResultsToFile() {
        try {
            // Batch update phone statuses
            if (!otherPhoneNumbers.isEmpty()) {
                List<String> otherPhoneList = new ArrayList<>();
                otherPhoneNumbers.drainTo(otherPhoneList);
                if (!otherPhoneList.isEmpty()) {
                    FileUtil.appendUtf8Lines(otherPhoneList, sitePhoneFilePathOther);

                    // 可以再这里写入后台Spring+数据库


                    // 清空otherPhoneList占用内存
                    releaseStrList(otherPhoneList);
                }
            }
        } catch (Exception e) {
            log.error("saveOtherResultsToFile Error saving", e);
        }
    }


    private synchronized void saveRequestResultsToFile() {
        try {
            // Batch update phone statuses
            if (!requestPhoneNumbers.isEmpty()) {
                List<String> requestPhoneList = new ArrayList<>();
                requestPhoneNumbers.drainTo(requestPhoneList);
                if (!requestPhoneList.isEmpty()) {
                    FileUtil.appendUtf8Lines(requestPhoneList, sitePhoneFilePathRequest);

                    // 可以再这里写入后台Spring+数据库

                    releaseStrList(requestPhoneList);
                }
            }
        } catch (Exception e) {
            log.error("Error saving saveRequestResultsToFile 1", e);
        }
    }




    private void releaseStrList(List<String> list) {
        if (list != null && !list.isEmpty()) {
            Collections.fill(list, null);
            list.clear();
            list = null;
        }
    }
    private void releaseSet(Set<String> phoneSet) {
        if (phoneSet != null && !phoneSet.isEmpty()) {
            phoneSet.clear();
            phoneSet = null;
        }
    }



}