package gr.brokerportal.demo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import gr.brokerportal.submission.ActionRequest;
import gr.brokerportal.submission.LineOfBusiness;
import gr.brokerportal.submission.Submission;
import gr.brokerportal.submission.SubmissionAction;
import gr.brokerportal.submission.SubmissionDetails;
import gr.brokerportal.submission.SubmissionEvent;
import gr.brokerportal.submission.SubmissionEventRepository;
import gr.brokerportal.submission.SubmissionMessage;
import gr.brokerportal.submission.SubmissionMessageRepository;
import gr.brokerportal.submission.SubmissionRepository;
import gr.brokerportal.submission.SubmissionStatus;
import gr.brokerportal.user.Brokerage;
import gr.brokerportal.user.BrokerageRepository;
import gr.brokerportal.user.User;
import gr.brokerportal.user.UserRepository;

// run with --spring.profiles.active=demo to get a few users and submissions in every status
@Component
@Profile("demo")
class DemoData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoData.class);
    static final String PASSWORD = "demo1234";

    private final BrokerageRepository brokerages;
    private final UserRepository users;
    private final SubmissionRepository submissions;
    private final SubmissionEventRepository events;
    private final SubmissionMessageRepository messages;
    private final PasswordEncoder passwords;
    private final Clock clock;

    DemoData(BrokerageRepository brokerages, UserRepository users, SubmissionRepository submissions,
            SubmissionEventRepository events, SubmissionMessageRepository messages, PasswordEncoder passwords,
            Clock clock) {
        this.brokerages = brokerages;
        this.users = users;
        this.submissions = submissions;
        this.events = events;
        this.messages = messages;
        this.passwords = passwords;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        String hash = passwords.encode(PASSWORD);
        Brokerage aegean = brokerages.save(new Brokerage("Aegean Insurance Brokers"));
        Brokerage northern = brokerages.save(new Brokerage("Northern Risk Partners"));
        User maria = users.save(User.broker("maria@aegean.example", hash, "Maria Papadopoulou", aegean));
        User nikos = users.save(User.broker("nikos@aegean.example", hash, "Nikos Georgiou", aegean));
        User eleni = users.save(User.broker("eleni@northern.example", hash, "Eleni Dimou", northern));
        User anna = users.save(User.underwriter("anna@insurer.example", hash, "Anna Ioannou"));
        User george = users.save(User.underwriter("george@insurer.example", hash, "George Katsaros"));

        new Story(maria, details(LineOfBusiness.PROPERTY, "Thermaikos Logistics SA", "Warehousing", 4_500_000,
                "Two bonded warehouses in Kalochori, sprinklered, 24/7 guarded."), 2);

        new Story(maria, details(LineOfBusiness.CYBER, "Olympus Dental Clinics", "Healthcare", 800_000,
                "Six clinics sharing one patient system, backups offsite daily."), 9)
                .act(anna, SubmissionAction.START_REVIEW, null, 8)
                .act(anna, SubmissionAction.REQUEST_INFO, "Do you have MFA on remote access? Please attach the last pen test summary.", 7);

        new Story(nikos, details(LineOfBusiness.GENERAL_LIABILITY, "Kastra Events Ltd", "Event organising", 1_200_000,
                "Concerts up to 3,000 people, outdoor venues in Northern Greece."), 12)
                .act(george, SubmissionAction.START_REVIEW, null, 4)
                .quote(george, 18_400, 5, 3);

        new Story(maria, details(LineOfBusiness.MARINE_CARGO, "Halkidiki Olive Exports", "Food export", 2_000_000,
                "Olive oil in containers, Thessaloniki port to Rotterdam and Hamburg."), 20)
                .act(anna, SubmissionAction.START_REVIEW, null, 19)
                .quote(anna, 9_750, 25, 16)
                .act(maria, SubmissionAction.REQUEST_BIND, null, 14)
                .act(anna, SubmissionAction.BIND, null, 13)
                .act(anna, SubmissionAction.BOOK, null, 12);

        new Story(eleni, details(LineOfBusiness.DIRECTORS_AND_OFFICERS, "Vergina Tech SA", "Software", 15_000_000,
                "Listed on Athens Exchange since 2021, board of seven."), 6)
                .act(george, SubmissionAction.START_REVIEW, null, 5)
                .quote(george, 42_000, 14, 3)
                .act(eleni, SubmissionAction.REQUEST_BIND, null, 1);

        new Story(eleni, details(LineOfBusiness.PROPERTY, "Pella Cold Storage", "Agriculture", 3_100_000,
                "Ammonia refrigeration plant, built 1988."), 15)
                .act(anna, SubmissionAction.START_REVIEW, null, 14)
                .act(anna, SubmissionAction.DECLINE, "Ammonia plant without a recent survey is outside our appetite.", 13);

        new Story(nikos, details(LineOfBusiness.PROFESSIONAL_INDEMNITY, "Lefkos Architects", "Architecture", 650_000,
                "Residential and small commercial projects."), 1)
                .submit(1);

        log.info("Demo data loaded, log in as maria@aegean.example or anna@insurer.example with password {}",
                PASSWORD);
    }

    private SubmissionDetails details(LineOfBusiness line, String insured, String industry, long sumInsured,
            String risk) {
        LocalDate start = LocalDate.now(clock).plusMonths(1).withDayOfMonth(1);
        return new SubmissionDetails(line, insured, "GR", industry, BigDecimal.valueOf(sumInsured * 2),
                BigDecimal.valueOf(sumInsured), "EUR", start, start.plusYears(1), null, risk);
    }

    // walks one submission through its life, backdated so the timeline and turnaround look real
    private class Story {

        private final Submission submission;

        Story(User broker, SubmissionDetails details, int createdDaysAgo) {
            String reference = "SUB-%d-%06d".formatted(LocalDate.now(clock).getYear(),
                    submissions.nextReferenceNumber());
            submission = submissions.save(Submission.draft(broker, reference, details, daysAgo(createdDaysAgo)));
            events.save(new SubmissionEvent(submission, null, SubmissionStatus.DRAFT, broker, null,
                    daysAgo(createdDaysAgo)));
            if (createdDaysAgo > 1) {
                submit(createdDaysAgo - 1);
            }
        }

        Story submit(int daysAgo) {
            return act(submission.getBroker(), SubmissionAction.SUBMIT, null, daysAgo);
        }

        Story act(User actor, SubmissionAction action, String note, int daysAgo) {
            events.save(submission.perform(actor, ActionRequest.of(action, note), day(daysAgo), daysAgo(daysAgo)));
            if (note != null && action == SubmissionAction.REQUEST_INFO) {
                messages.save(new SubmissionMessage(submission, actor, note, daysAgo(daysAgo)));
            }
            return this;
        }

        Story quote(User underwriter, long premium, int validForDays, int daysAgo) {
            var request = new ActionRequest(SubmissionAction.QUOTE, null, BigDecimal.valueOf(premium),
                    day(daysAgo).plusDays(validForDays), "Standard wording, EUR 2,500 deductible.");
            events.save(submission.perform(underwriter, request, day(daysAgo), daysAgo(daysAgo)));
            return this;
        }

        private Instant daysAgo(int days) {
            return clock.instant().minus(Duration.ofDays(days)).minus(Duration.ofHours(days % 5));
        }

        private LocalDate day(int daysAgo) {
            return LocalDate.ofInstant(daysAgo(daysAgo), clock.getZone());
        }
    }
}
