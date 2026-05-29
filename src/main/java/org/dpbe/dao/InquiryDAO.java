package org.dpbe.dao;

import org.dpbe.db.DBA;
import org.dpbe.enums.InquiryStatus;
import org.dpbe.enums.InquiryType;
import org.dpbe.inquiry.Inquiry;
import java.util.List;

public class InquiryDAO {

    public static void save(Inquiry i) {
        String inquiryType = i.getInquiryType() != null ? i.getInquiryType().name() : null;
        String status = i.getStatus() != null ? i.getStatus().name() : null;
        DBA.executeUpdate(
            "INSERT INTO inquiries (inquiry_no, customer_name, inquiry_type, title, content,"
            + " attachment_file_name, attachment_file_size, answer_content, answered_at, status, created_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status),"
            + " answer_content=VALUES(answer_content), answered_at=VALUES(answered_at)",
            i.getInquiryNo(),
            i.getCustomerName(),
            inquiryType,
            i.getTitle(),
            i.getContent(),
            i.getAttachmentFileName(),
            i.getAttachmentFileSize(),
            i.getAnswerContent(),
            i.getAnsweredAt(),
            status,
            i.getReceivedAt());
    }

    public static List<Inquiry> findAll() {
        return findByCustomerName(null);
    }

    public static List<Inquiry> findByCustomerName(String customerName) {
        String sql = "SELECT inquiry_no, customer_name, inquiry_type, title, content,"
            + " attachment_file_name, attachment_file_size,"
            + " answer_content, answered_at, status, created_at FROM inquiries";
        if (customerName != null) sql += " WHERE customer_name=?";
        final String finalSql = sql;
        return customerName != null
            ? DBA.executeQuery(finalSql, rs -> mapRow(rs), customerName)
            : DBA.executeQuery(finalSql, rs -> mapRow(rs));
    }

    public static List<Inquiry> findByCustomerNameAndStatus(String customerName, InquiryStatus inquiryStatus) {
        return DBA.executeQuery(
            "SELECT inquiry_no, customer_name, inquiry_type, title, content,"
            + " attachment_file_name, attachment_file_size,"
            + " answer_content, answered_at, status, created_at"
            + " FROM inquiries WHERE customer_name=? AND status=?",
            rs -> mapRow(rs), customerName, inquiryStatus.name());
    }

    private static Inquiry mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Inquiry i = new Inquiry();
        i.setInquiryNo(rs.getString("inquiry_no"));
        i.setCustomerName(rs.getString("customer_name"));
        String it = rs.getString("inquiry_type");
        if (it != null) {
            try { i.setInquiryType(InquiryType.valueOf(it)); }
            catch (IllegalArgumentException ignored) {}
        }
        i.setTitle(rs.getString("title"));
        i.setContent(rs.getString("content"));
        i.setAttachmentFileName(rs.getString("attachment_file_name"));
        long fileSize = rs.getLong("attachment_file_size");
        if (!rs.wasNull()) i.setAttachmentFileSize(fileSize);
        i.setAnswerContent(rs.getString("answer_content"));
        java.sql.Timestamp aat = rs.getTimestamp("answered_at");
        if (aat != null) i.setAnsweredAt(aat.toLocalDateTime());
        String st = rs.getString("status");
        if (st != null) {
            try { i.setStatus(InquiryStatus.valueOf(st)); }
            catch (IllegalArgumentException ignored) {}
        }
        java.sql.Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) i.setReceivedAt(cat.toLocalDateTime());
        return i;
    }
}