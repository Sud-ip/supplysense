package com.supplysense.backend.dashboard.controller;

import com.supplysense.backend.dashboard.dto.DashboardSummaryResponse;
import com.supplysense.backend.dashboard.dto.StockTrendPointResponse;
import com.supplysense.backend.dashboard.dto.TopMoverResponse;
import com.supplysense.backend.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/trends")
    public List<StockTrendPointResponse> getTrends(@RequestParam(defaultValue = "30") int days) {
        return dashboardService.getTrends(days);
    }

    @GetMapping("/top-movers")
    public List<TopMoverResponse> getTopMovers(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return dashboardService.getTopMovers(days, limit);
    }
}
