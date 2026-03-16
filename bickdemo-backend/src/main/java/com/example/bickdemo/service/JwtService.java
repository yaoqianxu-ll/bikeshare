package com.example.bickdemo.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JWT 工具服务。
 * 负责 token 的生成、解析、签名校验和有效期判断，是整个无状态认证方案的基础组件。
 *
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.previous-secrets:}")
    private String jwtPreviousSecrets;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * 从 token 中提取用户名。
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从 token 中提取指定 claim。
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 生成基础 JWT Token。
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * 生成带扩展 claims 的 JWT Token。
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * 构建 JWT。
     * subject 固定写用户名，签名算法使用 HS256，过期时间由配置项控制。
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getPrimarySignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 校验 token 是否属于指定用户且尚未过期。
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * 粗粒度校验 token 是否可用。
     * 主要给 WebSocket 握手这种只需要知道 token 是否过期/损坏的场景使用。
     */
    public boolean validateToken(String token) {
        try {
            if (isTokenExpired(token)) {
                log.debug("Token 已过期");
                return false;
            }
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token 已过期");
            return false;
        } catch (Exception e) {
            log.debug("Token 验证失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 预留的 token 拉黑接口。
     * 当前项目采用短期 JWT + 前端本地删除 token 的退出模式，因此这里暂未启用。
     */
    @SuppressWarnings("unused")
    public void blacklistToken(String token) {
        log.debug("Token 黑名单功能已禁用");
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        RuntimeException lastException = null;

        for (Key signInKey : getValidationKeys()) {
            try {
                return Jwts.parserBuilder()
                        .setSigningKey(signInKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (ExpiredJwtException e) {
                // 只要某把密钥能正确验签，过期也应按“真实过期”处理，而不是继续尝试其他密钥。
                throw e;
            } catch (RuntimeException e) {
                lastException = e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new IllegalStateException("JWT signing keys are not configured");
    }

    private Key getPrimarySignInKey() {
        return decodeSignInKey(jwtSecret);
    }

    private List<Key> getValidationKeys() {
        Set<String> secrets = new LinkedHashSet<>();
        if (StringUtils.hasText(jwtSecret)) {
            secrets.add(jwtSecret.trim());
        }

        if (StringUtils.hasText(jwtPreviousSecrets)) {
            Arrays.stream(jwtPreviousSecrets.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(secrets::add);
        }

        return secrets.stream()
                .map(this::decodeSignInKey)
                .collect(Collectors.toList());
    }

    private Key decodeSignInKey(String secret) {
        // 配置中的 secret 以 Base64 形式存储，这里先解码再生成 HMAC 密钥。
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
