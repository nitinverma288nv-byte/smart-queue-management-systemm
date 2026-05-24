package com.smartqueue.app.security;

import com.smartqueue.app.entity.User;
import com.smartqueue.app.entity.Staff;
import com.smartqueue.app.repository.StaffRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    @Autowired
    private StaffRepository staffRepository;

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("staff_type", user.getStaffType());
        claims.put("sector_type", user.getStaffType());

        if (user.getRole() == com.smartqueue.app.entity.Role.ROLE_STAFF) {
            java.util.Optional<Staff> staffOpt = staffRepository.findByUserId(user.getId());
            if (staffOpt.isPresent()) {
                Staff staff = staffOpt.get();
                claims.put("branch_id", staff.getReferenceId());
                if (staff.getCounter() != null) {
                    claims.put("assigned_counter", staff.getCounter().getCounterName());
                } else {
                    claims.put("assigned_counter", null);
                }
            } else {
                claims.put("branch_id", null);
                claims.put("assigned_counter", null);
            }
        } else {
            claims.put("branch_id", null);
            claims.put("assigned_counter", null);
        }

        return createToken(claims, user.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
