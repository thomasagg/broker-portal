package gr.brokerportal.submission;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import gr.brokerportal.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/submissions")
class SubmissionController {

    private final SubmissionService service;
    private final CurrentUser currentUser;

    SubmissionController(SubmissionService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    record NewMessage(@NotBlank @Size(max = 4000) String body) {
    }

    @GetMapping
    List<SubmissionResponse> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) List<SubmissionStatus> status,
            @RequestParam(required = false) String q) {
        return service.list(currentUser.from(jwt), status, q);
    }

    @GetMapping("/{id}")
    SubmissionDetail get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(currentUser.from(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SubmissionDetail create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SubmissionDetails details) {
        return service.create(currentUser.from(jwt), details);
    }

    @PutMapping("/{id}")
    SubmissionDetail update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody SubmissionDetails details) {
        return service.update(currentUser.from(jwt), id, details);
    }

    @PostMapping("/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    SubmissionDetail copy(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.copy(currentUser.from(jwt), id);
    }

    @PostMapping("/{id}/actions")
    SubmissionDetail perform(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ActionRequest request) {
        return service.perform(currentUser.from(jwt), id, request);
    }

    @PostMapping("/{id}/messages")
    SubmissionDetail addMessage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody NewMessage message) {
        return service.addMessage(currentUser.from(jwt), id, message.body());
    }
}
