// Configuration - Update this after deploying the backend
const BASE_URL = window.location.origin; // Change to your deployed backend URL, e.g., 'https://your-backend.app'

// Theme management
function initializeTheme() {
    const savedTheme = localStorage.getItem('pasteTheme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const theme = savedTheme || (prefersDark ? 'dark' : 'light');
    
    if (theme === 'dark') {
        document.body.classList.add('darkTheme');
        document.body.classList.remove('lightTheme');
    } else {
        document.body.classList.add('lightTheme');
        document.body.classList.remove('darkTheme');
    }
    
    const themeToggle = document.getElementById('themeToggle');
    if (themeToggle) {
        themeToggle.setAttribute('aria-pressed', theme === 'dark' ? 'true' : 'false');
        themeToggle.addEventListener('click', toggleTheme);
    }
}

function toggleTheme() {
    const isDark = document.body.classList.toggle('darkTheme');
    if (isDark) {
        document.body.classList.remove('lightTheme');
    } else {
        document.body.classList.add('lightTheme');
    }
    localStorage.setItem('pasteTheme', isDark ? 'dark' : 'light');
    const themeToggle = document.getElementById('themeToggle');
    if (themeToggle) {
        themeToggle.setAttribute('aria-pressed', isDark ? 'true' : 'false');
    }
}

// Adjust textarea height dynamically
function adjustTextareaHeight() {
    const textarea = document.getElementById('pasteInput') || document.getElementById('paste-editor');
    if (textarea && textarea.parentElement) {
        requestAnimationFrame(() => {
            const container = textarea.closest('.editorContainer');
            if (container) {
                const rect = container.getBoundingClientRect();
                const buttonContainer = container.querySelector('.button-container');
                const buttonHeight = buttonContainer ? buttonContainer.offsetHeight : 0;
                const availableHeight = rect.height - buttonHeight - 32;
                if (availableHeight > 100) {
                    textarea.style.height = availableHeight + 'px';
                }
            }
        });
    }
}

// Info button navigation
function initializeInfoButton() {
    const infoButton = document.getElementById('infoButton');
    if (infoButton) {
        infoButton.addEventListener('click', () => {
            location.href = '/about';
        });
    }
}

// Initialize theme on load
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        initializeTheme();
        initializeInfoButton();
    });
} else {
    initializeTheme();
    initializeInfoButton();
}

// Adjust height on resize
window.addEventListener('resize', adjustTextareaHeight);
window.addEventListener('load', adjustTextareaHeight);

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
const userCountElement = document.getElementById('user-count');

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
        const response = await fetch(`${BASE_URL}/api/${pasteId}`);
        if (!response.ok) {
            throw new Error('Paste not found');
        }
        
        const data = await response.json();
        originalContent = data.text;
        displayContent(originalContent);
        
        // Check if paste is deleted
        if (data.deleted || (window.pasteStatus && window.pasteStatus.deleted)) {
            showDeletedState();
        }
    } catch (error) {
        console.error('Error loading paste:', error);
        pasteDisplay.textContent = 'Error: Paste not found';
    }
}

// Show deleted state UI
function showDeletedState() {
    const deletedBanner = document.getElementById('deletedBanner');
    if (deletedBanner) {
        deletedBanner.classList.remove('hidden');
    }
    
    // Disable edit button
    if (editButton) {
        editButton.disabled = true;
        editButton.title = 'Cannot edit deleted paste';
    }
    
    // Disable textarea
    if (pasteEditor) {
        pasteEditor.disabled = true;
    }
    
    console.log('Paste is deleted - read-only mode');
}

// Display content in view mode
function displayContent(content) {
    pasteDisplay.textContent = content;
}

// Update online users count
function updateUserCount(count) {
    if (userCountElement) {
        userCountElement.textContent = count;
    }
}

// Connect to WebSocket for real-time updates
function connectWebSocket() {
    const baseUrlObj = new URL(BASE_URL);
    const protocol = baseUrlObj.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${baseUrlObj.host}/ws/${pasteId}`;
    
    try {
        ws = new WebSocket(wsUrl);
        
        ws.onopen = () => {
            console.log('WebSocket connected');
            statusIndicator.textContent = '● Connected';
            statusIndicator.className = 'connected';
            reconnectAttempts = 0;
        };
        
        ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                
                if (message.type === 'init' || message.type === 'update') {
                    const content = message.text;
                    
                    // Update display if not currently editing
                    if (!isEditing) {
                        originalContent = content;
                        displayContent(content);
                    } else {
                        // Update original content but don't override editor
                        originalContent = content;
                    }
                } else if (message.type === 'userCount') {
                    // Update online users count
                    updateUserCount(message.count);
                }
            } catch (e) {
                // Fallback for non-JSON messages (backward compatibility)
                const content = event.data;
                if (!isEditing) {
                    originalContent = content;
                    displayContent(content);
                } else {
                    originalContent = content;
                }
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
        
        ws.onclose = (event) => {
            console.log('WebSocket closed:', event.code, event.reason);
            
            // If closed immediately (likely due to deleted paste), show deleted state
            if (event.code === 1006 || event.code === 1008) {
                showDeletedState();
            }
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
    // Check if paste is deleted
    if (editButton && editButton.disabled) {
        return; // Don't allow editing deleted pastes
    }
    
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
        const response = await fetch(`${BASE_URL}/${pasteId}`, {
            method: 'PUT',
            body: content
        });
        
        if (response.status === 410) {
            // Paste has been deleted
            showDeletedState();
            exitEditMode();
            alert('This paste has been deleted and cannot be edited');
            return;
        }
        
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
