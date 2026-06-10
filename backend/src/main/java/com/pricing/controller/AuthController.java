package com.pricing.controller;

import com.pricing.common.CommonResult;
import com.pricing.dto.LoginRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @PostMapping("/login")
    public CommonResult<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
        if ("admin".equals(loginRequest.getUsername()) && "123456".equals(loginRequest.getPassword())) {
            Map<String, String> data = new HashMap<>();
            data.put("token", "admin-token");
            data.put("username", "管理员");
            return CommonResult.success(data);
        } else {
            return CommonResult.failed("用户名或密码不正确");
        }
    }

    @PostMapping("/logout")
    public CommonResult<String> logout() {
        return CommonResult.success("退出成功");
    }
}
