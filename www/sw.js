const K='bgm-cutter-v1',F=['./','index.html','manifest.webmanifest','icon-192.png','icon-512.png'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(K).then(c=>c.addAll(F)).then(()=>self.skipWaiting()))});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(k=>Promise.all(k.filter(x=>x!==K).map(x=>caches.delete(x)))).then(()=>self.clients.claim()))});
self.addEventListener('fetch',e=>{if(e.request.method!=='GET')return;const same=new URL(e.request.url).origin===location.origin;
 const keep=r=>{if(r.ok||r.type==='opaque'){const cp=r.clone();caches.open(K).then(c=>c.put(e.request,cp))}return r};
 e.respondWith(same?fetch(e.request).then(keep).catch(()=>caches.match(e.request)):caches.match(e.request).then(r=>r||fetch(e.request).then(keep)))});
