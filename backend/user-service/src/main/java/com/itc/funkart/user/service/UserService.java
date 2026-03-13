package com.itc.funkart.user.service;

import com.itc.funkart.user.dto.user.LoginRequest;
import com.itc.funkart.user.dto.user.SignupRequest;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.AlreadyExistsException;
import com.itc.funkart.user.exceptions.BadRequestException;
import com.itc.funkart.user.exceptions.UnauthorizedException;
import com.itc.funkart.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------------
    // Service Login/Signup Methods
    // ------------------------

    public User signUp(SignupRequest request) {
        String email = request.email();
        String password = request.password();
        String name = request.name();

        validateSignupInput(email, password, name);
        checkEmailExists(email);

        String hashedPassword = hashPassword(password);
        User newUser = new User(email, hashedPassword, name);

        return userRepository.save(newUser);
    }

    public User login(LoginRequest request) {
        String email = request.email();
        String password = request.password();

        validateLoginInput(email, password);

        User user = fetchUserByEmail(email);
        validatePassword(password, user.getPassword());

        return user;
    }


    // ------------------------
    // Validation Methods
    // ------------------------

    private void validateSignupInput(String email, String password, String name) {

        emailAndPasswordCheck(email, password);

        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }

    }

    private void validateLoginInput(String email, String password) {
        emailAndPasswordCheck(email, password);
    }

    private void emailAndPasswordCheck(String email, String password) {

        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }
    }


    // ------------------------
    // Helper Methods
    // ------------------------

    /**
     * Check database if email exists
     *
     * @param email email of the user
     */
    private void checkEmailExists(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new AlreadyExistsException("Email already registered");
        }
    }

    /**
     * helper to compare password passed to password stored in db
     *
     * @param rawPassword    password passed
     * @param storedPassword stored password
     */
    private void validatePassword(String rawPassword, String storedPassword) {
        if (!passwordEncoder.matches(rawPassword, storedPassword)) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    /**
     * Method to hash the password
     *
     * @param rawPassword password
     * @return successfully hashed password
     */
    private String hashPassword(String rawPassword) {
        //hashing the password
       return passwordEncoder.encode(rawPassword);
    }

    /**
     * Check db for email existing
     *
     * @param email users email
     */
    private User fetchUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
    }

    /**
     * Check db for user existing
     *
     * @param id users id
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

}
