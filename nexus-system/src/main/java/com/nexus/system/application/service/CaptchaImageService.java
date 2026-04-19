package com.nexus.system.application.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaImageService {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final long TTL_SECONDS = 300;

    private static final String CHAR_SET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final StringRedisTemplate redisTemplate;

    public CaptchaImageService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public record CaptchaResult(String uuid, String img) {}

    public CaptchaResult generateCaptcha() {
        // Generate random code
        String code = generateCode(CODE_LENGTH);
        String uuid = UUID.randomUUID().toString();

        // Store in Redis
        redisTemplate.opsForValue().set(CAPTCHA_PREFIX + uuid, code.toLowerCase(), TTL_SECONDS, TimeUnit.SECONDS);

        // Generate image
        String base64Img = generateImage(code);

        return new CaptchaResult(uuid, base64Img);
    }

    public boolean validate(String uuid, String input) {
        if (uuid == null || input == null) {
            return false;
        }
        String key = CAPTCHA_PREFIX + uuid;
        String expected = redisTemplate.opsForValue().get(key);
        if (expected == null) {
            return false;
        }
        // Delete after one use
        redisTemplate.delete(key);
        return expected.equalsIgnoreCase(input.trim());
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHAR_SET.charAt((int) (Math.random() * CHAR_SET.length())));
        }
        return sb.toString();
    }

    private String generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw code
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(code)) / 2;
        int y = (HEIGHT + fm.getAscent()) / 2 - 2;
        g.drawString(code, x, y);

        // Add noise lines
        g.setColor(new Color(200, 200, 200));
        for (int i = 0; i < 3; i++) {
            int x1 = (int) (Math.random() * WIDTH);
            int y1 = (int) (Math.random() * HEIGHT);
            int x2 = (int) (Math.random() * WIDTH);
            int y2 = (int) (Math.random() * HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate captcha image", e);
        }
    }
}
