// ==========================================================================
// BloodConnect — Frontend JavaScript (Vanilla JS & Fetch API)
// ==========================================================================

const API_BASE = window.location.hostname === "localhost" ||
                 window.location.hostname === "127.0.0.1"
    ? "http://localhost:8080/api"
    : "https://bloodconnect-gl1r.onrender.com/api";

// ---------------- Session Helpers ----------------
function getCurrentUser() {
    try {
        const u = sessionStorage.getItem("bloodconnect_user");
        return u ? JSON.parse(u) : null;
    } catch (e) {
        return null;
    }
}

function setCurrentUser(user) {
    sessionStorage.setItem("bloodconnect_user", JSON.stringify(user));
}

function logout() {
    sessionStorage.removeItem("bloodconnect_user");
    window.location.href = "login.html";
}

function requireAuth(expectedRole) {
    const user = getCurrentUser();
    if (!user) {
        window.location.href = "login.html";
        return null;
    }
    if (expectedRole && user.role !== expectedRole) {
        alert("Unauthorized access. Redirecting to your assigned portal.");
        if (user.role === "CUSTOMER") window.location.href = "donor.html";
        else if (user.role === "HOSPITAL") window.location.href = "hospital.html";
        else if (user.role === "ADMIN") window.location.href = "admin.html";
        else window.location.href = "index.html";
        return null;
    }
    return user;
}

function showAlert(containerId, message, type = "danger") {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `
        <div class="alert alert-${type}">
            ${message}
        </div>
    `;
}

// ---------------- 1. Public Landing Page (index.html) ----------------
async function initLandingPage() {
    const user = getCurrentUser();
    const navUser = document.getElementById("navUserArea");
    if (navUser && user) {
        let portalUrl = "donor.html";
        if (user.role === "HOSPITAL") portalUrl = "hospital.html";
        if (user.role === "ADMIN") portalUrl = "admin.html";

        navUser.innerHTML = `
            <span>Signed in as <strong>${user.fullName}</strong></span>
            <span class="user-badge">${user.role}</span>
            <a href="${portalUrl}" class="btn btn-sm btn-primary">Go to Dashboard</a>
            <button onclick="logout()" class="btn btn-sm btn-outline">Logout</button>
        `;
    }

    try {
        const res = await fetch(`${API_BASE}/donor/blood-requests`);
        const json = await res.json();
        const tbody = document.getElementById("publicRequestsBody");
        if (!tbody) return;

        if (json.success && json.data.length > 0) {
            tbody.innerHTML = json.data.map(r => `
                <tr>
                    <td><span class="blood-type" style="font-size:1.1rem;">${r.bloodGroup}</span></td>
                    <td><strong>${r.requiredUnits} Units</strong></td>
                    <td><span class="badge ${r.urgencyLevel === 'CRITICAL' ? 'badge-danger' : r.urgencyLevel === 'URGENT' ? 'badge-warning' : 'badge-info'}">${r.urgencyLevel}</span></td>
                    <td>${r.hospitalName || 'Hospital'} (${r.hospitalCity || 'City'})</td>
                    <td>${r.hospitalPhone || '-'}</td>
                    <td>${new Date(r.createdAt).toLocaleDateString()}</td>
                </tr>
            `).join("");
        } else {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#64748b;">No active emergency blood requests right now.</td></tr>`;
        }
    } catch (e) {
        console.error("Failed to fetch public requests", e);
    }
}

// ---------------- 2. Login Page (login.html) ----------------
async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById("loginEmail").value.trim();
    const password = document.getElementById("loginPassword").value;
    const btn = document.getElementById("loginSubmitBtn");

    btn.disabled = true;
    btn.innerText = "Signing in...";
    showAlert("loginAlert", "");

    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();

        if (data.success && data.user) {
            setCurrentUser(data.user);
            showAlert("loginAlert", "Login successful! Redirecting...", "success");

            setTimeout(() => {
                if (data.user.role === "CUSTOMER") window.location.href = "donor.html";
                else if (data.user.role === "HOSPITAL") window.location.href = "hospital.html";
                else if (data.user.role === "ADMIN") window.location.href = "admin.html";
                else window.location.href = "index.html";
            }, 600);
        } else {
            showAlert("loginAlert", data.message || "Invalid credentials.");
            btn.disabled = false;
            btn.innerText = "Sign In";
        }
    } catch (err) {
        showAlert("loginAlert", "Unable to reach server. Please ensure Spring Boot is running on port 8080.");
        btn.disabled = false;
        btn.innerText = "Sign In";
    }
}

// ---------------- 3. Register Page (register.html) ----------------
async function handleCustomerRegister(e) {
    e.preventDefault();
    const form = document.getElementById("customerRegForm");
    const payload = {
        fullName: form.fullName.value.trim(),
        email: form.email.value.trim(),
        password: form.password.value,
        phone: form.phone.value.trim(),
        bloodGroup: form.bloodGroup.value,
        dateOfBirth: form.dateOfBirth.value,
        gender: form.gender.value,
        city: form.city.value.trim(),
        pincode: form.pincode.value.trim()
    };

    try {
        const res = await fetch(`${API_BASE}/auth/register-customer`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (data.success) {
            showAlert("regAlert", data.message, "success");
            form.reset();
            setTimeout(() => { window.location.href = "login.html"; }, 1500);
        } else {
            showAlert("regAlert", data.message || "Registration failed.");
        }
    } catch (err) {
        showAlert("regAlert", "Failed to connect to backend server.");
    }
}

async function handleHospitalRegister(e) {
    e.preventDefault();
    const form = document.getElementById("hospitalRegForm");
    const payload = {
        hospitalName: form.hospitalName.value.trim(),
        licenseNumber: form.licenseNumber.value.trim(),
        email: form.email.value.trim(),
        password: form.password.value,
        contactPhone: form.contactPhone.value.trim(),
        contactPerson: form.contactPerson.value.trim(),
        address: form.address.value.trim(),
        city: form.city.value.trim(),
        pincode: form.pincode.value.trim()
    };

    try {
        const res = await fetch(`${API_BASE}/auth/register-hospital`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (data.success) {
            showAlert("regAlert", data.message, "success");
            form.reset();
        } else {
            showAlert("regAlert", data.message || "Registration failed.");
        }
    } catch (err) {
        showAlert("regAlert", "Failed to connect to backend server.");
    }
}

// ---------------- 4. Donor Portal (donor.html) ----------------
async function initDonorPage() {
    const user = requireAuth("CUSTOMER");
    if (!user) return;

    document.getElementById("donorNameDisplay").innerText = user.fullName;

    // Load Profile
    try {
        const res = await fetch(`${API_BASE}/donor/profile?userId=${user.id}`);
        const data = await res.json();
        if (data.success) {
            const p = data.data;
            document.getElementById("profileBloodGroup").innerText = p.bloodGroup;
            document.getElementById("profileCity").innerText = `${p.city} (${p.pincode})`;
            document.getElementById("profileLastDonation").innerText = p.lastDonationDate || "Never";

            const eligBadge = document.getElementById("profileEligibility");
            if (p.isEligible) {
                eligBadge.className = "badge badge-success";
                eligBadge.innerText = "ELIGIBLE TO DONATE";
            } else {
                eligBadge.className = "badge badge-warning";
                eligBadge.innerText = `COOLDOWN (Eligible after: ${p.nextEligibleDate})`;
            }
        }
    } catch (e) {
        console.error("Profile load failed", e);
    }

    // Load Approved Hospitals for Scheduling Dropdown
    try {
        const res = await fetch(`${API_BASE}/donor/hospitals`);
        const data = await res.json();
        const select = document.getElementById("scheduleHospitalSelect");
        if (select && data.success) {
            select.innerHTML = '<option value="">-- Choose Hospital --</option>' +
                data.data.map(h => `<option value="${h.id}">${h.hospitalName} (${h.city})</option>`).join("");
        }
    } catch (e) {
        console.error("Hospitals load failed", e);
    }

    // Load Appointments
    loadDonorAppointments(user.id);

    // Load Requests
    loadDonorRequests();
}

async function loadDonorAppointments(userId) {
    try {
        const res = await fetch(`${API_BASE}/donor/donations?userId=${userId}`);
        const data = await res.json();
        const tbody = document.getElementById("donorAppointmentsBody");
        if (!tbody) return;

        if (data.success && data.data.length > 0) {
            tbody.innerHTML = data.data.map(d => {
                let badgeClass = "badge-info";
                if (d.status === "CONFIRMED") badgeClass = "badge-warning";
                if (d.status === "COMPLETED") badgeClass = "badge-success";
                if (d.status === "CANCELLED") badgeClass = "badge-danger";

                let actionHtml = "-";
                if (d.status === "SCHEDULED" || d.status === "CONFIRMED") {
                    actionHtml = `<button onclick="cancelAppointment(${d.id})" class="btn btn-sm btn-danger">Cancel</button>`;
                }

                return `
                    <tr>
                        <td><strong>${d.hospitalName || 'Hospital'}</strong></td>
                        <td>${d.bloodGroup}</td>
                        <td>${d.scheduledDate}</td>
                        <td>${d.timeSlot}</td>
                        <td><span class="badge ${badgeClass}">${d.status}</span></td>
                        <td>${d.remarks || '-'}</td>
                        <td>${actionHtml}</td>
                    </tr>
                `;
            }).join("");
        } else {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#64748b;">No scheduled donations yet.</td></tr>`;
        }
    } catch (e) {
        console.error("Failed to load appointments", e);
    }
}

async function handleScheduleDonation(e) {
    e.preventDefault();
    const user = getCurrentUser();
    const hospitalId = document.getElementById("scheduleHospitalSelect").value;
    const scheduledDate = document.getElementById("scheduleDate").value;
    const timeSlot = document.getElementById("scheduleTimeSlot").value;

    if (!hospitalId || !scheduledDate || !timeSlot) {
        showAlert("scheduleAlert", "Please fill in all appointment fields.");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/donor/schedule`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId: user.id, hospitalId, scheduledDate, timeSlot })
        });
        const data = await res.json();
        if (data.success) {
            showAlert("scheduleAlert", data.message, "success");
            loadDonorAppointments(user.id);
            document.getElementById("scheduleForm").reset();
        } else {
            showAlert("scheduleAlert", data.message || "Failed to schedule appointment.");
        }
    } catch (e) {
        showAlert("scheduleAlert", "Failed to connect to backend server.");
    }
}

async function cancelAppointment(donationId) {
    if (!confirm("Are you sure you want to cancel this donation appointment?")) return;
    const user = getCurrentUser();
    try {
        const res = await fetch(`${API_BASE}/donor/donations/${donationId}/cancel?userId=${user.id}`, {
            method: "PUT"
        });
        const data = await res.json();
        if (data.success) {
            alert(data.message);
            loadDonorAppointments(user.id);
        } else {
            alert(data.message || "Failed to cancel.");
        }
    } catch (e) {
        alert("Failed to connect to server.");
    }
}

async function loadDonorRequests() {
    try {
        const res = await fetch(`${API_BASE}/donor/blood-requests`);
        const json = await res.json();
        const tbody = document.getElementById("donorRequestsBody");
        if (!tbody) return;

        if (json.success && json.data.length > 0) {
            tbody.innerHTML = json.data.map(r => `
                <tr>
                    <td><span class="blood-type" style="font-size:1.1rem;">${r.bloodGroup}</span></td>
                    <td><strong>${r.requiredUnits} Units</strong></td>
                    <td><span class="badge ${r.urgencyLevel === 'CRITICAL' ? 'badge-danger' : 'badge-warning'}">${r.urgencyLevel}</span></td>
                    <td>${r.hospitalName} (${r.hospitalCity})</td>
                    <td>${r.notes || '-'}</td>
                </tr>
            `).join("");
        } else {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:#64748b;">No open blood requests.</td></tr>`;
        }
    } catch (e) {
        console.error(e);
    }
}

// ---------------- 5. Hospital Portal (hospital.html) ----------------
async function initHospitalPage() {
    const user = requireAuth("HOSPITAL");
    if (!user) return;

    document.getElementById("hospitalNameDisplay").innerText = user.hospitalName || user.fullName;

    // Load Inventory
    loadHospitalInventory(user.hospitalId);

    // Load Appointments
    loadHospitalAppointments(user.hospitalId);

    // Load Blood Requests
    loadHospitalRequests(user.hospitalId);
}

async function loadHospitalInventory(hospitalId) {
    try {
        const res = await fetch(`${API_BASE}/hospital/inventory?hospitalId=${hospitalId}`);
        const data = await res.json();
        const grid = document.getElementById("inventoryGrid");
        if (!grid) return;

        if (data.success && data.data) {
            let totalUnits = 0;
            grid.innerHTML = data.data.map(item => {
                totalUnits += item.unitsAvailable;
                return `
                    <div class="inventory-card">
                        <div class="blood-type">${item.bloodGroup}</div>
                        <div class="blood-units">${item.unitsAvailable}</div>
                        <div style="font-size:0.75rem; color:#64748b;">Units available</div>
                    </div>
                `;
            }).join("");

            const totalEl = document.getElementById("statTotalUnits");
            if (totalEl) totalEl.innerText = totalUnits;
        }
    } catch (e) {
        console.error("Failed to load inventory", e);
    }
}

async function handleAdjustInventory(e) {
    e.preventDefault();
    const user = getCurrentUser();
    const bloodGroup = document.getElementById("adjBloodGroup").value;
    const change = parseInt(document.getElementById("adjChange").value, 10);
    const remark = document.getElementById("adjRemark").value.trim();

    try {
        const res = await fetch(`${API_BASE}/hospital/inventory/adjust`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ hospitalId: user.hospitalId, bloodGroup, change, remark })
        });
        const data = await res.json();
        if (data.success) {
            showAlert("inventoryAlert", data.message, "success");
            loadHospitalInventory(user.hospitalId);
            document.getElementById("adjForm").reset();
        } else {
            showAlert("inventoryAlert", data.message || "Adjustment failed.");
        }
    } catch (e) {
        showAlert("inventoryAlert", "Server error adjusting inventory.");
    }
}

async function loadHospitalAppointments(hospitalId) {
    try {
        const res = await fetch(`${API_BASE}/hospital/appointments?hospitalId=${hospitalId}`);
        const data = await res.json();
        const tbody = document.getElementById("hospitalAppointmentsBody");
        if (!tbody) return;

        if (data.success && data.data.length > 0) {
            tbody.innerHTML = data.data.map(a => {
                let badgeClass = "badge-info";
                if (a.status === "CONFIRMED") badgeClass = "badge-warning";
                if (a.status === "COMPLETED") badgeClass = "badge-success";
                if (a.status === "CANCELLED") badgeClass = "badge-danger";

                let actionButtons = "";
                if (a.status === "SCHEDULED") {
                    actionButtons = `<button onclick="updateAppointmentStatus(${a.id}, 'CONFIRMED')" class="btn btn-sm btn-outline">Confirm</button>`;
                }
                if (a.status === "SCHEDULED" || a.status === "CONFIRMED") {
                    actionButtons += ` <button onclick="completeAppointment(${a.id})" class="btn btn-sm btn-success">Complete (+1 Unit)</button>`;
                }

                return `
                    <tr>
                        <td><strong>${a.donorName || 'Donor'}</strong></td>
                        <td>${a.donorPhone || '-'}</td>
                        <td><span class="blood-type" style="font-size:1.05rem;">${a.bloodGroup}</span></td>
                        <td>${a.scheduledDate}</td>
                        <td>${a.timeSlot}</td>
                        <td><span class="badge ${badgeClass}">${a.status}</span></td>
                        <td>${actionButtons || '-'}</td>
                    </tr>
                `;
            }).join("");
        } else {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#64748b;">No appointments scheduled yet.</td></tr>`;
        }
    } catch (e) {
        console.error("Failed to load hospital appointments", e);
    }
}

async function updateAppointmentStatus(donationId, status) {
    const user = getCurrentUser();
    try {
        const res = await fetch(`${API_BASE}/hospital/appointments/${donationId}/status`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ hospitalId: user.hospitalId, status })
        });
        const data = await res.json();
        if (data.success) {
            alert(data.message);
            loadHospitalAppointments(user.hospitalId);
        } else {
            alert(data.message || "Failed to update status.");
        }
    } catch (e) {
        alert("Server communication error.");
    }
}

async function completeAppointment(donationId) {
    if (!confirm("Confirm completion? This will add +1 blood unit to hospital inventory and apply donor cooldown.")) return;
    const user = getCurrentUser();
    try {
        const res = await fetch(`${API_BASE}/hospital/appointments/${donationId}/complete`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ hospitalId: user.hospitalId, remarks: "Completed donation session" })
        });
        const data = await res.json();
        if (data.success) {
            alert(data.message);
            loadHospitalAppointments(user.hospitalId);
            loadHospitalInventory(user.hospitalId);
        } else {
            alert(data.message || "Failed to complete donation.");
        }
    } catch (e) {
        alert("Server communication error.");
    }
}

async function loadHospitalRequests(hospitalId) {
    try {
        const res = await fetch(`${API_BASE}/hospital/requests?hospitalId=${hospitalId}`);
        const data = await res.json();
        const tbody = document.getElementById("hospitalRequestsBody");
        if (!tbody) return;

        if (data.success && data.data.length > 0) {
            tbody.innerHTML = data.data.map(r => {
                let actionBtn = "-";
                if (r.status === "PENDING" || r.status === "APPROVED") {
                    actionBtn = `<button onclick="fulfillBloodRequest(${r.id})" class="btn btn-sm btn-success">Fulfill (-${r.requiredUnits} Units)</button>`;
                }
                return `
                    <tr>
                        <td><span class="blood-type" style="font-size:1.05rem;">${r.bloodGroup}</span></td>
                        <td><strong>${r.requiredUnits} Units</strong></td>
                        <td><span class="badge ${r.urgencyLevel === 'CRITICAL' ? 'badge-danger' : 'badge-warning'}">${r.urgencyLevel}</span></td>
                        <td><span class="badge ${r.status === 'FULFILLED' ? 'badge-success' : 'badge-info'}">${r.status}</span></td>
                        <td>${r.notes || '-'}</td>
                        <td>${actionBtn}</td>
                    </tr>
                `;
            }).join("");
        } else {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#64748b;">No blood requests created yet.</td></tr>`;
        }
    } catch (e) {
        console.error("Failed to load requests", e);
    }
}

async function handleCreateBloodRequest(e) {
    e.preventDefault();
    const user = getCurrentUser();
    const bloodGroup = document.getElementById("reqBloodGroup").value;
    const requiredUnits = parseInt(document.getElementById("reqUnits").value, 10);
    const urgency = document.getElementById("reqUrgency").value;
    const notes = document.getElementById("reqNotes").value.trim();

    try {
        const res = await fetch(`${API_BASE}/hospital/requests`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ hospitalId: user.hospitalId, bloodGroup, requiredUnits, urgency, notes })
        });
        const data = await res.json();
        if (data.success) {
            showAlert("createReqAlert", data.message, "success");
            loadHospitalRequests(user.hospitalId);
            document.getElementById("createReqForm").reset();
        } else {
            showAlert("createReqAlert", data.message || "Failed to create request.");
        }
    } catch (e) {
        showAlert("createReqAlert", "Server error creating request.");
    }
}

async function fulfillBloodRequest(requestId) {
    if (!confirm("Fulfill this blood request? Required units will be deducted from your available inventory.")) return;
    const user = getCurrentUser();
    try {
        const res = await fetch(`${API_BASE}/hospital/requests/${requestId}/fulfill`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ hospitalId: user.hospitalId })
        });
        const data = await res.json();
        if (data.success) {
            alert(data.message);
            loadHospitalRequests(user.hospitalId);
            loadHospitalInventory(user.hospitalId);
        } else {
            alert(data.message || "Failed to fulfill request.");
        }
    } catch (e) {
        alert("Server error fulfilling request.");
    }
}

// ---------------- 6. Admin Portal (admin.html) ----------------
async function initAdminPage() {
    const user = requireAuth("ADMIN");
    if (!user) return;

    loadAdminStats();
    loadAdminHospitals();
    loadAdminUsers();
    loadAdminInventory();
}

async function loadAdminStats() {
    try {
        const res = await fetch(`${API_BASE}/admin/dashboard`);
        const data = await res.json();
        if (data.success && data.data) {
            const s = data.data;
            document.getElementById("statDonors").innerText = s.totalDonors;
            document.getElementById("statHospitals").innerText = s.approvedHospitals;
            document.getElementById("statPendingHospitals").innerText = s.pendingHospitals;
            document.getElementById("statBloodUnits").innerText = s.totalBloodUnits;
        }
    } catch (e) {
        console.error("Admin stats failed", e);
    }
}

async function loadAdminHospitals() {
    try {
        const res = await fetch(`${API_BASE}/admin/hospitals`);
        const data = await res.json();
        const tbody = document.getElementById("adminHospitalsBody");
        if (!tbody) return;

        if (data.success && data.data.length > 0) {
            tbody.innerHTML = data.data.map(h => {
                let badgeClass = h.verificationStatus === "APPROVED" ? "badge-success" : h.verificationStatus === "PENDING" ? "badge-warning" : "badge-danger";
                let actionBtns = "";
                if (h.verificationStatus === "PENDING") {
                    actionBtns = `
                        <button onclick="verifyHospital(${h.id}, 'APPROVE')" class="btn btn-sm btn-success">Approve</button>
                        <button onclick="verifyHospital(${h.id}, 'REJECT')" class="btn btn-sm btn-danger">Reject</button>
                    `;
                } else if (h.verificationStatus === "APPROVED") {
                    actionBtns = `<button onclick="verifyHospital(${h.id}, 'REJECT')" class="btn btn-sm btn-danger">Revoke</button>`;
                } else {
                    actionBtns = `<button onclick="verifyHospital(${h.id}, 'APPROVE')" class="btn btn-sm btn-success">Approve</button>`;
                }

                return `
                    <tr>
                        <td><strong>${h.hospitalName}</strong></td>
                        <td>${h.licenseNumber}</td>
                        <td>${h.city}</td>
                        <td>${h.contactPerson} (${h.contactPhone})</td>
                        <td><span class="badge ${badgeClass}">${h.verificationStatus}</span></td>
                        <td>${actionBtns}</td>
                    </tr>
                `;
            }).join("");
        } else {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#64748b;">No registered hospitals found.</td></tr>`;
        }
    } catch (e) {
        console.error("Admin hospitals failed", e);
    }
}

async function verifyHospital(hospitalId, action) {
    if (!confirm(`Are you sure you want to ${action} this hospital?`)) return;
    try {
        const res = await fetch(`${API_BASE}/admin/hospitals/${hospitalId}/verify`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ action })
        });
        const data = await res.json();
        if (data.success) {
            alert(data.message);
            loadAdminStats();
            loadAdminHospitals();
            loadAdminUsers();
        } else {
            alert(data.message || "Failed to update hospital status.");
        }
    } catch (e) {
        alert("Server communication error.");
    }
}

async function loadAdminUsers() {
    try {
        const res = await fetch(`${API_BASE}/admin/users`);
        const data = await res.json();
        const tbody = document.getElementById("adminUsersBody");
        if (!tbody) return;

        if (data.success && data.data.length > 0) {
            tbody.innerHTML = data.data.map(u => `
                <tr>
                    <td><strong>${u.fullName}</strong></td>
                    <td>${u.email}</td>
                    <td><span class="badge badge-info">${u.role}</span></td>
                    <td><span class="badge ${u.isActive ? 'badge-success' : 'badge-danger'}">${u.isActive ? 'ACTIVE' : 'DEACTIVATED'}</span></td>
                    <td>
                        ${u.role !== 'ADMIN' ? `<button onclick="toggleUser(${u.id})" class="btn btn-sm btn-outline">${u.isActive ? 'Deactivate' : 'Activate'}</button>` : '<em style="color:#64748b;">System Admin</em>'}
                    </td>
                </tr>
            `).join("");
        }
    } catch (e) {
        console.error("Admin users failed", e);
    }
}

async function toggleUser(userId) {
    try {
        const res = await fetch(`${API_BASE}/admin/users/${userId}/toggle-status`, {
            method: "PUT"
        });
        const data = await res.json();
        if (data.success) {
            alert(data.message);
            loadAdminUsers();
        } else {
            alert(data.message || "Action failed.");
        }
    } catch (e) {
        alert("Server communication error.");
    }
}

async function loadAdminInventory() {
    try {
        const res = await fetch(`${API_BASE}/admin/inventory`);
        const data = await res.json();
        const tbody = document.getElementById("adminInventoryBody");
        if (!tbody) return;

        if (data.success && data.data.length > 0) {
            tbody.innerHTML = data.data.map(inv => `
                <tr>
                    <td><strong>${inv.hospitalName || 'Hospital #' + inv.hospitalId}</strong></td>
                    <td>${inv.city || '-'}</td>
                    <td><span class="blood-type" style="font-size:1.05rem;">${inv.bloodGroup}</span></td>
                    <td><strong>${inv.unitsAvailable} Units</strong></td>
                    <td>${new Date(inv.lastUpdated).toLocaleString()}</td>
                </tr>
            `).join("");
        } else {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:#64748b;">No hospital inventory records found.</td></tr>`;
        }
    } catch (e) {
        console.error("Admin inventory failed", e);
    }
}
