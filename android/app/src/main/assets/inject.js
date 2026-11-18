(function () {
  if (window.__xliteInjected) return;
  window.__xliteInjected = true;
  var css = document.createElement('style');
  css.textContent =
    '.xl-btn{display:inline-flex;align-items:center;gap:6px;margin-inline-start:6px;padding:6px 12px;border:0;border-radius:999px;' +
    'background:linear-gradient(135deg,#7C5CFF,#1D9BF0);color:#fff;font:600 12px system-ui;box-shadow:0 4px 14px rgba(124,92,255,.35);' +
    'cursor:pointer;transition:transform .15s,opacity .2s;-webkit-tap-highlight-color:transparent}' +
    '.xl-btn:active{transform:scale(.92)}' +
    '.xl-btn.xl-done{background:#22C55E;box-shadow:none}' +
    '.xl-btn.xl-err{background:#F43F5E;box-shadow:none}';
  document.head.appendChild(css);

  function tweetUrl(article) {
    var t = article.querySelector('a[href*="/status/"] time');
    var a = t ? t.closest('a') : article.querySelector('a[href*="/status/"]');
    if (!a) return null;
    var m = a.href.match(/https?:\/\/[^\/]+\/[^\/]+\/status\/\d+/);
    return m ? m[0].replace('://mobile.', '://') : null;
  }

  function addButtons() {
    var arts = document.querySelectorAll('article');
    for (var i = 0; i < arts.length; i++) {
      var art = arts[i];
      if (art.querySelector('.xl-btn')) continue;
      var group = art.querySelector('[role="group"]');
      var url = tweetUrl(art);
      if (!group || !url) continue;
      var hasVideo = !!art.querySelector('video,[data-testid="videoPlayer"],[data-testid="videoComponent"]');
      var b = document.createElement('button');
      b.className = 'xl-btn';
      b.textContent = '⬇ کانال';
      if (!hasVideo) b.style.opacity = '.55';
      if (window.XLite && XLite.isSent(url)) { b.classList.add('xl-done'); b.textContent = '✓ ارسال شد'; }
      (function (btn, u) {
        btn.addEventListener('click', function (e) {
          e.preventDefault(); e.stopPropagation();
          var ok = window.XLite && XLite.send(u);
          btn.classList.add(ok ? 'xl-done' : 'xl-err');
          btn.textContent = ok ? '✓ تو صف' : '✕ خطا';
        }, true);
      })(b, url);
      group.appendChild(b);
    }
  }
  addButtons();
  new MutationObserver(function () { addButtons(); })
    .observe(document.body, { childList: true, subtree: true });
})();
