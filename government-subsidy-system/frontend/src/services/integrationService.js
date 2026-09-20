import api from './api';

export const integrationService = {
  testTreasuryTransfer: async ({ accountNumber, ifsc, amount, schemeCode }) => {
    const params = new URLSearchParams({
      accountNumber,
      ifsc,
      amount: String(amount),
      schemeCode,
    });
    const response = await api.post(`/api/integrations/treasury/test-transfer?${params.toString()}`);
    return response.data; // TreasuryDisbursementResult
  },

  verifyIdentity: async (identityNumber) => {
    const response = await api.get(`/api/integrations/beneficiary/verify-identity?identityNumber=${encodeURIComponent(identityNumber)}`);
    return response.data; // IdentityVerificationResponse
  },

  getDigiLockerStatus: async () => {
    const response = await api.get('/api/integrations/digilocker/status');
    return response.data;
  },

  pullDigiLockerDocuments: async (aadhaarNumber, docTypes = ['AADHAAR', 'INCOME_CERTIFICATE', 'LAND_RECORD', 'CASTE_CERTIFICATE']) => {
    const params = new URLSearchParams({
      aadhaarNumber,
      docTypes: docTypes.join(','),
    });
    const response = await api.post(`/api/integrations/digilocker/pull-documents?${params.toString()}`);
    return response.data;
  },
};
