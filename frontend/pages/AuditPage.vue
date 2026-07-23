/**
 * Audit Log Page
 * Shows history of all actions.
 * Displays who did what, when and details of each action.
 * Includes search functionality to find specific things.
 */

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AppShell from '../components/AppShell.vue'
import { useTicketStore } from '../stores/ticketStore'

const store = useTicketStore()
const search = ref('')
const actionFilter = ref('All')
const roleFilter = ref('All')

onMounted(() => {
  store.loadAudit()
})

// re-query the backend whenever the search box changes; also reset the
// client-side dropdowns so a stale selection can't orphan its <select> or
// hide rows the server just returned
watch(search, () => {
  actionFilter.value = 'All'
  roleFilter.value = 'All'
  store.loadAudit(search.value)
})

const actionOptions = computed(() => ['All', ...new Set(store.state.auditLog.map((entry) => entry.action))])
const roleOptions = computed(() => ['All', ...new Set(store.state.auditLog.map((entry) => entry.role))])

// the search box hits the backend; these two dropdowns filter the loaded rows
const filteredAudit = computed(() => {
  return store.state.auditLog.filter((entry) => {
    const matchesAction = actionFilter.value === 'All' || entry.action === actionFilter.value
    const matchesRole = roleFilter.value === 'All' || entry.role === roleFilter.value
    return matchesAction && matchesRole
  })
})
</script>

<template>
  <AppShell title="Audit trail">
    <section class="content">
      <div class="page-header">
        <div>
          <h1 class="page-title">Audit Trail</h1>
          <p class="page-kicker">User, role, action, and time for important system events.</p>
        </div>
      </div>

      <div class="filter-bar">
        <input v-model="search" class="search-input" placeholder="Search audit events" />
        <select v-model="actionFilter" class="select-input">
          <option v-for="action in actionOptions" :key="action" :value="action">{{ action }}</option>
        </select>
        <select v-model="roleFilter" class="select-input">
          <option v-for="role in roleOptions" :key="role" :value="role">{{ role }}</option>
        </select>
      </div>

      <div class="table-wrap">
        <table class="ticket-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>User</th>
              <th>Role</th>
              <th>Action</th>
              <th>Target</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="entry in filteredAudit" :key="entry.id">
              <td>{{ store.formatDate(entry.at) }}</td>
              <td>{{ entry.user }}</td>
              <td>{{ entry.role }}</td>
              <td>{{ entry.action }}</td>
              <td class="ticket-id">{{ entry.target }}</td>
              <td>{{ entry.details }}</td>
            </tr>
          </tbody>
        </table>

        <p v-if="!filteredAudit.length" class="empty-state">
          No audit events match the current filter.
        </p>
      </div>
    </section>
  </AppShell>
</template>
