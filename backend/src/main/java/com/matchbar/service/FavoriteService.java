package com.matchbar.service;

import com.matchbar.dto.response.BarResponse;
import com.matchbar.entity.Favorite;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.BarRepository;
import com.matchbar.repository.FavoriteRepository;
import com.matchbar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final BarRepository barRepository;

    public void add(String userId, String barId) {
        if (favoriteRepository.existsByUserIdAndBarId(userId, barId)) return;
        if (!userRepository.existsById(userId)) throw new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        if (!barRepository.existsById(barId)) throw new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado");
        favoriteRepository.save(Favorite.builder().userId(userId).barId(barId).build());
    }

    public void remove(String userId, String barId) {
        favoriteRepository.deleteByUserIdAndBarId(userId, barId);
    }

    public List<BarResponse> listByUser(String userId) {
        List<String> barIds = favoriteRepository.findByUserId(userId).stream()
                .map(Favorite::getBarId)
                .collect(Collectors.toList());
        if (barIds.isEmpty()) return List.of();
        return barRepository.findAllById(barIds).stream()
                .map(BarResponse::from)
                .collect(Collectors.toList());
    }
}
