// js/comment.js
class Comment {
    static apiBaseUrl = '';

    static init(config) {
        if (config && config.apiBaseUrl) {
            Comment.apiBaseUrl = config.apiBaseUrl.replace(/\/$/, '');
        }
    }

    static getApiUrl(path) {
        return `${Comment.apiBaseUrl}${path}`;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const container = document.getElementById("bit-talk");
    if (!container) return;

    // 添加样式
    addStyles();

    // 初始化页面结构
    const form = createCommentForm();
    const commentList = document.createElement("div");
    commentList.id = "cmt-list";
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

            // 提交评论
            const commentData = {
                url: window.location.href, // 当前页面 URL
                title: document.title, // 当前页面标题
                username,
                email,
                content,
                website: website || undefined,
            };

            const response = await submitComment(commentData, timestamp, random, nonce);
            if (response.ok) {
                alert("评论提交成功，审核通过后显示！");
                form.reset();
                loadComments(); // 刷新评论列表
            } else {
                alert("评论提交失败：" + (await response.text()));
            }
        } catch (error) {
            console.error("提交评论时出错：", error);
            alert("提交评论时发生错误，请稍后重试！");
        }
    });
});

function addStyles() {
    const style = document.createElement('style');
    style.textContent = `
        #bit-talk {
            max-width: 700px;
            margin: 0 auto;
            padding: 20px;
            font-size: 16px;
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
        }

        .cmt-form textarea {
            width: 100%;
            height: 130px;
            padding: 10px;
            border: 1px solid #d0d0d0;
            border-radius: 4px;
            resize: vertical;
        }

        .cmt-form button {
            padding: 8px 20px;
            background-color: #e1e1e1;
            color: #212121;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }

        .cmt-form button:hover {
            background-color: #d5d5d5;
        }

        #cmt-list {
            margin-top: 20px;
        }

        .cmt-item {
            border-bottom: 1px solid #eee;
            padding: 15px 0;
        }

        .cmt-header {
            margin-bottom: 10px;
        }

        .cmt-author {
            font-weight: bold;
            margin-right: 5px;
        }

        .cmt-date {
            color: #888;
            font-size: 0.9em;
        }

        .cmt-content {
            line-height: 1.4;
            color: #454545;
        }
    `;
    document.head.appendChild(style);
}

// 创建评论表单
function createCommentForm() {
    const form = document.createElement("form");
    form.className = "cmt-form";
    form.innerHTML = `
        <input type="text" name="username" required placeholder="昵称" oninvalid="this.setCustomValidity('请输入昵称')" oninput="this.setCustomValidity('')">
        <input type="email" name="email" required placeholder="邮箱" oninvalid="this.setCustomValidity('请输入正确的邮箱格式')" oninput="this.setCustomValidity('')">
        <input type="url" name="website" placeholder="网址（可选）">
        <textarea name="content" required placeholder="评论内容" oninvalid="this.setCustomValidity('请输入内容')" oninput="this.setCustomValidity('')" style="font-size: 1em"></textarea>
        <button type="submit" id="cmt-send">发送</button>
    `;
    return form;
}

// 加载评论列表
async function loadComments() {
    const commentList = document.getElementById("cmt-list");
    if (!commentList) return;

    const url = window.location.href;
    const encodedUrl = btoa(url);
    try {
        const response = await fetch(Comment.getApiUrl(`/api/v1/comment/${encodedUrl}`));
        if (!response.ok) throw new Error("加载评论失败");

        const comments = await response.json();
        commentList.innerHTML = "";
        comments.forEach((comment) => {
            const commentItem = document.createElement("div");
            commentItem.className = "cmt-item";
            commentItem.innerHTML = `
                <div class="cmt-header">
                    <strong class="cmt-author">
                        ${comment.website
                ? `<a href="${comment.website}" target="_blank" rel="noopener noreferrer">${comment.username}</a>`
                : comment.username}
                    </strong>
                    <span class="cmt-date">${new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(comment.createdAt))}</span>
                </div>
                <div class="cmt-content">${comment.content}</div>
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
    if (!response.ok) throw new Error("获取 PoW 挑战失败");
    return response.json();
}

// 使用 Web Crypto API 计算 nonce
async function calculateNonce(timestamp, random, difficulty) {
    const target = "0".repeat(difficulty);
    let nonce = 0;

    while (true) {
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
}

// 提交评论
async function submitComment(commentData, timestamp, random, nonce) {
    const response = await fetch(Comment.getApiUrl("/api/v1/comment"), {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            c_timestamp: timestamp,
            c_random: random,
            c_nonce: nonce,
        },
        body: JSON.stringify(commentData),
    });
    return response;
}
