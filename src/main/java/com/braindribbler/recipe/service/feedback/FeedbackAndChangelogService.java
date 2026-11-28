package com.braindribbler.recipe.service.feedback;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.feedback.ChangelogEntry;
import com.braindribbler.recipe.domain.feedback.ErrorLog;
import com.braindribbler.recipe.domain.feedback.UserFeedback;
import com.braindribbler.recipe.repository.feedback.ChangelogEntryRepository;
import com.braindribbler.recipe.repository.feedback.ErrorLogRepository;
import com.braindribbler.recipe.repository.feedback.UserFeedbackRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class FeedbackAndChangelogService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackAndChangelogService.class);
    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Set<String> VALID_FEEDBACK_TYPES = Set.of("BUG", "SUGGESTION", "ERROR_REPORT");
    private static final Set<String> VALID_STATUSES = Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "DISMISSED");
    private static final Set<String> VALID_CATEGORIES = Set.of("NEW", "IMPROVED", "FIXED");

    private final ErrorLogRepository errorLogRepository;
    private final UserFeedbackRepository userFeedbackRepository;
    private final ChangelogEntryRepository changelogEntryRepository;

    public FeedbackAndChangelogService(ErrorLogRepository errorLogRepository,
                                       UserFeedbackRepository userFeedbackRepository,
                                       ChangelogEntryRepository changelogEntryRepository) {
        this.errorLogRepository = errorLogRepository;
        this.userFeedbackRepository = userFeedbackRepository;
        this.changelogEntryRepository = changelogEntryRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ErrorLog recordErrorEvent(int statusCode, String requestUri, String httpMethod,
                                     Throwable ex, User user, String userAgent) {
        try {
            ErrorLog log = new ErrorLog();
            log.setReferenceCode(generateUniqueRefCode());
            log.setStatusCode(statusCode);
            log.setRequestUri(truncate(requestUri, 1000));
            log.setHttpMethod(truncate(httpMethod, 16));
            log.setUser(user);
            log.setUserAgent(truncate(userAgent, 500));

            if (ex != null) {
                log.setExceptionClass(ex.getClass().getName());
                log.setExceptionMessage(truncate(ex.getMessage(), 2000));
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                log.setStackTrace(truncate(sw.toString(), 20000));
            }
            return errorLogRepository.save(log);
        } catch (Exception loggingFailure) {
            logger.error("Failed to persist ErrorLog telemetry", loggingFailure);
            return null;
        }
    }

    @Transactional
    public UserFeedback submitFeedback(String rawType, String subject, String description,
                                       String pageUrl, String errorRef, String userAgent, User user) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Please enter a description before submitting.");
        }

        if (user != null) {
            long recentCount = userFeedbackRepository.countBySubmittedBy_UserIdAndCreatedAtAfter(
                    user.getUserId(), OffsetDateTime.now().minusHours(1));
            if (recentCount >= 8) {
                throw new IllegalStateException("You have submitted several reports recently. Please wait a bit before sending another.");
            }
        }

        String normalizedType = (rawType != null && VALID_FEEDBACK_TYPES.contains(rawType.toUpperCase()))
                ? rawType.toUpperCase()
                : "SUGGESTION";

        UserFeedback feedback = new UserFeedback();
        feedback.setFeedbackType(normalizedType);
        feedback.setStatus("OPEN");
        feedback.setSubject(subject != null && !subject.isBlank() ? truncate(subject.trim(), 200) : null);
        feedback.setDescription(truncate(description.trim(), 4000));
        feedback.setPageUrl(truncate(pageUrl, 1000));
        feedback.setUserAgent(truncate(userAgent, 500));
        feedback.setSubmittedBy(user);

        if (errorRef != null && !errorRef.isBlank()) {
            errorLogRepository.findByReferenceCode(errorRef.trim().toUpperCase())
                    .ifPresent(feedback::setErrorLog);
        }

        return userFeedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public List<UserFeedback> getAllFeedback() {
        return userFeedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long getOpenFeedbackCount() {
        return userFeedbackRepository.countByStatus("OPEN");
    }

    @Transactional(readOnly = true)
    public UserFeedback getFeedbackById(Integer feedbackId) {
        return userFeedbackRepository.findWithDetailsByFeedbackId(feedbackId)
                .orElseThrow(() -> new EntityNotFoundException("Feedback entry not found"));
    }

    @Transactional
    public void updateFeedbackTriage(Integer feedbackId, String status, String adminNotes) {
        UserFeedback fb = userFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new EntityNotFoundException("Feedback entry not found"));

        if (status != null && VALID_STATUSES.contains(status.toUpperCase())) {
            fb.setStatus(status.toUpperCase());
        }
        fb.setAdminNotes(adminNotes != null && !adminNotes.isBlank() ? truncate(adminNotes.trim(), 4000) : null);
        fb.setUpdatedAt(OffsetDateTime.now());
        userFeedbackRepository.save(fb);
    }

    @Transactional
    public void deleteFeedback(Integer feedbackId) {
        if (feedbackId != null) {
            userFeedbackRepository.deleteById(feedbackId);
        }
    }

    @Transactional
    public void deleteChangelogEntry(Integer changelogId) {
        if (changelogId != null) {
            changelogEntryRepository.deleteById(changelogId);
        }
    }

    @Transactional(readOnly = true)
    public List<ChangelogEntry> getPublishedChangelog() {
        return changelogEntryRepository.findByIsPublishedTrueOrderByPublishedDateDescChangelogIdDesc();
    }

    @Transactional(readOnly = true)
    public List<ChangelogEntry> getAllChangelogAdmin() {
        return changelogEntryRepository.findAllByOrderByPublishedDateDescChangelogIdDesc();
    }

    @Transactional(readOnly = true)
    public Integer getLatestPublishedChangelogId() {
        var latest = changelogEntryRepository.findFirstByIsPublishedTrueOrderByPublishedDateDescChangelogIdDesc();
        return latest.isPresent() ? latest.get().getChangelogId() : 0;
    }

    @Transactional
    public ChangelogEntry saveChangelogEntry(Integer changelogId, String title, String summary,
                                             String category, String versionTag, LocalDate publishedDate,
                                             boolean isPublished, User author) {
        if (title == null || title.isBlank() || summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Title and summary are required.");
        }

        ChangelogEntry entry = (changelogId != null)
                ? changelogEntryRepository.findById(changelogId).orElseGet(ChangelogEntry::new)
                : new ChangelogEntry();

        String normalizedCat = (category != null && VALID_CATEGORIES.contains(category.toUpperCase()))
                ? category.toUpperCase()
                : "NEW";

        entry.setTitle(truncate(title.trim(), 200));
        entry.setSummary(truncate(summary.trim(), 4000));
        entry.setCategory(normalizedCat);
        entry.setVersionTag(versionTag != null && !versionTag.isBlank() ? truncate(versionTag.trim(), 40) : null);
        entry.setPublishedDate(publishedDate != null ? publishedDate : LocalDate.now());
        entry.setIsPublished(isPublished);
        if (entry.getCreatedBy() == null) {
            entry.setCreatedBy(author);
        }

        return changelogEntryRepository.save(entry);
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeOldUnreferencedErrorLogs() {
        int deleted = errorLogRepository.deleteUnreferencedOlderThan(OffsetDateTime.now().minusDays(30));
        if (deleted > 0) {
            logger.info("Purged {} unreferenced error logs older than 30 days.", deleted);
        }
    }

    private String generateUniqueRefCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder("ERR-");
            for (int i = 0; i < 6; i++) {
                sb.append(HEX_CHARS.charAt(RANDOM.nextInt(HEX_CHARS.length())));
            }
            String candidate = sb.toString();
            if (errorLogRepository.findByReferenceCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "ERR-" + Long.toHexString(System.currentTimeMillis()).toUpperCase();
    }

    private String truncate(String val, int maxLen) {
        if (val == null) return null;
        return val.length() <= maxLen ? val : val.substring(0, maxLen);
    }

    public static String extractOriginalUri(HttpServletRequest request) {
        Object forwardUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String uri = (forwardUri != null) ? forwardUri.toString() : request.getRequestURI();
        String query = request.getQueryString();
        return (query != null && !query.isBlank() && forwardUri == null) ? uri + "?" + query : uri;
    }

    @Transactional(readOnly = true)
    public List<ErrorLog> getRecentErrorLogs() {
        return errorLogRepository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional
    public void deleteErrorLog(Integer errorLogId) {
        if (errorLogId != null) {
            errorLogRepository.deleteById(errorLogId);
        }
    }

    @Transactional
    public int purgeAllUnreferencedErrorLogs() {
        return errorLogRepository.deleteAllUnreferenced();
    }

}
