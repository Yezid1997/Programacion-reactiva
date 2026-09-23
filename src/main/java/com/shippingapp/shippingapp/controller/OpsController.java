package com.shippingapp.shippingapp.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ops")
public class OpsController {
    @GetMapping(value = "/tablero", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<?> getDashBoard(){
        return Mono.just(null);
    }
}
