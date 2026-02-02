import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { getFirestore, collection, getDocs, limit, query } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

const firebaseConfig = {
    apiKey: "AIzaSyBvmr1cu8-WnGNiD5M_cla6lxr88QEYu28",
    authDomain: "eperpus-sekolah.firebaseapp.com",
    projectId: "eperpus-sekolah",
    storageBucket: "eperpus-sekolah.firebasestorage.app",
    messagingSenderId: "303647816343",
    appId: "1:303647816343:web:78ad36d2d1be25930547d2"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function inspectStudents() {
    console.log("--- Memeriksa Data Siswa (Firestore) ---");
    try {
        const q = query(collection(db, "students"), limit(3));
        const snapshot = await getDocs(q);
        
        if (snapshot.empty) {
            console.log("Koleksi 'students' KOSONG atau tidak ditemukan!");
            console.log("Coba cek nama koleksi lain...");
        } else {
            console.log(`Ditemukan ${snapshot.size} data sampel siswa:`);
            snapshot.forEach(doc => {
                console.log(`\nID: ${doc.id}`);
                console.log(JSON.stringify(doc.data(), null, 2));
            });
        }
    } catch (e) {
        console.error("Error akses Firestore:", e.message);
    }
}

inspectStudents();