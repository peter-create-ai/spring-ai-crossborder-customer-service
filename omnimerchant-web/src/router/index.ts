import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('@/views/chat/ChatView.vue'),
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      children: [
        { path: '', name: 'Dashboard', component: () => import('@/views/admin/DashboardView.vue') },
        { path: 'tenants', name: 'Tenants', component: () => import('@/views/admin/TenantView.vue') },
        { path: 'knowledge', name: 'Knowledge', component: () => import('@/views/admin/KnowledgeView.vue') },
        { path: 'conversations', name: 'Conversations', component: () => import('@/views/admin/ConversationView.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/chat' },
  ],
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  if (to.meta.public || authStore.isLoggedIn) {
    next()
  } else {
    next('/login')
  }
})

export default router
