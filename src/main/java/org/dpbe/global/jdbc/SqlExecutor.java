package org.dpbe.global.jdbc;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

/**
 * Spring 트랜잭션과 통합되는 JDBC 실행 헬퍼.
 *
 * 커넥션은 {@link DataSourceUtils}를 통해 Spring DataSource에서 획득한다.
 * → {@code @Transactional}이 연 트랜잭션 커넥션을 그대로 이어받아 사용하므로
 *   선언적 트랜잭션이 정상 동작한다.
 *
 * SQLException은 런타임 예외로 변환하여 던진다(트랜잭션 롤백 유도).
 */
@Component
public class SqlExecutor {

    private final DataSource dataSource;

    public SqlExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** ResultSet 한 행 → 객체 매핑 */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public int executeUpdate(String sql, Object... params) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("[SqlExecutor] update 실패: " + e.getMessage(), e);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    /**
     * INSERT 후 DB가 생성한 AUTO_INCREMENT 키를 반환한다.
     * surrogate-PK(id) 방식에서 업무번호를 id로부터 파생하기 위해 사용한다.
     */
    public long executeInsertReturningKey(String sql, Object... params) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new RuntimeException("[SqlExecutor] 생성키를 반환받지 못했습니다.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("[SqlExecutor] insert 실패: " + e.getMessage(), e);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    public <T> List<T> executeQuery(String sql, RowMapper<T> mapper, Object... params) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    T row = mapper.map(rs);
                    if (row != null) result.add(row);
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("[SqlExecutor] query 실패: " + e.getMessage(), e);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = executeQuery(sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    private void bind(PreparedStatement ps, Object[] params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null)                              ps.setNull(i + 1, Types.NULL);
            else if (p instanceof String s)             ps.setString(i + 1, s);
            else if (p instanceof Integer n)            ps.setInt(i + 1, n);
            else if (p instanceof Long n)               ps.setLong(i + 1, n);
            else if (p instanceof Double d)             ps.setDouble(i + 1, d);
            else if (p instanceof Boolean b)            ps.setBoolean(i + 1, b);
            else if (p instanceof java.time.LocalDate d)
                ps.setDate(i + 1, Date.valueOf(d));
            else if (p instanceof java.time.LocalDateTime dt)
                ps.setTimestamp(i + 1, Timestamp.valueOf(dt));
            else                                        ps.setObject(i + 1, p);
        }
    }
}
