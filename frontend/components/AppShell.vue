/**
 * App Shell
 * Wraps all pages with header, navigationm and footer.
 * Shows user info and logout button in header.
 * Handles responsive design and navigation between pages.
 */

<script setup>
import { useTicketStore } from '../stores/ticketStore'

defineProps({
  title: {
    type: String,
    required: true
  }
})

const store = useTicketStore()

async function logout() {
  await store.logout()
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <img class="brand-logo" src="/emagiz-logo.svg" alt="eMagiz logo" />
        <span class="brand-name">eMagiz</span>
      </div>

      <nav>
        <p class="nav-section">Support</p>
        <!-- these links only show up if the role is allowed to see them -->
        <button class="nav-link" @click="store.goTo('dashboard')">Dashboard</button>
        <button class="nav-link" @click="store.goTo('view-tickets')">Tickets</button>
        <button
            v-if="store.canCreateTicket(store.currentUser.value)"
            class="nav-link"
            type="button"
            @click="store.goTo('create-ticket')"
        >
          Create
        </button>
        <button
            v-if="store.canManageCustomers(store.currentUser.value)"
            class="nav-link"
            @click="store.goTo('customers')"
        >
          Customers
        </button>
        <button
            v-if="store.canAudit(store.currentUser.value)"
            class="nav-link"
            @click="store.goTo('audit')"
        >
          Audit
        </button>
      </nav>
    </aside>

    <main class="main-area">
      <header class="topbar">
        <span class="topbar-title">{{ title }}</span>
        <div class="topbar-user" v-if="store.currentUser.value">
          <span>{{ store.currentUser.value.name }}</span>
          <span class="role-chip">{{ store.currentUser.value.role }}</span>
          <span class="avatar">{{ store.initials(store.currentUser.value.name) }}</span>
          <button class="topbar-button" @click="logout">Sign out</button>
        </div>
      </header>

      <slot />
    </main>
  </div>
</template>
