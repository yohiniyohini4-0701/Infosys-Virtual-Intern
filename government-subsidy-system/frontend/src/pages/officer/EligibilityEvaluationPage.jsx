import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { applicationService } from '../../services/applicationService';
import { useToast } from '../../context/ToastContext';
import { formatCurrencyINR, formatDate } from '../../utils/formatters';
import { StatusBadge, RiskBadge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import {
  ClipboardCheck,
  PlayCircle,
  RefreshCw,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Eye,
  TrendingUp,
  BarChart2,
  Layers,
} from 'lucide-react';

// ─────────────────────────────────────────────────────────────────────────────
// Small helper: score breakdown table shown inside the evaluate modal
// ─────────────────────────────────────────────────────────────────────────────
const ScoreBreakdownTable = ({ criteriaDetails }) => {
  if (!criteriaDetails || criteriaDetails.length === 0) {
    return (
      <p style={{ fontSize: '13px', color: 'var(--text-muted)', fontStyle: 'italic' }}>
        No individual criteria details available (scheme uses default pass-all logic).
      </p>
    );
  }

  return (
    <div className="table-wrapper" style={{ marginTop: '12px' }}>
      <table className="gov-table" style={{ fontSize: '12px' }}>
        <thead>
          <tr>
            <th>Criterion</th>
            <th>Operator</th>
            <th>Required</th>
            <th>Actual</th>
            <th>Mandatory</th>
            <th>Result</th>
            <th style={{ textAlign: 'right' }}>Pts Awarded</th>
            <th style={{ textAlign: 'right' }}>Max Pts</th>
          </tr>
        </thead>
        <tbody>
          {criteriaDetails.map((c, idx) => (
            <tr key={idx} style={{ backgroundColor: c.satisfied ? undefined : 'rgba(220,38,38,0.04)' }}>
              <td style={{ fontWeight: 600 }}>
                {c.criterionType?.replace(/_/g, ' ')}
              </td>
              <td style={{ color: 'var(--text-muted)', fontSize: '11px' }}>
                {c.operator?.replace(/_/g, ' ')}
              </td>
              <td>{c.expectedValue}</td>
              <td style={{ fontWeight: 600 }}>{c.actualValue}</td>
              <td>
                {c.mandatory ? (
                  <span style={{ color: 'var(--gov-crimson-600)', fontWeight: 700, fontSize: '11px' }}>YES</span>
                ) : (
                  <span style={{ color: 'var(--text-muted)', fontSize: '11px' }}>No</span>
                )}
              </td>
              <td>
                {c.satisfied ? (
                  <span style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#059669', fontWeight: 600, fontSize: '11px' }}>
                    <CheckCircle2 size={13} /> Passed
                  </span>
                ) : (
                  <span style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#dc2626', fontWeight: 600, fontSize: '11px' }}>
                    <XCircle size={13} /> Failed
                  </span>
                )}
              </td>
              <td style={{ textAlign: 'right', fontWeight: 700, color: c.satisfied ? '#059669' : '#dc2626' }}>
                {c.weightPointsAwarded}
              </td>
              <td style={{ textAlign: 'right', color: 'var(--text-muted)' }}>
                {c.maxWeightPoints}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
// Summary stat card component (used in the bulk results panel)
// ─────────────────────────────────────────────────────────────────────────────
const SummaryCard = ({ label, value, color, icon: Icon, dimmed }) => (
  <div
    style={{
      background: dimmed ? 'var(--gov-slate-50)' : 'white',
      border: `1.5px solid ${color}30`,
      borderRadius: 'var(--radius-md)',
      padding: '16px 18px',
      display: 'flex',
      alignItems: 'center',
      gap: '14px',
      opacity: dimmed ? 0.55 : 1,
    }}
  >
    <div
      style={{
        width: 40,
        height: 40,
        borderRadius: '50%',
        background: `${color}18`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
      }}
    >
      <Icon size={20} color={color} />
    </div>
    <div>
      <div style={{ fontSize: '22px', fontWeight: 800, color, lineHeight: 1 }}>{value}</div>
      <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>{label}</div>
    </div>
  </div>
);

// ─────────────────────────────────────────────────────────────────────────────
// MAIN PAGE
// ─────────────────────────────────────────────────────────────────────────────
export const EligibilityEvaluationPage = () => {
  const { success: toastSuccess, error: toastError } = useToast();

  // Pending SUBMITTED applications
  const [submittedApps, setSubmittedApps] = useState([]);
  const [loadingApps, setLoadingApps] = useState(true);

  // Individual evaluate modal state
  const [evaluateModalApp, setEvaluateModalApp] = useState(null);   // app row being evaluated
  const [evalResult, setEvalResult] = useState(null);                // EligibilityEvaluationResult
  const [evaluating, setEvaluating] = useState(false);              // spinner for individual eval

  // Bulk evaluation state
  const [bulkRunning, setBulkRunning] = useState(false);
  const [bulkResult, setBulkResult] = useState(null);               // BulkEligibilityResponse
  const [bulkError, setBulkError] = useState(null);

  // Retry-failed state
  const [retryRunning, setRetryRunning] = useState(false);

  // ── Load SUBMITTED applications ────────────────────────────────────────────
  const loadSubmittedApps = useCallback(async () => {
    setLoadingApps(true);
    try {
      const data = await applicationService.getAllApplications('SUBMITTED');
      setSubmittedApps(Array.isArray(data) ? data : []);
    } catch (err) {
      toastError(err?.message || 'Failed to load pending applications.');
      setSubmittedApps([]);
    } finally {
      setLoadingApps(false);
    }
  }, []);

  useEffect(() => {
    loadSubmittedApps();
  }, [loadSubmittedApps]);

  // ── Individual evaluation ──────────────────────────────────────────────────
  const handleEvaluateOne = async (app) => {
    setEvaluateModalApp(app);
    setEvalResult(null);
    setEvaluating(true);
    try {
      const result = await applicationService.evaluateEligibility(app.id);
      setEvalResult(result);
      // Remove from pending queue since it is no longer SUBMITTED
      setSubmittedApps((prev) => prev.filter((a) => a.id !== app.id));
      toastSuccess(`Eligibility evaluated for ${app.applicationNumber}`);
    } catch (err) {
      toastError(err?.message || 'Evaluation failed. Please try again.');
      setEvaluateModalApp(null);
    } finally {
      setEvaluating(false);
    }
  };

  // ── Bulk evaluation ────────────────────────────────────────────────────────
  const handleEvaluateAll = async () => {
    setBulkRunning(true);
    setBulkResult(null);
    setBulkError(null);
    try {
      const result = await applicationService.bulkEvaluateAllPending();
      setBulkResult(result);
      // Reload queue — processed apps are no longer SUBMITTED
      await loadSubmittedApps();
      if (result.failureCount === 0) {
        toastSuccess(`Bulk evaluation complete: ${result.successCount} applications processed.`);
      } else {
        toastError(`Bulk evaluation finished with ${result.failureCount} failure(s). Review the results below.`);
      }
    } catch (err) {
      setBulkError(err?.message || 'Bulk evaluation failed. Please check your connection and try again.');
      toastError('Bulk evaluation failed.');
    } finally {
      setBulkRunning(false);
    }
  };

  // ── Retry failed items ─────────────────────────────────────────────────────
  const handleRetryFailed = async () => {
    if (!bulkResult) return;
    const failedItems = bulkResult.items.filter((i) => !i.isSuccess && !i.success);
    if (failedItems.length === 0) return;

    setRetryRunning(true);
    const updatedItems = [...bulkResult.items];
    let newSuccessCount = bulkResult.successCount;
    let newFailureCount = bulkResult.failureCount;
    let newEligibleCount = bulkResult.eligibleCount;
    let newIneligibleCount = bulkResult.ineligibleCount;

    for (const failedItem of failedItems) {
      const idx = updatedItems.findIndex((i) => i.applicationId === failedItem.applicationId);
      try {
        const result = await applicationService.evaluateEligibility(failedItem.applicationId);
        updatedItems[idx] = {
          ...updatedItems[idx],
          success: true,
          eligible: result.eligible,
          score: result.totalScore,
          riskLevel: result.assignedRiskLevel,
          newStatus: result.eligible ? 'FIELD_VERIFICATION' : 'REJECTED',
          recommendation: result.recommendation,
          failureReason: null,
        };
        newSuccessCount++;
        newFailureCount--;
        if (result.eligible) newEligibleCount++;
        else newIneligibleCount++;
      } catch (err) {
        updatedItems[idx] = {
          ...updatedItems[idx],
          failureReason: err?.message || 'Retry failed',
        };
      }
    }

    setBulkResult({
      ...bulkResult,
      successCount: newSuccessCount,
      failureCount: newFailureCount,
      eligibleCount: newEligibleCount,
      ineligibleCount: newIneligibleCount,
      items: updatedItems,
    });
    await loadSubmittedApps();
    setRetryRunning(false);
    toastSuccess('Retry complete.');
  };

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div>
      {/* ── Page Header ── */}
      <div
        style={{
          background: 'linear-gradient(135deg, var(--gov-navy-950), var(--gov-navy-800))',
          color: '#ffffff',
          borderRadius: 'var(--radius-lg)',
          padding: '24px 28px',
          marginBottom: '24px',
          boxShadow: 'var(--shadow-md)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '16px',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
            <ClipboardCheck size={24} color="#f59e0b" />
            <h2 style={{ fontSize: '20px', fontWeight: 800, color: '#ffffff', margin: 0 }}>
              Eligibility Evaluation Workbench
            </h2>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--gov-slate-300)', margin: 0 }}>
            Evaluate SUBMITTED applications using the scheme criteria. Only authorized Field Officers may perform evaluation.
          </p>
        </div>

        {/* EVALUATE ALL primary action */}
        <div style={{ display: 'flex', gap: '10px', flexShrink: 0 }}>
          <button
            onClick={handleEvaluateAll}
            disabled={bulkRunning || loadingApps || submittedApps.length === 0}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '11px 22px',
              borderRadius: 'var(--radius-md)',
              border: 'none',
              background: bulkRunning || submittedApps.length === 0
                ? 'rgba(255,255,255,0.15)'
                : 'linear-gradient(135deg, #f59e0b, #d97706)',
              color: '#ffffff',
              fontWeight: 700,
              fontSize: '13px',
              cursor: bulkRunning || submittedApps.length === 0 ? 'not-allowed' : 'pointer',
              boxShadow: bulkRunning || submittedApps.length === 0 ? 'none' : '0 2px 12px rgba(245,158,11,0.35)',
              transition: 'all 0.2s',
            }}
          >
            {bulkRunning ? (
              <>
                <span
                  style={{
                    width: 16,
                    height: 16,
                    border: '2px solid rgba(255,255,255,0.4)',
                    borderTopColor: '#fff',
                    borderRadius: '50%',
                    animation: 'spin 0.7s linear infinite',
                    display: 'inline-block',
                  }}
                />
                Evaluating…
              </>
            ) : (
              <>
                <PlayCircle size={17} />
                EVALUATE ALL PENDING
              </>
            )}
          </button>
        </div>
      </div>

      {/* ── Bulk In-Progress Banner ── */}
      {bulkRunning && (
        <div
          style={{
            background: 'linear-gradient(90deg, #1e3a5f, #1d4ed8)',
            color: '#fff',
            borderRadius: 'var(--radius-md)',
            padding: '16px 22px',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
          }}
        >
          <span
            style={{
              width: 22,
              height: 22,
              border: '3px solid rgba(255,255,255,0.35)',
              borderTopColor: '#fff',
              borderRadius: '50%',
              animation: 'spin 0.7s linear infinite',
              flexShrink: 0,
              display: 'inline-block',
            }}
          />
          <div>
            <div style={{ fontWeight: 700, fontSize: '14px' }}>
              Evaluating applications…
            </div>
            <div style={{ fontSize: '12px', color: 'rgba(255,255,255,0.75)', marginTop: '2px' }}>
              Processing {submittedApps.length} pending application{submittedApps.length !== 1 ? 's' : ''}. Please wait.
            </div>
          </div>
          <div style={{ flexGrow: 1 }}>
            <div
              style={{
                height: 6,
                background: 'rgba(255,255,255,0.2)',
                borderRadius: 99,
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  height: '100%',
                  width: '60%',
                  background: 'rgba(255,255,255,0.8)',
                  borderRadius: 99,
                  animation: 'progress-indeterminate 1.4s ease-in-out infinite',
                }}
              />
            </div>
          </div>
        </div>
      )}

      {/* ── API Error Banner ── */}
      {bulkError && !bulkRunning && (
        <div
          style={{
            background: '#fef2f2',
            border: '1.5px solid #fca5a5',
            borderRadius: 'var(--radius-md)',
            padding: '14px 18px',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
          }}
        >
          <XCircle size={18} color="#dc2626" />
          <div>
            <strong style={{ color: '#dc2626' }}>Bulk Evaluation Failed</strong>
            <div style={{ fontSize: '12px', color: '#7f1d1d', marginTop: '2px' }}>{bulkError}</div>
          </div>
        </div>
      )}

      {/* ── Bulk Results Summary ── */}
      {bulkResult && !bulkRunning && (
        <div
          style={{
            background: 'white',
            border: '1.5px solid var(--border)',
            borderRadius: 'var(--radius-lg)',
            padding: '20px 24px',
            marginBottom: '24px',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          {/* Header */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <BarChart2 size={20} color="var(--gov-navy-700)" />
              <h3 style={{ fontWeight: 800, fontSize: '15px', margin: 0 }}>Bulk Evaluation Results</h3>
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              {bulkResult.failureCount > 0 && (
                <Button
                  variant="warning"
                  size="sm"
                  icon={RefreshCw}
                  loading={retryRunning}
                  onClick={handleRetryFailed}
                >
                  RETRY FAILED ({bulkResult.failureCount})
                </Button>
              )}
            </div>
          </div>

          {/* Partial failure alert */}
          {bulkResult.failureCount > 0 && (
            <div
              style={{
                background: '#fffbeb',
                border: '1.5px solid #fbbf24',
                borderRadius: 'var(--radius-md)',
                padding: '10px 14px',
                marginBottom: '16px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '12px',
                color: '#92400e',
              }}
            >
              <AlertTriangle size={15} color="#d97706" />
              <span>
                <strong>{bulkResult.failureCount} application{bulkResult.failureCount !== 1 ? 's' : ''} could not be evaluated.</strong>
                {' '}Use "RETRY FAILED" to attempt them again, or inspect the table below for details.
              </span>
            </div>
          )}

          {/* Summary KPI grid */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))',
              gap: '12px',
              marginBottom: '20px',
            }}
          >
            <SummaryCard label="Total Processed" value={bulkResult.totalProcessed} color="#1e3a5f" icon={Layers} />
            <SummaryCard label="Successfully Evaluated" value={bulkResult.successCount} color="#059669" icon={CheckCircle2} />
            <SummaryCard label="Failed" value={bulkResult.failureCount} color="#dc2626" icon={XCircle} dimmed={bulkResult.failureCount === 0} />
            <SummaryCard label="Eligible" value={bulkResult.eligibleCount} color="#0284c7" icon={CheckCircle2} dimmed={bulkResult.eligibleCount === 0} />
            <SummaryCard label="Not Eligible" value={bulkResult.ineligibleCount} color="#9333ea" icon={XCircle} dimmed={bulkResult.ineligibleCount === 0} />
            <SummaryCard label="High-Value Escalation" value={bulkResult.highValueCount} color="#d97706" icon={AlertTriangle} dimmed={bulkResult.highValueCount === 0} />
            <SummaryCard label="Flagged / Borderline" value={bulkResult.flaggedCount} color="#f59e0b" icon={AlertTriangle} dimmed={bulkResult.flaggedCount === 0} />
          </div>

          {/* Per-application results table */}
          <div className="table-wrapper">
            <table className="gov-table" style={{ fontSize: '12px' }}>
              <thead>
                <tr>
                  <th>App Ref</th>
                  <th>Beneficiary</th>
                  <th style={{ textAlign: 'center' }}>Result</th>
                  <th style={{ textAlign: 'center' }}>Score</th>
                  <th>Risk</th>
                  <th>New Status</th>
                  <th>Recommendation / Failure</th>
                </tr>
              </thead>
              <tbody>
                {bulkResult.items.map((item) => (
                  <tr
                    key={item.applicationId}
                    style={{
                      backgroundColor: !item.success
                        ? 'rgba(220,38,38,0.04)'
                        : item.eligible
                        ? undefined
                        : 'rgba(147,51,234,0.03)',
                    }}
                  >
                    <td style={{ fontWeight: 700, color: 'var(--gov-navy-900)' }}>
                      {item.applicationNumber}
                    </td>
                    <td>{item.beneficiaryName}</td>
                    <td style={{ textAlign: 'center' }}>
                      {!item.success ? (
                        <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', color: '#dc2626', fontWeight: 700 }}>
                          <XCircle size={13} /> ERROR
                        </span>
                      ) : item.eligible ? (
                        <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', color: '#059669', fontWeight: 700 }}>
                          <CheckCircle2 size={13} /> ELIGIBLE
                        </span>
                      ) : (
                        <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', color: '#9333ea', fontWeight: 700 }}>
                          <XCircle size={13} /> REJECTED
                        </span>
                      )}
                    </td>
                    <td style={{ textAlign: 'center', fontWeight: 700 }}>
                      {item.success ? item.score : '—'}
                    </td>
                    <td>
                      {item.riskLevel ? <RiskBadge riskLevel={item.riskLevel} /> : '—'}
                    </td>
                    <td>
                      {item.newStatus ? <StatusBadge status={item.newStatus} /> : '—'}
                    </td>
                    <td style={{ fontSize: '11px', color: item.success ? 'var(--text-muted)' : '#dc2626', maxWidth: 260 }}>
                      {item.success ? item.recommendation : item.failureReason}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ── Pending Applications Queue ── */}
      <div className="gov-card">
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '16px 20px',
            borderBottom: '1px solid var(--border)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <TrendingUp size={18} color="var(--gov-navy-700)" />
            <h3 style={{ fontWeight: 800, fontSize: '15px', margin: 0 }}>
              Applications Pending Eligibility Evaluation
            </h3>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              {submittedApps.length} SUBMITTED
            </span>
            <Button variant="secondary" size="sm" icon={RefreshCw} onClick={loadSubmittedApps} loading={loadingApps}>
              Refresh
            </Button>
          </div>
        </div>

        {loadingApps ? (
          <div style={{ padding: '16px 20px' }}>
            <LoadingSkeleton rows={4} height={52} />
          </div>
        ) : submittedApps.length === 0 ? (
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '48px 20px',
              color: 'var(--text-muted)',
              gap: '10px',
            }}
          >
            <CheckCircle2 size={40} color="var(--gov-emerald-500)" />
            <p style={{ fontWeight: 700, fontSize: '15px', margin: 0 }}>No pending applications</p>
            <p style={{ fontSize: '13px', margin: 0 }}>
              All submitted applications have been evaluated, or none have been submitted yet.
            </p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="gov-table">
              <thead>
                <tr>
                  <th>App Ref</th>
                  <th>Beneficiary</th>
                  <th>Scheme</th>
                  <th>Region</th>
                  <th>Applied Amount</th>
                  <th>Submitted</th>
                  <th style={{ textAlign: 'center' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {submittedApps.map((app) => (
                  <tr key={app.id}>
                    <td style={{ fontWeight: 700, color: 'var(--gov-navy-900)' }}>
                      {app.applicationNumber}
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{app.beneficiaryName}</div>
                      <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                        {app.beneficiaryIdentityNumber}
                      </div>
                    </td>
                    <td>
                      <div style={{ fontWeight: 500 }}>{app.schemeTitle}</div>
                      <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{app.schemeCode}</div>
                    </td>
                    <td>{app.regionName}</td>
                    <td style={{ fontWeight: 600 }}>{formatCurrencyINR(app.appliedAmount)}</td>
                    <td style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                      {formatDate(app.createdAt)}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <div style={{ display: 'flex', justifyContent: 'center', gap: '6px' }}>
                        <Button
                          variant="primary"
                          size="sm"
                          icon={ClipboardCheck}
                          onClick={() => handleEvaluateOne(app)}
                          loading={evaluating && evaluateModalApp?.id === app.id}
                        >
                          Evaluate
                        </Button>
                        <Link to={`/beneficiary/applications/${app.id}`}>
                          <Button variant="secondary" size="sm" icon={Eye} />
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ── Individual Evaluate Result Modal ── */}
      {evaluateModalApp && (
        <Modal
          isOpen
          onClose={() => { setEvaluateModalApp(null); setEvalResult(null); }}
          title={`Eligibility Evaluation: ${evaluateModalApp.applicationNumber}`}
          size="lg"
        >
          {evaluating && (
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                padding: '32px',
                gap: '14px',
                color: 'var(--text-muted)',
              }}
            >
              <span
                style={{
                  width: 36,
                  height: 36,
                  border: '4px solid var(--border)',
                  borderTopColor: 'var(--gov-navy-700)',
                  borderRadius: '50%',
                  animation: 'spin 0.7s linear infinite',
                  display: 'inline-block',
                }}
              />
              <p style={{ fontWeight: 600 }}>Calculating eligibility score…</p>
            </div>
          )}

          {evalResult && !evaluating && (
            <div>
              {/* Result banner */}
              <div
                style={{
                  borderRadius: 'var(--radius-md)',
                  padding: '14px 18px',
                  marginBottom: '18px',
                  background: evalResult.eligible ? '#f0fdf4' : '#fef2f2',
                  border: `1.5px solid ${evalResult.eligible ? '#86efac' : '#fca5a5'}`,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                }}
              >
                {evalResult.eligible ? (
                  <CheckCircle2 size={28} color="#059669" />
                ) : (
                  <XCircle size={28} color="#dc2626" />
                )}
                <div>
                  <div
                    style={{
                      fontWeight: 800,
                      fontSize: '16px',
                      color: evalResult.eligible ? '#065f46' : '#991b1b',
                    }}
                  >
                    {evalResult.eligible ? 'ELIGIBLE' : 'NOT ELIGIBLE'}
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '2px' }}>
                    {evalResult.recommendation}
                  </div>
                </div>
                <div style={{ marginLeft: 'auto', textAlign: 'right' }}>
                  <div style={{ fontSize: '28px', fontWeight: 900, color: evalResult.eligible ? '#059669' : '#dc2626' }}>
                    {evalResult.totalScore}
                  </div>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                    / min {evalResult.minQualifyingScore} pts
                  </div>
                </div>
              </div>

              {/* Risk level */}
              <div style={{ marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 600 }}>Risk Classification:</span>
                <RiskBadge riskLevel={evalResult.assignedRiskLevel} />
              </div>

              {/* Score breakdown table */}
              <h4 style={{ fontWeight: 700, fontSize: '13px', marginBottom: '6px' }}>
                Score Breakdown by Criterion
              </h4>
              <ScoreBreakdownTable criteriaDetails={evalResult.criteriaDetails} />

              {/* Close button */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '20px' }}>
                <Button
                  variant="secondary"
                  onClick={() => { setEvaluateModalApp(null); setEvalResult(null); }}
                >
                  Close
                </Button>
              </div>
            </div>
          )}
        </Modal>
      )}

      {/* Keyframe animations via inline style tag */}
      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        @keyframes progress-indeterminate {
          0%   { transform: translateX(-100%); width: 50%; }
          50%  { transform: translateX(60%); width: 50%; }
          100% { transform: translateX(200%); width: 50%; }
        }
      `}</style>
    </div>
  );
};
