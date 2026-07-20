package com.example.product_api.service;

import com.example.product_api.model.AppUser;
import com.example.product_api.model.RefreshToken;
import com.example.product_api.repository.RefreshTokenRepository;
import com.example.product_api.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refreshExpirationMs}")
    private int refreshExpirationMs; // 7 días

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtUtils jwtUtils) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtils = jwtUtils;
    }

    public RefreshToken createRefreshToken(AppUser user) {
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID().toString(),
                Instant.now().plus(refreshExpirationMs, ChronoUnit.MILLIS),
                user
        );
        return refreshTokenRepository.save(refreshToken);
    }

    public boolean isValid(RefreshToken token) {
        return token != null && token.getExpiryDate().isAfter(Instant.now());
    }

    public void deleteByUser(AppUser user) {
        refreshTokenRepository.deleteByUser(user);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Rotación del refresh token: valida el token actual, lo elimina,
     * genera nuevos access y refresh tokens, y los devuelve en un Map.
     */
    public Map<String, String> refreshToken(String token) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token no encontrado"));
        if (!isValid(oldToken)) {
            deleteByUser(oldToken.getUser());
            throw new RuntimeException("Refresh token expirado");
        }
        // Eliminar el token antiguo (rotación)
        refreshTokenRepository.delete(oldToken);
        // Generar nuevos tokens
        AppUser user = oldToken.getUser();
        String newAccessToken = jwtUtils.generateToken(user.getUsername());
        RefreshToken newRefreshToken = createRefreshToken(user);
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken.getToken());
        return tokens;
    }
}