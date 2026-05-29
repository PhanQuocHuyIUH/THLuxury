package com.thluxury.identity.config;

import com.thluxury.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sau khi Flyway chạy V2__seed.sql (password_hash NULL), set bcrypt hash
 * cho mật khẩu mặc định "Demo@123" để các seed account login được.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_PASSWORD = "Demo@123";

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String hash = encoder.encode(DEFAULT_PASSWORD);
        int patched = 0;
        for (var u : users.findAll()) {
            if (u.getPasswordHash() == null) {
                u.setPasswordHash(hash);
                patched++;
            }
        }
        if (patched > 0) {
            log.info("DataSeeder: applied default password to {} seed accounts (password = '{}')",
                    patched, DEFAULT_PASSWORD);
        }
    }
}
