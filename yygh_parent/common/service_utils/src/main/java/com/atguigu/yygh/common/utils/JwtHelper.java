package com.atguigu.yygh.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;


public class JwtHelper {

    //token过期时间固定值
    private static long tokenExpiration = 60 * 60 * 1000L;

    //加密秘钥
    private static SecretKey tokenSignKey =
            Keys.hmacShaKeyFor("M0PKKI6pYGVWWfDZw90a0lTpGYX1d4AQ".getBytes());

    //生成token方法，参数用户id和用户名称
    public static String createToken(Long userId, String username) {
        String token = Jwts.builder().
                //设置分类
                        setSubject("USER_INFO").
                //设置生成token过期时间
                        setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
                //jwt负载内容，设置用户信息到token
                .claim("userId", userId)
                .claim("userName", username).
                //根据秘钥进行加密
                        signWith(tokenSignKey).
                //把生成token压缩
                        compressWith(CompressionCodecs.GZIP).
                compact();
        return token;
    }

    //根据token字符串，从token获取userid
    public static Long getUserId(String token) {
        if(StringUtils.isEmpty(token)) return null;
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(tokenSignKey).parseClaimsJws(token);
        Claims claims = claimsJws.getBody();
        Integer userId = (Integer)claims.get("userId");
        return userId.longValue();
    }

    //根据token字符串，从token获取UserName
    public static String getUserName(String token) {
        if(StringUtils.isEmpty(token)) return "";
        Jws<Claims> claimsJws
                = Jwts.parser().setSigningKey(tokenSignKey).parseClaimsJws(token);
        Claims claims = claimsJws.getBody();
        return (String)claims.get("userName");
    }

    public static void main(String[] args) {
        String token = JwtHelper.createToken(1L, "lucy");
        System.out.println(token);
        System.out.println(JwtHelper.getUserId(token));
        System.out.println(JwtHelper.getUserName(token));
    }
}
