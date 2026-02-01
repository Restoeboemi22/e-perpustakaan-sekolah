// URL Database Firebase
const DATABASE_URL = "https://smpn3pacet-app-default-rtdb.asia-southeast1.firebasedatabase.app";
const ENDPOINT = "/literacy_tasks.json";

// Load Tasks on Startup
document.addEventListener('DOMContentLoaded', loadTasks);

// Attach event listener to Refresh button
document.getElementById('refreshBtn').addEventListener('click', loadTasks);

// Expose deleteTask to global scope for HTML onclick (or attach event listeners dynamically)
// Since we are using module, onclick="deleteTask()" won't work directly.
// We'll use event delegation or attach it to window.
window.deleteTask = deleteTask;

async function loadTasks() {
    try {
        const response = await fetch(DATABASE_URL + ENDPOINT);
        if (!response.ok) throw new Error('Gagal mengambil data');
        
        const data = await response.json();
        renderTasks(data);
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('taskList').innerHTML = `<tr><td colspan="5" class="text-danger text-center">Gagal koneksi ke Firebase: ${error.message}</td></tr>`;
    }
}

function renderTasks(data) {
    const tbody = document.getElementById('taskList');
    tbody.innerHTML = '';
    
    if (!data) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">Belum ada tugas di Firebase.</td></tr>';
        return;
    }

    // Firebase mengembalikan object dengan key=ID, kita ubah jadi array
    Object.keys(data).forEach(key => {
        const task = data[key];
        const row = `
            <tr>
                <td><strong>${task.title}</strong></td>
                <td>${task.description}</td>
                <td><span class="badge bg-warning text-dark">${task.points} Poin</span></td>
                <td>${task.durationMinutes} Menit</td>
                <td>
                    <button onclick="deleteTask('${key}')" class="btn btn-sm btn-danger">Hapus</button>
                </td>
            </tr>
        `;
        tbody.innerHTML += row;
    });
}

// Handle Form Submit
document.getElementById('addTaskForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const payload = {
        title: document.getElementById('title').value,
        description: document.getElementById('description').value,
        points: parseInt(document.getElementById('points').value) || 75,
        durationMinutes: parseInt(document.getElementById('duration').value) || 60,
        createdAt: Date.now(),
        isActive: true
    };

    try {
        const response = await fetch(DATABASE_URL + ENDPOINT, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert('✅ Tugas Berhasil Dikirim ke Siswa!');
            document.getElementById('addTaskForm').reset();
            loadTasks(); // Reload list
        } else {
            const errorText = await response.text();
            alert('❌ Gagal mengirim: ' + errorText);
            console.error(errorText);
        }
    } catch (error) {
        alert('Error Koneksi: ' + error.message);
    }
});

// Handle Delete
async function deleteTask(id) {
    if (!confirm('Yakin ingin menghapus tugas ini dari Aplikasi Siswa?')) return;

    try {
        // Endpoint delete spesifik ID
        const deleteUrl = `${DATABASE_URL}/literacy_tasks/${id}.json`;
        
        const response = await fetch(deleteUrl, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadTasks();
        } else {
            alert('Gagal menghapus tugas');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}