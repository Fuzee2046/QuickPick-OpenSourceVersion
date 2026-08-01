package com.fujian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fujian.common.JwtTokenUtil;
import com.fujian.common.Result;
import com.fujian.mapper.UserMapper;
import com.fujian.pojo.User;
import com.fujian.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Autowired
    private ObjectMapper objectMapper;

    // 从配置文件读取微信小程序配置
    @Value("${wechat.miniapp.appid}")
    private String appId;

    @Value("${wechat.miniapp.secret}")
    private String appSecret;

    @Override
    public Result<Map<String, Object>> login(String code, String phoneCode, String name) {
        if (code == null || code.isEmpty()) {
            return Result.error("code不能为空");
        }

        // 检查微信配置是否正确
        if ("wx_mock_secret_replace_me".equals(appSecret) || "wx_mock_appid_replace_me".equals(appId)) {
            return Result.error("后端未配置微信小程序AppID和AppSecret，请在 application.yaml 中配置");
        }

        String openid;
        String phoneNumber = null;

        try {
            // 1. 获取微信OpenID
            String jscode2sessionUrl = String.format("https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    appId, appSecret, code);
            
            System.out.println("调用微信jscode2session接口，URL: " + jscode2sessionUrl.replace(appSecret, "***"));
            
            // 使用String响应类型，避免content-type问题
            String sessionResponseStr = restTemplate.getForObject(jscode2sessionUrl, String.class);
            
            if (sessionResponseStr == null || sessionResponseStr.trim().isEmpty()) {
                return Result.error("微信登录失败: API返回空响应");
            }
            
            System.out.println("微信jscode2session原始响应: " + sessionResponseStr);
            
            try {
                // 解析JSON响应
                Map<String, Object> sessionResponse = objectMapper.readValue(sessionResponseStr, Map.class);
                
                if (!sessionResponse.containsKey("openid")) {
                    String errmsg = (String) sessionResponse.get("errmsg");
                    Integer errcode = (Integer) sessionResponse.get("errcode");
                    return Result.error("微信登录失败: " + (errmsg != null ? errmsg : "未知错误") + 
                                      (errcode != null ? " (错误码: " + errcode + ")" : ""));
                }
                
                openid = (String) sessionResponse.get("openid");
                System.out.println("获取到真实微信openid: " + openid + "，长度: " + openid.length());
                
            } catch (Exception jsonParseException) {
                // 如果JSON解析失败，可能是其他错误格式
                System.out.println("JSON解析失败，响应内容: " + sessionResponseStr);
                return Result.error("微信登录失败: 响应格式错误 - " + sessionResponseStr);
            }

            // 2. 获取手机号（如果提供了phoneCode）
            if (phoneCode != null && !phoneCode.isEmpty()) {
                // 2.1 获取Access Token
                String tokenUrl = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                        appId, appSecret);
                System.out.println("获取access_token，URL: " + tokenUrl.replace(appSecret, "***"));
                
                // 使用String响应类型获取access_token
                String tokenResponseStr = restTemplate.getForObject(tokenUrl, String.class);
                if (tokenResponseStr != null && !tokenResponseStr.trim().isEmpty()) {
                    try {
                        Map<String, Object> tokenResponse = objectMapper.readValue(tokenResponseStr, Map.class);
                        if (tokenResponse != null && tokenResponse.containsKey("access_token")) {
                            String accessToken = (String) tokenResponse.get("access_token");
                            System.out.println("获取access_token成功，长度: " + accessToken.length());
                            
                            // 2.2 获取手机号
                            String phoneUrl = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;
                            Map<String, String> phoneBody = new HashMap<>();
                            phoneBody.put("code", phoneCode);
                            
                            System.out.println("调用获取手机号接口，phoneCode: " + phoneCode);
                            Map<String, Object> phoneResponse = restTemplate.postForObject(phoneUrl, phoneBody, Map.class);
                            
                            if (phoneResponse != null && (Integer) phoneResponse.get("errcode") == 0) {
                                Map<String, Object> phoneInfo = (Map<String, Object>) phoneResponse.get("phone_info");
                                phoneNumber = (String) phoneInfo.get("phoneNumber");
                                System.out.println("获取到真实手机号: " + phoneNumber);
                            } else {
                                System.out.println("获取手机号失败，响应: " + phoneResponse);
                                // 不设置phoneNumber，让用户手动填写
                            }
                        } else {
                            System.out.println("获取access_token失败，响应内容: " + tokenResponseStr);
                        }
                    } catch (Exception e) {
                        System.out.println("解析access_token响应失败: " + tokenResponseStr);
                        System.out.println("错误信息: " + e.getMessage());
                    }
                } else {
                    System.out.println("获取access_token失败，返回空响应");
                }
            }

        } catch (Exception e) {
            System.out.println("微信API调用异常: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return Result.error("微信API调用异常: " + e.getMessage());
        }

        // 3. Find or Create User
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setCreateTime(LocalDateTime.now());
        }
        
        // Update user info
        if (phoneNumber != null) {
            user.setPhone(phoneNumber);
        }
        if (name != null && !name.isEmpty()) {
            user.setName(name);
        }
        user.setUpdateTime(LocalDateTime.now());
        
        this.saveOrUpdate(user);

        // 4. Generate Token
        String token = jwtTokenUtil.generateToken(user.getId(), user.getName(), user.getOpenid());

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("token", token);
        resultData.put("userInfo", user);

        return Result.success(resultData);
    }
}
