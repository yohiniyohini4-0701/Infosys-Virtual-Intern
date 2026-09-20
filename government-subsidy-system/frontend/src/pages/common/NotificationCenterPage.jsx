import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { notificationService } from '../../services/notificationService';
import { useToast } from '../../context/ToastContext';
import { formatDateTime } from '../../utils/formatters';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { EmptyState } from '../../components/common/EmptyState';
import {
  Bell,
  CheckCircle2,
  AlertTriangle,
  FileText,
  CreditCard,
  Info,
  CheckCheck,
  Trash2,
  ExternalLink,
  Search,
  Filter,
  RefreshCw,
  ShieldAlert,
} from 'lucide-react';

export const NotificationCenterPage = () => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeFilter, setActiveFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [processingId, setProcessingId] = useState(null);
  const [markingAll, setMarkingAll] = useState(false);

  const { success, error } = useToast();
  const navigate = useNavigate();

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const data = await notificationService.getUserNotifications(false);
      setNotifications(Array.isArray(data) ? data : []);
    } catch (err) {
      error(err.message || 'Failed to load notifications.');
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  const handleMarkAsRead = async (id, e) => {
    e?.stopPropagation();
    setProcessingId(id);
    try {
      const updated = await notificationService.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true, readAt: updated.readAt } : n))
      );
      success('Notification marked as read.');
    } catch (err) {
      error(err.message || 'Failed to update notification.');
    } finally {
      setProcessingId(null);
    }
  };

  const handleMarkAllRead = async () => {
    setMarkingAll(true);
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      success('All notifications marked as read.');
    } catch (err) {
      error(err.message || 'Failed to mark all as read.');
    } finally {
      setMarkingAll(false);
    }
  };

  const handleDelete = async (id, e) => {
    e?.stopPropagation();
    setProcessingId(id);
    try {
      await notificationService.deleteNotification(id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      success('Notification dismissed.');
    } catch (err) {
      error(err.message || 'Failed to dismiss notification.');
    } finally {
      setProcessingId(null);
    }
  };

  const handleOpenAction = (notif) => {
    if (!notif.isRead) {
      handleMarkAsRead(notif.id);
    }
    if (notif.actionUrl) {
      navigate(notif.actionUrl);
    }
  };

  // KPI Calculations
  const totalCount = notifications.length;
  const unreadCount = notifications.filter((n) => !n.isRead).length;
  const dbtCount = notifications.filter((n) => n.type === 'DISBURSEMENT_RELEASED').length;
  const verificationCount = notifications.filter(
    (n) => n.type === 'VERIFICATION_UPDATE' || n.type === 'ELIGIBILITY_EVALUATION'
  ).length;

  // Filtering
  const filteredNotifications = useMemo(() => {
    return notifications.filter((n) => {
      // Tab filter
      if (activeFilter === 'UNREAD' && n.isRead) return false;
      if (
        activeFilter === 'STATUS_UPDATES' &&
        n.type !== 'APPLICATION_STATUS_UPDATE' &&
        n.type !== 'ELIGIBILITY_EVALUATION'
      )
        return false;
      if (activeFilter === 'DISBURSEMENTS' && n.type !== 'DISBURSEMENT_RELEASED') return false;
      if (activeFilter === 'VERIFICATIONS' && n.type !== 'VERIFICATION_UPDATE') return false;
      if (
        activeFilter === 'ALERTS' &&
        n.type !== 'OVERDUE_ALERT' &&
        n.type !== 'MILESTONE_DUE' &&
        n.type !== 'SYSTEM_NOTICE'
      )
        return false;

      // Search term
      if (searchTerm.trim()) {
        const query = searchTerm.toLowerCase();
        const matchTitle = n.title?.toLowerCase().includes(query);
        const matchMessage = n.message?.toLowerCase().includes(query);
        const matchType = n.type?.toLowerCase().includes(query);
        if (!matchTitle && !matchMessage && !matchType) return false;
      }

      return true;
    });
  }, [notifications, activeFilter, searchTerm]);

  const getTypeBadge = (type) => {
    switch (type) {
      case 'DISBURSEMENT_RELEASED':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              padding: '3px 8px',
              borderRadius: 'var(--radius-sm)',
              fontSize: '11px',
              fontWeight: 700,
              backgroundColor: 'var(--gov-emerald-100)',
              color: 'var(--gov-emerald-800)',
            }}
          >
            <CreditCard size={12} /> DBT RELEASE
          </span>
        );
      case 'ELIGIBILITY_EVALUATION':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              padding: '3px 8px',
              borderRadius: 'var(--radius-sm)',
              fontSize: '11px',
              fontWeight: 700,
              backgroundColor: '#e0f2fe',
              color: '#0369a1',
            }}
          >
            <CheckCircle2 size={12} /> ELIGIBILITY
          </span>
        );
      case 'VERIFICATION_UPDATE':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              padding: '3px 8px',
              borderRadius: 'var(--radius-sm)',
              fontSize: '11px',
              fontWeight: 700,
              backgroundColor: 'var(--gov-gold-100)',
              color: 'var(--gov-gold-800)',
            }}
          >
            <AlertTriangle size={12} /> VERIFICATION
          </span>
        );
      case 'OVERDUE_ALERT':
      case 'MILESTONE_DUE':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              padding: '3px 8px',
              borderRadius: 'var(--radius-sm)',
              fontSize: '11px',
              fontWeight: 700,
              backgroundColor: 'var(--gov-crimson-100)',
              color: 'var(--gov-crimson-800)',
            }}
          >
            <ShieldAlert size={12} /> ACTION DUE
          </span>
        );
      case 'APPLICATION_STATUS_UPDATE':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              padding: '3px 8px',
              borderRadius: 'var(--radius-sm)',
              fontSize: '11px',
              fontWeight: 700,
              backgroundColor: 'var(--gov-navy-100)',
              color: 'var(--gov-navy-800)',
            }}
          >
            <FileText size={12} /> APPLICATION
          </span>
        );
      default:
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              padding: '3px 8px',
              borderRadius: 'var(--radius-sm)',
              fontSize: '11px',
              fontWeight: 700,
              backgroundColor: 'var(--gov-slate-100)',
              color: 'var(--gov-slate-800)',
            }}
          >
            <Info size={12} /> SYSTEM NOTICE
          </span>
        );
    }
  };

  return (
    <div>
      {/* Top Banner */}
      <div
        style={{
          background: 'linear-gradient(135deg, var(--gov-navy-950), var(--gov-navy-850))',
          color: '#ffffff',
          borderRadius: 'var(--radius-lg)',
          padding: '24px 28px',
          marginBottom: '24px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '16px',
          boxShadow: 'var(--shadow-md)',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
            <Bell size={24} color="#f59e0b" />
            <h2 style={{ fontSize: '20px', fontWeight: 800, color: '#ffffff', margin: 0 }}>
              National Subsidy Notification & Alerts Center
            </h2>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--gov-slate-300)', margin: 0 }}>
            Real-time status updates, verification decisions, DBT fund tranches, and milestone compliance alerts.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <Button
            variant="secondary"
            size="sm"
            icon={RefreshCw}
            onClick={loadNotifications}
            loading={loading}
          >
            Refresh
          </Button>
          {unreadCount > 0 && (
            <Button
              variant="primary"
              size="sm"
              icon={CheckCheck}
              onClick={handleMarkAllRead}
              loading={markingAll}
            >
              Mark All Read ({unreadCount})
            </Button>
          )}
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid-cols-4" style={{ marginBottom: '24px' }}>
        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">Total Notifications</span>
            <span className="stat-value">{totalCount}</span>
            <span className="stat-subtext">All time recorded alerts</span>
          </div>
          <div className="stat-icon" style={{ backgroundColor: 'var(--gov-navy-50)', color: 'var(--gov-navy-700)' }}>
            <Bell size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">Unread Messages</span>
            <span className="stat-value" style={{ color: unreadCount > 0 ? 'var(--gov-crimson-600)' : 'inherit' }}>
              {unreadCount}
            </span>
            <span className="stat-subtext">Pending citizen / officer review</span>
          </div>
          <div className="stat-icon" style={{ backgroundColor: 'var(--gov-crimson-50)', color: 'var(--gov-crimson-600)' }}>
            <AlertTriangle size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">DBT Fund Releases</span>
            <span className="stat-value">{dbtCount}</span>
            <span className="stat-subtext">Direct Treasury transfers</span>
          </div>
          <div className="stat-icon" style={{ backgroundColor: 'var(--gov-emerald-50)', color: 'var(--gov-emerald-700)' }}>
            <CreditCard size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">Verification Updates</span>
            <span className="stat-value">{verificationCount}</span>
            <span className="stat-subtext">Scoring & inspection reports</span>
          </div>
          <div className="stat-icon" style={{ backgroundColor: '#e0f2fe', color: '#0369a1' }}>
            <CheckCircle2 size={22} />
          </div>
        </div>
      </div>

      {/* Main Content Card */}
      <div className="gov-card">
        {/* Filter and Search Bar */}
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid var(--border)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '14px',
          }}
        >
          {/* Tab Filter Pills */}
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            {[
              { id: 'ALL', label: 'All Alerts', count: totalCount },
              { id: 'UNREAD', label: 'Unread', count: unreadCount },
              { id: 'STATUS_UPDATES', label: 'Applications', count: null },
              { id: 'DISBURSEMENTS', label: 'DBT Releases', count: dbtCount },
              { id: 'VERIFICATIONS', label: 'Verifications', count: null },
              { id: 'ALERTS', label: 'System & Notices', count: null },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveFilter(tab.id)}
                style={{
                  padding: '6px 14px',
                  borderRadius: '99px',
                  fontSize: '12px',
                  fontWeight: activeFilter === tab.id ? 700 : 600,
                  border: activeFilter === tab.id ? '1px solid var(--gov-navy-800)' : '1px solid var(--border)',
                  backgroundColor: activeFilter === tab.id ? 'var(--gov-navy-800)' : 'var(--surface)',
                  color: activeFilter === tab.id ? '#ffffff' : 'var(--gov-slate-700)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  transition: 'all 0.15s',
                }}
              >
                <span>{tab.label}</span>
                {tab.count !== null && tab.count > 0 && (
                  <span
                    style={{
                      fontSize: '10px',
                      padding: '1px 5px',
                      borderRadius: '99px',
                      backgroundColor: activeFilter === tab.id ? 'rgba(255,255,255,0.25)' : 'var(--gov-slate-200)',
                      color: activeFilter === tab.id ? '#ffffff' : 'var(--gov-slate-800)',
                    }}
                  >
                    {tab.count}
                  </span>
                )}
              </button>
            ))}
          </div>

          {/* Search Input */}
          <div style={{ position: 'relative', width: '260px' }}>
            <Search
              size={15}
              style={{
                position: 'absolute',
                left: '10px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--gov-slate-400)',
              }}
            />
            <input
              type="text"
              className="form-input"
              style={{ paddingLeft: '32px', fontSize: '12px', height: '34px' }}
              placeholder="Search notifications..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        {/* Notifications List */}
        {loading ? (
          <div style={{ padding: '20px' }}>
            <LoadingSkeleton rows={5} height={68} />
          </div>
        ) : filteredNotifications.length === 0 ? (
          <div style={{ padding: '48px 20px' }}>
            <EmptyState
              title="No Notifications Found"
              description={
                activeFilter === 'UNREAD'
                  ? 'You have read all pending notifications.'
                  : 'No notification records match your selected filter criteria.'
              }
            />
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            {filteredNotifications.map((notif) => (
              <div
                key={notif.id}
                onClick={() => handleOpenAction(notif)}
                style={{
                  padding: '16px 20px',
                  borderBottom: '1px solid var(--border)',
                  backgroundColor: notif.isRead ? 'var(--surface)' : 'rgba(30, 58, 138, 0.035)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                  gap: '16px',
                  cursor: notif.actionUrl ? 'pointer' : 'default',
                  transition: 'background-color 0.15s',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--gov-slate-50)')}
                onMouseLeave={(e) =>
                  (e.currentTarget.style.backgroundColor = notif.isRead
                    ? 'var(--surface)'
                    : 'rgba(30, 58, 138, 0.035)')
                }
              >
                <div style={{ display: 'flex', gap: '14px', flex: 1 }}>
                  <div style={{ marginTop: '2px', flexShrink: 0 }}>
                    {!notif.isRead ? (
                      <div
                        style={{
                          width: '10px',
                          height: '10px',
                          borderRadius: '50%',
                          backgroundColor: 'var(--gov-navy-700)',
                          marginTop: '6px',
                        }}
                        title="Unread notification"
                      />
                    ) : (
                      <div
                        style={{
                          width: '10px',
                          height: '10px',
                          borderRadius: '50%',
                          backgroundColor: 'var(--gov-slate-300)',
                          marginTop: '6px',
                        }}
                      />
                    )}
                  </div>

                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px', flexWrap: 'wrap' }}>
                      <span style={{ fontSize: '14px', fontWeight: notif.isRead ? 600 : 800, color: 'var(--gov-navy-950)' }}>
                        {notif.title}
                      </span>
                      {getTypeBadge(notif.type)}
                    </div>

                    <p style={{ fontSize: '13px', color: 'var(--gov-slate-700)', margin: '0 0 6px 0', lineHeight: 1.45 }}>
                      {notif.message}
                    </p>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px', fontSize: '11px', color: 'var(--text-muted)' }}>
                      <span>Recorded: {formatDateTime(notif.createdAt)}</span>
                      {notif.isRead && notif.readAt && (
                        <span>Read on: {formatDateTime(notif.readAt)}</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Row Actions */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
                  {notif.actionUrl && (
                    <Button
                      variant="outline"
                      size="sm"
                      icon={ExternalLink}
                      onClick={(e) => {
                        e.stopPropagation();
                        handleOpenAction(notif);
                      }}
                    >
                      View Details
                    </Button>
                  )}

                  {!notif.isRead && (
                    <Button
                      variant="secondary"
                      size="sm"
                      icon={CheckCheck}
                      onClick={(e) => handleMarkAsRead(notif.id, e)}
                      loading={processingId === notif.id}
                      title="Mark as read"
                    />
                  )}

                  <Button
                    variant="secondary"
                    size="sm"
                    icon={Trash2}
                    onClick={(e) => handleDelete(notif.id, e)}
                    loading={processingId === notif.id}
                    title="Dismiss notification"
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
