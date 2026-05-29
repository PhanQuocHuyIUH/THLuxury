package com.thluxury.identity.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.thluxury.identity.config.JwtProperties;
import com.thluxury.identity.domain.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public record IssuedToken(String token, String jti, Instant expiresAt) {}

    private final JwtKeyManager keys;
    private final JwtProperties props;

    public JwtService(JwtKeyManager keys, JwtProperties props) {
        this.keys = keys;
        this.props = props;
    }

    public IssuedToken issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.accessTtlMinutes(), ChronoUnit.MINUTES);
        String jti = UUID.randomUUID().toString();

        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(props.issuer())
                .subject(user.getId().toString())
                .audience("thluxury")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .jwtID(jti)
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("typ", "access");
        if (user.getBranchId() != null) {
            claims.claim("branchId", user.getBranchId().toString());
        }
        return sign(claims.build(), jti, exp);
    }

    public IssuedToken issueRefreshToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.refreshTtlDays(), ChronoUnit.DAYS);
        String jti = UUID.randomUUID().toString();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(props.issuer())
                .subject(user.getId().toString())
                .audience("thluxury")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .jwtID(jti)
                .claim("typ", "refresh")
                .build();
        return sign(claims, jti, exp);
    }

    private IssuedToken sign(JWTClaimsSet claims, String jti, Instant exp) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(keys.kid())
                    .type(JOSEObjectType.JWT)
                    .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new RSASSASigner(keys.privateKey()));
            return new IssuedToken(jwt.serialize(), jti, exp);
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    /** Parse & verify a JWT signature using our own public key. */
    public JWTClaimsSet parseAndVerify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new RSASSAVerifier(keys.publicKey()))) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                throw new IllegalArgumentException("Token expired");
            }
            return claims;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT: " + e.getMessage(), e);
        }
    }
}
