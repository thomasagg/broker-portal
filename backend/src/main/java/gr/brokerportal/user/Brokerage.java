package gr.brokerportal.user;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Brokerage {

    @Id
    private UUID id;

    private String name;

    protected Brokerage() {
    }

    public Brokerage(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
