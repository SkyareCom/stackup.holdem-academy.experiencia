// StackUp Hold'em Academy — service worker
const CACHE = "academy-v2.1.7-remote-refresh-r3-20261003";
const CORE = ["./", "index.html", "manifest.webmanifest", "privacy.html", "auth-production.js", "billing-production.js"];

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE)
      .then(cache => cache.addAll(CORE))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(key => key !== CACHE).map(key => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", event => {
  const request = event.request;
  if (request.method !== "GET") return;

  const url = new URL(request.url);
  if (url.origin !== location.origin) return;

  // Remote-first: when online, every deploy is visible immediately.
  // Cached files remain only as an offline fallback.
  event.respondWith(
    fetch(request)
      .then(response => {
        if (response && response.ok) {
          const copy = response.clone();
          caches.open(CACHE).then(cache => cache.put(request, copy));
        }
        return response;
      })
      .catch(() =>
        caches.match(request, { ignoreSearch: true })
          .then(response => response || (request.mode === "navigate" ? caches.match("index.html") : undefined))
      )
  );
});
