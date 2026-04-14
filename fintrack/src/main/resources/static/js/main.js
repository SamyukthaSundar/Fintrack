/**
 * FinTrack — Main JavaScript
 * Dark/Light Mode Toggle, OCR Upload, Chart.js helpers, Notifications
 */

// ─────────────────────────────────────────────
// 1. THEME TOGGLE (Dark ↔ Light)
// ─────────────────────────────────────────────
(function initTheme() {
  const saved = localStorage.getItem('fintrack-theme') || 'dark';
  document.documentElement.setAttribute('data-theme', saved);
  updateThemeButton(saved);
})();

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'dark';
  const next    = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('fintrack-theme', next);
  updateThemeButton(next);
}

function updateThemeButton(theme) {
  const btn = document.getElementById('themeToggle');
  if (btn) {
    btn.innerHTML = theme === 'dark'
      ? '<span>☀️</span> Light Mode'
      : '<span>🌙</span> Dark Mode';
  }
}

// ─────────────────────────────────────────────
// 2. NOTIFICATION BADGE — live unread count
// ─────────────────────────────────────────────
function refreshNotificationBadge() {
  fetch('/notifications/api/unread-count')
    .then(r => r.json())
    .then(count => {
      const badge = document.getElementById('notifBadge');
      if (!badge) return;
      badge.textContent  = count;
      badge.style.display = count > 0 ? 'inline-flex' : 'none';
    })
    .catch(() => {});
}

document.addEventListener('DOMContentLoaded', () => {
  refreshNotificationBadge();
  setInterval(refreshNotificationBadge, 30000);
});

// ─────────────────────────────────────────────
// 3. OCR RECEIPT SCANNER
// ─────────────────────────────────────────────
function initOcrZone() {
  const zone  = document.getElementById('ocrZone');
  const input = document.getElementById('receiptFile');
  const preview = document.getElementById('ocrPreview');
  const amtField = document.getElementById('totalAmount');
  const status  = document.getElementById('ocrStatus');

  if (!zone || !input) return;

  // Drag & Drop
  zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('drag-over'); });
  zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
  zone.addEventListener('drop', e => {
    e.preventDefault();
    zone.classList.remove('drag-over');
    if (e.dataTransfer.files.length) {
      input.files = e.dataTransfer.files;
      processOcr(e.dataTransfer.files[0]);
    }
  });

  input.addEventListener('change', () => {
    if (input.files.length) processOcr(input.files[0]);
  });
}

function processOcr(file) {
  const status   = document.getElementById('ocrStatus');
  const amtField = document.getElementById('totalAmount');
  const rawText  = document.getElementById('ocrRawText');

  if (status) { status.textContent = '🔍 Scanning receipt...'; status.className = 'text-muted'; }

  const fd = new FormData();
  fd.append('receiptFile', file);

  fetch('/expenses/ocr-scan', { method: 'POST', body: fd })
    .then(r => r.json())
    .then(data => {
      if (status) { status.textContent = '✓ OCR complete!'; status.className = 'neon-text'; }
      if (amtField && data.detectedAmount) amtField.value = data.detectedAmount;
      if (rawText) rawText.value = data.rawText || '';
      const preview = document.getElementById('ocrPreview');
      if (preview && data.rawText) {
        preview.innerHTML = `<pre style="white-space:pre-wrap;font-size:0.75rem;color:var(--text-secondary);max-height:140px;overflow:auto">${escapeHtml(data.rawText)}</pre>`;
      }
      // Show receipt image preview
      const imgPreview = document.getElementById('receiptImagePreview');
      if (imgPreview) {
        const reader = new FileReader();
        reader.onload = e => { imgPreview.src = e.target.result; imgPreview.style.display = 'block'; };
        reader.readAsDataURL(file);
      }
    })
    .catch(() => { if (status) { status.textContent = '⚠ OCR failed — enter amount manually.'; status.className = 'neon-amber'; }});
}

// ─────────────────────────────────────────────
// 4. SPLIT TYPE SWITCHER + PARTICIPANT SYNC
// FIX: Disable inputs in hidden sections so they don't pollute splitData.
//      Sync split-data rows to only show selected participants.
// ─────────────────────────────────────────────
function initSplitType() {
  const select = document.getElementById('splitType');
  if (!select) return;

  select.addEventListener('change', () => {
    const val = select.value;

    // Hide all sections and DISABLE their inputs (so they're not submitted)
    document.querySelectorAll('.split-section').forEach(section => {
      section.style.display = 'none';
      section.querySelectorAll('input').forEach(inp => { inp.disabled = true; });
    });

    // Show active section and ENABLE its inputs
    const active = document.getElementById('split-' + val.toLowerCase());
    if (active) {
      active.style.display = 'block';
      // Only enable inputs for checked (selected) participants
      syncSplitRowsToParticipants(val);
    }

    updateSplitHints(val);
    updateLivePreview();
  });

  // Also re-sync when participant checkboxes change
  document.querySelectorAll('.participant-checkbox').forEach(cb => {
    cb.addEventListener('change', () => {
      const currentType = select.value;
      syncSplitRowsToParticipants(currentType);
      updateLivePreview();
    });
  });

  // Wire live preview on amount change
  const totalInput = document.getElementById('totalAmount');
  if (totalInput) totalInput.addEventListener('input', updateLivePreview);

  // Wire live preview on splitData input changes
  document.addEventListener('input', e => {
    if (e.target.closest('.split-section')) updateLivePreview();
  });

  // Trigger on load
  select.dispatchEvent(new Event('change'));
}

/**
 * For WEIGHTED / PERCENTAGE / EXACT sections:
 * Enable only the rows whose participant checkbox is checked,
 * disable (and zero out) rows for unchecked participants.
 */
function syncSplitRowsToParticipants(splitType) {
  const sectionId = 'split-' + splitType.toLowerCase();
  const section = document.getElementById(sectionId);
  if (!section || splitType === 'EQUAL') return;

  const checkedIds = getCheckedParticipantIds();

  section.querySelectorAll('.split-data-row').forEach(row => {
    const uid = row.dataset.userId;
    const inp = row.querySelector('input[type="number"]');
    if (!inp) return;

    if (checkedIds.has(uid)) {
      row.style.display = 'flex';
      inp.disabled = false;
    } else {
      row.style.display = 'none';
      inp.disabled = true;
      inp.value = '';
    }
  });
}

/** Returns a Set of user ID strings that are currently checked as participants */
function getCheckedParticipantIds() {
  const checked = new Set();
  document.querySelectorAll('.participant-checkbox:checked').forEach(cb => {
    checked.add(cb.value);
  });
  // If no checkboxes exist (all-members mode), include everyone
  if (checked.size === 0) {
    document.querySelectorAll('.participant-checkbox').forEach(cb => {
      checked.add(cb.value);
    });
  }
  return checked;
}

/**
 * Live split preview — shows each participant's computed share as they type.
 */
function updateLivePreview() {
  const preview = document.getElementById('splitPreview');
  if (!preview) return;

  const splitType = document.getElementById('splitType')?.value;
  const totalStr  = document.getElementById('totalAmount')?.value;
  const total     = parseFloat(totalStr) || 0;

  if (!splitType || total <= 0) { preview.innerHTML = ''; return; }

  const checkedIds = getCheckedParticipantIds();
  const names = {};
  document.querySelectorAll('.participant-checkbox').forEach(cb => {
    names[cb.value] = cb.dataset.name || cb.value;
  });
  // Also include members that have no checkbox (all-member mode)
  document.querySelectorAll('[data-member-id]').forEach(el => {
    names[el.dataset.memberId] = el.dataset.memberName || el.dataset.memberId;
  });

  const participants = [...checkedIds];
  if (participants.length === 0) { preview.innerHTML = ''; return; }

  let rows = [];

  if (splitType === 'EQUAL') {
    const share = total / participants.length;
    participants.forEach(uid => {
      rows.push({ name: names[uid] || uid, amount: share });
    });

  } else if (splitType === 'PERCENTAGE') {
    let sumPct = 0;
    participants.forEach(uid => {
      const inp = document.querySelector(`#split-percentage input[data-user-id="${uid}"]`);
      const pct = parseFloat(inp?.value) || 0;
      sumPct += pct;
      rows.push({ name: names[uid] || uid, amount: (total * pct / 100), pct });
    });
    // Show warning if percentages don't add up
    const warn = Math.abs(sumPct - 100) > 0.02;
    rows = rows.map(r => ({ ...r, warn }));
    if (warn) rows.push({ warning: `Total: ${sumPct.toFixed(2)}% (must be 100%)` });

  } else if (splitType === 'EXACT') {
    let sumAmt = 0;
    participants.forEach(uid => {
      const inp = document.querySelector(`#split-exact input[data-user-id="${uid}"]`);
      const amt = parseFloat(inp?.value) || 0;
      sumAmt += amt;
      rows.push({ name: names[uid] || uid, amount: amt });
    });
    const warn = Math.abs(sumAmt - total) > 0.02;
    if (warn) rows.push({ warning: `Sum ₹${sumAmt.toFixed(2)} ≠ Total ₹${total.toFixed(2)}` });

  } else if (splitType === 'WEIGHTED') {
    let totalWeight = 0;
    const weights = {};
    participants.forEach(uid => {
      const inp = document.querySelector(`#split-weighted input[data-user-id="${uid}"]`);
      const w = parseFloat(inp?.value) || 1;
      weights[uid] = w;
      totalWeight += w;
    });
    participants.forEach(uid => {
      rows.push({ name: names[uid] || uid, amount: total * (weights[uid] / totalWeight), weight: weights[uid] });
    });
  }

  // Render
  let html = '<div class="split-preview-box">';
  html += '<div style="font-size:0.78rem;color:var(--text-muted);margin-bottom:0.4rem;font-weight:600">PREVIEW</div>';
  rows.forEach(r => {
    if (r.warning) {
      html += `<div class="split-preview-row split-preview-warn">⚠ ${r.warning}</div>`;
    } else {
      const label = r.weight ? ` (×${r.weight})` : r.pct != null ? ` (${r.pct}%)` : '';
      html += `<div class="split-preview-row">
        <span class="split-preview-name">${escapeHtml(r.name)}${label}</span>
        <span class="split-preview-amt">₹${r.amount.toFixed(2)}</span>
      </div>`;
    }
  });
  html += '</div>';
  preview.innerHTML = html;
}

function updateSplitHints(type) {
  const hints = {
    EQUAL:      'Amount is divided equally among all participants.',
    PERCENTAGE: 'Assign % of the total to each participant (must sum to 100%).',
    EXACT:      'Enter the exact rupee amount each person owes.',
    WEIGHTED:   'Assign relative weights (e.g., 2:1:1) to distribute proportionally.'
  };
  const hint = document.getElementById('splitHint');
  if (hint) hint.textContent = hints[type] || '';
}

// ─────────────────────────────────────────────
// 5. CHART.JS HELPERS (Sanika's Analytics)
// ─────────────────────────────────────────────
const NEON_COLORS = [
  'rgba(0,200,150,0.8)',
  'rgba(0,180,216,0.8)',
  'rgba(155,93,229,0.8)',
  'rgba(255,165,2,0.8)',
  'rgba(255,71,87,0.8)',
  'rgba(52,211,153,0.8)',
  'rgba(99,179,237,0.8)',
  'rgba(237,100,166,0.8)',
];

function renderMonthlyChart(canvasId, labels, values) {
  const ctx = document.getElementById(canvasId);
  if (!ctx) return;
  new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: 'Expenses (₹)',
        data: values,
        backgroundColor: 'rgba(0,200,150,0.3)',
        borderColor: 'rgba(0,200,150,1)',
        borderWidth: 2,
        borderRadius: 6,
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: { label: ctx => '₹ ' + ctx.parsed.y.toLocaleString('en-IN') }
        }
      },
      scales: {
        x: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#888' } },
        y: {
          grid: { color: 'rgba(255,255,255,0.05)' },
          ticks: { color: '#888', callback: v => '₹' + v.toLocaleString('en-IN') }
        }
      }
    }
  });
}

function renderCategoryChart(canvasId, labels, values) {
  const ctx = document.getElementById(canvasId);
  if (!ctx) return;
  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{ data: values, backgroundColor: NEON_COLORS, borderWidth: 0 }]
    },
    options: {
      responsive: true,
      cutout: '65%',
      plugins: {
        legend: { position: 'bottom', labels: { color: '#888', padding: 16, font: { size: 11 } } },
        tooltip: {
          callbacks: { label: ctx => ctx.label + ': ₹' + ctx.parsed.toLocaleString('en-IN') }
        }
      }
    }
  });
}

// ─────────────────────────────────────────────
// 6. CONFIRMATION DIALOGS
// ─────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('[data-confirm]').forEach(el => {
    el.addEventListener('click', e => {
      if (!confirm(el.getAttribute('data-confirm') || 'Are you sure?')) e.preventDefault();
    });
  });
  initOcrZone();
  initSplitType();
});

// ─────────────────────────────────────────────
// 7. AUTO-DISMISS ALERTS
// ─────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  setTimeout(() => {
    document.querySelectorAll('.alert.auto-dismiss').forEach(el => {
      el.style.transition = 'opacity 0.5s';
      el.style.opacity = '0';
      setTimeout(() => el.remove(), 500);
    });
  }, 4000);
});

// ─────────────────────────────────────────────
// 8. UTILITIES
// ─────────────────────────────────────────────
function escapeHtml(str) {
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function formatCurrency(amount, currency = 'INR') {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency }).format(amount);
}