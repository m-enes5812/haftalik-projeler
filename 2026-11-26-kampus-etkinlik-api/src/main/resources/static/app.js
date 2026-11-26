// Kampüs Etkinlik web arayüzü: aynı sunucudaki REST API'yi kullanan küçük bir tek sayfa uygulaması.
// Kullanıcı verisi DOM'a her zaman textContent ile yazılır (innerHTML yok), böylece XSS'e kapı açılmaz.
"use strict";

const TOKEN_KEY = "kampus.token";
const app = document.getElementById("app");
let me = null; // { id, email, fullName, role }

const ROLE_TR = { STUDENT: "Öğrenci", CLUB: "Kulüp", ADMIN: "Yönetici" };
const STATUS_TR = {
  DRAFT: "Taslak", PUBLISHED: "Yayında", CANCELLED: "İptal",
  ACTIVE: "Geçerli", USED: "Kullanıldı",
};

// ---------- yardımcılar ----------

function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v == null || v === false) continue;
    if (k.startsWith("on")) el.addEventListener(k.slice(2), v);
    else if (k === "class") el.className = v;
    else el.setAttribute(k, v === true ? "" : v);
  }
  for (const c of children.flat()) {
    if (c == null || c === false) continue;
    el.append(c instanceof Node ? c : document.createTextNode(String(c)));
  }
  return el;
}

function render(...nodes) {
  app.replaceChildren(...nodes);
}

function toast(msg) {
  const t = document.getElementById("toast");
  t.textContent = msg;
  t.classList.add("show");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => t.classList.remove("show"), 3200);
}

const fmt = new Intl.DateTimeFormat("tr-TR", { dateStyle: "medium", timeStyle: "short" });
const when = (iso) => (iso ? fmt.format(new Date(iso)) : "-");

function badge(status) {
  return h("span", { class: "badge " + status }, STATUS_TR[status] || status);
}

function token() {
  try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; }
}
function setToken(t) {
  try { t ? sessionStorage.setItem(TOKEN_KEY, t) : sessionStorage.removeItem(TOKEN_KEY); } catch { /* yok say */ }
}

class ApiError extends Error {
  constructor(status, problem) {
    const fields = problem && problem.errors ? Object.values(problem.errors).join(", ") : "";
    super(fields || (problem && problem.detail) || "İstek başarısız (" + status + ")");
    this.status = status;
  }
}

async function api(method, path, body, raw = false) {
  const headers = {};
  const t = token();
  if (t) headers.Authorization = "Bearer " + t;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const res = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  if (res.status === 401 && t) {
    logout("Oturum süresi doldu, tekrar giriş yap.");
    throw new ApiError(401, null);
  }
  if (!res.ok) {
    let problem = null;
    try { problem = await res.json(); } catch { /* gövde yok */ }
    throw new ApiError(res.status, problem);
  }
  if (raw) return res;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

// Bir butona basılınca işlemi çalıştırır; o sırada butonu kilitler, hatayı gösterir.
function action(fn) {
  return async (ev) => {
    const btn = ev && ev.currentTarget;
    if (btn) btn.disabled = true;
    try { await fn(ev); }
    catch (e) { if (e.status !== 401) toast(e.message); }
    finally { if (btn && btn.isConnected) btn.disabled = false; }
  };
}

function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}

// ---------- oturum ----------

async function loadMe() {
  me = token() ? await api("GET", "/api/me").catch(() => null) : null;
  if (!me) setToken(null);
}

function logout(msg) {
  setToken(null);
  me = null;
  if (msg) toast(msg);
  location.hash = "#/giris";
  route();
}

function drawHeader() {
  const links = [["#/etkinlikler", "Etkinlikler"]];
  if (me && me.role === "STUDENT") links.push(["#/biletlerim", "Biletlerim"]);
  if (me && (me.role === "CLUB" || me.role === "ADMIN")) {
    links.push(["#/etkinliklerim", "Etkinliklerim"], ["#/kapi", "Kapı Girişi"]);
  }
  if (me && me.role === "ADMIN") links.push(["#/kullanicilar", "Kullanıcılar"]);
  const cur = location.hash.split("?")[0];
  document.getElementById("nav").replaceChildren(
    ...links.map(([href, text]) => h("a", { href, class: cur === href ? "active" : null }, text)),
  );
  document.getElementById("who").replaceChildren(
    ...(me
      ? [h("span", {}, me.fullName + " · " + ROLE_TR[me.role]), h("button", { onclick: () => logout("Çıkış yapıldı.") }, "Çıkış")]
      : [h("a", { href: "#/giris" }, "Giriş yap")]),
  );
}

// ---------- ekranlar ----------

function loginView() {
  let mode = "login";
  const err = h("div", { class: "err" });
  const box = h("div", { class: "card narrow" });

  function draw() {
    const isLogin = mode === "login";
    const form = h("form", { onsubmit: submit },
      !isLogin && h("label", {}, "Ad Soyad", h("input", { name: "fullName", required: true, maxlength: 120, autocomplete: "name" })),
      h("label", {}, "E-posta", h("input", { name: "email", type: "email", required: true, autocomplete: "email" })),
      h("label", {}, "Şifre" + (isLogin ? "" : " (en az 8 karakter)"),
        h("input", { name: "password", type: "password", required: true, minlength: isLogin ? null : 8, maxlength: 72,
          autocomplete: isLogin ? "current-password" : "new-password" })),
      err,
      h("button", { class: "primary", type: "submit" }, isLogin ? "Giriş yap" : "Kayıt ol"),
    );
    box.replaceChildren(
      h("div", { class: "tabs" },
        h("button", { type: "button", class: isLogin ? "on" : null, onclick: () => { mode = "login"; draw(); } }, "Giriş"),
        h("button", { type: "button", class: isLogin ? null : "on", onclick: () => { mode = "register"; draw(); } }, "Kayıt ol")),
      form,
      !isLogin && h("p", { class: "meta" }, "Yeni hesaplar öğrenci olarak açılır. Kulüp yetkisini yönetici verir."),
    );
    err.textContent = "";
  }

  async function submit(ev) {
    ev.preventDefault();
    const btn = ev.target.querySelector("button[type=submit]");
    btn.disabled = true;
    err.textContent = "";
    const data = formData(ev.target);
    try {
      if (mode === "register") await api("POST", "/api/auth/register", data);
      const tok = await api("POST", "/api/auth/login", { email: data.email, password: data.password });
      setToken(tok.accessToken);
      await loadMe();
      toast("Hoş geldin, " + me.fullName + "!");
      location.hash = "#/etkinlikler";
    } catch (e) {
      err.textContent = e.status === 401 ? "E-posta veya şifre hatalı." : e.message;
      btn.disabled = false;
    }
  }

  draw();
  render(h("h1", { class: "narrow" }, "Hoş geldin"), box);
}

function capacityBar(e) {
  const taken = e.capacity - e.remainingSeats;
  const pct = e.capacity ? Math.round((taken / e.capacity) * 100) : 0;
  return [
    h("div", { class: "bar" }, h("div", { style: "width:" + pct + "%" })),
    h("div", { class: "meta" }, e.remainingSeats + " / " + e.capacity + " yer boş"),
  ];
}

async function eventsView() {
  render(h("h1", {}, "Yaklaşan Etkinlikler"), h("p", { class: "meta" }, "Yükleniyor…"));
  const page = await api("GET", "/api/events?size=50");
  const mineByEvent = new Map();
  if (me && me.role === "STUDENT") {
    for (const t of await api("GET", "/api/me/tickets")) {
      if (t.status !== "CANCELLED") mineByEvent.set(t.eventId, t);
    }
  }
  const cards = page.content.map((e) => {
    let btn;
    if (!me) btn = h("a", { href: "#/giris" }, "Bilet almak için giriş yap");
    else if (me.role !== "STUDENT") btn = h("span", { class: "meta" }, "Bilet sadece öğrenciler içindir");
    else if (mineByEvent.has(e.id)) btn = h("a", { href: "#/biletlerim" }, "✓ Biletin var, göster");
    else if (e.remainingSeats <= 0) btn = h("button", { disabled: true }, "Kontenjan dolu");
    else {
      btn = h("button", {
        class: "primary",
        onclick: action(async () => {
          await api("POST", "/api/events/" + e.id + "/tickets");
          toast("Biletin hazır! QR kodu Biletlerim sayfasında.");
          location.hash = "#/biletlerim";
        }),
      }, "Bilet Al");
    }
    return h("div", { class: "card" },
      h("h2", {}, e.title),
      h("div", { class: "meta" }, "📅 " + when(e.startsAt) + " · 📍 " + e.location),
      h("div", { class: "meta" }, "Düzenleyen: " + e.organizerName),
      e.description && h("div", { class: "desc" }, e.description),
      ...capacityBar(e),
      h("div", { class: "row" }, btn),
    );
  });
  render(
    h("h1", {}, "Yaklaşan Etkinlikler"),
    cards.length ? h("div", { class: "grid" }, cards) : h("div", { class: "empty" }, "Şu an yayında etkinlik yok."),
  );
}

async function ticketsView() {
  const tickets = await api("GET", "/api/me/tickets");
  const urls = [];
  const cards = tickets.map((t) => {
    const img = h("img", { class: "qr", alt: "Bilet QR kodu" });
    if (t.status === "ACTIVE") {
      // QR görseli yetki ister; fetch ile alıp nesne URL'si olarak gösteriyoruz.
      api("GET", "/api/tickets/" + t.id + "/qr", undefined, true)
        .then((r) => r.blob())
        .then((b) => { const u = URL.createObjectURL(b); urls.push(u); img.src = u; })
        .catch(() => { img.alt = "QR yüklenemedi"; });
    }
    return h("div", { class: "card" },
      h("div", { class: "row" }, h("h2", {}, t.eventTitle), h("span", { class: "spacer" }), badge(t.status)),
      h("div", { class: "meta" }, "📅 " + when(t.eventStartsAt)),
      t.status === "ACTIVE" && img,
      t.status === "ACTIVE" && h("div", { class: "meta" }, "Girişte bu kodu görevliye okut."),
      t.status === "USED" && h("div", { class: "meta" }, "Giriş yapıldı: " + when(t.checkedInAt)),
      h("div", { class: "code" }, "Kod: " + t.code),
      t.status === "ACTIVE" && h("div", { class: "row" },
        h("button", {
          class: "danger",
          onclick: action(async () => {
            if (!confirm("Bu bileti iptal etmek istiyor musun?")) return;
            await api("DELETE", "/api/tickets/" + t.id);
            toast("Bilet iptal edildi.");
            route();
          }),
        }, "Bileti iptal et")),
    );
  });
  render(
    h("h1", {}, "Biletlerim"),
    cards.length
      ? h("div", { class: "grid" }, cards)
      : h("div", { class: "empty" }, "Henüz biletin yok. ", h("a", { href: "#/etkinlikler" }, "Etkinliklere göz at")),
  );
  return () => urls.forEach(URL.revokeObjectURL);
}

// datetime-local değeri (yerel saat) ↔ ISO-8601
const toIso = (local) => new Date(local).toISOString();
function toLocal(iso) {
  const d = new Date(iso);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

function eventForm(initial, onSave, onCancel) {
  const err = h("div", { class: "err" });
  const v = initial || {};
  const form = h("form", {
    onsubmit: async (ev) => {
      ev.preventDefault();
      err.textContent = "";
      const d = formData(form);
      const btn = form.querySelector("button[type=submit]");
      btn.disabled = true;
      try {
        await onSave({
          title: d.title, description: d.description || null, location: d.location,
          startsAt: toIso(d.startsAt), endsAt: toIso(d.endsAt), capacity: Number(d.capacity),
        });
      } catch (e) { err.textContent = e.message; btn.disabled = false; }
    },
  },
    h("label", {}, "Başlık", h("input", { name: "title", required: true, maxlength: 160, value: v.title })),
    h("label", {}, "Açıklama", h("textarea", { name: "description", maxlength: 4000 }, v.description || "")),
    h("label", {}, "Yer", h("input", { name: "location", required: true, maxlength: 200, value: v.location })),
    h("div", { class: "row" },
      h("label", {}, "Başlangıç", h("input", { name: "startsAt", type: "datetime-local", required: true, value: v.startsAt && toLocal(v.startsAt) })),
      h("label", {}, "Bitiş", h("input", { name: "endsAt", type: "datetime-local", required: true, value: v.endsAt && toLocal(v.endsAt) })),
      h("label", {}, "Kontenjan", h("input", { name: "capacity", type: "number", min: 1, max: 100000, required: true, value: v.capacity || 50 }))),
    err,
    h("div", { class: "row" },
      h("button", { class: "primary", type: "submit" }, initial ? "Kaydet" : "Taslak olarak oluştur"),
      h("button", { type: "button", onclick: onCancel }, "Vazgeç")),
  );
  return h("div", { class: "card" }, h("h2", {}, initial ? "Etkinliği düzenle" : "Yeni etkinlik"), form);
}

async function myEventsView() {
  const list = await api("GET", "/api/me/events");
  const formSlot = h("div");
  const openForm = (initial) => {
    formSlot.replaceChildren(eventForm(initial, async (body) => {
      if (initial) await api("PUT", "/api/events/" + initial.id, body);
      else await api("POST", "/api/events", body);
      toast(initial ? "Etkinlik güncellendi." : "Taslak oluşturuldu. Hazır olunca 'Yayınla'ya bas.");
      route();
    }, () => formSlot.replaceChildren()));
    formSlot.scrollIntoView({ behavior: "smooth" });
  };

  const cards = list.map((e) => {
    const statsBox = h("div");
    const buttons = [];
    if (e.status === "DRAFT") {
      buttons.push(
        h("button", { class: "primary", onclick: action(async () => {
          await api("POST", "/api/events/" + e.id + "/publish");
          toast("Etkinlik yayında!");
          route();
        }) }, "Yayınla"),
        h("button", { onclick: () => openForm(e) }, "Düzenle"));
    }
    if (e.status !== "CANCELLED") {
      buttons.push(h("button", { onclick: action(async () => {
        const s = await api("GET", "/api/events/" + e.id + "/stats");
        statsBox.replaceChildren(h("div", { class: "stats" },
          ...[["Kontenjan", s.capacity], ["Kayıtlı", s.registered], ["Giriş yapan", s.checkedIn], ["İptal", s.cancelled]]
            .map(([k, n]) => h("div", {}, h("b", {}, n), h("span", { class: "meta" }, k)))));
      }) }, "İstatistik"));
    }
    if (e.status === "PUBLISHED") {
      buttons.push(h("a", { class: "btn", href: "#/kapi?etkinlik=" + e.id }, "Kapı girişi"));
    }
    if (e.status !== "CANCELLED") {
      buttons.push(h("button", { class: "danger", onclick: action(async () => {
        if (!confirm("'" + e.title + "' iptal edilsin mi? Bu geri alınamaz.")) return;
        await api("POST", "/api/events/" + e.id + "/cancel");
        toast("Etkinlik iptal edildi.");
        route();
      }) }, "İptal et"));
    }
    return h("div", { class: "card" },
      h("div", { class: "row" }, h("h2", {}, e.title), h("span", { class: "spacer" }), badge(e.status)),
      h("div", { class: "meta" }, "📅 " + when(e.startsAt) + " · 📍 " + e.location),
      ...capacityBar(e),
      statsBox,
      h("div", { class: "row" }, buttons),
    );
  });

  render(
    h("div", { class: "row" }, h("h1", {}, "Etkinliklerim"), h("span", { class: "spacer" }),
      h("button", { class: "primary", onclick: () => openForm(null) }, "+ Yeni etkinlik")),
    formSlot,
    h("p"),
    cards.length ? h("div", { class: "grid" }, cards) : h("div", { class: "empty" }, "Henüz etkinlik oluşturmadın."),
  );
}

async function gateView(params) {
  const list = (await api("GET", "/api/me/events")).filter((e) => e.status === "PUBLISHED");
  if (!list.length) {
    render(h("h1", {}, "Kapı Girişi"), h("div", { class: "empty" }, "Yayında etkinliğin yok. ", h("a", { href: "#/etkinliklerim" }, "Etkinliklerim")));
    return;
  }
  const select = h("select", {}, list.map((e) => h("option", { value: e.id, selected: String(e.id) === params.get("etkinlik") }, e.title + " · " + when(e.startsAt))));
  const result = h("div");
  const input = h("input", { name: "code", placeholder: "Bilet kodu (okuyucu veya elle)", required: true, autocomplete: "off" });
  let busy = false;
  let lastCode = "";

  async function checkIn(code) {
    code = code.trim();
    if (!code || busy) return;
    busy = true;
    try {
      const r = await api("POST", "/api/events/" + select.value + "/check-in", { code });
      result.replaceChildren(h("div", { class: "result ok" }, "✅ Giriş onaylandı: " + r.attendeeName));
    } catch (e) {
      if (e.status !== 401) result.replaceChildren(h("div", { class: "result bad" }, "⛔ " + e.message));
    } finally {
      busy = false;
      input.value = "";
      input.focus();
    }
  }

  const form = h("form", { onsubmit: (ev) => { ev.preventDefault(); checkIn(input.value); } },
    h("label", {}, "Etkinlik", select),
    h("div", { class: "row" }, input, h("button", { class: "primary", type: "submit" }, "Onayla")));

  // Tarayıcı destekliyorsa kamerayla QR okut.
  let stream = null;
  let timer = null;
  const camBox = h("div");
  if ("BarcodeDetector" in window && navigator.mediaDevices) {
    const video = h("video", { playsinline: true, muted: true });
    const camBtn = h("button", {
      type: "button",
      onclick: async () => {
        if (stream) { stop(); camBtn.textContent = "📷 Kamerayı aç"; return; }
        try {
          stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
        } catch { toast("Kameraya erişilemedi."); return; }
        video.srcObject = stream;
        await video.play();
        camBtn.textContent = "Kamerayı kapat";
        const detector = new BarcodeDetector({ formats: ["qr_code"] });
        timer = setInterval(async () => {
          try {
            const [hit] = await detector.detect(video);
            if (hit && hit.rawValue !== lastCode) {
              lastCode = hit.rawValue;
              setTimeout(() => { lastCode = ""; }, 3000);
              checkIn(hit.rawValue);
            }
          } catch { /* kare hazır değil */ }
        }, 400);
      },
    }, "📷 Kamerayı aç");
    camBox.append(h("div", { class: "row" }, camBtn), video);
  } else {
    camBox.append(h("p", { class: "meta" }, "Bu tarayıcı kamerayla QR okumayı desteklemiyor; USB okuyucu veya elle kod girişi kullan."));
  }
  function stop() {
    clearInterval(timer);
    if (stream) stream.getTracks().forEach((t) => t.stop());
    stream = null;
  }

  render(h("h1", {}, "Kapı Girişi"), h("div", { class: "card" }, form, result, camBox));
  input.focus();
  return stop;
}

async function usersView() {
  const page = await api("GET", "/api/admin/users?size=200");
  const rows = page.content.map((u) => {
    const sel = h("select", {
      disabled: u.id === me.id,
      onchange: action(async () => {
        await api("PATCH", "/api/admin/users/" + u.id + "/role", { role: sel.value });
        toast(u.fullName + " artık " + ROLE_TR[sel.value] + ".");
      }),
    }, Object.entries(ROLE_TR).map(([k, v]) => h("option", { value: k, selected: k === u.role }, v)));
    return h("tr", {}, h("td", {}, u.fullName), h("td", {}, u.email), h("td", {}, sel), h("td", { class: "meta" }, when(u.createdAt)));
  });
  render(
    h("h1", {}, "Kullanıcılar"),
    h("p", { class: "meta" }, "Bir öğrenciye 'Kulüp' rolü verirsen etkinlik oluşturabilir. Rol değişikliği o kişinin bir sonraki girişinde geçerli olur."),
    h("div", { class: "table-wrap" }, h("table", {},
      h("thead", {}, h("tr", {}, h("th", {}, "Ad"), h("th", {}, "E-posta"), h("th", {}, "Rol"), h("th", {}, "Kayıt"))),
      h("tbody", {}, rows))),
  );
}

// ---------- yönlendirme ----------

const ROUTES = {
  "#/giris": { view: loginView },
  "#/etkinlikler": { view: eventsView },
  "#/biletlerim": { view: ticketsView, roles: ["STUDENT"] },
  "#/etkinliklerim": { view: myEventsView, roles: ["CLUB", "ADMIN"] },
  "#/kapi": { view: gateView, roles: ["CLUB", "ADMIN"] },
  "#/kullanicilar": { view: usersView, roles: ["ADMIN"] },
};

let cleanup = null;

async function route() {
  if (cleanup) { cleanup(); cleanup = null; }
  const [path, query] = location.hash.split("?");
  let r = ROUTES[path];
  if (!r) { location.hash = "#/etkinlikler"; return; }
  if (r.roles && !me) { location.hash = "#/giris"; return; }
  if (r.roles && !r.roles.includes(me.role)) { location.hash = "#/etkinlikler"; return; }
  drawHeader();
  try {
    cleanup = (await r.view(new URLSearchParams(query || ""))) || null;
  } catch (e) {
    if (e.status !== 401) render(h("div", { class: "empty err" }, "Bir hata oluştu: " + e.message));
  }
}

window.addEventListener("hashchange", route);
loadMe().then(route);
