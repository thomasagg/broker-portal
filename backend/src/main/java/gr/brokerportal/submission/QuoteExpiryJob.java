package gr.brokerportal.submission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class QuoteExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(QuoteExpiryJob.class);

    private final SubmissionService service;

    QuoteExpiryJob(SubmissionService service) {
        this.service = service;
    }

    // a few minutes past the hour, so it doesn't pile up with other hourly jobs
    // also on startup: a free host sleeps when idle and misses the hourly runs, so a quote
    // could sit there past its date until the next :05 after waking up
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 5 * * * *", zone = "${app.zone}")
    void run() {
        int expired = service.expireQuotes();
        if (expired > 0) {
            log.info("Expired {} quotes", expired);
        }
    }
}
