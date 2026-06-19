// 共通JavaScript: 既存画面に軽い操作補助を追加
window.addEventListener('DOMContentLoaded', function () {
  document.body.classList.add('page-loaded');

  // 削除・制限などの操作だけ確認を出す
  document.querySelectorAll('form').forEach(function (form) {
    var action = (form.getAttribute('action') || '').toLowerCase();
    var text = form.textContent || '';
    var needsConfirm = action.includes('delete') || action.includes('restrict') || action.includes('reset-password') || text.includes('削除') || text.includes('利用制限') || text.includes('リセット');
    if (needsConfirm) {
      form.addEventListener('submit', function (event) {
        if (!confirm('この操作を実行してよろしいですか？')) {
          event.preventDefault();
        }
      });
    }
  });

  // 入力欄の先頭に自動フォーカス
  var firstInput = document.querySelector('input:not([type="hidden"]):not([type="submit"]):not([type="button"]), textarea, select');
  if (firstInput && !firstInput.value) {
    firstInput.focus();
  }

  // ページ上部へ戻るボタン
  var topButton = document.createElement('button');
  topButton.type = 'button';
  topButton.textContent = '↑ 上へ';
  topButton.className = 'back-to-top';
  document.body.appendChild(topButton);

  topButton.addEventListener('click', function () {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

  window.addEventListener('scroll', function () {
    if (window.scrollY > 250) {
      topButton.classList.add('show');
    } else {
      topButton.classList.remove('show');
    }
  });
});
