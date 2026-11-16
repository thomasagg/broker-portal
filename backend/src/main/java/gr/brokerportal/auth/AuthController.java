package gr.brokerportal.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import gr.brokerportal.user.User;
import gr.brokerportal.user.UserRepository;
import gr.brokerportal.user.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final CurrentUser currentUser;
    // checked against when the email is unknown, so that case takes as long as a wrong password
    private final String dummyHash;

    AuthController(UserRepository users, PasswordEncoder passwords, TokenService tokens, CurrentUser currentUser) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.currentUser = currentUser;
        this.dummyHash = passwords.encode("not-a-real-password");
    }

    record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    record LoginResponse(String token, UserResponse user) {
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmail(request.email().trim().toLowerCase()).orElse(null);
        boolean matches = passwords.matches(request.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }
        return new LoginResponse(tokens.issue(user), UserResponse.of(user));
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.of(currentUser.from(jwt));
    }
}
