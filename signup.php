<?php
// Halaman 3: Sign Up
require_once __DIR__ . '/config.php';

if (is_logged_in()) {
    redirect('home.php');
}

$errors = [];
$old = [
    'full_name' => '', 'gender' => '', 'birth_date' => '',
    'location' => '', 'phone' => '', 'email' => '',
];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!csrf_valid()) {
        $errors['form'] = 'Invalid session. Please reload.';
    } else {
        foreach ($old as $k => $_) {
            $old[$k] = trim($_POST[$k] ?? '');
        }
        $password = $_POST['password'] ?? '';
        $phone    = normalize_phone($old['phone']);

        if (mb_strlen($old['full_name']) < 2 || mb_strlen($old['full_name']) > 100) {
            $errors['full_name'] = 'Your name must have 2-100 character.';
        }
        if (!in_array($old['gender'], ['Male', 'Female', 'Others'], true)) {
            $errors['gender'] = 'Choose gender.';
        }

        $dt = DateTime::createFromFormat('Y-m-d', $old['birth_date']);
        if (!$dt || $dt->format('Y-m-d') !== $old['birth_date']) {
            $errors['birth_date'] = 'Invalid birthdate.';
        } elseif ($dt->diff(new DateTime('today'))->y < 18 || $dt > new DateTime('today')) {
            $errors['birth_date'] = 'You must be at least 18 years old.';
        }

        if ($old['location'] === '' || mb_strlen($old['location']) > 150) {
            $errors['location'] = 'You must input location.';
        }
        if (!preg_match('/^\+?[0-9]{8,15}$/', $phone)) {
            $errors['phone'] = 'Invalid phone number.';
        }
        if (!filter_var($old['email'], FILTER_VALIDATE_EMAIL)) {
            $errors['email'] = 'Invalid email format.';
        }
        if (strlen($password) < 8) {
            $errors['password'] = 'Password must contain at least 8 character.';
        }

        if (!$errors) {
            try {
                $stmt = db()->prepare('SELECT email, phone FROM users WHERE email = ? OR phone = ?');
                $stmt->execute([strtolower($old['email']), $phone]);
                foreach ($stmt->fetchAll() as $row) {
                    if ($row['email'] === strtolower($old['email'])) $errors['email'] = 'Email already registered.';
                    if ($row['phone'] === $phone) $errors['phone'] = 'Phone number already registered.';
                }

                if (!$errors) {
                    $ins = db()->prepare(
                        'INSERT INTO users (full_name, gender, birth_date, zodiac, location, phone, email, password_hash)
                         VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
                    );
                    $ins->execute([
                        $old['full_name'],
                        $old['gender'],
                        $old['birth_date'],
                        zodiac_from_date($old['birth_date']),
                        $old['location'],
                        $phone,
                        strtolower($old['email']),
                        password_hash($password, PASSWORD_DEFAULT),
                    ]);

                    session_regenerate_id(true);
                    $_SESSION['user_id'] = (int)db()->lastInsertId();
                    redirect('home.php');
                }
            } catch (PDOException $ex) {
                $errors['form'] = 'There's been an error on the server. Try again later.';
            }
        }
    }
}

$pageTitle = 'Sign Up';
$bodyClass = 'signup-page';
?>
<?php require __DIR__ . '/header.php'; ?>
<h1 class="page-title grad-text">Welcome To<br><span style="font-size:24px;">Stardust</span></h1>

<?php if (!empty($errors['form'])): ?>
  <div class="alert alert-error"><?= e($errors['form']) ?></div>
<?php endif; ?>

<form class="form" method="post" action="signup.php" novalidate>
  <?= csrf_field() ?>

  <div class="form-group">
    <label for="full_name">Full Name</label>
    <input class="field" type="text" id="full_name" name="full_name" value="<?= e($old['full_name']) ?>" autocomplete="name" required>
    <?php if (isset($errors['full_name'])): ?><span class="error-text"><?= e($errors['full_name']) ?></span><?php endif; ?>
  </div>

  <div class="form-group">
    <span class="label">Gender</span>
    <div class="radio-row">
      <?php foreach (['Male', 'Female', 'Others'] as $g): ?>
        <label>
          <input type="radio" name="gender" value="<?= $g ?>" <?= $old['gender'] === $g ? 'checked' : '' ?>>
          <span class="dot"></span> <?= $g ?>
        </label>
      <?php endforeach; ?>
    </div>
    <?php if (isset($errors['gender'])): ?><span class="error-text"><?= e($errors['gender']) ?></span><?php endif; ?>
  </div>

  <div class="form-group">
    <label for="birth_date">Birth Date</label>
    <input class="field" type="date" id="birth_date" name="birth_date" value="<?= e($old['birth_date']) ?>"
           max="<?= date('Y-m-d') ?>" required>
    <?php if (isset($errors['birth_date'])): ?><span class="error-text"><?= e($errors['birth_date']) ?></span><?php endif; ?>
  </div>

  <div class="form-group">
    <label for="location">Location</label>
    <input class="field" type="text" id="location" name="location" value="<?= e($old['location']) ?>" required>
    <?php if (isset($errors['location'])): ?><span class="error-text"><?= e($errors['location']) ?></span><?php endif; ?>
  </div>

  <div class="form-group">
    <label for="phone">Phone Number</label>
    <input class="field" type="tel" id="phone" name="phone" value="<?= e($old['phone']) ?>" autocomplete="tel" required>
    <?php if (isset($errors['phone'])): ?><span class="error-text"><?= e($errors['phone']) ?></span><?php endif; ?>
  </div>

  <div class="form-group">
    <label for="email">Email</label>
    <input class="field" type="email" id="email" name="email" value="<?= e($old['email']) ?>" autocomplete="email" required>
    <?php if (isset($errors['email'])): ?><span class="error-text"><?= e($errors['email']) ?></span><?php endif; ?>
  </div>

  <div class="form-group">
    <label for="password">Password</label>
    <input class="field" type="password" id="password" name="password" autocomplete="new-password" required>
    <?php if (isset($errors['password'])): ?><span class="error-text"><?= e($errors['password']) ?></span><?php endif; ?>
  </div>

  <button class="btn" type="submit" style="margin-top:8px;">Sign Up</button>
</form>

<p class="link-row">Already have an account? <a href="signin.php">Sign In</a></p>
<?php require __DIR__ . '/footer.php'; ?>
