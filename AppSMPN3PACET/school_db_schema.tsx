import React, { useState } from 'react';
import { Database, Table, Key, Users, BookOpen, Calendar, Award, Shield, Bell } from 'lucide-react';

const DatabaseSchema = () => {
  const [selectedTable, setSelectedTable] = useState(null);

  const tables = {
    users: {
      icon: Users,
      color: 'bg-blue-500',
      name: 'users',
      description: 'Tabel utama untuk semua user (siswa, guru, admin)',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key, auto increment' },
        { name: 'username', type: 'VARCHAR(50)', key: 'UNIQUE', desc: 'Username untuk login' },
        { name: 'password', type: 'VARCHAR(255)', key: '', desc: 'Password ter-hash (bcrypt)' },
        { name: 'email', type: 'VARCHAR(100)', key: 'UNIQUE', desc: 'Email user' },
        { name: 'full_name', type: 'VARCHAR(100)', key: '', desc: 'Nama lengkap' },
        { name: 'role', type: 'ENUM', key: '', desc: 'student, teacher, admin' },
        { name: 'nis_nip', type: 'VARCHAR(20)', key: 'UNIQUE', desc: 'NIS untuk siswa, NIP untuk guru' },
        { name: 'phone', type: 'VARCHAR(15)', key: '', desc: 'Nomor telepon' },
        { name: 'address', type: 'TEXT', key: '', desc: 'Alamat lengkap' },
        { name: 'gender', type: 'ENUM', key: '', desc: 'male, female' },
        { name: 'birth_date', type: 'DATE', key: '', desc: 'Tanggal lahir' },
        { name: 'profile_picture', type: 'VARCHAR(255)', key: '', desc: 'URL foto profil' },
        { name: 'is_active', type: 'BOOLEAN', key: '', desc: 'Status aktif akun' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu pembuatan' },
        { name: 'updated_at', type: 'TIMESTAMP', key: '', desc: 'Waktu update terakhir' }
      ]
    },
    students: {
      icon: Users,
      color: 'bg-green-500',
      name: 'students',
      description: 'Detail tambahan khusus siswa',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'user_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke users.id' },
        { name: 'class_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke classes.id' },
        { name: 'parent_name', type: 'VARCHAR(100)', key: '', desc: 'Nama orang tua/wali' },
        { name: 'parent_phone', type: 'VARCHAR(15)', key: '', desc: 'Telepon orang tua' },
        { name: 'parent_email', type: 'VARCHAR(100)', key: '', desc: 'Email orang tua' },
        { name: 'admission_year', type: 'YEAR', key: '', desc: 'Tahun masuk' },
        { name: 'discipline_points', type: 'INTEGER', key: '', desc: 'Total poin kedisiplinan' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu pembuatan' },
        { name: 'updated_at', type: 'TIMESTAMP', key: '', desc: 'Waktu update terakhir' }
      ]
    },
    teachers: {
      icon: Users,
      color: 'bg-purple-500',
      name: 'teachers',
      description: 'Detail tambahan khusus guru',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'user_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke users.id' },
        { name: 'subject', type: 'VARCHAR(50)', key: '', desc: 'Mata pelajaran yang diampu' },
        { name: 'is_homeroom_teacher', type: 'BOOLEAN', key: '', desc: 'Apakah wali kelas' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu pembuatan' },
        { name: 'updated_at', type: 'TIMESTAMP', key: '', desc: 'Waktu update terakhir' }
      ]
    },
    classes: {
      icon: Users,
      color: 'bg-yellow-500',
      name: 'classes',
      description: 'Data kelas (7A, 8B, 9C, dll)',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'class_name', type: 'VARCHAR(10)', key: 'UNIQUE', desc: 'Nama kelas: 7A, 8B, 9C' },
        { name: 'grade_level', type: 'INTEGER', key: '', desc: 'Tingkat: 7, 8, atau 9' },
        { name: 'homeroom_teacher_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke teachers.id' },
        { name: 'academic_year', type: 'VARCHAR(9)', key: '', desc: 'Tahun ajaran: 2024/2025' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu pembuatan' },
        { name: 'updated_at', type: 'TIMESTAMP', key: '', desc: 'Waktu update terakhir' }
      ]
    },
    books: {
      icon: BookOpen,
      color: 'bg-pink-500',
      name: 'books',
      description: 'Master data buku perpustakaan',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'isbn', type: 'VARCHAR(20)', key: 'UNIQUE', desc: 'ISBN buku' },
        { name: 'title', type: 'VARCHAR(200)', key: '', desc: 'Judul buku' },
        { name: 'author', type: 'VARCHAR(100)', key: '', desc: 'Penulis' },
        { name: 'publisher', type: 'VARCHAR(100)', key: '', desc: 'Penerbit' },
        { name: 'publication_year', type: 'YEAR', key: '', desc: 'Tahun terbit' },
        { name: 'category', type: 'VARCHAR(50)', key: '', desc: 'Kategori: Fiksi, Sains, dll' },
        { name: 'cover_image', type: 'VARCHAR(255)', key: '', desc: 'URL cover buku' },
        { name: 'description', type: 'TEXT', key: '', desc: 'Deskripsi buku' },
        { name: 'total_copies', type: 'INTEGER', key: '', desc: 'Total eksemplar' },
        { name: 'available_copies', type: 'INTEGER', key: '', desc: 'Eksemplar tersedia' },
        { name: 'location', type: 'VARCHAR(50)', key: '', desc: 'Lokasi rak buku' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu pembuatan' },
        { name: 'updated_at', type: 'TIMESTAMP', key: '', desc: 'Waktu update terakhir' }
      ]
    },
    book_loans: {
      icon: BookOpen,
      color: 'bg-indigo-500',
      name: 'book_loans',
      description: 'Riwayat peminjaman buku',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'book_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke books.id' },
        { name: 'student_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke students.id' },
        { name: 'loan_date', type: 'DATE', key: '', desc: 'Tanggal pinjam' },
        { name: 'due_date', type: 'DATE', key: '', desc: 'Tanggal jatuh tempo' },
        { name: 'return_date', type: 'DATE', key: '', desc: 'Tanggal pengembalian (NULL jika belum)' },
        { name: 'status', type: 'ENUM', key: '', desc: 'borrowed, returned, overdue' },
        { name: 'fine_amount', type: 'DECIMAL(10,2)', key: '', desc: 'Denda keterlambatan' },
        { name: 'notes', type: 'TEXT', key: '', desc: 'Catatan tambahan' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu pembuatan' },
        { name: 'updated_at', type: 'TIMESTAMP', key: '', desc: 'Waktu update terakhir' }
      ]
    },
    attendances: {
      icon: Calendar,
      color: 'bg-teal-500',
      name: 'attendances',
      description: 'Data absensi siswa harian',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'student_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke students.id' },
        { name: 'date', type: 'DATE', key: '', desc: 'Tanggal absensi' },
        { name: 'status', type: 'ENUM', key: '', desc: 'present, absent, late, sick, permit' },
        { name: 'check_in_time', type: 'TIME', key: '', desc: 'Waktu check-in' },
        { name: 'check_in_method', type: 'ENUM', key: '', desc: 'qr_code, manual, nfc' },
        { name: 'notes', type: 'TEXT', key: '', desc: 'Catatan/alasan' },
        { name: 'proof_document', type: 'VARCHAR(255)', key: '', desc: 'URL surat izin/dokter' },
        { name: 'recorded_by', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke users.id (guru)' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu pembuatan' },
        { name: 'updated_at', type: 'TIMESTAMP', key: '', desc: 'Waktu update terakhir' }
      ]
    },
    discipline_rules: {
      icon: Shield,
      color: 'bg-red-500',
      name: 'discipline_rules',
      description: 'Master aturan & poin pelanggaran/prestasi',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'rule_name', type: 'VARCHAR(100)', key: '', desc: 'Nama aturan' },
        { name: 'category', type: 'ENUM', key: '', desc: 'violation, achievement' },
        { name: 'points', type: 'INTEGER', key: '', desc: 'Poin (negatif untuk pelanggaran)' },
        { name: 'severity', type: 'ENUM', key: '', desc: 'low, medium, high, critical' },
        { name: 'description', type: 'TEXT', key: '', desc: 'Deskripsi aturan' },
        { name: 'is_active', type: 'BOOLEAN', key: '', desc: 'Status aktif aturan' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu pembuatan' },
        { name: 'updated_at', type: 'TIMESTAMP', key: '', desc: 'Waktu update terakhir' }
      ]
    },
    discipline_records: {
      icon: Shield,
      color: 'bg-orange-500',
      name: 'discipline_records',
      description: 'Catatan pelanggaran/prestasi siswa',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'student_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke students.id' },
        { name: 'rule_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke discipline_rules.id' },
        { name: 'date', type: 'DATE', key: '', desc: 'Tanggal kejadian' },
        { name: 'points', type: 'INTEGER', key: '', desc: 'Poin yang didapat' },
        { name: 'description', type: 'TEXT', key: '', desc: 'Detail kejadian' },
        { name: 'evidence', type: 'VARCHAR(255)', key: '', desc: 'URL foto/dokumen bukti' },
        { name: 'recorded_by', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke users.id (guru)' },
        { name: 'status', type: 'ENUM', key: '', desc: 'pending, approved, rejected' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu pembuatan' },
        { name: 'updated_at', type: 'TIMESTAMP', key: '', desc: 'Waktu update terakhir' }
      ]
    },
    virtual_pets: {
      icon: Award,
      color: 'bg-cyan-500',
      name: 'virtual_pets',
      description: 'Data virtual pet tiap siswa',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'student_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke students.id (UNIQUE)' },
        { name: 'pet_name', type: 'VARCHAR(50)', key: '', desc: 'Nama pet yang dipilih siswa' },
        { name: 'pet_type', type: 'VARCHAR(50)', key: '', desc: 'Jenis pet: cat, dog, dragon, dll' },
        { name: 'level', type: 'INTEGER', key: '', desc: 'Level pet (1-100)' },
        { name: 'experience_points', type: 'INTEGER', key: '', desc: 'XP pet' },
        { name: 'health', type: 'INTEGER', key: '', desc: 'HP pet (0-100)' },
        { name: 'happiness', type: 'INTEGER', key: '', desc: 'Happiness (0-100)' },
        { name: 'hunger', type: 'INTEGER', key: '', desc: 'Hunger level (0-100)' },
        { name: 'last_fed', type: 'TIMESTAMP', key: '', desc: 'Terakhir diberi makan' },
        { name: 'last_played', type: 'TIMESTAMP', key: '', desc: 'Terakhir diajak main' },
        { name: 'status', type: 'ENUM', key: '', desc: 'healthy, sick, happy, sad, dead' },
        { name: 'accessories', type: 'JSON', key: '', desc: 'Array accessories yang dimiliki' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu pembuatan' },
        { name: 'updated_at', type: 'TIMESTAMP', key: '', desc: 'Waktu update terakhir' }
      ]
    },
    pet_activities: {
      icon: Award,
      color: 'bg-lime-500',
      name: 'pet_activities',
      description: 'Log aktivitas yang memberi XP ke pet',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'pet_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke virtual_pets.id' },
        { name: 'activity_type', type: 'ENUM', key: '', desc: 'study, attendance, discipline, library' },
        { name: 'activity_description', type: 'TEXT', key: '', desc: 'Deskripsi aktivitas' },
        { name: 'xp_earned', type: 'INTEGER', key: '', desc: 'XP yang didapat' },
        { name: 'health_change', type: 'INTEGER', key: '', desc: 'Perubahan HP (+/-)' },
        { name: 'happiness_change', type: 'INTEGER', key: '', desc: 'Perubahan happiness (+/-)' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu aktivitas' }
      ]
    },
    bullying_reports: {
      icon: Shield,
      color: 'bg-rose-500',
      name: 'bullying_reports',
      description: 'Laporan bullying (bisa anonim)',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'reporter_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke students.id (NULL jika anonim)' },
        { name: 'is_anonymous', type: 'BOOLEAN', key: '', desc: 'Apakah laporan anonim' },
        { name: 'victim_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke students.id (korban)' },
        { name: 'perpetrator_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke students.id (pelaku)' },
        { name: 'incident_date', type: 'DATE', key: '', desc: 'Tanggal kejadian' },
        { name: 'incident_location', type: 'VARCHAR(100)', key: '', desc: 'Lokasi kejadian' },
        { name: 'incident_type', type: 'ENUM', key: '', desc: 'physical, verbal, cyber, social' },
        { name: 'description', type: 'TEXT', key: '', desc: 'Detail kejadian' },
        { name: 'evidence', type: 'JSON', key: '', desc: 'Array URL foto/video bukti' },
        { name: 'status', type: 'ENUM', key: '', desc: 'pending, investigating, resolved, closed' },
        { name: 'priority', type: 'ENUM', key: '', desc: 'low, medium, high, urgent' },
        { name: 'assigned_to', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke users.id (guru BK)' },
        { name: 'resolution_notes', type: 'TEXT', key: '', desc: 'Catatan penyelesaian' },
        { name: 'resolved_at', type: 'TIMESTAMP', key: '', desc: 'Waktu selesai ditangani' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu laporan dibuat' },
        { name: 'updated_at', type: 'TIMESTAMP', key: '', desc: 'Waktu update terakhir' }
      ]
    },
    notifications: {
      icon: Bell,
      color: 'bg-violet-500',
      name: 'notifications',
      description: 'Notifikasi untuk user',
      fields: [
        { name: 'id', type: 'INTEGER', key: 'PK', desc: 'Primary Key' },
        { name: 'user_id', type: 'INTEGER', key: 'FK', desc: 'Foreign Key ke users.id' },
        { name: 'title', type: 'VARCHAR(100)', key: '', desc: 'Judul notifikasi' },
        { name: 'message', type: 'TEXT', key: '', desc: 'Isi notifikasi' },
        { name: 'type', type: 'ENUM', key: '', desc: 'info, warning, alert, success' },
        { name: 'category', type: 'ENUM', key: '', desc: 'attendance, discipline, library, pet, bullying, general' },
        { name: 'is_read', type: 'BOOLEAN', key: '', desc: 'Status sudah dibaca' },
        { name: 'action_url', type: 'VARCHAR(255)', key: '', desc: 'Deep link ke halaman terkait' },
        { name: 'created_at', type: 'TIMESTAMP', key: '', desc: 'Waktu notifikasi dibuat' }
      ]
    }
  };

  const TableCard = ({ table, data }) => {
    const Icon = data.icon;
    return (
      <div 
        onClick={() => setSelectedTable(table)}
        className={`cursor-pointer p-4 rounded-lg border-2 transition-all ${
          selectedTable === table ? 'border-blue-500 shadow-lg' : 'border-gray-200 hover:border-gray-300'
        }`}
      >
        <div className="flex items-center gap-3 mb-2">
          <div className={`${data.color} p-2 rounded-lg`}>
            <Icon className="text-white" size={20} />
          </div>
          <h3 className="font-bold text-lg">{data.name}</h3>
        </div>
        <p className="text-sm text-gray-600">{data.description}</p>
        <p className="text-xs text-gray-400 mt-2">{data.fields.length} fields</p>
      </div>
    );
  };

  return (
    <div className="w-full max-w-7xl mx-auto p-6">
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-4">
          <Database className="text-blue-600" size={32} />
          <h1 className="text-3xl font-bold">Database Schema</h1>
        </div>
        <p className="text-gray-600">Aplikasi Digitalisasi Sekolah SMP - 13 Tabel Database</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
        {Object.entries(tables).map(([key, data]) => (
          <TableCard key={key} table={key} data={data} />
        ))}
      </div>

      {selectedTable && (
        <div className="bg-white rounded-lg shadow-lg p-6 border-2 border-blue-500">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-3">
              <Table className="text-blue-600" size={28} />
              <div>
                <h2 className="text-2xl font-bold">{tables[selectedTable].name}</h2>
                <p className="text-gray-600">{tables[selectedTable].description}</p>
              </div>
            </div>
            <button 
              onClick={() => setSelectedTable(null)}
              className="text-gray-400 hover:text-gray-600"
            >
              ✕
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b-2 border-gray-200">
                  <th className="text-left py-3 px-4 font-semibold">Field Name</th>
                  <th className="text-left py-3 px-4 font-semibold">Type</th>
                  <th className="text-left py-3 px-4 font-semibold">Key</th>
                  <th className="text-left py-3 px-4 font-semibold">Description</th>
                </tr>
              </thead>
              <tbody>
                {tables[selectedTable].fields.map((field, idx) => (
                  <tr key={idx} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-4 font-mono text-sm">{field.name}</td>
                    <td className="py-3 px-4 text-sm text-gray-700">{field.type}</td>
                    <td className="py-3 px-4">
                      {field.key && (
                        <span className={`inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-semibold ${
                          field.key === 'PK' ? 'bg-blue-100 text-blue-700' :
                          field.key === 'FK' ? 'bg-green-100 text-green-700' :
                          'bg-yellow-100 text-yellow-700'
                        }`}>
                          {field.key === 'PK' && <Key size={12} />}
                          {field.key}
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-sm text-gray-600">{field.desc}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="mt-8 p-6 bg-blue-50 rounded-lg">
        <h3 className="font-bold text-lg mb-3">📝 Catatan Penting:</h3>
        <ul className="space-y-2 text-sm text-gray-700">
          <li>• <strong>PK</strong> = Primary Key (ID unik)</li>
          <li>• <strong>FK</strong> = Foreign Key (relasi ke tabel lain)</li>
          <li>• <strong>UNIQUE</strong> = Nilai harus unik dalam tabel</li>
          <li>• Semua tabel punya <code>created_at</code> dan <code>updated_at</code> untuk tracking</li>
          <li>• Password di-hash menggunakan bcrypt (tidak disimpan plain text)</li>
          <li>• JSON digunakan untuk data array/object (accessories, evidence)</li>
          <li>• ENUM untuk field dengan nilai tetap/terbatas</li>
        </ul>
      </div>

      <div className="mt-6 p-6 bg-green-50 rounded-lg">
        <h3 className="font-bold text-lg mb-3">🔗 Relasi Antar Tabel:</h3>
        <ul className="space-y-2 text-sm text-gray-700">
          <li>• <strong>users</strong> ↔ students (1:1)</li>
          <li>• <strong>users</strong> ↔ teachers (1:1)</li>
          <li>• <strong>students</strong> ↔ classes (many:1)</li>
          <li>• <strong>students</strong> ↔ virtual_pets (1:1)</li>
          <li>• <strong>students</strong> ↔ book_loans (1:many)</li>
          <li>• <strong>students</strong> ↔ attendances (1:many)</li>
          <li>• <strong>students</strong> ↔ discipline_records (1:many)</li>
          <li>• <strong>books</strong> ↔ book_loans (1:many)</li>
          <li>• <strong>virtual_pets</strong> ↔ pet_activities (1:many)</li>
        </ul>
      </div>
    </div>
  );
};

export default DatabaseSchema;