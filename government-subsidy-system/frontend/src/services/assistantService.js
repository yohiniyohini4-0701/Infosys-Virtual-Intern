import api from './api';

export const assistantService = {
  /**
   * Query the Smart Information Assistant (deterministic, rule & database backed)
   * @param {string} query 
   * @returns {Promise<any>}
   */
  queryAssistant: async (query) => {
    const response = await api.post('/api/assistant/query', { query });
    return response.data;
  },

  /**
   * Fetch personalized scheme recommendations for the current beneficiary
   * @returns {Promise<any>}
   */
  getRecommendations: async () => {
    const response = await api.get('/api/schemes/recommendations');
    return response.data;
  },

  /**
   * Pre-application eligibility preview for a scheme without applying
   * @param {number|string} schemeId 
   * @returns {Promise<any>}
   */
  previewEligibility: async (schemeId) => {
    const response = await api.get(`/api/schemes/${schemeId}/eligibility-preview`);
    return response.data;
  },
};
