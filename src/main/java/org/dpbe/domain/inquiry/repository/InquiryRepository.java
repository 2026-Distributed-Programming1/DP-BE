package org.dpbe.domain.inquiry.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.common.enums.InquiryStatus;
import org.dpbe.domain.common.enums.InquiryType;
import org.dpbe.domain.inquiry.entity.Inquiry;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class InquiryRepository {

    private static final String COLS =
            "id, customer_id, customer_name, inquiry_type, title, content,"
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

    public List<Inquiry> findByCustomerId(Long customerId) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM inquiries WHERE customer_id=? ORDER BY id DESC",
                this::mapRow, customerId);
    }

    public List<Inquiry> findByCustomerIdAndStatus(Long customerId, String status) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM inquiries WHERE customer_id=? AND status=? ORDER BY id DESC",
                this::mapRow, customerId, status);
    }

    public List<Inquiry> findByCustomerNameAndStatus(String customerName, String status) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM inquiries WHERE customer_name=? AND status=? ORDER BY id DESC",
                this::mapRow, customerName, status);
    }

    public int countByFilters(Long customerId, String customerName, String status) {
        QueryParts query = buildFilterQuery("SELECT COUNT(*) AS cnt FROM inquiries", customerId, customerName, status);
        return sql.queryOne(query.sql(), rs -> rs.getInt("cnt"), query.params().toArray());
    }

    public List<Inquiry> findPageByFilters(
            Long customerId, String customerName, String status, int limit, int offset) {
        QueryParts query = buildFilterQuery("SELECT " + COLS + " FROM inquiries", customerId, customerName, status);
        List<Object> params = new ArrayList<>(query.params());
        params.add(limit);
        params.add(offset);
        return sql.executeQuery(query.sql() + " ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapRow, params.toArray());
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
                + " (customer_id, customer_name, inquiry_type, title, content,"
                + "  attachment_file_name, attachment_file_size, status, created_at)"
                + " VALUES (?,?,?,?,?,?,?,?,?)",
                inquiry.getCustomerId(), inquiry.getCustomerName(), inquiryType,
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
        long customerId = rs.getLong("customer_id");
        i.setCustomerId(rs.wasNull() ? null : customerId);
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

    private QueryParts buildFilterQuery(String selectSql, Long customerId, String customerName, String status) {
        StringBuilder query = new StringBuilder(selectSql);
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (customerId != null) {
            conditions.add("customer_id=?");
            params.add(customerId);
        }
        if (customerName != null && !customerName.isBlank()) {
            conditions.add("customer_name=?");
            params.add(customerName);
        }
        if (status != null && !status.isBlank()) {
            conditions.add("status=?");
            params.add(status);
        }
        if (!conditions.isEmpty()) {
            query.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        return new QueryParts(query.toString(), params);
    }

    private record QueryParts(String sql, List<Object> params) {
    }
}
