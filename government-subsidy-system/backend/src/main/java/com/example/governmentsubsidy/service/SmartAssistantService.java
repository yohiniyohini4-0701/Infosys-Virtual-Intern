package com.example.governmentsubsidy.service;

import com.example.governmentsubsidy.dto.assistant.AssistantResponse;
import com.example.governmentsubsidy.dto.assistant.SchemeRecommendationResponse;
import com.example.governmentsubsidy.entity.*;
import com.example.governmentsubsidy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmartAssistantService {

    private final SchemeRepository schemeRepository;
    private final SubsidyApplicationRepository applicationRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRecommendationService recommendationService;

    public SmartAssistantService(SchemeRepository schemeRepository,
                                 SubsidyApplicationRepository applicationRepository,
                                 BeneficiaryRepository beneficiaryRepository,
                                 SchemeRecommendationService recommendationService) {
        this.schemeRepository = schemeRepository;
        this.applicationRepository = applicationRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.recommendationService = recommendationService;
    }

    @Transactional(readOnly = true)
    public AssistantResponse answerQuery(String queryText, User currentUser) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return new AssistantResponse(
                    "Please provide a question or topic so I can assist you with government schemes, your application status, or eligibility rules.",
                    "GENERAL_HELP"
            );
        }

        String lower = queryText.toLowerCase().trim();
        Optional<Beneficiary> beneficiaryOpt = (currentUser != null)
                ? beneficiaryRepository.findByUserId(currentUser.getId())
                : Optional.empty();

        // 1. Application status / tracking query
        if (lower.contains("status") || lower.contains("track") || lower.contains("my application") || lower.contains("application number") || lower.contains("sub-")) {
            return handleApplicationStatusQuery(lower, beneficiaryOpt);
        }

        // 2. Eligibility & criteria preview query
        if (lower.contains("eligible") || lower.contains("eligibility") || lower.contains("qualify") || lower.contains("score") || lower.contains("criteria")) {
            return handleEligibilityQuery(lower, beneficiaryOpt);
        }

        // 3. Milestone & disbursement query
        if (lower.contains("milestone") || lower.contains("disburse") || lower.contains("dbt") || lower.contains("payment") || lower.contains("money") || lower.contains("fund") || lower.contains("tranche")) {
            return handleMilestoneDisbursementQuery(beneficiaryOpt);
        }

        // 4. Document requirements
        if (lower.contains("document") || lower.contains("upload") || lower.contains("aadhaar") || lower.contains("pan") || lower.contains("certificate") || lower.contains("proof")) {
            return handleDocumentQuery(lower);
        }

        // 5. Scheme inquiry / Available schemes
        if (lower.contains("scheme") || lower.contains("subsidy") || lower.contains("grant") || lower.contains("agri") || lower.contains("solar") || lower.contains("artisan") || lower.contains("available") || lower.contains("recommend")) {
            return handleSchemeInquiryQuery(lower, beneficiaryOpt);
        }

        // 6. Greetings & Help
        if (lower.contains("hi") || lower.contains("hello") || lower.contains("hey") || lower.contains("help") || lower.contains("what can you do") || lower.contains("who are you")) {
            return handleGeneralHelp(currentUser, beneficiaryOpt);
        }

        // Fallback with suggested queries
        return handleFallback(currentUser);
    }

    private AssistantResponse handleApplicationStatusQuery(String query, Optional<Beneficiary> beneficiaryOpt) {
        if (beneficiaryOpt.isEmpty()) {
            return new AssistantResponse(
                    "To check application tracking, please ensure you are logged in as a registered Beneficiary with existing applications.",
                    "APPLICATION_STATUS"
            );
        }

        Beneficiary b = beneficiaryOpt.get();
        List<SubsidyApplication> apps = applicationRepository.findByBeneficiaryId(b.getId());

        if (apps.isEmpty()) {
            return new AssistantResponse(
                    "You do not have any submitted subsidy applications yet. You can explore active schemes in the 'Available Schemes' catalog and submit an application today.",
                    "APPLICATION_STATUS"
            );
        }

        // Look for specific application reference in query
        for (SubsidyApplication app : apps) {
            if (query.contains(app.getApplicationNumber().toLowerCase())) {
                String ans = String.format(
                        "Application **%s** for scheme **%s** is currently at stage **%s** (Risk: %s). Applied amount: ₹%,.2f, Approved: ₹%,.2f.",
                        app.getApplicationNumber(),
                        app.getScheme().getTitle(),
                        app.getStatus(),
                        app.getRiskLevel(),
                        app.getAppliedAmount(),
                        app.getApprovedAmount() != null ? app.getApprovedAmount() : 0.0
                );
                return new AssistantResponse(ans, "APPLICATION_STATUS", buildAppRelatedData(List.of(app)));
            }
        }

        // Return summary of all active user applications
        StringBuilder sb = new StringBuilder("Here is the latest status of your active subsidy application(s):\n\n");
        for (SubsidyApplication app : apps) {
            sb.append(String.format("• **%s** (%s): Status is **%s** (Risk Level: %s). Applied on %s.\n",
                    app.getApplicationNumber(),
                    app.getScheme().getTitle(),
                    app.getStatus(),
                    app.getRiskLevel(),
                    app.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
            ));
        }
        sb.append("\nYou can click on any application in 'My Applications' for the complete timeline and verification history.");

        return new AssistantResponse(sb.toString(), "APPLICATION_STATUS", buildAppRelatedData(apps));
    }

    private AssistantResponse handleEligibilityQuery(String query, Optional<Beneficiary> beneficiaryOpt) {
        List<Scheme> schemes = schemeRepository.findByActiveTrue();
        if (schemes.isEmpty()) {
            return new AssistantResponse("There are currently no active welfare schemes in the portal.", "ELIGIBILITY");
        }

        if (beneficiaryOpt.isPresent()) {
            Beneficiary b = beneficiaryOpt.get();
            List<SchemeRecommendationResponse> recommendations = recommendationService.getRecommendationsForBeneficiary(b.getUser().getId());

            StringBuilder sb = new StringBuilder();
            sb.append("Based on your current profile (Income: ₹").append(b.getAnnualIncome())
              .append(", Land: ").append(b.getLandHoldingHectares()).append(" ha, Category: ").append(b.getCategory()).append("):\n\n");

            for (SchemeRecommendationResponse rec : recommendations) {
                String badge = "STRONG".equals(rec.getMatchLevel()) ? "🟢 Likely Match (Strong)" : ("MODERATE".equals(rec.getMatchLevel()) ? "🟡 Moderate Match" : "⚪ Low Match");
                sb.append(String.format("• **%s** [%s]: Score **%d/%d** — %s (Grant: ₹%,.0f - ₹%,.0f)\n",
                        rec.getTitle(),
                        rec.getCode(),
                        rec.getMatchScore(),
                        rec.getMinEligibilityScore(),
                        badge,
                        rec.getMinGrantAmount(),
                        rec.getMaxGrantAmount()
                ));
            }
            sb.append("\nOur rule engine performs an automated Eligibility Preview matching your income, category, and land holding against scheme criteria. Final eligibility is subject to official verification and document scrutiny.");
            return new AssistantResponse(sb.toString(), "ELIGIBILITY");
        }

        StringBuilder sb = new StringBuilder("General Eligibility Criteria for active schemes:\n\n");
        for (Scheme s : schemes) {
            sb.append(String.format("• **%s** (%s): Minimum qualifying score is %d points. Grant range ₹%,.0f to ₹%,.0f.\n",
                    s.getTitle(), s.getCode(), s.getMinEligibilityScore(), s.getMinGrantAmount(), s.getMaxGrantAmount()));
        }
        sb.append("\nPlease log in to view your personalized criteria matching breakdown.");
        return new AssistantResponse(sb.toString(), "ELIGIBILITY");
    }

    private AssistantResponse handleMilestoneDisbursementQuery(Optional<Beneficiary> beneficiaryOpt) {
        if (beneficiaryOpt.isPresent()) {
            Beneficiary b = beneficiaryOpt.get();
            List<SubsidyApplication> apps = applicationRepository.findByBeneficiaryId(b.getId());
            boolean hasDisbursed = apps.stream().anyMatch(a -> a.getDisbursementPlan() != null);

            if (hasDisbursed) {
                StringBuilder sb = new StringBuilder("Disbursement and Milestone status for your schemes:\n\n");
                for (SubsidyApplication a : apps) {
                    if (a.getDisbursementPlan() != null) {
                        DisbursementPlan p = a.getDisbursementPlan();
                        sb.append(String.format("• Scheme **%s** (%s):\n", a.getScheme().getTitle(), a.getApplicationNumber()));
                        sb.append(String.format("  - Total Planned: ₹%,.2f | Released: ₹%,.2f\n", p.getTotalPlannedAmount(), p.getTotalReleasedAmount()));
                        sb.append(String.format("  - Disbursement Status: **%s**\n", p.getStatus()));
                        if (p.getMilestones() != null && !p.getMilestones().isEmpty()) {
                            sb.append("  - Milestones: ");
                            String ms = p.getMilestones().stream()
                                    .map(m -> String.format("Milestone %d (₹%,.0f, %s)", m.getSequenceNumber(), m.getScheduledAmount(), m.getReleaseStatus()))
                                    .collect(Collectors.joining(", "));
                            sb.append(ms).append("\n");
                        }
                    }
                }
                sb.append("\nFunds are disbursed directly via Direct Benefit Transfer (DBT) to your verified bank account.");
                return new AssistantResponse(sb.toString(), "DISBURSEMENT_MILESTONE");
            }
        }

        return new AssistantResponse(
                "Government Direct Benefit Transfer (DBT) operates in phased tranches linked to verifiable physical milestones.\n\n" +
                "1. **Tranche 1 (Advance)**: Released upon Financial Sanction approval.\n" +
                "2. **Intermediate Tranches**: Released following Ground Field Verification and photo/document inspection.\n" +
                "3. **Final Tranche**: Credited after 100% utilization certificate submission and audit clearance.\n\n" +
                "All payments are routed through RBI e-Kuber / Treasury Gateway with unique UTR tracking.",
                "DISBURSEMENT_MILESTONE"
        );
    }

    private AssistantResponse handleDocumentQuery(String query) {
        return new AssistantResponse(
                "Standard mandatory documentation required for Government Subsidy applications:\n\n" +
                "1. **Identity & KYC Proof**: Aadhaar Card or Voter ID (DigiLocker verified or scanned PDF).\n" +
                "2. **Bank Account Details**: Passbook copy or cancelled cheque showing valid IFSC and account number.\n" +
                "3. **Income Certificate**: Issued by Revenue Authority / Tehsildar (valid within the financial year).\n" +
                "4. **Land / Asset Documentation**: 7/12 extract, Patta/Khata certificate, or tenant lease agreement (for agriculture).\n" +
                "5. **Category Certificate**: SC/ST/OBC/EWS certificate if claiming affirmative priority points.\n\n" +
                "Files must be in PDF, JPG, or PNG format under 10MB per document.",
                "DOCUMENTATION"
        );
    }

    private AssistantResponse handleSchemeInquiryQuery(String query, Optional<Beneficiary> beneficiaryOpt) {
        List<Scheme> schemes = schemeRepository.findByActiveTrue();
        if (schemes.isEmpty()) {
            return new AssistantResponse("There are currently no active government subsidy schemes available.", "SCHEME_INQUIRY");
        }

        // Check if query is targeting a specific scheme
        for (Scheme s : schemes) {
            if (query.contains(s.getCode().toLowerCase()) ||
                query.contains(s.getTitle().toLowerCase()) ||
                (s.getTitle().toLowerCase().contains("krishi") && (query.contains("krishi") || query.contains("agri") || query.contains("farm"))) ||
                (s.getTitle().toLowerCase().contains("solar") && (query.contains("solar") || query.contains("energy") || query.contains("green"))) ||
                (s.getTitle().toLowerCase().contains("artisan") && (query.contains("artisan") || query.contains("handicraft") || query.contains("craft")))) {

                String ans = String.format(
                        "### %s (%s)\n\n" +
                        "**Department**: %s\n\n" +
                        "**Description**: %s\n\n" +
                        "**Grant Slab**: ₹%,.2f to ₹%,.2f\n\n" +
                        "**Minimum Eligibility Score**: %d / 100 points\n\n" +
                        "**Remaining Department Budget**: ₹%,.2f\n\n" +
                        "You can preview your exact eligibility and submit your application from the Available Schemes page.",
                        s.getTitle(), s.getCode(), s.getDepartment(), s.getDescription(),
                        s.getMinGrantAmount(), s.getMaxGrantAmount(), s.getMinEligibilityScore(),
                        s.getRemainingBudget()
                );
                return new AssistantResponse(ans, "SCHEME_INQUIRY", buildSchemeRelatedData(List.of(s)));
            }
        }

        // If beneficiary is logged in, provide personalized recommendations
        if (beneficiaryOpt.isPresent()) {
            List<SchemeRecommendationResponse> recs = recommendationService.getRecommendationsForBeneficiary(beneficiaryOpt.get().getUser().getId());
            StringBuilder sb = new StringBuilder("Here are the active schemes available for you, ordered by profile match score:\n\n");
            for (SchemeRecommendationResponse rec : recs) {
                sb.append(String.format("• **%s** (%s) — Match Score: **%d/100** (%s)\n  Department: %s | Grant: ₹%,.0f - ₹%,.0f\n\n",
                        rec.getTitle(), rec.getCode(), rec.getMatchScore(), rec.getMatchLevel(), rec.getDepartment(),
                        rec.getMinGrantAmount(), rec.getMaxGrantAmount()));
            }
            sb.append("Select any scheme on your dashboard or Schemes catalog to view criterion breakdown.");
            return new AssistantResponse(sb.toString(), "SCHEME_INQUIRY", buildSchemeRelatedData(schemes));
        }

        // Generic catalog
        StringBuilder sb = new StringBuilder("Active Government Welfare Schemes available in the portal:\n\n");
        for (Scheme s : schemes) {
            sb.append(String.format("• **%s** (%s): Grant ₹%,.0f to ₹%,.0f (%s)\n",
                    s.getTitle(), s.getCode(), s.getMinGrantAmount(), s.getMaxGrantAmount(), s.getDepartment()));
        }
        sb.append("\nAsk me about any specific scheme to learn about criteria, documents, or grant slabs.");
        return new AssistantResponse(sb.toString(), "SCHEME_INQUIRY", buildSchemeRelatedData(schemes));
    }

    private AssistantResponse handleGeneralHelp(User currentUser, Optional<Beneficiary> beneficiaryOpt) {
        String greeting = (currentUser != null) ? "Hello " + currentUser.getFullName() + "! " : "Hello! ";
        String ans = greeting + "I am your Government Subsidy & Grant Information Assistant, providing real-time database tracking and rule-based criteria evaluations.\n\n" +
                "Here are some questions you can ask me:\n" +
                "1. **\"What schemes are available?\"** — View all active subsidy schemes\n" +
                "2. **\"What is the status of my application?\"** — Real-time tracking of submitted applications\n" +
                "3. **\"What are the eligibility requirements?\"** — Criteria evaluation & score requirements\n" +
                "4. **\"What documents do I need to submit?\"** — Mandatory checklist for uploads\n" +
                "5. **\"How do milestone disbursements work?\"** — DBT payment schedule & inspection rules\n" +
                "6. **\"Tell me about Pradhan Mantri Krishi scheme\"** — Detailed scheme information\n\n" +
                "How may I assist you today?";
        return new AssistantResponse(ans, "GENERAL_HELP");
    }

    private AssistantResponse handleFallback(User currentUser) {
        return new AssistantResponse(
                "I couldn't find a direct answer for your inquiry. Here are common topics you can ask me about:\n\n" +
                "• **Available Schemes**: \"Which schemes can I apply for?\"\n" +
                "• **Application Tracking**: \"Check status of my application\"\n" +
                "• **Eligibility**: \"Am I eligible for agriculture subsidies?\"\n" +
                "• **Documents**: \"What documents are needed for verification?\"\n" +
                "• **Disbursements**: \"When will my milestone payment be released?\"",
                "GENERAL_HELP"
        );
    }

    private List<Map<String, Object>> buildAppRelatedData(List<SubsidyApplication> apps) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SubsidyApplication a : apps) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("applicationId", a.getId());
            map.put("applicationNumber", a.getApplicationNumber());
            map.put("schemeTitle", a.getScheme().getTitle());
            map.put("status", a.getStatus().name());
            map.put("appliedAmount", a.getAppliedAmount());
            map.put("riskLevel", a.getRiskLevel().name());
            list.add(map);
        }
        return list;
    }

    private List<Map<String, Object>> buildSchemeRelatedData(List<Scheme> schemes) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Scheme s : schemes) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("code", s.getCode());
            map.put("title", s.getTitle());
            map.put("department", s.getDepartment());
            map.put("minGrant", s.getMinGrantAmount());
            map.put("maxGrant", s.getMaxGrantAmount());
            map.put("minScore", s.getMinEligibilityScore());
            list.add(map);
        }
        return list;
    }
}
