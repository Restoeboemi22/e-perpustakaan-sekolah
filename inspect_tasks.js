
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { getFirestore, collection, getDocs, doc, updateDoc, orderBy, query } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

const firebaseConfig = {
    apiKey: "AIzaSyBvmr1cu8-WnGNiD5M_cla6lxr88QEYu28",
    authDomain: "eperpus-sekolah.firebaseapp.com",
    projectId: "eperpus-sekolah",
    storageBucket: "eperpus-sekolah.firebasestorage.app",
    messagingSenderId: "303647816343",
    appId: "1:303647816343:web:78ad36d2d1be25930547d2"
};

const app = initializeApp(firebaseConfig, "InspectTasksApp");
const db = getFirestore(app);

async function inspectAndFix() {
    console.log("--- Memeriksa Daftar Tugas di Firestore ---");
    const q = query(collection(db, "literacy_tasks"), orderBy("createdAt", "desc")); // Asumsi field createdAt ada
    // Jika error index, kita coba getDocs biasa lalu sort manual
    
    try {
        const snapshot = await getDocs(collection(db, "literacy_tasks"));
        const tasks = snapshot.docs.map(d => ({id: d.id, ...d.data()}));
        
        // Sort manual just in case
        tasks.sort((a, b) => {
            const dateA = new Date(a.createdAt || 0);
            const dateB = new Date(b.createdAt || 0);
            return dateB - dateA;
        });

        console.log(`Ditemukan ${tasks.length} tugas.`);
        
        tasks.forEach(t => {
            console.log(`[${t.status.toUpperCase()}] ID: ${t.id}`);
            console.log(`   Judul: ${t.title}`);
            console.log(`   Dibuat: ${t.createdAt}`);
            console.log("-----------------------------------");
        });

        // Cari tugas "mantap" yang muncul di HP siswa
        const oldTask = tasks.find(t => t.title && t.title.toLowerCase().includes("mantap") && t.status === 'active');
        const newTask = tasks.find(t => t.title && t.title.includes("Membaca Buku") && t.status === 'active');

        if (oldTask) {
            console.log("\nDETEKSI MASALAH:");
            console.log(`Tugas lama '${oldTask.title}' masih berstatus ACTIVE.`);
            console.log("Ini kemungkinan memblokir tugas baru untuk muncul di HP Siswa.");
            
            // Auto-fix option (uncomment to enable)
            // console.log("Mencoba menonaktifkan tugas lama...");
            // await updateDoc(doc(db, "literacy_tasks", oldTask.id), { status: 'inactive' });
            // console.log("SUKSES! Tugas lama telah dinonaktifkan.");
        }

        if (newTask) {
             console.log(`\nTugas Baru '${newTask.title}' statusnya ACTIVE. Seharusnya muncul jika tugas lama dimatikan.`);
        }

    } catch (e) {
        console.error("Error:", e);
    }
}

inspectAndFix();
