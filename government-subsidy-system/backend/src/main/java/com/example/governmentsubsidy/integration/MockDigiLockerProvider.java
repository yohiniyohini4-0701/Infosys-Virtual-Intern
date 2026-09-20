package com.example.governmentsubsidy.integration;

import com.example.governmentsubsidy.enums.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DigiLocker Demo Mode Provider.
 * Adheres strictly to government transparency requirements: clearly identifies itself as a
 * Sandbox / Simulation provider when official partner API credentials are not provisioned.
 */
@Component
public class MockDigiLockerProvider implements DocumentVerificationProvider {

    private static final Logger log = LoggerFactory.getLogger(MockDigiLockerProvider.class);

    @Value("${app.digilocker.mode:DEMO}")
    private String mode;

    @Override
    public ProviderStatus getProviderStatus() {
        boolean isDemo = "DEMO".equalsIgnoreCase(mode);
        return new ProviderStatus(
                "DigiLocker National Document Gateway",
                !isDemo,
                isDemo,
                isDemo ? "DigiLocker Sandbox (Demo Mode Active - Requires Official MeitY / Digital India API Onboarding for Production)"
                        : "DigiLocker Live Partner Gateway Connected",
                "MeitY Electronic Consent Framework v2.1 (Simulated)"
        );
    }

    @Override
    public List<VerifiedDocumentRecord> fetchBeneficiaryDocuments(String aadhaarOrIdentityNumber, String userConsentToken) {
        log.info("[DigiLocker Demo Mode] Retrieving digitally signed citizen documents for identity: {}", aadhaarOrIdentityNumber);

        List<VerifiedDocumentRecord> documents = new ArrayList<>();

        // Aadhaar Document
        documents.add(new VerifiedDocumentRecord(
                "DL-AADHAAR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                DocumentType.IDENTITY_PROOF,
                "UIDAI Resident Identity Card",
                "Unique Identification Authority of India (UIDAI)",
                "SHA256:UIDAI:" + UUID.randomUUID(),
                "digilocker://gov.uidai/aadhaar/" + aadhaarOrIdentityNumber,
                true,
                LocalDateTime.now().minusYears(2)
        ));

        // Income Certificate
        documents.add(new VerifiedDocumentRecord(
                "DL-INCOME-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                DocumentType.INCOME_CERTIFICATE,
                "State Revenue Department Certified Income Certificate",
                "State Revenue Department / District Collectorate",
                "SHA256:REV:" + UUID.randomUUID(),
                "digilocker://gov.state.revenue/income/cert_2026",
                true,
                LocalDateTime.now().minusMonths(3)
        ));

        // Land 7/12 Extract
        documents.add(new VerifiedDocumentRecord(
                "DL-LAND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                DocumentType.LAND_RECORD,
                "Digital Land Record Extract (RoR / 7/12 Patta)",
                "Land Records Information System (Bhoomi / Bhulekh)",
                "SHA256:LAND:" + UUID.randomUUID(),
                "digilocker://gov.state.land/ror/plot_9981",
                true,
                LocalDateTime.now().minusMonths(6)
        ));

        // Bank Account Proof
        documents.add(new VerifiedDocumentRecord(
                "DL-BANK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                DocumentType.BANK_PASSBOOK,
                "NPCI Aadhaar-Seeded DBT Bank Account Confirmation",
                "National Payments Corporation of India (NPCI) / PFMS",
                "SHA256:NPCI:" + UUID.randomUUID(),
                "digilocker://gov.npci.dbt/bank_mandate",
                true,
                LocalDateTime.now().minusMonths(1)
        ));

        return documents;
    }

    @Override
    public boolean verifyDocumentAuthenticity(String documentUri, String checksum) {
        log.info("[DigiLocker Demo Mode] Verifying cryptographic signature for URI: {}", documentUri);
        return documentUri != null && !documentUri.isBlank() && checksum != null && !checksum.isBlank();
    }
}
