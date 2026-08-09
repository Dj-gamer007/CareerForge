package com.careerforge.service;

import com.careerforge.entity.RefreshToken;
import com.careerforge.entity.User;

import java.util.Optional;

public interface RefreshTokenService {

    Optional<RefreshToken> findByToken(String token);

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyExpiration(RefreshToken token);

    int deleteByUserId(Long userId);
}
