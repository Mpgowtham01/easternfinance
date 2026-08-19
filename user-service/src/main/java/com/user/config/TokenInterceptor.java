package com.user.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class TokenInterceptor {

    public static String extractInvestorIdFromToken(String token, String secretKey)
    {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid token format. Must start with 'Bearer '");
        }

        String rawToken = token.replace("Bearer ", "");

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseClaimsJws(rawToken)
                .getBody();

        // Extract and return the investor_id claim
        String investor_id = String.valueOf(claims.get("investor_id"));

        if(investor_id == null || investor_id.isEmpty() || investor_id.equals("0"))
        {
            investor_id = "";
        }

        return investor_id;
    }

    public static String extractAdminIdFromToken(String token, String secretKey)
    {
        if (token == null || !token.startsWith("Bearer "))
        {
            throw new IllegalArgumentException("Invalid token format. Must start with 'Bearer '");
        }

        String rawToken = token.replace("Bearer ", "");

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseClaimsJws(rawToken)
                .getBody();

        // Extract and return the investor_id claim
        String user_id = String.valueOf(claims.get("user_id"));

        if(user_id == null || user_id.isEmpty() || user_id.equals("0"))
        {
            user_id = "";
        }

        return user_id;
    }
    public static String extractClientNamedFromToken(String token, String secretKey)
    {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid token format. Must start with 'Bearer '");
        }

        String rawToken = token.replace("Bearer ", "");

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseClaimsJws(rawToken)
                .getBody();

        // Extract and return the investor_id claim
        String client_name = String.valueOf(claims.get("client_name"));

        if(client_name == null || client_name.isEmpty() || client_name.equals("0"))
        {
            client_name = "";
        }

        return client_name;
    }
}
