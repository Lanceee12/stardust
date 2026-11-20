<?php
// Halaman 1: Logo / Splash
require_once __DIR__ . '/config.php';

if (is_logged_in()) {
    redirect('home.php');
}

$pageTitle = 'Welcome';
$bodyClass = 'splash-page';
$next = 'welcome.php';
?>
<?php require __DIR__ . '/header.php'; ?>
<section class="splash" onclick="location.href='<?= e($next) ?>'">
  <!-- Ganti dengan logo asli: <img src="assets/logo.png" alt="Stardust"> -->
  <div class="logo-circle">
    <svg class="star-top" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#f4d089" stroke-width="2"><path d="M12 2l2.9 6.9L22 10l-5.5 4.8L18 22l-6-3.7L6 22l1.5-7.2L2 10l7.1-1.1z"/></svg>
    <svg width="46" height="46" viewBox="0 0 24 24" fill="none" stroke="#f4d089" stroke-width="1.6"><path d="M12 21s-7.5-4.6-9.3-9.1C1.4 8.6 3.4 5 6.9 5c2 0 3.6 1.1 5.1 3 1.5-1.9 3.1-3 5.1-3 3.5 0 5.5 3.6 4.2 6.9C19.5 16.4 12 21 12 21z"/></svg>
  </div>
  <h1 class="brand grad-text">Stardust</h1>
  <p class="tagline">FIND YOUR DESTINED STAR</p>
</section>
<script>
  setTimeout(function () { location.href = '<?= e($next) ?>'; }, 2500);
</script>
<?php require __DIR__ . '/footer.php'; ?>
