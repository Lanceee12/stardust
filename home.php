<?php
// Halaman sementara setelah login (placeholder)
require_once __DIR__ . '/config.php';

if (!is_logged_in()) {
    redirect('signin.php');
}

$stmt = db()->prepare('SELECT full_name, zodiac, location, email FROM users WHERE id = ?');
$stmt->execute([$_SESSION['user_id']]);
$user = $stmt->fetch();

if (!$user) {
    session_destroy();
    redirect('signin.php');
}

$pageTitle = 'Home';
$bodyClass = 'home-page';
?>
<?php require __DIR__ . '/header.php'; ?>
<h1 class="page-title grad-text" style="margin-top:40px;">Hello, <?= e($user['full_name']) ?></h1>

<div class="home-card">
  <div><span class="muted">Zodiac:</span> <?= e($user['zodiac']) ?></div>
  <div><span class="muted">Location:</span> <?= e($user['location']) ?></div>
  <div><span class="muted">Email:</span> <?= e($user['email']) ?></div>
</div>

<a class="btn" href="logout.php">Log Out</a>
<?php require __DIR__ . '/footer.php'; ?>
