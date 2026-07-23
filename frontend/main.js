/**
 * eMagiz Ticketing System Frontend
 * Initializes the Vue application and connects it to the DOM.
 * Loads the main App component and styles.
 */

import { createApp } from 'vue'
import App from './App.vue'
import './styles.css'

createApp(App)
    .mount('#app')
