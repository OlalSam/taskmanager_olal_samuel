package com.ignium.taskmanager.task.controller;

import com.ignium.taskmanager.task.dto.TagDetailResponse;
import com.ignium.taskmanager.task.dto.TagResponse;
import com.ignium.taskmanager.task.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Slf4j
public class TagController {
    
    private final TagService tagService;
    
    @GetMapping
    public ResponseEntity<List<TagResponse>> getAllTags() {
        List<TagResponse> tags = tagService.getAllTags();
        return ResponseEntity.ok(tags);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TagDetailResponse> getTagById(@PathVariable UUID id) {
        TagDetailResponse tag = tagService.getTagById(id);
        return ResponseEntity.ok(tag);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable UUID id) {
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
