package com.luixtech.rpc.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class AuthController {

    @Operation(summary = "auth")
    @PostMapping("/api/auth")
    public Object auth(@RequestBody User domain) {
        if ("admin@demo.com".equals(domain.getEmail()) && "demo".equals(domain.getPassword())) {
            AuthUser authUser = new AuthUser();
            authUser.setId("2");
            authUser.setFirst_name("Camden");
            authUser.setLast_name("Lowe");
            authUser.setEmail(domain.getEmail());
            authUser.setEmail_verified_at("2023-03-02T12:40:30.000000Z");
            authUser.setCreated_at("2023-03-02T12:40:30.000000Z");
            authUser.setUpdated_at("2023-03-02T12:40:30.000000Z");
            authUser.setApi_token("$2y$10$rrQvQ5MIhJEY7ZkVM1jXQ.PY.zj9C/Z.PN9elVtvVCQlAl0HwJhou");
            return authUser;
        } else {
            EmailError emailError = new EmailError();
            emailError.setEmail(new String[]{"The provided credentials are incorrect"});
            Error error = new Error();
            error.setMessage("The provided credentials are incorrect");
            error.setErrors(emailError);
            return error;
        }
    }
}

@Data
class User {
    private String email;
    private String password;
}

//{
//        "id": 2,
//        "first_name": "Camden",
//        "last_name": "Lowe",
//        "email": "admin@demo.com",
//        "email_verified_at": "2023-03-02T12:40:30.000000Z",
//        "created_at": "2023-03-02T12:40:30.000000Z",
//        "updated_at": "2023-03-02T12:40:30.000000Z",
//        "api_token": "$2y$10$rrQvQ5MIhJEY7ZkVM1jXQ.PY.zj9C\/Z.PN9elVtvVCQlAl0HwJhou"
//        }
@Data
class AuthUser {
    private String id;
    private String first_name;
    private String last_name;
    private String email;
    private String email_verified_at;
    private String created_at;
    private String updated_at;
    private String api_token;
}

@Data
class Error {
    private String     message;
    private EmailError errors;
}

@Data
class EmailError {
    private String[] email;
}


