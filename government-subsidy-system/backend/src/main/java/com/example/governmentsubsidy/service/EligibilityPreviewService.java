package com.example.governmentsubsidy.service;

import com.example.governmentsubsidy.dto.assistant.EligibilityPreviewResponse;
import com.example.governmentsubsidy.dto.assistant.SchemeRecommendationResponse;
import com.example.governmentsubsidy.entity.Beneficiary;
import com.example.governmentsubsidy.entity.Scheme;
import com.example.governmentsubsidy.exception.ResourceNotFoundException;
import com.example.governmentsubsidy.repository.BeneficiaryRepository;
import com.example.governmentsubsidy.repository.SchemeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EligibilityPreviewService {

    private final SchemeRepository schemeRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRecommendationService recommendationService;

    public EligibilityPreviewService(SchemeRepository schemeRepository,
                                     BeneficiaryRepository beneficiaryRepository,
                                     SchemeRecommendationService recommendationService) {
        this.schemeRepository = schemeRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.recommendationService = recommendationService;
    }

    @Transactional(readOnly = true)
    public EligibilityPreviewResponse previewEligibility(Long schemeId, Long userId) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + schemeId));

        Beneficiary beneficiary = beneficiaryRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary profile not found for user ID: " + userId));

        SchemeRecommendationResponse eval = recommendationService.evaluateSchemeForBeneficiary(scheme, beneficiary);

        boolean isEligible = eval.getMatchScore() >= scheme.getMinEligibilityScore() && !"WEAK".equals(eval.getMatchLevel());

        String recommendation;
        if (eval.getMatchScore() >= scheme.getMinEligibilityScore() && !"WEAK".equals(eval.getMatchLevel())) {
            if (eval.getMatchScore() >= 80) {
                recommendation = "Highly Recommended — You meet all mandatory conditions and achieve a high eligibility score (" + eval.getMatchScore() + "/" + scheme.getMinEligibilityScore() + ").";
            } else {
                recommendation = "Eligible — You qualify for this scheme with a score of " + eval.getMatchScore() + " (Minimum: " + scheme.getMinEligibilityScore() + "). Standard verification workflow applies.";
            }
        } else {
            recommendation = "Criteria Gap Identified — Your current score is " + eval.getMatchScore() + " vs minimum required " + scheme.getMinEligibilityScore() + ", or a mandatory condition was not satisfied.";
        }

        EligibilityPreviewResponse preview = new EligibilityPreviewResponse();
        preview.setSchemeId(scheme.getId());
        preview.setSchemeCode(scheme.getCode());
        preview.setSchemeTitle(scheme.getTitle());
        preview.setTotalScore(eval.getMatchScore());
        preview.setMinQualifyingScore(scheme.getMinEligibilityScore());
        preview.setEligible(isEligible);
        preview.setMatchLevel(eval.getMatchLevel());
        preview.setRecommendation(recommendation);
        preview.setMinGrantAmount(scheme.getMinGrantAmount());
        preview.setMaxGrantAmount(scheme.getMaxGrantAmount());
        preview.setCriteriaDetails(eval.getCriteriaBreakdown());

        return preview;
    }
}
