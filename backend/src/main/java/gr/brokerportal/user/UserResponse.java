package gr.brokerportal.user;

import java.util.UUID;

public record UserResponse(UUID id, String email, String fullName, Role role, String brokerageName) {

    public static UserResponse of(User user) {
        String brokerage = user.getBrokerage() == null ? null : user.getBrokerage().getName();
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), brokerage);
    }
}
