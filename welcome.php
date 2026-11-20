<?php
// Halaman 2: Login / Welcome
require_once __DIR__ . '/config.php';

if (is_logged_in()) {
    redirect('home.php');
}

$pageTitle = 'Find Your Star';
$bodyClass = 'welcome-page';
?>
<?php require __DIR__ . '/header.php'; ?>
<section class="welcome" style="display:flex;flex-direction:column;align-items:center;flex:1;">
  <!-- Ganti dengan ilustrasi asli: <img class="hero-img" src="assets/hero.png" alt=""> -->
  <div class="hero-img img-placeholder">Image placeholder</div>

  <h2 class="brand grad-text" style="font-family:'Poppins',sans-serif;font-size:22px;">Stardust</h2>
  <span class="badge">&#10022; COSMIC MATCHMAKING</span>

  <h1 class="grad-text">Find Your Star</h1>
  <p>Let the ancient wisdom of the constellations guide you to your true alignment. A cosmic connection awaits.</p>

  <div class="actions">
    <a class="btn" href="signup.php">Get Started &rarr;</a>
    <div class="center"><a class="link-small" href="signin.php">I Already Have an Account</a></div>
  </div>
</section>
<?php require __DIR__ . '/footer.php'; ?>
