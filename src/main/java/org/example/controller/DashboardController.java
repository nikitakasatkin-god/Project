package org.example.controller;

import org.example.model.Request;
import org.example.model.RequestStatus;
import org.example.model.Role;
import org.example.model.Trip;
import org.example.model.User;
import org.example.model.Carrier;
import org.example.repository.CarrierRepository;
import org.example.repository.RequestRepository;
import org.example.repository.TripRepository;
import org.example.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
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
        List<Trip> trips;
        List<Carrier> carriers;

        if (currentUser.getRole() == Role.ADMIN) {
            requests = requestRepository.findAll();
            trips = tripRepository.findAll();
            carriers = carrierRepository.findAll();
        } else if (currentUser.getRole() == Role.LOGIST) {
            requests = requestRepository.findByOwner(currentUser);
            List<Long> requestIds = requests.stream().map(Request::getId).collect(Collectors.toList());
            trips = tripRepository.findAll().stream()
                    .filter(t -> requestIds.contains(t.getRequest().getId()))
                    .collect(Collectors.toList());
            List<Long> carrierIds = trips.stream()
                    .map(t -> t.getCarrier() != null ? t.getCarrier().getId() : null)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            carriers = carrierRepository.findAll().stream()
                    .filter(c -> carrierIds.contains(c.getId()))
                    .collect(Collectors.toList());
        } else if (currentUser.getRole() == Role.DISPATCHER) {
            requests = requestRepository.findByDivision(currentUser.getDivision());
            trips = tripRepository.findByRequest_Division(currentUser.getDivision());
            List<Long> carrierIds = trips.stream()
                    .map(t -> t.getCarrier() != null ? t.getCarrier().getId() : null)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            carriers = carrierRepository.findAll().stream()
                    .filter(c -> carrierIds.contains(c.getId()))
                    .collect(Collectors.toList());
        } else {
            requests = List.of();
            trips = List.of();
            carriers = List.of();
        }

        long activeRequests = requests.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == RequestStatus.IN_PROGRESS)
                .count();
        data.put("activeRequests", activeRequests);

        long todayTrips = trips.stream()
                .filter(t -> t.getTripDate() != null && t.getTripDate().equals(LocalDate.now()))
                .count();
        data.put("todayTrips", todayTrips);

        data.put("carriersCount", (long) carriers.size());

        List<Request> recentRequests = requests.stream()
                .sorted(Comparator.comparing(Request::getId).reversed())
                .limit(5)
                .collect(Collectors.toList());
        data.put("recentRequests", recentRequests);

        return data;
    }
}