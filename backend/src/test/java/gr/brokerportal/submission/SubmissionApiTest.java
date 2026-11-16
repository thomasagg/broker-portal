package gr.brokerportal.submission;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import gr.brokerportal.TestcontainersConfiguration;
import gr.brokerportal.user.Brokerage;
import gr.brokerportal.user.BrokerageRepository;
import gr.brokerportal.user.User;
import gr.brokerportal.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestcontainersConfiguration.class)
class SubmissionApiTest {

    static final LocalDate START = LocalDate.now().plusMonths(1);

    @Autowired
    MockMvcTester mvc;

    @Autowired
    UserRepository users;

    @Autowired
    BrokerageRepository brokerages;

    @Autowired
    PasswordEncoder passwords;

    String maria;
    String nikos;
    String eleni;
    String anna;
    String george;

    @BeforeEach
    void setUp() {
        String hash = passwords.encode("secret123");
        Brokerage aegean = brokerages.save(new Brokerage("Aegean Test Brokers"));
        Brokerage northern = brokerages.save(new Brokerage("Northern Test Partners"));
        users.save(User.broker("maria@aegean.test", hash, "Maria Papadopoulou", aegean));
        users.save(User.broker("nikos@aegean.test", hash, "Nikos Georgiou", aegean));
        users.save(User.broker("eleni@northern.test", hash, "Eleni Dimou", northern));
        users.save(User.underwriter("anna@insurer.test", hash, "Anna Ioannou"));
        users.save(User.underwriter("george@insurer.test", hash, "George Katsaros"));
        maria = login("maria@aegean.test");
        nikos = login("nikos@aegean.test");
        eleni = login("eleni@northern.test");
        anna = login("anna@insurer.test");
        george = login("george@insurer.test");
    }

    @Test
    void wrongPasswordIsRejected() {
        assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "maria@aegean.test", "password": "nope"}
                        """))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "nobody@aegean.test", "password": "secret123"}
                        """))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void apiNeedsAToken() {
        assertThat(mvc.get().uri("/api/submissions")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/submissions").header("Authorization", "Bearer not-a-jwt"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meReturnsTheBrokerage() {
        assertThat(mvc.get().uri("/api/auth/me").header("Authorization", maria))
                .hasStatusOk()
                .bodyJson().extractingPath("$.brokerageName").isEqualTo("Aegean Test Brokers");
    }

    @Test
    void underwritersCannotCreateSubmissions() {
        assertThat(mvc.post().uri("/api/submissions").header("Authorization", anna)
                .contentType(MediaType.APPLICATION_JSON).content(completeJson("Kastra Events")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void submittingAnIncompleteDraftListsTheMissingFields() {
        String id = create(maria, """
                {"lineOfBusiness": "CYBER", "insuredName": "Olympus Dental"}
                """);

        MvcTestResult result = action(maria, id, """
                {"action": "SUBMIT"}
                """);

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.missingFields").asArray()
                .contains("sumInsured", "coverageStart", "riskDescription");
    }

    @Test
    void otherBrokeragesAndUnderwritersCannotSeeADraft() {
        String id = create(maria, completeJson("Thermaikos Logistics"));

        assertThat(mvc.get().uri("/api/submissions/" + id).header("Authorization", nikos)).hasStatusOk();
        assertThat(mvc.get().uri("/api/submissions/" + id).header("Authorization", eleni))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/submissions/" + id).header("Authorization", anna))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void walksTheWholeLifecycleAndKeepsATimeline() {
        String id = create(maria, completeJson("Halkidiki Olive Exports"));
        assertThat(action(maria, id, "{\"action\": \"SUBMIT\"}")).hasStatusOk();

        MvcTestResult picked = action(anna, id, "{\"action\": \"START_REVIEW\"}");
        assertThat(picked).bodyJson().extractingPath("$.submission.underwriterName").isEqualTo("Anna Ioannou");
        assertThat(picked).bodyJson().extractingPath("$.submission.availableActions").asArray()
                .containsExactly("REQUEST_INFO", "QUOTE", "DECLINE");

        assertThat(action(george, id, "{\"action\": \"QUOTE\", \"premium\": 1, \"validUntil\": \"%s\"}"
                .formatted(START))).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.detail").isEqualTo("Anna Ioannou is handling this submission");

        assertThat(action(anna, id, """
                {"action": "QUOTE", "premium": 9750.00, "validUntil": "%s", "terms": "EUR 2,500 deductible"}
                """.formatted(START))).hasStatusOk()
                .bodyJson().extractingPath("$.submission.status").isEqualTo("QUOTED");

        assertThat(action(nikos, id, "{\"action\": \"REQUEST_BIND\"}")).hasStatusOk();
        assertThat(action(anna, id, "{\"action\": \"BIND\"}")).hasStatusOk();
        MvcTestResult booked = action(anna, id, "{\"action\": \"BOOK\"}");

        assertThat(booked).bodyJson().extractingPath("$.submission.policyNumber").asString().startsWith("POL-");
        assertThat(booked).bodyJson().extractingPath("$.events[*].toStatus").asArray().containsExactly(
                "DRAFT", "SUBMITTED", "IN_REVIEW", "QUOTED", "BIND_REQUESTED", "BOUND", "BOOKED");
        assertThat(booked).bodyJson().extractingPath("$.events[4].actorName").isEqualTo("Nikos Georgiou");
    }

    @Test
    void infoRequestsGoBackAndForthWithEdits() {
        String id = submitted(maria, "Olympus Dental");
        action(anna, id, "{\"action\": \"START_REVIEW\"}");
        assertThat(action(anna, id, "{\"action\": \"REQUEST_INFO\"}")).hasStatus(HttpStatus.BAD_REQUEST);
        action(anna, id, "{\"action\": \"REQUEST_INFO\", \"note\": \"Is MFA on for remote access?\"}");

        assertThat(mvc.put().uri("/api/submissions/" + id).header("Authorization", maria)
                .contentType(MediaType.APPLICATION_JSON)
                .content(completeJson("Olympus Dental Clinics")))
                .hasStatusOk();
        MvcTestResult answered = action(maria, id, """
                {"action": "PROVIDE_INFO", "note": "Yes, MFA everywhere, name corrected too"}
                """);

        assertThat(answered).bodyJson().extractingPath("$.submission.status").isEqualTo("IN_REVIEW");
        assertThat(answered).bodyJson().extractingPath("$.submission.insuredName")
                .isEqualTo("Olympus Dental Clinics");
    }

    @Test
    void bothSidesCanMessageOnASubmission() {
        String id = submitted(maria, "Kastra Events");

        mvc.post().uri("/api/submissions/" + id + "/messages").header("Authorization", anna)
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"Any claims in the last 5 years?\"}")
                .exchange();
        MvcTestResult reply = mvc.post().uri("/api/submissions/" + id + "/messages").header("Authorization", maria)
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"None\"}").exchange();

        assertThat(reply).bodyJson().extractingPath("$.messages[*].authorRole").asArray()
                .containsExactly("UNDERWRITER", "BROKER");
        assertThat(mvc.post().uri("/api/submissions/" + id + "/messages").header("Authorization", eleni)
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"hi\"}"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void copyingStartsANewDraftOneYearLater() {
        String id = submitted(maria, "Vergina Tech");

        MvcTestResult copy = mvc.post().uri("/api/submissions/" + id + "/copy").header("Authorization", maria)
                .exchange();

        assertThat(copy).hasStatus(HttpStatus.CREATED);
        assertThat(copy).bodyJson().extractingPath("$.submission.status").isEqualTo("DRAFT");
        assertThat(copy).bodyJson().extractingPath("$.submission.coverageStart")
                .isEqualTo(START.plusYears(1).toString());
    }

    @Test
    void listsAreScopedAndFilterable() {
        submitted(maria, "Thermaikos Logistics");
        create(maria, completeJson("Pella Cold Storage"));
        submitted(eleni, "Vergina Tech");

        assertThat(namesFor(maria, "")).containsExactlyInAnyOrder("Thermaikos Logistics", "Pella Cold Storage");
        assertThat(namesFor(maria, "?status=DRAFT")).containsExactly("Pella Cold Storage");
        assertThat(namesFor(maria, "?q=therm")).containsExactly("Thermaikos Logistics");
        assertThat(namesFor(anna, "?q=e")).contains("Thermaikos Logistics", "Vergina Tech")
                .doesNotContain("Pella Cold Storage");
    }

    @Test
    void dashboardShowsWhatIsWaitingOnMe() {
        String id = submitted(maria, "Lefkos Architects");

        assertThat(mvc.get().uri("/api/dashboard").header("Authorization", anna)).hasStatusOk()
                .bodyJson().extractingPath("$.waitingOnMe[*].id").asArray().contains(id);

        action(anna, id, "{\"action\": \"START_REVIEW\"}");
        action(anna, id, "{\"action\": \"REQUEST_INFO\", \"note\": \"Claims history please\"}");
        // only decline is left for anna, that's not her move
        assertThat(mvc.get().uri("/api/dashboard").header("Authorization", anna))
                .bodyJson().extractingPath("$.waitingOnMe[*].id").asArray().doesNotContain(id);
        action(maria, id, "{\"action\": \"PROVIDE_INFO\", \"note\": \"None in 5 years\"}");
        action(anna, id, """
                {"action": "QUOTE", "premium": 3200, "validUntil": "%s"}
                """.formatted(LocalDate.now().plusDays(3)));

        MvcTestResult dashboard = mvc.get().uri("/api/dashboard").header("Authorization", maria).exchange();
        assertThat(dashboard).bodyJson().extractingPath("$.countsByStatus.QUOTED").isEqualTo(1);
        assertThat(dashboard).bodyJson().extractingPath("$.waitingOnMe[*].id").asArray().containsExactly(id);
        assertThat(dashboard).bodyJson().extractingPath("$.expiringQuotes[*].id").asArray().containsExactly(id);
        assertThat(dashboard).bodyJson().extractingPath("$.turnaround.quotedSample").isNotNull();
    }

    private List<String> namesFor(String auth, String query) {
        return JsonPath.read(body(mvc.get().uri("/api/submissions" + query).header("Authorization", auth).exchange()),
                "$[*].insuredName");
    }

    private String login(String email) {
        MvcTestResult result = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"secret123\"}".formatted(email)).exchange();
        return "Bearer " + JsonPath.read(body(result), "$.token");
    }

    private String create(String auth, String json) {
        MvcTestResult result = mvc.post().uri("/api/submissions").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(body(result), "$.submission.id");
    }

    private String submitted(String auth, String insured) {
        String id = create(auth, completeJson(insured));
        assertThat(action(auth, id, "{\"action\": \"SUBMIT\"}")).hasStatusOk();
        return id;
    }

    private MvcTestResult action(String auth, String id, String json) {
        return mvc.post().uri("/api/submissions/" + id + "/actions").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String body(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String completeJson(String insured) {
        return """
                {"lineOfBusiness": "PROPERTY", "insuredName": "%s", "insuredCountry": "GR",
                 "insuredIndustry": "Logistics", "sumInsured": 2000000, "currency": "EUR",
                 "coverageStart": "%s", "coverageEnd": "%s", "riskDescription": "Sprinklered, guarded 24/7"}
                """.formatted(insured, START, START.plusYears(1));
    }
}
