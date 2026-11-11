/**
 * NetSentinel Platform Core Frontend Scripts
 * Strict Professional Monochrome System & Interactive Visualizations
 * Network Anomaly Detection and Incident Analyzer
 */

document.addEventListener('DOMContentLoaded', () => {
    // Ensure clean single-theme environment
    try {
        localStorage.removeItem('netsentinel_theme');
        document.documentElement.removeAttribute('data-theme');
    } catch (e) {}

    initNotifications();
    initPasswordToggles();
    initQuickLoginButtons();
    initCharts();

    // Auto-mount Firebase Realtime Client
    if (!document.querySelector('script[src*="firebase-client.js"]')) {
        const fbScript = document.createElement('script');
        fbScript.src = '/js/firebase-client.js';
        document.head.appendChild(fbScript);
    }
});

/* ==========================================================================
   Password Visibility Toggle
   ========================================================================== */
function initPasswordToggles() {
    const toggleButtons = document.querySelectorAll('.password-toggle-btn');
    toggleButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-target') || 'passwordInput';
            const input = document.getElementById(targetId);
            if (!input) return;
            const isPassword = input.getAttribute('type') === 'password';
            input.setAttribute('type', isPassword ? 'text' : 'password');
            btn.innerHTML = isPassword
                ? `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"></path><circle cx="12" cy="12" r="3"></circle></svg>`
                : `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"></path><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"></path><path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"></path><line x1="2" x2="22" y1="2" y2="22"></line></svg>`;
        });
    });
}

/* ==========================================================================
   Demo Account Quick-Login Buttons (Canonical 5 Accounts)
   ========================================================================== */
function initQuickLoginButtons() {
    const quickButtons = document.querySelectorAll('.btn-quick-login');
    quickButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            const role = btn.getAttribute('data-role');
            const usernameInput = document.getElementById('usernameInput');
            const passwordInput = document.getElementById('passwordInput');
            const loginForm = document.getElementById('loginForm');

            if (!role || !loginForm) return;

            let user = '';
            let pass = '';
            switch(role.toLowerCase()) {
                case 'student101':
                    user = 'student101';
                    pass = 'student123';
                    break;
                case 'student102':
                    user = 'student102';
                    pass = 'student123';
                    break;
                case 'faculty101':
                    user = 'faculty101';
                    pass = 'faculty123';
                    break;
                case 'faculty102':
                    user = 'faculty102';
                    pass = 'faculty123';
                    break;
                case 'admin01':
                    user = 'admin01';
                    pass = 'admin123';
                    break;
                default:
                    user = role;
                    pass = 'student123';
            }

            if (usernameInput && passwordInput) {
                usernameInput.value = user;
                passwordInput.value = pass;
                showToast(`Authenticating demo session: ${user}...`, 'info');
                setTimeout(() => loginForm.submit(), 250);
            }
        });
    });
}

/* ==========================================================================
   Notifications Drawer / Popover
   ========================================================================== */
function initNotifications() {
    const bellBtn = document.getElementById('notificationBellBtn');
    const dropdown = document.getElementById('notificationDropdown');

    if (bellBtn && dropdown) {
        bellBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            dropdown.classList.toggle('active');
        });

        document.addEventListener('click', (e) => {
            if (!dropdown.contains(e.target) && e.target !== bellBtn) {
                dropdown.classList.remove('active');
            }
        });
    }

    const markAllReadBtn = document.getElementById('markAllReadBtn');
    if (markAllReadBtn) {
        markAllReadBtn.addEventListener('click', async () => {
            try {
                const res = await fetch('/api/v1/notifications/read-all', { method: 'POST' });
                if (res.ok) {
                    const badge = document.querySelector('.header-right .badge-count');
                    if (badge) badge.remove();
                    const notifList = document.querySelectorAll('.notification-item.unread');
                    notifList.forEach(el => el.classList.remove('unread'));
                    showToast('All notifications marked as read', 'success');
                }
            } catch (err) {
                console.error(err);
            }
        });
    }
}

/* ==========================================================================
   Toast Notification Manager (Monochrome System)
   ========================================================================== */
function showToast(message, type = 'info') {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <div style="flex:1;">${message}</div>
        <button type="button" style="background:none;border:none;cursor:pointer;color:inherit;opacity:0.7;padding:0 4px;" onclick="this.parentElement.remove()">
            ✕
        </button>
    `;

    container.appendChild(toast);
    setTimeout(() => {
        if (toast.parentElement) {
            toast.remove();
        }
    }, 4500);
}

/* ==========================================================================
   Native SVG Interactive Visualizations (Monochrome Palette)
   ========================================================================== */
function initCharts() {
    renderSeverityDonut();
    renderActivityHourlyBar();
}

function renderSeverityDonut() {
    const container = document.getElementById('severityDonutChart');
    if (!container) return;

    // Read data attributes
    const crit = parseInt(container.getAttribute('data-critical') || '0', 10);
    const high = parseInt(container.getAttribute('data-high') || '0', 10);
    const med = parseInt(container.getAttribute('data-medium') || '0', 10);
    const low = parseInt(container.getAttribute('data-low') || '0', 10);

    const total = crit + high + med + low;

    if (total === 0) {
        // Empty state ring
        container.innerHTML = `
            <svg viewBox="0 0 100 100" style="width: 140px; height: 140px;">
                <circle cx="50" cy="50" r="40" fill="transparent" stroke="var(--border-color)" stroke-width="12"></circle>
                <text x="50" y="54" text-anchor="middle" font-size="10" fill="var(--text-muted)" font-family="var(--font-mono)">0</text>
            </svg>
        `;
        return;
    }

    const critPct = (crit / total) * 100;
    const highPct = (high / total) * 100;
    const medPct = (med / total) * 100;
    const lowPct = (low / total) * 100;

    // Circumference of r=40 is ~251.32
    const c = 251.32;
    const critLen = (critPct / 100) * c;
    const highLen = (highPct / 100) * c;
    const medLen = (medPct / 100) * c;
    const lowLen = (lowPct / 100) * c;

    // Colors: Red for critical/high security anomalies, neutral greys for med/low
    const critColor = '#DC2626'; // Red strictly for genuine security warning
    const highColor = '#52525B'; // Charcoal dark grey
    const medColor = '#A1A1AA';  // Medium neutral grey
    const lowColor = '#E4E4E7';  // Light grey

    const svg = `
    <svg viewBox="0 0 100 100" style="width: 140px; height: 140px; transform: rotate(-90deg);">
        <circle cx="50" cy="50" r="40" fill="transparent" stroke="var(--bg-muted)" stroke-width="12"></circle>
        
        <circle cx="50" cy="50" r="40" fill="transparent" stroke="${critColor}" stroke-width="12"
            stroke-dasharray="${critLen} ${c - critLen}" stroke-dashoffset="0"></circle>
            
        <circle cx="50" cy="50" r="40" fill="transparent" stroke="${highColor}" stroke-width="12"
            stroke-dasharray="${highLen} ${c - highLen}" stroke-dashoffset="${-critLen}"></circle>
            
        <circle cx="50" cy="50" r="40" fill="transparent" stroke="${medColor}" stroke-width="12"
            stroke-dasharray="${medLen} ${c - medLen}" stroke-dashoffset="${-(critLen + highLen)}"></circle>
            
        <circle cx="50" cy="50" r="40" fill="transparent" stroke="${lowColor}" stroke-width="12"
            stroke-dasharray="${lowLen} ${c - lowLen}" stroke-dashoffset="${-(critLen + highLen + medLen)}"></circle>
    </svg>
    `;
    container.innerHTML = svg;
}

function renderActivityHourlyBar() {
    const container = document.getElementById('activityHourlyChart');
    if (!container) return;

    // Activity distribution bins
    const bins = [
        { label: '08:00', val: 12 },
        { label: '09:00', val: 34 },
        { label: '10:00', val: 58 },
        { label: '11:00', val: 76 },
        { label: '12:00', val: 45 },
        { label: '13:00', val: 30 },
        { label: '14:00', val: 62 },
        { label: '15:00', val: 88 },
        { label: '16:00', val: 51 },
        { label: '17:00', val: 28 },
        { label: '18:00', val: 18 },
        { label: '19:00', val: 14 }
    ];

    const maxVal = Math.max(1, ...bins.map(b => b.val));
    let barsHtml = '';
    bins.forEach(b => {
        const heightPct = Math.round((b.val / maxVal) * 100);
        barsHtml += `
            <div style="flex:1; display:flex; flex-direction:column; align-items:center; gap:6px; height:100%; justify-content:flex-end;">
                <div title="${b.label}: ${b.val} actions" style="width:100%; max-width:24px; height:${heightPct}%; background:var(--accent-primary); border-radius:3px 3px 0 0; transition: height 0.3s ease;"></div>
                <span style="font-size:0.65rem; color:var(--text-muted); font-family:var(--font-mono);">${b.label.split(':')[0]}h</span>
            </div>
        `;
    });

    container.innerHTML = `
        <div style="display:flex; align-items:flex-end; gap:8px; height:140px; width:100%; padding-top:10px;">
            ${barsHtml}
        </div>
    `;
}
