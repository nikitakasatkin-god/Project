package org.example.controller;

import org.example.model.Request;
import org.example.model.RequestStatus;
import org.example.model.Role;
import org.example.model.User;
import org.example.repository.CarrierRepository;
import org.example.repository.RequestRepository;
import org.example.repository.TripRepository;
import org.example.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final RequestRepository requestRepository;
    private final TripRepository tripRepository;
    private final CarrierRepository carrierRepository;
    private final UserRepository userRepository;

    public DashboardController(RequestRepository requestRepository,
                               TripRepository tripRepository,
                               CarrierRepository carrierRepository,
                               UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.tripRepository = tripRepository;
        this.carrierRepository = carrierRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();
        User currentUser = getCurrentUser();

        List<Request> requests;

        // Фильтруем заявки в зависимости от роли
        if (currentUser.getRole() == Role.ADMIN) {
            requests = requestRepository.findAll();
        } else if (currentUser.getRole() == Role.LOGIST) {
            // Логист видит ТОЛЬКО свои заявки
            requests = requestRepository.findByOwner(currentUser);
        } else if (currentUser.getRole() == Role.DISPATCHER) {
            // Диспетчер видит заявки своего подразделения
            requests = requestRepository.findByDivision(currentUser.getDivision());
        } else {
            requests = List.of();
        }

        // Активные заявки (в работе)
        long activeRequests = requests.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == RequestStatus.IN_PROGRESS)
                .count();
        data.put("activeRequests", activeRequests);

        // Рейсы сегодня
        long todayTrips = tripRepository.findAll().stream()
                .filter(t -> t.getTripDate() != null && t.getTripDate().equals(LocalDate.now()))
                .count();
        data.put("todayTrips", todayTrips);

        // Количество перевозчиков
        data.put("carriersCount", carrierRepository.count());

        // Последние 5 заявок (с учетом роли)
        List<Request> recentRequests = requests.stream()
                .limit(5)
                .collect(Collectors.toList());
        data.put("recentRequests", recentRequests);

        return data;
    }
}