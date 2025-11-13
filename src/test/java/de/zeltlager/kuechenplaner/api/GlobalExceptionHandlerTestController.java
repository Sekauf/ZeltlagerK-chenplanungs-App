package de.zeltlager.kuechenplaner.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.zeltlager.kuechenplaner.api.exception.ResourceNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/test")
class GlobalExceptionHandlerTestController {

    @GetMapping("/not-found")
    void notFound() {
        throw new ResourceNotFoundException("Ressource fehlt");
    }

    @PostMapping("/validate")
    void validate(@Valid @RequestBody TestPayload payload) {
        // no-op
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new RuntimeException("kaputt");
    }

    static class TestPayload {

        @NotBlank(message = "Wert muss befüllt werden")
        String value;
    }
}
