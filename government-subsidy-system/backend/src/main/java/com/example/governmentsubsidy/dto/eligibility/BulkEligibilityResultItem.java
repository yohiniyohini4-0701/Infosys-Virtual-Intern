package com.example.governmentsubsidy.dto.eligibility;

import com.example.governmentsubsidy.enums.ApplicationStatus;
import com.example.governmentsubsidy.enums.RiskLevel;

/**
 * Represents the outcome of evaluating a single application during a bulk run.
 */
public class BulkEligibilityResultItem {

    private Long applicationId;
    private String applicationNumber;
    private String beneficiaryName;

    /** True if the evaluation call completed without a system error. */
    private boolean success;

    /** True if the applicant is eligible (only meaningful when success=true). */
    private boolean eligible;

    private int score;
    private RiskLevel riskLevel;
    private ApplicationStatus newStatus;
    private String recommendation;

    /** Populated only when success=false. */
    private String failureReason;

    public BulkEligibilityResultItem() {}

    // ── Getters and Setters ──────────────────────────────────────────────────

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public void setApplicationNumber(String applicationNumber) {
        this.applicationNumber = applicationNumber;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public ApplicationStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ApplicationStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
