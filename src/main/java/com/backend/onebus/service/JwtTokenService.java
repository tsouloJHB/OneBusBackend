package com.backend.onebus.service;

import com.backend.onebus.model.User;
import com.backend.onebus.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtTokenService(
            @Value("${security.jwt.secret:dev-secret-change-me}") String secret,
            @Value("${security.jwt.expiration-minutes:720}") long expirationMinutes
    ) {
        // If the provided secret is short, pad it to ensure valid key length
        String padded = secret.length() < 32 ? String.format("%-32s", secret).replace(' ', '0') : secret;
        this.secretKey = Keys.hmacShaKeyFor(padded.getBytes());
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("name", user.getFullName())
                .claim("companyId", user.getCompanyId())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parse JWT token and extract claims
     * @throws JwtException if token is invalid or expired
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * Extract email (subject) from token
     */
    public String getEmailFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * Extract role from token
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * Extract name from token
     */
    public String getNameFromToken(String token) {
        return parseToken(token).get("name", String.class);
    }

    /**
     * Extract company ID from token
     */
    public Long getCompanyIdFromToken(String token) {
        Integer companyId = parseToken(token).get("companyId", Integer.class);
        return companyId != null ? companyId.longValue() : null;
    }

    public Instant getExpiryInstant() {
        return Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
    }
}
