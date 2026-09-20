package com.example.governmentsubsidy;

import com.example.governmentsubsidy.dto.assistant.AssistantResponse;
import com.example.governmentsubsidy.dto.assistant.EligibilityPreviewResponse;
import com.example.governmentsubsidy.dto.assistant.SchemeRecommendationResponse;
import com.example.governmentsubsidy.entity.User;
import com.example.governmentsubsidy.repository.UserRepository;
import com.example.governmentsubsidy.service.EligibilityPreviewService;
import com.example.governmentsubsidy.service.SchemeRecommendationService;
import com.example.governmentsubsidy.service.SmartAssistantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SmartAssistantAndRecommendationTest {

    @Autowired
    private SmartAssistantService assistantService;

    @Autowired
    private SchemeRecommendationService recommendationService;

    @Autowired
    private EligibilityPreviewService eligibilityPreviewService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Smart Assistant returns helpful response for general greeting")
    void testAssistantGeneralHelp() {
        User user = userRepository.findByUsername("farmer_john").orElse(null);
        AssistantResponse res = assistantService.answerQuery("Hello, how can you help me?", user);

        assertNotNull(res);
        assertEquals("GENERAL_HELP", res.getCategory());
        assertTrue(res.getAnswer().contains("Government Subsidy & Grant Information Assistant"));
    }

    @Test
    @DisplayName("Smart Assistant queries active schemes and returns scheme details")
    void testAssistantSchemeInquiry() {
        User user = userRepository.findByUsername("farmer_john").orElse(null);
        AssistantResponse res = assistantService.answerQuery("What schemes are available for agriculture?", user);

        assertNotNull(res);
        assertEquals("SCHEME_INQUIRY", res.getCategory());
        assertFalse(res.getAnswer().isBlank());
    }

    @Test
    @DisplayName("Smart Assistant answers status query for seeded beneficiary")
    void testAssistantApplicationStatus() {
        User user = userRepository.findByUsername("farmer_john").orElse(null);
        AssistantResponse res = assistantService.answerQuery("What is the status of my application?", user);

        assertNotNull(res);
        assertEquals("APPLICATION_STATUS", res.getCategory());
        assertFalse(res.getAnswer().isBlank());
    }

    @Test
    @DisplayName("Smart Assistant answers document checklist inquiry")
    void testAssistantDocumentQuery() {
        User user = userRepository.findByUsername("farmer_john").orElse(null);
        AssistantResponse res = assistantService.answerQuery("What documents are required to apply?", user);

        assertNotNull(res);
        assertEquals("DOCUMENTATION", res.getCategory());
        assertTrue(res.getAnswer().contains("Aadhaar"));
        assertTrue(res.getAnswer().contains("Bank Account Details"));
    }

    @Test
    @DisplayName("Scheme recommendation service returns ranked schemes for farmer_john")
    void testSchemeRecommendations() {
        User user = userRepository.findByUsername("farmer_john").orElseThrow();
        List<SchemeRecommendationResponse> recommendations = recommendationService.getRecommendationsForBeneficiary(user.getId());

        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        // Verify scores are non-negative and criteria breakdown is populated
        for (SchemeRecommendationResponse rec : recommendations) {
            assertTrue(rec.getMatchScore() >= 0);
            assertNotNull(rec.getMatchLevel());
            assertNotNull(rec.getCriteriaBreakdown());
        }
    }

    @Test
    @DisplayName("Eligibility preview evaluates scheme criteria without creating application in DB")
    void testEligibilityPreview() {
        User user = userRepository.findByUsername("farmer_john").orElseThrow();
        List<SchemeRecommendationResponse> recommendations = recommendationService.getRecommendationsForBeneficiary(user.getId());
        Long schemeId = recommendations.get(0).getSchemeId();

        EligibilityPreviewResponse preview = eligibilityPreviewService.previewEligibility(schemeId, user.getId());

        assertNotNull(preview);
        assertEquals(schemeId, preview.getSchemeId());
        assertTrue(preview.getTotalScore() >= 0);
        assertNotNull(preview.getCriteriaDetails());
        assertFalse(preview.getCriteriaDetails().isEmpty());
    }
}
