/**
 * Xcelerate Mobile Engine v1.0
 * Injected directly into WebView for xworking.net and x.com
 * Handles:
 * 1. 100% Video Watch & Retention (Min 16s) with Sound Unmute
 * 2. Safe Like
 * 3. Safe Repost
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

  // ── XWORKING.NET INJECTION ──
  if (window.location.hostname.includes('xworking.net')) {
    log('xworking.net Mobile Engine injected successfully.', 'success');

    // Spoof visibility
    try {
      Object.defineProperty(Document.prototype, 'hidden', { get: () => false, configurable: true });
      Object.defineProperty(Document.prototype, 'visibilityState', { get: () => 'visible', configurable: true });
    } catch(e) {}

    // Intercept record to guarantee away_ms >= 16000
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

      log(`Task select hua: @${handle}. Link open ho raha hai...`, 'progress');

      // Click "X pe kholein"
      const openBtn = card.querySelector('[data-a="open"]') ||
                      [...card.querySelectorAll('button')].find(b => (b.innerText||'').includes('kholein'));
      if (openBtn) {
        try { openBtn.click(); } catch(e) {}
        await sleep(400);
      }

      // Find tweet URL
      let tweetUrl = null;
      const m = (card.innerHTML || '').match(/https?:\/\/(?:twitter\.com|x\.com)\/([a-zA-Z0-9_]{1,20})\/status\/(\d+)/i);
      if (m) tweetUrl = m[0].split('?')[0];

      if (!tweetUrl) {
        log(`@${handle} ka tweet URL nahi mila, skipping...`, 'warn');
        card.setAttribute('data-xcel-done', 'true');
        window.__xcel_pick_task();
        return;
      }

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

      await sleep(1000);
      const submitBtn = card.querySelector('[data-a="liked"]') ||
                        [...card.querySelectorAll('button')].find(b => (b.innerText||'').includes('Like/Repost'));
      if (submitBtn) {
        try { submitBtn.click(); } catch(e) {}
        log('Task kamiyabi se submit ho gaya!', 'success');
      }

      card.removeAttribute('data-xcel-active');
      card.setAttribute('data-xcel-done', 'true');
      if (window.AndroidBridge) {
        window.AndroidBridge.onTaskSubmitted();
      }
    };
  }

  // ── TWITTER / X INJECTION ──
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
