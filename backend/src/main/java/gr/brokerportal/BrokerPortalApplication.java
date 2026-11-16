package gr.brokerportal;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BrokerPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrokerPortalApplication.class, args);
    }

    // quote expiry and "days in status" depend on the date, tests swap this for a fixed clock
    @Bean
    Clock clock(@Value("${app.zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
