/* Trang quản trị do server Java phục vụ (src/main/resources/admin, không qua React build).
 * Cùng origin với API nên cookie `token` / `refreshToken` được gửi tự động.
 * Sự kiện mới (video, báo cáo, tin nhắn của người đăng...) đẩy về qua WebSocket /ws/admin. */
(() => {
  'use strict';

  // ============================================================
  // Helpers
  // ============================================================
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

  /** Tạo element an toàn (text luôn đi qua textContent, không innerHTML dữ liệu người dùng) */
  function h(tag, attrs = {}, ...children) {
    const el = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs || {})) {
      if (v == null || v === false) continue;
      if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
      else if (k === 'class') el.className = v;
      else if (k in el && typeof v !== 'string') el[k] = v;
      else el.setAttribute(k, v === true ? '' : v);
    }
    for (const c of children.flat()) {
      if (c == null || c === false) continue;
      el.append(c instanceof Node ? c : document.createTextNode(String(c)));
    }
    return el;
  }

  const fmtNum = (n) => Number(n || 0).toLocaleString('vi-VN');
  const fmtDate = (d) => (d ? new Date(d).toLocaleString('vi-VN') : '—');
  const fmtBytes = (b) => `${(b / 1024 / 1024).toFixed(1)} MB`;
  function fmtDuration(sec) {
    const d = Math.floor(sec / 86400), hh = Math.floor((sec % 86400) / 3600), mm = Math.floor((sec % 3600) / 60);
    return [d && `${d} ngày`, hh && `${hh} giờ`, `${mm} phút`].filter(Boolean).join(' ');
  }
  /** "4:59" / "6 ngày 23:59:59" / "đã hết hạn" */
  function fmtLeft(ms) {
    if (ms <= 0) return 'đã hết hạn';
    const s = Math.floor(ms / 1000);
    const d = Math.floor(s / 86400), hh = Math.floor((s % 86400) / 3600), mm = Math.floor((s % 3600) / 60);
    const sec = String(s % 60).padStart(2, '0');
    const hms = hh ? `${hh}:${String(mm).padStart(2, '0')}:${sec}` : `${mm}:${sec}`;
    return d ? `${d} ngày ${hms}` : hms;
  }

  let toastTimer;
  function toast(msg, isError = false) {
    const el = $('#toast');
    // Dialog mở bằng showModal() nằm trên top layer, che mọi thứ ở body -> đưa toast vào trong dialog
    (document.querySelector('dialog[open]') || document.body).append(el);
    el.textContent = msg;
    el.className = `toast${isError ? ' error' : ''}`;
    el.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => (el.hidden = true), 3000);
  }

  const store = {
    get(key, fallback) {
      try { return JSON.parse(localStorage.getItem(key)) ?? fallback; } catch { return fallback; }
    },
    set(key, value) {
      try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* private mode */ }
    },
  };

  // ============================================================
  // API (tự refresh token 1 lần khi access token hết hạn)
  // ============================================================
  let refreshing = null;
  function refreshSession() {
    // Nhiều request cùng 401 thì chỉ refresh 1 lần
    refreshing ??= fetch('/api/user/token-refresh', { method: 'POST', credentials: 'same-origin' })
      .then((r) => r.ok)
      .catch(() => false)
      .finally(() => setTimeout(() => (refreshing = null), 0));
    return refreshing;
  }

  async function api(path, { method = 'GET', body } = {}) {
    const send = () => fetch(path, {
      method,
      credentials: 'same-origin',
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });

    let res = await send();
    if (res.status === 401) {
      const data = await res.clone().json().catch(() => ({}));
      if (data.code !== 'NO_TOKEN' && await refreshSession()) res = await send();
    }

    const data = await res.json().catch(() => ({}));
    if (res.status === 401) { showLogin(); throw new Error('Phiên đăng nhập đã hết hạn'); }
    if (res.status === 403) { showLogin('Tài khoản này không có quyền admin'); throw new Error('Không có quyền'); }
    if (!res.ok) throw new Error(data.message || data.error || `HTTP ${res.status}`);
    return data;
  }

  // Chạy action, báo lỗi bằng toast
  async function attempt(fn, okMsg) {
    try {
      const r = await fn();
      if (okMsg) toast(okMsg);
      return r;
    } catch (err) {
      toast(err.message, true);
    }
  }

  // ============================================================
  // Auth
  // ============================================================
  let me = null;
  let live = null; // kết nối /ws/admin, tạo ở cuối file

  function showLogin(message) {
    me = null;
    live?.stop();
    $('#app').hidden = true;
    $('#login-screen').hidden = false;
    const err = $('#login-error');
    err.hidden = !message;
    err.textContent = message || '';
  }

  function showApp(user) {
    me = user;
    $('#login-screen').hidden = true;
    $('#app').hidden = false;
    $('#me').replaceChildren(h('strong', {}, user.username), h('small', {}, user.email));
    route();
    refreshNavCounts();
    live?.start();
  }

  async function checkSession() {
    let res = await fetch('/api/user/userinfo', { credentials: 'same-origin' });
    if (res.status === 401 && await refreshSession()) {
      res = await fetch('/api/user/userinfo', { credentials: 'same-origin' });
    }
    if (!res.ok) return showLogin();
    const user = await res.json();
    if (user.role !== 'admin') return showLogin(`Tài khoản ${user.email} không có quyền admin`);
    showApp(user);
  }

  $('#login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = new FormData(e.target);
    const res = await fetch('/api/user/login', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: form.get('email'), password: form.get('password') }),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) return showLogin(data.message || 'Đăng nhập thất bại');
    if (data.user?.role !== 'admin') return showLogin('Tài khoản này không có quyền admin');
    e.target.reset();
    checkSession();
  });

  $('#logout-btn').addEventListener('click', async () => {
    await fetch('/api/user/logout', { method: 'POST', credentials: 'same-origin' });
    showLogin();
  });

  // ============================================================
  // Router (#hash)
  // ============================================================
  const views = {};
  let current = null;

  function route() {
    if (!me) return;
    const name = location.hash.slice(1) || 'dashboard';
    const view = views[name] ? name : 'dashboard';
    $$('#nav a').forEach((a) => a.classList.toggle('active', a.dataset.view === view));
    $$('[data-page]').forEach((p) => (p.hidden = p.dataset.page !== view));
    if (current !== view) views[current]?.leave?.();
    current = view;
    views[view].load();
  }
  window.addEventListener('hashchange', route);

  // Nút "Tải lại" & form lọc trong từng trang
  $$('[data-page]').forEach((page) => {
    const name = page.dataset.page;
    page.querySelectorAll('[data-reload]').forEach((b) => b.addEventListener('click', () => views[name].load()));
    page.querySelector('[data-filter]')?.addEventListener('submit', (e) => {
      e.preventDefault();
      views[name].page = 1;
      views[name].load();
    });
  });

  const filtersOf = (name) => Object.fromEntries(new FormData($(`[data-page="${name}"] [data-filter]`)));

  function renderTable(table, headers, rows, emptyText = 'Không có dữ liệu') {
    const head = h('thead', {}, h('tr', {}, headers.map((t) => h('th', {}, t))));
    const body = h('tbody', {}, rows.length
      ? rows
      : h('tr', {}, h('td', { class: 'empty', colSpan: headers.length }, emptyText)));
    table.replaceChildren(head, body);
  }

  function renderPager(el, { total, page, limit }, view) {
    const pages = Math.max(1, Math.ceil(total / limit));
    const go = (p) => { view.page = p; view.load(); };
    el.replaceChildren(
      `${fmtNum(total)} mục · trang ${page}/${pages}`,
      h('button', { class: 'btn small', disabled: page <= 1, onclick: () => go(page - 1) }, '‹ Trước'),
      h('button', { class: 'btn small', disabled: page >= pages, onclick: () => go(page + 1) }, 'Sau ›'),
    );
  }

  const query = (obj) => new URLSearchParams(Object.entries(obj).filter(([, v]) => v !== '' && v != null)).toString();

  // ============================================================
  // Dashboard
  // ============================================================
  const stat = (label, value, sub) =>
    h('div', { class: 'stat' }, h('div', { class: 'label' }, label), h('div', { class: 'value' }, value), sub && h('div', { class: 'sub' }, sub));

  // Số báo cáo đang chờ cạnh menu
  function setNavCounts({ pending, usersPending, appeals }) {
    $('#nav-count-reports').textContent = pending ? pending : '';
    $('#nav-count-user-reports').textContent = usersPending ? usersPending : '';
    // Người đăng nhắn từ Studio, chưa ai trả lời
    $('#nav-count-videos').textContent = appeals ? appeals : '';
  }
  const refreshNavCounts = () => api('/api/admin/stats').then((s) => setNavCounts(s.reports)).catch(() => {});

  const banBadge = (ban) => ban && h('span', {
    class: 'badge bad',
    title: `${ban.permanent ? 'Vĩnh viễn' : `Đến ${fmtDate(ban.until)}`}${ban.reason ? ` · ${ban.reason}` : ''}`,
  }, ban.permanent ? 'Khoá vĩnh viễn' : `Khoá đến ${new Date(ban.until).toLocaleDateString('vi-VN')}`);

  // ============================================================
  // Khoá / mở khoá tài khoản (dùng chung cho trang Người dùng, Báo cáo, chi tiết user)
  // ============================================================
  /** Mở dialog chọn thời hạn + lý do. @returns {Promise<{days:number, reason:string}|null>} */
  function askBan(username, reason = '') {
    const dlg = $('#ban-dialog');
    const form = $('#ban-form');
    $('#ban-title').textContent = `Khoá tài khoản ${username}`;
    form.reset();
    form.reason.value = reason;
    dlg.returnValue = ''; // Esc không đổi returnValue -> reset để khỏi dính 'ok' của lần trước
    dlg.showModal();
    return new Promise((resolve) => {
      dlg.addEventListener('close', () => resolve(dlg.returnValue === 'ok'
        ? { days: Number(form.days.value), reason: form.reason.value.trim() }
        : null), { once: true });
    });
  }

  async function banUser(user, reason) {
    const input = await askBan(user.username, reason);
    if (!input) return false;
    const r = await attempt(() => api(`/api/admin/users/${user.id}/ban`, { method: 'POST', body: input }));
    if (!r) return false;
    toast(`Đã khoá ${user.username}${r.resolvedReports ? `, đóng ${r.resolvedReports} báo cáo` : ''}`);
    refreshNavCounts();
    return true;
  }

  async function unbanUser(user) {
    if (!confirm(`Mở khoá tài khoản ${user.username}?`)) return false;
    return !!(await attempt(() => api(`/api/admin/users/${user.id}/ban`, { method: 'DELETE' }), 'Đã mở khoá'));
  }

  // ============================================================
  // Chi tiết người dùng (drawer)
  // ============================================================
  const AUDIT_LABELS = {
    'user.role': 'Đổi role',
    'user.ban': 'Khoá tài khoản',
    'user.unban': 'Mở khoá',
    'user.purge_comments': 'Xoá mọi bình luận',
    'user.videos_status': 'Ẩn/hiện mọi video',
    'user.delete': 'Xoá tài khoản',
    'video.status': 'Ẩn/hiện video',
    'video.bulk_status': 'Ẩn/hiện nhiều video',
    'video.block': 'Chặn video',
    'video.approve': 'Duyệt video',
    'video.reply': 'Trả lời người đăng',
    'comment.delete': 'Xoá bình luận',
    'report_comment.status': 'Xử lý báo cáo bình luận',
    'report_user.status': 'Xử lý báo cáo người dùng',
    'notification.broadcast': 'Gửi thông báo',
    'logs.clear': 'Xoá log truy cập',
  };
  const auditLabel = (a) => AUDIT_LABELS[a] || a;

  /** Tóm tắt detail của 1 dòng audit thành chữ dễ đọc */
  function auditDetail(e) {
    const d = e.detail || {};
    switch (e.action) {
      case 'user.role': return `→ ${d.role}`;
      case 'user.ban': return `${d.days ? `${d.days} ngày` : 'vĩnh viễn'}${d.reason ? ` · ${d.reason}` : ''}`;
      case 'user.purge_comments': return `${fmtNum(d.deleted)} bình luận`;
      case 'user.delete': return `kèm ${fmtNum(d.videos)} video, ${fmtNum(d.comments)} bình luận`;
      case 'user.videos_status':
      case 'video.status': return d.status === 1 ? 'hiện' : 'ẩn';
      case 'video.bulk_status': return `${d.status === 1 ? 'hiện' : 'ẩn'} ${d.ids?.length || 0} video`;
      case 'video.block': return [d.rules?.map(ruleLabel).join(', '), d.atTime != null && `tại ${fmtClock(d.atTime)}`, d.note]
        .filter(Boolean).join(' · ');
      case 'video.approve': return d.note || 'đạt chuẩn';
      case 'video.reply': return d.content || '';
      case 'report_comment.status': return d.status === 1 ? 'đã xử lý' : 'mở lại';
      case 'report_user.status': return d.status;
      case 'notification.broadcast': return `"${d.title}" → ${d.audience} (${fmtNum(d.sent)})`;
      default: return Object.keys(d).length ? JSON.stringify(d) : '';
    }
  }

  // Tải lại trang hiện tại sau khi thao tác trong drawer
  const reloadCurrent = () => views[current]?.load();

  async function openUser(id) {
    const dlg = $('#user-dialog');
    const box = $('#user-detail');
    box.replaceChildren(h('p', { class: 'muted' }, 'Đang tải…'));
    if (!dlg.open) dlg.showModal();
    const u = await attempt(() => api(`/api/admin/users/${id}`));
    if (!u) return dlg.close();

    const c = u.counts;
    const isMe = u.id === me.id;
    const after = async (ok) => { if (ok) { await openUser(id); reloadCurrent(); } };
    const action = (label, cls, fn) => h('button', { class: `btn small ${cls}`, onclick: async () => after(await fn()) }, label);

    box.replaceChildren(
      h('header', { class: 'drawer-head' },
        h('img', { class: 'avatar lg', src: u.avatar || '/default-avatar.png', alt: '' }),
        h('div', { class: 'grow' },
          h('h2', {}, u.username, ' ', h('span', { class: `badge ${u.role === 'admin' ? 'info' : ''}` }, u.role)),
          h('div', { class: 'muted' }, u.email),
          h('div', { class: 'id' }, u.id),
          h('div', { class: 'muted small' },
            `Đăng nhập: ${u.loginMethods.join(', ') || '—'} · Tham gia: ${u.created_at ? fmtDate(u.created_at) : 'không rõ'}`)),
        h('button', { class: 'btn small ghost', onclick: () => dlg.close() }, '✕')),

      u.ban ? h('div', { class: 'callout bad' },
        h('strong', {}, u.ban.permanent ? 'Đang bị khoá vĩnh viễn' : `Đang bị khoá đến ${fmtDate(u.ban.until)}`),
        u.ban.reason && h('div', {}, `Lý do: ${u.ban.reason}`)) : '',

      h('div', { class: 'stats compact' },
        stat('Video', fmtNum(c.videos), `${fmtNum(c.visibleVideos)} đang hiện`),
        stat('Lượt xem', fmtNum(c.views)),
        stat('Bình luận', fmtNum(c.comments), `${fmtNum(c.commentReports)} lần bị báo cáo`),
        stat('Follower', fmtNum(c.followers), `đang theo dõi ${fmtNum(c.following)}`),
        stat('Bị báo cáo', fmtNum(c.reportsTotal), `${fmtNum(c.reportsPending)} đang chờ`),
      ),

      h('div', { class: 'filters' },
        !isMe && u.role !== 'admin' && (u.ban
          ? action('Mở khoá', '', () => unbanUser(u))
          : action('Khoá tài khoản', 'danger', () => banUser(u))),
        c.visibleVideos > 0 && action('Ẩn mọi video', 'danger', () => {
          if (!confirm(`Ẩn toàn bộ ${c.visibleVideos} video đang hiện của ${u.username}?`)) return false;
          return attempt(() => api(`/api/admin/users/${u.id}/videos/status`, { method: 'PATCH', body: { status: 0 } }), 'Đã ẩn video');
        }),
        c.videos > c.visibleVideos && action('Hiện lại mọi video', '', () =>
          attempt(() => api(`/api/admin/users/${u.id}/videos/status`, { method: 'PATCH', body: { status: 1 } }), 'Đã hiện video')),
        c.comments > 0 && action('Xoá mọi bình luận', 'danger', () => {
          if (!confirm(`Xoá toàn bộ ${c.comments} bình luận của ${u.username} (kèm các trả lời)? Không hoàn tác được.`)) return false;
          return attempt(() => api(`/api/admin/users/${u.id}/comments`, { method: 'DELETE' }), 'Đã xoá bình luận');
        }),
        h('a', { class: 'btn small ghost', href: '#broadcast', onclick: () => { dlg.close(); prefillBroadcast(u.id); } }, 'Gửi thông báo'),
        // Xoá xong thì user không còn để mở lại -> đóng drawer thay vì dùng action()
        !isMe && u.role !== 'admin' && h('button', {
          class: 'btn small danger',
          onclick: async () => {
            if (!confirm(`Xoá hẳn tài khoản ${u.username} cùng ${c.videos} video, ${c.comments} bình luận, follow, thư viện...? Không hoàn tác được.`)) return;
            if (!(await attempt(() => api(`/api/admin/users/${u.id}`, { method: 'DELETE' }), 'Đã xoá tài khoản'))) return;
            dlg.close();
            reloadCurrent();
            refreshNavCounts();
          },
        }, 'Xoá tài khoản'),
      ),

      h('h3', {}, 'Bình luận gần đây'),
      u.recentComments.length
        ? h('ul', { class: 'plain-list' }, u.recentComments.map((cm) => h('li', {},
          h('div', {}, cm.content),
          h('div', { class: 'muted small' }, `${cm.video_title || '(video đã xoá)'} · ${fmtDate(cm.created_at)}`))))
        : h('p', { class: 'muted' }, 'Chưa có bình luận'),

      h('h3', {}, 'Lịch sử xử lý'),
      u.history.length
        ? h('ul', { class: 'plain-list' }, u.history.map((e) => h('li', {},
          h('strong', {}, auditLabel(e.action)), ' ', h('span', {}, auditDetail(e)),
          h('div', { class: 'muted small' }, `${e.admin_name || '?'} · ${fmtDate(e.created_at)}`))))
        : h('p', { class: 'muted' }, 'Chưa bị xử lý lần nào'),
    );
  }
  $('#user-dialog').addEventListener('click', (e) => { if (e.target.id === 'user-dialog') e.target.close(); });

  const userLink = (id, name) => id
    ? h('a', { href: '#', class: 'user-link', onclick: (e) => { e.preventDefault(); openUser(id); } }, name || id)
    : '—';

  const videoLink = (id, title) =>
    h('a', { href: '#', class: 'user-link', onclick: (e) => { e.preventDefault(); openReview(id); } }, title || id);

  /** 75 -> "1:15", 3725 -> "1:02:05" */
  function fmtClock(sec) {
    const s = Math.max(0, Math.floor(sec || 0));
    const hh = Math.floor(s / 3600), mm = Math.floor((s % 3600) / 60), ss = String(s % 60).padStart(2, '0');
    return hh ? `${hh}:${String(mm).padStart(2, '0')}:${ss}` : `${mm}:${ss}`;
  }

  // ============================================================
  // Duyệt video: player của frontend trong iframe + checklist tiêu chuẩn cộng đồng
  // ============================================================
  let videoRules = null;
  const loadRules = async () => (videoRules ??= await api('/api/admin/video-rules'));
  const ruleLabel = (id) => videoRules?.find((r) => r.id === id)?.label || id;

  const MODERATION = {
    approved: ['ok', 'Đã duyệt'],
    blocked: ['bad', 'Đã chặn'],
    resubmitted: ['warn', 'Chờ duyệt lại'],
  };
  const moderationBadge = (m) => {
    const [tone, label] = MODERATION[m?.state] || ['warn', 'Chờ duyệt'];
    return h('span', { class: `badge ${tone}`, title: m?.rules?.length ? m.rules.map(ruleLabel).join(', ') : '' }, label);
  };
  const appealBadge = (appeal) => appeal?.pending && h('span', { class: 'badge info', title: 'Người đăng đã nhắn từ Studio' }, 'Cần phản hồi');

  const MESSAGE_ROLE = { creator: 'Người đăng', admin: 'Admin', system: 'Hệ thống' };

  const messageItem = (msg) => h('li', { class: `msg ${msg.role}` },
    h('div', { class: 'muted small' },
      `${MESSAGE_ROLE[msg.role] || msg.role}${msg.sender_name ? ` · ${msg.sender_name}` : ''} · ${fmtDate(msg.created_at)}`),
    h('div', {}, msg.content));

  /**
   * Luồng trao đổi với người đăng + ô trả lời (trong màn duyệt video).
   * Tin mới (người đăng nhắn, admin khác trả lời) được thêm vào ngay qua WebSocket: appendThreadMessage.
   */
  function messageThread(v, onSent) {
    const empty = h('li', { class: 'muted small' }, 'Chưa có trao đổi nào. Người đăng có thể nhắn từ trang Studio.');
    const list = h('ul', { class: 'thread' }, v.messages.length ? v.messages.map(messageItem) : empty);
    review.thread = { videoId: v.id, list, empty, ids: new Set(v.messages.map((m) => m.id)) };
    requestAnimationFrame(() => (list.scrollTop = list.scrollHeight));

    const input = h('textarea', { name: 'content', rows: 2, maxLength: 1000, required: true,
      placeholder: 'Trả lời người đăng (Enter để gửi, Shift+Enter xuống dòng). Họ nhận thông báo kèm link tới Studio' });
    const send = h('button', { class: 'btn small primary' }, 'Gửi trả lời');
    const form = h('form', { class: 'form-grid' }, input, h('div', { class: 'filters' }, send));
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); form.requestSubmit(); }
    });
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const content = input.value.trim();
      if (!content || send.disabled) return;
      send.disabled = true;
      const r = await attempt(() => api(`/api/admin/videos/${v.id}/messages`, { method: 'POST', body: { content } }));
      send.disabled = false;
      if (!r) return;
      // Không mở lại cả màn duyệt: giữ nguyên checklist / ghi chú đang điền
      appendThreadMessage(r.item);
      input.value = '';
      input.focus();
      onSent();
    });
    return h('div', {}, list, form);
  }

  /** Thêm 1 tin vào luồng đang mở (bỏ qua nếu đã có). Trả true nếu đang mở đúng video đó */
  function appendThreadMessage(msg) {
    const t = review.thread;
    if (!t || !reviewDialog.open || t.videoId !== msg.video_id) return false;
    if (t.ids.has(msg.id)) return true;
    t.ids.add(msg.id);
    t.empty.remove();
    const nearBottom = t.list.scrollHeight - t.list.scrollTop - t.list.clientHeight < 60;
    t.list.append(messageItem(msg));
    if (nearBottom || msg.role === 'admin') t.list.scrollTop = t.list.scrollHeight;
    return true;
  }

  const review = { id: null, time: 0, origin: null, queue: [], thread: null };
  const reviewFrame = $('#review-frame');
  const reviewDialog = $('#review-dialog');

  // Player trong iframe báo thời điểm đang xem (pages/admin/VideoReview.js)
  window.addEventListener('message', (e) => {
    if (e.source !== reviewFrame.contentWindow || e.origin !== review.origin) return;
    if (e.data?.type !== 'review:time') return;
    review.time = e.data.t;
    const now = $('#review-now');
    if (now) now.textContent = fmtClock(review.time);
  });

  const seekPlayer = (t) => reviewFrame.contentWindow?.postMessage({ type: 'review:seek', t }, review.origin);

  reviewDialog.addEventListener('close', () => {
    reviewFrame.src = 'about:blank'; // dừng phát
    delete reviewFrame.dataset.src;
    review.id = null;
    review.thread = null;
  });
  reviewDialog.addEventListener('click', (e) => { if (e.target === reviewDialog) reviewDialog.close(); });

  /**
   * Mở màn duyệt 1 video. queue: danh sách id để bấm "tiếp" sau khi ra quyết định
   * (mặc định lấy theo thứ tự trong bảng Video đang hiện).
   */
  async function openReview(id, queue = review.queue) {
    review.queue = queue;
    const side = $('#review-side');
    side.replaceChildren(h('p', { class: 'muted' }, 'Đang tải…'));
    if (!reviewDialog.open) reviewDialog.showModal();

    const [v] = await Promise.all([attempt(() => api(`/api/admin/videos/${id}`)), attempt(loadRules)]);
    if (!v || !videoRules) return reviewDialog.close();

    review.id = v.id;
    review.time = 0;
    review.origin = new URL(v.reviewUrl).origin;
    if (reviewFrame.dataset.src !== v.reviewUrl) {
      reviewFrame.src = v.reviewUrl;
      reviewFrame.dataset.src = v.reviewUrl;
    }
    $('#review-frame-hint').textContent =
      `Player lấy từ frontend (${review.origin}). Không hiện thì kiểm tra đã chạy "npm start" và CLIENT_URL trong server-java/.env. Admin xem không tính lượt view.`;

    const m = v.moderation;
    const up = v.uploader;
    const next = review.queue[review.queue.indexOf(v.id) + 1];

    const form = h('form', { class: 'form-grid review-form' },
      h('fieldset', { class: 'rules' },
        h('legend', {}, 'Vi phạm tiêu chuẩn nào?'),
        videoRules.map((r) => h('label', { class: 'rule' },
          h('input', { type: 'checkbox', name: 'rules', value: r.id, checked: m?.state === 'blocked' && m.rules.includes(r.id) }),
          h('span', {}, h('strong', {}, r.label), h('small', { class: 'muted' }, r.hint))))),
      h('label', { class: 'check' },
        h('input', { type: 'checkbox', name: 'withTime', checked: true }),
        'Ghi mốc vi phạm tại ', h('strong', { id: 'review-now' }, fmtClock(review.time)),
        h('span', { class: 'muted small' }, ' (thời điểm đang dừng trên player)')),
      h('label', {}, 'Ghi chú (người đăng sẽ thấy khi bị chặn)',
        h('textarea', { name: 'note', rows: 3, maxLength: 1000 }, m?.note || '')),
      h('div', { class: 'filters' },
        h('button', { type: 'button', class: 'btn danger', onclick: () => decide('block') }, 'Chặn video'),
        h('button', { type: 'button', class: 'btn primary', onclick: () => decide('approve') }, 'Đạt chuẩn, cho hiện'),
        next && h('button', { type: 'button', class: 'btn ghost', onclick: () => openReview(next) }, 'Bỏ qua, xem tiếp ›')),
    );

    async function decide(decision) {
      const fd = new FormData(form);
      const body = {
        decision,
        rules: fd.getAll('rules'),
        note: fd.get('note'),
        atTime: decision === 'block' && fd.get('withTime') ? review.time : null,
      };
      if (decision === 'approve' && body.rules.length && !confirm('Đang tick tiêu chuẩn vi phạm mà lại cho hiện video?')) return;
      if (decision === 'block' && !confirm(`Chặn video "${v.title}" và báo cho ${up?.username || 'người đăng'}?`)) return;
      const r = await attempt(() => api(`/api/admin/videos/${v.id}/moderate`, { method: 'POST', body }),
        decision === 'block' ? 'Đã chặn video và báo người đăng' : 'Đã duyệt video');
      if (!r) return;
      if (current === 'videos') views.videos.load();
      refreshNavCounts();
      next ? openReview(next) : reviewDialog.close();
    }

    // replaceChildren in cả false/null thành chữ -> lọc bỏ các phần không hiện
    side.replaceChildren(...[
      h('header', { class: 'drawer-head' },
        h('div', { class: 'grow' },
          h('h2', {}, v.title || '(không tên)'),
          h('div', { class: 'filters' }, videoStatusBadge(v.status), moderationBadge(m), appealBadge(v.appeal),
            h('span', { class: 'badge' }, v.video_group), h('span', { class: 'muted small' }, fmtDate(v.uploadAt))),
          h('div', { class: 'id' }, v.id)),
        h('button', { class: 'btn small ghost', onclick: () => reviewDialog.close() }, '✕')),

      m?.state === 'blocked' && h('div', { class: 'callout bad' },
        h('strong', {}, `Đã chặn bởi ${m.by_name || '?'} · ${fmtDate(m.at)}`),
        h('div', {}, m.rules.map(ruleLabel).join(', ')),
        m.note && h('div', {}, m.note),
        m.at_time != null && h('button', { class: 'btn small', onclick: () => seekPlayer(m.at_time) }, `Tua tới ${fmtClock(m.at_time)}`)),
      m?.state === 'resubmitted' && h('div', { class: 'callout warn' },
        h('strong', {}, 'Người đăng đã sửa và gửi duyệt lại'),
        v.media_updated_at && h('div', {}, `Thay file video lúc ${fmtDate(v.media_updated_at)}`),
        h('div', {}, `Lần chặn trước (${m.by_name || '?'} · ${fmtDate(m.at)}): ${m.rules.map(ruleLabel).join(', ')}`),
        m.note && h('div', {}, m.note)),
      m?.state === 'approved' && h('p', { class: 'muted small' }, `Đã duyệt bởi ${m.by_name || '?'} · ${fmtDate(m.at)}`),

      up && h('div', { class: 'uploader' },
        h('img', { class: 'avatar', src: up.avatar || '/default-avatar.png', alt: '' }),
        h('div', { class: 'grow' },
          userLink(up.id, up.username), ' ', banBadge(up.ban),
          h('div', { class: 'muted small' }, `${fmtNum(up.videos)} video`,
            up.blockedVideos ? h('span', { class: 'badge bad' }, ` ${up.blockedVideos} video từng bị chặn`) : ''))),

      h('div', { class: 'stats compact' },
        stat('Lượt xem', fmtNum(v.stats.view)),
        stat('Thích', fmtNum(v.stats.likes), `${fmtNum(v.stats.dislikes)} không thích`),
        stat('Bình luận', fmtNum(v.stats.comments))),

      v.tags.length ? h('div', { class: 'filters' }, v.tags.map((t) => h('span', { class: 'badge' }, `#${t}`))) : '',
      v.description ? h('details', { class: 'desc' }, h('summary', {}, 'Mô tả'), h('p', {}, v.description)) : '',

      h('h3', {}, 'Quyết định'),
      form,

      h('h3', {}, `Trao đổi với người đăng${v.messages.length ? ` (${v.messages.length})` : ''}`),
      messageThread(v, () => { refreshNavCounts(); if (current === 'videos') views.videos.load(); }),

      v.history.length ? h('h3', {}, 'Lịch sử') : '',
      v.history.length ? h('ul', { class: 'plain-list' }, v.history.map((e) => h('li', {},
        h('strong', {}, auditLabel(e.action)), ' ', auditDetail(e),
        h('div', { class: 'muted small' }, `${e.admin_name || '?'} · ${fmtDate(e.created_at)}`)))) : '',
    ].filter((x) => x != null && x !== false));
  }

  const videoStatusBadge = (s) =>
    Number(s) === 1 ? h('span', { class: 'badge ok' }, 'Đang hiện') : h('span', { class: 'badge bad' }, 'Đã ẩn');

  views.dashboard = {
    async load() {
      const s = await attempt(() => api('/api/admin/stats'));
      if (!s) return;
      $('#stats').replaceChildren(
        stat('Người dùng', fmtNum(s.users.total), `${s.users.admins} admin · ${s.users.banned} bị khoá`),
        stat('Video', fmtNum(s.videos.total), `${fmtNum(s.videos.visible)} đang hiện`),
        stat('Lượt xem', fmtNum(s.videos.views)),
        stat('Bình luận', fmtNum(s.comments.total)),
        stat('Báo cáo bình luận chờ', fmtNum(s.reports.pending), `tổng ${fmtNum(s.reports.total)}`),
        stat('Báo cáo người dùng chờ', fmtNum(s.reports.usersPending)),
        stat('Tag', fmtNum(s.tags.total)),
      );
      setNavCounts(s.reports);
      renderTable($('#recent-videos'), ['Tiêu đề', 'Người đăng', 'Lượt xem', 'Trạng thái', 'Ngày đăng'],
        s.recentVideos.map((v) => h('tr', {},
          h('td', { class: 'clip' }, v.title || '(không tên)'),
          h('td', {}, v.username || '—'),
          h('td', { class: 'num' }, fmtNum(v.view)),
          h('td', {}, videoStatusBadge(v.status)),
          h('td', {}, fmtDate(v.uploadAt)),
        )));
    },
  };

  // ============================================================
  // Users
  // ============================================================
  views.users = {
    page: 1,
    async load() {
      const data = await attempt(() => api(`/api/admin/users?${query({ ...filtersOf('users'), page: this.page })}`));
      if (!data) return;
      renderTable($('#users-table'), ['Người dùng', 'Email', 'Trạng thái', 'Video', 'Báo cáo chờ', 'Follower', ''],
        data.items.map((u) => {
          const isMe = u.id === me.id;
          const nextRole = u.role === 'admin' ? 'user' : 'admin';
          return h('tr', {},
            h('td', {},
              h('img', { class: 'avatar', src: u.avatar || '/default-avatar.png', alt: '', loading: 'lazy' }),
              userLink(u.id, u.username), isMe && h('span', { class: 'muted small' }, ' (bạn)'),
              h('div', { class: 'id' }, u.id)),
            h('td', {}, u.email),
            h('td', {}, h('span', { class: `badge ${u.role === 'admin' ? 'info' : ''}` }, u.role || 'user'), ' ', banBadge(u.ban)),
            h('td', { class: 'num' }, fmtNum(u.videos)),
            h('td', { class: 'num' }, u.pendingReports ? h('span', { class: 'badge warn' }, fmtNum(u.pendingReports)) : '0'),
            h('td', { class: 'num' }, fmtNum(u.followers)),
            h('td', { class: 'actions' },
              !isMe && u.role !== 'admin' && h('button', {
                class: `btn small ${u.ban ? '' : 'danger'}`,
                onclick: async () => { if (await (u.ban ? unbanUser(u) : banUser(u))) this.load(); },
              }, u.ban ? 'Mở khoá' : 'Khoá'),
              h('button', {
                class: 'btn small', disabled: isMe,
                onclick: async () => {
                  if (!confirm(`Đổi role của ${u.username} thành "${nextRole}"?`)) return;
                  const ok = await attempt(() => api(`/api/admin/users/${u.id}/role`, { method: 'PATCH', body: { role: nextRole } }), 'Đã đổi role');
                  if (ok) this.load();
                },
              }, nextRole === 'admin' ? 'Cấp admin' : 'Bỏ admin')),
          );
        }));
      renderPager($('#users-pager'), data, this);
    },
  };

  // ============================================================
  // Videos
  // ============================================================
  views.videos = {
    page: 1,
    selected: new Set(),
    renderBulk() {
      const n = this.selected.size;
      $('#videos-bulk').hidden = !n;
      $('#videos-bulk-count').textContent = `Đã chọn ${n} video`;
    },
    async load() {
      const data = await attempt(() => api(`/api/admin/videos?${query({ ...filtersOf('videos'), page: this.page })}`));
      if (!data) return;
      const boxes = [];
      const all = h('input', {
        type: 'checkbox', title: 'Chọn cả trang',
        onchange: (e) => boxes.forEach((b) => { b.checked = e.target.checked; b.onchange(); }),
      });
      loadRules().catch(() => {}); // để tooltip badge hiện tên tiêu chuẩn
      const ids = data.items.map((v) => v.id);
      renderTable($('#videos-table'), [all, '', 'Tiêu đề', 'Loại', 'Người đăng', 'Lượt xem', 'Bình luận', 'Trạng thái', 'Duyệt', 'Ngày đăng', ''],
        data.items.map((v) => {
          const visible = Number(v.status) === 1;
          const box = h('input', { type: 'checkbox', checked: this.selected.has(v.id) });
          box.onchange = () => { box.checked ? this.selected.add(v.id) : this.selected.delete(v.id); this.renderBulk(); };
          boxes.push(box);
          return h('tr', {},
            h('td', {}, box),
            h('td', {}, v.thumbnail && h('img', {
              class: 'thumb clickable', src: v.thumbnail, alt: '', loading: 'lazy', title: 'Xem & duyệt',
              onclick: () => openReview(v.id, ids),
            })),
            h('td', { class: 'clip' }, v.title || '(không tên)', h('div', { class: 'id' }, v.id)),
            h('td', {}, v.video_group),
            h('td', {}, userLink(v.user_id, v.username)),
            h('td', { class: 'num' }, fmtNum(v.view)),
            h('td', { class: 'num' }, fmtNum(v.comments)),
            h('td', {}, videoStatusBadge(v.status)),
            h('td', {}, moderationBadge(v.moderation), ' ', appealBadge(v.appeal)),
            h('td', {}, fmtDate(v.uploadAt)),
            h('td', { class: 'actions' },
              h('button', { class: 'btn small primary', onclick: () => openReview(v.id, ids) }, 'Xem & duyệt'),
              h('button', {
                class: `btn small ${visible ? 'danger' : ''}`,
                onclick: async () => {
                  const ok = await attempt(() => api(`/api/admin/videos/${v.id}/status`, { method: 'PATCH', body: { status: visible ? 0 : 1 } }),
                    visible ? 'Đã ẩn video' : 'Đã hiện video');
                  if (ok) this.load();
                },
              }, visible ? 'Ẩn' : 'Hiện')),
          );
        }));
      renderPager($('#videos-pager'), data, this);
      this.renderBulk();
    },
  };
  $$('[data-bulk-status]').forEach((b) => b.addEventListener('click', async () => {
    const view = views.videos;
    const status = Number(b.dataset.bulkStatus);
    if (!confirm(`${status ? 'Hiện' : 'Ẩn'} ${view.selected.size} video đã chọn?`)) return;
    const r = await attempt(() => api('/api/admin/videos/status', { method: 'PATCH', body: { ids: [...view.selected], status } }));
    if (!r) return;
    toast(`Đã cập nhật ${r.modified} video`);
    view.selected.clear();
    view.load();
  }));
  $('#videos-bulk-clear').addEventListener('click', () => { views.videos.selected.clear(); views.videos.load(); });

  // ============================================================
  // Comment reports
  // ============================================================
  views.reports = {
    page: 1,
    async load() {
      const data = await attempt(() => api(`/api/admin/reports/comments?${query({ ...filtersOf('reports'), page: this.page })}`));
      if (!data) return;
      renderTable($('#reports-table'), ['Bình luận', 'Video', 'Lý do', 'Người báo cáo', 'Thời gian', 'Trạng thái', ''],
        data.items.map((r) => {
          const done = Number(r.status) === 1;
          const deleted = r.comment_content == null;
          return h('tr', {},
            h('td', { class: 'clip' },
              deleted ? h('em', { class: 'muted' }, '(đã xoá)') : r.comment_content,
              r.comment_author && h('div', { class: 'muted small' }, `— ${r.comment_author}`)),
            h('td', { class: 'clip' }, r.video_title || '—'),
            h('td', { class: 'clip' }, r.reason, r.detail && h('div', { class: 'muted small' }, r.detail)),
            h('td', {}, r.reporter || '—'),
            h('td', {}, fmtDate(r.created_at)),
            h('td', {}, done ? h('span', { class: 'badge ok' }, 'Đã xử lý') : h('span', { class: 'badge warn' }, 'Chờ xử lý')),
            h('td', { class: 'actions' },
              h('button', {
                class: 'btn small',
                onclick: async () => {
                  const ok = await attempt(() => api(`/api/admin/reports/comments/${r.id}/status`, { method: 'PATCH', body: { status: done ? 0 : 1 } }),
                    done ? 'Đã mở lại báo cáo' : 'Đã đánh dấu xử lý');
                  if (ok) { this.load(); refreshNavCounts(); }
                },
              }, done ? 'Mở lại' : 'Bỏ qua'),
              !deleted && h('button', {
                class: 'btn small danger',
                onclick: async () => {
                  if (!confirm('Xoá bình luận này (kèm các trả lời)?')) return;
                  const ok = await attempt(() => api(`/api/admin/comments/${r.comment_id}`, { method: 'DELETE' }), 'Đã xoá bình luận');
                  if (ok) { this.load(); refreshNavCounts(); }
                },
              }, 'Xoá bình luận')),
          );
        }));
      renderPager($('#reports-pager'), data, this);
    },
  };

  // ============================================================
  // User reports
  // ============================================================
  const USER_REPORT_STATUS = {
    pending: ['warn', 'Chờ xử lý'],
    resolved: ['ok', 'Đã xử lý'],
    dismissed: ['', 'Đã bỏ qua'],
  };

  views['user-reports'] = {
    page: 1,
    async load() {
      const data = await attempt(() => api(`/api/admin/reports/users?${query({ ...filtersOf('user-reports'), page: this.page })}`));
      if (!data) return;
      const setStatus = async (r, status, msg) => {
        const ok = await attempt(() => api(`/api/admin/reports/users/${r.id}/status`, { method: 'PATCH', body: { status } }), msg);
        if (ok) { this.load(); refreshNavCounts(); }
      };
      renderTable($('#user-reports-table'), ['Bị báo cáo', 'Lý do', 'Người báo cáo', 'Thời gian', 'Trạng thái', ''],
        data.items.map((r) => {
          const [tone, label] = USER_REPORT_STATUS[r.status] || ['', r.status];
          const target = { id: r.reported_user, username: r.reported_name };
          const canBan = r.reported_name && r.reported_role !== 'admin' && !r.reported_ban;
          return h('tr', {},
            h('td', {},
              h('img', { class: 'avatar', src: r.reported_avatar || '/default-avatar.png', alt: '', loading: 'lazy' }),
              userLink(r.reported_user, r.reported_name || '(đã xoá)'),
              r.pending_against > 1 && h('span', { class: 'badge warn', title: 'Số báo cáo đang chờ về người này' }, ` ×${r.pending_against}`),
              ' ', banBadge(r.reported_ban)),
            h('td', { class: 'clip' }, r.reason, r.detail && h('div', { class: 'muted small' }, r.detail)),
            h('td', {}, userLink(r.reporter, r.reporter_name)),
            h('td', {}, fmtDate(r.created_at)),
            h('td', {}, h('span', { class: `badge ${tone}` }, label)),
            h('td', { class: 'actions' },
              r.status === 'pending'
                ? [
                  h('button', { class: 'btn small', onclick: () => setStatus(r, 'dismissed', 'Đã bỏ qua báo cáo') }, 'Bỏ qua'),
                  h('button', { class: 'btn small', onclick: () => setStatus(r, 'resolved', 'Đã đánh dấu xử lý') }, 'Đã xử lý'),
                  canBan && h('button', {
                    class: 'btn small danger',
                    onclick: async () => { if (await banUser(target, r.reason)) this.load(); },
                  }, 'Khoá'),
                ]
                : h('button', { class: 'btn small', onclick: () => setStatus(r, 'pending', 'Đã mở lại báo cáo') }, 'Mở lại')),
          );
        }), 'Không có báo cáo nào');
      renderPager($('#user-reports-pager'), data, this);
    },
  };

  // ============================================================
  // Analytics (biểu đồ SVG tự vẽ, không cần thư viện)
  // ============================================================
  function svg(tag, attrs = {}, ...children) {
    const el = document.createElementNS('http://www.w3.org/2000/svg', tag);
    for (const [k, v] of Object.entries(attrs)) if (v != null) el.setAttribute(k, v);
    children.flat().forEach((c) => c != null && el.append(c instanceof Node ? c : document.createTextNode(String(c))));
    return el;
  }

  /** Biểu đồ cột theo ngày, hover từng cột để xem số */
  function barChart(title, labels, values, color) {
    const W = 600, H = 140, pad = { t: 8, r: 4, b: 2, l: 4 };
    const max = Math.max(1, ...values);
    const total = values.reduce((a, b) => a + b, 0);
    const slot = (W - pad.l - pad.r) / values.length;
    const bw = Math.max(1, slot - (values.length > 40 ? 1 : 3));
    const y = (v) => pad.t + (H - pad.t - pad.b) * (1 - v / max);
    const day = (d) => d.slice(5).split('-').reverse().join('/');

    return h('div', { class: 'chart' },
      h('div', { class: 'chart-head' },
        h('span', { class: 'muted' }, title),
        h('strong', {}, fmtNum(total)),
        h('span', { class: 'muted small' }, `cao nhất ${fmtNum(Math.max(...values))}/ngày`)),
      svg('svg', { viewBox: `0 0 ${W} ${H}`, preserveAspectRatio: 'none', role: 'img', 'aria-label': `${title}: ${total}` },
        svg('line', { x1: pad.l, x2: W - pad.r, y1: H - pad.b, y2: H - pad.b, class: 'baseline' }),
        values.map((v, i) => svg('rect', {
          x: pad.l + slot * i, y: y(v), width: bw, height: Math.max(v ? 2 : 0, H - pad.b - y(v)), rx: 2, fill: color,
        }, svg('title', {}, `${day(labels[i])}: ${fmtNum(v)}`)))),
      // Nhãn trục để ngoài SVG: SVG co giãn theo chiều ngang (preserveAspectRatio none) làm méo chữ
      h('div', { class: 'axis' },
        h('span', {}, day(labels[0])), h('span', {}, day(labels[Math.floor(labels.length / 2)])), h('span', {}, day(labels.at(-1)))),
    );
  }

  /** So sánh nửa sau với nửa đầu của khoảng thời gian -> "+12%" */
  function trend(values) {
    const half = Math.floor(values.length / 2);
    const a = values.slice(0, half).reduce((x, y) => x + y, 0);
    const b = values.slice(half).reduce((x, y) => x + y, 0);
    if (!a) return b ? 'mới có số liệu' : 'không đổi';
    const pct = Math.round(((b - a) / a) * 100);
    return `${pct >= 0 ? '+' : ''}${pct}% so với nửa đầu kỳ`;
  }

  views.analytics = {
    async load() {
      const a = await attempt(() => api(`/api/admin/analytics?${query(filtersOf('analytics'))}`));
      if (!a) return;
      const sum = (arr) => arr.reduce((x, y) => x + y, 0);
      const { like, dislike } = a.reactions;
      const ratio = like + dislike ? Math.round((like / (like + dislike)) * 100) : 0;

      $('#analytics-kpis').replaceChildren(
        stat(`Người dùng mới (${a.days} ngày)`, fmtNum(sum(a.series.users)), trend(a.series.users)),
        stat(`Video mới (${a.days} ngày)`, fmtNum(sum(a.series.videos)), trend(a.series.videos)),
        stat(`Bình luận (${a.days} ngày)`, fmtNum(sum(a.series.comments)), trend(a.series.comments)),
        stat('Tỉ lệ thích', `${ratio}%`, `${fmtNum(like)} 👍 · ${fmtNum(dislike)} 👎`),
      );
      $('#analytics-charts').replaceChildren(
        barChart('Người dùng đăng ký', a.labels, a.series.users, 'var(--accent)'),
        barChart('Video được đăng', a.labels, a.series.videos, 'var(--ok)'),
        barChart('Bình luận', a.labels, a.series.comments, '#c084fc'),
        barChart('Báo cáo (bình luận + người dùng)', a.labels, a.series.reports, 'var(--bad)'),
      );

      renderTable($('#top-videos'), ['#', 'Video', 'Lượt xem', 'Thích', 'Bình luận'],
        a.topVideos.map((v, i) => h('tr', {},
          h('td', { class: 'muted' }, i + 1),
          h('td', { class: 'clip' }, v.title || '(không tên)', ' ', Number(v.status) !== 1 && videoStatusBadge(v.status),
            h('div', { class: 'muted small' }, v.username || '—')),
          h('td', { class: 'num' }, fmtNum(v.view)),
          h('td', { class: 'num' }, fmtNum(v.likes)),
          h('td', { class: 'num' }, fmtNum(v.comments)))));

      renderTable($('#top-creators'), ['#', 'Người đăng', 'Video', 'Tổng lượt xem', 'Follower'],
        a.topCreators.map((c, i) => h('tr', {},
          h('td', { class: 'muted' }, i + 1),
          h('td', {}, userLink(c.id, c.username)),
          h('td', { class: 'num' }, fmtNum(c.videos)),
          h('td', { class: 'num' }, fmtNum(c.views)),
          h('td', { class: 'num' }, fmtNum(c.followers)))));

      const maxTag = Math.max(1, ...a.topTags.map((t) => t.videos));
      $('#top-tags').replaceChildren(...(a.topTags.length
        ? a.topTags.map((t) => h('div', { class: 'tag-bar' },
          h('span', { class: 'tag-name' }, `#${t.name}`),
          h('span', { class: 'tag-track' }, h('span', { class: 'tag-fill', style: `width:${(t.videos / maxTag) * 100}%` })),
          h('span', { class: 'num' }, fmtNum(t.videos))))
        : [h('p', { class: 'muted' }, 'Chưa có tag nào')]));
    },
  };

  // ============================================================
  // Broadcast
  // ============================================================
  const broadcastForm = $('#broadcast-form');
  const syncAudience = () => {
    const single = broadcastForm.audience.value === 'user';
    $('[data-audience-user]').hidden = !single;
    broadcastForm.userId.required = single;
  };
  broadcastForm.audience.addEventListener('change', syncAudience);

  function prefillBroadcast(userId) {
    broadcastForm.audience.value = 'user';
    broadcastForm.userId.value = userId;
    syncAudience();
  }

  broadcastForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const body = Object.fromEntries(new FormData(broadcastForm));
    const who = { all: 'TẤT CẢ người dùng', admins: 'mọi admin', user: `user ${body.userId}` }[body.audience];
    if (!confirm(`Gửi thông báo "${body.title}" tới ${who}?`)) return;
    const r = await attempt(() => api('/api/admin/notifications/broadcast', { method: 'POST', body }));
    if (!r) return;
    toast(`Đã gửi tới ${fmtNum(r.sent)} người`);
    broadcastForm.title.value = '';
    broadcastForm.content.value = '';
  });

  views.broadcast = { load: syncAudience };

  // ============================================================
  // Thanh toán
  // ============================================================
  const fmtVnd = (n) => `${fmtNum(n)}đ`;
  const PAY_STATUS = {
    paid: ['ok', 'Thành công'], pending: ['warn', 'Chờ thanh toán'], failed: ['bad', 'Thất bại'], expired: ['info', 'Hết hạn'],
  };

  views.payments = {
    page: 1,
    async load() {
      const data = await attempt(() => api(`/api/admin/payments?${query({ ...filtersOf('payments'), page: this.page })}`));
      if (!data) return;
      const s = data.summary;
      $('#payments-stats').replaceChildren(
        stat('Doanh thu (đã thanh toán)', fmtVnd(s.gross), `${fmtNum(s.paidOrders)} đơn`),
        stat('Phí nền tảng', fmtVnd(s.platformFee)),
        stat('Đang chờ', fmtNum(s.pending)),
        stat('Thất bại', fmtNum(s.failed)),
        stat('Cổng', data.providers.map((p) => `${p.id}${p.enabled ? '' : ' (tắt)'}`).join(', ')),
      );
      renderTable($('#payments-table'), ['Thời gian', 'Người mua', 'Kênh', 'Nội dung', 'Cổng', 'Số tiền', 'Trạng thái', 'Mã đơn'],
        data.items.map((o) => {
          const [tone, label] = PAY_STATUS[o.status] || ['info', o.status];
          return h('tr', {},
            h('td', {}, fmtDate(o.createdAt)),
            h('td', {}, userLink(o.buyerId, o.buyerName || o.buyerId)),
            h('td', {}, userLink(o.channelId, o.channelName || o.channelId)),
            h('td', { class: 'clip' }, o.type === 'membership'
              ? `Hội viên ${o.tierName || ''} · ${o.months} tháng`
              : ['Super Thanks · ', o.videoId ? videoLink(o.videoId, o.videoTitle || o.videoId) : '—']),
            h('td', {}, o.provider),
            h('td', {}, fmtVnd(o.amount)),
            h('td', {}, h('span', { class: `badge ${tone}`, title: o.note || '' }, label)),
            h('td', { class: 'id', title: o.providerTxn ? `Mã GD cổng: ${o.providerTxn}` : '' }, o.id.slice(0, 8)),
          );
        }), 'Chưa có giao dịch nào');
      renderPager($('#payments-pager'), data, this);
    },
  };

  // ============================================================
  // Audit log
  // ============================================================
  views.audit = {
    page: 1,
    async load() {
      const data = await attempt(() => api(`/api/admin/audit?${query({ ...filtersOf('audit'), page: this.page })}`));
      if (!data) return;
      await loadRules().catch(() => {}); // tên tiêu chuẩn cho các dòng "Chặn video"
      // Danh sách hành động lấy từ DB, giữ lựa chọn đang chọn
      const select = $('#audit-actions');
      const chosen = select.value;
      select.replaceChildren(h('option', { value: '' }, 'Mọi hành động'),
        ...data.actions.map((a) => h('option', { value: a }, auditLabel(a))));
      select.value = chosen;

      renderTable($('#audit-table'), ['Thời gian', 'Admin', 'Hành động', 'Đối tượng', 'Chi tiết', 'IP'],
        data.items.map((e) => h('tr', {},
          h('td', {}, fmtDate(e.created_at)),
          h('td', {}, userLink(e.admin_id, e.admin_name)),
          h('td', {}, h('span', { class: `badge ${/^user\.ban$|^video\.block$|delete|purge|clear/.test(e.action) ? 'bad' : 'info'}` }, auditLabel(e.action))),
          h('td', {}, e.target_type === 'user' && e.target_id
            ? userLink(e.target_id, e.target_label || e.target_id)
            : e.target_type === 'video' && e.target_id
            ? videoLink(e.target_id, e.target_label || e.target_id)
            : e.target_id ? h('span', { class: 'id' }, `${e.target_type} ${e.target_id}`) : '—'),
          h('td', { class: 'clip' }, auditDetail(e)),
          h('td', { class: 'id' }, e.ip || '—'),
        )), 'Chưa có thao tác nào');
      renderPager($('#audit-pager'), data, this);
    },
  };

  // ============================================================
  // Logs
  // ============================================================
  views.logs = {
    async load() {
      const data = await attempt(() => api('/api/admin/logs?limit=500'));
      if (!data) return;
      $('#logs-summary').textContent = `Hiển thị ${fmtNum(data.items.length)} / ${fmtNum(data.total)} bản ghi (mới nhất trước)`;
      renderTable($('#logs-table'), ['Thời gian', 'IP', 'Referrer', 'User agent'],
        data.items.map((l) => h('tr', {},
          h('td', {}, fmtDate(l.timestamp)),
          h('td', { class: 'id' }, l.ip),
          h('td', { class: 'clip' }, l.referrer),
          h('td', { class: 'clip muted small' }, l.userAgent),
        )), 'Chưa có log nào');
    },
  };
  $('#clear-logs').addEventListener('click', async () => {
    if (!confirm('Xoá toàn bộ log truy cập?')) return;
    const ok = await attempt(() => api('/api/admin/logs', { method: 'DELETE' }), 'Đã xoá log');
    if (ok) views.logs.load();
  });

  // ============================================================
  // System
  // ============================================================
  views.system = {
    async load() {
      const s = await attempt(() => api('/api/admin/system'));
      if (!s) return;
      $('#system-info').replaceChildren(
        stat('Node', s.node, s.platform),
        stat('Uptime', fmtDuration(s.uptime), `PID ${s.pid}`),
        stat('RAM (RSS)', fmtBytes(s.memory.rss)),
        stat('Heap', fmtBytes(s.memory.heapUsed), `/ ${fmtBytes(s.memory.heapTotal)}`),
      );
      renderTable($('#system-config'), ['Biến', 'Giá trị'],
        Object.entries(s.config).map(([k, v]) => h('tr', {}, h('td', { class: 'id' }, k), h('td', {}, String(v ?? '—')))));
    },
  };

  // ============================================================
  // Test API
  // ============================================================
  // Endpoint có sẵn (lấy từ các router trong server/controllers & server/Route)
  const PRESETS = {
    'Auth': [
      ['POST', '/api/user/login', { email: 'admin@test.local', password: '123456' }],
      ['POST', '/api/user/register', { username: 'test', email: 'test@test.local', password: '123456' }],
      ['POST', '/api/user/token-refresh'],
      ['GET', '/api/user/token-status'],
      ['POST', '/api/user/logout'],
    ],
    'Profile': [
      ['GET', '/api/user/userinfo'],
      ['GET', '/api/user/userdata/:id'],
      ['POST', '/api/user/update', { username: 'newname' }],
      ['POST', '/api/user/change-password', { email: '', oldPassword: '', newPassword: '' }],
      ['GET', '/api/user/notification'],
      ['GET', '/api/user/notification/unread/count'],
      ['POST', '/api/user/notification/read-all'],
      ['GET', '/api/user/create-captcha'],
    ],
    'Video': [
      ['GET', '/api/videos/all'],
      ['GET', '/api/videos/more'],
      ['GET', '/api/videos/watch/:slug/:id'],
      ['GET', '/api/videos/user/:id'],
      ['GET', '/api/videos/next/:videoId'],
      ['GET', '/api/videos/prev/:videoId'],
      ['GET', '/api/videos/viewlike/:slug/:id/stat'],
      ['POST', '/api/videos/viewlike/:slug/:id/like'],
      ['GET', '/api/videos/comments/:videoId'],
      ['POST', '/api/videos/comments/:videoId', { content: 'Bình luận test' }],
      ['GET', '/api/videos/library'],
    ],
    'Khác': [
      ['GET', '/api/search', null, [['q', 'random']]],
      ['GET', '/api/tag'],
      ['GET', '/api/tag/suggest', null, [['q', '']]],
      ['GET', '/api/tag/:slug'],
      ['GET', '/api/follow/:id'],
      ['POST', '/api/follow/:id'],
      ['DELETE', '/api/follow/:id'],
    ],
    'Admin': [
      ['GET', '/api/admin/stats'],
      ['GET', '/api/admin/analytics', null, [['days', '30']]],
      ['GET', '/api/admin/system'],
      ['GET', '/api/admin/users/:id'],
      ['POST', '/api/admin/users/:id/ban', { days: 7, reason: 'Spam' }],
      ['DELETE', '/api/admin/users/:id/ban'],
      ['PATCH', '/api/admin/videos/status', { ids: [], status: 0 }],
      ['GET', '/api/admin/video-rules'],
      ['GET', '/api/admin/videos/:id'],
      ['POST', '/api/admin/videos/:id/moderate', { decision: 'block', rules: ['spam'], note: '', atTime: 0 }],
      ['GET', '/api/admin/reports/users', null, [['status', 'pending']]],
      ['POST', '/api/admin/notifications/broadcast', { title: 'Bảo trì', content: 'Hệ thống bảo trì lúc 23h', audience: 'admins' }],
      ['GET', '/api/admin/audit'],
      ['GET', '/api/admin/users', null, [['search', ''], ['page', '1'], ['limit', '20']]],
      ['PATCH', '/api/admin/users/:id/role', { role: 'user' }],
      ['GET', '/api/admin/videos', null, [['search', ''], ['status', '']]],
      ['PATCH', '/api/admin/videos/:id/status', { status: 0 }],
      ['GET', '/api/admin/reports/comments', null, [['status', '0']]],
      ['GET', '/api/admin/logs', null, [['limit', '50']]],
    ],
  };

  const tester = {
    params: [{ key: '', value: '', enabled: true }],
    headers: [{ key: 'Content-Type', value: 'application/json', enabled: true }],
    history: store.get('admin.testapi.history', []),
    lastBody: '',
  };

  // --- key/value editor ---
  function renderKV(kind) {
    const list = tester[kind];
    $(`#kv-${kind}`).replaceChildren(...list.map((row, i) => h('div', { class: 'kv-row' },
      h('input', { type: 'checkbox', checked: row.enabled, onchange: (e) => (row.enabled = e.target.checked) }),
      h('input', { type: 'text', placeholder: 'key', value: row.key, oninput: (e) => (row.key = e.target.value) }),
      h('input', { type: 'text', placeholder: 'value', value: row.value, oninput: (e) => (row.value = e.target.value) }),
      h('button', { type: 'button', class: 'x', title: 'Xoá', onclick: () => { list.splice(i, 1); renderKV(kind); } }, '×'),
    )));
  }
  $$('[data-add-kv]').forEach((b) => b.addEventListener('click', () => {
    tester[b.dataset.addKv].push({ key: '', value: '', enabled: true });
    renderKV(b.dataset.addKv);
  }));

  // --- tabs ---
  $$('[data-tabs]').forEach((bar) => bar.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-tab]');
    if (!btn) return;
    const group = bar.dataset.tabs;
    $$('[data-tab]', bar).forEach((b) => b.classList.toggle('active', b === btn));
    $$(`[data-tabpanel^="${group}:"]`).forEach((p) => (p.hidden = p.dataset.tabpanel !== `${group}:${btn.dataset.tab}`));
  }));

  // --- presets ---
  const presetSelect = $('#req-preset');
  Object.entries(PRESETS).forEach(([group, items]) => {
    presetSelect.append(h('optgroup', { label: group },
      items.map(([m, url], i) => h('option', { value: `${group}|${i}` }, `${m}  ${url}`))));
  });
  presetSelect.addEventListener('change', () => {
    if (!presetSelect.value) return;
    const [group, i] = presetSelect.value.split('|');
    const [m, url, body, params] = PRESETS[group][i];
    applyRequest({
      method: m,
      url,
      body: body ? JSON.stringify(body, null, 2) : '',
      params: (params || []).map(([key, value]) => ({ key, value, enabled: true })),
    });
    presetSelect.value = '';
    $('#req-url').focus();
  });

  function applyRequest({ method, url, body, params, headers }) {
    $('#req-method').value = method;
    $('#req-url').value = url;
    if (body !== undefined) $('#req-body').value = body;
    tester.params = params?.length ? params : [{ key: '', value: '', enabled: true }];
    if (headers) tester.headers = headers;
    renderKV('params');
    renderKV('headers');
  }

  $('#fmt-json').addEventListener('click', () => {
    try {
      $('#req-body').value = JSON.stringify(JSON.parse($('#req-body').value), null, 2);
    } catch {
      toast('JSON không hợp lệ', true);
    }
  });

  function buildUrl() {
    let url = $('#req-url').value.trim();
    if (!/^https?:\/\//.test(url) && !url.startsWith('/')) url = `/${url}`;
    const qs = tester.params
      .filter((p) => p.enabled && p.key.trim())
      .map((p) => `${encodeURIComponent(p.key.trim())}=${encodeURIComponent(p.value)}`)
      .join('&');
    return qs ? `${url}${url.includes('?') ? '&' : '?'}${qs}` : url;
  }

  function statusBadge(el, code, text) {
    const tone = code === 0 || code >= 500 ? 'bad' : code >= 400 ? 'warn' : code >= 200 && code < 300 ? 'ok' : '';
    el.className = `badge ${tone}`;
    el.textContent = code ? `${code} ${text}` : text;
  }

  $('#req-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const method = $('#req-method').value;
    const url = buildUrl();
    const headers = Object.fromEntries(tester.headers.filter((x) => x.enabled && x.key.trim()).map((x) => [x.key.trim(), x.value]));
    const hasBody = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) && $('#req-body').value.trim();
    const options = { method, headers, credentials: 'include', body: hasBody ? $('#req-body').value : undefined };

    const sendBtn = $('#req-send');
    sendBtn.disabled = true;
    $('#resp-status').className = 'badge';
    $('#resp-status').textContent = 'Đang gửi…';
    const start = performance.now();
    let refreshed = false;

    try {
      let res = await fetch(url, options);
      if (res.status === 401 && $('#req-autorefresh').checked && !url.includes('/token-refresh')) {
        const data = await res.clone().json().catch(() => ({}));
        if (data.code === 'TOKEN_EXPIRED' || data.code === 'TOKEN_INVALID') {
          refreshed = await refreshSession();
          tokenLog(refreshed ? 'Test API: 401 -> đã refresh, gửi lại request' : 'Test API: 401 -> refresh thất bại', refreshed ? 'ok' : 'error');
          if (refreshed) res = await fetch(url, options);
        }
      }
      const ms = Math.round(performance.now() - start);
      const text = await res.text();
      let pretty = text;
      try { pretty = JSON.stringify(JSON.parse(text), null, 2); } catch { /* không phải JSON */ }

      tester.lastBody = pretty;
      statusBadge($('#resp-status'), res.status, res.statusText || (res.ok ? 'OK' : 'Error'));
      $('#resp-time').textContent = `${ms} ms${refreshed ? ' (có refresh)' : ''}`;
      $('#resp-size').textContent = `${(new Blob([text]).size / 1024).toFixed(2)} KB`;
      $('#resp-body').textContent = pretty || '(trống)';
      $('#resp-headers').textContent = [...res.headers].map(([k, v]) => `${k}: ${v}`).join('\n');

      addHistory({ method, url: $('#req-url').value, fullUrl: url, status: res.status, ms,
        params: tester.params, headers: tester.headers, body: $('#req-body').value });
    } catch (err) {
      statusBadge($('#resp-status'), 0, 'Network error');
      $('#resp-time').textContent = `${Math.round(performance.now() - start)} ms`;
      $('#resp-size').textContent = '';
      $('#resp-body').textContent = err.message;
      $('#resp-headers').textContent = '';
    } finally {
      sendBtn.disabled = false;
      if (url.startsWith('/api/user/')) loadTokenStatus();
    }
  });

  $('#resp-copy').addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(tester.lastBody);
      toast('Đã copy response');
    } catch {
      toast('Không copy được', true);
    }
  });

  // --- history ---
  function addHistory(item) {
    tester.history = [{ ...item, at: Date.now() }, ...tester.history].slice(0, 30);
    store.set('admin.testapi.history', tester.history);
    renderHistory();
  }
  function renderHistory() {
    const list = $('#hist-list');
    if (!tester.history.length) return list.replaceChildren(h('li', { class: 'muted' }, 'Chưa có request nào'));
    list.replaceChildren(...tester.history.map((it) => h('li', {
      title: 'Nạp lại request này',
      onclick: () => applyRequest({
        method: it.method, url: it.url, body: it.body,
        params: structuredClone(it.params), headers: structuredClone(it.headers),
      }),
    },
      h('span', { class: 'h-url' }, h('span', { class: `m m-${it.method}` }, it.method), it.fullUrl),
      h('span', { class: 'h-meta' }, `${it.status} · ${it.ms} ms · ${new Date(it.at).toLocaleTimeString('vi-VN')}`),
    )));
  }
  $('#hist-clear').addEventListener('click', () => {
    tester.history = [];
    store.set('admin.testapi.history', []);
    renderHistory();
  });

  // --- token status ---
  let tokenStatus = null, clockOffset = 0, tokenTimer = null;

  function tokenLog(text, tone = 'info') {
    const ul = $('#tk-log');
    ul.hidden = false;
    ul.prepend(h('li', { class: tone }, h('time', {}, new Date().toLocaleTimeString('vi-VN')), text));
    while (ul.children.length > 20) ul.lastChild.remove();
  }

  function renderTokenBox(el, label, cookie, info) {
    const left = info?.present ? info.expiresAt - (Date.now() + clockOffset) : 0;
    const state = !info?.present ? 'missing' : left <= 0 || !info.valid ? 'expired' : 'ok';
    const badge = { ok: ['ok', 'hợp lệ'], expired: ['bad', 'hết hạn'], missing: ['', 'không có'] }[state];
    el.replaceChildren(
      h('div', { class: 'row' },
        h('div', {}, h('strong', {}, label), h('div', { class: 'id' }, `cookie: ${cookie}`)),
        h('span', { class: `badge ${badge[0]}` }, badge[1])),
      info?.present && h('div', { class: 'left' }, fmtLeft(left),
        h('span', { class: 'muted small' }, `  (${new Date(info.expiresAt).toLocaleTimeString('vi-VN')})`)),
    );
  }

  function renderTokens() {
    renderTokenBox($('#tk-access'), 'Access token', 'token', tokenStatus?.access);
    renderTokenBox($('#tk-refresh-box'), 'Refresh token', 'refreshToken', tokenStatus?.refresh);
  }

  async function loadTokenStatus() {
    try {
      const res = await fetch('/api/user/token-status', { credentials: 'same-origin' });
      tokenStatus = await res.json();
      clockOffset = tokenStatus.now - Date.now();
      $('#token-config').textContent =
        `ACCESS_TOKEN_EXPIRES=${tokenStatus.config.accessExpires} · REFRESH_TOKEN_EXPIRES=${tokenStatus.config.refreshExpires}`;
      renderTokens();
    } catch (err) {
      tokenLog(`Không lấy được trạng thái token: ${err.message}`, 'error');
    }
  }

  $('#tk-reload').addEventListener('click', loadTokenStatus);
  $('#tk-refresh').addEventListener('click', async () => {
    const ok = await refreshSession();
    tokenLog(ok ? 'Refresh thủ công: thành công' : 'Refresh thủ công: thất bại', ok ? 'ok' : 'error');
    loadTokenStatus();
  });
  $('#tk-protected').addEventListener('click', async () => {
    // Gọi thẳng (không auto refresh) để thấy đúng mã lỗi server trả về
    const res = await fetch('/api/user/userinfo', { credentials: 'same-origin' });
    const data = await res.json().catch(() => ({}));
    tokenLog(`GET /api/user/userinfo -> ${res.status} ${res.ok ? data.username : data.code || ''}`, res.ok ? 'ok' : 'error');
    loadTokenStatus();
  });

  views['test-api'] = {
    load() {
      renderKV('params');
      renderKV('headers');
      renderHistory();
      loadTokenStatus();
      clearInterval(tokenTimer);
      tokenTimer = setInterval(renderTokens, 1000);
    },
    leave() {
      clearInterval(tokenTimer);
    },
  };

  // ============================================================
  // Realtime: WebSocket /ws/admin
  // Server đẩy sự kiện ngay khi có video mới, báo cáo mới, người đăng nhắn tin, admin khác thao tác...
  // -> hiện thông báo (bấm để xử lý), cập nhật số đếm menu, tải lại trang đang xem.
  // ============================================================
  const FEED_MAX = 50;
  const baseTitle = document.title;
  const feed = { items: [], unread: 0 };
  const pendingReload = new Set();
  let navTimer = null;
  const reloadTimers = {};

  const preview = (text, n = 80) => (text.length > n ? `${text.slice(0, n)}…` : text);

  // Nhiều sự kiện liền nhau chỉ gọi /stats 1 lần
  function refreshNavSoon() {
    clearTimeout(navTimer);
    navTimer = setTimeout(refreshNavCounts, 400);
  }

  /** Tải lại trang đang xem nếu không làm gián đoạn admin (đang mở dialog / ở trang 2+ / đang gõ ô lọc) */
  function touch(names) {
    if (!names.includes(current)) return;
    const view = views[current];
    const typing = document.activeElement?.closest?.(`[data-page="${current}"] [data-filter]`);
    if (document.querySelector('dialog[open]') || (view.page && view.page > 1) || typing) {
      pendingReload.add(current);
      return;
    }
    const name = current;
    clearTimeout(reloadTimers[name]);
    reloadTimers[name] = setTimeout(() => current === name && views[name].load(), 500);
  }
  // Dialog đóng thì tải lại phần đã hoãn
  $$('dialog').forEach((d) => d.addEventListener('close', () => {
    if (!pendingReload.has(current) || document.querySelector('dialog[open]')) return;
    pendingReload.delete(current);
    views[current].load();
  }));
  window.addEventListener('hashchange', () => pendingReload.delete(current));

  // ---- Danh sách thông báo (chuông ở sidebar) ----
  function renderFeed() {
    $('#feed-count').textContent = feed.unread ? feed.unread : '';
    document.title = `${feed.unread && document.hidden ? `(${feed.unread}) ` : ''}${baseTitle}`;
    const list = $('#feed-list');
    list.replaceChildren(...(feed.items.length
      ? feed.items.map((it) => h('li', { class: it.action ? 'clickable' : '', onclick: () => it.action?.() },
        h('div', {}, it.text), h('div', { class: 'muted small' }, fmtDate(it.at))))
      : [h('li', { class: 'muted small' }, 'Chưa có thông báo mới.')]));
    const perm = $('#feed-desktop');
    perm.hidden = !('Notification' in window) || Notification.permission !== 'default';
  }

  function addFeed(text, action) {
    feed.items.unshift({ text, action, at: new Date() });
    feed.items.length = Math.min(feed.items.length, FEED_MAX);
    if ($('#feed').hidden || document.hidden) feed.unread++;
    renderFeed();
  }

  /** Thông báo cần xử lý: vào danh sách + toast có nút + thông báo hệ điều hành khi tab đang ẩn */
  function notify(text, actionLabel, action) {
    addFeed(text, action);
    const box = $('#live-toasts');
    (document.querySelector('dialog[open]') || document.body).append(box);
    const item = h('div', { class: 'live-toast' },
      h('span', { class: 'grow' }, text),
      action && h('button', { class: 'btn small primary', onclick: () => { item.remove(); action(); } }, actionLabel),
      h('button', { class: 'btn small ghost', title: 'Đóng', onclick: () => item.remove() }, '✕'));
    box.append(item);
    while (box.children.length > 4) box.firstElementChild.remove();
    setTimeout(() => item.remove(), 10000);

    if (document.hidden && 'Notification' in window && Notification.permission === 'granted') {
      const n = new Notification('hello-react admin', { body: text, tag: text });
      n.onclick = () => { window.focus(); n.close(); action?.(); };
    }
  }

  $('#feed-btn').addEventListener('click', () => {
    const panel = $('#feed');
    panel.hidden = !panel.hidden;
    if (!panel.hidden) feed.unread = 0;
    renderFeed();
  });
  $('#feed-clear').addEventListener('click', () => { feed.items = []; feed.unread = 0; renderFeed(); });
  $('#feed-desktop').addEventListener('click', async () => {
    await Notification.requestPermission();
    renderFeed();
  });
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden && !$('#feed').hidden) feed.unread = 0;
    renderFeed();
  });

  const openReviewOf = (id) => () => openReview(id, []);
  const goTo = (hash) => () => { location.hash = hash; };
  const ACTION_VIEWS = {
    video: ['videos', 'dashboard'],
    user: ['users', 'user-reports', 'dashboard'],
    report: ['reports', 'user-reports'],
    comment: ['reports'],
  };

  function handleEvent(ev) {
    switch (ev.type) {
      case 'video:new':
        notify(`Video mới: “${ev.title}” · ${ev.uploader}`, 'Duyệt', openReviewOf(ev.videoId));
        touch(['dashboard', 'videos']);
        break;
      case 'video:media':
        notify(`${ev.uploader} vừa thay file video, cần duyệt lại`, 'Duyệt', openReviewOf(ev.videoId));
        touch(['videos']);
        break;
      case 'video:message': {
        const m = ev.message;
        const inThread = appendThreadMessage(m);
        if (m.role === 'creator' && !inThread) {
          notify(`${m.sender_name || 'Người đăng'} nhắn: “${preview(m.content)}”`, 'Trả lời', openReviewOf(ev.videoId));
        } else if (m.role === 'creator' && document.hidden) {
          notify(`${m.sender_name || 'Người đăng'} nhắn: “${preview(m.content)}”`);
        }
        touch(['videos']);
        break;
      }
      case 'report:comment':
        notify(`Báo cáo bình luận mới: ${preview(ev.reason)}`, 'Xem', goTo('#reports'));
        touch(['reports', 'dashboard']);
        break;
      case 'report:user':
        notify(`Báo cáo người dùng mới: ${preview(ev.reason)}`, 'Xem', goTo('#user-reports'));
        touch(['user-reports']);
        break;
      case 'user:new':
        addFeed(`Người dùng mới: ${ev.username}`, () => openUser(ev.userId));
        touch(['users', 'dashboard']);
        return; // không đổi số đếm menu
      case 'admin:action': {
        touch(['audit']);
        if (ev.adminId === me?.id) return; // thao tác của chính mình đã cập nhật ở chỗ bấm
        const label = `${ev.adminName}: ${auditLabel(ev.action)}${ev.targetLabel ? ` · ${ev.targetLabel}` : ''}`;
        // Video đang duyệt vừa được admin khác ra quyết định -> báo ngay để không xử lý trùng
        if (ev.targetType === 'video' && ev.targetId === review.id) notify(`${label} (video bạn đang mở)`);
        else addFeed(label, ev.targetType === 'video' && ev.targetId ? openReviewOf(ev.targetId) : null);
        touch(ACTION_VIEWS[ev.targetType] || []);
        break;
      }
      default:
        return;
    }
    refreshNavSoon();
  }

  live = (() => {
    let ws = null;
    let running = false;
    let retry = 0;
    let retryTimer = null;
    let pingTimer = null;

    function setStatus(state) {
      const el = $('#live-status');
      el.dataset.state = state;
      $('#live-label').textContent = { on: 'Realtime: đang kết nối', connecting: 'Realtime: đang nối…', off: 'Realtime: mất kết nối, thử lại…' }[state];
    }

    /** Refresh token cho lần nối lại. 'expired' = refresh token cũng hết hạn -> phải đăng nhập lại */
    async function renew() {
      try {
        const r = await fetch('/api/user/token-refresh', { method: 'POST', credentials: 'same-origin' });
        return r.ok ? 'ok' : r.status === 401 ? 'expired' : 'retry';
      } catch {
        return 'retry';
      }
    }

    function connect() {
      clearTimeout(retryTimer);
      if (!running || (ws && ws.readyState <= WebSocket.OPEN)) return;
      let opened = false;
      setStatus('connecting');
      const socket = new WebSocket(`${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/admin`);
      ws = socket;

      socket.onopen = () => {
        opened = true;
        const reconnected = retry > 0;
        retry = 0;
        setStatus('on');
        pingTimer = setInterval(() => socket.readyState === WebSocket.OPEN && socket.send('{"type":"ping"}'), 25000);
        // Mất kết nối có thể đã lỡ sự kiện -> đồng bộ lại
        if (reconnected) { refreshNavCounts(); touch([current]); }
      };
      socket.onmessage = (e) => {
        let ev;
        try { ev = JSON.parse(e.data); } catch { return; }
        handleEvent(ev);
      };
      socket.onclose = async (e) => {
        clearInterval(pingTimer);
        if (ws === socket) ws = null;
        if (!running) return;
        if (e.code === 4403) return showLogin('Tài khoản này không còn quyền admin');
        setStatus(opened && e.code === 4401 ? 'connecting' : 'off');
        // Bị từ chối lúc bắt tay (thường là access token hết hạn) hoặc server đóng vì token hết hạn
        let delay = Math.min(30000, 1000 * 2 ** retry++);
        if (!opened || e.code === 4401) {
          const r = await renew();
          if (r === 'expired') return showLogin('Phiên đăng nhập đã hết hạn');
          if (r === 'ok' && e.code === 4401) delay = 0;
        }
        if (running) retryTimer = setTimeout(connect, delay);
      };
    }

    return {
      start() {
        if (running) return;
        running = true;
        retry = 0;
        renderFeed();
        connect();
      },
      stop() {
        running = false;
        clearTimeout(retryTimer);
        clearInterval(pingTimer);
        ws?.close();
        ws = null;
      },
    };
  })();

  // ============================================================
  checkSession();
})();
