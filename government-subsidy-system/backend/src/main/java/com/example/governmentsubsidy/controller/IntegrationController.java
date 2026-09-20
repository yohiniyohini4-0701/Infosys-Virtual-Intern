package com.example.governmentsubsidy.controller;

import com.example.governmentsubsidy.dto.common.ApiResponse;
import com.example.governmentsubsidy.integration.DocumentVerificationProvider;
import com.example.governmentsubsidy.service.ExternalBeneficiarySyncService;
import com.example.governmentsubsidy.service.TreasuryIntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private final TreasuryIntegrationService treasuryService;
    private final ExternalBeneficiarySyncService externalSyncService;
    private final DocumentVerificationProvider documentVerificationProvider;

    public IntegrationController(TreasuryIntegrationService treasuryService,
                                 ExternalBeneficiarySyncService externalSyncService,
                                 DocumentVerificationProvider documentVerificationProvider) {
        this.treasuryService = treasuryService;
        this.externalSyncService = externalSyncService;
        this.documentVerificationProvider = documentVerificationProvider;
    }

    @PostMapping("/treasury/test-transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<TreasuryIntegrationService.TreasuryDisbursementResult>> testTreasury(
            @RequestParam String accountNumber,
            @RequestParam String ifsc,
            @RequestParam BigDecimal amount,
            @RequestParam String schemeCode) {
        TreasuryIntegrationService.TreasuryDisbursementResult res =
                treasuryService.processTreasuryTransfer(accountNumber, ifsc, amount, schemeCode);
        return ResponseEntity.ok(ApiResponse.success("Treasury transfer simulated successfully", res));
    }

    @GetMapping("/beneficiary/verify-identity")
    @PreAuthorize("hasAnyRole('ADMIN', 'FIELD_OFFICER', 'DISTRICT_OFFICER', 'FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<ExternalBeneficiarySyncService.IdentityVerificationResponse>> verifyIdentity(
            @RequestParam String identityNumber) {
        ExternalBeneficiarySyncService.IdentityVerificationResponse res =
                externalSyncService.verifyIdentityWithNationalRegistry(identityNumber);
        return ResponseEntity.ok(ApiResponse.success("Identity verification check completed", res));
    }

    @GetMapping("/digilocker/status")
    public ResponseEntity<ApiResponse<DocumentVerificationProvider.ProviderStatus>> getDigiLockerStatus() {
        DocumentVerificationProvider.ProviderStatus status = documentVerificationProvider.getProviderStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PostMapping("/digilocker/pull-documents")
    public ResponseEntity<ApiResponse<List<DocumentVerificationProvider.VerifiedDocumentRecord>>> pullDigiLockerDocuments(
            @RequestParam String identityNumber,
            @RequestParam(defaultValue = "CONSENT_GRANTED_ELECTRONIC") String consentToken) {
        List<DocumentVerificationProvider.VerifiedDocumentRecord> docs =
                documentVerificationProvider.fetchBeneficiaryDocuments(identityNumber, consentToken);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved from DigiLocker [DEMO MODE]", docs));
    }
}
