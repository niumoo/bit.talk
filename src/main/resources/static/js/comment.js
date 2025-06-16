// js/comment.js

class Comment {
    static talkServer = ''; // 更名为 talkServer
    static appId = '';
    static pageId = '';
    static pageTitle = ''; // 用于存储页面标题

    // getApiUrl 方法保持不变，它会使用 talkServer
    static getApiUrl(path) {
        return `${Comment.talkServer}${path}`;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const container = document.getElementById("bit-talk");
    if (!container) return;

    // === 新增：从 div 元素读取配置 ===
    // 1. talk-server (原 apiBaseUrl)
    Comment.talkServer = container.getAttribute('server');
    if (!Comment.talkServer) {
        console.warn("bit-talk: 'server' attribute not found. Defaulting to 'http://localhost:8080'.");
        Comment.talkServer = 'http://localhost:8080';
    }
    Comment.talkServer = Comment.talkServer.replace(/\/$/, ''); // 确保没有末尾斜杠

    // 2. talk-app-id
    Comment.appId = container.dataset.talkAppId || ''; // 使用 dataset 访问 data-* 属性
    if (!Comment.appId) {
        console.warn("bit-talk: 'talk-app-id' attribute not found. Comments might not be linked correctly.");
    }

    // 3. talk-page-id (原 window.location.href)
    Comment.pageId = container.dataset.talkPageId;
    if (!Comment.pageId) {
        Comment.pageId = window.location.href; // 如果未提供，则回退到当前页面 URL
        console.warn(`bit-talk: 'talk-page-id' attribute not found. Defaulting to window.location.href: ${Comment.pageId}`);
    }

    // 4. pageTitle (总是使用 document.title)
    Comment.pageTitle = document.title;
    // ===================================

    // 添加样式
    addStyles();

    // 初始化页面结构
    // 标题评论
    const title = document.createElement("h3");
    title.style.margin = "5px 0";
    title.innerText = "评论";
    const form = createCommentForm();
    const commentList = document.createElement("div");
    commentList.id = "cmt-list";
    container.appendChild(title);
    container.appendChild(form);
    container.appendChild(commentList);

    // 加载评论列表
    loadComments();

    // 表单提交事件
    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const submitButton = document.getElementById("cmt-send");
        // 禁用提交按钮
        submitButton.disabled = true;
        // 2秒后恢复提交按钮
        setTimeout(() => {
            submitButton.disabled = false;
        }, 2000);

        // 获取表单数据
        const formData = new FormData(form);
        const username = formData.get("username");
        const email = formData.get("email");
        const content = formData.get("content");
        const website = formData.get("website");

        // 校验必填字段
        if (!username || !email || !content) {
            alert("请填写所有必填字段！");
            return;
        }

        try {
            // 获取 PoW 挑战
            const challenge = await fetchPowChallenge();
            const { timestamp, random, difficulty } = challenge;

            // 计算 nonce
            const nonce = await calculateNonce(timestamp, random, difficulty);

            // === 修改：评论数据包含 appId, pageId, title ===
            const commentData = {
                appId: Comment.appId,
                pageId: Comment.pageId,
                title: Comment.pageTitle,
                username,
                email,
                content,
                website: website || undefined, // 如果为空，则不发送该字段
            };
            // ===============================================

            const response = await submitComment(commentData, timestamp, random, nonce);
            if (response.ok) {
                alert("评论提交成功，审核通过后显示！");
                form.reset();
                loadComments(); // 刷新评论列表
            } else {
                const errorText = await response.text();
                alert("评论提交失败：" + errorText);
                console.error("Comment submission failed:", response.status, errorText);
            }
        } catch (error) {
            console.error("提交评论时出错：", error);
            alert("提交评论时发生错误，请稍后重试！");
        }
    });
});

function escapeHTML(str) {
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function addStyles() {
    const style = document.createElement('style');
    style.textContent = `
        #bit-talk {
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            font-size: 16px;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif;
            color: #333;
        }

        .cmt-form {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin-bottom: 20px;
        }

        .cmt-form input[type="text"],
        .cmt-form input[type="email"],
        .cmt-form input[type="url"] {
            flex: 1;
            min-width: 0;
            padding: 10px;
            border: 1px solid #d0d0d0;
            background-color: white;
            border-radius: 4px;
            box-sizing: border-box; /* Ensures padding doesn't increase total width */
        }

        .cmt-form textarea {
            width: 100%;
            height: 130px;
            padding: 10px;
            border: 1px solid #d0d0d0;
            border-radius: 4px;
            resize: vertical;
            box-sizing: border-box;
            font-family: inherit; /* Inherit font from parent */
        }

        .cmt-form button {
            padding: 6px 18px;
            background-color: #ededed;
            color: black;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            transition: background-color 0.2s ease;
        }

        .cmt-form button:hover {
            background-color: #dfdcdc;
        }

        .cmt-form button:disabled {
            background-color: #cccccc;
            cursor: not-allowed;
        }

        #cmt-list {
            margin-top: 20px;
        }

        .cmt-item {
            border-bottom: 1px solid #eee;
            padding: 15px 0;
        }
        .cmt-item:last-child {
            border-bottom: none; /* No border for the last item */
        }

        .cmt-header {
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            flex-wrap: wrap; /* Allow wrapping on smaller screens */
        }

        .cmt-author {
            font-weight: bold;
            margin-right: 10px;
            color: #333;
        }

        .cmt-author a {
            color: #404040;
            text-decoration: none;
        }

        .cmt-author a:hover {
            text-decoration: underline;
        }

        .cmt-date {
            color: #888;
            font-size: 0.9em;
        }

        .cmt-content {
            line-height: 1.6;
            color: #454545;
            word-wrap: break-word; /* Ensure long words break */
        }

        .error-message {
            color: #dc3545;
            text-align: center;
            padding: 15px;
            border: 1px solid #dc3545;
            background-color: #f8d7da;
            border-radius: 4px;
        }
    `;
    document.head.appendChild(style);
}

// 创建评论表单
function createCommentForm() {
    const form = document.createElement("form");
    form.className = "cmt-form";
    form.innerHTML = `
        <input type="text" name="username" required placeholder="昵称 *" oninvalid="this.setCustomValidity('请输入昵称')" oninput="this.setCustomValidity('')">
        <input type="email" name="email" required placeholder="邮箱 *（不会展示）" oninvalid="this.setCustomValidity('请输入正确的邮箱格式')" oninput="this.setCustomValidity('')">
        <input type="url" name="website" placeholder="网址（可选）">
        <textarea name="content" required placeholder="评论内容 *" oninvalid="this.setCustomValidity('请输入内容')" oninput="this.setCustomValidity('')" style="font-size: 1em"></textarea>
        <button type="submit" id="cmt-send">发送</button>
    `;
    return form;
}

// 加载评论列表
async function loadComments() {
    const commentList = document.getElementById("cmt-list");
    if (!commentList) return;

    // === 修改：使用 appId 和 pageId 作为查询参数 ===
    const apiUrl = Comment.getApiUrl(
        `/api/v1/comment/${Comment.appId}/${btoa(Comment.pageId)}/list`
    );
    // ===============================================

    try {
        const response = await fetch(apiUrl);
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`加载评论失败: ${response.status} ${errorText}`);
        }

        const comments = await response.json();
        commentList.innerHTML = ""; // 清空现有评论
        if (comments.length === 0) {
            commentList.innerHTML = "<p style='text-align: center; color: #888;'>暂无评论，快来发表第一条评论吧！</p>";
            return;
        }

        comments.forEach((comment) => {
            const commentItem = document.createElement("div");
            commentItem.className = "cmt-item";
            commentItem.innerHTML = `
                <div class="cmt-header">
                    <strong class="cmt-author">
                        ${comment.website
                ? `<a href="${escapeHTML(comment.website)}" target="_blank" rel="noopener noreferrer">${escapeHTML(comment.username)} 🔗</a>`
                : `${escapeHTML(comment.username)}`}
                    </strong>
                    <span class="cmt-date">${new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(comment.createdAt))}</span>
                </div>
                <div class="cmt-content">${escapeHTML(comment.content)}</div>
            `;
            commentList.appendChild(commentItem);
        });
    } catch (error) {
        console.error("加载评论时出错：", error);
        commentList.innerHTML = "<p class='error-message'>加载评论失败，请稍后重试。</p>";
    }
}


// 获取 PoW 挑战
async function fetchPowChallenge() {
    const response = await fetch(Comment.getApiUrl("/api/v1/pow/generate"));
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`获取 PoW 挑战失败: ${response.status} ${errorText}`);
    }
    return response.json();
}

// 使用 Web Crypto API 计算 nonce
async function calculateNonce(timestamp, random, difficulty) {
    const target = "0".repeat(difficulty);
    let nonce = 0;
    const maxAttempts = 1000000; // 防止无限循环，设置最大尝试次数

    while (nonce < maxAttempts) {
        const data = `${timestamp}${random}${nonce}`;
        const hashBuffer = await crypto.subtle.digest(
            "SHA-256",
            new TextEncoder().encode(data)
        );
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        const hashHex = hashArray.map((byte) => byte.toString(16).padStart(2, "0")).join("");

        if (hashHex.startsWith(target)) {
            return nonce.toString();
        }
        nonce++;
    }
    throw new Error("Failed to find nonce within max attempts. PoW difficulty might be too high or calculation is slow.");
}

// 提交评论
async function submitComment(commentData, timestamp, random, nonce) {
    const response = await fetch(Comment.getApiUrl("/api/v1/comment"), {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "c-timestamp": timestamp, // HTTP Headers 习惯用 kebab-case
            "c-random": random,
            "c-nonce": nonce,
        },
        body: JSON.stringify(commentData),
    });
    return response;
}
