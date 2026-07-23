/**
 * Ticket Detail Page
 * Shows full ticket information with updates and comments.
 * Displays comments and lets the users add new ones.
 * Consultant can update tickets that are assigned to them.
 */

<script setup>
import { computed, onMounted, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import { useTicketStore } from '../stores/ticketStore'

const store = useTicketStore()
const message = ref('')
const commentText = ref('')
const commentVisibility = ref('public')
const selectedAssignee = ref('')
const deadlineInput = ref('')
const today = new Date()
const minDeadline = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0')

const ticket = computed(() => store.ticketById(store.state.selectedTicketId))
const canEdit = computed(() => store.canEditTicket(store.currentUser.value, ticket.value))
const canSeeInternal = computed(() => ['Support', 'Consultant'].includes(store.currentUser.value.role))
const visibleComments = computed(() => store.state.comments)

onMounted(async () => {
  await store.loadTickets()
  await store.loadConsultants()

  if (!ticket.value || !store.canViewTicket(store.currentUser.value, ticket.value)) {
    store.goTo('view-tickets')
    return
  }

  selectedAssignee.value = ticket.value.assigneeId || ''
  deadlineInput.value = ticket.value.deadline ? ticket.value.deadline.slice(0, 10) : ''
  await store.loadComments(ticket.value.id)
})

async function setStatus(status) {
  const result = await store.updateTicket(ticket.value.id, { status })
  message.value = result.message
}

async function assignTicket() {
  const result = await store.updateTicket(ticket.value.id, { assigneeId: Number(selectedAssignee.value) })
  message.value = result.message
}

async function saveDeadline() {
  const result = await store.updateTicket(ticket.value.id, { deadline: deadlineInput.value })
  message.value = result.message
}

async function addComment() {
  // customers/requesters can't post internal notes, so force public for them
  const visibility = canSeeInternal.value ? commentVisibility.value : 'public'
  const result = await store.addComment(ticket.value.id, commentText.value, visibility)
  message.value = result.message

  if (result.ok) {
    commentText.value = ''
  }
}
</script>

<template>
  <AppShell v-if="ticket" :title="`EM-${ticket.id}`">
    <section class="content">
      <div class="page-header">
        <div>
          <p class="ticket-id">EM-{{ ticket.id }}</p>
          <h1 class="page-title">{{ ticket.title }}</h1>
          <p class="page-kicker">
            {{ ticket.requester }} - {{ ticket.source }} - Updated {{ store.formatDate(ticket.updatedAt) }}
          </p>
        </div>

        <button class="btn btn-secondary" @click="store.goTo('view-tickets')">
          Back to queue
        </button>
      </div>

      <div class="detail-grid">
        <section class="panel detail-main">
          <div class="ticket-meta-row">
            <span class="badge" :class="store.badgeClass(ticket.type)">{{ ticket.type }}</span>
            <span class="badge" :class="store.badgeClass(ticket.status)">{{ ticket.status }}</span>
            <span class="badge" :class="store.badgeClass(ticket.priority)">{{ ticket.priority }}</span>
            <span v-if="store.slaStatus(ticket) === 'overdue'" class="badge badge-critical">Not completed within the time frame</span>
            <span v-else-if="store.slaStatus(ticket) === 'at-risk'" class="badge badge-pending">At SLA risk</span>
          </div>

          <h2>Description</h2>
          <p class="description">{{ ticket.description }}</p>

          <div v-if="canSeeInternal" class="internal-note">
            <h3>Internal notes</h3>
            <p>{{ ticket.privateNotes || 'No internal notes yet.' }}</p>
          </div>

          <h2>Conversation</h2>
          <div class="timeline">
            <article v-for="comment in visibleComments" :key="comment.id" class="timeline-item">
              <div class="timeline-header">
                <strong>{{ comment.author }}</strong>
                <span>{{ store.formatDate(comment.createdAt) }}</span>
                <span class="badge" :class="comment.visibility === 'internal' ? 'badge-pending' : 'badge-open'">
                  {{ comment.visibility }}
                </span>
              </div>
              <p>{{ comment.text }}</p>
            </article>
          </div>

          <div class="comment-box">
            <div class="field">
              <label for="comment">Add comment</label>
              <textarea id="comment" v-model="commentText" rows="4" placeholder="Write an update for this ticket."></textarea>
            </div>
            <div class="form-actions">
              <select v-if="canSeeInternal" v-model="commentVisibility" class="select-input">
                <option value="public">Public</option>
                <option value="internal">Internal</option>
              </select>
              <button class="btn btn-primary" @click="addComment">
                Add comment
              </button>
            </div>
          </div>
        </section>

        <aside class="panel detail-side">
          <h2>Workflow</h2>

          <dl class="detail-list">
            <div>
              <dt>Requester</dt>
              <dd>{{ ticket.requester }}</dd>
            </div>
            <div>
              <dt>Assignee</dt>
              <dd>{{ store.assigneeName(ticket) || 'Unassigned' }}</dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{{ store.formatDate(ticket.createdAt) }}</dd>
            </div>
            <div>
              <dt>Deadline</dt>
              <dd>{{ ticket.deadline ? store.formatDate(ticket.deadline) : 'No deadline' }}</dd>
            </div>
          </dl>

          <div v-if="canEdit" class="workflow-actions">
            <div class="field" v-if="store.canAssign(store.currentUser.value)">
              <label for="assignee">Assign responsible person</label>
              <select id="assignee" v-model="selectedAssignee" class="select-input">
                <option value="">Unassigned</option>
                <option v-for="user in store.supportUsers.value" :key="user.id" :value="user.id">
                  {{ user.name }}
                </option>
              </select>
            </div>

            <button v-if="store.canAssign(store.currentUser.value)" class="btn btn-secondary full-width" @click="assignTicket">
              Save assignee
            </button>

            <div class="field" v-if="store.canAssign(store.currentUser.value)">
              <label for="ticket-deadline">Deadline</label>
              <input id="ticket-deadline" type="date" v-model="deadlineInput" :min="minDeadline" />
            </div>
            <button v-if="store.canAssign(store.currentUser.value)" class="btn btn-secondary full-width" @click="saveDeadline">
              Save deadline
            </button>
            <button v-if="store.canAssign(store.currentUser.value)" class="btn btn-primary full-width" type="button" @click="setStatus('Accepted')">
              Accept
            </button>
            <button class="btn btn-primary full-width" @click="setStatus('In Progress')">
              Start work
            </button>
            <button class="btn btn-primary full-width" @click="setStatus('Resolved')">
              Resolve
            </button>
            <button v-if="store.canAssign(store.currentUser.value)" class="btn btn-danger full-width" @click="setStatus('Denied')">
              Deny
            </button>
          </div>

          <p v-else class="muted">Your role can view this ticket but cannot change workflow fields.</p>

          <p
              v-if="message"
              class="alert"
              :class="message.includes('cannot') || message.includes('not') || message.includes('failed') ? 'alert-error' : 'alert-success'"
          >
            {{ message }}
          </p>
        </aside>
      </div>
    </section>
  </AppShell>
</template>

<style scoped>
.full-width {
  width: 100%;
}
</style>
