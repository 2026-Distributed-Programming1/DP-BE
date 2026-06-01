package org.dpbe.domain.claim.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.claim.entity.AccidentReport;
import org.dpbe.domain.claim.entity.Dispatch;
import org.dpbe.domain.common.enums.DispatchStatus;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

/**
 * 현장 출동 리포지토리 (Spring 트랜잭션 통합 경로).
 * PK는 surrogate id. dispatch_no는 INSERT 후 id에서 파생(DSP+%05d).
 * 매핑은 기존 {@code DispatchDAO}를 정답지로 따르되 id를 포함한다.
 * (dispatches 테이블 컬럼: id, accident_id, status)
 */
@Repository
public class DispatchRepository {

    private static final String COLS = "id, accident_id, status";

    private final SqlExecutor sql;

    public DispatchRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 신규 출동 저장 — INSERT 후 생성 id에서 dispatch_no 파생. */
    public void save(Dispatch d) {
        Long accidentId  = d.getAccident() != null ? d.getAccident().getId() : null;
        String status     = d.getStatus() != null ? d.getStatus().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO dispatches (accident_id, status) VALUES (?,?)",
                accidentId, status);
        d.setId(id);
        d.setDispatchNo("DSP" + String.format("%05d", id));
    }

    public List<Dispatch> findAll() {
        return sql.executeQuery("SELECT " + COLS + " FROM dispatches", this::mapRow);
    }

    public Dispatch findById(Long id) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM dispatches WHERE id=?", this::mapRow, id);
    }

    public Dispatch findByAccidentNo(String accidentNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM dispatches WHERE accident_id=?", this::mapRow, parseId(accidentNo));
    }

    private Dispatch mapRow(ResultSet rs) throws SQLException {
        long accidentId = rs.getLong("accident_id");
        AccidentReport shell = !rs.wasNull()
                ? new AccidentReport("ACC" + String.format("%05d", accidentId))
                : null;
        if (shell != null) shell.setId(accidentId);
        String st = rs.getString("status");
        DispatchStatus status = DispatchStatus.REQUESTED;
        if (st != null) {
            try { status = DispatchStatus.valueOf(st); }
            catch (IllegalArgumentException ignored) {}
        }
        Dispatch d = new Dispatch("DSP" + String.format("%05d", rs.getLong("id")), shell, status);
        d.setId(rs.getLong("id"));
        return d;
    }

    private Long parseId(String businessNo) {
        return Long.parseLong(businessNo.replaceAll("\\D", ""));
    }
}
