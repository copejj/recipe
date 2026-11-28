package com.braindribbler.recipe.repository.feedback;

import com.braindribbler.recipe.domain.feedback.UserFeedback;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Integer> {
    @EntityGraph(attributePaths = {"submittedBy", "errorLog"})
    List<UserFeedback> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"submittedBy", "errorLog"})
    Optional<UserFeedback> findWithDetailsByFeedbackId(Integer feedbackId);

    long countBySubmittedBy_UserIdAndCreatedAtAfter(Integer userId, OffsetDateTime after);
    long countByStatus(String status);
}
