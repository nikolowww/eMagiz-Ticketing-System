<template>
  <LoginPage v-if="!store.currentUser.value" />
  <DashboardPage v-else-if="store.state.currentPage === 'dashboard'" />
  <CreateTicketPage v-else-if="store.state.currentPage === 'create-ticket'" />
  <ViewTicketsPage v-else-if="store.state.currentPage === 'view-tickets'" />
  <TicketDetailPage v-else-if="store.state.currentPage === 'ticket-detail'" />
  <MyAccountPage v-else-if="store.state.currentPage === 'my-account' && store.currentUser.value.role === 'Customer'" />
  <AuditPage v-else-if="store.state.currentPage === 'audit' && store.canAudit(store.currentUser.value)" />
  <CustomersPage v-else-if="store.state.currentPage === 'customers' && store.canManageCustomers(store.currentUser.value)" />
  <DashboardPage v-else />
</template>

<script setup>
import LoginPage from './pages/LoginPage.vue'
import DashboardPage from './pages/DashboardPage.vue'
import CreateTicketPage from './pages/CreateTicketPage.vue'
import ViewTicketsPage from './pages/ViewTicketsPage.vue'
import TicketDetailPage from './pages/TicketDetailPage.vue'
import MyAccountPage from './pages/MyAccountPage.vue'
import AuditPage from './pages/AuditPage.vue'
import CustomersPage from './pages/CustomersPage.vue'
import { useTicketStore } from './stores/ticketStore'

const store = useTicketStore()
</script>
