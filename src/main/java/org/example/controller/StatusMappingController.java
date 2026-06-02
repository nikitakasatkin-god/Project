package org.example.controller;

import org.example.model.StatusMapping;
import org.example.repository.StatusMappingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/status-mappings")
public class StatusMappingController {

    private final StatusMappingRepository statusMappingRepository;

    public StatusMappingController(StatusMappingRepository statusMappingRepository) {
        this.statusMappingRepository = statusMappingRepository;
    }

    @GetMapping
    public List<StatusMapping> getAllMappings() {
        return statusMappingRepository.findByActiveTrue();
    }

    @PostMapping
    public ResponseEntity<?> createMapping(@RequestBody Map<String, Object> data) {
        Long dispatchStatusId = Long.parseLong(data.get("dispatchStatusId").toString());
        String localStatus = data.get("localStatus").toString();

        StatusMapping mapping = new StatusMapping();
        mapping.setDispatchStatusId(dispatchStatusId);
        mapping.setDispatchStatusName("Status_" + dispatchStatusId);
        mapping.setLocalStatus(localStatus);
        mapping.setActive(true);

        return ResponseEntity.ok(statusMappingRepository.save(mapping));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMapping(@PathVariable Long id) {
        if (!statusMappingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        statusMappingRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}