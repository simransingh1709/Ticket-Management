/**
 * Ticket Management System (TMS) - Frontend Application
 * Designed & Built by Simran
 */

(function () {
  'use strict';

  // API Base Endpoints
  const API_BASE = '/api/v1';
  const API_TICKETS = `${API_BASE}/tickets`;

  // App State
  const state = {
    tickets: [],
    currentPage: 0,
    pageSize: 10,
    totalPages: 1,
    totalElements: 0,
    statusFilter: '',
    searchKeyword: '',
    currentTicket: null,
    comments: [],
    isLoading: false,
  };

  // DOM Elements
  const elements = {
    // Stat counters
    valTotal: document.getElementById('valTotal'),
    valOpen: document.getElementById('valOpen'),
    valProgress: document.getElementById('valProgress'),
    valResolved: document.getElementById('valResolved'),

    // Toolbar
    searchInput: document.getElementById('searchInput'),
    clearSearchBtn: document.getElementById('clearSearchBtn'),
    statusFilter: document.getElementById('statusFilter'),
    pageSizeSelect: document.getElementById('pageSizeSelect'),
    btnRefresh: document.getElementById('btnRefresh'),
    btnQuickSeed: document.getElementById('btnQuickSeed'),

    // Grid & States
    ticketsGrid: document.getElementById('ticketsGrid'),
    loadingState: document.getElementById('loadingState'),
    emptyState: document.getElementById('emptyState'),
    emptyTitle: document.getElementById('emptyTitle'),
    emptyDesc: document.getElementById('emptyDesc'),
    ticketCountBadge: document.getElementById('ticketCountBadge'),

    // Pagination
    paginationBar: document.getElementById('paginationBar'),
    paginationInfo: document.getElementById('paginationInfo'),
    pageIndicator: document.getElementById('pageIndicator'),
    btnPrevPage: document.getElementById('btnPrevPage'),
    btnNextPage: document.getElementById('btnNextPage'),

    // Create Modal
    createModal: document.getElementById('createModal'),
    btnNewTicketNav: document.getElementById('btnNewTicketNav'),
    btnEmptyCreate: document.getElementById('btnEmptyCreate'),
    closeCreateModalBtn: document.getElementById('closeCreateModalBtn'),
    cancelCreateBtn: document.getElementById('cancelCreateBtn'),
    createTicketForm: document.getElementById('createTicketForm'),
    createTitle: document.getElementById('createTitle'),
    createTitleCount: document.getElementById('createTitleCount'),
    createAssignee: document.getElementById('createAssignee'),
    createDescription: document.getElementById('createDescription'),
    submitCreateBtn: document.getElementById('submitCreateBtn'),

    // Detail Modal
    detailModal: document.getElementById('detailModal'),
    closeDetailModalBtn: document.getElementById('closeDetailModalBtn'),
    detailId: document.getElementById('detailId'),
    detailStatusBadge: document.getElementById('detailStatusBadge'),
    detailTitle: document.getElementById('detailTitle'),
    detailAssignee: document.getElementById('detailAssignee'),
    detailCreatedAt: document.getElementById('detailCreatedAt'),
    detailUpdatedAt: document.getElementById('detailUpdatedAt'),
    detailDescription: document.getElementById('detailDescription'),
    transitionActions: document.getElementById('transitionActions'),
    lifecycleRuleText: document.getElementById('lifecycleRuleText'),
    btnToggleEdit: document.getElementById('btnToggleEdit'),
    btnEditLabel: document.getElementById('btnEditLabel'),
    detailViewMode: document.getElementById('detailViewMode'),
    editTicketForm: document.getElementById('editTicketForm'),
    editTitle: document.getElementById('editTitle'),
    editTitleCount: document.getElementById('editTitleCount'),
    editAssignee: document.getElementById('editAssignee'),
    editDescription: document.getElementById('editDescription'),
    cancelEditBtn: document.getElementById('cancelEditBtn'),

    // Comments
    commentCountBadge: document.getElementById('commentCountBadge'),
    commentsList: document.getElementById('commentsList'),
    addCommentForm: document.getElementById('addCommentForm'),
    commentAuthor: document.getElementById('commentAuthor'),
    commentContent: document.getElementById('commentContent'),
    commentCharCount: document.getElementById('commentCharCount'),
    btnSubmitComment: document.getElementById('btnSubmitComment'),

    // Toasts
    toastContainer: document.getElementById('toastContainer'),
  };

  // State Transition Rules (aligned strictly with TicketStatus.java)
  const ALLOWED_TRANSITIONS = {
    OPEN: [
      { target: 'IN_PROGRESS', label: 'Start Progress', className: 'to-progress', icon: '⚡' },
      { target: 'CANCELLED', label: 'Cancel Ticket', className: 'to-cancelled', icon: '✕' },
    ],
    IN_PROGRESS: [
      { target: 'RESOLVED', label: 'Mark Resolved', className: 'to-resolved', icon: '✓' },
      { target: 'CANCELLED', label: 'Cancel Ticket', className: 'to-cancelled', icon: '✕' },
    ],
    RESOLVED: [
      { target: 'CLOSED', label: 'Close Ticket', className: 'to-closed', icon: '🔒' },
    ],
    CLOSED: [],
    CANCELLED: [],
  };

  /**
   * Initialize App
   */
  function init() {
    bindEvents();
    loadTickets();
    loadOverallStats();
  }

  /**
   * Bind event listeners
   */
  function bindEvents() {
    // Search with debounce
    let debounceTimer;
    elements.searchInput.addEventListener('input', (e) => {
      const val = e.target.value.trim();
      elements.clearSearchBtn.style.display = val ? 'block' : 'none';
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
        state.searchKeyword = val;
        state.currentPage = 0;
        loadTickets();
      }, 350);
    });

    elements.clearSearchBtn.addEventListener('click', () => {
      elements.searchInput.value = '';
      elements.clearSearchBtn.style.display = 'none';
      state.searchKeyword = '';
      state.currentPage = 0;
      loadTickets();
    });

    // Status filter
    elements.statusFilter.addEventListener('change', (e) => {
      state.statusFilter = e.target.value;
      state.currentPage = 0;
      loadTickets();
    });

    // Page size
    elements.pageSizeSelect.addEventListener('change', (e) => {
      state.pageSize = parseInt(e.target.value, 10);
      state.currentPage = 0;
      loadTickets();
    });

    // Refresh button
    elements.btnRefresh.addEventListener('click', () => {
      const icon = elements.btnRefresh.querySelector('.spin-icon');
      if (icon) icon.classList.add('spinning');
      Promise.all([loadTickets(), loadOverallStats()]).finally(() => {
        if (icon) icon.classList.remove('spinning');
      });
    });

    // Quick demo seed button
    elements.btnQuickSeed.addEventListener('click', handleSeedData);

    // Pagination
    elements.btnPrevPage.addEventListener('click', () => {
      if (state.currentPage > 0) {
        state.currentPage--;
        loadTickets();
      }
    });

    elements.btnNextPage.addEventListener('click', () => {
      if (state.currentPage < state.totalPages - 1) {
        state.currentPage++;
        loadTickets();
      }
    });

    // Create Modal triggers
    elements.btnNewTicketNav.addEventListener('click', openCreateModal);
    elements.btnEmptyCreate.addEventListener('click', openCreateModal);
    elements.closeCreateModalBtn.addEventListener('click', closeCreateModal);
    elements.cancelCreateBtn.addEventListener('click', closeCreateModal);
    elements.createModal.addEventListener('click', (e) => {
      if (e.target === elements.createModal) closeCreateModal();
    });

    // Character counter for Create Title
    elements.createTitle.addEventListener('input', () => {
      elements.createTitleCount.textContent = `${elements.createTitle.value.length}/150`;
    });

    // Create Ticket Submit
    elements.createTicketForm.addEventListener('submit', handleCreateTicket);

    // Detail Modal Close
    elements.closeDetailModalBtn.addEventListener('click', closeDetailModal);
    elements.detailModal.addEventListener('click', (e) => {
      if (e.target === elements.detailModal) closeDetailModal();
    });

    // Edit Ticket Toggle
    elements.btnToggleEdit.addEventListener('click', toggleEditMode);
    elements.cancelEditBtn.addEventListener('click', () => toggleEditMode(false));
    elements.editTitle.addEventListener('input', () => {
      elements.editTitleCount.textContent = `${elements.editTitle.value.length}/150`;
    });
    elements.editTicketForm.addEventListener('submit', handleSaveEditTicket);

    // Add Comment Submit
    elements.commentContent.addEventListener('input', () => {
      elements.commentCharCount.textContent = `${elements.commentContent.value.length}/1000`;
    });
    elements.addCommentForm.addEventListener('submit', handleAddComment);

    // Global ESC key to close modals
    window.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        if (elements.createModal.style.display !== 'none') closeCreateModal();
        if (elements.detailModal.style.display !== 'none') closeDetailModal();
      }
    });
  }

  /**
   * Fetch tickets from backend API
   */
  async function loadTickets() {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append('page', state.currentPage);
      params.append('size', state.pageSize);
      if (state.statusFilter) params.append('status', state.statusFilter);
      if (state.searchKeyword) params.append('keyword', state.searchKeyword);

      const response = await fetch(`${API_TICKETS}?${params.toString()}`);
      if (!response.ok) {
        throw new Error(await extractErrorMessage(response, 'Failed to fetch tickets'));
      }

      const data = await response.json();
      state.tickets = data.content || [];
      state.totalPages = data.totalPages || 1;
      state.totalElements = data.totalElements || 0;
      state.currentPage = data.pageNumber || 0;

      renderTicketsList();
      renderPagination();
      updateTicketCountBadge();
    } catch (err) {
      showToast(err.message, 'error');
      renderEmptyState('Connection Error', err.message);
    } finally {
      setLoading(false);
    }
  }

  /**
   * Load overall stats for the dashboard counters
   */
  async function loadOverallStats() {
    try {
      // Query overview counts by status
      const [allRes, openRes, progRes, resRes] = await Promise.all([
        fetch(`${API_TICKETS}?page=0&size=1`),
        fetch(`${API_TICKETS}?status=OPEN&page=0&size=1`),
        fetch(`${API_TICKETS}?status=IN_PROGRESS&page=0&size=1`),
        fetch(`${API_TICKETS}?status=RESOLVED&page=0&size=1`),
      ]);

      if (allRes.ok) {
        const d = await allRes.json();
        elements.valTotal.textContent = d.totalElements || 0;
      }
      if (openRes.ok) {
        const d = await openRes.json();
        elements.valOpen.textContent = d.totalElements || 0;
      }
      if (progRes.ok) {
        const d = await progRes.json();
        elements.valProgress.textContent = d.totalElements || 0;
      }
      if (resRes.ok) {
        const d = await resRes.json();
        elements.valResolved.textContent = d.totalElements || 0;
      }
    } catch (e) {
      console.warn('Could not update live stat cards', e);
    }
  }

  /**
   * Render Tickets Grid
   */
  function renderTicketsList() {
    if (state.tickets.length === 0) {
      const isFiltered = Boolean(state.statusFilter || state.searchKeyword);
      renderEmptyState(
        isFiltered ? 'No matching tickets' : 'No tickets in the system',
        isFiltered
          ? 'Try adjusting or clearing your search filter.'
          : 'Be the first to create an issue ticket in this project.'
      );
      return;
    }

    elements.emptyState.style.display = 'none';
    elements.ticketsGrid.style.display = 'grid';
    elements.ticketsGrid.innerHTML = '';

    state.tickets.forEach((ticket) => {
      const card = document.createElement('div');
      card.className = 'ticket-card';
      card.setAttribute('data-id', ticket.id);

      const status = ticket.status || 'OPEN';
      const formattedDate = formatDate(ticket.createdAt);
      const assigneeText = ticket.assigneeId ? escapeHtml(ticket.assigneeId) : 'Unassigned';

      card.innerHTML = `
        <div class="ticket-card-header">
          <span class="ticket-id">#${ticket.id}</span>
          <span class="status-badge ${status}">${formatStatus(status)}</span>
        </div>
        <h3 class="ticket-card-title">${escapeHtml(ticket.title)}</h3>
        <p class="ticket-card-desc">${escapeHtml(ticket.description || '')}</p>
        <div class="ticket-card-footer">
          <span class="assignee-pill">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            ${assigneeText}
          </span>
          <span class="ticket-date">${formattedDate}</span>
        </div>
      `;

      card.addEventListener('click', () => openDetailModal(ticket.id));
      elements.ticketsGrid.appendChild(card);
    });
  }

  /**
   * Render empty state message
   */
  function renderEmptyState(title, desc) {
    elements.ticketsGrid.style.display = 'none';
    elements.emptyState.style.display = 'flex';
    elements.emptyTitle.textContent = title;
    elements.emptyDesc.textContent = desc;
  }

  /**
   * Render pagination controls
   */
  function renderPagination() {
    if (state.totalElements === 0) {
      elements.paginationBar.style.display = 'none';
      return;
    }

    elements.paginationBar.style.display = 'flex';
    const start = state.currentPage * state.pageSize + 1;
    const end = Math.min((state.currentPage + 1) * state.pageSize, state.totalElements);

    elements.paginationInfo.textContent = `Showing ${start} to ${end} of ${state.totalElements} tickets`;
    elements.pageIndicator.textContent = `Page ${state.currentPage + 1} of ${Math.max(state.totalPages, 1)}`;

    elements.btnPrevPage.disabled = state.currentPage <= 0;
    elements.btnNextPage.disabled = state.currentPage >= state.totalPages - 1;
  }

  function updateTicketCountBadge() {
    elements.ticketCountBadge.textContent = state.totalElements;
  }

  function setLoading(loading) {
    state.isLoading = loading;
    if (loading) {
      elements.loadingState.style.display = 'flex';
      elements.ticketsGrid.style.display = 'none';
      elements.emptyState.style.display = 'none';
    } else {
      elements.loadingState.style.display = 'none';
    }
  }

  /**
   * Modal: Create Ticket
   */
  function openCreateModal() {
    elements.createTicketForm.reset();
    elements.createTitleCount.textContent = '0/150';
    elements.createModal.style.display = 'flex';
    setTimeout(() => elements.createTitle.focus(), 50);
  }

  function closeCreateModal() {
    elements.createModal.style.display = 'none';
  }

  async function handleCreateTicket(e) {
    e.preventDefault();
    const title = elements.createTitle.value.trim();
    const description = elements.createDescription.value.trim();
    const assigneeId = elements.createAssignee.value.trim() || null;

    if (title.length < 5 || title.length > 150) {
      showToast('Title must be between 5 and 150 characters long', 'error');
      return;
    }
    if (!description) {
      showToast('Description is required', 'error');
      return;
    }

    elements.submitCreateBtn.disabled = true;
    elements.submitCreateBtn.innerHTML = `<span>Creating...</span>`;

    try {
      const response = await fetch(API_TICKETS, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, description, assigneeId }),
      });

      if (!response.ok) {
        throw new Error(await extractErrorMessage(response, 'Failed to create ticket'));
      }

      const created = await response.json();
      showToast(`Ticket #${created.id} created successfully!`, 'success');
      closeCreateModal();
      loadTickets();
      loadOverallStats();
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      elements.submitCreateBtn.disabled = false;
      elements.submitCreateBtn.innerHTML = `<span>Create Ticket</span>`;
    }
  }

  /**
   * Modal: View / Edit Ticket Details & Comments
   */
  async function openDetailModal(ticketId) {
    toggleEditMode(false);
    elements.detailModal.style.display = 'flex';
    elements.detailId.textContent = `#${ticketId}`;
    elements.detailTitle.textContent = 'Loading ticket details...';
    elements.detailDescription.textContent = '';
    elements.transitionActions.innerHTML = '';
    elements.commentsList.innerHTML = '<div class="no-comments">Loading discussion...</div>';
    elements.addCommentForm.reset();
    elements.commentCharCount.textContent = '0/1000';

    try {
      const [ticketRes, commentsRes] = await Promise.all([
        fetch(`${API_TICKETS}/${ticketId}`),
        fetch(`${API_TICKETS}/${ticketId}/comments?page=0&size=50`),
      ]);

      if (!ticketRes.ok) {
        throw new Error(await extractErrorMessage(ticketRes, 'Failed to load ticket'));
      }

      const ticket = await ticketRes.json();
      state.currentTicket = ticket;
      renderDetailTicket(ticket);

      if (commentsRes.ok) {
        const commentsData = await commentsRes.json();
        state.comments = commentsData.content || [];
        renderComments(state.comments);
      }
    } catch (err) {
      showToast(err.message, 'error');
      closeDetailModal();
    }
  }

  function closeDetailModal() {
    elements.detailModal.style.display = 'none';
    state.currentTicket = null;
    state.comments = [];
  }

  function renderDetailTicket(ticket) {
    elements.detailId.textContent = `#${ticket.id}`;
    elements.detailTitle.textContent = ticket.title;
    elements.detailDescription.textContent = ticket.description || '(No description provided)';
    elements.detailAssignee.textContent = ticket.assigneeId || 'Unassigned';
    elements.detailCreatedAt.textContent = formatDateTime(ticket.createdAt);
    elements.detailUpdatedAt.textContent = formatDateTime(ticket.updatedAt);

    // Status Badge
    elements.detailStatusBadge.className = `status-badge ${ticket.status}`;
    elements.detailStatusBadge.textContent = formatStatus(ticket.status);

    // Prepare Edit Form values
    elements.editTitle.value = ticket.title;
    elements.editTitleCount.textContent = `${ticket.title.length}/150`;
    elements.editAssignee.value = ticket.assigneeId || '';
    elements.editDescription.value = ticket.description || '';

    // Render Allowed Lifecycle Transitions
    renderTransitionButtons(ticket);
  }

  /**
   * Render allowed status transition actions
   */
  function renderTransitionButtons(ticket) {
    const transitions = ALLOWED_TRANSITIONS[ticket.status] || [];
    elements.transitionActions.innerHTML = '';

    if (transitions.length === 0) {
      elements.lifecycleRuleText.textContent = 'Current status is terminal:';
      elements.transitionActions.innerHTML = `
        <span class="terminal-badge">No further transitions allowed for ${ticket.status}</span>
      `;
      return;
    }

    elements.lifecycleRuleText.textContent = 'Available next states:';
    transitions.forEach((trans) => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = `btn-transition ${trans.className}`;
      btn.innerHTML = `<span>${trans.icon}</span> <span>${trans.label}</span>`;
      btn.addEventListener('click', () => handleStatusTransition(ticket.id, trans.target));
      elements.transitionActions.appendChild(btn);
    });
  }

  /**
   * Handle Status Transition via PATCH /api/v1/tickets/{id}/status
   */
  async function handleStatusTransition(ticketId, targetStatus) {
    try {
      const response = await fetch(`${API_TICKETS}/${ticketId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: targetStatus }),
      });

      if (!response.ok) {
        throw new Error(await extractErrorMessage(response, 'Status transition rejected'));
      }

      const updated = await response.json();
      state.currentTicket = updated;
      renderDetailTicket(updated);
      showToast(`Ticket #${updated.id} transitioned to ${formatStatus(targetStatus)}`, 'success');
      loadTickets();
      loadOverallStats();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  /**
   * Toggle Edit Mode for Ticket
   */
  function toggleEditMode(forceEdit) {
    const isEditing = forceEdit !== undefined ? forceEdit : elements.editTicketForm.style.display === 'none';
    if (isEditing) {
      elements.detailViewMode.style.display = 'none';
      elements.editTicketForm.style.display = 'flex';
      elements.btnEditLabel.textContent = 'Viewing';
      elements.editTitle.focus();
    } else {
      elements.detailViewMode.style.display = 'block';
      elements.editTicketForm.style.display = 'none';
      elements.btnEditLabel.textContent = 'Edit';
    }
  }

  /**
   * Save Edited Ticket via PUT /api/v1/tickets/{id}
   */
  async function handleSaveEditTicket(e) {
    e.preventDefault();
    if (!state.currentTicket) return;

    const title = elements.editTitle.value.trim();
    const description = elements.editDescription.value.trim();
    const assigneeId = elements.editAssignee.value.trim() || null;

    if (title.length < 5 || title.length > 150) {
      showToast('Title must be between 5 and 150 characters long', 'error');
      return;
    }
    if (!description) {
      showToast('Description is required', 'error');
      return;
    }

    try {
      const response = await fetch(`${API_TICKETS}/${state.currentTicket.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, description, assigneeId }),
      });

      if (!response.ok) {
        throw new Error(await extractErrorMessage(response, 'Failed to update ticket'));
      }

      const updated = await response.json();
      state.currentTicket = updated;
      renderDetailTicket(updated);
      toggleEditMode(false);
      showToast(`Ticket #${updated.id} updated successfully`, 'success');
      loadTickets();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  /**
   * Comments Section
   */
  function renderComments(comments) {
    elements.commentCountBadge.textContent = comments.length;
    elements.commentsList.innerHTML = '';

    if (comments.length === 0) {
      elements.commentsList.innerHTML = `
        <div class="no-comments">No discussion yet. Leave the first note below!</div>
      `;
      return;
    }

    comments.forEach((c) => {
      const item = document.createElement('div');
      item.className = 'comment-item';
      item.innerHTML = `
        <div class="comment-item-header">
          <span class="comment-author">${escapeHtml(c.author || 'Anonymous')}</span>
          <span class="comment-time">${formatDateTime(c.createdAt)}</span>
        </div>
        <div class="comment-content">${escapeHtml(c.content)}</div>
      `;
      elements.commentsList.appendChild(item);
    });
  }

  /**
   * Add Comment via POST /api/v1/tickets/{id}/comments
   */
  async function handleAddComment(e) {
    e.preventDefault();
    if (!state.currentTicket) return;

    const content = elements.commentContent.value.trim();
    const author = elements.commentAuthor.value.trim() || null;

    if (!content) {
      showToast('Comment content cannot be empty', 'error');
      return;
    }
    if (content.length > 1000) {
      showToast('Comment is too long (maximum 1000 characters)', 'error');
      return;
    }

    elements.btnSubmitComment.disabled = true;

    try {
      const response = await fetch(`${API_TICKETS}/${state.currentTicket.id}/comments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content, author }),
      });

      if (!response.ok) {
        throw new Error(await extractErrorMessage(response, 'Failed to post comment'));
      }

      const newComment = await response.json();
      state.comments.push(newComment);
      renderComments(state.comments);
      elements.commentContent.value = '';
      elements.commentCharCount.textContent = '0/1000';
      showToast('Comment posted', 'success');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      elements.btnSubmitComment.disabled = false;
    }
  }

  /**
   * Seed Sample Data
   */
  async function handleSeedData() {
    if (!confirm('Would you like to seed 3 demo tickets created by Simran into the system?')) {
      return;
    }

    const demoTickets = [
      {
        title: 'Initial setup of TicketPulse platform architecture',
        description: 'Complete setup of the Spring Boot 4 REST API, JPA relational schema, and responsive UI dashboard built by Simran.',
        assigneeId: 'simran',
        initialStatusTransition: 'IN_PROGRESS',
        initialComment: 'Architecture scaffolding verified. Clean UI connected to REST endpoints.',
      },
      {
        title: 'Implement Concurrency-Safe State Machine for Tickets',
        description: 'Enforce valid status transitions (OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED) with optimistic locking or state validation.',
        assigneeId: 'eng-team',
        initialStatusTransition: null,
        initialComment: 'State transitions matrix matches product specifications.',
      },
      {
        title: 'Review OpenAPI and Swagger UI Contract Compliance',
        description: 'Validate OpenAPI schema at /api/v1/openapi.json and ensure all validation constraints are enforced.',
        assigneeId: 'simran',
        initialStatusTransition: 'RESOLVED',
        initialComment: 'OpenAPI verified: title, description, and Simran developer info up to date.',
      },
    ];

    let createdCount = 0;
    for (const demo of demoTickets) {
      try {
        const createRes = await fetch(API_TICKETS, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            title: demo.title,
            description: demo.description,
            assigneeId: demo.assigneeId,
          }),
        });

        if (createRes.ok) {
          const ticket = await createRes.json();
          createdCount++;

          // Transition if requested
          if (demo.initialStatusTransition === 'IN_PROGRESS') {
            await fetch(`${API_TICKETS}/${ticket.id}/status`, {
              method: 'PATCH',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ status: 'IN_PROGRESS' }),
            });
          } else if (demo.initialStatusTransition === 'RESOLVED') {
            await fetch(`${API_TICKETS}/${ticket.id}/status`, {
              method: 'PATCH',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ status: 'IN_PROGRESS' }),
            });
            await fetch(`${API_TICKETS}/${ticket.id}/status`, {
              method: 'PATCH',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ status: 'RESOLVED' }),
            });
          }

          // Add comment
          if (demo.initialComment) {
            await fetch(`${API_TICKETS}/${ticket.id}/comments`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                content: demo.initialComment,
                author: 'Simran',
              }),
            });
          }
        }
      } catch (e) {
        console.error('Seed ticket error', e);
      }
    }

    showToast(`Seeded ${createdCount} demo tickets!`, 'success');
    loadTickets();
    loadOverallStats();
  }

  /**
   * Helper: Extract rich error message from ProblemDetail or HTTP response
   */
  async function extractErrorMessage(response, defaultMsg) {
    try {
      const data = await response.json();
      if (data.fieldErrors && Array.isArray(data.fieldErrors)) {
        return data.fieldErrors.map((f) => `${f.field}: ${f.message}`).join('; ');
      }
      if (data.detail) return data.detail;
      if (data.message) return data.message;
      if (data.title) return data.title;
    } catch (_) {
      // Ignore JSON parse failure
    }
    return `${defaultMsg} (${response.status} ${response.statusText})`;
  }

  /**
   * Toast Notifications
   */
  function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'ℹ️';
    if (type === 'success') icon = '✓';
    if (type === 'error') icon = '⚠️';

    toast.innerHTML = `<span>${icon}</span><span>${escapeHtml(message)}</span>`;
    elements.toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 250);
    }, 4000);
  }

  /**
   * Format Utilities
   */
  function formatStatus(status) {
    if (!status) return 'OPEN';
    return status.replace(/_/g, ' ');
  }

  function formatDate(isoString) {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (_) {
      return isoString;
    }
  }

  function formatDateTime(isoString) {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch (_) {
      return isoString;
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // Run app on DOM load
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
