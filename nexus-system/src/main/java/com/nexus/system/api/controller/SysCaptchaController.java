package com.nexus.system.api.controller;

import com.nexus.common.core.domain.Result;
import com.nexus.system.application.service.CaptchaImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/captcha")
@RequiredArgsConstructor
public class SysCaptchaController {

    private final CaptchaImageService captchaImageService;

    @GetMapping("/image")
    public Result<CaptchaImageService.CaptchaResult> getImage() {
        return Result.ok(captchaImageService.generateCaptcha());
    }

    @PostMapping("/validate")
    public Result<Boolean> validate(@RequestParam String uuid, @RequestParam String code) {
        return Result.ok(captchaImageService.validate(uuid, code));
    }
}
