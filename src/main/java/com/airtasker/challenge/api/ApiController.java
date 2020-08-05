package com.airtasker.challenge.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/")
public class ApiController {

    @GetMapping
    String getData(){
        return "Hello, Successfully retrieved request";
    }
}
