package gr.brokerportal.auth;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import gr.brokerportal.user.User;
import gr.brokerportal.user.UserRepository;

@Component
public class CurrentUser {

    private final UserRepository users;

    CurrentUser(UserRepository users) {
        this.users = users;
    }

    // a token can outlive its user, that's still a 401
    public User from(Jwt jwt) {
        return users.findById(UUID.fromString(jwt.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
