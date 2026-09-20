import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { notificationService } from '../../services/notificationService';
import { formatDateTime } from '../../utils/formatters';
import {
  Bell,
  CheckCircle2,
  AlertTriangle,
  FileText,
  CreditCard,
  Info,
  CheckCheck,
  ExternalLink,
} from 'lucide-react';

export const NotificationDropdown = () => {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef(null);
  const navigate = useNavigate();

  const fetchUnreadCount = async () => {
    try {
      const count = await notificationService.getUnreadCount();
      setUnreadCount(typeof count === 'number' ? count : 0);
    } catch (err) {
      // Silently fail if not logged in or network error
    }
  };

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const data = await notificationService.getUserNotifications(false);
      setNotifications(Array.isArray(data) ? data.slice(0, 5) : []);
      fetchUnreadCount();
    } catch (err) {
      console.error('Failed to load notifications:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (open) {
      loadNotifications();
    }
  }, [open]);

  // Click outside to close
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleNotificationClick = async (notif) => {
    try {
      if (!notif.isRead) {
        await notificationService.markAsRead(notif.id);
        fetchUnreadCount();
      }
    } catch (err) {
      console.error(err);
    }
    setOpen(false);
    if (notif.actionUrl) {
      navigate(notif.actionUrl);
    } else {
      navigate('/notifications');
    }
  };

  const handleMarkAllRead = async (e) => {
    e.stopPropagation();
    try {
      await notificationService.markAllAsRead();
      setUnreadCount(0);
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    } catch (err) {
      console.error(err);
    }
  };

  const getIcon = (type) => {
    switch (type) {
      case 'DISBURSEMENT_RELEASED':
        return <CreditCard size={16} color="var(--gov-emerald-600)" />;
      case 'ELIGIBILITY_EVALUATION':
      case 'VERIFICATION_UPDATE':
        return <CheckCircle2 size={16} color="#0284c7" />;
      case 'OVERDUE_ALERT':
      case 'MILESTONE_DUE':
        return <AlertTriangle size={16} color="var(--gov-gold-600)" />;
      case 'APPLICATION_STATUS_UPDATE':
        return <FileText size={16} color="var(--gov-navy-700)" />;
      default:
        return <Info size={16} color="var(--gov-slate-600)" />;
    }
  };

  return (
    <div style={{ position: 'relative' }} ref={dropdownRef}>
      <button
        onClick={() => setOpen(!open)}
        style={{
          position: 'relative',
          background: open ? 'var(--gov-slate-100)' : 'none',
          border: '1px solid ' + (open ? 'var(--gov-slate-300)' : 'transparent'),
          borderRadius: 'var(--radius-md)',
          padding: '8px',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--gov-slate-700)',
          transition: 'all 0.2s',
        }}
        title="Notifications"
        aria-label="View notifications"
      >
        <Bell size={20} />
        {unreadCount > 0 && (
          <span
            style={{
              position: 'absolute',
              top: '2px',
              right: '2px',
              background: 'var(--gov-crimson-600)',
              color: '#ffffff',
              fontSize: '10px',
              fontWeight: 800,
              minWidth: '16px',
              height: '16px',
              borderRadius: '99px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '0 4px',
              boxShadow: '0 0 0 2px var(--surface)',
            }}
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div
          style={{
            position: 'absolute',
            right: 0,
            top: 'calc(100% + 8px)',
            width: '360px',
            maxWidth: '90vw',
            backgroundColor: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 'var(--radius-lg)',
            boxShadow: 'var(--shadow-lg)',
            zIndex: 100,
            overflow: 'hidden',
          }}
        >
          {/* Header */}
          <div
            style={{
              padding: '12px 16px',
              borderBottom: '1px solid var(--border)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              backgroundColor: 'var(--gov-slate-50)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontWeight: 800, fontSize: '14px', color: 'var(--gov-navy-950)' }}>
                Notifications
              </span>
              {unreadCount > 0 && (
                <span
                  style={{
                    backgroundColor: 'var(--gov-navy-100)',
                    color: 'var(--gov-navy-800)',
                    fontSize: '11px',
                    fontWeight: 700,
                    padding: '2px 6px',
                    borderRadius: '99px',
                  }}
                >
                  {unreadCount} new
                </span>
              )}
            </div>

            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--gov-navy-700)',
                  fontSize: '11px',
                  fontWeight: 600,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <CheckCheck size={14} />
                Mark all read
              </button>
            )}
          </div>

          {/* List */}
          <div style={{ maxHeight: '340px', overflowY: 'auto' }}>
            {loading ? (
              <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '13px' }}>
                Loading alerts…
              </div>
            ) : notifications.length === 0 ? (
              <div style={{ padding: '32px 20px', textAlign: 'center', color: 'var(--text-muted)' }}>
                <CheckCircle2 size={32} color="var(--gov-emerald-500)" style={{ margin: '0 auto 8px' }} />
                <p style={{ fontWeight: 600, fontSize: '13px', margin: 0 }}>All caught up!</p>
                <p style={{ fontSize: '11px', margin: '4px 0 0' }}>No new notifications at this time.</p>
              </div>
            ) : (
              notifications.map((notif) => (
                <div
                  key={notif.id}
                  onClick={() => handleNotificationClick(notif)}
                  style={{
                    padding: '12px 16px',
                    borderBottom: '1px solid var(--border)',
                    backgroundColor: notif.isRead ? 'var(--surface)' : 'rgba(30, 58, 138, 0.04)',
                    cursor: 'pointer',
                    display: 'flex',
                    gap: '12px',
                    transition: 'background-color 0.15s',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--gov-slate-50)')}
                  onMouseLeave={(e) =>
                    (e.currentTarget.style.backgroundColor = notif.isRead
                      ? 'var(--surface)'
                      : 'rgba(30, 58, 138, 0.04)')
                  }
                >
                  <div style={{ marginTop: '2px', flexShrink: 0 }}>{getIcon(notif.type)}</div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div
                      style={{
                        fontSize: '12px',
                        fontWeight: notif.isRead ? 600 : 800,
                        color: 'var(--gov-navy-950)',
                        marginBottom: '2px',
                      }}
                    >
                      {notif.title}
                    </div>
                    <div
                      style={{
                        fontSize: '11px',
                        color: 'var(--gov-slate-600)',
                        lineHeight: 1.35,
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden',
                      }}
                    >
                      {notif.message}
                    </div>
                    <div style={{ fontSize: '10px', color: 'var(--text-muted)', marginTop: '4px' }}>
                      {formatDateTime(notif.createdAt)}
                    </div>
                  </div>
                  {!notif.isRead && (
                    <div
                      style={{
                        width: '8px',
                        height: '8px',
                        borderRadius: '50%',
                        backgroundColor: 'var(--gov-navy-700)',
                        marginTop: '6px',
                        flexShrink: 0,
                      }}
                    />
                  )}
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          <div
            style={{
              padding: '10px 16px',
              backgroundColor: 'var(--gov-slate-50)',
              borderTop: '1px solid var(--border)',
              textAlign: 'center',
            }}
          >
            <Link
              to="/notifications"
              onClick={() => setOpen(false)}
              style={{
                fontSize: '12px',
                fontWeight: 700,
                color: 'var(--gov-navy-800)',
                textDecoration: 'none',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              Open Notification Center
              <ExternalLink size={13} />
            </Link>
          </div>
        </div>
      )}
    </div>
  );
};
