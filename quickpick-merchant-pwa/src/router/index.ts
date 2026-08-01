import { createRouter, createWebHashHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { hasStaleOrders } from '@/utils/staleOrders';

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/Login.vue'),
    },
    {
      path: '/portfolio',
      component: () => import('@/views/Portfolio.vue'),
      meta: { public: true },
    },
    {
      path: '/change-password',
      component: () => import('@/views/ChangePassword.vue'),
    },
    {
      path: '/order/:id',
      component: () => import('@/views/OrderDetail.vue'),
    },
    {
      path: '/',
      component: () => import('@/views/Layout.vue'),
      children: [
        { path: '', redirect: 'dashboard' },
        { path: 'dashboard', component: () => import('@/views/Dashboard.vue') },
        { path: 'dishes', component: () => import('@/views/DishManagement.vue') },
        { path: 'categories', component: () => import('@/views/CategoryManagement.vue') },
        { path: 'billing', component: () => import('@/views/MerchantBilling.vue') },
        { path: 'profile', component: () => import('@/views/Profile.vue') },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { admin: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        { path: 'dashboard', component: () => import('@/views/admin/AdminDashboard.vue') },
        { path: 'orders', component: () => import('@/views/admin/AdminOrders.vue') },
        { path: 'shops', component: () => import('@/views/admin/AdminShops.vue') },
        { path: 'users', component: () => import('@/views/admin/AdminUsers.vue') },
        { path: 'feedbacks', component: () => import('@/views/admin/AdminFeedbacks.vue') },
        { path: 'config', component: () => import('@/views/admin/AdminConfig.vue') },
        { path: 'billing', component: () => import('@/views/admin/AdminBilling.vue') },
        { path: 'account', component: () => import('@/views/admin/AdminAccount.vue') },
      ],
    },
  ],
});

router.beforeEach(async (to, from, next) => {
  const auth = useAuthStore();

  if (to.meta.public) {
    next();
    return;
  }
  
  if (to.path !== '/login' && !auth.isLoggedIn) {
    next('/login');
    return;
  }

  if (auth.isLoggedIn && auth.requirePasswordChange && to.path !== '/change-password') {
    next('/change-password');
    return;
  }

  if (to.meta.admin && !auth.isAdmin) {
    next('/dashboard');
    return;
  }

  if (auth.isAdmin && !to.meta.admin && to.path !== '/login' && to.path !== '/change-password') {
    next('/admin/dashboard');
    return;
  }

  if (auth.isLoggedIn && !auth.isAdmin && !auth.requirePasswordChange && to.path !== '/login' && to.path !== '/dashboard') {
    try {
      if (await hasStaleOrders()) {
        next('/dashboard');
        return;
      }
    } catch (error) {
      console.error('Failed to check stale orders', error);
      next('/dashboard');
      return;
    }
  }

  next();
});

export default router;
