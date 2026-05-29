package org.dpbe.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Database Access (DBA)
 *
 * JDBC 연결을 관리하고 SQL 실행을 담당하는 클래스이다.
 * DAO -> DBA -> DB 계층에서 중간 계층 역할을 한다.
 *
 * 사용법:
 *   DBA.executeUpdate("INSERT INTO customers VALUES (?,?,?)", id, name, phone);
 *   List<Customer> list = DBA.executeQuery("SELECT * FROM customers", rs -> {
 *       Customer c = new Customer(...);
 *       return c;
 *   });
 */
public class DBA {

    private static final HikariDataSource DATA_SOURCE;

    /** 트랜잭션 전용 커넥션 (스레드당 1개) */
    private static final ThreadLocal<Connection> TX_CONN = new ThreadLocal<>();

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/insurance_db"
                + "?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true"
                + "&characterEncoding=UTF-8");
        config.setUsername("admin");
        config.setPassword("1234");
        config.setMaximumPoolSize(10);   // 풀이 유지할 최대 커넥션 수
        config.setMinimumIdle(2);        // 풀이 유지할 최소 유휴 커넥션 수
        config.setConnectionTimeout(30_000);  // 커넥션 획득 대기 최대 시간 (ms) — 초과 시 예외
        config.setIdleTimeout(600_000);       // 유휴 커넥션이 풀에서 제거되기까지의 시간 (ms)
        config.setMaxLifetime(1_800_000);     // 커넥션 최대 수명 (ms) — 만료 시 풀에서 교체
        DATA_SOURCE = new HikariDataSource(config);
    }

    private DBA() {}

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    // ── 트랜잭션 API ──────────────────────────────────────────────────────────

    public static void beginTransaction() {
        try {
            Connection conn = DATA_SOURCE.getConnection();
            conn.setAutoCommit(false);
            TX_CONN.set(conn);
        } catch (SQLException e) {
            System.err.println("[DBA] beginTransaction 오류: " + e.getMessage());
        }
    }

    public static void commit() {
        Connection conn = TX_CONN.get();
        if (conn == null) return;
        try {
            conn.commit();
        } catch (SQLException e) {
            System.err.println("[DBA] commit 오류: " + e.getMessage());
        } finally {
            closeTx(conn);
        }
    }

    public static void rollback() {
        Connection conn = TX_CONN.get();
        if (conn == null) return;
        try {
            conn.rollback();
        } catch (SQLException e) {
            System.err.println("[DBA] rollback 오류: " + e.getMessage());
        } finally {
            closeTx(conn);
        }
    }

    private static void closeTx(Connection conn) {
        try {
            conn.setAutoCommit(true);
            conn.close();
        } catch (SQLException ignored) {
        } finally {
            TX_CONN.remove();
        }
    }

    /** TX_CONN이 있으면 재사용, 없으면 풀에서 새 커넥션 획득 */
    private static Connection acquireConnection() throws SQLException {
        Connection tx = TX_CONN.get();
        return (tx != null) ? tx : DATA_SOURCE.getConnection();
    }

    /** TX 중이면 커넥션을 닫지 않음 (commit/rollback이 담당) */
    private static void releaseConnection(Connection conn) {
        if (TX_CONN.get() == null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** ResultSet → 객체 매핑 함수형 인터페이스 */
    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /**
     * INSERT / UPDATE / DELETE 실행
     * @return 영향받은 행 수
     */
    public static int executeUpdate(String sql, Object... params) {
        Connection con = null;
        try {
            con = acquireConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                setParams(ps, params);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[DBA] executeUpdate 오류: " + e.getMessage());
            return 0;
        } finally {
            if (con != null) releaseConnection(con);
        }
    }

    /**
     * SELECT 실행
     * @param mapper  ResultSet 한 행을 객체로 변환하는 함수
     * @return 결과 목록
     */
    public static <T> List<T> executeQuery(String sql, ResultSetMapper<T> mapper, Object... params) {
        List<T> result = new ArrayList<>();
        Connection con = null;
        try {
            con = acquireConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        T row = mapper.map(rs);
                        if (row != null) result.add(row);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DBA] executeQuery 오류: " + e.getMessage());
        } finally {
            if (con != null) releaseConnection(con);
        }
        return result;
    }

    /**
     * 단일 행 SELECT (없으면 null)
     */
    public static <T> T queryOne(String sql, ResultSetMapper<T> mapper, Object... params) {
        List<T> list = executeQuery(sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 존재 여부 확인
     */
    public static boolean exists(String sql, Object... params) {
        Connection con = null;
        try {
            con = acquireConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.err.println("[DBA] exists 오류: " + e.getMessage());
            return false;
        } finally {
            if (con != null) releaseConnection(con);
        }
    }

    private static void setParams(PreparedStatement ps, Object[] params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null)                          ps.setNull(i + 1, Types.NULL);
            else if (p instanceof String s)         ps.setString(i + 1, s);
            else if (p instanceof Integer n)        ps.setInt(i + 1, n);
            else if (p instanceof Long n)           ps.setLong(i + 1, n);
            else if (p instanceof Double d)         ps.setDouble(i + 1, d);
            else if (p instanceof Boolean b)        ps.setBoolean(i + 1, b);
            else if (p instanceof java.time.LocalDate d)
                ps.setDate(i + 1, Date.valueOf(d));
            else if (p instanceof java.time.LocalDateTime dt)
                ps.setTimestamp(i + 1, Timestamp.valueOf(dt));
            else                                    ps.setObject(i + 1, p);
        }
    }
}
