package com.supplysense.backend.inventory.controller;

import com.supplysense.backend.inventory.dto.ReorderThresholdRequest;
import com.supplysense.backend.inventory.dto.ReorderThresholdResponse;
import com.supplysense.backend.inventory.service.ReorderThresholdService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reorder-thresholds")
public class ReorderThresholdController {

    private final ReorderThresholdService thresholdService;

    public ReorderThresholdController(ReorderThresholdService thresholdService) {
        this.thresholdService = thresholdService;
    }

    @GetMapping
    public List<ReorderThresholdResponse> findAll() {
        return thresholdService.findAll();
    }

    @PutMapping
    public ReorderThresholdResponse upsert(@Valid @RequestBody ReorderThresholdRequest request) {
        return thresholdService.upsert(request);
    }
}
