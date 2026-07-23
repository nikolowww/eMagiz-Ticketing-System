<script setup>
import { computed, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import { useTicketStore } from '../stores/ticketStore'

const store = useTicketStore()
const newPassword = ref('')
const confirmPassword = ref('')
const message = ref('')

const user = computed(() => store.currentUser.value)
const passwordsDoNotMatch = computed(() => confirmPassword.value && newPassword.value !== confirmPassword.value)

async function savePassword() {
  if (!newPassword.value) {
    message.value = 'Enter a new password'
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    message.value = 'Passwords do not match'
    return
  }

  const result = await store.updateCustomer({
    id: user.value.id,
    name: user.value.name,
    email: user.value.email,
    username: user.value.username,
    password: newPassword.value
  })

  message.value = result.message

  if (result.ok) {
    newPassword.value = ''
    confirmPassword.value = ''
  }
}
</script>

<template>
  <AppShell title="My Account">
    <section class="content">
      <div class="page-header">
        <div>
          <h1 class="page-title">My Account</h1>
          <p class="page-kicker">Change your password.</p>
        </div>
      </div>

      <section class="panel">
        <div class="field">
          <label for="account-name">Name</label>
          <input id="account-name" :value="user.name" readonly />
        </div>

        <div class="field">
          <label for="account-username">Username</label>
          <input id="account-username" :value="user.username" readonly />
        </div>

        <div class="field">
          <label for="new-password">New Password</label>
          <input id="new-password" v-model="newPassword" type="password" />
        </div>

        <div class="field">
          <label for="confirm-password">Confirm New Password</label>
          <input id="confirm-password" v-model="confirmPassword" type="password" />
        </div>

        <p v-if="passwordsDoNotMatch" class="alert alert-error">Passwords do not match</p>

        <div class="form-actions">
          <button
              class="btn btn-primary"
              type="button"
              :disabled="!newPassword || passwordsDoNotMatch"
              @click="savePassword"
          >
            Save
          </button>
        </div>

        <p
            v-if="message"
            class="alert"
            :class="message.includes('updated') ? 'alert-success' : 'alert-error'"
        >
          {{ message }}
        </p>
      </section>
    </section>
  </AppShell>
</template>
