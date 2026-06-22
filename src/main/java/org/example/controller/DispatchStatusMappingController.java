package org.example.controller;

import org.example.model.DispatchStatusMapping;
import org.example.model.TripStatusEntity;
import org.example.repository.DispatchStatusMappingRepository;
import org.example.repository.TripStatusRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispatch-mappings")
public class DispatchStatusMappingController {

    private final DispatchStatusMappingRepository mappingRepository;
    private final TripStatusRepository tripStatusRepository;

    public DispatchStatusMappingController(DispatchStatusMappingRepository mappingRepository,
                                           TripStatusRepository tripStatusRepository) {
        this.mappingRepository = mappingRepository;
        this.tripStatusRepository = tripStatusRepository;
    }

    @GetMapping
    public List<DispatchStatusMapping> getAllMappings() {
        return mappingRepository.findByActiveTrue();
    }

    @PostMapping
    public ResponseEntity<?> createMapping(@RequestBody Map<String, Object> data) {
        Long dispatchStatusId = Long.parseLong(data.get("dispatchStatusId").toString());
        String dispatchStatusName = data.get("dispatchStatusName").toString();
        Long localStatusId = Long.parseLong(data.get("localStatusId").toString());

        TripStatusEntity localStatus = tripStatusRepository.findById(localStatusId).orElse(null);
        if (localStatus == null) {
            return ResponseEntity.badRequest().body("Локальный статус не найден");
        }

        if (mappingRepository.findByDispatchStatusId(dispatchStatusId).isPresent()) {
            return ResponseEntity.badRequest().body("Сопоставление для этого статуса диспетчеризации уже существует");
        }

        DispatchStatusMapping mapping = new DispatchStatusMapping();
        mapping.setDispatchStatusId(dispatchStatusId);
        mapping.setDispatchStatusName(dispatchStatusName);
        mapping.setLocalStatus(localStatus);
        mapping.setActive(true);

        return ResponseEntity.ok(mappingRepository.save(mapping));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMapping(@PathVariable Long id) {
        if (!mappingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        mappingRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}