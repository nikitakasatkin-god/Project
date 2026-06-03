package org.example.controller;

import org.example.model.TripStatusEntity;
import org.example.repository.TripStatusRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trip-statuses")
public class TripStatusController {

    private final TripStatusRepository tripStatusRepository;

    public TripStatusController(TripStatusRepository tripStatusRepository) {
        this.tripStatusRepository = tripStatusRepository;
    }

    @GetMapping
    public List<TripStatusEntity> getAllStatuses() {
        return tripStatusRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    @GetMapping("/all")
    public List<TripStatusEntity> getAllIncludingInactive() {
        return tripStatusRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripStatusEntity> getStatusById(@PathVariable Long id) {
        return tripStatusRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public TripStatusEntity createStatus(@RequestBody TripStatusEntity status) {
        if (status.getSortOrder() == null) {
            status.setSortOrder(0);
        }
        return tripStatusRepository.save(status);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripStatusEntity> updateStatus(@PathVariable Long id, @RequestBody TripStatusEntity status) {
        if (!tripStatusRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        status.setId(id);
        return ResponseEntity.ok(tripStatusRepository.save(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStatus(@PathVariable Long id) {
        TripStatusEntity status = tripStatusRepository.findById(id).orElse(null);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        if (Boolean.TRUE.equals(status.getSystemDefault())) {
            return ResponseEntity.badRequest().body("Нельзя удалить системный статус");
        }
        tripStatusRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}