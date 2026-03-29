/* ═══════════════════════════════════════════════════════════
   Library Management System — Frontend Logic
   ═══════════════════════════════════════════════════════════ */

const API = '';  // same origin

// ─── AUTH CHECK ────────────────────────────────────────────
if (!localStorage.getItem('librarianToken') && !window.location.pathname.includes('login.html')) {
  window.location.href = '/login.html';
}

function logout() {
  localStorage.removeItem('librarianToken');
  window.location.href = '/login.html';
}

// ─── DATA LOADING ──────────────────────────────────────────
async function loadAll() {
  try {
    const [booksRes, membersRes, issuedRes] = await Promise.all([
      fetch(`${API}/api/books`),
      fetch(`${API}/api/members`),
      fetch(`${API}/api/issued`)
    ]);
    const books   = await booksRes.json();
    const members = await membersRes.json();
    const issued  = await issuedRes.json();

    renderStats(books, members, issued);
    renderBooks(books);
    renderMembers(members, issued, books);
    renderIssued(issued, books, members);
    renderRankings(members);
    renderWarnings(issued, books, members);
  } catch (err) {
    console.error('Failed to load data:', err);
    toast('Could not connect to server', 'error');
  }
}

// ─── STATS ─────────────────────────────────────────────────
function renderStats(books, members, issued) {
  animateNumber('totalBooks', books.length);
  animateNumber('totalMembers', members.length);
  animateNumber('totalIssued', issued.length);
  animateNumber('totalAvailable', books.filter(b => b.available).length);
}

function animateNumber(id, target) {
  const el = document.getElementById(id);
  const start = parseInt(el.textContent) || 0;
  const duration = 500;
  const startTime = performance.now();

  function tick(now) {
    const progress = Math.min((now - startTime) / duration, 1);
    const ease = 1 - Math.pow(1 - progress, 3); // easeOutCubic
    el.textContent = Math.round(start + (target - start) * ease);
    if (progress < 1) requestAnimationFrame(tick);
  }
  requestAnimationFrame(tick);
}

// ─── RENDER BOOKS TABLE ────────────────────────────────────
function renderBooks(books) {
  const tbody = document.getElementById('booksTable');
  if (books.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
      <br>No books yet. Click <strong>"Add Book"</strong> to get started.</td></tr>`;
    return;
  }
  tbody.innerHTML = books.map((b, i) => `
    <tr style="animation:fadeUp .35s ${i * .04}s both">
      <td><strong>#${b.id}</strong></td>
      <td>${escapeHtml(b.title)}</td>
      <td>${escapeHtml(b.author)}</td>
      <td>${b.available
        ? '<span class="badge badge-green">● Available</span>'
        : '<span class="badge badge-red">● Issued</span>'}</td>
    </tr>`).join('');
}

// ─── RENDER MEMBERS TABLE ──────────────────────────────────
function renderMembers(members, issued, books) {
  const tbody = document.getElementById('membersTable');
  if (members.length === 0) {
    tbody.innerHTML = `<tr><td colspan="3" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
      <br>No members yet. Click <strong>"Add Member"</strong> to get started.</td></tr>`;
    return;
  }
  
  // lookup maps
  const bookMap = {};
  if(books) books.forEach(b => bookMap[b.id] = b.title);

  tbody.innerHTML = members.map((m, i) => {
    let borrowed = [];
    if(issued && books) {
      borrowed = issued.filter(issue => issue.memberId === m.id).map(iso => bookMap[iso.bookId] || `Book #${iso.bookId}`);
    }
    let borrowedText = borrowed.length > 0 ? borrowed.join(', ') : '<span style="color:var(--text-muted)">None</span>';

    return `
    <tr style="animation:fadeUp .35s ${i * .04}s both">
      <td><strong>#${m.id}</strong></td>
      <td>${escapeHtml(m.name)}</td>
      <td>${borrowedText}</td>
    </tr>`
  }).join('');
}

// ─── RENDER ISSUED TABLE ───────────────────────────────────
function renderIssued(issued, books, members) {
  const tbody = document.getElementById('issuedTable');
  if (issued.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
      <br>No books currently issued.</td></tr>`;
    return;
  }

  // lookup maps
  const bookMap = {};
  books.forEach(b => bookMap[b.id] = b.title);
  const memberMap = {};
  members.forEach(m => memberMap[m.id] = m.name);

  tbody.innerHTML = issued.map((r, i) => {
    return `
    <tr style="animation:fadeUp .35s ${i * .04}s both">
      <td><strong>#${r.bookId}</strong> <span style="color:var(--text-muted); font-size:.82rem">${escapeHtml(bookMap[r.bookId] || '')}</span></td>
      <td><strong>#${r.memberId}</strong> <span style="color:var(--text-muted); font-size:.82rem">${escapeHtml(memberMap[r.memberId] || '')}</span></td>
      <td>${r.issueDate || 'N/A'}</td>
      <td>${r.dueDate || 'N/A'}</td>
      <td>${r.daysOverdue > 0 ? `<span class="badge badge-red">${r.daysOverdue} days</span>` : '<span class="badge badge-green">On Time</span>'}</td>
      <td>${r.fine > 0 ? `<strong style="color: #ef4444">₹${r.fine}</strong>` : '₹0'}</td>
    </tr>`
  }).join('');
}

// ─── RENDER RANKINGS ───────────────────────────────────────
function renderRankings(members) {
  const tbody = document.getElementById('rankingsTable');
  if (members.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="empty-state">No rankings yet.</td></tr>`;
    return;
  }
  // sort members by totalBorrowed descending
  const sorted = [...members].sort((a,b) => (b.totalBorrowed || 0) - (a.totalBorrowed || 0));
  
  tbody.innerHTML = sorted.map((m, i) => {
    let rankBadge = i === 0 ? '👑 1st' : i === 1 ? '🥈 2nd' : i === 2 ? '🥉 3rd' : `${i+1}th`;
    return `
    <tr style="animation:fadeUp .35s ${i * .04}s both">
      <td><strong>${rankBadge}</strong></td>
      <td>#${m.id}</td>
      <td>${escapeHtml(m.name)}</td>
      <td><span class="badge badge-green">${m.totalBorrowed || 0} books</span></td>
    </tr>`;
  }).join('');
}

// ─── RENDER WARNINGS ───────────────────────────────────────
function renderWarnings(issued, books, members) {
  const dueTbody = document.getElementById('warningsDueTomorrowTable');
  const missedTbody = document.getElementById('warningsMissedTable');
  
  const bookMap = {};
  books.forEach(b => bookMap[b.id] = b.title);
  const memberMap = {};
  members.forEach(m => memberMap[m.id] = m.name);

  // find due tomorrow (daysOverdue === -1 implies due exactly tomorrow)
  const dueTomorrow = issued.filter(r => r.daysOverdue === -1);
  const missed = issued.filter(r => r.missed); // missed handles exactly the 14+ day penalty

  // update badge
  const badge = document.getElementById('warningBadge');
  const totalWarnings = dueTomorrow.length + missed.length;
  if(totalWarnings > 0) {
    badge.style.display = 'inline-block';
    badge.textContent = totalWarnings;
  } else {
    badge.style.display = 'none';
  }

  // Render Due Tomorrow
  if (dueTomorrow.length === 0) {
    dueTbody.innerHTML = `<tr><td colspan="2" class="empty-state" style="padding: 1.5rem; border: none;">No books due tomorrow. 🎉</td></tr>`;
  } else {
    dueTbody.innerHTML = dueTomorrow.map((r, i) => `
      <tr style="animation:fadeUp .35s ${i * .04}s both">
        <td><strong>#${r.bookId}</strong> <span style="color:var(--text-muted)">${escapeHtml(bookMap[r.bookId] || '')}</span></td>
        <td><strong>#${r.memberId}</strong> <span style="color:var(--text-muted)">${escapeHtml(memberMap[r.memberId] || '')}</span></td>
      </tr>
    `).join('');
  }

  // Render Missed Books
  if (missed.length === 0) {
    missedTbody.innerHTML = `<tr><td colspan="3" class="empty-state" style="padding: 1.5rem; border: none;">No missed books. 🎉</td></tr>`;
  } else {
    missedTbody.innerHTML = missed.map((r, i) => `
      <tr style="animation:fadeUp .35s ${i * .04}s both">
        <td><strong>#${r.bookId}</strong> <span style="color:var(--text-muted)">${escapeHtml(bookMap[r.bookId] || '')}</span></td>
        <td><strong>#${r.memberId}</strong> <span style="color:var(--text-muted)">${escapeHtml(memberMap[r.memberId] || '')}</span></td>
        <td><strong style="color: #ef4444">₹${r.fine}</strong></td>
      </tr>
    `).join('');
  }
}

// ─── TAB SWITCHING ─────────────────────────────────────────
function switchTab(name) {
  document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.dataset.tab === name));
  document.querySelectorAll('.tab-content').forEach(c => c.classList.toggle('active', c.id === `tab-${name}`));
}

// ─── MODAL ─────────────────────────────────────────────────
const formMap = {
  addBook:    { title: 'Add a New Book',    form: 'formAddBook'    },
  addMember:  { title: 'Add a New Member',  form: 'formAddMember'  },
  issueBook:  { title: 'Issue a Book',      form: 'formIssueBook'  },
  returnBook: { title: 'Return a Book',     form: 'formReturnBook' },
};

function openModal(type) {
  const info = formMap[type];
  document.getElementById('modalTitle').textContent = info.title;
  document.querySelectorAll('.modal-form').forEach(f => f.classList.remove('visible'));
  document.getElementById(info.form).classList.add('visible');
  document.getElementById('modalOverlay').classList.add('open');
}

function closeModal() {
  document.getElementById('modalOverlay').classList.remove('open');
  // reset forms after animation
  setTimeout(() => {
    document.querySelectorAll('.modal-form').forEach(f => { f.reset(); f.classList.remove('visible'); });
  }, 250);
}

// close on Escape
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

// ─── FORM HANDLERS ─────────────────────────────────────────
async function submitAddBook(e) {
  e.preventDefault();
  const title  = document.getElementById('bookTitle').value.trim();
  const author = document.getElementById('bookAuthor').value.trim();
  if (!title || !author) return;

  try {
    const res = await fetch(`${API}/api/books`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title, author })
    });
    const data = await res.json();
    toast(data.message, 'success');
    closeModal();
    loadAll();
  } catch (err) {
    toast('Failed to add book', 'error');
  }
}

async function submitAddMember(e) {
  e.preventDefault();
  const name = document.getElementById('memberName').value.trim();
  if (!name) return;

  try {
    const res = await fetch(`${API}/api/members`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name })
    });
    const data = await res.json();
    toast(data.message, 'success');
    closeModal();
    loadAll();
  } catch (err) {
    toast('Failed to add member', 'error');
  }
}

async function submitIssueBook(e) {
  e.preventDefault();
  const bookId   = parseInt(document.getElementById('issueBookId').value);
  const memberId = parseInt(document.getElementById('issueMemberId').value);
  if (isNaN(bookId) || isNaN(memberId)) return;

  try {
    const res = await fetch(`${API}/api/issue`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ bookId, memberId })
    });
    const data = await res.json();
    const isErr = data.message.toLowerCase().includes('error') ||
                  data.message.toLowerCase().includes('not') ||
                  data.message.toLowerCase().includes('already');
    toast(data.message, isErr ? 'error' : 'success');
    closeModal();
    loadAll();
  } catch (err) {
    toast('Failed to issue book', 'error');
  }
}

async function submitReturnBook(e) {
  e.preventDefault();
  const bookId = parseInt(document.getElementById('returnBookId').value);
  if (isNaN(bookId)) return;

  try {
    const res = await fetch(`${API}/api/return`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ bookId })
    });
    const data = await res.json();
    const isErr = data.message.toLowerCase().includes('error') ||
                  data.message.toLowerCase().includes('not') ||
                  data.message.toLowerCase().includes('already');
    toast(data.message, isErr ? 'error' : 'success');
    closeModal();
    loadAll();
  } catch (err) {
    toast('Failed to return book', 'error');
  }
}

// ─── TOAST NOTIFICATIONS ───────────────────────────────────
function toast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  const el = document.createElement('div');
  el.className = `toast ${type}`;

  const icon = type === 'success'
    ? '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>'
    : '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';

  el.innerHTML = `${icon}<span>${escapeHtml(message)}</span>`;
  container.appendChild(el);

  setTimeout(() => el.remove(), 3600);
}

// ─── UTILS ─────────────────────────────────────────────────
function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

// ─── INIT ──────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', loadAll);
