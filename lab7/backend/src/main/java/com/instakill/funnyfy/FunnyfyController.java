package com.instakill.funnyfy;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/funnyfy")
public class FunnyfyController {

    private final FunnyfyService service;

    public FunnyfyController(FunnyfyService service) {
        this.service = service;
    }

    @PostMapping
    public FunnyfyResponse funnyfy(@Valid @RequestBody FunnyfyRequest request) {
        return new FunnyfyResponse(service.funnyfy(request.text()));
    }
}