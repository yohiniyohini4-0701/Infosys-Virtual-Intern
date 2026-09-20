package com.example.governmentsubsidy.config;

import com.example.governmentsubsidy.entity.*;
import com.example.governmentsubsidy.enums.*;
import com.example.governmentsubsidy.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;
    private final SubsidyApplicationRepository applicationRepository;
    private final DisbursementPlanRepository planRepository;
    private final VerificationRepository verificationRepository;
    private final FundReleaseRepository fundReleaseRepository;
    private final FundUtilizationRepository utilizationRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           RegionRepository regionRepository,
                           BeneficiaryRepository beneficiaryRepository,
                           SchemeRepository schemeRepository,
                           SubsidyApplicationRepository applicationRepository,
                           DisbursementPlanRepository planRepository,
                           VerificationRepository verificationRepository,
                           FundReleaseRepository fundReleaseRepository,
                           FundUtilizationRepository utilizationRepository,
                           NotificationRepository notificationRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.applicationRepository = applicationRepository;
        this.planRepository = planRepository;
        this.verificationRepository = verificationRepository;
        this.fundReleaseRepository = fundReleaseRepository;
        this.utilizationRepository = utilizationRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            log.info("Database already initialized with seed data.");
            userRepository.findByUsername("admin").ifPresent(adminUser -> {
                adminUser.setPassword(passwordEncoder.encode("admin123"));
                userRepository.save(adminUser);
                log.info("Updated admin user credentials to admin/admin123 successfully.");
            });

            // Ensure submitted queue demo app exists if DB was seeded previously
            if (!applicationRepository.existsByApplicationNumber("SUB-2026-DL007")) {
                List<Beneficiary> bens = beneficiaryRepository.findAll();
                List<Scheme> schemes = schemeRepository.findByActiveTrue();
                List<Region> regions = regionRepository.findAll();
                if (!bens.isEmpty() && !schemes.isEmpty() && !regions.isEmpty()) {
                    Beneficiary b = bens.get(bens.size() - 1);
                    Scheme s = schemes.get(0);
                    Region r = regions.get(0);
                    SubsidyApplication app7 = new SubsidyApplication();
                    app7.setApplicationNumber("SUB-2026-DL007");
                    app7.setBeneficiary(b);
                    app7.setScheme(s);
                    app7.setRegion(r);
                    app7.setAppliedAmount(new BigDecimal("90000.00"));
                    app7.setApprovedAmount(BigDecimal.ZERO);
                    app7.setStatus(ApplicationStatus.SUBMITTED);
                    app7.setRiskLevel(RiskLevel.LOW);
                    app7.setEligibilityScore(0);
                    app7.addDocument(new ApplicationDocument(app7, DocumentType.IDENTITY_PROOF, "aadhaar_sunita.pdf", "application/pdf", "uploads/documents/demo_aadhaar.pdf"));
                    app7.addDocument(new ApplicationDocument(app7, DocumentType.INCOME_CERTIFICATE, "income_sunita.pdf", "application/pdf", "uploads/documents/demo_income.pdf"));
                    applicationRepository.save(app7);
                    log.info("Seeded pending submitted application SUB-2026-DL007 for existing DB.");
                }
            }
            if (!applicationRepository.existsByApplicationNumber("SUB-2026-DL006")) {
                List<Beneficiary> bens = beneficiaryRepository.findAll();
                List<Scheme> schemes = schemeRepository.findByActiveTrue();
                List<Region> regions = regionRepository.findAll();
                if (!bens.isEmpty() && !schemes.isEmpty() && !regions.isEmpty()) {
                    Beneficiary b = bens.get(bens.size() - 1);
                    Scheme s = schemes.get(schemes.size() - 1);
                    Region r = regions.get(0);
                    SubsidyApplication app6 = new SubsidyApplication();
                    app6.setApplicationNumber("SUB-2026-DL006");
                    app6.setBeneficiary(b);
                    app6.setScheme(s);
                    app6.setRegion(r);
                    app6.setAppliedAmount(new BigDecimal("200000.00"));
                    app6.setApprovedAmount(BigDecimal.ZERO);
                    app6.setStatus(ApplicationStatus.REJECTED);
                    app6.setRiskLevel(RiskLevel.FLAGGED);
                    app6.setEligibilityScore(40);
                    app6.setRejectionReason("Eligibility Criteria Unmet: Applicant age verification failed / insufficient documentation");
                    applicationRepository.save(app6);
                    log.info("Seeded rejected application SUB-2026-DL006 for existing DB.");
                }
            }
            return;
        }

        log.info("Initializing Government Subsidy System seed data...");

        // 1. Roles
        Role rBeneficiary = roleRepository.save(new Role(RoleType.ROLE_BENEFICIARY));
        Role rFieldOfficer = roleRepository.save(new Role(RoleType.ROLE_FIELD_OFFICER));
        Role rDistrictOfficer = roleRepository.save(new Role(RoleType.ROLE_DISTRICT_OFFICER));
        Role rFinanceOfficer = roleRepository.save(new Role(RoleType.ROLE_FINANCE_OFFICER));
        Role rAdmin = roleRepository.save(new Role(RoleType.ROLE_ADMIN));

        // 2. Users (all mock demo accounts)
        User admin = createUser("admin", "admin123", "Rajesh Sharma (Admin)", "admin@gov.in", "+91 9876543210", Set.of(rAdmin));
        User fieldOfficer = createUser("field_officer1", "Officer@123", "Amit Kumar (Field Officer)", "amit.field@gov.in", "+91 9876543211", Set.of(rFieldOfficer));
        User districtOfficer = createUser("district_officer1", "District@123", "Dr. Neha Verma (District Magistrate)", "neha.district@gov.in", "+91 9876543212", Set.of(rDistrictOfficer));
        User financeOfficer = createUser("finance_officer1", "Finance@123", "Sanjay Gupta (Finance Officer)", "sanjay.finance@gov.in", "+91 9876543213", Set.of(rFinanceOfficer));
        User beneficiaryUser1 = createUser("farmer_john", "User@123", "Rameshwar Rao (Farmer)", "ramesh.farmer@email.com", "+91 9876543214", Set.of(rBeneficiary));
        User beneficiaryUser2 = createUser("artisan_priya", "User@123", "Priya Patel (Handicrafts)", "priya.artisan@email.com", "+91 9876543215", Set.of(rBeneficiary));
        User beneficiaryUser3 = createUser("sunita_devi", "User@123", "Sunita Devi (Self-Help Group)", "sunita.dairy@email.com", "+91 9876543216", Set.of(rBeneficiary));

        // 3. Regions
        Region rDelhi = regionRepository.save(new Region("REG-DL-01", "Delhi", "New Delhi", "Chanakyapuri", new BigDecimal("50000000.00")));
        Region rPune = regionRepository.save(new Region("REG-MH-01", "Maharashtra", "Pune", "Haveli", new BigDecimal("100000000.00")));
        Region rBangalore = regionRepository.save(new Region("REG-KA-01", "Karnataka", "Bengaluru Rural", "Devanahalli", new BigDecimal("80000000.00")));

        // 4. Beneficiaries
        Beneficiary b1 = new Beneficiary();
        b1.setUser(beneficiaryUser1);
        b1.setRegion(rPune);
        b1.setIdentityNumber("AADHAAR-8839-2091-1123");
        b1.setCategory(BeneficiaryCategory.OBC);
        b1.setDateOfBirth(LocalDate.of(1985, 4, 15));
        b1.setAnnualIncome(new BigDecimal("150000.00"));
        b1.setLandHoldingHectares(new BigDecimal("1.80"));
        b1.setDisabled(false);
        b1.setBankAccountNumber("309811223344");
        b1.setBankIfscCode("SBIN0001234");
        b1.setBankName("State Bank of India");
        b1.setAddressLine("Village Wadgaon, Taluk Haveli, District Pune");
        b1.setKycStatus(KycStatus.VERIFIED);
        b1 = beneficiaryRepository.save(b1);

        Beneficiary b2 = new Beneficiary();
        b2.setUser(beneficiaryUser2);
        b2.setRegion(rBangalore);
        b2.setIdentityNumber("AADHAAR-5512-3902-8844");
        b2.setCategory(BeneficiaryCategory.EWS);
        b2.setDateOfBirth(LocalDate.of(1992, 8, 20));
        b2.setAnnualIncome(new BigDecimal("95000.00"));
        b2.setLandHoldingHectares(new BigDecimal("0.00"));
        b2.setDisabled(false);
        b2.setBankAccountNumber("601244556677");
        b2.setBankIfscCode("PUNB0123400");
        b2.setBankName("Punjab National Bank");
        b2.setAddressLine("Handloom Colony, Devanahalli, Bengaluru Rural");
        b2.setKycStatus(KycStatus.VERIFIED);
        b2 = beneficiaryRepository.save(b2);

        Beneficiary b3 = new Beneficiary();
        b3.setUser(beneficiaryUser3);
        b3.setRegion(rDelhi);
        b3.setIdentityNumber("AADHAAR-9901-4412-7721");
        b3.setCategory(BeneficiaryCategory.SC);
        b3.setDateOfBirth(LocalDate.of(1988, 11, 10));
        b3.setAnnualIncome(new BigDecimal("120000.00"));
        b3.setLandHoldingHectares(new BigDecimal("0.50"));
        b3.setDisabled(false);
        b3.setBankAccountNumber("901233445566");
        b3.setBankIfscCode("BARB0DELHI0");
        b3.setBankName("Bank of Baroda");
        b3.setAddressLine("Bawana Rural Cluster, North West Delhi");
        b3.setKycStatus(KycStatus.PENDING); // Realistic pending lifecycle example
        b3 = beneficiaryRepository.save(b3);

        // 5. Schemes
        Scheme sAgri = new Scheme();
        sAgri.setCode("SCH-AGRI-01");
        sAgri.setTitle("Pradhan Mantri Krishi Vikas Subsidy");
        sAgri.setDescription("Direct financial support and equipment assistance for small, marginal, and tenant farmers.");
        sAgri.setDepartment("Department of Agriculture & Farmers Welfare");
        sAgri.setTotalBudget(new BigDecimal("50000000.00"));
        sAgri.setRemainingBudget(new BigDecimal("49750000.00"));
        sAgri.setMinGrantAmount(new BigDecimal("20000.00"));
        sAgri.setMaxGrantAmount(new BigDecimal("150000.00"));
        sAgri.setMinEligibilityScore(60);
        sAgri.setActive(true);
        sAgri.addCriterion(new EligibilityCriterion(sAgri, CriterionType.ANNUAL_INCOME, ComparisonOperator.LESS_THAN_OR_EQUAL, "300000", 35, true, "Annual household income must be <= 300,000 INR"));
        sAgri.addCriterion(new EligibilityCriterion(sAgri, CriterionType.LAND_HOLDING, ComparisonOperator.LESS_THAN_OR_EQUAL, "2.5", 35, true, "Land holding must not exceed 2.5 hectares"));
        sAgri.addCriterion(new EligibilityCriterion(sAgri, CriterionType.BENEFICIARY_CATEGORY, ComparisonOperator.IN, "OBC,SC,ST,EWS,GENERAL", 30, false, "Affirmative social category priority score"));
        schemeRepository.save(sAgri);

        Scheme sSolar = new Scheme();
        sSolar.setCode("SCH-SOLAR-02");
        sSolar.setTitle("National Rural Solar & Green Energy Grant");
        sSolar.setDescription("Capital subsidy for installing solar agricultural pumps, rooftop panels, and farm microgrids.");
        sSolar.setDepartment("Ministry of New and Renewable Energy");
        sSolar.setTotalBudget(new BigDecimal("100000000.00"));
        sSolar.setRemainingBudget(new BigDecimal("99800000.00"));
        sSolar.setMinGrantAmount(new BigDecimal("50000.00"));
        sSolar.setMaxGrantAmount(new BigDecimal("250000.00"));
        sSolar.setMinEligibilityScore(50);
        sSolar.setActive(true);
        sSolar.addCriterion(new EligibilityCriterion(sSolar, CriterionType.ANNUAL_INCOME, ComparisonOperator.LESS_THAN_OR_EQUAL, "400000", 50, true, "Annual income <= 400,000 INR"));
        sSolar.addCriterion(new EligibilityCriterion(sSolar, CriterionType.AGE, ComparisonOperator.GREATER_THAN_OR_EQUAL, "21", 50, true, "Applicant must be at least 21 years of age"));
        schemeRepository.save(sSolar);

        Scheme sArtisan = new Scheme();
        sArtisan.setCode("SCH-ARTISAN-03");
        sArtisan.setTitle("PM Vishwakarma Artisan Toolkit & Modernization Scheme");
        sArtisan.setDescription("Financial assistance, advanced toolkits, and collateral-free enterprise development for traditional artisans.");
        sArtisan.setDepartment("Ministry of Micro, Small & Medium Enterprises");
        sArtisan.setTotalBudget(new BigDecimal("60000000.00"));
        sArtisan.setRemainingBudget(new BigDecimal("59900000.00"));
        sArtisan.setMinGrantAmount(new BigDecimal("25000.00"));
        sArtisan.setMaxGrantAmount(new BigDecimal("200000.00"));
        sArtisan.setMinEligibilityScore(50);
        sArtisan.setActive(true);
        sArtisan.addCriterion(new EligibilityCriterion(sArtisan, CriterionType.ANNUAL_INCOME, ComparisonOperator.LESS_THAN_OR_EQUAL, "250000", 50, true, "Household income <= 250,000 INR"));
        sArtisan.addCriterion(new EligibilityCriterion(sArtisan, CriterionType.BENEFICIARY_CATEGORY, ComparisonOperator.IN, "OBC,SC,ST,EWS", 50, false, "Artisan guild verification priority"));
        schemeRepository.save(sArtisan);

        // 6. Applications across workflow states

        // App 1: Field Verification Queue (Farmer John - Agri)
        SubsidyApplication app1 = new SubsidyApplication();
        app1.setApplicationNumber("SUB-2026-MH001");
        app1.setBeneficiary(b1);
        app1.setScheme(sAgri);
        app1.setRegion(rPune);
        app1.setAppliedAmount(new BigDecimal("100000.00"));
        app1.setApprovedAmount(BigDecimal.ZERO);
        app1.setStatus(ApplicationStatus.FIELD_VERIFICATION);
        app1.setRiskLevel(RiskLevel.LOW);
        app1.setEligibilityScore(100);
        app1.addDocument(new ApplicationDocument(app1, DocumentType.IDENTITY_PROOF, "aadhaar_card.pdf", "application/pdf", "uploads/documents/demo_aadhaar.pdf"));
        app1.addDocument(new ApplicationDocument(app1, DocumentType.INCOME_CERTIFICATE, "income_cert.pdf", "application/pdf", "uploads/documents/demo_income.pdf"));
        app1.addDocument(new ApplicationDocument(app1, DocumentType.LAND_RECORD, "land_712_extract.pdf", "application/pdf", "uploads/documents/demo_land.pdf"));
        applicationRepository.save(app1);

        // App 2: District Review Queue (Artisan Priya - Artisan Scheme)
        SubsidyApplication app2 = new SubsidyApplication();
        app2.setApplicationNumber("SUB-2026-KA002");
        app2.setBeneficiary(b2);
        app2.setScheme(sArtisan);
        app2.setRegion(rBangalore);
        app2.setAppliedAmount(new BigDecimal("75000.00"));
        app2.setApprovedAmount(BigDecimal.ZERO);
        app2.setStatus(ApplicationStatus.DISTRICT_REVIEW);
        app2.setRiskLevel(RiskLevel.LOW);
        app2.setEligibilityScore(100);
        app2.addDocument(new ApplicationDocument(app2, DocumentType.IDENTITY_PROOF, "aadhaar_priya.pdf", "application/pdf", "uploads/documents/demo_aadhaar.pdf"));
        app2.addDocument(new ApplicationDocument(app2, DocumentType.INCOME_CERTIFICATE, "income_priya.pdf", "application/pdf", "uploads/documents/demo_income.pdf"));
        app2 = applicationRepository.save(app2);

        verificationRepository.save(new Verification(
                app2, VerificationStage.FIELD_VERIFICATION, fieldOfficer,
                VerificationDecision.APPROVED, "Site inspection completed. Workshop tools and machinery verified in person."
        ));

        // App 3: Finance Sanction Queue (Farmer John - Solar)
        SubsidyApplication app3 = new SubsidyApplication();
        app3.setApplicationNumber("SUB-2026-MH003");
        app3.setBeneficiary(b1);
        app3.setScheme(sSolar);
        app3.setRegion(rPune);
        app3.setAppliedAmount(new BigDecimal("150000.00"));
        app3.setApprovedAmount(BigDecimal.ZERO);
        app3.setStatus(ApplicationStatus.FINANCE_APPROVAL);
        app3.setRiskLevel(RiskLevel.LOW);
        app3.setEligibilityScore(100);
        app3.addDocument(new ApplicationDocument(app3, DocumentType.IDENTITY_PROOF, "aadhaar_card.pdf", "application/pdf", "uploads/documents/demo_aadhaar.pdf"));
        app3 = applicationRepository.save(app3);

        verificationRepository.save(new Verification(
                app3, VerificationStage.FIELD_VERIFICATION, fieldOfficer,
                VerificationDecision.APPROVED, "Agricultural land suitability for solar grid confirmed."
        ));
        verificationRepository.save(new Verification(
                app3, VerificationStage.DISTRICT_REVIEW, districtOfficer,
                VerificationDecision.APPROVED, "District quota approved by District Collectorate."
        ));

        // App 4: Utilization Monitoring Pending (Artisan Priya - Solar)
        SubsidyApplication app4 = new SubsidyApplication();
        app4.setApplicationNumber("SUB-2026-KA004");
        app4.setBeneficiary(b2);
        app4.setScheme(sSolar);
        app4.setRegion(rBangalore);
        app4.setAppliedAmount(new BigDecimal("100000.00"));
        app4.setApprovedAmount(new BigDecimal("100000.00"));
        app4.setStatus(ApplicationStatus.UTILIZATION_PENDING);
        app4.setRiskLevel(RiskLevel.LOW);
        app4.setEligibilityScore(100);
        app4 = applicationRepository.save(app4);

        DisbursementPlan plan4 = new DisbursementPlan(app4, new BigDecimal("100000.00"));
        DisbursementMilestone m1 = new DisbursementMilestone(plan4, 1, "Mobilization & Inverter Procurement", MilestoneType.DOCUMENTATION, new BigDecimal("50000.00"), LocalDate.now().minusDays(15), "Vendor quotation approval");
        m1.setComplianceSatisfied(true);
        m1.setReleaseStatus(MilestoneReleaseStatus.RELEASED);
        m1.setCompletedAt(LocalDateTime.now().minusDays(10));

        DisbursementMilestone m2 = new DisbursementMilestone(plan4, 2, "Installation & Commissioning", MilestoneType.FINAL_COMPLETION, new BigDecimal("50000.00"), LocalDate.now().plusDays(20), "Grid interconnection certificate");
        plan4.addMilestone(m1);
        plan4.addMilestone(m2);
        plan4.setTotalReleasedAmount(new BigDecimal("50000.00"));
        plan4.setTotalRemainingAmount(new BigDecimal("50000.00"));
        plan4.setStatus("ACTIVE");
        plan4 = planRepository.save(plan4);

        FundRelease release4 = new FundRelease(m1, "UTR202609010049281", new BigDecimal("50000.00"), financeOfficer, "DIRECT_BENEFIT_TRANSFER", "SUCCESS");
        release4 = fundReleaseRepository.save(release4);
        m1.setFundRelease(release4);

        app4.setDisbursementPlan(plan4);
        app4 = applicationRepository.save(app4);

        // Submitted utilization voucher awaiting verification
        FundUtilization util4 = new FundUtilization(app4, new BigDecimal("50000.00"), "uploads/documents/solar_invoice_50k.pdf", "Inverter and solar panels purchased from authorized supplier");
        utilizationRepository.save(util4);

        // App 5: Completed Application (Farmer John - Agri)
        SubsidyApplication app5 = new SubsidyApplication();
        app5.setApplicationNumber("SUB-2026-MH005");
        app5.setBeneficiary(b1);
        app5.setScheme(sAgri);
        app5.setRegion(rPune);
        app5.setAppliedAmount(new BigDecimal("80000.00"));
        app5.setApprovedAmount(new BigDecimal("80000.00"));
        app5.setStatus(ApplicationStatus.COMPLETED);
        app5.setRiskLevel(RiskLevel.LOW);
        app5.setEligibilityScore(100);
        app5 = applicationRepository.save(app5);

        DisbursementPlan plan5 = new DisbursementPlan(app5, new BigDecimal("80000.00"));
        DisbursementMilestone mFull = new DisbursementMilestone(plan5, 1, "Full Sanction", MilestoneType.FINAL_COMPLETION, new BigDecimal("80000.00"), LocalDate.now().minusDays(30), "Inspection certificate");
        mFull.setComplianceSatisfied(true);
        mFull.setReleaseStatus(MilestoneReleaseStatus.RELEASED);
        plan5.addMilestone(mFull);
        plan5.setTotalReleasedAmount(new BigDecimal("80000.00"));
        plan5.setTotalRemainingAmount(BigDecimal.ZERO);
        plan5.setStatus("COMPLETED");
        plan5 = planRepository.save(plan5);

        FundRelease release5 = new FundRelease(mFull, "UTR202608159981240", new BigDecimal("80000.00"), financeOfficer, "DIRECT_BENEFIT_TRANSFER", "SUCCESS");
        fundReleaseRepository.save(release5);
        mFull.setFundRelease(release5);

        app5.setDisbursementPlan(plan5);
        app5 = applicationRepository.save(app5);

        FundUtilization util5 = new FundUtilization(app5, new BigDecimal("80000.00"), "uploads/documents/tractor_attachment_bill.pdf", "Purchased precision seeder equipment");
        util5.setVerificationStatus(UtilizationVerificationStatus.VERIFIED);
        util5.setVerifiedByUser(fieldOfficer);
        utilizationRepository.save(util5);

        // App 6: Rejected Example
        SubsidyApplication app6 = new SubsidyApplication();
        app6.setApplicationNumber("SUB-2026-DL006");
        app6.setBeneficiary(b3);
        app6.setScheme(sSolar);
        app6.setRegion(rDelhi);
        app6.setAppliedAmount(new BigDecimal("200000.00"));
        app6.setApprovedAmount(BigDecimal.ZERO);
        app6.setStatus(ApplicationStatus.REJECTED);
        app6.setRiskLevel(RiskLevel.FLAGGED);
        app6.setEligibilityScore(40);
        app6.setRejectionReason("Eligibility Criteria Unmet: Applicant age verification failed / insufficient documentation");
        app6 = applicationRepository.save(app6);

        // App 7: Submitted Queue Example (Sunita Devi - Agri Scheme) - Ready for Eligibility Evaluation Queue
        SubsidyApplication app7 = new SubsidyApplication();
        app7.setApplicationNumber("SUB-2026-DL007");
        app7.setBeneficiary(b3);
        app7.setScheme(sAgri);
        app7.setRegion(rDelhi);
        app7.setAppliedAmount(new BigDecimal("90000.00"));
        app7.setApprovedAmount(BigDecimal.ZERO);
        app7.setStatus(ApplicationStatus.SUBMITTED);
        app7.setRiskLevel(RiskLevel.LOW);
        app7.setEligibilityScore(0);
        app7.addDocument(new ApplicationDocument(app7, DocumentType.IDENTITY_PROOF, "aadhaar_sunita.pdf", "application/pdf", "uploads/documents/demo_aadhaar.pdf"));
        app7.addDocument(new ApplicationDocument(app7, DocumentType.INCOME_CERTIFICATE, "income_sunita.pdf", "application/pdf", "uploads/documents/demo_income.pdf"));
        app7 = applicationRepository.save(app7);

        // Seed Notifications
        notificationRepository.save(new Notification(
                "farmer_john",
                "Application SUB-2026-MH001 Submitted",
                "Your application for Pradhan Mantri Krishi Vikas Subsidy was successfully registered in Maharashtra Region.",
                NotificationType.APPLICATION_STATUS_UPDATE,
                "SubsidyApplication",
                app1.getId(),
                "/beneficiary/applications/" + app1.getId()
        ));

        notificationRepository.save(new Notification(
                "farmer_john",
                "Field Inspection Completed",
                "Officer Amit Kumar conducted physical verification for application SUB-2026-MH001. Land records verified.",
                NotificationType.VERIFICATION_UPDATE,
                "SubsidyApplication",
                app1.getId(),
                "/beneficiary/applications/" + app1.getId()
        ));

        notificationRepository.save(new Notification(
                "artisan_priya",
                "DBT Grant Released: INR 50,000",
                "First DBT tranche of INR 50,000 credited to bank account for application SUB-2026-KA004. Treasury UTR: UTR202609010049281.",
                NotificationType.DISBURSEMENT_RELEASED,
                "SubsidyApplication",
                app4.getId(),
                "/beneficiary/applications/" + app4.getId()
        ));

        notificationRepository.save(new Notification(
                "field_officer1",
                "Pending Eligibility Evaluation",
                "Application SUB-2026-DL007 has been registered and is pending rule eligibility scoring in the queue.",
                NotificationType.APPLICATION_STATUS_UPDATE,
                "SubsidyApplication",
                app7.getId(),
                "/officer/eligibility-evaluation"
        ));

        notificationRepository.save(new Notification(
                "field_officer1",
                "Pending Field Inspections",
                "Application SUB-2026-MH001 has passed automated scoring and is awaiting physical ground verification.",
                NotificationType.APPLICATION_STATUS_UPDATE,
                "SubsidyApplication",
                app1.getId(),
                "/officer/field-verification"
        ));

        notificationRepository.save(new Notification(
                "admin",
                "System Notice: Compliance Audit Complete",
                "All regional jurisdictions reported zero unverified disbursements exceeding 60-day thresholds.",
                NotificationType.SYSTEM_NOTICE,
                "System",
                1L,
                "/admin/dashboard"
        ));

        log.info("Seed data initialization finished successfully!");
    }

    private User createUser(String username, String rawPassword, String fullName, String email, String phone, Set<Role> roles) {
        User u = new User(username, passwordEncoder.encode(rawPassword), fullName, email, phone);
        u.setRoles(new HashSet<>(roles));
        return userRepository.save(u);
    }
}
