package org.example.controller;

import org.example.model.Request;
import org.example.repository.CarrierRepository;
import org.example.repository.RequestRepository;
import org.example.repository.TripRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
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
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();

        // Активные заявки (в работе)
        long activeRequests = requestRepository.findAll().stream()
                .filter(r -> r.getStatus() != null && r.getStatus().toString().equals("IN_PROGRESS"))
                .count();
        data.put("activeRequests", activeRequests);

        // Рейсы сегодня
        long todayTrips = tripRepository.findAll().stream()
                .filter(t -> t.getTripDate() != null && t.getTripDate().equals(LocalDate.now()))
                .count();
        data.put("todayTrips", todayTrips);

        // Количество перевозчиков
        data.put("carriersCount", carrierRepository.count());

        // Последние 5 заявок
        List<Request> recentRequests = requestRepository.findAll().stream()
                .limit(5)
                .toList();
        data.put("recentRequests", recentRequests);

        return data;
    }
}