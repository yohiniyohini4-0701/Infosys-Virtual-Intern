package com.example.governmentsubsidy.service;

import com.example.governmentsubsidy.dto.eligibility.BulkEligibilityResponse;
import com.example.governmentsubsidy.dto.eligibility.BulkEligibilityResultItem;
import com.example.governmentsubsidy.dto.eligibility.EligibilityEvaluationResult;
import com.example.governmentsubsidy.entity.SubsidyApplication;
import com.example.governmentsubsidy.enums.ApplicationStatus;
import com.example.governmentsubsidy.enums.RiskLevel;
import com.example.governmentsubsidy.repository.SubsidyApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates bulk eligibility evaluation for all SUBMITTED applications.
 *
 * <p>This service is intentionally thin: it delegates actual eligibility
 * calculation to {@link ApplicationService#evaluateEligibility} which in turn
 * delegates to {@link EligibilityService#evaluate}. There is no duplicate
 * scoring logic here — EligibilityService remains the single source of truth.
 *
 * <p>If one application throws an exception, the batch continues processing the
 * remaining applications. The failure is captured and included in the summary.
 */
@Service
public class BulkEligibilityEvaluationService {

    private final SubsidyApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final AuditLogService auditLogService;

    public BulkEligibilityEvaluationService(SubsidyApplicationRepository applicationRepository,
                                             ApplicationService applicationService,
                                             AuditLogService auditLogService) {
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
        this.auditLogService = auditLogService;
    }

    /**
     * Evaluates all applications currently in {@link ApplicationStatus#SUBMITTED} status.
     *
     * @param actorUsername the username of the FIELD_OFFICER performing the action
     * @return {@link BulkEligibilityResponse} with per-application outcomes and summary counts
     */
    public BulkEligibilityResponse evaluateAllPending(String actorUsername) {
        // Fetch only SUBMITTED applications — the only status eligible for evaluation.
        // No unnecessary loading of other statuses.
        List<SubsidyApplication> pendingApplications =
                applicationRepository.findByStatus(ApplicationStatus.SUBMITTED);

        List<BulkEligibilityResultItem> items = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        int eligibleCount = 0;
        int ineligibleCount = 0;
        int highValueCount = 0;
        int flaggedCount = 0;

        for (SubsidyApplication app : pendingApplications) {
            BulkEligibilityResultItem item = new BulkEligibilityResultItem();
            item.setApplicationId(app.getId());
            item.setApplicationNumber(app.getApplicationNumber());
            item.setBeneficiaryName(
                    app.getBeneficiary().getUser() != null
                            ? app.getBeneficiary().getUser().getFullName()
                            : "Unknown"
            );

            try {
                // Delegate to ApplicationService — which calls EligibilityService internally.
                // This also saves the result, transitions status, and writes the per-application audit log.
                EligibilityEvaluationResult result =
                        applicationService.evaluateEligibility(app.getId(), actorUsername);

                item.setSuccess(true);
                item.setEligible(result.isEligible());
                item.setScore(result.getTotalScore());
                item.setRiskLevel(result.getAssignedRiskLevel());
                item.setRecommendation(result.getRecommendation());
                item.setNewStatus(result.isEligible()
                        ? ApplicationStatus.FIELD_VERIFICATION
                        : ApplicationStatus.REJECTED);

                successCount++;
                if (result.isEligible()) {
                    eligibleCount++;
                } else {
                    ineligibleCount++;
                }
                if (result.getAssignedRiskLevel() == RiskLevel.HIGH_VALUE) {
                    highValueCount++;
                } else if (result.getAssignedRiskLevel() == RiskLevel.FLAGGED) {
                    flaggedCount++;
                }

            } catch (Exception ex) {
                // Do NOT abort the batch — capture the failure and move on.
                item.setSuccess(false);
                item.setFailureReason(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
                failureCount++;
            }

            items.add(item);
        }

        int totalProcessed = pendingApplications.size();

        // Single bulk audit log entry summarising the entire run.
        auditLogService.logAction(
                actorUsername,
                "BULK_ELIGIBILITY_EVALUATED",
                "SubsidyApplication",
                "BULK",
                null,
                null,
                String.format(
                        "Bulk evaluation by %s: total=%d, success=%d, failed=%d, eligible=%d, ineligible=%d, highValue=%d, flagged=%d",
                        actorUsername, totalProcessed, successCount, failureCount,
                        eligibleCount, ineligibleCount, highValueCount, flaggedCount
                )
        );

        BulkEligibilityResponse response = new BulkEligibilityResponse();
        response.setTotalProcessed(totalProcessed);
        response.setSuccessCount(successCount);
        response.setFailureCount(failureCount);
        response.setEligibleCount(eligibleCount);
        response.setIneligibleCount(ineligibleCount);
        response.setHighValueCount(highValueCount);
        response.setFlaggedCount(flaggedCount);
        response.setItems(items);

        return response;
    }
}
