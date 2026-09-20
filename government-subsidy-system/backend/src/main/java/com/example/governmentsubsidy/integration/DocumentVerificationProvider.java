package com.example.governmentsubsidy.integration;

import com.example.governmentsubsidy.enums.DocumentType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enterprise abstraction for external document repository verification (e.g. DigiLocker, NIC, State e-District).
 * Allows plugging in live partner credentials when onboarded, or operating in explicit Sandbox / Demo mode.
 */
public interface DocumentVerificationProvider {

    record VerifiedDocumentRecord(
            String documentIdentifier,
            DocumentType documentType,
            String documentTitle,
            String issuerAuthority,
            String verificationChecksum,
            String documentUri,
            boolean verified,
            LocalDateTime issuedAt
    ) {}

    record ProviderStatus(
            String providerName,
            boolean isLivePartner,
            boolean isSandboxDemo,
            String statusMessage,
            String consentFramework
    ) {}

    ProviderStatus getProviderStatus();

    List<VerifiedDocumentRecord> fetchBeneficiaryDocuments(String aadhaarOrIdentityNumber, String userConsentToken);

    boolean verifyDocumentAuthenticity(String documentUri, String checksum);
}
