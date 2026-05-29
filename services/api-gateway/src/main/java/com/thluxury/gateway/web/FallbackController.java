package com.thluxury.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Đích forward của CircuitBreaker filter khi một service downstream mở mạch.
 * Trả 503 + JSON thân thiện để FE hiển thị "đang bận".
 */
@RestController
@RequestMapping("/__fallback")
public class FallbackController {

    @RequestMapping(value = "/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> fallback(@PathVariable String service) {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "service", service,
                "message", "Dịch vụ đang bận, vui lòng thử lại sau giây lát")));
    }
}
