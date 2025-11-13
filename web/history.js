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
            '<tr><td colspan="7" style="text-align: center; color: #ef4444;">Error loading history</td></tr>';
    }
}

// Display history in table
function displayHistory(history) {
    const tbody = document.getElementById('history-body');
    
    if (history.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">No history entries found</td></tr>';
        return;
    }
    
    tbody.innerHTML = '';
    
    history.forEach(entry => {
        const row = document.createElement('tr');
        
        // ID column with link
        const idCell = document.createElement('td');
        const idLink = document.createElement('a');
        idLink.href = '/' + entry.id;
        idLink.textContent = entry.id;
        idLink.style.color = '#4a9eff';
        idLink.style.textDecoration = 'none';
        idCell.appendChild(idLink);
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
        actionCell.style.textTransform = 'capitalize';
        if (entry.action === 'create') {
            actionCell.style.color = '#4ade80';
        } else if (entry.action === 'update') {
            actionCell.style.color = '#fbbf24';
        } else if (entry.action === 'delete') {
            actionCell.style.color = '#ef4444';
        }
        row.appendChild(actionCell);
        
        // Status column
        const statusCell = document.createElement('td');
        if (entry.deleted) {
            statusCell.textContent = 'Deleted';
            statusCell.style.color = '#ef4444';
            row.style.opacity = '0.6';
        } else {
            statusCell.textContent = 'Active';
            statusCell.style.color = '#4ade80';
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
document.addEventListener('DOMContentLoaded', loadHistory);
