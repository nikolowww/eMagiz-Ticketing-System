/**
 * Login Page
 * This allows users to sign in with username and password.
 * Shows success or error message after login try.
 */

<script setup>

import { ref } from 'vue'
import { useTicketStore } from '../stores/ticketStore'

const store = useTicketStore()

const username = ref('')
const password = ref('')
const message = ref('')

async function login() {
  const result = await store.login(username.value, password.value)
  message.value = result.message
}

</script>

<template>
  <div class="login-page">
    <section class="login-brand">
      <div>
        <img class="login-logo" src="/emagiz-logo.svg" alt="eMagiz logo" />
        <h1>eMagiz Ticketing System</h1>
        <p>
          A focused enterprise support workspace for integration requests,
          incidents, and team follow-up.
        </p>
      </div>
    </section>

    <main class="login-form-wrap">
      <section class="login-card">
        <h2 id="login-title">Sign in</h2>
        <p class="muted">Use your support workspace account to continue.</p>

        <div class="field">
          <label for="username">Username</label>
          <input id="username" v-model="username" autocomplete="username" placeholder="Enter username" />
        </div>

        <div class="field">
          <label for="password">Password</label>
          <input id="password" v-model="password" autocomplete="current-password" type="password" placeholder="Enter password" />
        </div>

        <button class="btn btn-orange full-width" @click="login">
          Login
        </button>

        <p
            v-if="message"
            class="alert"
            :class="message.includes('successful') ? 'alert-success' : 'alert-error'"
        >
          {{ message }}
        </p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.full-width {
  width: 100%;
  margin-top: 12px;
}
</style>
