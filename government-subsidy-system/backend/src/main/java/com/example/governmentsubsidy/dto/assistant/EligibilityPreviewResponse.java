package com.example.governmentsubsidy.dto.assistant;

import com.example.governmentsubsidy.dto.eligibility.CriterionEvaluationDetail;

import java.math.BigDecimal;
import java.util.List;

public class EligibilityPreviewResponse {

    private Long schemeId;
    private String schemeTitle;
    private String schemeCode;
    private int totalScore;
    private int minQualifyingScore;
    private boolean eligible;
    private String matchLevel; // STRONG, MODERATE, WEAK
    private String recommendation;
    private BigDecimal minGrantAmount;
    private BigDecimal maxGrantAmount;
    private List<CriterionEvaluationDetail> criteriaDetails;

    public EligibilityPreviewResponse() {}

    public Long getSchemeId() { return schemeId; }
    public void setSchemeId(Long schemeId) { this.schemeId = schemeId; }

    public String getSchemeTitle() { return schemeTitle; }
    public void setSchemeTitle(String schemeTitle) { this.schemeTitle = schemeTitle; }

    public String getSchemeCode() { return schemeCode; }
    public void setSchemeCode(String schemeCode) { this.schemeCode = schemeCode; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public int getMinQualifyingScore() { return minQualifyingScore; }
    public void setMinQualifyingScore(int minQualifyingScore) { this.minQualifyingScore = minQualifyingScore; }

    public boolean isEligible() { return eligible; }
    public void setEligible(boolean eligible) { this.eligible = eligible; }

    public String getMatchLevel() { return matchLevel; }
    public void setMatchLevel(String matchLevel) { this.matchLevel = matchLevel; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public BigDecimal getMinGrantAmount() { return minGrantAmount; }
    public void setMinGrantAmount(BigDecimal minGrantAmount) { this.minGrantAmount = minGrantAmount; }

    public BigDecimal getMaxGrantAmount() { return maxGrantAmount; }
    public void setMaxGrantAmount(BigDecimal maxGrantAmount) { this.maxGrantAmount = maxGrantAmount; }

    public List<CriterionEvaluationDetail> getCriteriaDetails() { return criteriaDetails; }
    public void setCriteriaDetails(List<CriterionEvaluationDetail> criteriaDetails) { this.criteriaDetails = criteriaDetails; }
}
