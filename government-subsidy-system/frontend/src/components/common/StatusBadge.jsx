import React from 'react';
import { STATUS_CONFIG } from '../../constants/applicationStatus';

/**
 * Generic status badge component.
 * Accepts a status string and looks up display configuration.
 */
export const StatusBadge = ({ status }) => {
  const config = STATUS_CONFIG[status] || {
    label: status || 'UNKNOWN',
    badgeClass: 'badge-draft',
  };
  return <span className={`badge ${config.badgeClass}`}>{config.label}</span>;
};
