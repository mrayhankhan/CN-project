// Robust history loading with detailed error handling and logging
(() => {
  // Configuration - Update this after deploying the backend
  const API_BASE = 'https://cn-project-production.up.railway.app';
  const base = API_BASE === 'YOUR_BACKEND_HOST' ? window.location.origin : API_BASE;
  const api = base + '/api/history';
  const tableBody = document.querySelector('#history-body');
  
  const loadingRow = () => {
    tableBody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:30px">Loading...</td></tr>';
  };
  
  const showError = msg => {
    tableBody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:30px;color:var(--colorAccent)">' + msg + '</td></tr>';
    console.error('history error', msg);
  };
  
  async function loadHistory() {
    try {
      loadingRow();
      console.log('fetching', api);
      const res = await fetch(api);
      console.log('response status', res.status);
      
      if (!res.ok) {
        const text = await res.text();
        showError('Server returned status ' + res.status + ' message: ' + text);
        return;
      }
      
      const data = await res.json();
      console.log('received data:', data);
      
      if (!Array.isArray(data)) {
        showError('Invalid response format - expected array');
        console.log('invalid payload', data);
        return;
      }
      
      if (data.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:30px">No history entries found</td></tr>';
        return;
      }
      
      tableBody.innerHTML = '';
      data.forEach(item => {
        const row = document.createElement('tr');
        if (item.deleted) {
          row.classList.add('deleted-row');
        }
        
        // ID column
        const idCell = document.createElement('td');
        if (item.deleted) {
          idCell.textContent = item.id;
          idCell.style.color = 'var(--colorSecondary)';
        } else {
          const link = document.createElement('a');
          link.href = 'view.html?' + item.id;
          link.textContent = item.id;
          link.classList.add('paste-link');
          idCell.appendChild(link);
        }
        row.appendChild(idCell);
        
        // Time column
        const timeCell = document.createElement('td');
        const timestamp = new Date(item.timestamp);
        timeCell.textContent = timestamp.toLocaleString();
        row.appendChild(timeCell);
        
        // Creator IP column
        const ipCell = document.createElement('td');
        ipCell.textContent = item.creator_ip || '';
        row.appendChild(ipCell);
        
        // Version column
        const versionCell = document.createElement('td');
        versionCell.textContent = item.version || '';
        row.appendChild(versionCell);
        
        // Action column
        const actionCell = document.createElement('td');
        actionCell.textContent = item.action || '';
        actionCell.classList.add('action-cell', 'action-' + (item.action || ''));
        row.appendChild(actionCell);
        
        // Status column
        const statusCell = document.createElement('td');
        statusCell.classList.add('status-cell');
        if (item.deleted) {
          statusCell.textContent = 'Deleted';
          statusCell.classList.add('status-deleted');
        } else {
          statusCell.textContent = 'Active';
          statusCell.classList.add('status-active');
        }
        row.appendChild(statusCell);
        
        // Actions column
        const actionsCell = document.createElement('td');
        if (!item.deleted) {
          const deleteBtn = document.createElement('button');
          deleteBtn.textContent = 'Delete';
          deleteBtn.className = 'btn-delete';
          deleteBtn.setAttribute('data-id', item.id);
          actionsCell.appendChild(deleteBtn);
        } else {
          actionsCell.textContent = '-';
        }
        row.appendChild(actionsCell);
        
        tableBody.appendChild(row);
      });
      
      console.log('Successfully loaded', data.length, 'history entries');
      
    } catch (e) {
      console.error('Exception loading history:', e);
      showError('Network or parsing error. See console for details: ' + e.message);
    }
  }
  
  // Load history on page load
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadHistory);
  } else {
    loadHistory();
  }
  
  // Handle delete button clicks
  document.body.addEventListener('click', e => {
    const t = e.target;
    if (t && t.classList && t.classList.contains('btn-delete')) {
      const id = t.getAttribute('data-id');
      if (!confirm('Are you sure you want to delete paste ' + id + '?')) {
        return;
      }
      
      fetch(window.location.origin + '/api/history/' + id + '/delete', { method: 'POST' })
        .then(r => {
          if (r.ok) {
            loadHistory();
          } else {
            r.text().then(t => alert('Delete failed: ' + t));
          }
        })
        .catch(err => {
          console.error('Delete error:', err);
          alert('Network error during delete');
        });
    }
  });
  
  // Load theme from app.js if available
  if (typeof initializeTheme === 'function') {
    initializeTheme();
  }
  if (typeof initializeInfoButton === 'function') {
    initializeInfoButton();
  }
})();
