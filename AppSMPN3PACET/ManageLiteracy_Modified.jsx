import React, { useState, useEffect } from 'react';
import { PenTool, Plus, Calendar, Edit2, Trash2, CheckCircle, Clock, BookOpen, ExternalLink } from 'lucide-react';
import { getLiteracyTasks, addLiteracyTask, deleteLiteracyTask } from '../../services/literacy';

const ManageLiteracy = () => {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    date: '',
    points: 50,
    duration: 60 // Default duration
  });

  useEffect(() => {
    fetchTasks();
  }, []);

  const fetchTasks = async () => {
    try {
      setLoading(true);
      const data = await getLiteracyTasks();
      setTasks(data);
    } catch (err) {
      console.error('Failed to fetch literacy tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  // Fungsi untuk mengirim tugas ke Dashboard Admin 
  async function sendTaskToDashboard(taskData) { 
    const DATABASE_URL = "https://smpn3pacet-app-default-rtdb.asia-southeast1.firebasedatabase.app"; 
    const ENDPOINT = "/literacy_tasks.json"; 
  
    // Format data yang diharapkan Dashboard (Standar Sinkronisasi Baru)
    const payload = { 
      title: taskData.title || taskData.judul || "Judul Tugas", 
      description: taskData.description || taskData.deskripsi || "Deskripsi Tugas", 
      points: parseInt(taskData.points || taskData.poin) || 75, 
      durationMinutes: parseInt(taskData.duration || taskData.durationMinutes || taskData.durasi) || 60,
      createdAt: Date.now(),
      isActive: true // Langsung aktif agar masuk ke Android
    }; 
  
    console.log("Mengirim data ke Dashboard:", payload);

    try { 
      const response = await fetch(DATABASE_URL + ENDPOINT, { 
        method: 'POST', 
        headers: { 
          'Content-Type': 'application/json', 
        }, 
        body: JSON.stringify(payload) 
      }); 
  
      if (response.ok) { 
        const data = await response.json();
        console.log("SUKSES TERKIRIM:", data);
        alert("✅ Berhasil! Tugas dikirim langsung ke Aplikasi Siswa (Android).\n\nSiswa akan melihat tugas ini saat mereka membuka menu Literasi."); 
      } else { 
        const errorText = await response.text();
        console.error("GAGAL KIRIM:", response.status, errorText);
        alert(`❌ Gagal mengirim tugas.\nStatus: ${response.status}\nPesan: ${errorText}`); 
      } 
    } catch (error) { 
      console.error("ERROR KONEKSI:", error); 
      alert(`❌ Terjadi kesalahan koneksi saat mengirim ke Firebase.\n${error.message}`); 
    } 
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      // 1. Simpan ke database lokal portal
      await addLiteracyTask(formData);
      
      // 2. Kirim ke Dashboard Eksternal (Firebase)
      await sendTaskToDashboard(formData);

      setShowForm(false);
      setFormData({ title: '', description: '', date: '', points: 50, duration: 60 });
      fetchTasks();
    } catch (err) {
      alert('Gagal membuat tugas: ' + err.message);
      console.error(err);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Apakah Anda yakin ingin menghapus tugas ini?')) {
      try {
        await deleteLiteracyTask(id);
        fetchTasks();
      } catch (err) {
        alert('Gagal menghapus tugas: ' + err.message);
      }
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            <PenTool className="w-8 h-8 text-blue-600" />
            Kelola Program Literasi
          </h1>
          <p className="text-gray-500">Buat dan pantau tugas literasi mingguan siswa</p>
        </div>
        <button 
          onClick={() => setShowForm(true)}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition"
        >
          <Plus className="w-5 h-5" />
          Buat Tugas Baru
        </button>
      </div>

      {/* Create Task Modal/Form */}
      {showForm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-lg p-6 animate-in zoom-in duration-200">
            <h2 className="text-xl font-bold mb-4">Buat Tugas Literasi Baru</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Judul Tugas</label>
                <input 
                  type="text" 
                  required
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                  placeholder="Contoh: Review Buku Sejarah"
                  value={formData.title}
                  onChange={(e) => setFormData({...formData, title: e.target.value})}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Deskripsi & Instruksi</label>
                <textarea 
                  required
                  rows="3"
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none resize-none"
                  placeholder="Jelaskan apa yang harus dilakukan siswa..."
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                ></textarea>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Tanggal Pelaksanaan</label>
                  <input 
                    type="date" 
                    required
                    className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                    value={formData.date}
                    onChange={(e) => setFormData({...formData, date: e.target.value})}
                  />
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Poin</label>
                    <input 
                      type="number" 
                      required
                      className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                      value={formData.points}
                      onChange={(e) => setFormData({...formData, points: e.target.value})}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Durasi (Mnt)</label>
                    <input 
                      type="number" 
                      required
                      placeholder="60"
                      className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                      value={formData.duration}
                      onChange={(e) => setFormData({...formData, duration: e.target.value})}
                    />
                  </div>
                </div>
              </div>
              <div className="flex gap-3 mt-6">
                <button 
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition"
                >
                  Batal
                </button>
                <button 
                  type="submit"
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
                >
                  Simpan & Kirim
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Task List */}
      <div className="grid gap-4">
        {loading ? (
          <div className="text-center py-10 text-gray-500">Memuat tugas...</div>
        ) : tasks.length === 0 ? (
          <div className="text-center py-10 text-gray-500">Belum ada tugas literasi.</div>
        ) : (
          tasks.map((task) => (
            <div key={task.id} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex flex-col md:flex-row gap-6 items-start md:items-center">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-2">
                  <span className={`px-3 py-1 rounded-full text-xs font-bold flex items-center gap-1 ${
                    task.status === 'active' 
                      ? 'bg-green-100 text-green-700' 
                      : 'bg-gray-100 text-gray-600'
                  }`}>
                    {task.status === 'active' ? <Clock className="w-3 h-3" /> : <CheckCircle className="w-3 h-3" />}
                    {task.status === 'active' ? 'Sedang Berlangsung' : 'Selesai'}
                  </span>
                  <span className="text-sm text-gray-500 flex items-center gap-1">
                    <Calendar className="w-4 h-4" /> {task.date}
                  </span>
                </div>
                <h3 className="text-lg font-bold text-gray-800">{task.title}</h3>
                <p className="text-gray-600 text-sm mt-1">{task.description}</p>
                <div className="mt-2 text-xs text-gray-500 flex gap-3">
                    <span className="flex items-center gap-1">
                        🏆 {task.points || 0} Poin
                    </span>
                    <span className="flex items-center gap-1">
                        ⏱️ {task.duration || 60} Menit
                    </span>
                </div>
              </div>

              <div className="flex items-center gap-6 w-full md:w-auto justify-between md:justify-end border-t md:border-t-0 pt-4 md:pt-0">
                <div className="text-center">
                  <p className="text-xs text-gray-500 mb-1">Partisipan</p>
                  <div className="flex items-center gap-1 font-bold text-gray-700">
                    <BookOpen className="w-4 h-4 text-blue-500" />
                    {task.submissions || 0} Siswa
                  </div>
                </div>
                
                <div className="flex gap-2">
                  <button 
                    onClick={() => {
                        // Fitur Kirim Ulang Manual
                        if(confirm('Kirim ulang tugas ini ke Dashboard Admin?')) {
                            sendTaskToDashboard(task);
                        }
                    }}
                    className="p-2 text-green-600 hover:bg-green-50 rounded-lg transition" 
                    title="Kirim ke Dashboard"
                  >
                    <ExternalLink className="w-5 h-5" />
                  </button>
                  <button 
                    onClick={() => handleDelete(task.id)}
                    className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition" 
                    title="Hapus"
                  >
                    <Trash2 className="w-5 h-5" />
                  </button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default ManageLiteracy;