package com.braindribbler.recipe.repository.feedback;

import com.braindribbler.recipe.domain.feedback.ChangelogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChangelogEntryRepository extends JpaRepository<ChangelogEntry, Integer> {
    List<ChangelogEntry> findByIsPublishedTrueOrderByPublishedDateDescChangelogIdDesc();
    List<ChangelogEntry> findAllByOrderByPublishedDateDescChangelogIdDesc();
    Optional<ChangelogEntry> findFirstByIsPublishedTrueOrderByPublishedDateDescChangelogIdDesc();
}
