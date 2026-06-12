package com.matvey.innowiseorderservice.client;

import com.matvey.innowiseorderservice.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestTemplate restTemplate;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    public UserDto getUserByEmail(String email) {
        String url = userServiceUrl + "/internal/users/email/" + email;

        ResponseEntity<UserDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                UserDto.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to get user from User Service");
        }

        return response.getBody();
    }

    public UserDto getUserByEmailFallback(String email, Exception e) {
        UserDto fallbackUser = new UserDto();
        fallbackUser.setEmail(email);
        fallbackUser.setName("Unknown");
        fallbackUser.setSurname("Unknown");
        fallbackUser.setActive(false);
        return fallbackUser;
    }
}
