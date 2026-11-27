package com.yt.payment.controller;

import com.yt.payment.dto.MembershipTierRequest;
import com.yt.payment.dto.MembershipTierResponse;
import com.yt.payment.service.MembershipTierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tiers")
public class MembershipTierController {

    private final MembershipTierService service;

    public MembershipTierController(MembershipTierService service) {
        this.service = service;
    }

    @GetMapping
    public List<MembershipTierResponse> getAll(
            @RequestParam(required = false) String channelId,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        return service.getAll(channelId, activeOnly);
    }

    @GetMapping("/{id}")
    public MembershipTierResponse getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<MembershipTierResponse> create(@Valid @RequestBody MembershipTierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public MembershipTierResponse update(@PathVariable String id, @Valid @RequestBody MembershipTierRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
