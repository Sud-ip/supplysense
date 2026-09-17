package com.supplysense.backend.alerting.controller;

import com.supplysense.backend.alerting.dto.LowStockAlertResponse;
import com.supplysense.backend.alerting.service.LowStockAlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final LowStockAlertService lowStockAlertService;

    public AlertController(LowStockAlertService lowStockAlertService) {
        this.lowStockAlertService = lowStockAlertService;
    }

    @GetMapping("/low-stock")
    public List<LowStockAlertResponse> getLowStockAlerts() {
        return lowStockAlertService.getLowStockAlerts();
    }
}
