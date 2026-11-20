<?php
// Halaman 6: Create New Password
require_once __DIR__ . '/config.php';

// Hanya bisa diakses setelah kode diverifikasi (maks 15 menit)
if (empty($_SESSION['reset_email']) || (time() - ($_SESSION['reset_time'] ?? 0)) > 900) {
    unset($_SESSION['reset_email'], $_SESSION['reset_time']);
    redirect('forgot.php');
}

$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $password = $_POST['password'] ?? '';
    $confirm  = $_POST['confirm_password'] ?? '';

    if (!csrf_valid()) {
        $error = 'Sesi tidak valid. Silakan muat ulang halaman.';
    } elseif (strlen($password) < 8) {
        $error = 'Password minimal 8 karakter.';
    } elseif ($password !== $confirm) {
        $error = 'Konfirmasi password tidak sama.';
    } else {
        try {
            db()->prepare('UPDATE users SET password_hash = ? WHERE email = ?')
                ->execute([password_hash($password, PASSWORD_DEFAULT), $_SESSION['reset_email']]);
            db()->prepare('DELETE FROM password_resets WHERE email = ?')
                ->execute([$_SESSION['reset_email']]);

            unset($_SESSION['reset_email'], $_SESSION['reset_time']);
            redirect('signin.php?reset=1');
        } catch (PDOException $ex) {
            $error = 'Terjadi kesalahan pada server. Coba lagi nanti.';
        }
    }
}

$pageTitle = 'New Password';
$bodyClass = 'reset-page';
?>
<?php require __DIR__ . '/header.php'; ?>
<h1 class="page-title grad-text" style="margin-top:56px;">Create<br><span style="font-size:22px;">New Password</span></h1>

<?php if ($error): ?><div class="alert alert-error"><?= e($error) ?></div><?php endif; ?>

<form class="form" method="post" action="reset.php" novalidate>
  <?= csrf_field() ?>

  <div class="form-group">
    <label for="password">Password</label>
    <input class="field" type="password" id="password" name="password" autocomplete="new-password" required>
  </div>

  <div class="form-group">
    <label for="confirm_password">Confirm Password</label>
    <input class="field" type="password" id="confirm_password" name="confirm_password" autocomplete="new-password" required>
  </div>

  <button class="btn" type="submit" style="margin-top:12px;">Continue to Sign In</button>
</form>
<?php require __DIR__ . '/footer.php'; ?>
