package com.fujian.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.merchant-expiration:31536000000}")
    private Long merchantExpiration;

    @Value("${jwt.user-expiration:157680000000}")
    private Long userExpiration;

    @Value("${jwt.admin-expiration:28800000}")
    private Long adminExpiration;

    public String generateToken(Long shopId, String shopName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("shopId", shopId);
        claims.put("shopName", shopName);
        claims.put("role", "merchant");
        return createToken(claims, String.valueOf(shopId), merchantExpiration);
    }

    public String generateToken(Long userId, String userName, String openid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userName", userName);
        claims.put("openid", openid);
        claims.put("role", "user");
        return createToken(claims, String.valueOf(userId), userExpiration);
    }

    public String generateAdminToken(Long adminId, String username, String displayName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", adminId);
        claims.put("username", username);
        claims.put("displayName", displayName);
        claims.put("role", "admin");
        return createToken(claims, String.valueOf(adminId), adminExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, Long tokenExpiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public Boolean validateToken(String token, Long shopId) {
        final Long extractedShopId = getShopIdFromToken(token);
        return (extractedShopId.equals(shopId) && !isTokenExpired(token));
    }

    public Boolean validateToken(String token) {
        return !isTokenExpired(token);
    }

    public Long getShopIdFromToken(String token) {
        final Claims claims = getAllClaimsFromToken(token);
        return ((Number) claims.get("shopId")).longValue();
    }
    
    public String getShopNameFromToken(String token) {
        final Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("shopName");
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
}
