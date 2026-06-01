package org.dpbe.global.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.dpbe.global.auth.entity.AuthUser;
import org.dpbe.global.auth.entity.UserRole;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class AuthUserRepository {

    private final SqlExecutor sql;

    public AuthUserRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public AuthUser findByUsername(String username) {
        return sql.queryOne("""
                SELECT au.id, au.username, au.password_hash, au.role, au.linked_customer_id,
                       c.customer_id AS linked_customer_no,
                       au.display_name, au.enabled, au.password_change_required,
                       au.created_at, au.updated_at
                  FROM auth_users au
                  LEFT JOIN customers c ON c.id = au.linked_customer_id
                 WHERE au.username = ?
                """, this::mapRow, username);
    }

    public boolean existsByUsername(String username) {
        Integer count = sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM auth_users WHERE username = ?",
                rs -> rs.getInt("cnt"),
                username);
        return count != null && count > 0;
    }

    public boolean existsByLinkedCustomerId(Long linkedCustomerId) {
        Integer count = sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM auth_users WHERE linked_customer_id = ?",
                rs -> rs.getInt("cnt"),
                linkedCustomerId);
        return count != null && count > 0;
    }

    public void save(String username,
                     String passwordHash,
                     UserRole role,
                     Long linkedCustomerId,
                     String displayName,
                     boolean enabled) {
        save(username, passwordHash, role, linkedCustomerId, displayName, enabled, false);
    }

    public void save(String username,
                     String passwordHash,
                     UserRole role,
                     Long linkedCustomerId,
                     String displayName,
                     boolean enabled,
                     boolean passwordChangeRequired) {
        sql.executeUpdate("""
                INSERT INTO auth_users
                    (username, password_hash, role, linked_customer_id, display_name, enabled, password_change_required)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                username, passwordHash, role.name(), linkedCustomerId, displayName, enabled, passwordChangeRequired);
    }

    public void updatePassword(Long id, String passwordHash, boolean passwordChangeRequired) {
        sql.executeUpdate("""
                UPDATE auth_users
                   SET password_hash = ?, password_change_required = ?
                 WHERE id = ?
                """, passwordHash, passwordChangeRequired, id);
    }

    private AuthUser mapRow(ResultSet rs) throws SQLException {
        return new AuthUser(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                UserRole.valueOf(rs.getString("role")),
                getNullableLong(rs, "linked_customer_id"),
                rs.getString("linked_customer_no"),
                rs.getString("display_name"),
                rs.getBoolean("enabled"),
                rs.getBoolean("password_change_required"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
