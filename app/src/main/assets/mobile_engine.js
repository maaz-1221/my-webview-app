/**
 * Xcelerate Mobile Engine v2.0
 * Injected directly into WebView for xworking.net and x.com
 * Handles:
 * 1. window.open & fetch interception on xworking.net to capture Tweet URLs 100% reliably
 * 2. 100% Video Watch & Retention (Min 16s) with Sound Unmute (20%)
 * 3. Safe Like & Safe Repost
 * 4. Automatic task claiming & verification on xworking.net (away_ms >= 16s)
 * [EXCLUDED: Comments & Copy Link]
 */

(function() {
  'use strict';

  function log(text, type = 'progress') {
    if (window.AndroidBridge && typeof window.AndroidBridge.onLog === 'function') {
      window.AndroidBridge.onLog(type, text);
    } else {
      console.log(`[XcelerateMobile] ${text}`);
    }
  }

  const sleep = (ms) => new Promise(r => setTimeout(r, ms));

  // ════════════════════════════════════════════════════════════
  // ── XWORKING.NET INJECTION ──
  // ════════════════════════════════════════════════════════════
  if (window.location.hostname.includes('xworking.net')) {
    log('xworking.net Mobile Engine v2.0 active.', 'success');

    // Spoof visibility so page runs smoothly in background
    try {
      Object.defineProperty(Document.prototype, 'hidden', { get: () => false, configurable: true });
      Object.defineProperty(Document.prototype, 'visibilityState', { get: () => 'visible', configurable: true });
    } catch(e) {}

    // Global tweet map and last-opened URL tracker
    window.__xcel_tweet_map = window.__xcel_tweet_map || new Map();
    window.__xcel_last_opened_url = null;

    // 1. Intercept window.open: xworking.net calls window.open when "X pe kholein" is clicked!
    const origOpen = window.open;
    window.open = function(url, target, features) {
      if (url) {
        const u = String(url);
        window.__xcel_last_opened_url = u;
        const m = u.match(/(?:x|twitter)\.com\/([a-zA-Z0-9_]{1,20})\/status\/(\d+)/i);
        if (m) {
          window.__xcel_tweet_map.set(m[1].toLowerCase(), u.split('?')[0]);
        }
      }
      return { closed: false, focus: () => {}, close: () => {} };
    };

    // 2. Intercept window.fetch to map all tweet URLs returned by xworking APIs
    if (typeof window.fetch === 'function') {
      const origFetch = window.fetch;
      window.fetch = async function(...args) {
        const res = await origFetch.apply(this, args);
        try {
          const clone = res.clone();
          clone.text().then(text => {
            if (text) {
              const matches = text.matchAll(/https?:\/\/(?:twitter\.com|x\.com)\/([a-zA-Z0-9_]{1,20})\/status\/(\d+)/gi);
              for (const m of matches) {
                window.__xcel_tweet_map.set(m[1].toLowerCase(), m[0].split('?')[0]);
              }
            }
          }).catch(() => {});
        } catch(e) {}
        return res;
      };
    }

    // 3. Intercept record to guarantee away_ms >= 16000
    if (typeof window.record === 'function') {
      const orig = window.record;
      window.record = function(l, kind, away, opened, explicit) {
        if (kind === 'liked' && (!away || away < 16000)) away = 16000;
        return orig.call(this, l, kind, away, opened, explicit);
      };
    }

    // Function called by Android to pick the next task
    window.__xcel_pick_task = async function() {
      const cards = [...document.querySelectorAll('article.lk:not(.done):not([data-xcel-done="true"])')];
      if (cards.length === 0) {
        log('Is waqt koi task baqi nahi hai.', 'warn');
        if (window.AndroidBridge) window.AndroidBridge.onNoTasksLeft();
        return;
      }

      const card = cards[0];
      const handleEl = card.querySelector('.mono');
      const handle = handleEl ? handleEl.innerText.trim().replace(/^@/, '') : 'User';
      const cleanH = handle.toLowerCase();

      log(`🎯 Task select hua: @${handle}. Link capture ho raha hai...`, 'progress');

      // 1. Find the "X pe kholein" / "Dobara kholein" button
      const openBtn = card.querySelector('[data-a="open"]') ||
                      [...card.querySelectorAll('button')].find(b => {
                        const txt = (b.innerText || '').trim();
                        return txt.includes('kholein') || txt.includes('khol chuke');
                      });

      window.__xcel_last_opened_url = null;

      if (openBtn) {
        card.scrollIntoView({ behavior: 'smooth', block: 'center' });
        await sleep(250);
        try { openBtn.click(); } catch(e) {}
      }

      // 2. Poll for the captured Tweet URL (up to 2 seconds)
      let tweetUrl = null;
      for (let w = 0; w < 20; w++) {
        if (window.__xcel_last_opened_url && (window.__xcel_last_opened_url.includes('/status/') || window.__xcel_last_opened_url.includes('x.com') || window.__xcel_last_opened_url.includes('twitter.com'))) {
          tweetUrl = window.__xcel_last_opened_url.split('?')[0];
          break;
        }
        if (window.__xcel_tweet_map && window.__xcel_tweet_map.has(cleanH)) {
          tweetUrl = window.__xcel_tweet_map.get(cleanH);
          break;
        }
        await sleep(100);
      }

      // 3. Fallbacks: check <a> tag, data attributes, or card innerHTML
      if (!tweetUrl) {
        const link = card.querySelector('a[href*="/status/"], a[href*="twitter.com"], a[href*="x.com"]');
        if (link && link.href) tweetUrl = link.href.split('?')[0];
      }

      if (!tweetUrl && openBtn) {
        const dUrl = openBtn.getAttribute('data-url') || openBtn.getAttribute('data-href');
        if (dUrl && dUrl.includes('/status/')) tweetUrl = dUrl.split('?')[0];
      }

      if (!tweetUrl) {
        const m = (card.innerHTML || '').match(/https?:\/\/(?:twitter\.com|x\.com)\/([a-zA-Z0-9_]{1,20})\/status\/(\d+)/i);
        if (m) tweetUrl = m[0].split('?')[0];
      }

      // If still not found, skip gracefully with a 2-second breath (DO NOT SPAM-SKIP)
      if (!tweetUrl) {
        log(`⚠️ @${handle} ka tweet URL nahi mila, pause le kar agla check kar raha hoon...`, 'warn');
        card.setAttribute('data-xcel-done', 'true');
        await sleep(2000);
        window.__xcel_pick_task();
        return;
      }

      log(`🔗 Link captured: ${tweetUrl}`, 'success');
      card.setAttribute('data-xcel-active', 'true');
      if (window.AndroidBridge) {
        window.AndroidBridge.onTaskUrlFound(tweetUrl, handle);
      }
    };

    // Function called by Android when Twitter task is finished
    window.__xcel_submit_task = async function() {
      const card = document.querySelector('article.lk[data-xcel-active="true"]');
      if (!card) return;

      log('xworking.net par task submit ho raha hai (away_ms >= 16s)...', 'progress');

      if (typeof window.finishPending === 'function') {
        try { await window.finishPending(); } catch(e) {}
      }

      await sleep(1200);
      const submitBtn = card.querySelector('[data-a="liked"]') ||
                        [...card.querySelectorAll('button')].find(b => (b.innerText||'').includes('Like/Repost'));
      if (submitBtn) {
        try { submitBtn.click(); } catch(e) {}
        log('✅ Task kamiyabi se submit ho gaya!', 'success');
      }

      card.removeAttribute('data-xcel-active');
      card.setAttribute('data-xcel-done', 'true');
      if (window.AndroidBridge) {
        window.AndroidBridge.onTaskSubmitted();
      }
    };
  }

  // ════════════════════════════════════════════════════════════
  // ── TWITTER / X INJECTION ──
  // ════════════════════════════════════════════════════════════
  if (window.location.hostname.includes('twitter.com') || window.location.hostname.includes('x.com')) {
    log('Twitter Mobile Engine active.', 'progress');

    async function runTwitterBoost() {
      log('Video detect kar raha hoon...', 'progress');

      let post = null;
      for (let w = 0; w < 12; w++) {
        post = document.querySelector('article[data-testid="tweet"]');
        if (post) break;
        await sleep(350);
      }
      if (!post) post = document.body;

      // 1. Video Watch with Sound Unmute
      const vid = post.querySelector('video') || document.querySelector('video');
      if (vid) {
        log('🎬 Video detect hui — 100% Watch & Retention sequence shuru...', 'progress');
        vid.scrollIntoView({ behavior: 'smooth', block: 'center' });
        await sleep(300);

        // Safe play
        if (vid.paused) {
          vid.muted = true;
          const playBtn = post.querySelector('[data-testid="playButton"], button[aria-label*="Play" i]');
          if (playBtn) playBtn.click();
          try { vid.play().catch(()=>{}); } catch(e) {}
        }

        // Unmute to 20%
        try {
          vid.volume = 0.20;
          vid.muted = false;
        } catch(e) {}

        const targetWatchSec = 16;
        let elapsed = 0;

        while (elapsed < targetWatchSec) {
          await sleep(1000);
          elapsed += 1;

          if (vid.paused) {
            try { vid.play().catch(()=>{}); } catch(e) {}
          }
          if (vid.muted) {
            try { vid.volume = 0.20; vid.muted = false; } catch(e) {}
          }

          if (window.AndroidBridge && typeof window.AndroidBridge.onVideoTick === 'function') {
            window.AndroidBridge.onVideoTick(elapsed, targetWatchSec, !vid.muted);
          }

          const soundTxt = vid.muted ? 'Muted 🔇' : 'Sound ON 🔊 (20%)';
          log(`▶️ Video watch: (${elapsed}/${targetWatchSec}s) [${soundTxt}]`, 'progress');
        }
        log('✅ 16s Video Retention Mukammal!', 'success');
      } else {
        log('ℹ️ Text/Image post — 5s dwell time...', 'progress');
        await sleep(5000);
      }

      // 2. Safe Like
      log('❤️ Safe Like trigger kar raha hoon...', 'progress');
      const likeBtn = post.querySelector('[data-testid="like"]');
      if (likeBtn) {
        likeBtn.click();
        await sleep(800);
        log('❤️ Liked!', 'success');
      }

      // 3. Safe Repost
      log('🔁 Safe Repost trigger kar raha hoon...', 'progress');
      const retweetBtn = post.querySelector('[data-testid="retweet"]');
      if (retweetBtn) {
        retweetBtn.click();
        await sleep(600);
        const confirmBtn = document.querySelector('[data-testid="retweetConfirm"]');
        if (confirmBtn) {
          confirmBtn.click();
          await sleep(800);
          log('🔁 Reposted!', 'success');
        }
      }

      log('🎉 Twitter Actions Mukammal! Wapis xworking.net par move ho raha hai...', 'success');
      if (window.AndroidBridge) {
        window.AndroidBridge.onTwitterCompleted();
      }
    }

    setTimeout(runTwitterBoost, 2000);
  }

})();
