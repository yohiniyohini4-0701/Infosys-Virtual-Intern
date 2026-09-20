package com.example.governmentsubsidy.dto.assistant;

import com.example.governmentsubsidy.dto.eligibility.CriterionEvaluationDetail;

import java.math.BigDecimal;
import java.util.List;

public class SchemeRecommendationResponse {

    private Long schemeId;
    private String code;
    private String title;
    private String department;
    private String description;
    private BigDecimal minGrantAmount;
    private BigDecimal maxGrantAmount;
    private int matchScore;
    private int minEligibilityScore;
    private String matchLevel; // STRONG, MODERATE, WEAK
    private List<CriterionEvaluationDetail> criteriaBreakdown;

    public SchemeRecommendationResponse() {}

    public Long getSchemeId() { return schemeId; }
    public void setSchemeId(Long schemeId) { this.schemeId = schemeId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getMinGrantAmount() { return minGrantAmount; }
    public void setMinGrantAmount(BigDecimal minGrantAmount) { this.minGrantAmount = minGrantAmount; }

    public BigDecimal getMaxGrantAmount() { return maxGrantAmount; }
    public void setMaxGrantAmount(BigDecimal maxGrantAmount) { this.maxGrantAmount = maxGrantAmount; }

    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }

    public int getMinEligibilityScore() { return minEligibilityScore; }
    public void setMinEligibilityScore(int minEligibilityScore) { this.minEligibilityScore = minEligibilityScore; }

    public String getMatchLevel() { return matchLevel; }
    public void setMatchLevel(String matchLevel) { this.matchLevel = matchLevel; }

    public List<CriterionEvaluationDetail> getCriteriaBreakdown() { return criteriaBreakdown; }
    public void setCriteriaBreakdown(List<CriterionEvaluationDetail> criteriaBreakdown) { this.criteriaBreakdown = criteriaBreakdown; }
}
