<?php
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
  <div class="logo-circle">
      <img class="logo-img" src="logo.jpg" alt="Stardust">
  </div>
  <h1 class="brand grad-text">Stardust</h1>
  <p class="tagline">FIND YOUR DESTINED STAR</p>
</section>
<script>
  setTimeout(function () { location.href = '<?= e($next) ?>'; }, 2500);
</script>
<?php require __DIR__ . '/footer.php'; ?>
