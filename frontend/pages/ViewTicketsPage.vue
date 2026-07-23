/**
 * View Tickets Page
 * Lists all tickets the current user can access based on the role that is given.
 * Users can search, filter, and click on tickets for details.
 */

 <script setup>
import { computed, onMounted, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import { useTicketStore } from '../stores/ticketStore'

const store = useTicketStore()
const search = ref('')
const statusFilter = ref('All')
const typeFilter = ref('All')

onMounted(() => {
  store.loadTickets()
})

const tickets = computed(() => store.visibleTickets())

const filteredTickets = computed(() => {
  const query = search.value.trim().toLowerCase()

  return tickets.value.filter((ticket) => {
    const requester = ticket.requester || ''
    const matchesSearch = !query
        || ticket.title.toLowerCase().includes(query)
        || ticket.description.toLowerCase().includes(query)
        || requester.toLowerCase().includes(query)
        || String(ticket.id).includes(query)
    const matchesStatus = statusFilter.value === 'All' || ticket.status === statusFilter.value
    const matchesType = typeFilter.value === 'All' || ticket.type === typeFilter.value

    return matchesSearch && matchesStatus && matchesType
  })
})

const statusOptions = computed(() => ['All', ...new Set(tickets.value.map((ticket) => ticket.status))])
const typeOptions = computed(() => ['All', ...new Set(tickets.value.map((ticket) => ticket.type))])
const awaitingTriage = computed(() => tickets.value.filter((ticket) => ['Submitted'].includes(ticket.status)).length)
const incidents = computed(() => tickets.value.filter((ticket) => ticket.type === 'Incident').length)
const rfcs = computed(() => tickets.value.filter((ticket) => ticket.type === 'RFC').length)
</script>

<template>
  <AppShell title="Ticket list">
    <section class="content">
      <div class="page-header">
        <div>
          <h1 class="page-title">Tickets</h1>
          <p class="page-kicker" v-if="store.currentUser.value.role === 'Support'">All tickets.</p>
          <p class="page-kicker" v-else-if="store.currentUser.value.role === 'Consultant'">Tickets assigned to you.</p>
          <p class="page-kicker" v-else>Your own tickets.</p>
        </div>

        <button
            v-if="store.canCreateTicket(store.currentUser.value)"
            class="btn btn-orange"
            @click="store.goTo('create-ticket')"
        >
          Create ticket
        </button>
      </div>

      <div class="stats-grid">
        <article class="stat-card">
          <p class="stat-value">{{ tickets.length }}</p>
          <p class="stat-label">Visible tickets</p>
        </article>
        <article class="stat-card">
          <p class="stat-value">{{ awaitingTriage }}</p>
          <p class="stat-label">Awaiting triage</p>
        </article>
        <article class="stat-card">
          <p class="stat-value">{{ incidents }}</p>
          <p class="stat-label">Incidents</p>
        </article>
        <article class="stat-card">
          <p class="stat-value">{{ rfcs }}</p>
          <p class="stat-label">RFCs</p>
        </article>
      </div>

      <div class="filter-bar">
        <input v-model="search" class="search-input" placeholder="Search by ID, title, requester, or description" />
        <select v-model="statusFilter" class="select-input">
          <option v-for="status in statusOptions" :key="status" :value="status">{{ status }}</option>
        </select>
        <select v-model="typeFilter" class="select-input">
          <option v-for="type in typeOptions" :key="type" :value="type">{{ type }}</option>
        </select>
      </div>

      <div class="table-wrap">
        <table class="ticket-table" style="table-layout: fixed">
          <thead>
            <tr>
              <th>Ticket</th>
              <th>Title</th>
              <th>Type</th>
              <th>Source</th>
              <th>Requester</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Assignee</th>
              <th>Deadline</th>
            </tr>
          </thead>

          <tbody v-if="filteredTickets.length">
            <tr
                v-for="ticket in filteredTickets"
                :key="ticket.id"
                class="clickable-row"
                @click="store.goTo('ticket-detail', ticket.id)"
            >
              <td class="ticket-id">EM-{{ ticket.id }}</td>
              <td style="overflow-wrap: anywhere">{{ ticket.title }}</td>
              <td>{{ ticket.type }}</td>
              <td>{{ ticket.source }}</td>
              <td>{{ ticket.requester }}</td>
              <td>
                <span class="badge" :class="store.badgeClass(ticket.status)">{{ ticket.status }}</span>
              </td>
              <td>
                <span class="badge" :class="store.badgeClass(ticket.priority)">{{ ticket.priority }}</span>
              </td>
              <td>{{ store.assigneeName(ticket) || 'Unassigned' }}</td>
              <td>
                <span v-if="store.slaStatus(ticket) === 'overdue'" class="badge badge-critical">Overdue</span>
                <span v-else-if="store.slaStatus(ticket) === 'at-risk'" class="badge badge-pending">At risk</span>
                <template v-else>{{ ticket.deadline ? store.formatDate(ticket.deadline) : '-' }}</template>
              </td>
            </tr>
          </tbody>
        </table>

        <p v-if="!filteredTickets.length" class="empty-state">
          No tickets match the current filter.
        </p>
      </div>
    </section>
  </AppShell>
</template>
