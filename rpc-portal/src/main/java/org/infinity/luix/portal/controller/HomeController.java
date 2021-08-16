package org.infinity.luix.portal.controller;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final Environment env;

    public HomeController(Environment env) {
        this.env = env;
    }

    /**
     * Home page.
     */
    @GetMapping("/")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok(env.getProperty("spring.application.name") + " Home Page");
    }
}
