package com.ignium.taskmanager.task.service;

import com.ignium.taskmanager.config.exception.TagNotFoundException;
import com.ignium.taskmanager.task.TestDataFactory;
import com.ignium.taskmanager.task.dto.TagDetailResponse;
import com.ignium.taskmanager.task.dto.TagResponse;
import com.ignium.taskmanager.task.entity.Tag;
import com.ignium.taskmanager.task.entity.Task;
import com.ignium.taskmanager.task.repository.TagRepository;
import com.ignium.taskmanager.user.service.UserContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService Unit Tests")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private TagService tagService;

    @BeforeEach
    void setUp() {
        when(userContextService.getCurrentUserId()).thenReturn(TestDataFactory.TEST_USER_ID);
    }

    // READ Operations Tests

    @Test
    @DisplayName("getAllTags_shouldReturnUserTags")
    void getAllTags_shouldReturnUserTags() {
        // Arrange
        List<Tag> tags = TestDataFactory.createTagList();

        when(tagRepository.findAllByUserId(TestDataFactory.TEST_USER_ID)).thenReturn(tags);

        // Act
        List<TagResponse> result = tagService.getAllTags();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).name()).isEqualTo("work");
        assertThat(result.get(1).name()).isEqualTo("urgent");
        assertThat(result.get(2).name()).isEqualTo("personal");

        verify(tagRepository).findAllByUserId(TestDataFactory.TEST_USER_ID);
    }

    @Test
    @DisplayName("getAllTags_withTaskCounts_shouldReturnCorrectCounts")
    void getAllTags_withTaskCounts_shouldReturnCorrectCounts() {
        // Arrange
        Tag tagWithTasks = TestDataFactory.createTagWithTasks();
        List<Tag> tags = List.of(tagWithTasks);

        when(tagRepository.findAllByUserId(TestDataFactory.TEST_USER_ID)).thenReturn(tags);

        // Act
        List<TagResponse> result = tagService.getAllTags();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).taskCount()).isEqualTo(2); // tagWithTasks has 2 tasks
    }

    @Test
    @DisplayName("getAllTags_whenEmpty_shouldReturnEmptyList")
    void getAllTags_whenEmpty_shouldReturnEmptyList() {
        // Arrange
        when(tagRepository.findAllByUserId(TestDataFactory.TEST_USER_ID)).thenReturn(List.of());

        // Act
        List<TagResponse> result = tagService.getAllTags();

        // Assert
        assertThat(result).isEmpty();
        verify(tagRepository).findAllByUserId(TestDataFactory.TEST_USER_ID);
    }

    @Test
    @DisplayName("getTagById_withValidId_shouldReturnTag")
    void getTagById_withValidId_shouldReturnTag() {
        // Arrange
        UUID tagId = TestDataFactory.TEST_TAG_ID;
        Tag tag = TestDataFactory.createTag();

        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        // Act
        TagDetailResponse result = tagService.getTagById(tagId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(tagId);
        assertThat(result.name()).isEqualTo(tag.getName());
        assertThat(result.color()).isEqualTo(tag.getColor());

        verify(tagRepository).findById(tagId);
    }

    @Test
    @DisplayName("getTagById_withTasks_shouldIncludeUserTasksOnly")
    void getTagById_withTasks_shouldIncludeUserTasksOnly() {
        // Arrange
        UUID tagId = TestDataFactory.TEST_TAG_ID;
        Tag tag = TestDataFactory.createTagWithTasks();
        // Add a task from different user
        Task otherUserTask = TestDataFactory.createTaskWithDifferentUser();
        tag.getTasks().add(otherUserTask);

        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        // Act
        TagDetailResponse result = tagService.getTagById(tagId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.tasks()).hasSize(2); // Only tasks from current user
        assertThat(result.tasks()).noneMatch(task -> task.id().equals(otherUserTask.getId()));
    }

    @Test
    @DisplayName("getTagById_withInvalidId_shouldThrowNotFoundException")
    void getTagById_withInvalidId_shouldThrowNotFoundException() {
        // Arrange
        UUID tagId = UUID.randomUUID();

        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tagService.getTagById(tagId))
            .isInstanceOf(TagNotFoundException.class)
            .hasMessageContaining(tagId.toString());

        verify(tagRepository).findById(tagId);
    }

    @Test
    @DisplayName("getTagById_differentUser_shouldThrowNotFoundException")
    void getTagById_differentUser_shouldThrowNotFoundException() {
        // Arrange
        UUID tagId = TestDataFactory.TEST_TAG_ID;
        Tag tag = TestDataFactory.createTag();
        tag.setUserId(TestDataFactory.OTHER_USER_ID); // Different user

        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        // Act & Assert
        assertThatThrownBy(() -> tagService.getTagById(tagId))
            .isInstanceOf(TagNotFoundException.class);

        verify(tagRepository).findById(tagId);
    }

    // DELETE Operations Tests

    @Test
    @DisplayName("deleteTag_withNoTasks_shouldDeleteTag")
    void deleteTag_withNoTasks_shouldDeleteTag() {
        // Arrange
        UUID tagId = TestDataFactory.TEST_TAG_ID;
        Tag tag = TestDataFactory.createTag(); // No tasks by default

        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        // Act
        tagService.deleteTag(tagId);

        // Assert
        verify(tagRepository).findById(tagId);
        verify(tagRepository).delete(tag);
    }

    @Test
    @DisplayName("deleteTag_withTasks_shouldThrowIllegalStateException")
    void deleteTag_withTasks_shouldThrowIllegalStateException() {
        // Arrange
        UUID tagId = TestDataFactory.TEST_TAG_ID;
        Tag tag = TestDataFactory.createTagWithTasks(); // Has tasks

        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        // Act & Assert
        assertThatThrownBy(() -> tagService.deleteTag(tagId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot delete tag with ID")
            .hasMessageContaining("because it is associated with 2 tasks");

        verify(tagRepository).findById(tagId);
        verify(tagRepository, never()).delete(any(Tag.class));
    }

    @Test
    @DisplayName("deleteTag_withInvalidId_shouldThrowNotFoundException")
    void deleteTag_withInvalidId_shouldThrowNotFoundException() {
        // Arrange
        UUID tagId = UUID.randomUUID();

        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tagService.deleteTag(tagId))
            .isInstanceOf(TagNotFoundException.class);

        verify(tagRepository).findById(tagId);
        verify(tagRepository, never()).delete(any(Tag.class));
    }

    @Test
    @DisplayName("deleteTag_differentUser_shouldThrowNotFoundException")
    void deleteTag_differentUser_shouldThrowNotFoundException() {
        // Arrange
        UUID tagId = TestDataFactory.TEST_TAG_ID;
        Tag tag = TestDataFactory.createTag();
        tag.setUserId(TestDataFactory.OTHER_USER_ID); // Different user

        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        // Act & Assert
        assertThatThrownBy(() -> tagService.deleteTag(tagId))
            .isInstanceOf(TagNotFoundException.class);

        verify(tagRepository).findById(tagId);
        verify(tagRepository, never()).delete(any(Tag.class));
    }
}
