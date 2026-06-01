package org.example.controller;

import org.example.repository.CarrierRepository;
import org.example.repository.RequestRepository;
import org.example.repository.TripRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final RequestRepository requestRepository;
    private final TripRepository tripRepository;
    private final CarrierRepository carrierRepository;

    public DashboardController(RequestRepository requestRepository,
                               TripRepository tripRepository,
                               CarrierRepository carrierRepository) {
        this.requestRepository = requestRepository;
        this.tripRepository = tripRepository;
        this.carrierRepository = carrierRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}