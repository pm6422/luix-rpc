package org.infinity.rpc.appclient.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @Autowired
    private Environment env;

    /**
     * Home page.
     */
    @GetMapping("/")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok(env.getProperty("spring.application.name") + " Home Page");
    }
}
