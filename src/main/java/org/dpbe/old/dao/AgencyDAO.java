package org.dpbe.old.dao;

import java.util.List;
import org.dpbe.domain.actor.Agency;
import org.dpbe.old.db.DBA;

public class AgencyDAO {

    public static void save(Agency a) {
        DBA.executeUpdate(
            "INSERT INTO agencies (channel_id, name, location, agency_number)"
            + " VALUES (?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE name=VALUES(name), location=VALUES(location)",
            a.getChannelId(), a.getName(), "", a.getAgencyNumber());
    }

    public static List<Agency> findAll() {
        return DBA.executeQuery(
            "SELECT channel_id, name, location, agency_number FROM agencies",
            rs -> new Agency(
                rs.getString("channel_id"),
                rs.getString("name"),
                rs.getString("location"),
                rs.getString("agency_number")));
    }
}