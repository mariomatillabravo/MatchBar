package com.matchbar.service;

import com.matchbar.dto.response.BarResponse;
import com.matchbar.entity.Bar;
import com.matchbar.entity.Favorite;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.BarRepository;
import com.matchbar.repository.FavoriteRepository;
import com.matchbar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final BarRepository barRepository;

    @Transactional
    public void add(Long userId, Long barId) {
        if (favoriteRepository.existsByUserIdAndBarId(userId, barId)) return;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Bar bar = barRepository.findById(barId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado"));
        favoriteRepository.save(Favorite.builder().user(user).bar(bar).build());
    }

    @Transactional
    public void remove(Long userId, Long barId) {
        favoriteRepository.deleteByUserIdAndBarId(userId, barId);
    }

    @Transactional(readOnly = true)
    public List<BarResponse> listByUser(Long userId) {
        return favoriteRepository.findByUserId(userId).stream()
                .map(f -> BarResponse.from(f.getBar()))
                .collect(Collectors.toList());
    }
}
