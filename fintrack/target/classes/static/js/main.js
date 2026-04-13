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
// 4. SPLIT TYPE SWITCHER
// ─────────────────────────────────────────────
function initSplitType() {
  const select = document.getElementById('splitType');
  if (!select) return;
  select.addEventListener('change', () => {
    const val = select.value;
    document.querySelectorAll('.split-section').forEach(s => s.style.display = 'none');
    const section = document.getElementById('split-' + val.toLowerCase());
    if (section) section.style.display = 'block';
    updateSplitHints(val);
  });
  // Trigger on load
  select.dispatchEvent(new Event('change'));
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
