<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <img class="admin-brand__logo" src="/favicon.ico" alt="" />
        <div class="admin-brand__copy"><strong>食刻快取</strong><span>运营管理端</span></div>
      </div>
      <div class="admin-menu__caption">工作台</div>
      <el-menu :default-active="route.path" router class="admin-menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <span class="admin-menu__icon"><el-icon><component :is="item.icon" /></el-icon></span>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <div class="admin-sidebar__footer">
        <span class="admin-sidebar__status"></span>
        <span>系统运行正常</span>
      </div>
    </aside>
    <section class="admin-main">
      <header class="admin-topbar">
        <el-dropdown @command="handleCommand">
          <button class="admin-account">
            <span class="admin-account__avatar"><el-icon><User /></el-icon></span>
            <span class="admin-account__copy"><strong>{{ auth.displayName || '管理员' }}</strong><small>超级管理员</small></span>
            <el-icon class="admin-account__arrow"><ArrowDown /></el-icon>
          </button>
          <template #dropdown><el-dropdown-menu><el-dropdown-item command="account">账号设置</el-dropdown-item><el-dropdown-item divided command="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </header>
      <main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="admin-route" mode="out-in"><component :is="Component" /></transition>
        </router-view>
      </main>
    </section>
  </div>
</template>
<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { DataAnalysis, Tickets, Shop, User, ChatLineRound, Setting, UserFilled, ArrowDown, Wallet } from '@element-plus/icons-vue';
const route=useRoute();const router=useRouter();const auth=useAuthStore();
const menus=[
  {path:'/admin/dashboard',label:'经营看板',subtitle:'掌握平台经营数据与运行趋势',icon:DataAnalysis},{path:'/admin/orders',label:'订单管理',subtitle:'查询订单并处理异常状态',icon:Tickets},
  {path:'/admin/shops',label:'商户与食堂',subtitle:'维护商户资料和食堂信息',icon:Shop},{path:'/admin/users',label:'用户管理',subtitle:'查看用户数据并处理违规行为',icon:User},
  {path:'/admin/feedbacks',label:'反馈处理',subtitle:'跟进并回复用户反馈',icon:ChatLineRound},{path:'/admin/config',label:'运营配置',subtitle:'配置预约规则与平台活动',icon:Setting},
  {path:'/admin/billing',label:'商户计费',subtitle:'管理计费方案、月度账单与支付记录',icon:Wallet},
  {path:'/admin/account',label:'管理员账号',subtitle:'维护账号安全与查看操作记录',icon:UserFilled},
];
const handleCommand=(command:string)=>{if(command==='logout'){auth.logout();router.replace('/login')}else router.push('/admin/account')};
</script>
<style scoped>
.admin-topbar{justify-content:flex-end!important}
.admin-shell{height:100vh;min-width:1120px;display:flex;background:#f6f8fb;color:#1e293b;font-size:14px}.admin-sidebar{width:216px;flex:0 0 216px;background:#fff;border-right:1px solid #e8edf5;display:flex;flex-direction:column;box-shadow:4px 0 18px rgba(30,64,175,.025);z-index:2}.admin-brand{height:72px;padding:0 18px;display:flex;align-items:center;gap:12px;border-bottom:1px solid #edf1f7}.admin-brand__logo{width:40px;height:40px;display:block;border-radius:8px;object-fit:cover;box-shadow:0 4px 12px rgba(26,140,255,.16)}.admin-brand__copy strong,.admin-brand__copy span{display:block;letter-spacing:0}.admin-brand__copy strong{font-size:18px;line-height:23px;color:#172033;font-weight:750}.admin-brand__copy span{font-size:11px;line-height:16px;color:#8491a5;margin-top:1px}.admin-menu__caption{padding:19px 20px 7px;font-size:11px;line-height:16px;color:#a0aabd;font-weight:600}.admin-menu{border:0;background:transparent;padding:0 10px;--el-menu-active-color:#1a8cff}.admin-menu :deep(.el-menu-item){position:relative;height:44px;line-height:44px;padding:0 12px!important;color:#526174;border-radius:6px;margin:3px 0;font-size:14px;gap:11px;transition:background-color .2s ease,color .2s ease,transform .2s ease}.admin-menu :deep(.el-menu-item:hover){background:#f2f7ff;color:#1a8cff;transform:translateX(2px)}.admin-menu :deep(.el-menu-item.is-active){background:#eaf4ff;color:#1a8cff;font-weight:650}.admin-menu :deep(.el-menu-item.is-active)::before{content:"";position:absolute;left:-10px;top:10px;width:3px;height:24px;border-radius:0 3px 3px 0;background:#1a8cff}.admin-menu__icon{width:28px;height:28px;display:grid;place-items:center;border-radius:6px;background:#f1f5f9;color:#718096;transition:all .2s ease}.admin-menu :deep(.el-menu-item:hover) .admin-menu__icon,.admin-menu :deep(.el-menu-item.is-active) .admin-menu__icon{background:#dceeff;color:#1a8cff}.admin-menu :deep(.el-icon){font-size:16px}.admin-sidebar__footer{margin:auto 16px 18px;padding:12px 10px;border-top:1px solid #edf1f7;display:flex;align-items:center;gap:8px;color:#7a879a;font-size:12px}.admin-sidebar__status{width:7px;height:7px;border-radius:50%;background:#16b981;box-shadow:0 0 0 4px rgba(22,185,129,.1)}.admin-main{flex:1;min-width:0;display:flex;flex-direction:column}.admin-topbar{height:68px;flex:0 0 68px;background:rgba(255,255,255,.96);border-bottom:1px solid #e8edf5;padding:0 26px;display:flex;align-items:center;justify-content:space-between;box-sizing:border-box}.admin-topbar__title{font-size:17px;line-height:22px;font-weight:700;color:#1e293b}.admin-topbar__subtitle{margin-top:3px;color:#94a0b2;font-size:12px;line-height:16px}.admin-account{min-width:150px;height:46px;padding:4px 9px 4px 5px;border:1px solid transparent;border-radius:7px;background:transparent;display:flex;align-items:center;gap:9px;cursor:pointer;color:#445166;text-align:left;transition:all .2s ease}.admin-account:hover{background:#f7faff;border-color:#e1ebf8}.admin-account__avatar{width:34px;height:34px;display:grid;place-items:center;border-radius:7px;background:#eaf4ff;color:#1a8cff;font-size:17px}.admin-account__copy{display:flex;flex:1;min-width:0;flex-direction:column}.admin-account__copy strong{font-size:13px;line-height:17px;color:#344054;font-weight:650}.admin-account__copy small{font-size:11px;line-height:15px;color:#98a2b3}.admin-account__arrow{font-size:13px;color:#98a2b3}.admin-content{flex:1;overflow:auto}.admin-route-enter-active,.admin-route-leave-active{transition:opacity .18s ease,transform .18s ease}.admin-route-enter-from{opacity:0;transform:translateY(5px)}.admin-route-leave-to{opacity:0;transform:translateY(-3px)}
</style>
