package com.detection.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

/**
 * HTTP客户端连接池配置
 * 优化HTTP请求性能,支持检测任务的高并发请求
 * 
 * @author Detection Platform
 * @since 2024-11-12
 */
@Slf4j
@Configuration
public class HttpClientConfig {

    /**
     * 连接池最大连接数
     */
    private static final int MAX_TOTAL_CONNECTIONS = 500;

    /**
     * 每个路由的最大连接数
     */
    private static final int MAX_PER_ROUTE = 100;

    /**
     * 连接超时时间(毫秒)
     */
    private static final int CONNECTION_TIMEOUT = 10000;

    /**
     * 读取超时时间(毫秒)
     */
    private static final int READ_TIMEOUT = 30000;

    /**
     * 连接请求超时时间(毫秒)
     */
    private static final int CONNECTION_REQUEST_TIMEOUT = 5000;

    /**
     * 配置HTTP连接池管理器
     */
    @Bean
    public PoolingHttpClientConnectionManager poolingHttpClientConnectionManager() {
        try {
            // 创建SSL上下文,信任所有证书
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true)
                    .build();

            // 创建SSL连接工厂
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE
            );

            // 注册HTTP和HTTPS连接工厂
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslConnectionSocketFactory)
                    .build();

            // 创建连接池管理器
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            
            // 设置最大连接数
            connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
            
            // 设置每个路由的最大连接数
            connectionManager.setDefaultMaxPerRoute(MAX_PER_ROUTE);
            
            // 设置连接验证时间
            connectionManager.setValidateAfterInactivity(2000);

            return connectionManager;
        } catch (Exception e) {
            throw new RuntimeException("HTTP连接池配置失败", e);
        }
    }

    /**
     * 连接保持策略
     */
    @Bean
    public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
        return (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator(
                    response.headerIterator(HTTP.CONN_KEEP_ALIVE)
            );
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && "timeout".equalsIgnoreCase(param)) {
                    try {
                        return Long.parseLong(value) * 1000;
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
            // 默认保持60秒
            return 60 * 1000;
        };
    }

    /**
     * 配置请求参数
     */
    @Bean
    public RequestConfig requestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
                .build();
    }

    /**
     * 配置HttpClient
     */
    @Bean
    public CloseableHttpClient httpClient(
            PoolingHttpClientConnectionManager connectionManager,
            ConnectionKeepAliveStrategy keepAliveStrategy,
            RequestConfig requestConfig) {
        
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy(keepAliveStrategy)
                .setDefaultRequestConfig(requestConfig)
                // 设置重试次数
                .setRetryHandler((exception, executionCount, context) -> {
                    if (executionCount > 3) {
                        return false;
                    }
                    // 超时、网络异常等情况下重试
                    return exception instanceof java.net.SocketTimeoutException
                            || exception instanceof java.net.ConnectException
                            || exception instanceof java.net.UnknownHostException;
                })
                .build();
    }

    /**
     * 配置RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECTION_TIMEOUT);
        factory.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT);
        
        return new RestTemplate(factory);
    }
}
