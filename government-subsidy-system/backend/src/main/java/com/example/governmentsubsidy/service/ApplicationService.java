package com.example.governmentsubsidy.service;

import com.example.governmentsubsidy.dto.application.ApplicationRequest;
import com.example.governmentsubsidy.dto.application.ApplicationResponse;
import com.example.governmentsubsidy.dto.application.DocumentDto;
import com.example.governmentsubsidy.dto.eligibility.EligibilityEvaluationResult;
import com.example.governmentsubsidy.entity.*;
import com.example.governmentsubsidy.enums.ApplicationStatus;
import com.example.governmentsubsidy.enums.DocumentType;
import com.example.governmentsubsidy.enums.RiskLevel;
import com.example.governmentsubsidy.exception.BadRequestException;
import com.example.governmentsubsidy.exception.InvalidStatusTransitionException;
import com.example.governmentsubsidy.exception.ResourceNotFoundException;
import com.example.governmentsubsidy.exception.UnauthorizedException;
import com.example.governmentsubsidy.enums.NotificationType;
import com.example.governmentsubsidy.repository.ApplicationDocumentRepository;
import com.example.governmentsubsidy.repository.BeneficiaryRepository;
import com.example.governmentsubsidy.repository.SubsidyApplicationRepository;
import com.example.governmentsubsidy.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final SubsidyApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final BeneficiaryService beneficiaryService;
    private final SchemeService schemeService;
    private final RegionService regionService;
    private final EligibilityService eligibilityService;
    private final AuditLogService auditLogService;
    private final DocumentStorageService documentStorageService;
    private final NotificationService notificationService;

    public ApplicationService(SubsidyApplicationRepository applicationRepository,
                              ApplicationDocumentRepository documentRepository,
                              BeneficiaryRepository beneficiaryRepository,
                              UserRepository userRepository,
                              BeneficiaryService beneficiaryService,
                              SchemeService schemeService,
                              RegionService regionService,
                              EligibilityService eligibilityService,
                              AuditLogService auditLogService,
                              DocumentStorageService documentStorageService,
                              NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.userRepository = userRepository;
        this.beneficiaryService = beneficiaryService;
        this.schemeService = schemeService;
        this.regionService = regionService;
        this.eligibilityService = eligibilityService;
        this.auditLogService = auditLogService;
        this.documentStorageService = documentStorageService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ApplicationResponse createApplication(ApplicationRequest request, User currentUser) {
        Beneficiary beneficiary;
        boolean isBeneficiaryRole = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_BENEFICIARY"));

        if (isBeneficiaryRole) {
            beneficiary = beneficiaryRepository.findByUser(currentUser)
                    .orElseThrow(() -> new BadRequestException("Beneficiary profile must be registered before creating an application"));
        } else if (request.getBeneficiaryId() != null) {
            beneficiary = beneficiaryService.getBeneficiaryEntity(request.getBeneficiaryId());
        } else {
            throw new BadRequestException("Beneficiary ID is required");
        }

        Scheme scheme = schemeService.getSchemeEntity(request.getSchemeId());
        Region region = regionService.getRegionById(request.getRegionId());

        if (!scheme.isActive()) {
            throw new BadRequestException("Scheme '" + scheme.getTitle() + "' is currently inactive");
        }

        if (request.getAppliedAmount().compareTo(scheme.getMinGrantAmount()) < 0 ||
                request.getAppliedAmount().compareTo(scheme.getMaxGrantAmount()) > 0) {
            throw new BadRequestException("Applied amount must be between " + scheme.getMinGrantAmount() + " and " + scheme.getMaxGrantAmount());
        }

        String applicationNumber = generateApplicationNumber();

        SubsidyApplication application = new SubsidyApplication();
        application.setApplicationNumber(applicationNumber);
        application.setBeneficiary(beneficiary);
        application.setScheme(scheme);
        application.setRegion(region);
        application.setAppliedAmount(request.getAppliedAmount());
        application.setStatus(ApplicationStatus.DRAFT);
        application.setRiskLevel(RiskLevel.LOW);

        SubsidyApplication saved = applicationRepository.save(application);

        auditLogService.logAction(
                currentUser.getUsername(),
                "APPLICATION_CREATED",
                "SubsidyApplication",
                saved.getId().toString(),
                null,
                ApplicationStatus.DRAFT.name(),
                "Application created for scheme: " + scheme.getCode() + " with amount: " + request.getAppliedAmount()
        );

        return mapToResponse(saved);
    }

    @Transactional
    public ApplicationResponse createApplication(ApplicationRequest request, String actingUsername) {
        User user = userRepository.findByUsername(actingUsername).orElse(null);
        if (user != null) {
            return createApplication(request, user);
        }
        Beneficiary beneficiary = beneficiaryService.getBeneficiaryEntity(request.getBeneficiaryId());
        Scheme scheme = schemeService.getSchemeEntity(request.getSchemeId());
        Region region = regionService.getRegionById(request.getRegionId());

        String applicationNumber = generateApplicationNumber();
        SubsidyApplication application = new SubsidyApplication();
        application.setApplicationNumber(applicationNumber);
        application.setBeneficiary(beneficiary);
        application.setScheme(scheme);
        application.setRegion(region);
        application.setAppliedAmount(request.getAppliedAmount());
        application.setStatus(ApplicationStatus.DRAFT);
        application.setRiskLevel(RiskLevel.LOW);

        SubsidyApplication saved = applicationRepository.save(application);
        return mapToResponse(saved);
    }

    @Transactional
    public DocumentDto attachDocument(Long applicationId, DocumentType documentType,
                                     String fileName, String fileType, String filePath, String actingUser) {
        SubsidyApplication application = getApplicationEntity(applicationId);

        ApplicationDocument doc = new ApplicationDocument(application, documentType, fileName, fileType, filePath);
        ApplicationDocument saved = documentRepository.save(doc);

        auditLogService.logAction(
                actingUser,
                "DOCUMENT_ATTACHED",
                "ApplicationDocument",
                saved.getId().toString(),
                null,
                "ATTACHED",
                "Document " + documentType + " (" + fileName + ") attached to application " + application.getApplicationNumber()
        );

        return new DocumentDto(
                saved.getId(),
                saved.getDocumentType(),
                saved.getFileName(),
                saved.getFileType(),
                saved.getFilePath(),
                saved.getVerificationStatus(),
                saved.getUploadedAt()
        );
    }

    @Transactional
    public DocumentDto attachDocumentFile(Long applicationId, DocumentType documentType,
                                         MultipartFile file, User actingUser) {
        SubsidyApplication application = getApplicationEntity(applicationId);

        // Security check: Beneficiaries can only upload documents for their own applications
        boolean isOwner = application.getBeneficiary().getUser().getId().equals(actingUser.getId());
        boolean isStaff = actingUser.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN") || r.getName().name().equals("ROLE_FIELD_OFFICER"));
        if (!isOwner && !isStaff) {
            throw new UnauthorizedException("You are not authorized to upload documents for another citizen's application");
        }

        DocumentStorageService.StoredDocumentInfo storedInfo = documentStorageService.storeFile(file);

        ApplicationDocument doc = new ApplicationDocument(
                application,
                documentType,
                storedInfo.originalFileName(),
                storedInfo.contentType(),
                storedInfo.storagePath()
        );
        ApplicationDocument saved = documentRepository.save(doc);

        auditLogService.logAction(
                actingUser.getUsername(),
                "DOCUMENT_UPLOADED",
                "ApplicationDocument",
                saved.getId().toString(),
                null,
                "ATTACHED",
                "Secure document upload: " + documentType + " (" + storedInfo.originalFileName() + ") to application " + application.getApplicationNumber()
        );

        return new DocumentDto(
                saved.getId(),
                saved.getDocumentType(),
                saved.getFileName(),
                saved.getFileType(),
                saved.getFilePath(),
                saved.getVerificationStatus(),
                saved.getUploadedAt()
        );
    }

    @Transactional(readOnly = true)
    public Resource loadDocumentResource(Long applicationId, Long documentId, User actingUser) {
        SubsidyApplication application = getApplicationEntity(applicationId);
        boolean isOwner = application.getBeneficiary().getUser().getId().equals(actingUser.getId());
        boolean isStaff = actingUser.getRoles().stream()
                .anyMatch(r -> !r.getName().name().equals("ROLE_BENEFICIARY"));
        if (!isOwner && !isStaff) {
            throw new UnauthorizedException("Access denied: You do not have permission to view documents for this application");
        }

        ApplicationDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        if (!doc.getApplication().getId().equals(applicationId)) {
            throw new BadRequestException("Document does not belong to the specified application");
        }

        return documentStorageService.loadFileAsResource(doc.getFilePath());
    }

    @Transactional
    public ApplicationResponse submitApplication(Long applicationId, String actingUser) {
        SubsidyApplication app = getApplicationEntity(applicationId);

        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new InvalidStatusTransitionException(app.getStatus(), ApplicationStatus.SUBMITTED);
        }

        app.setStatus(ApplicationStatus.SUBMITTED);
        SubsidyApplication saved = applicationRepository.save(app);

        auditLogService.logAction(
                actingUser,
                "APPLICATION_SUBMITTED",
                "SubsidyApplication",
                saved.getId().toString(),
                ApplicationStatus.DRAFT.name(),
                ApplicationStatus.SUBMITTED.name(),
                "Application submitted by beneficiary"
        );

        if (app.getBeneficiary() != null && app.getBeneficiary().getUser() != null) {
            String recipient = app.getBeneficiary().getUser().getUsername();
            notificationService.createNotification(
                    recipient,
                    "Application Submitted: " + app.getApplicationNumber(),
                    "Your subsidy application for '" + app.getScheme().getTitle() + "' has been submitted successfully and is queued for automated eligibility scoring.",
                    NotificationType.APPLICATION_STATUS_UPDATE,
                    "SubsidyApplication",
                    saved.getId(),
                    "/beneficiary/applications/" + saved.getId()
            );
        }

        return mapToResponse(saved);
    }

    @Transactional
    public EligibilityEvaluationResult evaluateEligibility(Long applicationId, String actingUser) {
        SubsidyApplication app = getApplicationEntity(applicationId);

        if (app.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new InvalidStatusTransitionException("Application must be in SUBMITTED status to evaluate eligibility. Current status: " + app.getStatus());
        }

        EligibilityEvaluationResult evaluation = eligibilityService.evaluate(app);

        app.setEligibilityScore(evaluation.getTotalScore());
        app.setRiskLevel(evaluation.getAssignedRiskLevel());

        ApplicationStatus previousStatus = app.getStatus();
        ApplicationStatus newStatus;

        if (evaluation.isEligible()) {
            newStatus = ApplicationStatus.FIELD_VERIFICATION;
            app.setRejectionReason(null);
        } else {
            newStatus = ApplicationStatus.REJECTED;
            app.setRejectionReason(evaluation.getRecommendation());
        }

        app.setStatus(newStatus);
        SubsidyApplication saved = applicationRepository.save(app);

        auditLogService.logAction(
                actingUser,
                "ELIGIBILITY_EVALUATED",
                "SubsidyApplication",
                saved.getId().toString(),
                previousStatus.name(),
                newStatus.name(),
                "Score: " + evaluation.getTotalScore() + ", Risk: " + evaluation.getAssignedRiskLevel() + ", Recommendation: " + evaluation.getRecommendation()
        );

        if (app.getBeneficiary() != null && app.getBeneficiary().getUser() != null) {
            String recipient = app.getBeneficiary().getUser().getUsername();
            if (evaluation.isEligible()) {
                notificationService.createNotification(
                        recipient,
                        "Eligibility Passed (" + evaluation.getTotalScore() + " pts)",
                        "Application " + app.getApplicationNumber() + " passed eligibility scoring (" + evaluation.getTotalScore() + " pts) and advanced to Ground Field Verification.",
                        NotificationType.ELIGIBILITY_EVALUATION,
                        "SubsidyApplication",
                        saved.getId(),
                        "/beneficiary/applications/" + saved.getId()
                );
            } else {
                notificationService.createNotification(
                        recipient,
                        "Eligibility Evaluation: Not Eligible",
                        "Application " + app.getApplicationNumber() + " did not meet qualifying criteria: " + evaluation.getRecommendation(),
                        NotificationType.ELIGIBILITY_EVALUATION,
                        "SubsidyApplication",
                        saved.getId(),
                        "/beneficiary/applications/" + saved.getId()
                );
            }
        }

        return evaluation;
    }

    @Transactional(readOnly = true)
    public SubsidyApplication getApplicationEntity(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        return mapToResponse(getApplicationEntity(id));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationByIdForUser(Long id, User currentUser) {
        SubsidyApplication app = getApplicationEntity(id);
        boolean isOwner = app.getBeneficiary().getUser().getId().equals(currentUser.getId());
        boolean isStaff = currentUser.getRoles().stream()
                .anyMatch(r -> !r.getName().name().equals("ROLE_BENEFICIARY"));
        if (!isOwner && !isStaff) {
            throw new UnauthorizedException("Access denied: You are only permitted to view your own applications");
        }
        return mapToResponse(app);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationByNumber(String applicationNumber) {
        SubsidyApplication app = applicationRepository.findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with number: " + applicationNumber));
        return mapToResponse(app);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByBeneficiary(Long beneficiaryId) {
        return applicationRepository.findByBeneficiaryId(beneficiaryId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByUser(Long userId) {
        return applicationRepository.findByBeneficiaryUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplications(ApplicationStatus status) {
        List<SubsidyApplication> list = (status != null) ?
                applicationRepository.findByStatus(status) :
                applicationRepository.findAll();
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private String generateApplicationNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "SUB-" + datePrefix + "-" + randomSuffix;
    }

    public ApplicationResponse mapToResponse(SubsidyApplication a) {
        ApplicationResponse res = new ApplicationResponse();
        res.setId(a.getId());
        res.setApplicationNumber(a.getApplicationNumber());
        res.setBeneficiaryId(a.getBeneficiary().getId());
        res.setBeneficiaryName(a.getBeneficiary().getUser().getFullName());
        res.setBeneficiaryIdentityNumber(a.getBeneficiary().getIdentityNumber());
        res.setSchemeId(a.getScheme().getId());
        res.setSchemeCode(a.getScheme().getCode());
        res.setSchemeTitle(a.getScheme().getTitle());
        res.setRegionId(a.getRegion().getId());
        res.setRegionName(a.getRegion().getDistrictName());
        res.setStateName(a.getRegion().getStateName());
        res.setAppliedAmount(a.getAppliedAmount());
        res.setApprovedAmount(a.getApprovedAmount());
        res.setStatus(a.getStatus());
        res.setRiskLevel(a.getRiskLevel());
        res.setEligibilityScore(a.getEligibilityScore());
        res.setRejectionReason(a.getRejectionReason());
        res.setDisbursementPlanId(a.getDisbursementPlan() != null ? a.getDisbursementPlan().getId() : null);
        res.setCreatedAt(a.getCreatedAt());
        res.setUpdatedAt(a.getUpdatedAt());

        // SLA monitoring calculation
        int targetDays = switch (a.getStatus()) {
            case DRAFT -> 1;
            case SUBMITTED -> 2;
            case FIELD_VERIFICATION -> 3;
            case DISTRICT_REVIEW -> 4;
            case FINANCE_APPROVAL -> 3;
            case DISBURSEMENT_PLANNED, MILESTONE_PENDING, DISBURSEMENT_IN_PROGRESS -> 5;
            case UTILIZATION_PENDING -> 14;
            case COMPLETED, REJECTED -> 0;
            default -> 3;
        };

        LocalDateTime baseDate = a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt();
        long elapsedDays = ChronoUnit.DAYS.between(baseDate, LocalDateTime.now());
        if (elapsedDays < 0) elapsedDays = 0;

        String slaStatus;
        if (a.getStatus() == ApplicationStatus.COMPLETED) {
            slaStatus = "COMPLETED";
        } else if (a.getStatus() == ApplicationStatus.REJECTED) {
            slaStatus = "REJECTED";
        } else if (elapsedDays > targetDays) {
            slaStatus = "OVERDUE";
        } else if (elapsedDays >= targetDays - 1) {
            slaStatus = "DUE_SOON";
        } else {
            slaStatus = "ON_TRACK";
        }

        res.setSlaTargetDays(targetDays);
        res.setSlaElapsedDays(elapsedDays);
        res.setSlaStatus(slaStatus);
        res.setSlaTargetDate(baseDate.plusDays(targetDays));

        if (a.getDocuments() != null) {
            res.setDocuments(a.getDocuments().stream().map(d -> new DocumentDto(
                    d.getId(),
                    d.getDocumentType(),
                    d.getFileName(),
                    d.getFileType(),
                    d.getFilePath(),
                    d.getVerificationStatus(),
                    d.getUploadedAt()
            )).collect(Collectors.toList()));
        }
        return res;
    }
}
