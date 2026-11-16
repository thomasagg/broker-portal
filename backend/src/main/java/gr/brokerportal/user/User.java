package gr.brokerportal.user;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    private UUID id;

    private String email;

    private String passwordHash;

    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    private Brokerage brokerage;

    protected User() {
    }

    private User(String email, String passwordHash, String fullName, Role role, Brokerage brokerage) {
        this.id = UUID.randomUUID();
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.brokerage = brokerage;
    }

    public static User broker(String email, String passwordHash, String fullName, Brokerage brokerage) {
        return new User(email, passwordHash, fullName, Role.BROKER, brokerage);
    }

    public static User underwriter(String email, String passwordHash, String fullName) {
        return new User(email, passwordHash, fullName, Role.UNDERWRITER, null);
    }

    public boolean isBroker() {
        return role == Role.BROKER;
    }

    public boolean isUnderwriter() {
        return role == Role.UNDERWRITER;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public Brokerage getBrokerage() {
        return brokerage;
    }
}
