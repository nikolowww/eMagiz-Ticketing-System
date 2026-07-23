/**
 * Create Ticket Page
 * Form for submitting new ticket the user created.
 * Lets users enter title, description, type, priority and otherr details.
 * Validate and checks input before sending it to backend.
 */

<script setup>
import { reactive, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import { useTicketStore } from '../stores/ticketStore'

const store = useTicketStore()
const message = ref('')
const today = new Date()
const minDeadline = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0')

const form = reactive({
  title: '',
  description: '',
  type: 'Incident',
  source: store.currentUser.value?.role === 'Customer' ? 'Customer Portal' : 'Internal',
  priority: 'Medium',
  requester: store.currentUser.value?.role === 'Customer' ? (store.currentUser.value?.username || '') : 'eMagiz Team',
  assigneeId: '',
  privateNotes: '',
  deadline: ''
})

async function submit() {
  const result = await store.createTicket(form)
  message.value = result.message

  if (result.ok) {
    store.goTo('ticket-detail', result.ticket.id)
  }
}
</script>

<template>
  <AppShell title="Create ticket">
    <section class="content">
      <div class="page-header">
        <div>
          <h1 class="page-title">Create Ticket</h1>
          <p class="page-kicker">Capture incidents, RFCs, alerts, or internal support actions.</p>
        </div>
      </div>

      <section class="panel create-panel">
        <h2>Ticket details</h2>

        <div class="form-grid">
          <div class="field">
            <label for="ticket-title">Title</label>
            <input id="ticket-title" v-model="form.title" placeholder="Short summary of the request" />
          </div>

          <div class="field">
            <label for="ticket-type">Type</label>
            <select id="ticket-type" v-model="form.type" class="select-input">
              <option>Incident</option>
              <option>RFC</option>
            </select>
            <p class="muted" v-if="store.currentUser.value.role === 'Customer'">
              RFC (Request for Change) is a request to change or add to an existing integration.
            </p>
          </div>

          <div class="field">
            <label for="ticket-source">Source</label>
            <select
                id="ticket-source"
                v-model="form.source"
                class="select-input"
                :disabled="store.currentUser.value.role === 'Customer'"
            >
              <option>Customer Portal</option>
              <option>eMagiz Alert</option>
              <option>Internal</option>
            </select>
            <p class="muted" v-if="store.currentUser.value.role === 'Customer'">
              Internal means the ticket was raised by the eMagiz team, not from the customer portal.
            </p>
          </div>

          <div class="field">
            <label for="ticket-priority">Priority</label>
            <select id="ticket-priority" v-model="form.priority" class="select-input">
              <option>Low</option>
              <option>Medium</option>
              <option>High</option>
              <option>Critical</option>
            </select>
          </div>

          <div class="field">
            <label for="ticket-requester">Requester</label>
            <select
                v-if="store.currentUser.value.role !== 'Customer'"
                id="ticket-requester"
                v-model="form.requester"
                class="select-input"
            >
              <option>eMagiz Team</option>
              <option>Group Platypus TM</option>
              <option>SPAR on Campus</option>
              <option>Inter Actief Ltd</option>
              <option>Commit & Pray B.V.</option>
              <option>Sprint 5 Survivors</option>
            </select>
            <input
                v-else
                id="ticket-requester"
                v-model="form.requester"
                readonly
                placeholder="Customer or internal team"
            />
          </div>

          <div class="field" v-if="store.canAssign(store.currentUser.value)">
            <label for="ticket-assignee">Assignee</label>
            <select id="ticket-assignee" v-model="form.assigneeId" class="select-input">
              <option value="">Unassigned</option>
              <option v-for="user in store.supportUsers.value" :key="user.id" :value="user.id">
                {{ user.name }}
              </option>
            </select>
          </div>

          <div class="field" v-if="store.canAssign(store.currentUser.value)">
            <label for="ticket-deadline">Deadline</label>
            <input id="ticket-deadline" type="date" v-model="form.deadline" :min="minDeadline" />
          </div>
        </div>

        <div class="field">
          <label for="ticket-description">Description</label>
          <textarea id="ticket-description" v-model="form.description" rows="6" placeholder="Describe impact, affected runtime/integration, expected result, and urgency."></textarea>
        </div>

        <div class="field" v-if="store.currentUser.value.role !== 'Customer'">
          <label for="ticket-private-notes">Internal notes</label>
          <textarea id="ticket-private-notes" v-model="form.privateNotes" rows="4" placeholder="Visible only to eMagiz roles."></textarea>
        </div>

        <div class="form-actions">
          <button class="btn btn-ghost" @click="store.goTo('dashboard')">
            Cancel
          </button>
          <button class="btn btn-primary" @click="submit">
            Create ticket
          </button>
        </div>

        <p
            v-if="message"
            class="alert"
            :class="message.includes('successfully') ? 'alert-success' : 'alert-error'"
        >
          {{ message }}
        </p>
      </section>
    </section>
  </AppShell>
</template>

<style scoped>
.create-panel {
  max-width: none;
}

.form-grid {
  align-items: start;
  row-gap: 16px;
  margin-bottom: 16px;
}

.form-grid .field {
  margin-bottom: 0;
}
</style>
