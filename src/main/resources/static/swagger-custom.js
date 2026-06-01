// Auto-apply token to Swagger UI when generated
(function() {
    console.log("Swagger Auto-Token Helper Loaded");

    // If the group is client-api, hide the topbar so clients cannot switch groups
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('group') === 'client-api') {
        const hideStyle = document.createElement('style');
        hideStyle.textContent = `
            .swagger-ui .topbar {
                display: none !important;
            }
        `;
        document.head.appendChild(hideStyle);
        console.log("Client view active: topbar hidden.");
    }

    // Function to set the token in Swagger UI
    function setAuthToken(token) {
        if (!token) return false;

        // Try to find Swagger UI instance
        if (window.ui) {
            try {
                // Try the standard Swagger UI preauthorizeApiKey method first
                if (typeof window.ui.preauthorizeApiKey === 'function') {
                    window.ui.preauthorizeApiKey("BearerAuth", token);
                    console.log("Token automatically applied via preauthorizeApiKey!");
                    showNotification("Token applied successfully!", "success");
                    return true;
                } else if (window.ui.authorizations && typeof window.ui.authorizations.add === 'function') {
                    // Fallback to legacy authorizations.add
                    window.ui.authorizations.add("BearerAuth", {
                        name: "Authorization",
                        value: "Bearer " + token,
                        schema: {
                            type: "http",
                            scheme: "bearer",
                            bearerFormat: "AES Token"
                        }
                    });
                    console.log("Token automatically applied via authorizations.add!");
                    showNotification("Token applied successfully!", "success");
                    return true;
                }
            } catch(e) {
                console.log("Swagger UI authorization failed:", e);
            }
        }

        // Method 2: Store in localStorage for persistence
        localStorage.setItem('swagger_token', token);
        localStorage.setItem('swagger_token_type', 'BearerAuth');

        // Method 3: Try to find and click authorize button
        setTimeout(() => {
            const authorizeBtn = document.querySelector('.auth-wrapper .btn-authorize');
            if (authorizeBtn) {
                authorizeBtn.click();
                setTimeout(() => {
                    const authInput = document.querySelector('.modal-dialog input[placeholder*="Bearer"]');
                    if (authInput) {
                        authInput.value = token;
                        const authBtn = document.querySelector('.modal-dialog .btn.modal-btn-auth');
                        if (authBtn) authBtn.click();
                        const closeBtn = document.querySelector('.modal-dialog .btn.cancel');
                        if (closeBtn) closeBtn.click();
                    }
                }, 500);
            }
        }, 1000);

        return true;
    }

    // Function to show notification
    function showNotification(message, type) {
        const notification = document.createElement('div');
        notification.style.cssText = `
            position: fixed;
            top: 70px;
            right: 20px;
            padding: 12px 20px;
            background: ${type === 'success' ? '#4caf50' : '#2196f3'};
            color: white;
            border-radius: 4px;
            z-index: 10000;
            font-family: monospace;
            box-shadow: 0 2px 10px rgba(0,0,0,0.2);
            animation: slideIn 0.3s ease-out;
        `;
        notification.textContent = message;
        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 3000);
    }

    // Intercept fetch requests to capture token from response
    const originalFetch = window.fetch;
    window.fetch = function(...args) {
        return originalFetch.apply(this, args).then(async (response) => {
            // Check if this is a generate-token request
            if (args[0] && args[0].includes('/api/aes/generate-token')) {
                const clonedResponse = response.clone();
                try {
                    const data = await clonedResponse.json();
                    if (data.token) {
                        console.log("Token captured from response!");
                        setAuthToken(data.token);
                        showNotification("Token auto-applied to all APIs!", "success");
                    }
                } catch(e) {
                    console.log("Error parsing token response:", e);
                }
            }
            return response;
        });
    };

    // Try to load saved token on page load
    window.addEventListener('load', () => {
        setTimeout(() => {
            const savedToken = localStorage.getItem('swagger_token');
            if (savedToken) {
                console.log("Found saved token, applying...");
                setAuthToken(savedToken);
                showNotification("Saved token restored!", "info");
            }
        }, 2000);
    });

    // Add CSS animation
    const style = document.createElement('style');
    style.textContent = `
        @keyframes slideIn {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
    `;
    document.head.appendChild(style);

    console.log("Auto-token helper ready!");
})();