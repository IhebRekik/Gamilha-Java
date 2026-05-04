package com.gamilha.services;

import com.gamilha.entity.User;
import com.gamilha.utils.ConnectionManager;
import com.gamilha.utils.PasswordHasher;
import com.gamilha.utils.SessionContext;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final Connection conn = ConnectionManager.getConnection();

    // ───────────── CRUD ─────────────

    public void add(User user) {
        String sql = "INSERT INTO user (email,password,name,profile_image,roles,reports,is_active,created_at," +
                "login_attempts,last_seen,is_online,verified) VALUES (?,?,?,?,?,?,?,?,0,NOW(),0,0)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, PasswordHasher.hash(user.getPassword()));
            ps.setString(3, user.getName());
            ps.setString(4, user.getProfileImage());
            ps.setString(5, user.getRoles() != null ? user.getRoles() : "[\"ROLE_USER\"]");
            ps.setInt(6, user.getReports());
            ps.setBoolean(7, user.isActive());
            ps.setTimestamp(8, user.getCreatedAt() != null ? user.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) user.setId(rs.getInt(1));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void update(User user) {
        boolean changePassword = user.getPassword() != null && !user.getPassword().isEmpty();
        String sql = "UPDATE user SET email=?, name=?, profile_image=?, roles=?, reports=?, is_active=?" +
                (changePassword ? ", password=?" : "") + " WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getName());
            ps.setString(3, user.getProfileImage());
            ps.setString(4, user.getRoles());
            ps.setInt(5, user.getReports());
            ps.setBoolean(6, user.isActive());
            if (changePassword) {
                String pwd = user.getPassword();
                if (!isBcryptHash(pwd)) pwd = PasswordHasher.hash(pwd);
                ps.setString(7, pwd);
                ps.setInt(8, user.getId());
            } else {
                ps.setInt(7, user.getId());
            }
            ps.executeUpdate();
            addAuditLog(user.getId(), "Profil modifié");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM user WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public User findByEmail(String email) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM user WHERE email=?")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapFull(rs);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public User findById(int id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM user WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapFull(rs);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM user");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapFull(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ───────────── LOGIN with attempts ─────────────

    public User login(String email, String plainPassword) {
        String sql = "SELECT * FROM user WHERE email=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            User user = mapFull(rs);

            // Check lock
            if (user.getLockExpiry() != null && user.getLockExpiry().getTime() > System.currentTimeMillis()) {
                long remaining = (user.getLockExpiry().getTime() - System.currentTimeMillis()) / 1000;
                throw new RuntimeException("LOCKED:" + remaining);
            }

            if (!isPasswordValid(plainPassword, user.getPassword())) {
                int attempts = user.getLoginAttempts() + 1;
                updateLoginAttempts(user.getId(), attempts);
                if (attempts >= 3) {
                    // Capture photo and send email silently
                    new Thread(() -> {
                        byte[] photo = com.gamilha.utils.CameraUtils.capturePhoto();
                        if (photo != null) {
                            com.gamilha.utils.EmailSender.sendIntruderPhoto(user.getEmail(), user.getEmail(), photo);
                        }
                    }).start();

                    Timestamp lockExpiry = new Timestamp(System.currentTimeMillis() + 60_000);
                    lockAccount(user.getId(), lockExpiry);
                    throw new RuntimeException("LOCKED:60");
                }
                return null;
            }

            // Reset attempts on success
            resetLoginAttempts(user.getId());

            // Silently reactivate if deactivated
            if (!user.isActive()) {
                user.setActive(true);
                user.setDeactivateUntil(null);
                toggleActive(user.getId(), true); // Also clears deactivate_until if we update toggleActive
            }

            updateOnlineStatus(user.getId(), true);
            return user;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateLoginAttempts(int userId, int attempts) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE user SET login_attempts=? WHERE id=?")) {
            ps.setInt(1, attempts);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void resetLoginAttempts(int userId) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE user SET login_attempts=0, lock_expiry=NULL WHERE id=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void lockAccount(int userId, Timestamp until) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE user SET lock_expiry=? WHERE id=?")) {
            ps.setTimestamp(1, until);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ───────────── ONLINE STATUS ─────────────

    public void updateOnlineStatus(int userId, boolean online) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE user SET is_online=?, last_seen=NOW() WHERE id=?")) {
            ps.setBoolean(1, online);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void logout(int userId) {
        updateOnlineStatus(userId, false);
    }

    // ───────────── BAN ─────────────

    public void banUser(int userId, String banUntil) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE user SET ban_until=? WHERE id=?")) {
            ps.setString(1, banUntil);
            ps.setInt(2, userId);
            ps.executeUpdate();
            addAuditLog(userId, "Utilisateur banni jusqu'au " + banUntil);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void unbanUser(int userId) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE user SET ban_until=NULL WHERE id=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            addAuditLog(userId, "Bannissement levé");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void toggleActive(int userId, boolean active) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE user SET is_active=?, deactivate_until=NULL WHERE id=?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            ps.executeUpdate();
            addAuditLog(userId, active ? "Compte activé (Réactivation)" : "Compte désactivé");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void deactivateAccount(int userId, Timestamp until) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE user SET is_active=0, deactivate_until=? WHERE id=?")) {
            ps.setTimestamp(1, until);
            ps.setInt(2, userId);
            ps.executeUpdate();
            addAuditLog(userId, "Compte désactivé par l'utilisateur" + (until != null ? " jusqu'au " + until : ""));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ───────────── PASSWORD RESET ─────────────

    public boolean sendResetOtp(String email) {
        User user = findByEmail(email);
        if (user == null) return false;
        String otp = generateOtp(user.getId());
        try {
            com.gamilha.utils.EmailSender.sendResetCode(email, otp, user.getName());
            addAuditLog(user.getId(), "Code OTP de réinitialisation envoyé");
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean resetPasswordWithOtp(String email, String otp, String newPassword) {
        User user = findByEmail(email);
        if (user == null) return false;
        if (verifyOtp(user.getId(), otp)) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE user SET password=?, two_factor_code=NULL WHERE id=?")) {
                ps.setString(1, PasswordHasher.hash(newPassword));
                ps.setInt(2, user.getId());
                ps.executeUpdate();
                addAuditLog(user.getId(), "Mot de passe réinitialisé via OTP");
                return true;
            } catch (Exception e) { e.printStackTrace(); }
        }
        return false;
    }

    // ───────────── OTP 2FA ─────────────

    public String generateOtp(int userId) {
        String code = String.valueOf((int)(Math.random()*900000+100000));
        try (PreparedStatement ps = conn.prepareStatement("UPDATE user SET two_factor_code=? WHERE id=?")) {
            ps.setString(1, code);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
        return code;
    }

    public boolean verifyOtp(int userId, String code) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT two_factor_code FROM user WHERE id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String stored = rs.getString("two_factor_code");
                return code.equals(stored);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // ───────────── AUDIT LOG ─────────────

    public void addAuditLog(int userId, String action) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO audit_log (user_id, action, created_at) VALUES (?,?,NOW()) " +
                "ON DUPLICATE KEY UPDATE created_at=NOW()")) {
            // fallback: just update notes field if audit_log table doesn't exist
        } catch (Exception ignored) {}
        // Simple: append to user notes field
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE user SET audit_log = CONCAT(IFNULL(audit_log,''), ?, '\n') WHERE id=?")) {
            String entry = "[" + new java.util.Date() + "] " + action;
            ps.setString(1, entry);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) { /* ignore if column missing */ }
    }

    // ───────────── AMIS ─────────────

    public List<User> getAmis() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM friend WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            User current = SessionContext.getCurrentUser();
            ps.setInt(1, current.getId());
            ResultSet rs = ps.executeQuery();
            List<User> all = findAll();
            while (rs.next()) {
                int friendId = rs.getInt("friend_id");
                all.stream().filter(u -> u.getId() == friendId).findFirst().ifPresent(list::add);
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return list;
    }

    // ───────────── MAPPING ─────────────

    private User mapFull(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setName(rs.getString("name"));
        u.setPassword(rs.getString("password"));
        u.setProfileImage(rs.getString("profile_image"));
        u.setRoles(rs.getString("roles"));
        u.setReports(rs.getInt("reports"));
        u.setActive(rs.getBoolean("is_active"));
        u.setCreatedAt(rs.getTimestamp("created_at"));

        // Safe optional columns
        try { u.setBanUntil(rs.getString("ban_until")); } catch (Exception ignored) {}
        try { u.setLoginAttempts(rs.getInt("login_attempts")); } catch (Exception ignored) {}
        try { u.setLockExpiry(rs.getTimestamp("lock_expiry")); } catch (Exception ignored) {}
        try { u.setLastSeen(rs.getTimestamp("last_seen")); } catch (Exception ignored) {}
        try { u.setOnline(rs.getBoolean("is_online")); } catch (Exception ignored) {}
        try { u.setLastIp(rs.getString("last_ip")); } catch (Exception ignored) {}
        try { u.setLastDevice(rs.getString("last_device")); } catch (Exception ignored) {}
        try { u.setAuditLog(rs.getString("audit_log")); } catch (Exception ignored) {}
        try { u.setVerified(rs.getBoolean("verified")); } catch (Exception ignored) {}
        try { u.setTwoFactorCode(rs.getString("two_factor_code")); } catch (Exception ignored) {}
        try { u.setDeactivateUntil(rs.getTimestamp("deactivate_until")); } catch (Exception ignored) {}

        return u;
    }

    // ───────────── PASSWORD ─────────────

    private boolean isPasswordValid(String plain, String stored) {
        if (stored == null) return false;
        String normalized = stored.startsWith("$2y$") ? "$2a$" + stored.substring(4) : stored;
        if (isBcryptHash(normalized)) return BCrypt.checkpw(plain, normalized);
        return plain.equals(stored);
    }

    private boolean isBcryptHash(String s) {
        return s != null && (s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$"));
    }
}
