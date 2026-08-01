import { defineStore } from 'pinia';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    shopId: localStorage.getItem('shopId') || null,
    shopName: localStorage.getItem('shopName') || null,
    token: localStorage.getItem('token') || null,
    role: localStorage.getItem('role') || null,
    adminId: localStorage.getItem('adminId') || null,
    displayName: localStorage.getItem('displayName') || null,
    requirePasswordChange: localStorage.getItem('requirePasswordChange') === 'true',
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.role === 'admin',
  },
  actions: {
    setAuth(data: { shopId?: any; shopName?: string; adminId?: any; displayName?: string; role?: string; token: string; requirePasswordChange?: boolean }) {
      this.shopId = data.shopId || null;
      this.shopName = data.shopName || null;
      this.token = data.token;
      this.role = data.role || 'merchant';
      this.adminId = data.adminId || null;
      this.displayName = data.displayName || null;
      this.requirePasswordChange = !!data.requirePasswordChange;
      
      if (data.shopId != null) localStorage.setItem('shopId', data.shopId.toString());
      if (data.shopName) localStorage.setItem('shopName', data.shopName);
      localStorage.setItem('token', data.token);
      localStorage.setItem('role', this.role);
      if (data.adminId != null) localStorage.setItem('adminId', data.adminId.toString());
      if (data.displayName) localStorage.setItem('displayName', data.displayName);
      if (data.requirePasswordChange !== undefined) {
        localStorage.setItem('requirePasswordChange', String(data.requirePasswordChange));
      }
    },
    logout() {
      this.shopId = null;
      this.shopName = null;
      this.token = null;
      this.role = null;
      this.adminId = null;
      this.displayName = null;
      this.requirePasswordChange = false;
      localStorage.clear();
    },
  },
});
