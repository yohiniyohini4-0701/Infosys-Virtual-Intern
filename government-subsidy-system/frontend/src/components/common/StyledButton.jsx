import React from 'react';
import { Loader2 } from 'lucide-react';

// Premium styled button using CSS variables for gradient and hover effects
export const StyledButton = ({
  children,
  variant = 'primary', // not used currently, reserved for future extensions
  size = 'md',
  loading = false,
  disabled = false,
  icon: Icon,
  className = '',
  ...props
}) => {
  const sizeClass = size === 'sm' ? 'btn-sm' : size === 'lg' ? 'btn-lg' : '';
  const baseClass = `styled-btn ${sizeClass} ${className}`;

  return (
    <button
      className={baseClass}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? (
        <Loader2 size={16} className="animate-spin" style={{ animation: 'spin 1s linear infinite' }} />
      ) : Icon ? (
        <Icon size={16} />
      ) : null}
      {children}
    </button>
  );
};
