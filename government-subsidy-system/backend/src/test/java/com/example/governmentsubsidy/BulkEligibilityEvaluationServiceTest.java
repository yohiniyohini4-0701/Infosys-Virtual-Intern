package com.example.governmentsubsidy;

import com.example.governmentsubsidy.dto.eligibility.BulkEligibilityResponse;
import com.example.governmentsubsidy.dto.eligibility.EligibilityEvaluationResult;
import com.example.governmentsubsidy.entity.*;
import com.example.governmentsubsidy.enums.ApplicationStatus;
import com.example.governmentsubsidy.enums.BeneficiaryCategory;
import com.example.governmentsubsidy.enums.ComparisonOperator;
import com.example.governmentsubsidy.enums.CriterionType;
import com.example.governmentsubsidy.enums.RiskLevel;
import com.example.governmentsubsidy.exception.InvalidStatusTransitionException;
import com.example.governmentsubsidy.repository.SubsidyApplicationRepository;
import com.example.governmentsubsidy.service.ApplicationService;
import com.example.governmentsubsidy.service.AuditLogService;
import com.example.governmentsubsidy.service.BulkEligibilityEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkEligibilityEvaluationServiceTest {

    @Mock
    private SubsidyApplicationRepository applicationRepository;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BulkEligibilityEvaluationService bulkService;

    private SubsidyApplication makeApp(Long id, String appNum, ApplicationStatus status) {
        // Minimal Scheme
        Scheme scheme = new Scheme();
        scheme.setId(1L);
        scheme.setCode("SCH-001");
        scheme.setTitle("Test Scheme");
        scheme.setMinEligibilityScore(60);
        scheme.setMinGrantAmount(new BigDecimal("1000"));
        scheme.setMaxGrantAmount(new BigDecimal("500000"));

        // Minimal Beneficiary / User
        User user = new User();
        user.setFullName("Beneficiary " + id);
        user.setUsername("user" + id);

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setId(id);
        beneficiary.setUser(user);
        beneficiary.setCategory(BeneficiaryCategory.OBC);
        beneficiary.setAnnualIncome(new BigDecimal("100000"));
        beneficiary.setLandHoldingHectares(new BigDecimal("1.0"));
        beneficiary.setDateOfBirth(LocalDate.of(1985, 1, 1));

        Region region = new Region();
        region.setId(1L);
        region.setDistrictName("TestDistrict");
        region.setStateName("TestState");

        SubsidyApplication app = new SubsidyApplication();
        app.setId(id);
        app.setApplicationNumber(appNum);
        app.setBeneficiary(beneficiary);
        app.setScheme(scheme);
        app.setRegion(region);
        app.setAppliedAmount(new BigDecimal("50000"));
        app.setStatus(status);
        return app;
    }

    private EligibilityEvaluationResult makeResult(Long appId, String appNum, boolean eligible, int score, RiskLevel risk) {
        EligibilityEvaluationResult r = new EligibilityEvaluationResult();
        r.setApplicationId(appId);
        r.setApplicationNumber(appNum);
        r.setEligible(eligible);
        r.setTotalScore(score);
        r.setMinQualifyingScore(60);
        r.setAssignedRiskLevel(risk);
        r.setRecommendation(eligible ? "Eligible" : "Ineligible");
        r.setCriteriaDetails(List.of());
        return r;
    }

    @Test
    @DisplayName("Bulk evaluation processes all SUBMITTED applications")
    void testBulkEvaluationProcessesAllSubmittedApplications() throws Exception {
        SubsidyApplication app1 = makeApp(1L, "SUB-001", ApplicationStatus.SUBMITTED);
        SubsidyApplication app2 = makeApp(2L, "SUB-002", ApplicationStatus.SUBMITTED);
        SubsidyApplication app3 = makeApp(3L, "SUB-003", ApplicationStatus.SUBMITTED);

        when(applicationRepository.findByStatus(ApplicationStatus.SUBMITTED))
                .thenReturn(List.of(app1, app2, app3));

        when(applicationService.evaluateEligibility(1L, "officer1"))
                .thenReturn(makeResult(1L, "SUB-001", true, 100, RiskLevel.LOW));
        when(applicationService.evaluateEligibility(2L, "officer1"))
                .thenReturn(makeResult(2L, "SUB-002", true, 80, RiskLevel.LOW));
        when(applicationService.evaluateEligibility(3L, "officer1"))
                .thenReturn(makeResult(3L, "SUB-003", false, 30, RiskLevel.LOW));

        BulkEligibilityResponse response = bulkService.evaluateAllPending("officer1");

        assertEquals(3, response.getTotalProcessed());
        assertEquals(3, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertEquals(2, response.getEligibleCount());
        assertEquals(1, response.getIneligibleCount());
        assertEquals(3, response.getItems().size());

        verify(applicationService, times(3)).evaluateEligibility(anyLong(), eq("officer1"));
    }

    @Test
    @DisplayName("Bulk evaluation only fetches SUBMITTED applications from repo")
    void testBulkEvaluationOnlyFetchesSubmittedFromRepo() {
        when(applicationRepository.findByStatus(ApplicationStatus.SUBMITTED))
                .thenReturn(List.of());

        BulkEligibilityResponse response = bulkService.evaluateAllPending("officer1");

        assertEquals(0, response.getTotalProcessed());
        verify(applicationRepository).findByStatus(ApplicationStatus.SUBMITTED);
        // Must NOT query other statuses
        verify(applicationRepository, never()).findByStatus(ApplicationStatus.DRAFT);
        verify(applicationRepository, never()).findByStatus(ApplicationStatus.FIELD_VERIFICATION);
    }

    @Test
    @DisplayName("Bulk evaluation continues on partial failure — failed app does not abort the batch")
    void testBulkEvaluationContinuesOnPartialFailure() throws Exception {
        SubsidyApplication app1 = makeApp(1L, "SUB-001", ApplicationStatus.SUBMITTED);
        SubsidyApplication app2 = makeApp(2L, "SUB-002", ApplicationStatus.SUBMITTED);
        SubsidyApplication app3 = makeApp(3L, "SUB-003", ApplicationStatus.SUBMITTED);

        when(applicationRepository.findByStatus(ApplicationStatus.SUBMITTED))
                .thenReturn(List.of(app1, app2, app3));

        when(applicationService.evaluateEligibility(1L, "officer1"))
                .thenReturn(makeResult(1L, "SUB-001", true, 90, RiskLevel.LOW));
        // App 2 throws an unexpected error
        when(applicationService.evaluateEligibility(2L, "officer1"))
                .thenThrow(new RuntimeException("Database connectivity issue"));
        when(applicationService.evaluateEligibility(3L, "officer1"))
                .thenReturn(makeResult(3L, "SUB-003", true, 80, RiskLevel.LOW));

        BulkEligibilityResponse response = bulkService.evaluateAllPending("officer1");

        assertEquals(3, response.getTotalProcessed());
        assertEquals(2, response.getSuccessCount());
        assertEquals(1, response.getFailureCount());
        assertEquals(2, response.getEligibleCount());
        assertEquals(0, response.getIneligibleCount());

        // Verify the failure item
        var failedItem = response.getItems().stream()
                .filter(i -> !i.isSuccess())
                .findFirst();
        assertTrue(failedItem.isPresent());
        assertEquals("SUB-002", failedItem.get().getApplicationNumber());
        assertTrue(failedItem.get().getFailureReason().contains("Database connectivity issue"));
    }

    @Test
    @DisplayName("Bulk evaluation writes a single bulk audit log entry")
    void testBulkEvaluationRecordsAuditLog() throws Exception {
        SubsidyApplication app1 = makeApp(1L, "SUB-001", ApplicationStatus.SUBMITTED);

        when(applicationRepository.findByStatus(ApplicationStatus.SUBMITTED))
                .thenReturn(List.of(app1));
        when(applicationService.evaluateEligibility(1L, "officer1"))
                .thenReturn(makeResult(1L, "SUB-001", true, 100, RiskLevel.LOW));

        bulkService.evaluateAllPending("officer1");

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditLogService).logAction(
                eq("officer1"),
                actionCaptor.capture(),
                eq("SubsidyApplication"),
                eq("BULK"),
                isNull(),
                isNull(),
                detailsCaptor.capture()
        );

        assertEquals("BULK_ELIGIBILITY_EVALUATED", actionCaptor.getValue());
        String details = detailsCaptor.getValue();
        assertTrue(details.contains("total=1"));
        assertTrue(details.contains("success=1"));
        assertTrue(details.contains("failed=0"));
    }

    @Test
    @DisplayName("Bulk evaluation correctly counts HIGH_VALUE and FLAGGED risk applications")
    void testBulkEvaluationCountsRiskLevels() throws Exception {
        SubsidyApplication app1 = makeApp(1L, "SUB-001", ApplicationStatus.SUBMITTED);
        SubsidyApplication app2 = makeApp(2L, "SUB-002", ApplicationStatus.SUBMITTED);
        SubsidyApplication app3 = makeApp(3L, "SUB-003", ApplicationStatus.SUBMITTED);

        when(applicationRepository.findByStatus(ApplicationStatus.SUBMITTED))
                .thenReturn(List.of(app1, app2, app3));
        when(applicationService.evaluateEligibility(1L, "officer1"))
                .thenReturn(makeResult(1L, "SUB-001", true, 100, RiskLevel.HIGH_VALUE));
        when(applicationService.evaluateEligibility(2L, "officer1"))
                .thenReturn(makeResult(2L, "SUB-002", true, 65, RiskLevel.FLAGGED));
        when(applicationService.evaluateEligibility(3L, "officer1"))
                .thenReturn(makeResult(3L, "SUB-003", true, 90, RiskLevel.LOW));

        BulkEligibilityResponse response = bulkService.evaluateAllPending("officer1");

        assertEquals(1, response.getHighValueCount());
        assertEquals(1, response.getFlaggedCount());
    }

    @Test
    @DisplayName("Empty SUBMITTED queue returns zero counts and empty items list")
    void testEmptyQueueReturnsZeroCounts() {
        when(applicationRepository.findByStatus(ApplicationStatus.SUBMITTED))
                .thenReturn(List.of());

        BulkEligibilityResponse response = bulkService.evaluateAllPending("officer1");

        assertEquals(0, response.getTotalProcessed());
        assertEquals(0, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertEquals(0, response.getEligibleCount());
        assertEquals(0, response.getIneligibleCount());
        assertNotNull(response.getItems());
        assertTrue(response.getItems().isEmpty());
    }
}
