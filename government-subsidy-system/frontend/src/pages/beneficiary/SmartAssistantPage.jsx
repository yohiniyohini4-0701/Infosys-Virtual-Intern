import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { assistantService } from '../../services/assistantService';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/Badge';
import { formatCurrencyINR } from '../../utils/formatters';
import {
  Bot,
  User,
  Send,
  Sparkles,
  Layers,
  FileText,
  CreditCard,
  ClipboardCheck,
  RefreshCw,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
  XCircle,
  HelpCircle
} from 'lucide-react';

export const SmartAssistantPage = () => {
  const { user } = useAuth();
  const [messages, setMessages] = useState([
    {
      id: 1,
      sender: 'assistant',
      text: `Namaste ${user?.fullName || 'Citizen'}! I am your Government Subsidy & Scheme Assistant.\n\nI can help you discover welfare schemes you qualify for, track your submitted application status, explain milestone disbursements, or provide document checklists. Select a quick query below or type your question!`,
      category: 'GENERAL_HELP',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    },
  ]);
  const [inputQuery, setInputQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [previewScheme, setPreviewScheme] = useState(null);
  const [previewData, setPreviewData] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const chatEndRef = useRef(null);

  const quickPrompts = [
    { label: 'Recommended Schemes', query: 'What schemes are available for me based on my profile?', icon: Layers },
    { label: 'Track Application Status', query: 'What is the status of my applications?', icon: FileText },
    { label: 'Document Checklist', query: 'What documents do I need to submit?', icon: ClipboardCheck },
    { label: 'Disbursement Rules', query: 'How do DBT milestone disbursements work?', icon: CreditCard },
  ];

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const handleSend = async (queryText) => {
    const text = queryText || inputQuery;
    if (!text || !text.trim() || loading) return;

    const userMsg = {
      id: Date.now(),
      sender: 'user',
      text: text.trim(),
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputQuery('');
    setLoading(true);

    try {
      const res = await assistantService.queryAssistant(text.trim());
      const botMsg = {
        id: Date.now() + 1,
        sender: 'assistant',
        text: res.answer,
        category: res.category,
        relatedData: res.relatedData || [],
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, botMsg]);
    } catch (err) {
      const errorMsg = {
        id: Date.now() + 1,
        sender: 'assistant',
        text: 'I encountered an issue retrieving the latest government records. Please verify your connection or try another query.',
        category: 'ERROR',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setLoading(false);
    }
  };

  const handlePreviewEligibility = async (schemeId) => {
    setPreviewLoading(true);
    setPreviewScheme(schemeId);
    try {
      const data = await assistantService.previewEligibility(schemeId);
      setPreviewData(data);
    } catch (err) {
      console.error('Failed to load eligibility preview:', err);
    } finally {
      setPreviewLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px' }}>
            <div style={{ backgroundColor: 'var(--gov-navy-900)', color: '#fff', padding: '6px', borderRadius: '8px' }}>
              <Bot size={22} />
            </div>
            <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--gov-navy-900)' }}>
              Smart Welfare Information Assistant
            </h1>
            <span style={{ fontSize: '11px', fontWeight: 700, padding: '3px 8px', borderRadius: '12px', backgroundColor: '#e0f2fe', color: '#0369a1' }}>
              Rule & Database Driven
            </span>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
            Official automated guidance for scheme eligibility, application tracking, DBT disbursements, and documentation.
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => setMessages([messages[0]])}
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <RefreshCw size={14} /> Clear Chat
        </Button>
      </div>

      {/* Quick Suggestion Prompts */}
      <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
        {quickPrompts.map((p, idx) => {
          const Icon = p.icon;
          return (
            <button
              key={idx}
              onClick={() => handleSend(p.query)}
              disabled={loading}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '8px 14px',
                borderRadius: '20px',
                border: '1px solid var(--border)',
                backgroundColor: 'var(--surface)',
                color: 'var(--gov-navy-900)',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              }}
              onMouseEnter={(e) => (e.currentTarget.style.borderColor = 'var(--gov-navy-800)')}
              onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'var(--border)')}
            >
              <Icon size={14} style={{ color: 'var(--gov-saffron-500)' }} />
              {p.label}
            </button>
          );
        })}
      </div>

      {/* Chat Container */}
      <Card style={{ padding: 0, overflow: 'hidden', minHeight: '520px', display: 'flex', flexDirection: 'column' }}>
        {/* Messages Scroll Area */}
        <div style={{ flex: 1, padding: '24px', overflowY: 'auto', maxHeight: '580px', display: 'flex', flexDirection: 'column', gap: '18px' }}>
          {messages.map((msg) => {
            const isAssistant = msg.sender === 'assistant';
            return (
              <div
                key={msg.id}
                style={{
                  display: 'flex',
                  gap: '12px',
                  alignSelf: isAssistant ? 'flex-start' : 'flex-end',
                  maxWidth: '85%',
                  flexDirection: isAssistant ? 'row' : 'row-reverse',
                }}
              >
                {/* Avatar */}
                <div
                  style={{
                    width: '36px',
                    height: '36px',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                    backgroundColor: isAssistant ? 'var(--gov-navy-900)' : 'var(--gov-saffron-500)',
                    color: '#fff',
                  }}
                >
                  {isAssistant ? <Bot size={18} /> : <User size={18} />}
                </div>

                {/* Message Body */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: isAssistant ? 'flex-start' : 'flex-end' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '11px', color: 'var(--text-muted)' }}>
                    <span style={{ fontWeight: 600 }}>{isAssistant ? 'Government Assistant' : 'You'}</span>
                    <span>• {msg.timestamp}</span>
                    {msg.category && (
                      <span style={{ fontSize: '10px', padding: '1px 6px', borderRadius: '4px', backgroundColor: '#f1f5f9', color: '#475569' }}>
                        {msg.category}
                      </span>
                    )}
                  </div>

                  <div
                    style={{
                      padding: '14px 18px',
                      borderRadius: isAssistant ? '0 16px 16px 16px' : '16px 0 16px 16px',
                      backgroundColor: isAssistant ? '#f8fafc' : 'var(--gov-navy-900)',
                      color: isAssistant ? 'var(--text)' : '#fff',
                      border: isAssistant ? '1px solid var(--border)' : 'none',
                      fontSize: '13px',
                      lineHeight: '1.6',
                      whiteSpace: 'pre-wrap',
                      boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                    }}
                  >
                    {msg.text}

                    {/* Related Data Links / Cards */}
                    {isAssistant && msg.relatedData && msg.relatedData.length > 0 && (
                      <div style={{ marginTop: '14px', paddingTop: '12px', borderTop: '1px solid #e2e8f0', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        <div style={{ fontSize: '11px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-muted)' }}>
                          Direct Actions & Records:
                        </div>
                        {msg.relatedData.map((item, i) => (
                          <div
                            key={i}
                            style={{
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'center',
                              padding: '10px 14px',
                              backgroundColor: '#fff',
                              borderRadius: '8px',
                              border: '1px solid #e2e8f0',
                            }}
                          >
                            <div>
                              <div style={{ fontWeight: 700, fontSize: '13px', color: 'var(--gov-navy-900)' }}>
                                {item.applicationNumber || item.title || item.code}
                              </div>
                              <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                                {item.schemeTitle ? item.schemeTitle : item.department}
                              </div>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                              {item.status && <StatusBadge status={item.status} />}
                              {item.applicationId && (
                                <Link to={`/beneficiary/applications/${item.applicationId}`}>
                                  <Button size="sm" variant="outline">
                                    Track <ArrowRight size={12} style={{ marginLeft: '4px' }} />
                                  </Button>
                                </Link>
                              )}
                              {item.id && (
                                <Button
                                  size="sm"
                                  variant="secondary"
                                  onClick={() => handlePreviewEligibility(item.id)}
                                >
                                  Preview Eligibility
                                </Button>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            );
          })}

          {loading && (
            <div style={{ display: 'flex', gap: '12px', alignItems: 'center', color: 'var(--text-muted)', fontSize: '13px' }}>
              <div style={{ width: '36px', height: '36px', borderRadius: '50%', backgroundColor: 'var(--gov-navy-900)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Bot size={18} />
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 16px', borderRadius: '16px', backgroundColor: '#f1f5f9' }}>
                <Sparkles size={16} className="animate-spin" style={{ color: 'var(--gov-saffron-500)' }} />
                <span>Checking official scheme databases & eligibility criteria...</span>
              </div>
            </div>
          )}
          <div ref={chatEndRef} />
        </div>

        {/* Input Bar */}
        <div style={{ padding: '16px 20px', borderTop: '1px solid var(--border)', backgroundColor: 'var(--surface)', display: 'flex', gap: '12px' }}>
          <input
            type="text"
            value={inputQuery}
            onChange={(e) => setInputQuery(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            placeholder="Ask a question about subsidy schemes, your application status, or eligibility criteria..."
            style={{
              flex: 1,
              padding: '12px 16px',
              borderRadius: '8px',
              border: '1px solid var(--border)',
              fontSize: '13px',
              outline: 'none',
            }}
            disabled={loading}
          />
          <Button
            onClick={() => handleSend()}
            disabled={!inputQuery.trim() || loading}
            style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '0 20px' }}
          >
            <Send size={16} /> Send
          </Button>
        </div>
      </Card>

      {/* Pre-Application Eligibility Preview Modal */}
      {previewScheme && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '20px',
          }}
        >
          <div
            style={{
              backgroundColor: '#fff',
              borderRadius: '12px',
              maxWidth: '650px',
              width: '100%',
              maxHeight: '85vh',
              overflowY: 'auto',
              padding: '24px',
              boxShadow: '0 10px 25px rgba(0,0,0,0.2)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--gov-navy-900)' }}>
                  Pre-Application Eligibility Preview
                </h3>
                <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                  Simulated criteria check based on your verified profile (No DB record created)
                </p>
              </div>
              <button
                onClick={() => { setPreviewScheme(null); setPreviewData(null); }}
                style={{ border: 'none', background: 'none', fontSize: '20px', cursor: 'pointer', color: 'var(--text-muted)' }}
              >
                ✕
              </button>
            </div>

            {previewLoading ? (
              <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
                <Sparkles size={24} className="animate-spin" style={{ margin: '0 auto 12px auto', color: 'var(--gov-saffron-500)' }} />
                <p>Simulating rule engine against your profile...</p>
              </div>
            ) : previewData ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {/* Result Banner */}
                <div
                  style={{
                    padding: '16px',
                    borderRadius: '8px',
                    backgroundColor: previewData.eligible ? '#ecfdf5' : '#fff1f2',
                    border: `1px solid ${previewData.eligible ? '#a7f3d0' : '#fecdd3'}`,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 800, fontSize: '15px', color: previewData.eligible ? '#065f46' : '#9f1239' }}>
                      {previewData.eligible ? 'Qualified for Application' : 'Criteria Gap Identified'}
                    </div>
                    <div style={{ fontSize: '12px', color: previewData.eligible ? '#047857' : '#be123c', marginTop: '2px' }}>
                      {previewData.recommendation}
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '22px', fontWeight: 800, color: previewData.eligible ? '#065f46' : '#9f1239' }}>
                      {previewData.totalScore} / {previewData.minQualifyingScore}
                    </div>
                    <div style={{ fontSize: '10px', textTransform: 'uppercase', fontWeight: 700 }}>Qualifying Score</div>
                  </div>
                </div>

                {/* Grant Range */}
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '12px 16px', backgroundColor: '#f8fafc', borderRadius: '8px', fontSize: '12px' }}>
                  <div>
                    <span style={{ color: 'var(--text-muted)' }}>Scheme: </span>
                    <strong>{previewData.schemeTitle} ({previewData.schemeCode})</strong>
                  </div>
                  <div>
                    <span style={{ color: 'var(--text-muted)' }}>Grant Slab: </span>
                    <strong>{formatCurrencyINR(previewData.minGrantAmount)} - {formatCurrencyINR(previewData.maxGrantAmount)}</strong>
                  </div>
                </div>

                {/* Criteria Breakdown */}
                <div>
                  <h4 style={{ fontSize: '13px', fontWeight: 700, marginBottom: '8px', color: 'var(--gov-navy-900)' }}>
                    Detailed Criteria Breakdown
                  </h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {previewData.criteriaDetails?.map((c, i) => (
                      <div
                        key={i}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '10px 14px',
                          border: '1px solid #e2e8f0',
                          borderRadius: '6px',
                          fontSize: '12px',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          {c.satisfied ? (
                            <CheckCircle2 size={16} style={{ color: '#10b981' }} />
                          ) : (
                            <XCircle size={16} style={{ color: '#ef4444' }} />
                          )}
                          <div>
                            <div style={{ fontWeight: 600 }}>
                              {c.criterionType} ({c.operator} {c.expectedValue})
                            </div>
                            <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                              Your profile: {c.actualValue || 'N/A'} • {c.message}
                            </div>
                          </div>
                        </div>
                        <div style={{ textAlign: 'right', fontWeight: 700 }}>
                          {c.weightPointsAwarded} / {c.maxWeightPoints} pts
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Actions */}
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                  <Button variant="outline" onClick={() => { setPreviewScheme(null); setPreviewData(null); }}>
                    Close
                  </Button>
                  <Link to={`/beneficiary/schemes/${previewData.schemeId}`}>
                    <Button>Proceed to Apply</Button>
                  </Link>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
};
