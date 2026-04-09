package com.yelshod.diagnosticserviceai.auth;

import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public MeResponse account(Principal principal) {
        return accountService.me(principal.getName());
    }

    @PatchMapping
    public MeResponse update(Principal principal, @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.update(principal.getName(), request);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
