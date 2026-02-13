package com.weatherapp.repository;

import com.weatherapp.model.UserPreference;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserPreference documents.
 */
@Repository
public interface UserPreferenceRepository extends MongoRepository<UserPreference, String> {

    /** Find preferences by user ID */
    Optional<UserPreference> findByUserId(String userId);

    /** Check if preferences exist for a user */
    boolean existsByUserId(String userId);
}
