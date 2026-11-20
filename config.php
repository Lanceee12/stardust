<?php
// ---------- Konfigurasi ----------
define('DB_HOST', 'localhost');
define('DB_NAME', 'stardust');
define('DB_USER', 'root');
define('DB_PASS', '');

// DEV_MODE true: kode verifikasi reset password ditampilkan di layar
// (karena belum ada pengiriman email). Ubah ke false saat production.
define('DEV_MODE', true);

if (session_status() === PHP_SESSION_NONE) {
    session_set_cookie_params([
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
    session_start();
}

// ---------- Database ----------
function db(): PDO
{
    static $pdo = null;
    if ($pdo === null) {
        $dsn = 'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4';
        $pdo = new PDO($dsn, DB_USER, DB_PASS, [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        ]);
    }
    return $pdo;
}

// ---------- Helper ----------
function e($value): string
{
    return htmlspecialchars((string)($value ?? ''), ENT_QUOTES, 'UTF-8');
}

function redirect(string $url): void
{
    header('Location: ' . $url);
    exit;
}

function csrf_token(): string
{
    if (empty($_SESSION['csrf'])) {
        $_SESSION['csrf'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf'];
}

function csrf_field(): string
{
    return '<input type="hidden" name="csrf" value="' . e(csrf_token()) . '">';
}

function csrf_valid(): bool
{
    return isset($_POST['csrf'], $_SESSION['csrf'])
        && hash_equals($_SESSION['csrf'], $_POST['csrf']);
}

function is_logged_in(): bool
{
    return !empty($_SESSION['user_id']);
}

function normalize_phone(string $phone): string
{
    return preg_replace('/[\s\-\(\)]/', '', $phone);
}

function zodiac_from_date(string $date): string
{
    $d = new DateTime($date);
    $m = (int)$d->format('n');
    $day = (int)$d->format('j');

    $signs = [
        [1, 20, 'Capricorn'], [2, 19, 'Aquarius'], [3, 21, 'Pisces'],
        [4, 20, 'Aries'],     [5, 21, 'Taurus'],   [6, 21, 'Gemini'],
        [7, 23, 'Cancer'],    [8, 23, 'Leo'],      [9, 23, 'Virgo'],
        [10, 23, 'Libra'],    [11, 22, 'Scorpio'], [12, 22, 'Sagittarius'],
    ];
    // Tanda untuk bulan $m berlaku sebelum tanggal batas, setelahnya tanda berikutnya
    [$limitMonth, $limitDay, $before] = $signs[$m - 1];
    if ($day < $limitDay) {
        return $before;
    }
    return $signs[$m % 12][2];
}
