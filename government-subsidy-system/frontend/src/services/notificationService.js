import api from './api';

export const notificationService = {
  getUserNotifications: async (unreadOnly = false) => {
    const response = await api.get(`/api/notifications?unreadOnly=${unreadOnly}`);
    return response.data || [];
  },

  getUnreadCount: async () => {
    const response = await api.get('/api/notifications/unread-count');
    return response.data || 0;
  },

  markAsRead: async (id) => {
    const response = await api.patch(`/api/notifications/${id}/read`);
    return response.data;
  },

  markAllAsRead: async () => {
    const response = await api.post('/api/notifications/read-all');
    return response.data;
  },

  deleteNotification: async (id) => {
    const response = await api.delete(`/api/notifications/${id}`);
    return response.data;
  },
};
