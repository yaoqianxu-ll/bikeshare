package com.example.bickdemo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图形验证码服务
 * 基于 Redis 存储验证码，支持无状态 JWT 系统
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final int LENGTH = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int EXPIRE_SECONDS = 180; // 3分钟过期

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * 生成验证码
     * @return 包含 captchaId 和 base64 图片的 Map
     */
    public Map<String, String> generateCaptcha() {
        String captchaId = generateCaptchaId();
        String answer = generateCode();

        // 存储到 Redis
        stringRedisTemplate.opsForValue().set(
                CAPTCHA_KEY_PREFIX + captchaId,
                answer,
                java.time.Duration.ofSeconds(EXPIRE_SECONDS)
        );

        // 生成图片
        String base64Image = generateImage(answer);

        Map<String, String> result = new HashMap<>();
        result.put("captchaId", captchaId);
        result.put("image", "data:image/png;base64," + base64Image);
        return result;
    }

    /**
     * 验证验证码
     * @param captchaId 验证码 ID
     * @param answer 用户输入的答案
     * @return 是否验证通过
     */
    public boolean verify(String captchaId, String answer) {
        if (captchaId == null || answer == null) {
            return false;
        }

        String key = CAPTCHA_KEY_PREFIX + captchaId;
        String storedAnswer = stringRedisTemplate.opsForValue().get(key);

        if (storedAnswer == null) {
            return false;
        }

        boolean isValid = storedAnswer.equalsIgnoreCase(answer.trim());

        // 验证成功后删除验证码（防止重复使用）
        if (isValid) {
            stringRedisTemplate.delete(key);
        }

        return isValid;
    }

    private String generateCaptchaId() {
        return ThreadLocalRandom.current().nextLong(1000000000000L, 9999999999999L) + "";
    }

    private String generateCode() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    private String generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置背景色
        g.setColor(new Color(240, 244, 248));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 设置字体
        Font font = new Font("Arial", Font.BOLD, 24);
        g.setFont(font);

        // 绘制字符
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < code.length(); i++) {
            int x = 15 + i * 25;
            int y = 28;

            // 随机颜色
            g.setColor(new Color(
                    50 + random.nextInt(100),
                    50 + random.nextInt(100),
                    50 + random.nextInt(100)
            ));

            // 随机旋转
            double angle = (random.nextDouble() - 0.5) * 0.3;
            g.rotate(angle, x, y);

            g.drawString(String.valueOf(code.charAt(i)), x, y);

            g.rotate(-angle, x, y);
        }

        // 添加干扰线
        for (int i = 0; i < 3; i++) {
            g.setColor(new Color(150 + random.nextInt(100), 150 + random.nextInt(100), 150 + random.nextInt(100)));
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 添加噪点
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(150 + random.nextInt(100), 150 + random.nextInt(100), 150 + random.nextInt(100)));
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            image.setRGB(x, y, g.getColor().getRGB());
        }

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            log.error("生成验证码图片失败", e);
            throw new RuntimeException("生成验证码图片失败");
        }
    }
}
