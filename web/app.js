// Extract paste ID from URL
const pasteId = window.location.pathname.substring(1);

// DOM elements
const pasteIdElement = document.getElementById('paste-id');
const viewMode = document.getElementById('view-mode');
const pasteDisplay = document.getElementById('paste-display');
const pasteEditor = document.getElementById('paste-editor');
const editButton = document.getElementById('edit-button');
const saveButton = document.getElementById('save-button');
const cancelButton = document.getElementById('cancel-button');
const statusIndicator = document.getElementById('status-indicator');

// State
let isEditing = false;
let originalContent = '';
let ws = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;

// Initialize
if (pasteId) {
    pasteIdElement.textContent = pasteId;
    loadPaste();
    connectWebSocket();
}

// Load paste content
async function loadPaste() {
    try {
        const response = await fetch(`/api/${pasteId}`);
        if (!response.ok) {
            throw new Error('Paste not found');
        }
        
        const data = await response.json();
        originalContent = data.text;
        displayContent(originalContent);
    } catch (error) {
        console.error('Error loading paste:', error);
        pasteDisplay.textContent = 'Error: Paste not found';
    }
}

// Display content in view mode
function displayContent(content) {
    pasteDisplay.textContent = content;
}

// Connect to WebSocket for real-time updates
function connectWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/${pasteId}`;
    
    try {
        ws = new WebSocket(wsUrl);
        
        ws.onopen = () => {
            console.log('WebSocket connected');
            statusIndicator.textContent = '● Connected';
            statusIndicator.className = 'connected';
            reconnectAttempts = 0;
        };
        
        ws.onmessage = (event) => {
            const content = event.data;
            
            // Update display if not currently editing
            if (!isEditing) {
                originalContent = content;
                displayContent(content);
            } else {
                // Update original content but don't override editor
                originalContent = content;
            }
        };
        
        ws.onclose = () => {
            console.log('WebSocket disconnected');
            statusIndicator.textContent = '● Disconnected';
            statusIndicator.className = 'disconnected';
            
            // Attempt to reconnect
            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                reconnectAttempts++;
                setTimeout(() => {
                    console.log(`Reconnecting... (attempt ${reconnectAttempts})`);
                    connectWebSocket();
                }, 2000 * reconnectAttempts);
            }
        };
        
        ws.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
    } catch (error) {
        console.error('Failed to create WebSocket:', error);
        statusIndicator.textContent = '● Disconnected';
        statusIndicator.className = 'disconnected';
    }
}

// Edit button click
editButton.addEventListener('click', () => {
    enterEditMode();
});

// Save button click
saveButton.addEventListener('click', async () => {
    await saveChanges();
});

// Cancel button click
cancelButton.addEventListener('click', () => {
    exitEditMode();
});

// Enter edit mode
function enterEditMode() {
    isEditing = true;
    pasteEditor.value = originalContent;
    
    viewMode.classList.add('hidden');
    pasteEditor.classList.remove('hidden');
    editButton.classList.add('hidden');
    saveButton.classList.remove('hidden');
    cancelButton.classList.remove('hidden');
    
    pasteEditor.focus();
}

// Exit edit mode
function exitEditMode() {
    isEditing = false;
    
    viewMode.classList.remove('hidden');
    pasteEditor.classList.add('hidden');
    editButton.classList.remove('hidden');
    saveButton.classList.add('hidden');
    cancelButton.classList.add('hidden');
    
    displayContent(originalContent);
}

// Save changes via HTTP PUT
async function saveChanges() {
    const content = pasteEditor.value;
    
    if (!content.trim()) {
        alert('Content cannot be empty');
        return;
    }
    
    saveButton.disabled = true;
    saveButton.textContent = 'Saving...';
    
    try {
        const response = await fetch(`/${pasteId}`, {
            method: 'PUT',
            body: content
        });
        
        if (!response.ok) {
            throw new Error('Failed to save');
        }
        
        originalContent = content;
        
        // Send update via WebSocket if connected
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(content);
        }
        
        exitEditMode();
    } catch (error) {
        console.error('Error saving paste:', error);
        alert('Failed to save changes');
    } finally {
        saveButton.disabled = false;
        saveButton.textContent = 'Save';
    }
}

// Handle keyboard shortcuts
document.addEventListener('keydown', (e) => {
    // Ctrl+E to toggle edit mode
    if (e.ctrlKey && e.key === 'e') {
        e.preventDefault();
        if (isEditing) {
            exitEditMode();
        } else {
            enterEditMode();
        }
    }
    
    // Ctrl+S to save
    if (e.ctrlKey && e.key === 's') {
        e.preventDefault();
        if (isEditing) {
            saveChanges();
        }
    }
    
    // Escape to cancel
    if (e.key === 'Escape' && isEditing) {
        exitEditMode();
    }
});
