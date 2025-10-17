package com.ignium.taskmanager.task.repository;

import com.ignium.taskmanager.task.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    
    Optional<Tag> findByName(String name);
    
    boolean existsByName(String name);
    
    Optional<Tag> findByNameAndUserId(String name, UUID userId);
    
    List<Tag> findAllByUserId(UUID userId);
}
