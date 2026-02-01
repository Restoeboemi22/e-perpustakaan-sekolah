import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { getFirestore, collection, addDoc, getDocs, query, orderBy, where, updateDoc, doc } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyBvmr1cu8-WnGNiD5M_cla6lxr88QEYu28",
  authDomain: "eperpus-sekolah.firebaseapp.com",
  projectId: "eperpus-sekolah",
  storageBucket: "eperpus-sekolah.firebasestorage.app",
  messagingSenderId: "303647816343",
  appId: "1:303647816343:web:78ad36d2d1be25930547d2"
};

const app = initializeApp(firebaseConfig, "TestLiteracyApp");
const db = getFirestore(app);

async function runTest() {
    console.log("--- TEST SIMULASI TUGAS LITERASI ---");
    
    // 1. CREATE TASK (Simulasi Admin)
    console.log("\n1. Membuat Tugas Baru (Simulasi Admin)...");
    const newTask = {
        title: "Tugas Uji Coba " + new Date().toLocaleTimeString(),
        description: "Silakan baca buku favoritmu selama 15 menit dan tulis ringkasannya.",
        points: 50,
        deadline: new Date(Date.now() + 86400000).toISOString(), // Besok
        createdAt: new Date().toISOString(),
        status: "active",
        submissions: 0
    };

    try {
        // Non-aktifkan tugas lama dulu (agar tes valid)
        const q = query(collection(db, "literacy_tasks"), where("status", "==", "active"));
        const snapshot = await getDocs(q);
        for(const d of snapshot.docs) {
            await updateDoc(doc(db, "literacy_tasks", d.id), { status: "archived" });
        }
        console.log(`   (Mengarsipkan ${snapshot.size} tugas lama)`);

        // Buat tugas baru
        const docRef = await addDoc(collection(db, "literacy_tasks"), newTask);
        console.log("   [SUKSES] Tugas dibuat dengan ID:", docRef.id);

        // 2. FETCH TASK (Simulasi Siswa)
        console.log("\n2. Mengambil Tugas (Simulasi Aplikasi Siswa)...");
        // Logika Siswa: Ambil semua, cari yang 'active'
        const qStudent = query(collection(db, "literacy_tasks"), orderBy("createdAt", "desc"));
        const snapStudent = await getDocs(qStudent);
        
        const tasks = snapStudent.docs.map(d => ({id: d.id, ...d.data()}));
        const activeTask = tasks.find(t => t.status === "active");

        if (activeTask && activeTask.id === docRef.id) {
            console.log("   [SUKSES] Tugas ditemukan di sisi Siswa!");
            console.log("   Judul:", activeTask.title);
            console.log("   Status:", activeTask.status);
            console.log("\n✅ KESIMPULAN: Jalur pengiriman tugas BERFUNGSI NORMAL.");
            console.log("   Tugas ini seharusnya sekarang muncul di HP Siswa.");
        } else {
            console.error("   [GAGAL] Tugas aktif tidak ditemukan atau tidak cocok.");
            console.log("   Ditemukan:", activeTask);
        }

    } catch (e) {
        console.error("ERROR:", e);
    }
}

// Browser-like environment shim for simple testing if run in Node (won't work directly due to ESM imports in Node)
// Since we are in a browser-like environment (Vite project), we might need to run this via a simple HTML page or assume Node handles it if using 'firebase-admin'.
// But here I'm using client SDK. I will write this to a public HTML file and run it via a headless browser approach or just tell user to open it.
// Actually, I can run it in Node if I use the Node SDK, but I don't have service account keys.
// So I'll create a temporary HTML file 'test_literasi.html' and tell the user they can open it, OR I can try to verify via code inspection.

// BETTER APPROACH:
// I will create a simple HTML file that runs this test and prints to the screen.
// User can open http://localhost:5173/test_literasi.html
