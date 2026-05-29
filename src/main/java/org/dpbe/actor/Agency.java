package org.dpbe.actor;

/**
 * 대리점 (Agency)
 * SalesChannel 상속
 */
public class Agency extends SalesChannel {

    private String agencyNumber;

    public Agency(String channelId, String name, String location, String agencyNumber) {
        super(channelId, name, location);
        this.agencyNumber = agencyNumber;
    }

    public String getAgencyNumber() { return agencyNumber; }
    public void setAgencyNumber(String agencyNumber) { this.agencyNumber = agencyNumber; }
}
