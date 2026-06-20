export function logToScreen(message) {
    let div = document.getElementById("screen");
    if (div === null) {
        div = document.createElement("div");
        div.id = "screen";
        div.textContent = "\n";
        document.body.appendChild(div);
    }
    div.textContent += "[CONSOLE] " + message + "\n";  // ignore for run_browser_test.sh
}

export function assert(condition, message) {
    if (!condition) {
        const msg = "Assertion failed: " + (message || "unspecified error");
        logToScreen(msg);
        throw new Error(msg);
    }
}
