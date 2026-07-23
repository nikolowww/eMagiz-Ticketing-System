/**
 * Customer Management Page
 * Lists all customers and lets support users manage them.
 * Can create new customers and also edit existing ones.
 * Handles password resets and user information updates.
 */

<script setup>
import { onMounted, reactive, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import { useTicketStore } from '../stores/ticketStore'

const store = useTicketStore()
const message = ref('')

const form = reactive({
  id: null,
  name: '',
  email: '',
  username: '',
  password: ''
})

onMounted(() => {
  store.loadCustomers()
})

function clearForm() {
  form.id = null
  form.name = ''
  form.email = ''
  form.username = ''
  form.password = ''
}

function editCustomer(customer) {
  form.id = customer.id
  form.name = customer.name
  form.email = customer.email || ''
  form.username = customer.username
  form.password = ''
}

async function saveCustomer() {
  let result

  // if the form has an id we're editing an existing customer, otherwise it's a new one
  if (form.id) {
    result = await store.updateCustomer(form)
  } else {
    result = await store.createCustomer(form)
  }

  message.value = result.message

  if (result.ok) {
    clearForm()
  }
}
</script>

<template>
  <AppShell title="Customers">
    <section class="content">
      <div class="page-header">
        <div>
          <h1 class="page-title">Customers</h1>
          <p class="page-kicker">Create and edit customer accounts.</p>
        </div>
      </div>

      <div class="dashboard-grid">
        <section class="panel">
          <h2>{{ form.id ? 'Edit customer' : 'Create customer' }}</h2>

          <div class="field">
            <label for="customer-name">Name</label>
            <input id="customer-name" v-model="form.name" />
          </div>

          <div class="field">
            <label for="customer-email">Email</label>
            <input id="customer-email" v-model="form.email" />
          </div>

          <div class="field">
            <label for="customer-username">Username</label>
            <input id="customer-username" v-model="form.username" />
          </div>

          <div class="field">
            <label for="customer-password">Temporary password</label>
            <input id="customer-password" v-model="form.password" type="password" />
          </div>

          <div class="form-actions">
            <button class="btn btn-ghost" @click="clearForm">Clear</button>
            <button class="btn btn-primary" type="button" @click="saveCustomer">Save</button>
          </div>

          <p
              v-if="message"
              class="alert"
              :class="message.includes('created') || message.includes('updated') ? 'alert-success' : 'alert-error'"
          >
            {{ message }}
          </p>
        </section>

        <section class="panel">
          <div class="section-header">
            <div>
              <h2>Customer list</h2>
              <p class="muted">Support can view all customers.</p>
            </div>
          </div>

          <div class="compact-list">
            <button
                v-for="customer in store.state.customers"
                :key="customer.id"
                class="ticket-row-button"
                @click="editCustomer(customer)"
            >
              <span class="ticket-id">CU-{{ customer.id }}</span>
              <span>{{ customer.name }}</span>
              <span class="badge badge-open">{{ customer.username }}</span>
            </button>
          </div>
        </section>
      </div>
    </section>
  </AppShell>
</template>
