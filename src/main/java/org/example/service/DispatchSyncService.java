package org.example.service;

import org.example.model.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class DispatchSyncService {

    private static final Logger log = LoggerFactory.getLogger(DispatchSyncService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dispatch.system.url:http://localhost:8081}")
    private String dispatchSystemUrl;

    @Value("${dispatch.system.username:admin}")
    private String dispatchUsername;

    @Value("${dispatch.system.password:admin123}")
    private String dispatchPassword;

    public boolean sendTripToDispatch(Trip trip) {
        log.info("=== ОТПРАВКА РЕЙСА {} В ДИСПЕТЧЕРИЗАЦИЮ ===", trip.getId());
        log.info("URL: {}", dispatchSystemUrl);

        try {
            String url = dispatchSystemUrl + "/api/sync/receive-trips";

            Map<String, Object> tripData = new HashMap<>();
            tripData.put("id", trip.getId());
            tripData.put("requestId", trip.getRequest().getId());
            tripData.put("carrierName", trip.getCarrier() != null ? trip.getCarrier().getName() : "");
            tripData.put("vehiclePlate", trip.getVehiclePlate() != null ? trip.getVehiclePlate() : "");
            tripData.put("trailerPlate", trip.getTrailerPlate() != null ? trip.getTrailerPlate() : "");
            tripData.put("vehicleBrand", trip.getVehicleBrand() != null ? trip.getVehicleBrand() : "");
            tripData.put("driverName", trip.getDriverName() != null ? trip.getDriverName() : "");
            tripData.put("tripDate", trip.getTripDate() != null ? trip.getTripDate().toString() : "");
            tripData.put("volume", trip.getVolume());
            tripData.put("status", trip.getStatus().name());

            log.info("Данные для отправки: id={}, requestId={}, carrier={}",
                    trip.getId(), trip.getRequest().getId(), trip.getCarrier().getName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String auth = dispatchUsername + ":" + dispatchPassword;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);

            HttpEntity<java.util.List<Map<String, Object>>> request =
                    new HttpEntity<>(java.util.Collections.singletonList(tripData), headers);

            var response = restTemplate.postForEntity(url, request, String.class);
            log.info("Ответ от диспетчеризации: статус={}", response.getStatusCode());

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("ОШИБКА при отправке рейса {}: {}", trip.getId(), e.getMessage(), e);
            return false;
        }
    }
}