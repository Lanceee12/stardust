<?php
// Halaman 5: Forgot / Reset Password (kirim kode + verifikasi kode)
require_once __DIR__ . '/config.php';

if (is_logged_in()) {
    redirect('home.php');
}

const RESET_CODE_TTL_MINUTES = 10;
const RESET_MAX_ATTEMPTS     = 5;

$error   = '';
$info    = '';
$devCode = '';
$email   = strtolower(trim($_POST['email'] ?? ''));

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';

    if (!csrf_valid()) {
        $error = 'Sesi tidak valid. Silakan muat ulang halaman.';
    } elseif (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        $error = 'Masukkan email yang valid.';
    } else {
        try {
            if ($action === 'send') {
                // Pesan sama baik email terdaftar atau tidak (hindari user enumeration)
                $stmt = db()->prepare('SELECT id FROM users WHERE email = ? LIMIT 1');
                $stmt->execute([$email]);

                if ($stmt->fetch()) {
                    $code = (string)random_int(100000, 999999);

                    db()->prepare('DELETE FROM password_resets WHERE email = ?')->execute([$email]);
                    db()->prepare(
                        'INSERT INTO password_resets (email, code_hash, expires_at)
                         VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ' . RESET_CODE_TTL_MINUTES . ' MINUTE))'
                    )->execute([$email, password_hash($code, PASSWORD_DEFAULT)]);

                    // TODO production: kirim email (PHPMailer / SMTP / layanan email)
                    // @mail($email, 'Kode reset password Stardust', "Kode kamu: $code (berlaku " . RESET_CODE_TTL_MINUTES . " menit)");

                    if (DEV_MODE) {
                        $devCode = $code;
                    }
                }
                $info = 'Jika email terdaftar, kode verifikasi sudah dikirim.';

            } elseif ($action === 'verify') {
                $code = trim($_POST['code'] ?? '');

                $stmt = db()->prepare(
                    'SELECT id, code_hash, attempts FROM password_resets
                     WHERE email = ? AND used = 0 AND expires_at > NOW()
                     ORDER BY id DESC LIMIT 1'
                );
                $stmt->execute([$email]);
                $row = $stmt->fetch();

                if (!$row || $row['attempts'] >= RESET_MAX_ATTEMPTS) {
                    $error = 'Kode tidak valid atau sudah kedaluwarsa. Kirim ulang kode.';
                } elseif (!password_verify($code, $row['code_hash'])) {
                    db()->prepare('UPDATE password_resets SET attempts = attempts + 1 WHERE id = ?')->execute([$row['id']]);
                    $error = 'Kode verifikasi salah.';
                } else {
                    db()->prepare('UPDATE password_resets SET used = 1 WHERE id = ?')->execute([$row['id']]);
                    session_regenerate_id(true);
                    $_SESSION['reset_email'] = $email;
                    $_SESSION['reset_time']  = time();
                    redirect('reset.php');
                }
            }
        } catch (PDOException $ex) {
            $error = 'Terjadi kesalahan pada server. Coba lagi nanti.';
        }
    }
}

$pageTitle = 'Reset Password';
$bodyClass = 'forgot-page';
?>
<?php require __DIR__ . '/header.php'; ?>
<h1 class="page-title grad-text" style="margin-top:56px;">Reset<br><span style="font-size:22px;">Password</span></h1>

<?php if ($error): ?><div class="alert alert-error"><?= e($error) ?></div><?php endif; ?>
<?php if ($info): ?><div class="alert alert-success"><?= e($info) ?></div><?php endif; ?>
<?php if ($devCode): ?>
  <div class="alert alert-info">[DEV MODE] Kode verifikasi: <strong><?= e($devCode) ?></strong></div>
<?php endif; ?>

<form class="form" method="post" action="forgot.php" novalidate>
  <?= csrf_field() ?>
  <!-- Tombol default saat tekan Enter: verifikasi kode -->
  <button type="submit" name="action" value="verify" hidden tabindex="-1" aria-hidden="true"></button>

  <div class="form-group">
    <label for="email">Email</label>
    <div class="input-wrap">
      <input class="field" type="email" id="email" name="email" value="<?= e($email) ?>" autocomplete="email" style="padding-right:90px;" required>
      <button class="field-action" type="submit" name="action" value="send">Send Code</button>
    </div>
    <p class="hint">Check your email for reset password</p>
  </div>

  <div class="form-group">
    <label for="code">Verification Code</label>
    <input class="field" type="text" id="code" name="code" inputmode="numeric" maxlength="6" autocomplete="one-time-code">
  </div>

  <button class="btn" type="submit" name="action" value="verify" style="margin-top:8px;">Continue</button>
</form>

<p class="link-row">Remember your password? <a href="signin.php">Sign In</a></p>
<?php require __DIR__ . '/footer.php'; ?>
