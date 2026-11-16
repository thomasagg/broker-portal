package gr.brokerportal.user;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BrokerageRepository extends JpaRepository<Brokerage, UUID> {
}
