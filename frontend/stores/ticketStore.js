/**
 * Ticket Store is the file with teh central data management for the frontend.
 * Handles all API communication, user state, and ticket data.
 * Uses Vue's reactive state to keep UI in sync with data.
 */

import { computed, reactive } from 'vue'

const API = import.meta.env.VITE_API_URL || 'http://localhost:7000'
const USER_KEY = 'emagiz-current-user'

const savedUser = sessionStorage.getItem(USER_KEY)

// Application state: tracks current page, user data, and loaded lists

const state = reactive({
    currentPage: 'dashboard',
    selectedTicketId: null,
    currentUserData: savedUser ? JSON.parse(savedUser) : null,
    tickets: [],
    comments: [],
    auditLog: [],
    customers: [],
    consultants: []
})

const currentUser = computed(() => state.currentUserData)

function goTo(page, ticketId = null) {
    state.currentPage = page
    state.selectedTicketId = ticketId
}

// attaches the logged-in user's session token so the backend can check who is
// calling; returns an empty object when nobody is logged in yet
function authHeader() {
    const token = currentUser.value?.token
    return token ? { Authorization: `Bearer ${token}` } : {}
}

// the backend returns 401 when our token is missing or no longer valid; when
// that happens we drop the local session so the app falls back to the login screen
function clearSession() {
    state.currentUserData = null
    sessionStorage.removeItem(USER_KEY)
}

/**
 * Fetches JSON data from the backend API.
 * Returns parsed data on success, fallback value on error.
 */

async function getJson(url, fallback) {
    try {
        const response = await fetch(url, {
            headers: authHeader()
        })
        if (response.status === 401) {
            clearSession()
            return fallback
        }
        return await response.json()
    } catch (e) {
        return fallback
    }
}

/**
 * Sends JSON data to the backend API.
 * Supports POST and PUT method.
 * Returns response from server.
 */

async function sendJson(url, method, data) {
    try {
        const response = await fetch(url, {
            method,
            headers: {
                'Content-Type': 'application/json',
                ...authHeader()
            },
            body: JSON.stringify(data)
        })

        if (response.status === 401) {
            clearSession()
        }

        return await response.json()
    } catch (e) {
        return {
            ok: false,
            message: 'Backend connection failed'
        }
    }
}

/**
 * Loads tickets from backend based on user role.
 * Customers see their own tickets, Support/Consultants see assigned ones.
 */

async function loadTickets() {
    if (!currentUser.value) {
        state.tickets = []
        return
    }

    const data = await getJson(
        `${API}/tickets?userId=${currentUser.value.id}`,
        []
    )

    console.log('LOAD TICKETS RESPONSE:', data)
    console.log('TICKET COUNT:', data.length)

    state.tickets = data

    console.log('STATE TICKETS:', state.tickets)
}

async function loadComments(ticketId) {
    if (!currentUser.value || !ticketId) {
        state.comments = []
        return
    }

    state.comments = await getJson(`${API}/tickets/${ticketId}/comments?userId=${currentUser.value.id}`, [])
}

/**
 * Loads audit logs for Support users only.
 * Can search logs by username, action, or details.
 */

async function loadAudit(search = '') {
    if (!canAudit(currentUser.value)) {
        state.auditLog = []
        return
    }

    state.auditLog = await getJson(`${API}/audit?userId=${currentUser.value.id}&search=${encodeURIComponent(search)}`, [])
}

async function loadCustomers() {
    if (!canManageCustomers(currentUser.value)) {
        state.customers = []
        return
    }

    state.customers = await getJson(`${API}/users?userId=${currentUser.value.id}`, [])
}

async function loadConsultants() {
    state.consultants = await getJson(`${API}/consultants`, [])
}

function visibleTickets() {
    return state.tickets
}

function ticketById(id) {
    return state.tickets.find((ticket) => ticket.id === Number(id))
}

// role split: Support sees everything, a Consultant only their assigned tickets,
// and everyone else (Customers) only the tickets they raised themselves
function canViewTicket(user, ticket) {
    if (!user || !ticket) {
        return false
    }

    if (user.role === 'Support') {
        return true
    }

    if (user.role === 'Consultant') {
        return ticket.assigneeId === user.id
    }

    return ticket.requesterUserId === user.id
}

function canCreateTicket(user) {
    return Boolean(user)
}

function canEditTicket(user, ticket) {
    if (!user || !ticket) {
        return false
    }

    if (user.role === 'Support') {
        return true
    }

    return user.role === 'Consultant' && ticket.assigneeId === user.id
}

function canAssign(user) {
    return user?.role === 'Support'
}

function canAudit(user) {
    return user?.role === 'Support'
}

function canManageCustomers(user) {
    return user?.role === 'Support'
}

async function login(username, password) {
    const result = await sendJson(`${API}/login`, 'POST', {
        username,
        password
    })

    if (result.ok) {
        const user = { ...result.user, token: result.token }
        state.currentUserData = user
        sessionStorage.setItem(USER_KEY, JSON.stringify(user))
        state.currentPage = 'dashboard'
        await loadTickets()
        await loadConsultants()
    }

    return result
}

async function logout() {
    if (currentUser.value) {
        await sendJson(`${API}/logout`, 'POST', {
            userId: currentUser.value.id
        })
    }

    state.currentUserData = null
    state.currentPage = 'dashboard'
    state.selectedTicketId = null
    state.tickets = []
    state.comments = []
    sessionStorage.removeItem(USER_KEY)
}

async function createTicket(input) {
    if (!input.title?.trim() || !input.description?.trim()) {
        return {
            ok: false,
            message: 'Title and description required'
        }
    }

    const result = await sendJson(`${API}/tickets`, 'POST', {
        currentUserId: currentUser.value.id,
        title: input.title,
        description: input.description,
        type: input.type || 'Incident',
        source: input.source || 'Customer Portal',
        priority: input.priority || 'Medium',
        requester: input.requester || currentUser.value.organization,
        assigneeId: input.assigneeId || 0, // 0 means unassigned for the backend
        privateNotes: input.privateNotes || '',
        deadline: input.deadline || ''
    })

    if (result.ok) {
        await loadTickets()
    }

    return result
}

async function updateTicket(id, patch) {
    const result = await sendJson(`${API}/tickets/${id}`, 'PUT', {
        currentUserId: currentUser.value.id,
        status: patch.status || '',
        assigneeId: patch.assigneeId || 0,
        privateNotes: patch.privateNotes || '',
        deadline: patch.deadline || ''
    })

    if (result.ok) {
        await loadTickets()
    }

    return result
}

async function addComment(id, text, visibility = 'public') {
    if (!text.trim()) {
        return {
            ok: false,
            message: 'Comment text required'
        }
    }

    const result = await sendJson(`${API}/tickets/${id}/comments`, 'POST', {
        currentUserId: currentUser.value.id,
        text,
        visibility
    })

    if (result.ok) {
        await loadComments(id)
    }

    return result
}

async function createCustomer(form) {
    const result = await sendJson(`${API}/users`, 'POST', {
        currentUserId: currentUser.value.id,
        name: form.name,
        email: form.email,
        username: form.username,
        password: form.password
    })

    if (result.ok) {
        await loadCustomers()
    }

    return result
}

async function updateCustomer(form) {
    const result = await sendJson(`${API}/users/${form.id}`, 'PUT', {
        currentUserId: currentUser.value.id,
        name: form.name,
        email: form.email,
        username: form.username,
        password: form.password
    })

    if (result.ok) {
        await loadCustomers()
    }

    return result
}

function assigneeName(ticket) {
    return state.consultants.find((user) => user.id === ticket.assigneeId)?.name
}

function initials(name) {
    return (name || '')
        .split(' ')
        .map((part) => part[0])
        .join('')
        .slice(0, 2)
        .toUpperCase()
}

function formatDate(value) {
    if (!value) {
        return ''
    }

    return new Intl.DateTimeFormat('en-GB', {
        day: '2-digit',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit'
    }).format(new Date(value))
}

// maps a status or priority to one of our badge colours, anything we
// don't recognise (e.g. the ticket type) just falls back to badge-open
function badgeClass(value) {
    const map = {
        Submitted: 'badge-open',
        Accepted: 'badge-resolved',
        'In Progress': 'badge-progress',
        Resolved: 'badge-resolved',
        Closed: 'badge-resolved',
        Denied: 'badge-critical',
        RFC: 'badge-pending',
        Critical: 'badge-critical',
        High: 'badge-critical',
        Medium: 'badge-pending',
        Low: 'badge-resolved'
    }

    return map[value] || 'badge-open'
}

// works out where a ticket stands against its deadline:
// 'overdue' when the deadline has passed, 'at-risk' when it is due within the
// next 48 hours, 'on-track' otherwise, and 'none' when there is no deadline or
// the ticket is already finished
function slaStatus(ticket) {
    if (!ticket || !ticket.deadline) {
        return 'none'
    }

    if (['Resolved', 'Closed', 'Denied'].includes(ticket.status)) {
        return 'none'
    }

    const now = new Date()
    const due = new Date(ticket.deadline)

    if (due < now) {
        return 'overdue'
    }

    const hoursLeft = (due - now) / (1000 * 60 * 60)
    if (hoursLeft <= 48) {
        return 'at-risk'
    }

    return 'on-track'
}

function isOverdue(ticket) {
    return slaStatus(ticket) === 'overdue'
}

export function useTicketStore() {
    return {
        state,
        currentUser,
        visibleTickets,
        ticketById,
        goTo,
        loadTickets,
        loadComments,
        loadAudit,
        loadCustomers,
        loadConsultants,
        canAudit,
        canAssign,
        canCreateTicket,
        canEditTicket,
        canManageCustomers,
        canViewTicket,
        login,
        logout,
        createTicket,
        updateTicket,
        addComment,
        createCustomer,
        updateCustomer,
        supportUsers: computed(() => state.consultants),
        assigneeName,
        initials,
        formatDate,
        badgeClass,
        isOverdue,
        slaStatus
    }
}
