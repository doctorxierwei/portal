package com.portal.auth.controller;

import com.portal.auth.service.AuthService;
import com.portal.common.result.R;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return authService.login(body.get("username"), body.get("password"));
    }

    @PostMapping("/register")
    public R<Void> register(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", null);
        String phone = body.getOrDefault("phone", null);
        return authService.register(
                body.get("username"),
                body.get("password"),
                body.getOrDefault("nickname", body.get("username")),
                (email == null || email.isEmpty()) ? null : email,
                (phone == null || phone.isEmpty()) ? null : phone);
    }
}
