<?php
// Halaman 4: Sign In
require_once __DIR__ . '/config.php';

if (is_logged_in()) {
    redirect('home.php');
}

$error = '';
$email = '';
$notice = isset($_GET['reset']) ? 'Password berhasil diubah. Silakan sign in.' : '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email    = strtolower(trim($_POST['email'] ?? ''));
    $password = $_POST['password'] ?? '';

    if (!csrf_valid()) {
        $error = 'Sesi tidak valid. Silakan muat ulang halaman.';
    } elseif ($email === '' || $password === '') {
        $error = 'Email dan password wajib diisi.';
    } else {
        try {
            $stmt = db()->prepare('SELECT id, password_hash FROM users WHERE email = ? LIMIT 1');
            $stmt->execute([$email]);
            $user = $stmt->fetch();

            if ($user && password_verify($password, $user['password_hash'])) {
                session_regenerate_id(true);
                $_SESSION['user_id'] = (int)$user['id'];
                redirect('home.php');
            }
            $error = 'Email atau password salah.';
        } catch (PDOException $ex) {
            $error = 'Terjadi kesalahan pada server. Coba lagi nanti.';
        }
    }
}

$pageTitle = 'Sign In';
$bodyClass = 'signin-page';
?>
<?php require __DIR__ . '/header.php'; ?>
<h1 class="page-title grad-text" style="margin-top:56px;">Sign In To<br><span style="font-size:22px;">Stardust</span></h1>

<?php if ($notice): ?><div class="alert alert-success"><?= e($notice) ?></div><?php endif; ?>
<?php if ($error): ?><div class="alert alert-error"><?= e($error) ?></div><?php endif; ?>

<form class="form" method="post" action="signin.php" novalidate>
  <?= csrf_field() ?>

  <div class="form-group">
    <label for="email">Email</label>
    <input class="field" type="email" id="email" name="email" value="<?= e($email) ?>" autocomplete="email" required>
  </div>

  <div class="form-group">
    <label for="password">Password</label>
    <input class="field" type="password" id="password" name="password" autocomplete="current-password" required>
  </div>

  <div class="forgot-link"><a href="forgot.php">Forgot Password?</a></div>

  <button class="btn" type="submit">Sign In</button>
</form>

<p class="link-row">Don't have an account? <a href="signup.php">Sign Up</a></p>
<?php require __DIR__ . '/footer.php'; ?>
