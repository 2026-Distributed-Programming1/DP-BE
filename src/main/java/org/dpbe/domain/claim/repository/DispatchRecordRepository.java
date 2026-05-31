package org.dpbe.domain.claim.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.dpbe.domain.claim.entity.Dispatch;
import org.dpbe.domain.claim.entity.DispatchRecord;
import org.dpbe.domain.common.entity.Attachment;
import org.dpbe.domain.common.enums.DispatchRecordStatus;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

/**
 * 현장 출동 기록 리포지토리 (Spring 트랜잭션 통합 경로).
 * PK는 surrogate id. record_no는 INSERT 후 id에서 파생(DRC+%05d).
 * 단일 테이블 매핑은 기존 {@code DispatchRecordDAO}를 정답지로 따르되 id를 포함하고,
 * 사진은 1:N 별도 테이블(dispatch_photos)에 메타를 저장한다(실파일은 파일시스템).
 */
@Repository
public class DispatchRecordRepository {

    private static final String COLS =
            "id, record_no, dispatch_no, agent_name, police_required, towing_required,"
            + " notes, transmitted_at, status";

    private final SqlExecutor sql;

    public DispatchRecordRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 신규 출동 기록 저장 — INSERT 후 생성 id에서 record_no 파생. */
    public void save(DispatchRecord r, String agentName) {
        String dispatchNo = r.getDispatch() != null ? r.getDispatch().getDispatchNo() : null;
        String status     = r.getStatus() != null ? r.getStatus().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO dispatch_records (dispatch_no, agent_name, police_required,"
                + " towing_required, notes, transmitted_at, status)"
                + " VALUES (?,?,?,?,?,?,?)",
                dispatchNo, agentName, r.isPoliceRequired(), r.isTowingRequired(),
                r.getNotes(), r.getTransmittedAt(), status);
        r.setId(id);
        r.setRecordId("DRC" + String.format("%05d", id));
        sql.executeUpdate("UPDATE dispatch_records SET record_no=? WHERE id=?", r.getRecordId(), id);
    }

    /** 전송 결과 반영 (transmitted_at·status 갱신). */
    public void markTransmitted(DispatchRecord r) {
        String status = r.getStatus() != null ? r.getStatus().name() : null;
        sql.executeUpdate(
                "UPDATE dispatch_records SET transmitted_at=?, status=? WHERE record_no=?",
                r.getTransmittedAt(), status, r.getRecordId());
    }

    /** 사진 메타 1건 저장 (record_no FK). */
    public void savePhoto(String recordNo, Attachment photo) {
        sql.executeUpdate(
                "INSERT INTO dispatch_photos (record_no, file_name, file_path, file_size,"
                + " mime_type, uploaded_at) VALUES (?,?,?,?,?,?)",
                recordNo, photo.getFileName(), photo.getFilePath(), photo.getFileSize(),
                photo.getMimeType(), photo.getUploadedAt());
    }

    /** 한 기록의 사진 파일명 목록. */
    public List<String> findPhotoNames(String recordNo) {
        return sql.executeQuery(
                "SELECT file_name FROM dispatch_photos WHERE record_no=? ORDER BY id",
                rs -> rs.getString("file_name"), recordNo);
    }

    public List<DispatchRecord> findAll() {
        return sql.executeQuery("SELECT " + COLS + " FROM dispatch_records", this::mapRow);
    }

    public DispatchRecord findByRecordNo(String recordNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM dispatch_records WHERE record_no=?", this::mapRow, recordNo);
    }

    public DispatchRecord findByDispatchNo(String dispatchNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM dispatch_records WHERE dispatch_no=?", this::mapRow, dispatchNo);
    }

    private DispatchRecord mapRow(ResultSet rs) throws SQLException {
        String dno = rs.getString("dispatch_no");
        Dispatch dispatchShell = dno != null ? new Dispatch(dno, null, null) : null;
        DispatchRecord rec = new DispatchRecord(dispatchShell);
        rec.setId(rs.getLong("id"));
        rec.setRecordId(rs.getString("record_no"));
        rec.setPoliceRequired(rs.getBoolean("police_required"));
        rec.setTowingRequired(rs.getBoolean("towing_required"));
        String n = rs.getString("notes");
        if (n != null) rec.enterNotes(n);
        java.sql.Timestamp tat = rs.getTimestamp("transmitted_at");
        if (tat != null) rec.setTransmittedAt(tat.toLocalDateTime());
        String st = rs.getString("status");
        if (st != null) {
            try { rec.setStatus(DispatchRecordStatus.valueOf(st)); }
            catch (IllegalArgumentException ignored) {}
        }
        return rec;
    }
}