package org.example.controller;

import org.example.model.Role;
import org.example.model.User;
import org.example.repository.DivisionRepository;
import org.example.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final DivisionRepository divisionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          DivisionRepository divisionRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.divisionRepository = divisionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> data) {
        try {
            String username = (String) data.get("username");
            String password = (String) data.get("password");
            String fullName = (String) data.get("fullName");
            String roleStr = (String) data.get("role");

            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Пользователь с таким именем уже существует"));
            }

            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setRole(Role.valueOf(roleStr));
            user.setActive(true);

            if (data.containsKey("divisionId") && data.get("divisionId") != null && !data.get("divisionId").toString().isEmpty()) {
                Long divisionId = Long.parseLong(data.get("divisionId").toString());
                divisionRepository.findById(divisionId).ifPresent(user::setDivision);
            }

            User saved = userRepository.save(user);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Обновляем ФИО
            if (data.containsKey("fullName")) {
                String fullName = (String) data.get("fullName");
                if (fullName != null) {
                    user.setFullName(fullName);
                }
            }

            // Обновляем роль
            if (data.containsKey("role")) {
                String roleStr = (String) data.get("role");
                if (roleStr != null) {
                    user.setRole(Role.valueOf(roleStr));
                }
            }

            // Обновляем подразделение - обрабатываем null как открепление
            if (data.containsKey("divisionId")) {
                Object divisionIdObj = data.get("divisionId");
                if (divisionIdObj == null ||
                        divisionIdObj.toString().isEmpty() ||
                        divisionIdObj.toString().equals("null")) {
                    user.setDivision(null);
                } else {
                    try {
                        Long divisionId = Long.parseLong(divisionIdObj.toString());
                        divisionRepository.findById(divisionId).ifPresent(user::setDivision);
                    } catch (NumberFormatException e) {
                        user.setDivision(null);
                    }
                }
            }

            // Обновляем активность
            if (data.containsKey("active")) {
                Boolean active = (Boolean) data.get("active");
                if (active != null) {
                    user.setActive(active);
                }
            }

            // Обновляем пароль, если передан
            if (data.containsKey("password") && data.get("password") != null && !((String) data.get("password")).isEmpty()) {
                user.setPassword(passwordEncoder.encode((String) data.get("password")));
            }

            User saved = userRepository.save(user);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}