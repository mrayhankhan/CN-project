// Testing: (1) Open history.html (2) Confirm header shows title left and Theme/Create New right (3) Toggle theme and reload, confirm theme persists (4) Shrink viewport, confirm table scrolls and header stays visible (5) Confirm Create New button does not overlap table (6) Confirm delete buttons work and table styling changes with theme

// Fetch and display history
async function loadHistory() {
    try {
        const response = await fetch('/api/history');
        if (!response.ok) {
            throw new Error('Failed to fetch history');
        }
        
        const history = await response.json();
        displayHistory(history);
    } catch (error) {
        console.error('Error loading history:', error);
        document.getElementById('history-body').innerHTML = 
            '<tr><td colspan="7" class="error-cell">Error loading history</td></tr>';
    }
}

// Display history in table
function displayHistory(history) {
    const tbody = document.getElementById('history-body');
    
    if (history.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-cell">No history entries found</td></tr>';
        return;
    }
    
    tbody.innerHTML = '';
    
    history.forEach(entry => {
        const row = document.createElement('tr');
        if (entry.deleted) {
            row.classList.add('deleted-row');
        }
        
        // ID column with link (disabled for deleted pastes)
        const idCell = document.createElement('td');
        if (entry.deleted) {
            // Show ID as plain text for deleted pastes
            idCell.textContent = entry.id;
            idCell.style.color = 'var(--colorSecondary)';
        } else {
            // Show clickable link for active pastes
            const idLink = document.createElement('a');
            idLink.href = '/' + entry.id;
            idLink.textContent = entry.id;
            idLink.classList.add('paste-link');
            idCell.appendChild(idLink);
        }
        row.appendChild(idCell);
        
        // Time column
        const timeCell = document.createElement('td');
        const timestamp = new Date(entry.timestamp);
        timeCell.textContent = timestamp.toLocaleString();
        row.appendChild(timeCell);
        
        // Creator IP column
        const ipCell = document.createElement('td');
        ipCell.textContent = entry.creator_ip;
        row.appendChild(ipCell);
        
        // Version column
        const versionCell = document.createElement('td');
        versionCell.textContent = entry.version;
        row.appendChild(versionCell);
        
        // Action column
        const actionCell = document.createElement('td');
        actionCell.textContent = entry.action;
        actionCell.classList.add('action-cell', 'action-' + entry.action);
        row.appendChild(actionCell);
        
        // Status column
        const statusCell = document.createElement('td');
        statusCell.classList.add('status-cell');
        if (entry.deleted) {
            statusCell.textContent = 'Deleted';
            statusCell.classList.add('status-deleted');
        } else {
            statusCell.textContent = 'Active';
            statusCell.classList.add('status-active');
        }
        row.appendChild(statusCell);
        
        // Actions column
        const actionsCell = document.createElement('td');
        if (!entry.deleted) {
            const deleteBtn = document.createElement('button');
            deleteBtn.textContent = 'Delete';
            deleteBtn.className = 'btn-delete';
            deleteBtn.onclick = () => deletePaste(entry.id);
            actionsCell.appendChild(deleteBtn);
        } else {
            actionsCell.textContent = '-';
        }
        row.appendChild(actionsCell);
        
        tbody.appendChild(row);
    });
}

// Delete a paste
async function deletePaste(id) {
    if (!confirm(`Are you sure you want to delete paste ${id}?`)) {
        return;
    }
    
    try {
        const response = await fetch(`/api/history/${id}/delete`, {
            method: 'POST'
        });
        
        if (!response.ok) {
            throw new Error('Failed to delete paste');
        }
        
        // Reload history
        await loadHistory();
    } catch (error) {
        console.error('Error deleting paste:', error);
        alert('Failed to delete paste');
    }
}

// Load history on page load
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadHistory);
} else {
    loadHistory();
}
