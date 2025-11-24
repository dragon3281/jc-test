package com.detection.platform.common.constants;

/**
 * 系统常量
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
public class SystemConstants {

    /**
     * 服务器状态
     */
    public static class ServerStatus {
        public static final Integer ONLINE = 1;    // 在线
        public static final Integer OFFLINE = 2;   // 离线
        public static final Integer ABNORMAL = 3;  // 异常
    }

    /**
     * 认证方式
     */
    public static class AuthType {
        public static final Integer PASSWORD = 1;  // 密码
        public static final Integer KEY = 2;       // 密钥
    }

    /**
     * 代理类型
     */
    public static class ProxyType {
        public static final Integer HTTP = 1;      // HTTP
        public static final Integer HTTPS = 2;     // HTTPS
        public static final Integer SOCKS5 = 3;    // SOCKS5
    }

    /**
     * 代理状态
     */
    public static class ProxyStatus {
        public static final Integer AVAILABLE = 1;      // 可用
        public static final Integer UNAVAILABLE = 2;    // 不可用
        public static final Integer CHECKING = 3;       // 检测中
    }

    /**
     * 任务状态
     */
    public static class TaskStatus {
        public static final Integer PENDING = 1;        // 待执行
        public static final Integer RUNNING = 2;        // 执行中
        public static final Integer PAUSED = 3;         // 已暂停
        public static final Integer COMPLETED = 4;      // 已完成
        public static final Integer FAILED = 5;         // 失败
        public static final Integer STOPPED = 6;        // 已停止
    }

    /**
     * 检测状态
     */
    public static class DetectionStatus {
        public static final Integer REGISTERED = 1;     // 已注册
        public static final Integer UNREGISTERED = 2;   // 未注册
        public static final Integer FAILED = 3;         // 检测失败
        public static final Integer ACCOUNT_ABNORMAL = 4;  // 账号异常
        public static final Integer PROXY_ABNORMAL = 5;    // 代理异常
    }

    /**
     * 账号类型
     */
    public static class AccountType {
        public static final Integer EMAIL = 1;         // 邮箱
        public static final Integer PHONE = 2;         // 手机号
        public static final Integer USERNAME = 3;      // 用户名
    }

    /**
     * 优先级
     */
    public static class Priority {
        public static final Integer HIGH = 1;          // 高
        public static final Integer MEDIUM = 2;        // 中
        public static final Integer LOW = 3;           // 低
    }
}
