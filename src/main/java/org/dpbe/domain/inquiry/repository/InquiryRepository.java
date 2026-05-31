package org.dpbe.domain.inquiry.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.common.enums.InquiryStatus;
import org.dpbe.domain.common.enums.InquiryType;
import org.dpbe.domain.inquiry.entity.Inquiry;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class InquiryRepository {

    private static final String COLS =
            "id, customer_name, inquiry_type, title, content,"
            + " attachment_file_name, attachment_file_size,"
            + " answer_content, answered_at, status, created_at";

    private final SqlExecutor sql;

    public InquiryRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<Inquiry> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM inquiries ORDER BY id DESC", this::mapRow);
    }

    public List<Inquiry> findByCustomerName(String customerName) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM inquiries WHERE customer_name=? ORDER BY id DESC",
                this::mapRow, customerName);
    }

    public List<Inquiry> findByStatus(String status) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM inquiries WHERE status=? ORDER BY id DESC",
                this::mapRow, status);
    }

    public List<Inquiry> findByCustomerNameAndStatus(String customerName, String status) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM inquiries WHERE customer_name=? AND status=? ORDER BY id DESC",
                this::mapRow, customerName, status);
    }

    public Inquiry findById(Long id) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM inquiries WHERE id=?", this::mapRow, id);
    }

    /** INSERT → id 회수 → inquiry_no 파생 UPDATE */
    public void save(Inquiry inquiry) {
        String inquiryType = inquiry.getInquiryType() != null ? inquiry.getInquiryType().name() : null;
        String status = inquiry.getStatus() != null ? inquiry.getStatus().name() : null;
        long id = sql.executeInsertReturningKey(
                "INSERT INTO inquiries"
                + " (customer_name, inquiry_type, title, content,"
                + "  attachment_file_name, attachment_file_size, status, created_at)"
                + " VALUES (?,?,?,?,?,?,?,?)",
                inquiry.getCustomerName(), inquiryType,
                inquiry.getTitle(), inquiry.getContent(),
                inquiry.getAttachmentFileName(), inquiry.getAttachmentFileSize(),
                status, inquiry.getReceivedAt());
        inquiry.setId(id);
        inquiry.setInquiryNo("INQ" + String.format("%05d", id));
    }

    /** 답변 등록 — answer_content, answered_at, status 갱신 */
    public void updateAnswer(Inquiry inquiry) {
        String status = inquiry.getStatus() != null ? inquiry.getStatus().name() : null;
        sql.executeUpdate(
                "UPDATE inquiries SET answer_content=?, answered_at=?, status=? WHERE id=?",
                inquiry.getAnswerContent(), inquiry.getAnsweredAt(), status, inquiry.getId());
    }

    private Inquiry mapRow(ResultSet rs) throws SQLException {
        Inquiry i = new Inquiry();
        i.setId(rs.getLong("id"));
        i.setInquiryNo("INQ" + String.format("%05d", rs.getLong("id")));
        i.setCustomerName(rs.getString("customer_name"));
        String it = rs.getString("inquiry_type");
        if (it != null) { try { i.setInquiryType(InquiryType.valueOf(it)); } catch (IllegalArgumentException ignored) {} }
        i.setTitle(rs.getString("title"));
        i.setContent(rs.getString("content"));
        i.setAttachmentFileName(rs.getString("attachment_file_name"));
        long fs = rs.getLong("attachment_file_size");
        if (!rs.wasNull()) i.setAttachmentFileSize(fs);
        i.setAnswerContent(rs.getString("answer_content"));
        java.sql.Timestamp aat = rs.getTimestamp("answered_at");
        if (aat != null) i.setAnsweredAt(aat.toLocalDateTime());
        String st = rs.getString("status");
        if (st != null) { try { i.setStatus(InquiryStatus.valueOf(st)); } catch (IllegalArgumentException ignored) {} }
        java.sql.Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) i.setReceivedAt(cat.toLocalDateTime());
        return i;
    }
}