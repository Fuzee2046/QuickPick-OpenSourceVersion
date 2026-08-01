package com.fujian.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class WechatSubscribeMessageService {
    private static final String DEFAULT_PICKUP_TIP = "下单即承诺，取餐显诚信。";
    private static final String OVERTIME_PICKUP_TIP = "该类型菜品请按时取餐，超时15分钟以上口感如有下降，不属于出餐质量问题。";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${wechat.miniapp.appid}")
    private String appId;

    @Value("${wechat.miniapp.secret}")
    private String appSecret;

    // 取餐提醒模板ID
    private static final String PICKUP_REMINDER_TEMPLATE_ID = "D06pjzeY0sEHt1E0uR0ECI6KvHoqXZ-JY1UKAE6SC3U";

    // 超时二次提醒模板ID
    private static final String PICKUP_OVERTIME_TEMPLATE_ID = "D06pjzeY0sEHt1E0uR0ECOIr5khGwkn6HAOY47sTgYQ";

    // 抽奖中奖通知模板ID
    private static final String LUCKY_DRAW_WINNER_TEMPLATE_ID = "vp723a9EocUwYIMpKXnO4tthQUEJsqWH8eCPQqCKrTY";

    // 小程序页面路径，可以跳转到订单详情页
    private static final String PAGE_PATH = "pages/order-detail/order-detail";

    // 抽奖页面路径
    private static final String LUCKY_DRAW_PAGE_PATH = "pages/free-meal/free-meal";

    // access_token缓存
    private String cachedAccessToken;
    private long tokenExpireTime;

    /**
     * 发送取餐提醒订阅消息
     * @param openid 用户openid
     * @param pickupTime 取餐时间（格式：HH:mm）
     * @param shopName 店铺名称
     * @param shopAddress 店铺地址
     * @param pickupCode 取餐号码
     * @param orderId 订单编号
     * @return 发送结果的CompletableFuture
     */
    @Async
    public CompletableFuture<Boolean> sendPickupReminder(String openid, String pickupTime, String shopName, String shopAddress, String pickupCode, String orderId) {
        return sendPickupReminder(openid, pickupTime, shopName, shopAddress, pickupCode, orderId, DEFAULT_PICKUP_TIP);
    }

    @Async
    public CompletableFuture<Boolean> sendPickupReminder(String openid, String pickupTime, String shopName, String shopAddress, String pickupCode, String orderId, String tipText) {
        System.out.println("===== 开始异步发送微信订阅消息 =====");
        System.out.println("线程: " + Thread.currentThread().getName());
        System.out.println("参数: openid=" + openid + ", pickupTime=" + pickupTime + ", shopName=" + shopName + ", shopAddress=" + shopAddress + ", pickupCode=" + pickupCode + ", orderId=" + orderId + ", tipText=" + tipText);
        System.out.println("模板ID: " + PICKUP_REMINDER_TEMPLATE_ID);
        
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                System.out.println("❌ 获取access_token失败");
                return CompletableFuture.completedFuture(false);
            }
            System.out.println("✅ 获取access_token成功，长度: " + (accessToken != null ? accessToken.length() : 0));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("touser", openid);
            requestBody.put("template_id", PICKUP_REMINDER_TEMPLATE_ID);
            requestBody.put("page", PAGE_PATH + "?orderId=" + orderId);

            // 构建模板数据
            Map<String, Map<String, String>> data = new HashMap<>();
            
            // 取餐时间字段 - time6
            Map<String, String> timeData = new HashMap<>();
            timeData.put("value", pickupTime);
            data.put("time6", timeData);
            
            // 门店名称字段 - thing2
            Map<String, String> nameData = new HashMap<>();
            nameData.put("value", shopName);
            data.put("thing2", nameData);
            
            // 取餐地址字段 - thing7
            Map<String, String> addressData = new HashMap<>();
            addressData.put("value", shopAddress);
            data.put("thing7", addressData);
            
            // 订单号码字段 - character_string4
            Map<String, String> codeData = new HashMap<>();
            codeData.put("value", pickupCode);
            data.put("character_string4", codeData);
            
            // 温馨提示字段 - thing11
            Map<String, String> tipData = new HashMap<>();
            tipData.put("value", tipText);
            data.put("thing11", tipData);

            requestBody.put("data", data);
            
            System.out.println("请求体: " + requestBody);
            System.out.println("模板字段映射: time6->取餐时间(" + pickupTime + "), thing2->门店名称(" + shopName + "), thing7->取餐地址(" + shopAddress + "), character_string4->取餐号码(" + pickupCode + "), thing11->温馨提示(" + tipText + ")");

            // 发送订阅消息
            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;
            System.out.println("调用微信API: " + url.replace(accessToken, "***" + (accessToken.length() > 10 ? accessToken.substring(accessToken.length() - 6) : "***")));
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);
            System.out.println("微信API响应状态码: " + response.getStatusCode());
            
            if (response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                System.out.println("微信API响应体: " + result);
                
                Integer errcode = (Integer) result.get("errcode");
                String errmsg = (String) result.get("errmsg");
                
                System.out.println("errcode: " + errcode + ", errmsg: " + errmsg);
                
                if (errcode != null && errcode == 0) {
                    System.out.println("✅ 微信订阅消息发送成功");
                    return CompletableFuture.completedFuture(true);
                } else {
                    System.out.println("❌ 微信订阅消息发送失败，错误码: " + errcode + ", 错误信息: " + errmsg);
                    // 常见错误码解释
                    if (errcode != null) {
                        switch (errcode) {
                            case 40001: 
                                System.out.println("提示: 40001 - access_token无效，将清除缓存并重试");
                                // 清除access_token缓存，强制重新获取
                                cachedAccessToken = null;
                                tokenExpireTime = 0;
                                break;
                            case 40003: System.out.println("提示: 40003 - openid无效"); break;
                            case 40037: System.out.println("提示: 40037 - 模板ID无效"); break;
                            case 43101: 
                                System.out.println("提示: 43101 - 用户拒绝接收消息");
                                System.out.println("解决方案: 用户需要在小程序中重新授权订阅消息");
                                System.out.println("前端需要在合适时机调用 uni.requestSubscribeMessage");
                                System.out.println("常见授权时机: 登录成功、下单成功、查看订单列表时");
                                break;
                            case 47003: System.out.println("提示: 47003 - 模板参数不匹配"); break;
                        }
                    }
                    return CompletableFuture.completedFuture(false);
                }
            } else {
                System.out.println("❌ 微信API返回空响应体");
                return CompletableFuture.completedFuture(false);
            }
        } catch (Exception e) {
            System.out.println("❌ 发送微信订阅消息异常: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("===== 微信订阅消息发送结束 =====");
        return CompletableFuture.completedFuture(false);
    }

    public CompletableFuture<Boolean> sendPickupOvertimeReminder(String openid, String pickupTime, String shopName, String shopAddress, String pickupCode, String orderId) {
        System.out.println("===== 开始异步发送超时二次提醒微信订阅消息 =====");
        System.out.println("参数: openid=" + openid + ", pickupTime=" + pickupTime + ", shopName=" + shopName + ", pickupCode=" + pickupCode + ", orderId=" + orderId);
        System.out.println("模板ID: " + PICKUP_OVERTIME_TEMPLATE_ID);

        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                System.out.println("❌ 获取access_token失败");
                return CompletableFuture.completedFuture(false);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("touser", openid);
            requestBody.put("template_id", PICKUP_OVERTIME_TEMPLATE_ID);
            requestBody.put("page", PAGE_PATH + "?orderId=" + orderId);

            Map<String, Map<String, String>> data = new HashMap<>();

            Map<String, String> timeData = new HashMap<>();
            timeData.put("value", pickupTime);
            data.put("time6", timeData);

            Map<String, String> nameData = new HashMap<>();
            nameData.put("value", shopName);
            data.put("thing2", nameData);

            Map<String, String> codeData = new HashMap<>();
            codeData.put("value", pickupCode);
            data.put("character_string4", codeData);

            Map<String, String> tipData = new HashMap<>();
            tipData.put("value", OVERTIME_PICKUP_TIP);
            data.put("thing11", tipData);

            requestBody.put("data", data);

            System.out.println("二次提醒请求体: " + requestBody);
            System.out.println("二次提醒模板字段映射: time6->取餐时间(" + pickupTime + "), thing2->门店名称(" + shopName + "), character_string4->取餐号码(" + pickupCode + "), thing11->温馨提示(" + OVERTIME_PICKUP_TIP + ")");

            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);
            System.out.println("二次提醒微信API响应状态码: " + response.getStatusCode());

            if (response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                System.out.println("二次提醒微信API响应体: " + result);

                Integer errcode = (Integer) result.get("errcode");
                String errmsg = (String) result.get("errmsg");
                if (errcode != null && errcode == 0) {
                    System.out.println("✅ 超时二次提醒微信订阅消息发送成功");
                    return CompletableFuture.completedFuture(true);
                }

                System.out.println("❌ 超时二次提醒微信订阅消息发送失败，错误码: " + errcode + ", 错误信息: " + errmsg);
                if (errcode != null && errcode == 40001) {
                    cachedAccessToken = null;
                    tokenExpireTime = 0;
                }
                return CompletableFuture.completedFuture(false);
            }

            System.out.println("❌ 二次提醒微信API返回空响应体");
        } catch (Exception e) {
            System.out.println("❌ 发送超时二次提醒异常: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }

        return CompletableFuture.completedFuture(false);
    }

    /**
     * 获取微信access_token
     * @return access_token
     */
    private String getAccessToken() {
        System.out.println("===== 获取微信access_token =====");
        System.out.println("AppID: " + appId + ", AppSecret: " + (appSecret != null ? "***" + appSecret.substring(Math.max(0, appSecret.length() - 4)) : "null"));
        
        // 检查缓存是否有效（提前5分钟刷新）
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime - 5 * 60 * 1000) {
            System.out.println("✅ 使用缓存的access_token，过期时间: " + new java.util.Date(tokenExpireTime));
            return cachedAccessToken;
        } else if (cachedAccessToken != null) {
            System.out.println("⚠️ 缓存access_token已过期或即将过期，重新获取");
        } else {
            System.out.println("ℹ️ 无缓存access_token，重新获取");
        }

        try {
            String url = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s", 
                    appId, appSecret);
            System.out.println("调用微信token接口: " + url.replace(appSecret, "***"));
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            System.out.println("微信token接口响应状态码: " + response.getStatusCode());
            
            if (response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                System.out.println("微信token接口响应体: " + result);
                
                if (result.containsKey("access_token")) {
                    String accessToken = (String) result.get("access_token");
                    Integer expiresIn = (Integer) result.get("expires_in");
                    
                    // 更新缓存
                    cachedAccessToken = accessToken;
                    tokenExpireTime = System.currentTimeMillis() + expiresIn * 1000L;
                    
                    System.out.println("✅ 获取access_token成功，长度: " + accessToken.length() + 
                                     ", 过期时间: " + expiresIn + "秒, 缓存至: " + new java.util.Date(tokenExpireTime));
                    return accessToken;
                } else {
                    System.out.println("❌ 微信token接口响应不包含access_token");
                    Integer errcode = (Integer) result.get("errcode");
                    String errmsg = (String) result.get("errmsg");
                    if (errcode != null) {
                        System.out.println("错误码: " + errcode + ", 错误信息: " + errmsg);
                        // 常见错误码解释
                        switch (errcode) {
                            case 40001: System.out.println("提示: 40001 - AppSecret错误"); break;
                            case 40002: System.out.println("提示: 40002 - 请确保grant_type字段值为client_credential"); break;
                            case 40164: System.out.println("提示: 40164 - 调用接口的IP地址不在白名单中"); break;
                        }
                    }
                }
            } else {
                System.out.println("❌ 微信token接口返回空响应体");
            }
        } catch (Exception e) {
            System.out.println("❌ 获取access_token异常: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("===== 获取access_token结束 =====");
        return null;
    }

    /**
     * 发送抽奖中奖通知订阅消息
     * @param openid 用户openid
     * @param activityName 活动名称
     * @return 发送结果
     */
    public boolean sendLuckyDrawWinnerNotification(String openid, String activityName) {
        System.out.println("===== 开始发送抽奖中奖微信订阅消息 =====");
        System.out.println("参数: openid=" + openid + ", activityName=" + activityName);
        System.out.println("模板ID: " + LUCKY_DRAW_WINNER_TEMPLATE_ID);
        
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                System.out.println("❌ 获取access_token失败");
                return false;
            }
            System.out.println("✅ 获取access_token成功，长度: " + (accessToken != null ? accessToken.length() : 0));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("touser", openid);
            requestBody.put("template_id", LUCKY_DRAW_WINNER_TEMPLATE_ID);
            requestBody.put("page", LUCKY_DRAW_PAGE_PATH);

            // 构建模板数据
            Map<String, Map<String, String>> data = new HashMap<>();
            
            // 活动名称字段 - thing3
            Map<String, String> activityNameData = new HashMap<>();
            activityNameData.put("value", activityName);
            data.put("thing3", activityNameData);
            
            // 活动商品字段 - thing1
            Map<String, String> productData = new HashMap<>();
            productData.put("value", "免单");
            data.put("thing1", productData);
            
            // 抽奖结果字段 - phrase5
            Map<String, String> resultData = new HashMap<>();
            resultData.put("value", "中奖");
            data.put("phrase5", resultData);
            
            // 温馨提示字段 - thing2
            Map<String, String> tipData = new HashMap<>();
            tipData.put("value", "恭喜你！请尽快打开小程序领取哦");
            data.put("thing2", tipData);

            requestBody.put("data", data);
            
            System.out.println("请求体: " + requestBody);
            System.out.println("模板字段映射: thing3->活动名称(" + activityName + "), thing1->活动商品(免单), phrase5->抽奖结果(中奖), thing2->温馨提示(恭喜你！请尽快打开小程序领取哦)");

            // 发送订阅消息
            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;
            System.out.println("调用微信API: " + url.replace(accessToken, "***" + (accessToken.length() > 10 ? accessToken.substring(accessToken.length() - 6) : "***")));
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);
            System.out.println("微信API响应状态码: " + response.getStatusCode());
            
            if (response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                System.out.println("微信API响应体: " + result);
                
                Integer errcode = (Integer) result.get("errcode");
                String errmsg = (String) result.get("errmsg");
                
                System.out.println("errcode: " + errcode + ", errmsg: " + errmsg);
                
                if (errcode != null && errcode == 0) {
                    System.out.println("✅ 抽奖中奖微信订阅消息发送成功");
                    return true;
                } else {
                    System.out.println("❌ 抽奖中奖微信订阅消息发送失败，错误码: " + errcode + ", 错误信息: " + errmsg);
                    // 常见错误码解释
                    if (errcode != null) {
                        switch (errcode) {
                            case 40003: System.out.println("提示: 40003 - openid无效"); break;
                            case 40037: System.out.println("提示: 40037 - 模板ID无效"); break;
                            case 43101: System.out.println("提示: 43101 - 用户拒绝接收消息"); break;
                            case 47003: System.out.println("提示: 47003 - 模板参数不匹配"); break;
                        }
                    }
                    return false;
                }
            } else {
                System.out.println("❌ 微信API返回空响应体");
                return false;
            }
        } catch (Exception e) {
            System.out.println("❌ 发送抽奖中奖微信订阅消息异常: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("===== 抽奖中奖微信订阅消息发送结束 =====");
        return false;
    }
}
