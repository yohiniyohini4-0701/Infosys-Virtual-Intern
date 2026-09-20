import React, { useState } from 'react';
import { applicationService } from '../../services/applicationService';
import { useToast } from '../../context/ToastContext';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { Upload, FileText } from 'lucide-react';

const DOCUMENT_TYPES = [
  { value: 'IDENTITY_PROOF', label: 'Identity Proof (Aadhaar / PAN)' },
  { value: 'ADDRESS_PROOF', label: 'Address Proof' },
  { value: 'INCOME_CERTIFICATE', label: 'Income Certificate' },
  { value: 'LAND_RECORD', label: 'Land Record / Khasra' },
  { value: 'BANK_PASSBOOK', label: 'Bank Passbook / Statement' },
  { value: 'CASTE_CERTIFICATE', label: 'Caste / Category Certificate' },
  { value: 'SCHEME_SPECIFIC', label: 'Scheme-Specific Document' },
  { value: 'OTHER', label: 'Other Supporting Document' },
];

/**
 * DocumentUploadSection
 *
 * A reusable modal that lets a beneficiary attach a document reference
 * (file name + document type) to an existing application.
 *
 * Props:
 *  - applicationId: string | number  — the application whose documents to update
 *  - isOpen:        boolean           — controls modal visibility
 *  - onClose:       () => void        — called when the modal should close
 *  - onSuccess?:    () => void        — optional callback after a successful upload
 */
const DocumentUploadSection = ({ applicationId, isOpen, onClose, onSuccess }) => {
  const [docType, setDocType] = useState('IDENTITY_PROOF');
  const [fileName, setFileName] = useState('');
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);

  const { success, error } = useToast();

  const resetForm = () => {
    setDocType('IDENTITY_PROOF');
    setFileName('');
    setFile(null);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleFileChange = (e) => {
    const selected = e.target.files[0];
    if (selected) {
      setFile(selected);
      if (!fileName) setFileName(selected.name);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const nameToUse = fileName.trim() || (file && file.name);
    if (!nameToUse) {
      error('Please provide a document file name or select a file.');
      return;
    }

    setUploading(true);
    try {
      if (file) {
        // Real file upload via multipart
        await applicationService.uploadDocument(applicationId, docType, file);
      } else {
        // Reference-only attachment (file name path)
        await applicationService.attachDocument(
          applicationId,
          docType,
          nameToUse,
          'application/pdf',
          `uploads/documents/${nameToUse}`
        );
      }
      success('Document attached to your application successfully.');
      resetForm();
      onClose();
      if (onSuccess) onSuccess();
    } catch (err) {
      error(err?.message || 'Document attachment failed. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title="Upload Supporting Document"
      size="md"
    >
      <form onSubmit={handleSubmit}>
        {/* Info banner */}
        <div
          style={{
            background: '#f0f9ff',
            border: '1px solid #bae6fd',
            borderRadius: 'var(--radius-md)',
            padding: '12px 14px',
            marginBottom: '18px',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '10px',
            fontSize: '12px',
            color: '#0c4a6e',
          }}
        >
          <FileText size={16} style={{ flexShrink: 0, marginTop: '1px' }} />
          <span>
            Upload identity, income, land or any scheme-specific document to support your
            application. Accepted formats: PDF, JPG, PNG. Max 10 MB.
          </span>
        </div>

        {/* Document Type */}
        <div className="form-group">
          <label className="form-label">
            Document Type <span className="required">*</span>
          </label>
          <select
            className="form-input"
            value={docType}
            onChange={(e) => setDocType(e.target.value)}
            required
          >
            {DOCUMENT_TYPES.map((dt) => (
              <option key={dt.value} value={dt.value}>
                {dt.label}
              </option>
            ))}
          </select>
        </div>

        {/* File Picker */}
        <div className="form-group">
          <label className="form-label">Select File</label>
          <input
            type="file"
            accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
            onChange={handleFileChange}
            style={{
              width: '100%',
              padding: '8px 10px',
              border: '1.5px dashed var(--border)',
              borderRadius: 'var(--radius-md)',
              fontSize: '13px',
              cursor: 'pointer',
              backgroundColor: 'var(--gov-slate-50)',
            }}
          />
          <p style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' }}>
            Or provide a file name reference below if uploading via other means.
          </p>
        </div>

        {/* File Name Reference */}
        <div className="form-group">
          <label className="form-label">
            Document File Name <span className="required">*</span>
          </label>
          <input
            type="text"
            className="form-input"
            placeholder="e.g. aadhaar_card.pdf"
            value={fileName}
            onChange={(e) => setFileName(e.target.value)}
          />
        </div>

        {/* Action Buttons */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '10px',
            marginTop: '20px',
          }}
        >
          <Button type="button" variant="secondary" onClick={handleClose} disabled={uploading}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" icon={Upload} loading={uploading}>
            {uploading ? 'Uploading…' : 'Attach Document'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default DocumentUploadSection;
