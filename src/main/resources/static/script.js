
const roomList = document.getElementById("room-list");
const roomCount = document.getElementById("room-count");
const errorMessage = document.getElementById("error-message");

const authForms = document.getElementById("auth-forms");
const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");
const userPanel = document.getElementById("user-panel");
const currentUsername = document.getElementById("current-username");

const loginMessage = document.getElementById("login-message");
const registerMessage = document.getElementById("register-message");
const logoutMessage = document.getElementById("logout-message");
const logoutButton = document.getElementById("logout-btn");

// Real-time chat elements
const chatSection = document.getElementById("chat-section");
const chatRoomName = document.getElementById("chat-room-name");
const chatConnectionStatus = document.getElementById("chat-connection-status");
const chatMessages = document.getElementById("chat-messages");
const chatForm = document.getElementById("chat-form");
const chatInput = document.getElementById("chat-input");
const chatErrorMessage = document.getElementById("chat-error-message");
const closeChatButton = document.getElementById("close-chat-btn");

// Music queue elements
const songForm = document.getElementById("add-song-form");
const songTitle = document.getElementById("song-title");
const songArtist = document.getElementById("song-artist");
const songBpm = document.getElementById("song-bpm");
const songGenre = document.getElementById("song-genre");
const queueMessage = document.getElementById("queue-message");
const queueList = document.getElementById("music-queue-list");

// AI DJ Transition elements
const transitionRoomName = document.getElementById("transition-room-name");
const transitionMessage = document.getElementById("transition-message");
const transitionList = document.getElementById("transition-list");
const refreshTransitionsButton =
    document.getElementById("refresh-transitions-btn");
    // Soundclash HTML elements
const soundclashCreateForm =
    document.getElementById("soundclash-create-form");

const soundclashCreateMessage =
    document.getElementById("soundclash-create-message");

const soundclashRoomName =
    document.getElementById("soundclash-room-name");

const soundclashRefreshButton =
    document.getElementById("soundclash-refresh-btn");

const soundclashMessage =
    document.getElementById("soundclash-message");

const soundclashRound =
    document.getElementById("soundclash-round");

const soundclashRoundId =
    document.getElementById("soundclash-round-id");

const soundclashDisplaySongA =
    document.getElementById("soundclash-display-song-a");

const soundclashDisplayArtistA =
    document.getElementById("soundclash-display-artist-a");

const soundclashVotesA =
    document.getElementById("soundclash-votes-a");

const soundclashVoteAButton =
    document.getElementById("soundclash-vote-a-btn");

const soundclashDisplaySongB =
    document.getElementById("soundclash-display-song-b");

const soundclashDisplayArtistB =
    document.getElementById("soundclash-display-artist-b");

const soundclashVotesB =
    document.getElementById("soundclash-votes-b");

const soundclashVoteBButton =
    document.getElementById("soundclash-vote-b-btn");

const soundclashTotalVotes =
    document.getElementById("soundclash-total-votes");

const soundclashWinner =
    document.getElementById("soundclash-winner");

const soundclashRoundStatus =
    document.getElementById("soundclash-round-status");

// Soundclash state
let activeSoundclashRoundId = null;
let soundclashVotedRoundId = null;
let loggedInUsername = null;
let roomsLoading = false;

let stompClient = null;
let activeChatRoomId = null;
let chatConnectionAttempt = 0;

// Current music room ID.
// Chat open karne par yeh selected room ke ID se update hoga.
let queueRoomId = 1;
let selectedRoomName = "";

// ========================================
// 1. COMMON API HELPER
// ========================================
async function apiRequest(url, options = {}) {
    const controller = new AbortController();

    const timeoutId = setTimeout(() => {
        controller.abort();
    }, 10000);

    try {
        console.log("API request started:", url);

        const response = await fetch(url, {
            ...options,
            credentials: "same-origin",
            signal: controller.signal
        });

        const text = await response.text();
        let data = {};

        try {
            data = text ? JSON.parse(text) : {};
        } catch {
            data = {
                message: text || "Unexpected server response."
            };
        }

        console.log("API response:", url, response.status);

        return { response, data };

    } catch (error) {
        if (error.name === "AbortError") {
            throw new Error(
                `Request timed out after 10 seconds: ${url}`
            );
        }

        throw error;

    } finally {
        clearTimeout(timeoutId);
    }
}


// ========================================
// 2. UPDATE LOGIN UI
// ========================================
function updateAuthUI(username) {
    loggedInUsername = username || null;

    if (!loggedInUsername && activeChatRoomId !== null) {
        closeRoomChat();
    }

    authForms.hidden = Boolean(loggedInUsername);
    userPanel.hidden = !loggedInUsername;

    currentUsername.textContent = loggedInUsername || "";

    renderRoomActions();

    if (!loggedInUsername) {
        transitionRoomName.textContent = "Please log in to view transitions.";
        transitionMessage.textContent =
            "Login karein aur music room join karein.";
        transitionList.replaceChildren();
    }
}


// ========================================
// 3. REGISTER
// ========================================
registerForm.addEventListener("submit", async event => {
    event.preventDefault();

    registerMessage.textContent = "";

    const username =
        document.getElementById("register-username").value.trim();

    const email =
        document.getElementById("register-email").value.trim();

    const password =
        document.getElementById("register-password").value;

    const button =
        registerForm.querySelector("button[type='submit']");

    button.disabled = true;
    button.textContent = "Creating account...";

    try {
        const { response, data } = await apiRequest(
            "/api/auth/register",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    username,
                    email,
                    password
                })
            }
        );

        if (!response.ok) {
            registerMessage.textContent =
                data.message || "Registration failed.";
            return;
        }

        registerMessage.textContent =
            "Registration successful! Please login.";

        registerForm.reset();

        document.getElementById("login-username").value = username;
        document.getElementById("login-password").focus();

    } catch (error) {
        console.error("Registration error:", error);

        registerMessage.textContent =
            error.message.includes("timed out")
                ? "Server response nahi de raha. Dobara try karein."
                : "Connection error. Spring Boot backend check karein.";

    } finally {
        button.disabled = false;
        button.textContent = "Create Account";
    }
});


// ========================================
// 4. LOGIN
// ========================================
loginForm.addEventListener("submit", async event => {
    event.preventDefault();

    loginMessage.textContent = "";

    const username =
        document.getElementById("login-username").value.trim();

    const password =
        document.getElementById("login-password").value;

    const button =
        loginForm.querySelector("button[type='submit']");

    button.disabled = true;
    button.textContent = "Logging in...";

    try {
        const { response, data } = await apiRequest(
            "/api/auth/login",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    username,
                    password
                })
            }
        );

        if (!response.ok) {
            loginMessage.textContent =
                data.message || "Login failed.";
            return;
        }

        const {
            response: meResponse,
            data: meData
        } = await apiRequest("/api/auth/me");

        if (!meResponse.ok || !meData.username) {
            loginMessage.textContent =
                "Login hua, lekin session verify nahi hua. Dobara login karein.";
            console.error("Session verification failed:", meData);
            return;
        }

        updateAuthUI(meData.username);

        loginForm.reset();
        loginMessage.textContent = "";

        await loadRooms();
        await loadRecommendations();

        console.log("Login successful:", meData.username);

    } catch (error) {
        console.error("Login process error:", error);

        loginMessage.textContent =
            error.message.includes("timed out")
                ? "Request timeout ho gayi. Backend check karein."
                : "Connection error. Spring Boot backend check karein.";

    } finally {
        button.disabled = false;
        button.textContent = "Login";
    }
});


// ========================================
// 5. LOGOUT
// ========================================
logoutButton.addEventListener("click", async () => {
    logoutMessage.textContent = "";

    logoutButton.disabled = true;
    logoutButton.textContent = "Logging out...";

    try {
        const { response, data } = await apiRequest(
            "/api/auth/logout",
            {
                method: "POST"
            }
        );

        if (!response.ok) {
            logoutMessage.textContent =
                data.message || "Logout failed.";
            return;
        }

        updateAuthUI(null);

        loginMessage.textContent = "";
        registerMessage.textContent = "";
        logoutMessage.textContent = "Logout successful!";

        await loadRooms();
        await loadRecommendations();

    } catch (error) {
        console.error("Logout error:", error);

        logoutMessage.textContent =
            error.message?.includes("timed out")
                ? "Logout request timeout ho gayi."
                : "Logout nahi ho paya. Console check karein.";

    } finally {
        logoutButton.disabled = false;
        logoutButton.textContent = "Logout";
    }
});


// ========================================
// 6. CHECK SESSION ON PAGE LOAD
// ========================================
async function checkLoginSession() {
    try {
        const { response, data } =
            await apiRequest("/api/auth/me");

        if (response.ok && data.username) {
            updateAuthUI(data.username);
        } else {
            updateAuthUI(null);
        }

    } catch (error) {
        console.error("Session check error:", error);
        updateAuthUI(null);
    }
}


/* ========================================
   7. LOAD MUSIC ROOMS AND MEMBERS
======================================== */
async function loadRooms() {
    if (roomsLoading) {
        console.log("Rooms request already running; skipping.");
        return;
    }

    roomsLoading = true;

    try {
        errorMessage.textContent = "";

        const { response, data: rooms } =
            await apiRequest("/api/rooms");

        if (!response.ok || !Array.isArray(rooms)) {
            throw new Error(
                `Rooms API failed with status ${response.status}`
            );
        }

        roomCount.textContent = `${rooms.length} rooms`;

        if (rooms.length === 0) {
            roomList.innerHTML = "<p>No music rooms available.</p>";
            return;
        }

        const roomsWithMembers = await Promise.all(
            rooms.map(async room => {
                try {
                    const {
                        response: membersResponse,
                        data: membersData
                    } = await apiRequest(
                        `/api/memberships/room/${Number(room.id)}`
                    );

                    if (
                        !membersResponse.ok ||
                        !Array.isArray(membersData)
                    ) {
                        throw new Error(
                            `Members API returned status ${membersResponse.status}`
                        );
                    }

                    const members = membersData.map(member => ({
                        ...member,
                        userName:
                            member.userName ??
                            member.username ??
                            member.name ??
                            "Unknown user"
                    }));

                    return {
                        ...room,
                        members,
                        membersLoaded: true
                    };

                } catch (error) {
                    console.error(
                        `Members load error for room ${room.id}:`,
                        error
                    );

                    return {
                        ...room,
                        members: [],
                        membersLoaded: false
                    };
                }
            })
        );

        console.log(
            "Room members debug:",
            roomsWithMembers.map(room => ({
                roomId: room.id,
                roomName: room.name,
                membersLoaded: room.membersLoaded,
                members: room.members,
                memberCount: room.members.length
            }))
        );

        roomList.innerHTML = roomsWithMembers.map((room, index) => {
            const icon = getRoomIcon(room.genre);
            const artClass = `art-${(index % 3) + 1}`;
            const roomId = Number(room.id);

            const members = room.members;
            const membersLoaded = room.membersLoaded;

            const isJoined = Boolean(loggedInUsername) &&
                membersLoaded &&
                members.some(member =>
                    member.userName === loggedInUsername
                );

            let memberNames;

            if (!membersLoaded) {
                memberNames =
                    "<li class='no-members'>Members load nahi ho paye. Console check karein.</li>";
            } else if (members.length === 0) {
                memberNames =
                    "<li class='no-members'>No members yet</li>";
            } else {
                memberNames = members.map(member => `
                    <li class="member-item">
                        <span class="member-icon">👤</span>
                        ${escapeHTML(member.userName)}
                    </li>
                `).join("");
            }

            return `
                <article class="room-card">
                    <div class="room-art ${artClass}">
                        ${icon}
                    </div>

                    <div class="room-info">
                        <span class="genre">
                            ${escapeHTML(room.genre)}
                        </span>

                        <h3>${escapeHTML(room.name)}</h3>

                        <p class="listeners">
                            🎧 ${Number(room.listeners) || 0} listeners
                        </p>

                        <div class="room-members">
                            <h4>
                                Room Members (${
                                    membersLoaded
                                        ? members.length
                                        : "—"
                                })
                            </h4>

                            <ul class="member-list">
                                ${memberNames}
                            </ul>
                        </div>

                        <button
    type="button"
    class="join-btn"
    data-room-id="${roomId}"
    data-room-name="${escapeHTML(room.name)}"
                            ${!loggedInUsername || isJoined ? "disabled" : ""}>
                            ${isJoined ? "Joined ✓" : "Join Room →"}
                        </button>

                        <button
                            type="button"
                            class="leave-btn"
                            data-room-id="${roomId}"
                            ${!loggedInUsername || !isJoined ? "disabled" : ""}>
                            Leave Room
                        </button>

                        <button
                            type="button"
                            class="chat-btn primary-btn"
                            data-room-id="${roomId}"
                            data-room-name="${escapeHTML(room.name)}"
                            ${!loggedInUsername || !isJoined ? "disabled" : ""}>
                            Open Chat 💬
                        </button>
                    </div>
                </article>
            `;
        }).join("");

    } catch (error) {
        roomCount.textContent = "Unavailable";
        roomList.innerHTML = "";

        errorMessage.textContent =
            "Rooms load nahi hue. Spring Boot backend aur API check karein.";

        console.error("Load rooms error:", error);

    } finally {
        roomsLoading = false;
    }
}


/* ========================================
   8. UPDATE ROOM BUTTONS
======================================== */
function renderRoomActions() {
    roomList.querySelectorAll(
        ".join-btn, .leave-btn, .chat-btn"
    ).forEach(button => {
        if (!loggedInUsername) {
            button.disabled = true;
        }
    });
}


// ========================================
// 9. JOIN, LEAVE OR OPEN CHAT
// ========================================
roomList.addEventListener("click", async event => {
    const chatButton = event.target.closest(".chat-btn");

    if (chatButton) {
        if (!loggedInUsername) {
            alert("Please login first.");
            return;
        }

        if (chatButton.disabled) return;

        await openRoomChat(
            Number(chatButton.dataset.roomId),
            chatButton.dataset.roomName
        );

        return;
    }

    const button = event.target.closest(".join-btn, .leave-btn");

    if (!button) return;

    if (!loggedInUsername) {
        alert("Please login first.");
        return;
    }

    if (button.disabled) return;

    const roomId = Number(button.dataset.roomId);
    const isJoining = button.classList.contains("join-btn");

    button.disabled = true;
    button.textContent = isJoining ? "Joining..." : "Leaving...";

    try {
        const { response, data } = await apiRequest(
            isJoining
                ? "/api/memberships/join"
                : "/api/memberships/leave",
            {
                method: isJoining ? "POST" : "DELETE",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ roomId })
            }
        );

        if (response.status === 401 || response.status === 403) {
            alert(
                "Session expired ya room access nahi hai. Please login again."
            );

            updateAuthUI(null);
            await loadRooms();
            return;
        }

        if (!response.ok) {
            alert(data.message || "Room operation failed.");
            return;
        }

        if (!isJoining && activeChatRoomId === roomId) {
            closeRoomChat();
        }

        alert(
            data.message ||
            (isJoining
                ? "Joined room successfully!"
                : "Left room successfully!")
                
        );
        if (isJoining) {
    queueRoomId = roomId;
    selectedRoomName = button.dataset.roomName || `Room ${roomId}`;

    await loadMusicQueue();
    await loadTransitions(roomId);
    await loadSoundclash();
}

        await loadRooms();

    } catch (error) {
        console.error("Room membership error:", error);

        alert(
            error.message?.includes("timed out")
                ? "Request timeout ho gayi. Dobara try karein."
                : "Server se connect nahi ho paya."
        );

    } finally {
        if (button.isConnected) {
            button.disabled = false;
        }
    }
});


// ========================================
// 10. ROOM ICON HELPER
// ========================================
function getRoomIcon(genre) {
    const value = String(genre ?? "").toLowerCase();

    if (value.includes("edm")) return "🎛️";
    if (value.includes("bollywood")) return "🎤";

    if (value.includes("lo-fi") || value.includes("lofi")) {
        return "🌙";
    }

    return "🎵";
}


// ========================================
// 11. HTML ESCAPING HELPER
// ========================================
function escapeHTML(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[char]);
}


// ========================================
// 12. DISPLAY CHAT MESSAGE
// ========================================
function displayChatMessage(message) {
    const placeholder = chatMessages.querySelector(":scope > p");

    if (placeholder) {
        placeholder.remove();
    }

    const item = document.createElement("div");
    item.className = "chat-message";

    if (message.username === loggedInUsername) {
        item.classList.add("own-message");
    }

    const sender = document.createElement("strong");
    sender.textContent = message.username || "Unknown user";

    const content = document.createElement("p");
    content.className = "chat-message-text";
    content.textContent = message.message || "";

    const time = document.createElement("small");

    if (message.createdAt) {
        const date = new Date(message.createdAt);

        time.textContent = Number.isNaN(date.getTime())
            ? ""
            : date.toLocaleTimeString();
    }

    const sentiment = document.createElement("small");
    sentiment.className = "chat-sentiment";

    const sentimentLabels = {
        POSITIVE: "😊 Positive",
        NEUTRAL: "😐 Neutral",
        NEGATIVE: "😞 Negative"
    };

    if (message.sentiment && sentimentLabels[message.sentiment]) {
        sentiment.textContent = sentimentLabels[message.sentiment];
    }

    item.append(sender, content, sentiment, time);

    chatMessages.appendChild(item);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}


// ========================================
// 13. OPEN ROOM CHAT
// ========================================
async function openRoomChat(roomId, roomName) {
    if (!loggedInUsername) {
        chatErrorMessage.textContent = "Please login first.";
        return;
    }

    if (!Number.isInteger(roomId) || roomId <= 0) {
        chatErrorMessage.textContent = "Invalid room ID.";
        return;
    }

    if (
        typeof SockJS === "undefined" ||
        typeof Stomp === "undefined" ||
        typeof Stomp.over !== "function"
    ) {
        chatSection.hidden = false;
        chatErrorMessage.textContent =
            "Chat libraries load nahi hui. Page refresh karke try karein.";
        return;
    }

    closeRoomChat();

    const attempt = chatConnectionAttempt;
    activeChatRoomId = roomId;

    // Queue aur transitions ke liye bhi selected room set karein.
    queueRoomId = roomId;
    selectedRoomName = roomName;

if (soundclashRoomName) {
    soundclashRoomName.textContent = roomName;
}

    chatSection.hidden = false;
    chatRoomName.textContent = roomName;
    chatConnectionStatus.textContent = "Loading previous messages...";
    chatErrorMessage.textContent = "";
    chatMessages.innerHTML = "<p>Loading messages...</p>";

    // Selected room ki queue aur transition guide load karein.
    await loadMusicQueue();
    await loadTransitions(roomId);

    try {
        const { response, data } = await apiRequest(
            `/api/chat/rooms/${roomId}/messages`
        );

        if (
            attempt !== chatConnectionAttempt ||
            activeChatRoomId !== roomId
        ) {
            return;
        }

        if (!response.ok) {
            throw new Error(
                data.message ||
                `Unable to load messages (${response.status}).`
            );
        }

        if (!Array.isArray(data)) {
            throw new Error("Unexpected chat history response.");
        }

        chatMessages.innerHTML = "";

        if (data.length === 0) {
            chatMessages.innerHTML =
                "<p>No messages yet. Say hello!</p>";
        } else {
            data.forEach(displayChatMessage);
        }

        const socket = new SockJS("/ws");
        const client = Stomp.over(socket);

        client.debug = () => {};
        stompClient = client;

        client.connect(
            {},
            () => {
                if (
                    attempt !== chatConnectionAttempt ||
                    activeChatRoomId !== roomId
                ) {
                    client.disconnect();
                    return;
                }

                chatConnectionStatus.textContent =
                    "🟢 Connected to live chat";

                // Real-time chat subscription
                client.subscribe(
                    `/topic/rooms/${roomId}`,
                    frame => {
                        if (activeChatRoomId !== roomId) return;

                        try {
                            const incomingMessage = JSON.parse(frame.body);
                            displayChatMessage(incomingMessage);
                        } catch (error) {
                            console.error("Invalid chat message:", error);
                        }
                    }
                );

                // Real-time Music Queue subscription
                client.subscribe(
                    `/topic/rooms/${roomId}/music-queue`,
                    frame => {
                        if (queueRoomId !== roomId) return;

                        try {
                            const updatedQueue = JSON.parse(frame.body);

                            renderMusicQueue(updatedQueue);
                            queueMessage.textContent = "";

                            // Queue change hone par transitions bhi update karein.
                            loadTransitions(roomId);
                        } catch (error) {
                            console.error(
                                "Invalid music queue update:",
                                error
                            );
                        }
                    }
                );
                // Real-time Soundclash results
client.subscribe(
    `/topic/rooms/${roomId}/soundclash`,
    frame => {
        if (queueRoomId !== roomId) return;

        try {
            const result = JSON.parse(frame.body);
            renderSoundclashResult(result);
        } catch (error) {
            console.error("Invalid Soundclash update:", error);
        }
    }
);

                // Connection ke baad queue aur transitions refresh karein.
                loadMusicQueue();
                loadTransitions(roomId);
            },
            error => {
                if (attempt !== chatConnectionAttempt) return;

                console.error("WebSocket connection error:", error);

                chatConnectionStatus.textContent =
                    "🔴 Chat disconnected";

                chatErrorMessage.textContent =
                    "Live chat connect nahi hua. Login, backend aur WebSocket security check karein.";
            }
        );

    } catch (error) {
        if (attempt !== chatConnectionAttempt) return;

        console.error("Open chat error:", error);

        chatConnectionStatus.textContent = "Chat unavailable";
        chatErrorMessage.textContent = error.message;
    }
}


// ========================================
// 14. CLOSE ROOM CHAT
// ========================================
function closeRoomChat() {
    chatConnectionAttempt++;

    activeChatRoomId = null;

    const client = stompClient;
    stompClient = null;

    if (client) {
        try {
            if (client.connected) {
                client.disconnect();
            } else if (client.ws) {
                client.ws.close();
            }
        } catch (error) {
            console.error("Chat disconnect error:", error);
        }
    }

    chatSection.hidden = true;
    chatErrorMessage.textContent = "";
    chatConnectionStatus.textContent = "Chat disconnected";
}

closeChatButton.addEventListener("click", closeRoomChat);


// ========================================
// 15. SEND CHAT MESSAGE
// ========================================
chatForm.addEventListener("submit", event => {
    event.preventDefault();

    const message = chatInput.value.trim();

    if (!message) return;

    if (!loggedInUsername) {
        chatErrorMessage.textContent = "Please login first.";
        return;
    }

    if (!activeChatRoomId) {
        chatErrorMessage.textContent = "Please open a room chat first.";
        return;
    }

    if (!stompClient || !stompClient.connected) {
        chatErrorMessage.textContent = "Chat connected nahi hai.";
        return;
    }

    try {
        stompClient.send(
            `/app/rooms/${activeChatRoomId}/send`,
            {},
            JSON.stringify({ message })
        );

        chatInput.value = "";
        chatErrorMessage.textContent = "";

    } catch (error) {
        console.error("Send chat message error:", error);
        chatErrorMessage.textContent = "Message send nahi hua.";
    }
});


// ========================================
// 16. LOAD MUSIC QUEUE
// ========================================
async function loadMusicQueue() {
    try {
        const { response, data } = await apiRequest(
            `/api/music-queue/${queueRoomId}`
        );

        if (!response.ok) {
            throw new Error(
                data.message || `Queue load failed (${response.status}).`
            );
        }

        if (!Array.isArray(data)) {
            throw new Error("Unexpected music queue response.");
        }

        renderMusicQueue(data);

    } catch (error) {
        console.error("Music queue load error:", error);
        queueMessage.textContent = error.message;
    }
}


// ========================================
// 17. RENDER MUSIC QUEUE
// ========================================
function renderMusicQueue(songs) {
    queueList.replaceChildren();

    if (!Array.isArray(songs) || songs.length === 0) {
        const item = document.createElement("li");
        item.textContent = "Queue is empty.";
        queueList.appendChild(item);
        return;
    }

    songs.forEach(song => {
        const item = document.createElement("li");
        const title = document.createElement("strong");
        const details = document.createElement("span");

        title.textContent = song.title || "Untitled";

        let detailText =
            ` — ${song.artist || "Unknown artist"}` +
            ` | Added by: ${song.addedBy || "Unknown user"}`;

        if (song.bpm != null) {
            detailText += ` | BPM: ${song.bpm}`;
        }

        if (song.genre) {
            detailText += ` | Genre: ${song.genre}`;
        }

        details.textContent = detailText;

        item.append(title, details);
        queueList.appendChild(item);
    });
}


// ========================================
// 18. ADD SONG WITH BPM AND GENRE
// ========================================
songForm.addEventListener("submit", async event => {
    event.preventDefault();

    const title = songTitle.value.trim();
    const artist = songArtist.value.trim();

    const bpmValue = songBpm ? songBpm.value.trim() : "";
    const genreValue = songGenre ? songGenre.value : "";

    if (!loggedInUsername) {
        queueMessage.textContent = "Song add karne ke liye pehle login karein.";
        return;
    }

    if (!title) {
        queueMessage.textContent = "Song title is required.";
        return;
    }

    if (!artist) {
        queueMessage.textContent = "Artist name is required.";
        return;
    }

    let bpm = null;

    if (bpmValue !== "") {
        bpm = Number(bpmValue);

        if (!Number.isInteger(bpm) || bpm < 1 || bpm > 300) {
            queueMessage.textContent =
                "BPM 1 se 300 ke beech ek valid number hona chahiye.";
            return;
        }
    }

    const requestBody = {
        title,
        artist,
        bpm,
        genre: genreValue || null
    };

    const submitButton =
        songForm.querySelector("button[type='submit']");

    if (submitButton) {
        submitButton.disabled = true;
    }

    queueMessage.textContent = "Adding song...";

    try {
        const { response, data: result } = await apiRequest(
            `/api/music-queue/${queueRoomId}`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(requestBody)
            }
        );

        if (!response.ok) {
            throw new Error(
                result.message || "Could not add song."
            );
        }

        songForm.reset();

        queueMessage.textContent =
            "Song added successfully!";

        await loadMusicQueue();
        await loadTransitions(queueRoomId);

    } catch (error) {
        console.error("Add song error:", error);
        queueMessage.textContent = error.message;

    } finally {
        if (submitButton) {
            submitButton.disabled = false;
        }
    }
});


// ========================================
// 19. AI DJ TRANSITION GUIDE
// ========================================
function getTransitionDetails(transition) {
    // DTO ke commonly used field names support karein.
    const currentTitle =
        transition.currentTitle ??
        transition.currentSongTitle ??
        transition.currentSong?.title ??
        "Current song";

    const nextTitle =
        transition.nextTitle ??
        transition.nextSongTitle ??
        transition.nextSong?.title ??
        "Next song";

    const currentBpm =
        transition.currentBpm ??
        transition.currentSongBpm ??
        transition.currentSong?.bpm;

    const nextBpm =
        transition.nextBpm ??
        transition.nextSongBpm ??
        transition.nextSong?.bpm;

    const currentGenre =
        transition.currentGenre ??
        transition.currentSongGenre ??
        transition.currentSong?.genre;

    const nextGenre =
        transition.nextGenre ??
        transition.nextSongGenre ??
        transition.nextSong?.genre;

    const recommendation = String(
        transition.recommendation ??
        transition.transitionType ??
        transition.suggestion ??
        "NOT_AVAILABLE"
    ).toUpperCase();

    // Backend se aane wali detailed explanation, agar available ho.
    const reason =
        typeof transition.reason === "string" && transition.reason.trim()
            ? transition.reason.trim()
            : "";

    return {
        currentTitle,
        nextTitle,
        currentBpm,
        nextBpm,
        currentGenre,
        nextGenre,
        recommendation,
        reason
    };
}


function getTransitionLabel(recommendation) {
    const labels = {
        SMOOTH: "🟢 Smooth Transition",
        MODERATE: "🟡 Moderate Transition",
        CHALLENGING: "🔴 Challenging Transition",
        ADD_BPM: "ℹ️ Add BPM",
        INVALID_BPM: "⚠️ Invalid BPM",
        NOT_AVAILABLE: "ℹ️ Not Available"
    };

    return labels[recommendation] ||
        `ℹ️ ${recommendation.replaceAll("_", " ")}`;
}


function getTransitionExplanation(recommendation) {
    const explanations = {
        SMOOTH:
            "BPM aur genre ke hisaab se yeh relatively smooth transition ho sakta hai.",
        MODERATE:
            "Mix karte waqt tempo ya genre ka difference dhyan mein rakhein.",
        CHALLENGING:
            "Songs ke tempo/genre mein noticeable difference hai. Transition carefully plan karein.",
        ADD_BPM:
            "Dono songs ke BPM enter karein taaki transition evaluate ho sake.",
        INVALID_BPM:
            "Song ka BPM invalid hai. BPM ko check karke dobara enter karein.",
        NOT_AVAILABLE:
            "Is pair ke liye transition recommendation available nahi hai."
    };

    return explanations[recommendation] ||
        "Yeh recommendation BPM aur genre ke available data par based hai.";
}


function renderTransitions(transitions) {
    transitionList.replaceChildren();

    if (!Array.isArray(transitions) || transitions.length === 0) {
        const placeholder = document.createElement("p");
        placeholder.textContent =
            "Abhi transition dikhane ke liye kam se kam 2 songs add karein.";
        transitionList.appendChild(placeholder);
        return;
    }

    transitions.forEach(transition => {
        const details = getTransitionDetails(transition);

        const card = document.createElement("article");
        card.className = "transition-card";

        const heading = document.createElement("h3");
        heading.textContent =
            `${details.currentTitle} → ${details.nextTitle}`;

        const status = document.createElement("p");
        status.className = "transition-status";
        status.dataset.status = details.recommendation;
        status.textContent =
            getTransitionLabel(details.recommendation);

        const songDetails = document.createElement("p");

        const currentBpmText =
            details.currentBpm != null
                ? details.currentBpm
                : "Not provided";

        const nextBpmText =
            details.nextBpm != null
                ? details.nextBpm
                : "Not provided";

        songDetails.textContent =
            `BPM: ${currentBpmText} → ${nextBpmText}`;

        const genres = document.createElement("p");

        genres.textContent =
            `Genre: ${details.currentGenre || "Unknown"} → ` +
            `${details.nextGenre || "Unknown"}`;

        const explanation = document.createElement("p");
        explanation.className = "transition-explanation";
        explanation.textContent =
            details.reason || getTransitionExplanation(details.recommendation);

        card.append(
            heading,
            status,
            songDetails,
            genres,
            explanation
        );

        transitionList.appendChild(card);
    });
}


async function loadTransitions(roomId = queueRoomId) {
    if (!transitionList || !transitionMessage || !transitionRoomName) {
        console.warn("AI DJ Transition HTML elements nahi mile.");
        return;
    }

    if (!loggedInUsername) {
        transitionRoomName.textContent =
            "Please log in to view transitions.";

        transitionMessage.textContent =
            "AI DJ Transition Guide use karne ke liye login karein.";

        transitionList.replaceChildren();
        return;
    }

    if (!Number.isInteger(Number(roomId)) || Number(roomId) <= 0) {
        transitionRoomName.textContent = "No valid room selected.";
        transitionMessage.textContent =
            "Pehle ek valid music room select karein.";
        transitionList.replaceChildren();
        return;
    }

    const selectedRoomId = Number(roomId);

    transitionRoomName.textContent =
        `Room ID: ${selectedRoomId} — AI DJ Transition Guide`;

    transitionMessage.textContent =
        "Loading transition recommendations...";

    if (refreshTransitionsButton) {
        refreshTransitionsButton.disabled = true;
    }

    try {
        const { response, data } = await apiRequest(
            `/api/music-queue/${selectedRoomId}/transitions`
        );

        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                transitionMessage.textContent =
                    "Is room ki transitions dekhne ke liye login karke room join karein.";
            } else {
                transitionMessage.textContent =
                    data.message ||
                    `Transitions load nahi hui (${response.status}).`;
            }

            transitionList.replaceChildren();
            return;
        }

        if (!Array.isArray(data)) {
            throw new Error(
                "Unexpected transition API response. Controller ka JSON check karein."
            );
        }

        // Agar user ne doosra room select kar liya hai,
        // purane room ke results display na karein.
        if (selectedRoomId !== queueRoomId) {
            return;
        }

        renderTransitions(data);

        transitionMessage.textContent =
            data.length > 0
                ? `${data.length} transition recommendation(s) loaded.`
                : "Queue mein kam se kam 2 songs add karein.";

    } catch (error) {
        console.error("Load transitions error:", error);

        transitionMessage.textContent =
            "Transitions load nahi hui: " + error.message;

        transitionList.replaceChildren();

    } finally {
        if (refreshTransitionsButton) {
            refreshTransitionsButton.disabled = false;
        }
    }
}


// Refresh Transitions button
if (refreshTransitionsButton) {
    refreshTransitionsButton.addEventListener("click", async () => {
        await loadTransitions(queueRoomId);
    });
}


// ========================================
// 20. MUSIC RECOMMENDATIONS
// ========================================
async function loadRecommendations() {
    const message = document.getElementById("recommendation-message");
    const songsContainer = document.getElementById("recommended-songs");

    if (!message || !songsContainer) {
        console.error("Recommendation HTML elements nahi mile.");
        return;
    }

    message.textContent = "Loading recommendations...";
    songsContainer.replaceChildren();

    try {
        const { response, data } = await apiRequest(
            "/api/recommendations"
        );

        if (!response.ok) {
            message.textContent =
                data.message ||
                (response.status === 401
                    ? "Recommendations ke liye pehle login karein."
                    : "Recommendations load nahi hui.");
            return;
        }

        const genres = Array.isArray(data.genres) ? data.genres : [];
        const songs = Array.isArray(data.songs) ? data.songs : [];

        document.querySelectorAll(
            "#preference-options input[type='checkbox']"
        ).forEach(input => {
            input.checked = genres.includes(input.value);
        });

        if (genres.length === 0 || songs.length === 0) {
            message.textContent =
                data.message ||
                "Apne favourite genres select karke Save My Preferences dabayein.";
            return;
        }

        message.textContent =
            "Recommended for: " + genres.join(", ");

        songs.forEach(song => {
            const card = document.createElement("div");
            const title = document.createElement("h3");
            const artist = document.createElement("p");
            const genre = document.createElement("p");

            title.textContent = song.title || "Untitled";

            artist.textContent =
                "Artist: " + (song.artist || "Unknown artist");

            genre.textContent =
                "Genre: " + (song.genre || "Unknown");

            card.append(title, artist, genre);
            songsContainer.appendChild(card);
        });

    } catch (error) {
        console.error("Load recommendations error:", error);

        message.textContent =
            "Recommendations load nahi hui. Backend aur login check karein.";
    }
}


// ========================================
// 21. SAVE MUSIC PREFERENCES
// ========================================
document.getElementById("save-preferences-btn")
    .addEventListener("click", async () => {
        const message =
            document.getElementById("recommendation-message");

        const genres = Array.from(
            document.querySelectorAll(
                "#preference-options input:checked"
            )
        ).map(input => input.value);

        if (genres.length === 0) {
            message.textContent =
                "Kam se kam ek music genre select karein.";
            return;
        }

        try {
            const { response, data } = await apiRequest(
                "/api/recommendations/preferences",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({ genres })
                }
            );

            if (!response.ok) {
                message.textContent =
                    data.message || "Could not save preferences.";
                return;
            }

            message.textContent =
                data.message || "Preferences saved successfully.";

            await loadRecommendations();

        } catch (error) {
            message.textContent = "Could not save preferences.";
            console.error("Save preferences error:", error);
        }
    });
    // ========================================
// SOUNCLASH: LOAD ACTIVE ROUND
// ========================================

function resetSoundclash(message) {
    activeSoundclashRoundId = null;

    if (soundclashRound) {
        soundclashRound.hidden = true;
    }

    if (soundclashMessage) {
        soundclashMessage.textContent =
            message || "No active Soundclash round.";
    }

    if (soundclashVoteAButton) {
        soundclashVoteAButton.disabled = true;
    }

    if (soundclashVoteBButton) {
        soundclashVoteBButton.disabled = true;
    }
}

function renderSoundclashResult(result) {
    if (!result || typeof result !== "object") {
        resetSoundclash("Invalid Soundclash response.");
        return;
    }

    // Backend may return active:false when no round exists.
    if (result.active === false &&
        result.roundId == null &&
        result.id == null) {
        resetSoundclash(
            result.message || "No active Soundclash round."
        );
        return;
    }

    const roundId = result.roundId ?? result.id;

    if (roundId == null) {
        resetSoundclash(
            result.message || "Round ID response mein nahi mili."
        );
        return;
    }

    activeSoundclashRoundId = Number(roundId);

    const songATitle = result.songATitle || "Song A";
    const songAArtist = result.songAArtist || "Unknown artist";
    const songBTitle = result.songBTitle || "Song B";
    const songBArtist = result.songBArtist || "Unknown artist";

    const votesA = Number(result.votesA) || 0;
    const votesB = Number(result.votesB) || 0;
    const totalVotes = Number(result.totalVotes ?? votesA + votesB);

    if (soundclashRound) {
        soundclashRound.hidden = false;
    }

    if (soundclashRoundId) {
        soundclashRoundId.textContent = `Round #${roundId}`;
    }

    if (soundclashDisplaySongA) {
        soundclashDisplaySongA.textContent = songATitle;
    }

    if (soundclashDisplayArtistA) {
        soundclashDisplayArtistA.textContent =
            `Artist: ${songAArtist}`;
    }

    if (soundclashDisplaySongB) {
        soundclashDisplaySongB.textContent = songBTitle;
    }

    if (soundclashDisplayArtistB) {
        soundclashDisplayArtistB.textContent =
            `Artist: ${songBArtist}`;
    }

    if (soundclashVotesA) {
        soundclashVotesA.textContent = `Votes: ${votesA}`;
    }

    if (soundclashVotesB) {
        soundclashVotesB.textContent = `Votes: ${votesB}`;
    }

    if (soundclashTotalVotes) {
        soundclashTotalVotes.textContent =
            `Total votes: ${totalVotes}`;
    }

    const winner = result.winner || (
        votesA > votesB ? "A" :
        votesB > votesA ? "B" :
        totalVotes === 0 ? "PENDING" : "TIE"
    );

    if (soundclashWinner) {
        if (winner === "A") {
            soundclashWinner.textContent =
                `Current leader: ${songATitle}`;
        } else if (winner === "B") {
            soundclashWinner.textContent =
                `Current leader: ${songBTitle}`;
        } else if (winner === "TIE") {
            soundclashWinner.textContent = "Tie";
        } else {
            soundclashWinner.textContent = "Waiting for votes";
        }
    }

    if (soundclashRoundStatus) {
        soundclashRoundStatus.textContent =
            result.active === false ? "Round closed" : "Round is active";
    }

    if (soundclashMessage) {
        soundclashMessage.textContent =
            soundclashVotedRoundId === activeSoundclashRoundId
                ? "Aap is round mein vote kar chuke hain."
                : "";
    }

    const canVote =
        Boolean(loggedInUsername) &&
        result.active !== false &&
        soundclashVotedRoundId !== activeSoundclashRoundId;

    if (soundclashVoteAButton) {
        soundclashVoteAButton.disabled = !canVote;
    }

    if (soundclashVoteBButton) {
        soundclashVoteBButton.disabled = !canVote;
    }
}

async function loadSoundclash() {
    if (!loggedInUsername) {
        resetSoundclash("Soundclash use karne ke liye login karein.");
        return;
    }

    const roomId = Number(queueRoomId);

    if (!Number.isInteger(roomId) || roomId <= 0) {
        resetSoundclash("Pehle ek valid room select karein.");
        return;
    }

    if (soundclashRoomName) {
        soundclashRoomName.textContent =
            `Selected Room: ${roomId}`;
    }

    if (soundclashMessage) {
        soundclashMessage.textContent =
            "Loading active Soundclash round...";
    }

    if (soundclashRefreshButton) {
        soundclashRefreshButton.disabled = true;
    }

    try {
        const { response, data } = await apiRequest(
            `/api/soundclash/rooms/${roomId}`
        );

        if (response.status === 401 || response.status === 403) {
            resetSoundclash(
                data.message || "Pehle is room ko join karein."
            );
            return;
        }

        if (!response.ok) {
            throw new Error(
                data.message || `Request failed (${response.status})`
            );
        }

        if (
            data.active === false ||
            (data.roundId == null && data.id == null)
        ) {
            resetSoundclash(
                data.message ||
                "No active round. Do songs select karke round create karein."
            );
            return;
        }

        renderSoundclashResult(data);

    } catch (error) {
        console.error("Soundclash load error:", error);
        resetSoundclash(
            `Soundclash load nahi hua: ${error.message}`
        );
    } finally {
        if (soundclashRefreshButton) {
            soundclashRefreshButton.disabled = false;
        }
    }
}


// ========================================
// SOUNCLASH: CREATE ROUND
// ========================================

if (soundclashCreateForm) {
    soundclashCreateForm.addEventListener("submit", async event => {
        event.preventDefault();

        if (soundclashCreateMessage) {
            soundclashCreateMessage.textContent = "";
        }

        if (!loggedInUsername) {
            if (soundclashCreateMessage) {
                soundclashCreateMessage.textContent =
                    "Round create karne ke liye pehle login karein.";
            }
            return;
        }

        const songATitle =
            document.getElementById("soundclash-song-a").value.trim();

        const songAArtist =
            document.getElementById("soundclash-artist-a").value.trim();

        const songBTitle =
            document.getElementById("soundclash-song-b").value.trim();

        const songBArtist =
            document.getElementById("soundclash-artist-b").value.trim();

        if (!songATitle || !songBTitle) {
            soundclashCreateMessage.textContent =
                "Song A aur Song B ke titles required hain.";
            return;
        }

        if (songATitle.toLowerCase() === songBTitle.toLowerCase()) {
            soundclashCreateMessage.textContent =
                "Dono songs ke titles alag hone chahiye.";
            return;
        }

        const button =
            soundclashCreateForm.querySelector("button[type='submit']");

        if (button) {
            button.disabled = true;
            button.textContent = "Creating round...";
        }

        try {
            const { response, data } = await apiRequest(
                `/api/soundclash/rooms/${Number(queueRoomId)}/rounds`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        songATitle,
                        songAArtist,
                        songBTitle,
                        songBArtist
                    })
                }
            );

            if (!response.ok) {
                throw new Error(
                    data.message ||
                    `Round create nahi hua (${response.status}).`
                );
            }

            soundclashVotedRoundId = null;

            if (soundclashCreateMessage) {
                soundclashCreateMessage.textContent =
                    data.message || "Soundclash round created!";
            }

            soundclashCreateForm.reset();
            await loadSoundclash();

        } catch (error) {
            console.error("Create Soundclash error:", error);

            if (soundclashCreateMessage) {
                soundclashCreateMessage.textContent =
                    error.message;
            }
        } finally {
            if (button) {
                button.disabled = false;
                button.textContent = "⚔️ Start Soundclash";
            }
        }
    });
}


// ========================================
// SOUNCLASH: CAST VOTE
// ========================================

async function castSoundclashVote(songChoice) {
    if (!loggedInUsername) {
        soundclashMessage.textContent = "Vote dene ke liye login karein.";
        return;
    }

    if (!activeSoundclashRoundId) {
        soundclashMessage.textContent = "Koi active round nahi hai.";
        return;
    }

    if (soundclashVotedRoundId === activeSoundclashRoundId) {
        soundclashMessage.textContent =
            "Aap is round mein pehle hi vote de chuke hain.";
        return;
    }

    const buttons = [
        soundclashVoteAButton,
        soundclashVoteBButton
    ].filter(Boolean);

    buttons.forEach(button => {
        button.disabled = true;
    });

    soundclashMessage.textContent = "Vote submit ho raha hai...";

    try {
        const { response, data } = await apiRequest(
            `/api/soundclash/rooms/${Number(queueRoomId)}/vote`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    roundId: activeSoundclashRoundId,
                    songChoice
                })
            }
        );

        if (!response.ok) {
            soundclashMessage.textContent =
                data.message || `Vote failed (${response.status}).`;

            // Backend already knows if the user voted.
            if (/already|duplicate|voted/i.test(data.message || "")) {
                soundclashVotedRoundId = activeSoundclashRoundId;
            }

            await loadSoundclash();
            return;
        }

        soundclashVotedRoundId = activeSoundclashRoundId;

        soundclashMessage.textContent =
            data.message || `Song ${songChoice} ke liye vote submit ho gaya!`;

        await loadSoundclash();

    } catch (error) {
        console.error("Soundclash vote error:", error);
        soundclashMessage.textContent =
            `Vote submit nahi hua: ${error.message}`;
    } finally {
        buttons.forEach(button => {
            button.disabled =
                soundclashVotedRoundId === activeSoundclashRoundId;
        });
    }
}

if (soundclashVoteAButton) {
    soundclashVoteAButton.addEventListener("click", () => {
        castSoundclashVote("A");
    });
}

if (soundclashVoteBButton) {
    soundclashVoteBButton.addEventListener("click", () => {
        castSoundclashVote("B");
    });
}

if (soundclashRefreshButton) {
    soundclashRefreshButton.addEventListener("click", loadSoundclash);
}


// ========================================
// 22. INITIALIZE APPLICATION
// ========================================
async function initializeApp() {
    await checkLoginSession();
    await loadRooms();
    if (loggedInUsername) {
    await loadSoundclash();
}
    await loadRecommendations();

    // Initial queue aur transitions load karein.
    await loadMusicQueue();

    if (loggedInUsername) {
        await loadTransitions(queueRoomId);
    }
}

initializeApp();


// ========================================
// 23. AUTO-REFRESH ROOMS
// ========================================
setInterval(() => {
    if (!document.hidden && !roomsLoading) {
        loadRooms();
    }
}, 5000);