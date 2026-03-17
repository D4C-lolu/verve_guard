package interswitch.academy.verve_guard.services;

import interswitch.academy.verve_guard.configuration.JwtProperties;
import interswitch.academy.verve_guard.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserPrincipal principal) {
        return generateToken(principal, jwtProperties.getAccessTokenExpiry());
    }

    public String generateRefreshToken(UserPrincipal principal) {
        return generateToken(principal, jwtProperties.getRefreshTokenExpiry());
    }

    private String generateToken(UserPrincipal principal, long expiry) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(principal.getUser().getId())
                .claim("email", principal.getUser().getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + (expiry * 1000)))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    public Date extractIssuedAt(String token) {
        return extractAllClaims(token).getIssuedAt();
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
