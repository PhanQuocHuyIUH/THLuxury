package com.thluxury.identity.service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.thluxury.identity.config.JwtProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HexFormat;
import java.util.List;

/**
 * Manage the RSA keypair used for JWT RS256 signing.
 * On startup: load key from disk; if absent, generate a new 2048-bit pair and persist.
 * Exposes the JWK Set (public key) via {@link #getJwkSet()} for the JWKS endpoint.
 */
@Component
public class JwtKeyManager {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyManager.class);

    private final JwtProperties props;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private String kid;

    public JwtKeyManager(JwtProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() throws Exception {
        Path privPath = Path.of(props.privateKeyPath());
        Path pubPath  = Path.of(props.publicKeyPath());

        if (Files.exists(privPath) && Files.exists(pubPath)) {
            log.info("Loading existing JWT keypair from {} / {}", privPath, pubPath);
            this.privateKey = loadPrivateKey(Files.readAllBytes(privPath));
            this.publicKey  = loadPublicKey(Files.readAllBytes(pubPath));
        } else {
            log.warn("JWT keypair not found — generating a new RSA-2048 pair at {} / {}", privPath, pubPath);
            KeyPair kp = generate();
            this.privateKey = (RSAPrivateKey) kp.getPrivate();
            this.publicKey  = (RSAPublicKey)  kp.getPublic();
            Files.createDirectories(privPath.getParent());
            Files.write(privPath, this.privateKey.getEncoded());
            Files.write(pubPath,  this.publicKey.getEncoded());
        }

        // kid = first 16 hex chars of SHA-256(public key DER)
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(this.publicKey.getEncoded());
        this.kid = HexFormat.of().formatHex(hash).substring(0, 16);
        log.info("JWT keypair ready. kid={}", kid);
    }

    private KeyPair generate() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    private RSAPrivateKey loadPrivateKey(byte[] der) throws Exception {
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private RSAPublicKey loadPublicKey(byte[] der) throws Exception {
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    public RSAPrivateKey privateKey() { return privateKey; }
    public RSAPublicKey publicKey()   { return publicKey;  }
    public String kid()                { return kid;        }

    public JWKSet getJwkSet() {
        RSAKey jwk = new RSAKey.Builder(publicKey)
                .keyID(kid)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .build();
        return new JWKSet(List.of(jwk));
    }
}
