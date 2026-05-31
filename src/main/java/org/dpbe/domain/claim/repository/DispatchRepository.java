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
 * (dispatches 테이블 컬럼: dispatch_no, accident_no, status)
 */
@Repository
public class DispatchRepository {

    private static final String COLS = "id, dispatch_no, accident_no, status";

    private final SqlExecutor sql;

    public DispatchRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 신규 출동 저장 — INSERT 후 생성 id에서 dispatch_no 파생. */
    public void save(Dispatch d) {
        String accidentNo = d.getAccident() != null ? d.getAccident().getReportNo() : null;
        String status     = d.getStatus() != null ? d.getStatus().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO dispatches (accident_no, status) VALUES (?,?)",
                accidentNo, status);
        d.setId(id);
        d.setDispatchNo("DSP" + String.format("%05d", id));
        sql.executeUpdate("UPDATE dispatches SET dispatch_no=? WHERE id=?", d.getDispatchNo(), id);
    }

    public List<Dispatch> findAll() {
        return sql.executeQuery("SELECT " + COLS + " FROM dispatches", this::mapRow);
    }

    public Dispatch findByDispatchNo(String dispatchNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM dispatches WHERE dispatch_no=?", this::mapRow, dispatchNo);
    }

    public Dispatch findByAccidentNo(String accidentNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM dispatches WHERE accident_no=?", this::mapRow, accidentNo);
    }

    private Dispatch mapRow(ResultSet rs) throws SQLException {
        String accNo = rs.getString("accident_no");
        AccidentReport shell = accNo != null ? new AccidentReport(accNo) : null;
        String st = rs.getString("status");
        DispatchStatus status = DispatchStatus.REQUESTED;
        if (st != null) {
            try { status = DispatchStatus.valueOf(st); }
            catch (IllegalArgumentException ignored) {}
        }
        Dispatch d = new Dispatch(rs.getString("dispatch_no"), shell, status);
        d.setId(rs.getLong("id"));
        return d;
    }
}