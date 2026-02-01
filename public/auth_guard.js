import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { getAuth, onAuthStateChanged, signOut, signInWithEmailAndPassword, createUserWithEmailAndPassword, updateProfile } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";
import { getFirestore, doc, getDoc, setDoc, updateDoc, serverTimestamp, collection, query, where, getDocs } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyBvmr1cu8-WnGNiD5M_cla6lxr88QEYu28",
  authDomain: "eperpus-sekolah.firebaseapp.com",
  projectId: "eperpus-sekolah",
  storageBucket: "eperpus-sekolah.firebasestorage.app",
  messagingSenderId: "303647816343",
  appId: "1:303647816343:web:78ad36d2d1be25930547d2"
};

const app = initializeApp(firebaseConfig, "AuthGuardApp");
const auth = getAuth(app);
const db = getFirestore(app);
const SESSION_KEY = 'lentera_device_id';

// --- 1. DEVICE GUARD (Rule: 1 Account 1 Device) ---

function getDeviceId() {
    let deviceId = localStorage.getItem(SESSION_KEY);
    if (!deviceId) {
        deviceId = crypto.randomUUID();
        localStorage.setItem(SESSION_KEY, deviceId);
    }
    return deviceId;
}

const currentDeviceId = getDeviceId();
console.log("[AuthManager] Active. Device ID:", currentDeviceId);

onAuthStateChanged(auth, async (user) => {
    if (user) {
        // Logged In: Check Device Lock
        console.log("[AuthManager] User logged in:", user.email);
        removeCustomLogin(); // Remove login form if present

        const userRef = doc(db, "users", user.uid);
        try {
            const userSnap = await getDoc(userRef);
            if (userSnap.exists()) {
                const userData = userSnap.data();
                
                // Admin Exception: Skip device check
                if (userData.role === 'admin' || userData.role === 'guru') {
                    console.log("[AuthManager] Admin/Teacher logged in. Skipping device lock.");
                    return;
                }

                if (userData.deviceId && userData.deviceId !== currentDeviceId) {
                     // LOCKED OUT
                     console.warn("[AuthManager] Blocked: Active on another device.");
                     alert("AKSES DITOLAK!\n\nAkun ini sedang aktif di perangkat lain.\nAturan: 1 Akun = 1 Perangkat.\n\nSilakan logout dari perangkat lama.");
                     await signOut(auth);
                     window.location.reload();
                     return;
                } else if (!userData.deviceId) {
                    // Claim device
                    await updateDoc(userRef, { deviceId: currentDeviceId, lastLogin: serverTimestamp() });
                }
            } else {
                // Create profile if missing
                 await setDoc(userRef, { 
                    deviceId: currentDeviceId,
                    email: user.email,
                    username: user.displayName, // Try to sync username
                    role: 'student',
                    lastLogin: serverTimestamp()
                }, { merge: true });
            }
        } catch (e) {
            console.error("[AuthManager] Guard check failed:", e);
        }
    } else {
        // Logged Out: Show Custom Login
        console.log("[AuthManager] User logged out.");
        injectCustomLogin();
    }
});

// --- 2. CUSTOM LOGIN UI (Rule: Name=Username, NISN=Password) ---

function injectCustomLogin() {
    if (document.getElementById('custom-login-overlay')) return;

    // Create Overlay
    const overlay = document.createElement('div');
    overlay.id = 'custom-login-overlay';
    overlay.style.cssText = `
        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
        background: linear-gradient(to bottom right, #2563eb, #4f46e5);
        z-index: 9999; display: flex; align-items: center; justify-content: center;
        font-family: sans-serif;
    `;

    overlay.innerHTML = `
        <div style="background: white; padding: 2rem; border-radius: 1rem; width: 90%; max-width: 400px; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1);">
            <h2 style="text-align: center; font-size: 1.5rem; font-weight: bold; color: #1f2937; margin-bottom: 1.5rem;">
                Login Siswa
            </h2>
            <div id="login-error" style="display:none; background: #fee2e2; color: #b91c1c; padding: 0.75rem; border-radius: 0.5rem; margin-bottom: 1rem; font-size: 0.875rem;"></div>
            
            <form id="custom-login-form">
                <div style="margin-bottom: 1rem;">
                    <label style="display: block; font-size: 0.875rem; font-weight: 500; color: #374151; margin-bottom: 0.25rem;">Nama Siswa (Username)</label>
                    <input type="text" id="inp-username" required style="width: 100%; padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 0.375rem;" placeholder="Contoh: Budi Santoso">
                </div>
                <div style="margin-bottom: 1.5rem;">
                    <label style="display: block; font-size: 0.875rem; font-weight: 500; color: #374151; margin-bottom: 0.25rem;">NISN (Password)</label>
                    <input type="password" id="inp-nisn" required style="width: 100%; padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 0.375rem;" placeholder="Masukkan NISN">
                </div>
                <button type="submit" id="btn-submit" style="width: 100%; background: #2563eb; color: white; padding: 0.75rem; border: none; border-radius: 0.375rem; font-weight: 600; cursor: pointer;">
                    Masuk
                </button>
            </form>
            
            <div style="margin-top: 1.5rem; text-align: center; font-size: 0.875rem;">
                <a href="#" id="link-register" style="color: #2563eb; text-decoration: none;">Belum punya akun? Daftar</a>
                <br><br>
                <a href="/kelola_literasi.html" style="color: #6b7280; text-decoration: none; font-size: 0.75rem;">Login Admin / Guru</a>
            </div>
            
             <div style="margin-top: 1rem; text-align: center; font-size: 0.75rem; color: #6b7280;">
                Aturan: 1 Akun = 1 Device
            </div>
        </div>
    `;

    document.body.appendChild(overlay);

    // Handle Login
    document.getElementById('custom-login-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('inp-username').value.trim();
        const nisn = document.getElementById('inp-nisn').value.trim();
        const btn = document.getElementById('btn-submit');
        const err = document.getElementById('login-error');

        btn.disabled = true;
        btn.textContent = 'Memproses...';
        err.style.display = 'none';

        try {
            // 1. Find Email by Username
            const q = query(collection(db, "users"), where("username", "==", username));
            const snapshot = await getDocs(q);

            if (snapshot.empty) {
                throw new Error("User tidak ditemukan. Pastikan Nama benar atau Daftar dulu.");
            }

            // Assume first match is correct (Limitation: duplicate names)
            const userDoc = snapshot.docs[0].data();
            const email = userDoc.email;

            if (!email) throw new Error("Data akun tidak valid (Email missing).");

            // 2. Login with Email & NISN
            await signInWithEmailAndPassword(auth, email, nisn);
            // AuthStateChanged will handle the rest
            
        } catch (error) {
            console.error(error);
            err.textContent = error.message.replace("Firebase: ", "");
            err.style.display = 'block';
            btn.disabled = false;
            btn.textContent = 'Masuk';
        }
    });

    // Handle Register Toggle
    document.getElementById('link-register').addEventListener('click', (e) => {
        e.preventDefault();
        showRegisterForm();
    });
}

function showRegisterForm() {
    const overlay = document.getElementById('custom-login-overlay');
    if (!overlay) return;

    overlay.querySelector('h2').textContent = 'Daftar Siswa Baru';
    const form = document.getElementById('custom-login-form');
    
    // Add Class Input if not exists
    if (!document.getElementById('inp-class')) {
        const div = document.createElement('div');
        div.style.marginBottom = '1rem';
        div.innerHTML = `
            <label style="display: block; font-size: 0.875rem; font-weight: 500; color: #374151; margin-bottom: 0.25rem;">Kelas</label>
            <select id="inp-class" required style="width: 100%; padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 0.375rem;">
                <option value="">Pilih Kelas</option>
                <option value="VII-A">VII-A</option><option value="VII-B">VII-B</option><option value="VII-C">VII-C</option>
                <option value="VIII-A">VIII-A</option><option value="VIII-B">VIII-B</option><option value="VIII-C">VIII-C</option>
                <option value="IX-A">IX-A</option><option value="IX-B">IX-B</option><option value="IX-C">IX-C</option>
            </select>
        `;
        form.insertBefore(div, form.children[1]); // Insert before NISN
    }

    const btn = document.getElementById('btn-submit');
    btn.textContent = 'Daftar';
    
    // Update link
    const link = document.getElementById('link-register');
    link.textContent = 'Sudah punya akun? Masuk';
    link.onclick = (e) => {
        e.preventDefault();
        window.location.reload(); // Simple reset
    };

    // Replace Submit Handler
    const newForm = form.cloneNode(true);
    form.parentNode.replaceChild(newForm, form);
    
    newForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('inp-username').value.trim();
        const nisn = document.getElementById('inp-nisn').value.trim();
        const kelas = document.getElementById('inp-class').value;
        const btn = document.getElementById('btn-submit'); // Re-select
        const err = document.getElementById('login-error');

        if(!username || !nisn || !kelas) return;

        btn.disabled = true;
        btn.textContent = 'Mendaftar...';
        err.style.display = 'none';

        try {
            // Check if username exists
            const q = query(collection(db, "users"), where("username", "==", username));
            const snapshot = await getDocs(q);
            if (!snapshot.empty) {
                throw new Error("Nama sudah terdaftar. Gunakan nama lain atau login.");
            }

            // Generate Email
            const email = `${username.replace(/\s+/g,'.').toLowerCase()}+${Date.now()}@sekolah.app`;

            // Create Auth User
            const userCred = await createUserWithEmailAndPassword(auth, email, nisn);
            const user = userCred.user;

            // Update Profile
            await updateProfile(user, { displayName: username });

            // Save to Firestore
            await setDoc(doc(db, "users", user.uid), {
                username: username,
                name: username,
                nis: nisn,
                class: kelas,
                email: email,
                role: 'student',
                deviceId: currentDeviceId, // Auto-lock to this device
                createdAt: serverTimestamp()
            });

            alert("Pendaftaran Berhasil! Silakan Login.");
            window.location.reload();

        } catch (error) {
            console.error(error);
            err.textContent = error.message.replace("Firebase: ", "");
            err.style.display = 'block';
            btn.disabled = false;
            btn.textContent = 'Daftar';
        }
    });
}

function removeCustomLogin() {
    const overlay = document.getElementById('custom-login-overlay');
    if (overlay) overlay.remove();
}

// Expose reset for Admin
window.resetDeviceLock = async () => {
    // ... same as before
};
