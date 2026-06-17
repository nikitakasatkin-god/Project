package org.example.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class DispatchApiClient {

    private static final Logger log = LoggerFactory.getLogger(DispatchApiClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dispatch.system.url:http://localhost:8081}")
    private String dispatchSystemUrl;

    @Value("${dispatch.system.username:admin}")
    private String dispatchUsername;

    @Value("${dispatch.system.password:admin123}")
    private String dispatchPassword;

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = dispatchUsername + ":" + dispatchPassword;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + new String(encodedAuth));
        return headers;
    }

    /**
     * Проверка доступности системы Диспетчеризации
     */
    private boolean checkHealth() {
        try {
            String url = dispatchSystemUrl + "/api/sync/health";
            log.debug("Проверка доступности Диспетчеризации: {}", url);

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            boolean isAvailable = response.getStatusCode().is2xxSuccessful();
            log.debug("Диспетчеризация доступна: {}", isAvailable);
            return isAvailable;

        } catch (ResourceAccessException e) {
            log.debug("❌ Диспетчеризация недоступна (Connection refused): {}", e.getMessage());
            return false;
        } catch (RestClientException e) {
            log.debug("❌ Диспетчеризация недоступна: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.debug("❌ Диспетчеризация недоступна: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Публичный метод для проверки доступности
     */
    public boolean isDispatchAvailable() {
        return checkHealth();
    }

    /**
     * Отправка рейсов в систему Диспетчеризации
     */
    public boolean sendTripsToDispatch(List<Map<String, Object>> tripsData) {
        log.info("=== ОТПРАВКА РЕЙСОВ В ДИСПЕТЧЕРИЗАЦИЮ ===");
        log.info("Количество рейсов: {}", tripsData.size());

        if (!checkHealth()) {
            log.warn("⚠️ Диспетчеризация недоступна, отправка рейсов отменена");
            return false;
        }

        try {
            String url = dispatchSystemUrl + "/api/sync/receive-trips";
            log.info("URL: {}", url);

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(tripsData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Рейсы успешно отправлены в Диспетчеризацию");
                return true;
            } else {
                log.error("❌ Ошибка отправки рейсов: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при отправке рейсов: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Получение статусов из системы Диспетчеризации
     */
    public List<Map<String, Object>> receiveStatusesFromDispatch() {
        log.info("=== ПОЛУЧЕНИЕ СТАТУСОВ ИЗ ДИСПЕТЧЕРИЗАЦИИ ===");

        if (!checkHealth()) {
            log.warn("⚠️ Диспетчеризация недоступна, получение статусов отменено");
            return List.of();
        }

        try {
            String url = dispatchSystemUrl + "/api/sync/send-statuses";
            log.info("URL: {}", url);

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Object updatesObj = responseBody.get("updates");

                if (updatesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> updates = (List<Map<String, Object>>) updatesObj;
                    log.info("✅ Получено {} обновлений статусов", updates.size());
                    return updates;
                } else {
                    log.warn("Поле 'updates' не является списком: {}", updatesObj);
                    return List.of();
                }
            } else {
                log.error("❌ Ошибка получения статусов: {}", response.getStatusCode());
                return List.of();
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при получении статусов: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Получение списка статусов из системы Диспетчеризации
     */
    public List<Map<String, Object>> fetchDispatchStatuses() {
        log.info("=== ПОЛУЧЕНИЕ СТАТУСОВ ИЗ ДИСПЕТЧЕРИЗАЦИИ (GET) ===");

        if (!checkHealth()) {
            log.warn("⚠️ Диспетчеризация недоступна, получение статусов отменено");
            return List.of();
        }

        try {
            String url = dispatchSystemUrl + "/api/statuses";
            log.info("URL: {}", url);

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    List.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("✅ Получено {} статусов из Диспетчеризации", response.getBody().size());
                return response.getBody();
            } else {
                log.error("❌ Ошибка получения статусов: {}", response.getStatusCode());
                return List.of();
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при получении статусов: {}", e.getMessage(), e);
            return List.of();
        }
    }
}