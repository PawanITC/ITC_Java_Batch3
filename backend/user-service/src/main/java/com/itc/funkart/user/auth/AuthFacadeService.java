package com.itc.funkart.user.auth;

import com.itc.funkart.user.dto.user.LoginRequest;
import com.itc.funkart.user.dto.user.OAuthResponse;
import com.itc.funkart.user.dto.user.SignupRequest;
import com.itc.funkart.user.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.mapper.UserMapper;
import com.itc.funkart.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <h2>Auth Facade Service</h2>
 * <p>
 * Orchestrates authentication flows across:
 * OAuth, user-service, JWT generation, and response mapping.
 */
@Service
@RequiredArgsConstructor
public class AuthFacadeService {

    private final GithubOAuthService githubOAuthService;
    private final UserService userService;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final PrincipalFactory principalFactory;


    /**
     * GitHub OAuth login flow.
     */
    public OAuthResponse handleGithubLogin(String code) {
        User user = githubOAuthService.processCode(code);
        userService.recordLogin(user, "github");
        String token = jwtService.generateJwtToken(principalFactory.create(user));
        return userMapper.toOAuthResponse(user, token);
    }

    /**
     * Email/password login flow.
     */
    public SuccessfulLoginResponse login(LoginRequest request) {
        User user = userService.login(request);
        userService.recordLogin(user, "email");
        String token = jwtService.generateJwtToken(principalFactory.create(user));
        return userMapper.toResponse(user, token);
    }

    /**
     * Email/password signup flow.
     */
    public SuccessfulLoginResponse signup(SignupRequest request) {
        User user = userService.signUp(request);
        userService.recordLogin(user, "email");
        String token = jwtService.generateJwtToken(principalFactory.create(user));
        return userMapper.toResponse(user, token);
    }
}