package com.ignium.taskmanager.user.repository;

import com.ignium.taskmanager.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByPreferredUsername(String preferredUsername);

    boolean existsByEmail(String email);

    boolean existsByPreferredUsername(String preferredUsername);

    /**
     * Upsert (insert or update) a user using PostgreSQL's ON CONFLICT clause.
     * This method handles concurrent user creation atomically at the database level.
     */
    @Modifying
    @Query(value = """
        INSERT INTO users (id, email, full_name, preferred_username, last_login, created_at, updated_at, calendar_connected)
        VALUES (:#{#user.id}, :#{#user.email}, :#{#user.fullName}, :#{#user.preferredUsername}, :#{#user.lastLogin}, 
                COALESCE(:#{#user.createdAt}, NOW()), NOW(), COALESCE(:#{#user.calendarConnected}, false))
        ON CONFLICT (id) DO UPDATE SET
            email = EXCLUDED.email,
            full_name = EXCLUDED.full_name,
            preferred_username = EXCLUDED.preferred_username,
            last_login = EXCLUDED.last_login,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsertUser(@Param("user") AppUser user);
}
