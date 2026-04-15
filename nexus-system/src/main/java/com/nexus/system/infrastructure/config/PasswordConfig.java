package com.nexus.system.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * 支持多种密码格式：
 * <ul>
 *   <li>{@code {noop}rawpassword} — 明文前缀，开发/演示阶段直接比对原始密码</li>
 *   <li>{@code {bcrypt}hash} — BCrypt 哈希，生产推荐</li>
 * </ul>
 * 默认编码器为 BCrypt，新密码或改密后自动用 BCrypt 存储。
 */
@Configuration
public class PasswordConfig {

    public static final String ENCODER_ID_BCRYPT = "bcrypt";
    public static final String ENCODER_ID_NOOP = "noop";

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(ENCODER_ID_BCRYPT, new BCryptPasswordEncoder());
        encoders.put(ENCODER_ID_NOOP, new NoOpPasswordEncoder());
        return new DelegatingPasswordEncoder(ENCODER_ID_BCRYPT, encoders);
    }

    /**
     * NoOpPasswordEncoder：直接比对原始密码（用于 {noop} 前缀）。
     * @deprecated 仅供 DelegatingPasswordEncoder 内部使用
     */
    @Deprecated
    public static final class NoOpPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            return rawPassword.toString();
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return rawPassword.toString().equals(encodedPassword);
        }
    }
}
