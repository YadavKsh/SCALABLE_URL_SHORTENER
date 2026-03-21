/* ============================================================
   app.js — All client-side logic for the URL Shortener UI.
   ============================================================ */

const API_URL = "http://localhost:8081/shorten";

/* ── Shorten URL ──────────────────────────────────────────── */

async function shortenUrl() {
    const input    = document.getElementById("urlInput").value.trim();
    const btn      = document.getElementById("submitBtn");
    const btnText  = document.getElementById("btnText");
    const spinner  = document.getElementById("spinner");
    const errorMsg = document.getElementById("errorMsg");
    const result   = document.getElementById("result");

    // Reset previous state
    errorMsg.style.display = "none";
    result.style.display   = "none";

    // Client-side validation
    if (!input) {
        showError("Please enter a URL before shortening.");
        document.getElementById("urlInput").focus();
        return;
    }

    if (!input.startsWith("http://") && !input.startsWith("https://")) {
        showError("URL must start with http:// or https://");
        return;
    }

    // Show loading state
    btn.disabled           = true;
    btnText.style.display  = "none";
    spinner.style.display  = "block";

    try {
        const response = await fetch(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ url: input })
        });

        const data = await response.json();

        if (response.ok) {
            const shortUrlEl  = document.getElementById("shortUrl");
            shortUrlEl.href   = data.shortUrl;
            shortUrlEl.innerText = data.shortUrl;

            // Re-trigger the result animation on each new result
            result.style.animation = "none";
            result.offsetHeight;    // force reflow
            result.style.animation = "";
            result.style.display   = "block";
        } else {
            showError(data.error || "Something went wrong. Please try again.");
        }

    } catch (err) {
        showError("Cannot reach the server. Make sure the backend is running.");
    } finally {
        // Always restore button to its default state
        btn.disabled          = false;
        btnText.style.display = "block";
        spinner.style.display = "none";
    }
}

/* ── Copy to Clipboard ────────────────────────────────────── */

async function copyUrl() {
    const url    = document.getElementById("shortUrl").href;
    const btn    = document.getElementById("copyBtn");
    const ICON_COPY = `
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
        </svg>`;
    const ICON_CHECK = `
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" stroke-width="2.5"
             stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"/>
        </svg>`;

    try {
        await navigator.clipboard.writeText(url);
        btn.classList.add("copied");
        btn.innerHTML = ICON_CHECK;

        setTimeout(() => {
            btn.classList.remove("copied");
            btn.innerHTML = ICON_COPY;
        }, 2000);

    } catch {
        showError("Could not copy to clipboard.");
    }
}

/* ── Helpers ──────────────────────────────────────────────── */

function showError(message) {
    const el      = document.getElementById("errorMsg");
    el.textContent = "⚠ " + message;
    el.style.display = "block";
}

/* Allow submitting with the Enter key */
document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("urlInput")
        .addEventListener("keydown", (e) => {
            if (e.key === "Enter") shortenUrl();
        });
});