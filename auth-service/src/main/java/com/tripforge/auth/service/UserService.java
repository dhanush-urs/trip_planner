package com.tripforge.auth.service;

import com.tripforge.auth.dto.UserPreferenceDto;
import com.tripforge.auth.dto.UserProfileDto;
import com.tripforge.auth.entity.User;
import com.tripforge.auth.entity.UserPreference;
import com.tripforge.auth.exception.ResourceNotFoundException;
import com.tripforge.auth.repository.UserPreferenceRepository;
import com.tripforge.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Service for user profile and preference management.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPreferenceRepository preferenceRepository;

    /**
     * Returns the profile of the authenticated user.
     */
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = findUserById(userId);
        return mapToProfileDto(user);
    }

    /**
     * Returns the travel preferences of the authenticated user.
     * Creates default preferences if none exist yet.
     */
    @Transactional
    public UserPreferenceDto getPreferences(Long userId) {
        User user = findUserById(userId);

        UserPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(user));

        return mapToPreferenceDto(preference);
    }

    /**
     * Updates the travel preferences of the authenticated user.
     * Creates the preference record if it doesn't exist.
     */
    @Transactional
    public UserPreferenceDto updatePreferences(Long userId, UserPreferenceDto dto) {
        log.info("Updating preferences for user id: {}", userId);
        User user = findUserById(userId);

        UserPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPreference newPref = new UserPreference();
                    newPref.setUser(user);
                    return newPref;
                });

        // Update fields if provided
        if (dto.getInterests() != null) {
            preference.setInterests(String.join(",", dto.getInterests()));
        }
        if (dto.getHotelPreference() != null) {
            preference.setHotelPreference(
                    UserPreference.HotelPreference.valueOf(dto.getHotelPreference()));
        }
        if (dto.getDefaultBudget() != null) {
            preference.setDefaultBudget(dto.getDefaultBudget());
        }
        if (dto.getDefaultTravelers() != null) {
            preference.setDefaultTravelers(dto.getDefaultTravelers());
        }

        UserPreference saved = preferenceRepository.save(preference);
        log.info("Preferences updated for user id: {}", userId);
        return mapToPreferenceDto(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
    }

    private UserPreference createDefaultPreferences(User user) {
        UserPreference pref = UserPreference.builder()
                .user(user)
                .interests("nature,food")
                .hotelPreference(UserPreference.HotelPreference.STANDARD)
                .defaultTravelers(2)
                .build();
        return preferenceRepository.save(pref);
    }

    private UserProfileDto mapToProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserPreferenceDto mapToPreferenceDto(UserPreference pref) {
        List<String> interests = (pref.getInterests() != null && !pref.getInterests().isBlank())
                ? Arrays.asList(pref.getInterests().split(","))
                : Collections.emptyList();

        return UserPreferenceDto.builder()
                .interests(interests)
                .hotelPreference(pref.getHotelPreference() != null
                        ? pref.getHotelPreference().name() : null)
                .defaultBudget(pref.getDefaultBudget())
                .defaultTravelers(pref.getDefaultTravelers())
                .build();
    }
}
