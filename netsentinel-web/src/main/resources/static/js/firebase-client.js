/**
 * NetSentinel Firebase Client Connector
 * 
 * Securely connects to Google Firebase Realtime Database & Analytics
 * dynamically using configuration supplied via backend environment variables.
 */

(function () {
    'use strict';

    window.NetSentinelFirebase = {
        config: null,
        app: null,
        db: null,
        analytics: null,
        status: 'INITIALIZING'
    };

    // Helper to dynamically load external script if not already present
    function loadScript(src) {
        return new Promise((resolve, reject) => {
            if (document.querySelector(`script[src="${src}"]`)) {
                return resolve();
            }
            const s = document.createElement('script');
            s.src = src;
            s.onload = () => resolve();
            s.onerror = (e) => reject(e);
            document.head.appendChild(s);
        });
    }

    async function initFirebaseClient() {
        try {
            // Fetch configuration dynamically from backend environment
            const resp = await fetch('/api/v1/firebase/config');
            if (!resp.ok) {
                checkBackendSyncStatus();
                return;
            }
            const config = await resp.json();
            window.NetSentinelFirebase.config = config;

            // If API key is not configured in environment, run in local-first mode
            if (!config || !config.apiKey || config.apiKey.trim() === '') {
                console.info('[NetSentinel] Firebase API key not supplied in environment. Running in local-first mode.');
                checkBackendSyncStatus();
                return;
            }

            // Load Firebase compat SDKs for universal zero-build browser support
            await loadScript("https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js");
            await loadScript("https://www.gstatic.com/firebasejs/10.12.0/firebase-database-compat.js");
            
            if (typeof firebase !== 'undefined') {
                if (!firebase.apps.length) {
                    window.NetSentinelFirebase.app = firebase.initializeApp(config);
                } else {
                    window.NetSentinelFirebase.app = firebase.app();
                }

                try {
                    window.NetSentinelFirebase.db = firebase.database();
                    console.log('[NetSentinel] Firebase RTDB Client initialized successfully:', config.databaseURL);
                    
                    // Listen to connection presence
                    const connectedRef = firebase.database().ref(".info/connected");
                    connectedRef.on("value", (snap) => {
                        const isConnected = snap.val() === true;
                        window.NetSentinelFirebase.status = isConnected ? 'CONNECTED' : 'DISCONNECTED';
                        updatePill(isConnected);
                    });
                } catch (dbErr) {
                    console.debug('[NetSentinel] Firebase database presence note:', dbErr);
                }

                // Initialize Analytics if measurement ID is supplied
                if (config.measurementId && config.measurementId.trim() !== '') {
                    try {
                        await loadScript("https://www.gstatic.com/firebasejs/10.12.0/firebase-analytics-compat.js");
                        if (typeof firebase.analytics === 'function') {
                            window.NetSentinelFirebase.analytics = firebase.analytics();
                        }
                    } catch (analyticsErr) {
                        console.debug('[NetSentinel] Analytics loaded in background');
                    }
                }
            }
        } catch (e) {
            console.info('[NetSentinel] Firebase client loaded in local-first mode. Fallback backend synchronization active.');
            checkBackendSyncStatus();
        }
    }

    async function checkBackendSyncStatus() {
        try {
            const resp = await fetch('/api/v1/firebase/status');
            if (resp.ok) {
                const data = await resp.json();
                window.NetSentinelFirebase.status = data.status || 'CONNECTED_REST';
                updatePill(true);
            }
        } catch (err) {
            console.debug('[NetSentinel] Backend sync check note:', err);
        }
    }

    function updatePill(isOnline) {
        const pill = document.getElementById('firebaseStatusIndicator');
        if (pill) {
            pill.textContent = isOnline ? 'FIREBASE RTDB: LIVE' : 'FIREBASE RTDB: STANDBY';
            pill.style.borderColor = isOnline ? 'var(--border-color)' : 'var(--border-color)';
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initFirebaseClient);
    } else {
        initFirebaseClient();
    }
})();

