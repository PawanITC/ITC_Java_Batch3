package com.itc.funkart.service;

import com.itc.funkart.entity.User;
import com.itc.funkart.exceptions.AlreadyExistsException;
import com.itc.funkart.exceptions.BadRequestException;
import com.itc.funkart.exceptions.UnauthorizedException;
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
        //validate sign up details
        validateSignupInput(email, password, name);
        //check email already exists in the database
        checkEmailExists(email);
        //hash password of new user
        String hashedPassword = hashPassword(password);
        User newUser = new User(email, hashedPassword, name);
        //save new user to repository
        return userRepository.save(newUser);
    }


    public User login(String email, String password) {
        //validate login details
        validateLoginInput(email, password);
        //fetch user email
        User user = fetchUserByEmail(email);
        //check password is correct
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
        // For now plain text; replace with hash comparison later
        if (!rawPassword.equals(storedPassword)) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    /**
     * A hashed password
     *
     * @param rawPassword password
     * @return successfully hashed password
     */
    private String hashPassword(String rawPassword) {
        // Barebones placeholder; in production use BCrypt or Argon2
        return rawPassword;
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

}
