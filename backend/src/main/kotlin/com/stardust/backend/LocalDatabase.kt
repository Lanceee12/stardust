package com.stardust.backend

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class AuthenticatedAccount(val id: Long, val state: PersistedUserState)

class DuplicateEmailException : RuntimeException()

object LocalDatabase {
    private const val passwordIterations = 160_000
    private const val sessionLifetimeSeconds = 60L * 60L * 24L * 30L
    private const val resetLifetimeSeconds = 60L * 15L
    private val random = SecureRandom()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val databaseUrl: String

    init {
        val file = File(System.getenv("STARDUST_DB_PATH") ?: "data/stardust.sqlite").absoluteFile
        file.parentFile?.mkdirs()
        Class.forName("org.sqlite.JDBC")
        databaseUrl = "jdbc:sqlite:${file.path}"
        connect().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
                statement.execute("CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT NOT NULL UNIQUE COLLATE NOCASE, salt BLOB NOT NULL, password_hash BLOB NOT NULL, reset_code_hash TEXT, reset_expires_at INTEGER)")
                statement.execute("CREATE TABLE IF NOT EXISTS sessions (token_hash TEXT PRIMARY KEY, account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE, expires_at INTEGER NOT NULL)")
                statement.execute("CREATE TABLE IF NOT EXISTS user_state (account_id INTEGER PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE, state_json TEXT NOT NULL)")
                statement.execute("CREATE TABLE IF NOT EXISTS user_connections (account_a INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE, account_b INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE, created_at INTEGER NOT NULL, PRIMARY KEY(account_a, account_b), CHECK(account_a < account_b))")
                statement.execute("CREATE TABLE IF NOT EXISTS direct_messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sender_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE, recipient_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE, text TEXT NOT NULL, sent_at INTEGER NOT NULL)")
                statement.execute("CREATE INDEX IF NOT EXISTS direct_messages_conversation ON direct_messages(sender_id, recipient_id, id)")
                statement.execute("CREATE TABLE IF NOT EXISTS swipe_notifications (id INTEGER PRIMARY KEY AUTOINCREMENT, recipient_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE, actor_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE, created_at INTEGER NOT NULL, read_at INTEGER, UNIQUE(recipient_id, actor_id))")
            }
        }
    }

    @Synchronized
    fun discoverableUsers(excludingAccountId: Long): List<RegisteredAccountProfile> = connect().use { connection ->
        connection.prepareStatement("SELECT accounts.id, user_state.state_json FROM accounts JOIN user_state ON user_state.account_id = accounts.id WHERE accounts.id != ? ORDER BY accounts.id DESC").use { query ->
            query.setLong(1, excludingAccountId)
            query.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        val state = json.decodeFromString<PersistedUserState>(result.getString("state_json"))
                        add(RegisteredAccountProfile(result.getLong("id"), state.profile))
                    }
                }
            }
        }
    }

    @Synchronized
    fun connectUsers(firstAccountId: Long, secondAccountId: Long) {
        if (firstAccountId == secondAccountId) return
        val first = minOf(firstAccountId, secondAccountId)
        val second = maxOf(firstAccountId, secondAccountId)
        connect().use { connection ->
            connection.prepareStatement("INSERT OR IGNORE INTO user_connections(account_a, account_b, created_at) VALUES(?, ?, ?)").use { insert ->
                insert.setLong(1, first)
                insert.setLong(2, second)
                insert.setLong(3, Instant.now().toEpochMilli())
                insert.executeUpdate()
            }
        }
    }

    @Synchronized
    fun connectedUsers(accountId: Long): List<Long> = connect().use { connection ->
        connection.prepareStatement("SELECT CASE WHEN account_a = ? THEN account_b ELSE account_a END AS other_id FROM user_connections WHERE account_a = ? OR account_b = ? ORDER BY created_at DESC").use { query ->
            query.setLong(1, accountId)
            query.setLong(2, accountId)
            query.setLong(3, accountId)
            query.executeQuery().use { result -> buildList { while (result.next()) add(result.getLong("other_id")) } }
        }
    }

    @Synchronized
    fun accountState(accountId: Long): PersistedUserState? = connect().use { connection ->
        connection.prepareStatement("SELECT state_json FROM user_state WHERE account_id = ?").use { query ->
            query.setLong(1, accountId)
            query.executeQuery().use { result ->
                if (result.next()) json.decodeFromString(result.getString(1)) else null
            }
        }
    }

    @Synchronized
    fun createSwipeNotification(recipientId: Long, actorId: Long) = connect().use { connection ->
        connection.prepareStatement("INSERT OR IGNORE INTO swipe_notifications(recipient_id, actor_id, created_at) VALUES(?, ?, ?)").use { insert ->
            insert.setLong(1, recipientId)
            insert.setLong(2, actorId)
            insert.setLong(3, Instant.now().toEpochMilli())
            insert.executeUpdate()
        }
    }

    @Synchronized
    fun unreadSwipeNotifications(accountId: Long): List<SwipeNotification> = connect().use { connection ->
        connection.prepareStatement("SELECT swipe_notifications.id, swipe_notifications.actor_id, swipe_notifications.created_at, user_state.state_json FROM swipe_notifications JOIN user_state ON user_state.account_id = swipe_notifications.actor_id WHERE swipe_notifications.recipient_id = ? AND swipe_notifications.read_at IS NULL ORDER BY swipe_notifications.id").use { query ->
            query.setLong(1, accountId)
            query.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        val actor = json.decodeFromString<PersistedUserState>(result.getString("state_json")).profile
                        val actorId = result.getLong("actor_id")
                        add(SwipeNotification(result.getLong("id"), actor.name, "member-$actorId", result.getLong("created_at")))
                    }
                }
            }
        }
    }

    @Synchronized
    fun markSwipeNotificationRead(accountId: Long, notificationId: Long) = connect().use { connection ->
        connection.prepareStatement("UPDATE swipe_notifications SET read_at = ? WHERE id = ? AND recipient_id = ? AND read_at IS NULL").use { update ->
            update.setLong(1, Instant.now().toEpochMilli())
            update.setLong(2, notificationId)
            update.setLong(3, accountId)
            update.executeUpdate()
        }
    }

    @Synchronized
    fun isConnected(firstAccountId: Long, secondAccountId: Long): Boolean {
        if (firstAccountId == secondAccountId) return false
        val first = minOf(firstAccountId, secondAccountId)
        val second = maxOf(firstAccountId, secondAccountId)
        return connect().use { connection ->
            connection.prepareStatement("SELECT 1 FROM user_connections WHERE account_a = ? AND account_b = ?").use { query ->
                query.setLong(1, first)
                query.setLong(2, second)
                query.executeQuery().use { it.next() }
            }
        }
    }

    @Synchronized
    fun sendDirectMessage(senderId: Long, recipientId: Long, text: String): List<ChatMessage> {
        connect().use { connection ->
            connection.prepareStatement("INSERT INTO direct_messages(sender_id, recipient_id, text, sent_at) VALUES(?, ?, ?, ?)").use { insert ->
                insert.setLong(1, senderId)
                insert.setLong(2, recipientId)
                insert.setString(3, text)
                insert.setLong(4, Instant.now().toEpochMilli())
                insert.executeUpdate()
            }
        }
        return directMessages(senderId, recipientId)
    }

    @Synchronized
    fun directMessages(accountId: Long, otherAccountId: Long): List<ChatMessage> = connect().use { connection ->
        connection.prepareStatement("SELECT id, sender_id, text, sent_at FROM direct_messages WHERE (sender_id = ? AND recipient_id = ?) OR (sender_id = ? AND recipient_id = ?) ORDER BY id").use { query ->
            query.setLong(1, accountId)
            query.setLong(2, otherAccountId)
            query.setLong(3, otherAccountId)
            query.setLong(4, accountId)
            query.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        val time = Instant.ofEpochMilli(result.getLong("sent_at")).atZone(ZoneId.systemDefault()).toLocalTime()
                        add(ChatMessage("shared-${result.getLong("id")}", result.getString("text"), result.getLong("sender_id") == accountId, time.format(DateTimeFormatter.ofPattern("HH:mm"))))
                    }
                }
            }
        }
    }

    @Synchronized
    fun register(request: RegisterRequest): AuthSession {
        val normalizedEmail = request.email.trim().lowercase()
        return connect().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement("SELECT 1 FROM accounts WHERE email = ?").use { query ->
                    query.setString(1, normalizedEmail)
                    query.executeQuery().use { if (it.next()) throw DuplicateEmailException() }
                }
                val salt = ByteArray(16).also(random::nextBytes)
                val profile = UserProfile(
                    name = request.name.trim(),
                    age = Period.between(LocalDate.parse(request.birthDate), LocalDate.now()).years,
                    gender = request.gender,
                    sexuality = request.sexuality,
                    ageRange = request.ageRange,
                    interests = request.interests,
                    birthDate = request.birthDate,
                    location = request.location.trim(),
                    phoneNumber = request.phoneNumber.trim(),
                    email = normalizedEmail
                )
                val accountId = connection.prepareStatement(
                    "INSERT INTO accounts(email, salt, password_hash) VALUES(?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
                ).use { insert ->
                    insert.setString(1, normalizedEmail)
                    insert.setBytes(2, salt)
                    insert.setBytes(3, passwordHash(request.password, salt))
                    insert.executeUpdate()
                    insert.generatedKeys.use { keys ->
                        check(keys.next()) { "SQLite did not return an account id" }
                        keys.getLong(1)
                    }
                }
                val state = PersistedUserState(profile)
                connection.prepareStatement("INSERT INTO user_state(account_id, state_json) VALUES(?, ?)").use { insert ->
                    insert.setLong(1, accountId)
                    insert.setString(2, json.encodeToString(state))
                    insert.executeUpdate()
                }
                val token = createSession(connection, accountId)
                connection.commit()
                AuthSession(token, profile)
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            }
        }
    }

    @Synchronized
    fun login(email: String, password: String): AuthSession? = connect().use { connection ->
        connection.prepareStatement("SELECT accounts.id, accounts.salt, accounts.password_hash, user_state.state_json FROM accounts JOIN user_state ON user_state.account_id = accounts.id WHERE accounts.email = ?").use { query ->
            query.setString(1, email.trim())
            query.executeQuery().use { result ->
                if (!result.next()) return@use null
                val salt = result.getBytes("salt")
                if (!MessageDigest.isEqual(result.getBytes("password_hash"), passwordHash(password, salt))) return@use null
                val accountId = result.getLong("id")
                val state = json.decodeFromString<PersistedUserState>(result.getString("state_json"))
                AuthSession(createSession(connection, accountId), state.profile)
            }
        }
    }

    @Synchronized
    fun authenticate(token: String?): AuthenticatedAccount? {
        if (token.isNullOrBlank()) return null
        return connect().use { connection ->
            connection.prepareStatement("SELECT sessions.account_id, sessions.expires_at, user_state.state_json FROM sessions JOIN user_state ON user_state.account_id = sessions.account_id WHERE sessions.token_hash = ?").use { query ->
                query.setString(1, sha256(token))
                query.executeQuery().use { result ->
                    if (!result.next() || result.getLong("expires_at") < Instant.now().epochSecond) return@use null
                    AuthenticatedAccount(result.getLong("account_id"), json.decodeFromString(result.getString("state_json")))
                }
            }
        }
    }

    @Synchronized
    fun saveState(accountId: Long, state: PersistedUserState) = connect().use { connection ->
        connection.prepareStatement("UPDATE user_state SET state_json = ? WHERE account_id = ?").use { update ->
            update.setString(1, json.encodeToString(state))
            update.setLong(2, accountId)
            check(update.executeUpdate() == 1) { "Account state could not be saved" }
        }
    }

    @Synchronized
    fun updateState(accountId: Long, updateState: (PersistedUserState) -> PersistedUserState): PersistedUserState {
        return connect().use { connection ->
            connection.autoCommit = false
            try {
                val current = connection.prepareStatement("SELECT state_json FROM user_state WHERE account_id = ?").use { query ->
                    query.setLong(1, accountId)
                    query.executeQuery().use { result ->
                        check(result.next()) { "Account state was not found" }
                        json.decodeFromString<PersistedUserState>(result.getString(1))
                    }
                }
                val updated = updateState(current)
                connection.prepareStatement("UPDATE user_state SET state_json = ? WHERE account_id = ?").use { update ->
                    update.setString(1, json.encodeToString(updated))
                    update.setLong(2, accountId)
                    update.executeUpdate()
                }
                connection.commit()
                updated
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            }
        }
    }

    @Synchronized
    fun revokeSession(token: String?) {
        if (token.isNullOrBlank()) return
        connect().use { connection ->
            connection.prepareStatement("DELETE FROM sessions WHERE token_hash = ?").use { delete ->
                delete.setString(1, sha256(token))
                delete.executeUpdate()
            }
        }
    }

    @Synchronized
    fun issueResetCode(email: String): String? = connect().use { connection ->
        connection.prepareStatement("SELECT id FROM accounts WHERE email = ?").use { query ->
            query.setString(1, email.trim())
            query.executeQuery().use { result ->
                if (!result.next()) return@use null
                val accountId = result.getLong(1)
                val code = "%06d".format(random.nextInt(1_000_000))
                connection.prepareStatement("UPDATE accounts SET reset_code_hash = ?, reset_expires_at = ? WHERE id = ?").use { update ->
                    update.setString(1, sha256("${email.trim().lowercase()}:$code"))
                    update.setLong(2, Instant.now().epochSecond + resetLifetimeSeconds)
                    update.setLong(3, accountId)
                    update.executeUpdate()
                }
                code
            }
        }
    }

    @Synchronized
    fun resetPassword(email: String, code: String, newPassword: String): Boolean = connect().use { connection ->
        connection.prepareStatement("SELECT id, reset_code_hash, reset_expires_at FROM accounts WHERE email = ?").use { query ->
            query.setString(1, email.trim())
            query.executeQuery().use { result ->
                if (!result.next()) return@use false
                val accountId = result.getLong("id")
                val expected = result.getString("reset_code_hash") ?: return@use false
                if ((result.getLong("reset_expires_at") ?: 0L) < Instant.now().epochSecond ||
                    !MessageDigest.isEqual(expected.toByteArray(), sha256("${email.trim().lowercase()}:$code").toByteArray())) return@use false
                val salt = ByteArray(16).also(random::nextBytes)
                connection.prepareStatement("UPDATE accounts SET salt = ?, password_hash = ?, reset_code_hash = NULL, reset_expires_at = NULL WHERE id = ?").use { update ->
                    update.setBytes(1, salt)
                    update.setBytes(2, passwordHash(newPassword, salt))
                    update.setLong(3, accountId)
                    update.executeUpdate() == 1
                }
            }
        }
    }

    private fun createSession(connection: Connection, accountId: Long): String {
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))
        connection.prepareStatement("INSERT INTO sessions(token_hash, account_id, expires_at) VALUES(?, ?, ?)").use { insert ->
            insert.setString(1, sha256(token))
            insert.setLong(2, accountId)
            insert.setLong(3, Instant.now().epochSecond + sessionLifetimeSeconds)
            insert.executeUpdate()
        }
        return token
    }

    private fun connect(): Connection = DriverManager.getConnection(databaseUrl)

    private fun passwordHash(password: String, salt: ByteArray): ByteArray = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(PBEKeySpec(password.toCharArray(), salt, passwordIterations, 256)).encoded

    private fun sha256(value: String): String = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.toByteArray()))
}