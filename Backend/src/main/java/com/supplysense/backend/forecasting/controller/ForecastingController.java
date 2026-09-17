package com.supplysense.backend.forecasting.controller;

import com.supplysense.backend.forecasting.dto.ForecastResponse;
import com.supplysense.backend.forecasting.service.ForecastingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forecasts")
public class ForecastingController {

    private final ForecastingService forecastingService;

    public ForecastingController(ForecastingService forecastingService) {
        this.forecastingService = forecastingService;
    }

    @GetMapping
    public List<ForecastResponse> getForecasts(@RequestParam(required = false) UUID productId) {
        return forecastingService.getForecasts(productId);
    }
}
