package com.example.governmentsubsidy.dto.eligibility;

import java.util.List;

/**
 * Summary result of a bulk eligibility evaluation run.
 */
public class BulkEligibilityResponse {

    private int totalProcessed;
    private int successCount;
    private int failureCount;
    private int eligibleCount;
    private int ineligibleCount;
    private int highValueCount;
    private int flaggedCount;

    private List<BulkEligibilityResultItem> items;

    public BulkEligibilityResponse() {}

    // ── Getters and Setters ──────────────────────────────────────────────────

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public int getEligibleCount() {
        return eligibleCount;
    }

    public void setEligibleCount(int eligibleCount) {
        this.eligibleCount = eligibleCount;
    }

    public int getIneligibleCount() {
        return ineligibleCount;
    }

    public void setIneligibleCount(int ineligibleCount) {
        this.ineligibleCount = ineligibleCount;
    }

    public int getHighValueCount() {
        return highValueCount;
    }

    public void setHighValueCount(int highValueCount) {
        this.highValueCount = highValueCount;
    }

    public int getFlaggedCount() {
        return flaggedCount;
    }

    public void setFlaggedCount(int flaggedCount) {
        this.flaggedCount = flaggedCount;
    }

    public List<BulkEligibilityResultItem> getItems() {
        return items;
    }

    public void setItems(List<BulkEligibilityResultItem> items) {
        this.items = items;
    }
}
