package org.example.controller;

import org.example.model.Division;
import org.example.model.User;
import org.example.repository.DivisionRepository;
import org.example.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/divisions")
public class DivisionController {

    private final DivisionRepository divisionRepository;
    private final UserRepository userRepository;

    public DivisionController(DivisionRepository divisionRepository, UserRepository userRepository) {
        this.divisionRepository = divisionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Map<String, Object>> getAllDivisions() {
        List<Division> divisions = divisionRepository.findAll();

        return divisions.stream().map(division -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", division.getId());
            result.put("name", division.getName());
            result.put("description", division.getDescription());

            List<User> users = userRepository.findByDivision(division);
            List<Map<String, Object>> userList = users.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("fullName", user.getFullName());
                userMap.put("role", user.getRole().name());
                return userMap;
            }).collect(Collectors.toList());

            result.put("users", userList);
            return result;
        }).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDivisionById(@PathVariable Long id) {
        return divisionRepository.findById(id)
                .map(division -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", division.getId());
                    result.put("name", division.getName());
                    result.put("description", division.getDescription());

                    List<User> users = userRepository.findByDivision(division);
                    List<Map<String, Object>> userList = users.stream().map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("fullName", user.getFullName());
                        userMap.put("role", user.getRole().name());
                        return userMap;
                    }).collect(Collectors.toList());

                    result.put("users", userList);
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Division createDivision(@RequestBody Division division) {
        return divisionRepository.save(division);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Division> updateDivision(@PathVariable Long id, @RequestBody Division division) {
        if (!divisionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        division.setId(id);
        return ResponseEntity.ok(divisionRepository.save(division));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDivision(@PathVariable Long id) {
        return divisionRepository.findById(id)
                .map(division -> {
                    List<User> users = userRepository.findByDivision(division);
                    for (User user : users) {
                        user.setDivision(null);
                        userRepository.save(user);
                    }
                    divisionRepository.deleteById(id);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{divisionId}/users/{userId}")
    public ResponseEntity<?> addUserToDivision(@PathVariable Long divisionId, @PathVariable Long userId) {
        Division division = divisionRepository.findById(divisionId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (division == null || user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setDivision(division);
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{divisionId}/users/{userId}")
    public ResponseEntity<?> removeUserFromDivision(@PathVariable Long divisionId, @PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setDivision(null);
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/unassigned-users")
    public List<Map<String, Object>> getUnassignedUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getDivision() == null)
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("fullName", user.getFullName());
                    userMap.put("role", user.getRole().name());
                    return userMap;
                })
                .collect(Collectors.toList());
    }
}