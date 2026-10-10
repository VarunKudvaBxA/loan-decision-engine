package com.loanengine.config;

import com.loanengine.domain.AppUser;
import com.loanengine.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** demo accounts, change or remove these before putting anything real behind this */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        create("analyst", "analyst123", "ANALYST");
        create("analyst2", "analyst123", "ANALYST");
        create("senior", "senior123", "SENIOR");
    }

    private void create(String username, String password, String role) {
        if (users.findByUsername(username).isEmpty()) {
            users.save(new AppUser(username, encoder.encode(password), role));
        }
    }
}
