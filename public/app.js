document.addEventListener('DOMContentLoaded', () => {
    
    // LOGIN LOGIC
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const code = document.getElementById('login-qr').value.trim();
            const feedback = document.getElementById('login-feedback');
            
            if (code.startsWith('BITS-P')) {
                sessionStorage.setItem('qrToken', code);
                window.location.href = '/owner_dashboard.html';
            } else if (code.startsWith('BITS-F')) {
                sessionStorage.setItem('qrToken', code);
                window.location.href = '/finder_dashboard.html';
            } else {
                feedback.className = 'feedback error';
                feedback.textContent = 'Invalid QR Token format.';
            }
        });
    }

    // Helper to convert file to Base64
    const fileToBase64 = (file) => new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result); // Preserve full data URI (mime + base64)
        reader.onerror = error => reject(error);
    });

    const loadHistory = async (qrToken) => {
        const list = document.getElementById('items-list');
        if (!list) return;

        try {
            const res = await fetch(`/api/items?qrToken=${qrToken}`);
            const data = await res.json();
            if (data.status === 'success') {
                list.innerHTML = '';
                if (data.items.length === 0) {
                    list.innerHTML = '<p>No items found.</p>';
                    return;
                }
                data.items.forEach(item => {
                    const card = document.createElement('div');
                    card.className = 'item-card';
                    const type = item.isFoundItem ? 'Found Item' : 'Registered Item';
                    card.innerHTML = `
                        <h3>${type} <small style="color:var(--text-muted); font-size: 0.8rem; float:right;">ID: ${item.id.substring(0,8)}</small></h3>
                        <p><strong>AI Descriptor:</strong> ${item.description}</p>
                    `;
                    list.appendChild(card);
                });
            }
        } catch (e) {
            console.error('Error loading history', e);
        }
    };

    const loadNotifications = async (qrToken) => {
        const list = document.getElementById('notifications-list');
        const section = document.getElementById('notifications-section');
        if (!list) return;

        try {
            const res = await fetch(`/api/notifications?qrToken=${qrToken}`);
            const data = await res.json();
            const badge = document.getElementById('notif-badge');
            
            if (data.status === 'success' && data.notifications.length > 0) {
                if (badge) {
                    badge.style.display = 'inline-block';
                    badge.textContent = data.notifications.length;
                }
                section.style.display = 'block';
                list.innerHTML = '';
                
                data.notifications.forEach(n => {
                    const card = document.createElement('div');
                    card.className = 'item-card';
                    card.style.borderColor = '#ffffff';

                    if (n.role === 'FINDER') {
                        if (n.status === 'PENDING') {
                            card.innerHTML = `
                                <h3>We found the owner!</h3>
                                <p>Please verify this is the same item. (Owner's Original Upload):</p>
                                <img src="${n.verificationImage}" class="verification-img" onclick="openImageModal('${n.verificationImage}')" title="Click to enlarge" />
                                <p>If this matches, please schedule a meetup to return it securely.</p>
                                <form onsubmit="submitMeetup(event, '${n.id}')">
                                    <input type="text" id="phone-${n.id}" placeholder="Your Phone Number" required style="width:100%; margin-bottom:10px; padding:8px;" />
                                    <select id="location-${n.id}" required style="width:100%; margin-bottom:10px; padding:8px; background:rgba(25,25,25,0.8); color:var(--text-main); border:1px solid var(--glass-border); border-radius:6px;">
                                        <option value="">Select Meeting Place</option>
                                        <option value="Mess 1">Mess 1</option>
                                        <option value="Mess 2">Mess 2</option>
                                    </select>
                                    <input type="datetime-local" id="time-${n.id}" required style="width:100%; margin-bottom:10px; padding:8px; background:rgba(255,255,255,0.1); color:white;" />
                                    <button type="submit" style="width:100%; margin-top:10px;">Schedule Meetup</button>
                                </form>
                            `;
                        } else {
                            const timeStr = new Date(n.meetupTime).toString() === 'Invalid Date' ? n.meetupTime : new Date(n.meetupTime).toLocaleString();
                            card.innerHTML = `
                                <h3>Meetup Scheduled</h3>
                                <img src="${n.verificationImage}" class="verification-img" onclick="openImageModal('${n.verificationImage}')" title="Click to enlarge" />
                                <p>You are meeting the owner at <strong>${n.location}</strong> on <strong>${timeStr}</strong>.</p>
                                <p>Your Contact: ${n.contactPhone}</p>
                            `;
                        }
                    } else if (n.role === 'OWNER') {
                        if (n.status === 'PENDING') {
                            card.innerHTML = `
                                <h3>Potential Match Found!</h3>
                                <p>Someone found your item! Here is their uploaded picture for double verification:</p>
                                <img src="${n.verificationImage}" class="verification-img" onclick="openImageModal('${n.verificationImage}')" title="Click to enlarge" />
                                <p>Waiting for the finder to share contact details...</p>
                            `;
                        } else {
                            const timeStr = new Date(n.meetupTime).toString() === 'Invalid Date' ? n.meetupTime : new Date(n.meetupTime).toLocaleString();
                            card.innerHTML = `
                                <h3>Item Verified & Meetup Scheduled!</h3>
                                <img src="${n.verificationImage}" class="verification-img" onclick="openImageModal('${n.verificationImage}')" title="Click to enlarge" />
                                <p>The finder verified the item. Meet them here:</p>
                                <ul>
                                    <li><strong>Location:</strong> ${n.location}</li>
                                    <li><strong>Time:</strong> ${timeStr}</li>
                                    <li><strong>Contact:</strong> ${n.contactPhone}</li>
                                </ul>
                            `;
                        }
                    }
                    list.appendChild(card);
                });
            } else {
                section.style.display = 'none';
            }
        } catch (e) {
            console.error('Error loading notifications', e);
        }
    };

    window.submitMeetup = async (e, notifId) => {
        e.preventDefault();
        const phone = document.getElementById(`phone-${notifId}`).value;
        const location = document.getElementById(`location-${notifId}`).value;
        const time = document.getElementById(`time-${notifId}`).value;
        
        await fetch('/api/meetup/schedule', {
            method: 'POST',
            headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ notificationId: notifId, phone, location, time })
        });
        
        const qrToken = sessionStorage.getItem('qrToken');
        loadNotifications(qrToken);
    };

    window.openImageModal = (src) => {
        document.getElementById('full-image').src = src;
        document.getElementById('image-modal').style.display = 'flex';
    };

    // OWNER DASHBOARD LOGIC
    const regForm = document.getElementById('register-form');
    if (regForm) {
        const qrToken = sessionStorage.getItem('qrToken');
        if (!qrToken) window.location.href = '/';
        loadHistory(qrToken);
        loadNotifications(qrToken);

        regForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const fileInput = document.getElementById('reg-image');
            if (fileInput.files.length === 0) return;
            
            const btn = regForm.querySelector('button');
            const feedback = document.getElementById('reg-feedback');

            btn.disabled = true;
            btn.textContent = 'Analyzing & Registering...';

            try {
                const base64Image = await fileToBase64(fileInput.files[0]);
                const response = await fetch('/api/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ qrToken: qrToken, imageBase64: base64Image })
                });

                const data = await response.json();
                if (data.status === 'success') {
                    feedback.className = 'feedback success';
                    feedback.innerHTML = `Item registered securely!<br><small>AI saw: "${data.description}"</small>`;
                    regForm.reset();
                    loadHistory(qrToken);
                    loadNotifications(qrToken);
                } else {
                    feedback.className = 'feedback error';
                    feedback.textContent = 'Failed to register item: ' + data.message;
                }
            } catch (err) {
                feedback.className = 'feedback error';
                feedback.textContent = 'Network error.';
            } finally {
                btn.disabled = false;
                btn.textContent = 'Secure Item';
            }
        });
    }

    // FINDER DASHBOARD LOGIC
    const foundForm = document.getElementById('found-form');
    if (foundForm) {
        const qrToken = sessionStorage.getItem('qrToken');
        if (!qrToken) window.location.href = '/';
        loadHistory(qrToken);
        loadNotifications(qrToken);

        foundForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const fileInput = document.getElementById('found-image');
            if (fileInput.files.length === 0) return;

            const btn = foundForm.querySelector('button');
            const feedback = document.getElementById('found-feedback');

            btn.disabled = true;
            btn.textContent = 'Searching AI Database...';

            try {
                const base64Image = await fileToBase64(fileInput.files[0]);
                const response = await fetch('/api/found', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ finderQr: qrToken, imageBase64: base64Image })
                });

                const data = await response.json();
                
                if (data.status === 'match') {
                    feedback.textContent = '';
                    document.getElementById('match-details-container').innerHTML = `
                        <p style="margin: 1rem 0; color: #34d399">Match Confidence: ${(data.confidence * 100).toFixed(1)}%</p>
                        <p style="margin-bottom: 1rem; color: #a5b4fc">AI Analysis: "${data.description}"</p>
                        <p style="margin-bottom: 1rem; color: #f8fafc">Owner Notified securely.</p>
                    `;
                    document.getElementById('match-modal').classList.remove('hidden');
                    foundForm.reset();
                    loadHistory(qrToken);
                    loadNotifications(qrToken);
                } else {
                    feedback.className = 'feedback error';
                    feedback.innerHTML = `No secure match found right now.<br><small>AI saw: "${data.description}"</small>`;
                    foundForm.reset();
                    loadHistory(qrToken);
                }
            } catch (err) {
                feedback.className = 'feedback error';
                feedback.textContent = 'Network error.';
            } finally {
                btn.disabled = false;
                btn.textContent = 'Find Owner';
            }
        });
    }
});

function closeModal() {
    document.getElementById('match-modal').classList.add('hidden');
}
