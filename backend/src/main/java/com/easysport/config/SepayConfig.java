package com.easysport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SepayConfig {

    @Value("${sepay.merchant-id:}")
    private String merchantId;

    @Value("${sepay.secret-key:}")
    private String secretKey;

    @Value("${sepay.ipn-secret:}")
    private String ipnSecret;

    @Value("${sepay.app-public-url:http://localhost:8080}")
    private String appPublicUrl;

    @Value("${sepay.checkout-url:https://pay-sandbox.sepay.vn/v1/checkout/init}")
    private String checkoutUrl;

    @Value("${sepay.query-api-url:https://pgapi-sandbox.sepay.vn}")
    private String queryApiUrl;

    public boolean isConfigured() {
        return merchantId != null && !merchantId.trim().isEmpty()
                && secretKey != null && !secretKey.trim().isEmpty()
                && ipnSecret != null && !ipnSecret.trim().isEmpty();
    }

    public String getMerchantId() {
        return merchantId != null ? merchantId.trim() : "";
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getSecretKey() {
        return secretKey != null ? secretKey.trim() : "";
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getIpnSecret() {
        return ipnSecret != null ? ipnSecret.trim() : "";
    }

    public void setIpnSecret(String ipnSecret) {
        this.ipnSecret = ipnSecret;
    }

    public String getAppPublicUrl() {
        String url = appPublicUrl != null ? appPublicUrl.trim() : "http://localhost:8080";
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    public void setAppPublicUrl(String appPublicUrl) {
        this.appPublicUrl = appPublicUrl;
    }

    public String getCheckoutUrl() {
        return checkoutUrl != null ? checkoutUrl.trim() : "https://pay-sandbox.sepay.vn/v1/checkout/init";
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getQueryApiUrl() {
        return queryApiUrl != null ? queryApiUrl.trim() : "https://pgapi-sandbox.sepay.vn";
    }

    public void setQueryApiUrl(String queryApiUrl) {
        this.queryApiUrl = queryApiUrl;
    }
}

