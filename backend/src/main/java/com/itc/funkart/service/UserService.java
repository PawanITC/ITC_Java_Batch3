package com.itc.funkart.service;

import com.itc.funkart.entity.User;
import com.itc.funkart.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ------------------------
    // Service Login/Signup Methods
    // ------------------------

    public User signUp(String email, String password, String name) {
        checkEmailExists(email);
        String hashedPassword = hashPassword(password);
        User newUser = new User(email, hashedPassword, name);
        return userRepository.save(newUser);
    }


    public User login(String email, String password) {
        User user = fetchUserByEmail(email);
        validatePassword(password, user.getPassword());
        return user;
    }


    // ------------------------
    // Helper Methods
    // ------------------------

    /**
     * Check database if email exists
     * @param email email of the user
     */
    private void checkEmailExists(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
    }

    /**
     * helper to compare password passed to password stored in db
     * @param rawPassword password passed
     * @param storedPassword stored password
     */
    private void validatePassword(String rawPassword, String storedPassword) {
        // For now plain text; replace with hash comparison later
        if (!rawPassword.equals(storedPassword)) {
            throw new RuntimeException("Invalid email or password");
        }
    }

    /**
     * A hashed password
     * @param rawPassword password
     * @return successfully hashed password
     */
    private String hashPassword(String rawPassword) {
        // Barebones placeholder; in production use BCrypt or Argon2
        return rawPassword;
    }

    /**
     * Check db for email existing
     * @param email users email
     */
    private User fetchUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
    }

}
