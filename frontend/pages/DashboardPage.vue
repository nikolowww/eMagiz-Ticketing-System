/**
 * Dashboard Page
 * Shows main menu with the options to click for different actions.
 * Displays different options based on the user role: support, consultant, customer.
 */

<script setup>
import { computed, onMounted } from 'vue'
import AppShell from '../components/AppShell.vue'
import { useTicketStore } from '../stores/ticketStore'

const store = useTicketStore()

const tickets = computed(() => store.visibleTickets())
const awaitingTriage = computed(() => tickets.value.filter((ticket) => ['New', 'Submitted'].includes(ticket.status)).length)
const activeTickets = computed(() => tickets.value.filter((ticket) => ['Accepted', 'In Progress'].includes(ticket.status)).length)
const criticalTickets = computed(() => tickets.value.filter((ticket) => ticket.priority === 'Critical').length)
const slaRisk = computed(() => tickets.value.filter((ticket) => ['overdue', 'at-risk'].includes(store.slaStatus(ticket))).length)
const recentTickets = computed(() => tickets.value.slice(0, 5))
const recentAudit = computed(() => store.state.auditLog.slice(0, 4))

onMounted(async () => {
  await store.loadTickets()
  // only support/consultant roles can see the audit trail
  if (store.canAudit(store.currentUser.value)) {
    await store.loadAudit()
  }
})
</script>

<template>
  <AppShell title="Ticketing workspace">
    <section class="content">
      <div class="page-header">
        <div>
          <h1 class="page-title">Dashboard</h1>
          <p class="page-kicker">
            {{ store.currentUser.value.organization }} - {{ store.currentUser.value.role }} workspace
          </p>
        </div>

        <div class="header-actions">
          <button
              v-if="store.canCreateTicket(store.currentUser.value)"
              class="btn btn-orange"
              @click="store.goTo('create-ticket')"
          >
            Create ticket
          </button>
          <button
              v-if="store.currentUser.value.role === 'Customer'"
              class="btn btn-orange"
              @click="store.goTo('my-account')"
          >
            My Account
          </button>
        </div>
      </div>

      <div class="stats-grid stats-grid-dashboard">
        <article class="stat-card">
          <p class="stat-value">{{ tickets.length }}</p>
          <p class="stat-label">Visible tickets</p>
        </article>
        <article class="stat-card">
          <p class="stat-value">{{ awaitingTriage }}</p>
          <p class="stat-label">Awaiting triage</p>
        </article>
        <article class="stat-card">
          <p class="stat-value">{{ activeTickets }}</p>
          <p class="stat-label">Accepted or active</p>
        </article>
        <article class="stat-card">
          <p class="stat-value">{{ criticalTickets }}</p>
          <p class="stat-label">Critical priority</p>
        </article>
        <article class="stat-card">
          <p class="stat-value">{{ slaRisk }}</p>
          <p class="stat-label">At SLA risk</p>
        </article>
      </div>

      <div class="dashboard-grid">
        <section class="panel">
          <div class="section-header">
            <div>
              <h2>Recent tickets</h2>
              <p class="muted">Latest visible support work.</p>
            </div>
            <button class="btn btn-orange" @click="store.goTo('view-tickets')">
              View queue
            </button>
          </div>

          <div class="compact-list">
            <button
                v-for="ticket in recentTickets"
                :key="ticket.id"
                class="ticket-row-button"
                @click="store.goTo('ticket-detail', ticket.id)"
            >
              <span class="ticket-id">EM-{{ ticket.id }}</span>
              <span>{{ ticket.title }}</span>
              <span class="badge" :class="store.badgeClass(ticket.status)">{{ ticket.status }}</span>
            </button>
          </div>
        </section>

        <section v-if="store.canAudit(store.currentUser.value)" class="panel">
          <div class="section-header">
            <div>
              <h2>Audit trail</h2>
              <p class="muted">Every important action records user and context.</p>
            </div>
            <button class="btn btn-orange" type="button" @click="store.goTo('audit')">
              Open audit
            </button>
          </div>

          <div class="audit-list">
            <article v-for="entry in recentAudit" :key="entry.id" class="audit-item">
              <strong>{{ entry.action }}</strong>
              <span>{{ entry.user }} - {{ store.formatDate(entry.at) }}</span>
              <p>{{ entry.details }}</p>
            </article>
          </div>
        </section>

        <section v-else class="panel">
          <div class="section-header">
            <div>
              <h2>Your workspace</h2>
              <p class="muted">Available actions for your current role.</p>
            </div>
          </div>

          <div class="compact-list">
            <button class="ticket-row-button" @click="store.goTo('create-ticket')">
              <span class="ticket-id">01</span>
              <span>Create an incident or RFC</span>
              <span class="badge badge-open">Allowed</span>
            </button>
            <button class="ticket-row-button" @click="store.goTo('view-tickets')">
              <span class="ticket-id">02</span>
              <span>Track visible tickets</span>
              <span class="badge badge-open">Allowed</span>
            </button>
          </div>
        </section>
      </div>
    </section>
  </AppShell>
</template>
