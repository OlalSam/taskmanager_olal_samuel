package com.ignium.taskmanager.task.service;

import com.ignium.taskmanager.config.exception.TagNotFoundException;
import com.ignium.taskmanager.task.dto.TagDetailResponse;
import com.ignium.taskmanager.task.dto.TagResponse;
import com.ignium.taskmanager.task.entity.Tag;
import com.ignium.taskmanager.task.repository.TagRepository;
import com.ignium.taskmanager.user.service.UserContextService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TagService {
    
    private final TagRepository tagRepository;
    private final UserContextService userContextService;
    
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        UUID currentUserId = userContextService.getCurrentUserId();
        log.info("Fetching all tags for user: {}", currentUserId);
        return tagRepository.findAllByUserId(currentUserId).stream()
            .map(TagResponse::from)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public TagDetailResponse getTagById(UUID id) {
        UUID currentUserId = userContextService.getCurrentUserId();
        log.info("Fetching tag details for ID: {} and user: {}", id, currentUserId);
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new TagNotFoundException(id));
        
        // Verify user ownership
        if (!tag.getUserId().equals(currentUserId)) {
            throw new TagNotFoundException(id);
        }
        
        return TagDetailResponse.from(tag, currentUserId);
    }
    
    @Transactional
    public void deleteTag(UUID id) {
        UUID currentUserId = userContextService.getCurrentUserId();
        log.info("Deleting tag with ID: {} for user: {}", id, currentUserId);
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new TagNotFoundException(id));
        
        // Verify user ownership
        if (!tag.getUserId().equals(currentUserId)) {
            throw new TagNotFoundException(id);
        }
        
        // Check if tag has tasks
        if (!tag.getTasks().isEmpty()) {
            throw new IllegalStateException(
                "Cannot delete tag with ID " + id + 
                " because it is associated with " + tag.getTasks().size() + " tasks"
            );
        }
        
        tagRepository.delete(tag);
        log.info("Tag deleted successfully with ID: {}", id);
    }
}
