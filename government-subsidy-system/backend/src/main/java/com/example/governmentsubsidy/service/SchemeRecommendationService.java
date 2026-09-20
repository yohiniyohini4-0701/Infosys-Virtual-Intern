package com.example.governmentsubsidy.service;

import com.example.governmentsubsidy.dto.assistant.SchemeRecommendationResponse;
import com.example.governmentsubsidy.dto.eligibility.CriterionEvaluationDetail;
import com.example.governmentsubsidy.entity.Beneficiary;
import com.example.governmentsubsidy.entity.EligibilityCriterion;
import com.example.governmentsubsidy.entity.Scheme;
import com.example.governmentsubsidy.enums.ComparisonOperator;
import com.example.governmentsubsidy.exception.ResourceNotFoundException;
import com.example.governmentsubsidy.repository.BeneficiaryRepository;
import com.example.governmentsubsidy.repository.SchemeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class SchemeRecommendationService {

    private final SchemeRepository schemeRepository;
    private final BeneficiaryRepository beneficiaryRepository;

    public SchemeRecommendationService(SchemeRepository schemeRepository,
                                       BeneficiaryRepository beneficiaryRepository) {
        this.schemeRepository = schemeRepository;
        this.beneficiaryRepository = beneficiaryRepository;
    }

    @Transactional(readOnly = true)
    public List<SchemeRecommendationResponse> getRecommendationsForBeneficiary(Long userId) {
        Beneficiary beneficiary = beneficiaryRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary profile not found for user ID: " + userId));

        List<Scheme> activeSchemes = schemeRepository.findByActiveTrue();
        List<SchemeRecommendationResponse> recommendations = new ArrayList<>();

        for (Scheme scheme : activeSchemes) {
            SchemeRecommendationResponse rec = evaluateSchemeForBeneficiary(scheme, beneficiary);
            recommendations.add(rec);
        }

        // Sort descending by match score
        recommendations.sort(Comparator.comparingInt(SchemeRecommendationResponse::getMatchScore).reversed());
        return recommendations;
    }

    public SchemeRecommendationResponse evaluateSchemeForBeneficiary(Scheme scheme, Beneficiary beneficiary) {
        List<EligibilityCriterion> criteria = scheme.getCriteria();
        List<CriterionEvaluationDetail> details = new ArrayList<>();

        int totalAwardedPoints = 0;
        int maxPossiblePoints = 0;
        boolean allMandatorySatisfied = true;

        if (criteria == null || criteria.isEmpty()) {
            SchemeRecommendationResponse response = new SchemeRecommendationResponse();
            response.setSchemeId(scheme.getId());
            response.setCode(scheme.getCode());
            response.setTitle(scheme.getTitle());
            response.setDepartment(scheme.getDepartment());
            response.setDescription(scheme.getDescription());
            response.setMinGrantAmount(scheme.getMinGrantAmount());
            response.setMaxGrantAmount(scheme.getMaxGrantAmount());
            response.setMatchScore(100);
            response.setMinEligibilityScore(scheme.getMinEligibilityScore());
            response.setMatchLevel("STRONG");
            response.setCriteriaBreakdown(details);
            return response;
        }

        for (EligibilityCriterion criterion : criteria) {
            maxPossiblePoints += criterion.getWeightPoints();
            String actualValue = resolveActualValue(beneficiary, criterion);
            boolean satisfied = evaluateCondition(actualValue, criterion.getComparisonOperator(), criterion.getExpectedValue());

            int awardedPoints = 0;
            if (satisfied) {
                awardedPoints = criterion.getWeightPoints();
                totalAwardedPoints += awardedPoints;
            } else if (criterion.isMandatory()) {
                allMandatorySatisfied = false;
            }

            String msg = satisfied ? "Condition satisfied" : (criterion.isMandatory() ? "Mandatory requirement not met" : "Optional condition not met");

            CriterionEvaluationDetail detail = new CriterionEvaluationDetail(
                    criterion.getCriterionType(),
                    criterion.getComparisonOperator(),
                    criterion.getExpectedValue(),
                    actualValue,
                    satisfied,
                    criterion.isMandatory(),
                    awardedPoints,
                    criterion.getWeightPoints(),
                    msg
            );
            details.add(detail);
        }

        String matchLevel;
        if (!allMandatorySatisfied) {
            matchLevel = "WEAK";
        } else if (totalAwardedPoints >= 80) {
            matchLevel = "STRONG";
        } else if (totalAwardedPoints >= scheme.getMinEligibilityScore()) {
            matchLevel = "MODERATE";
        } else {
            matchLevel = "WEAK";
        }

        SchemeRecommendationResponse response = new SchemeRecommendationResponse();
        response.setSchemeId(scheme.getId());
        response.setCode(scheme.getCode());
        response.setTitle(scheme.getTitle());
        response.setDepartment(scheme.getDepartment());
        response.setDescription(scheme.getDescription());
        response.setMinGrantAmount(scheme.getMinGrantAmount());
        response.setMaxGrantAmount(scheme.getMaxGrantAmount());
        response.setMatchScore(totalAwardedPoints);
        response.setMinEligibilityScore(scheme.getMinEligibilityScore());
        response.setMatchLevel(matchLevel);
        response.setCriteriaBreakdown(details);

        return response;
    }

    private String resolveActualValue(Beneficiary b, EligibilityCriterion c) {
        return switch (c.getCriterionType()) {
            case ANNUAL_INCOME -> b.getAnnualIncome() != null ? b.getAnnualIncome().toPlainString() : "0";
            case BENEFICIARY_CATEGORY -> b.getCategory() != null ? b.getCategory().name() : "";
            case AGE -> {
                if (b.getDateOfBirth() == null) yield "0";
                int age = Period.between(b.getDateOfBirth(), LocalDate.now()).getYears();
                yield String.valueOf(age);
            }
            case REGION_CODE -> (b.getRegion() != null && b.getRegion().getCode() != null) ? b.getRegion().getCode() : "";
            case LAND_HOLDING -> b.getLandHoldingHectares() != null ? b.getLandHoldingHectares().toPlainString() : "0";
            case DISABILITY_STATUS -> String.valueOf(b.isDisabled());
        };
    }

    private boolean evaluateCondition(String actual, ComparisonOperator op, String expected) {
        if (actual == null || expected == null) return false;

        try {
            switch (op) {
                case LESS_THAN_OR_EQUAL -> {
                    BigDecimal act = new BigDecimal(actual.trim());
                    BigDecimal exp = new BigDecimal(expected.trim());
                    return act.compareTo(exp) <= 0;
                }
                case GREATER_THAN_OR_EQUAL -> {
                    BigDecimal act = new BigDecimal(actual.trim());
                    BigDecimal exp = new BigDecimal(expected.trim());
                    return act.compareTo(exp) >= 0;
                }
                case EQUALS -> {
                    return actual.trim().equalsIgnoreCase(expected.trim());
                }
                case NOT_EQUALS -> {
                    return !actual.trim().equalsIgnoreCase(expected.trim());
                }
                case IN -> {
                    String[] allowed = expected.split(",");
                    return Arrays.stream(allowed)
                            .map(String::trim)
                            .anyMatch(val -> val.equalsIgnoreCase(actual.trim()));
                }
                case CONTAINS -> {
                    return actual.toLowerCase().contains(expected.trim().toLowerCase());
                }
            }
        } catch (NumberFormatException ignored) {
            return actual.trim().equalsIgnoreCase(expected.trim());
        }
        return false;
    }
}
