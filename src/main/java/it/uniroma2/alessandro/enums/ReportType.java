package it.uniroma2.alessandro.enums;

public enum ReportType {
    RELEASES("/Releases"),
    TICKETS("/Tickets"),
    COMMITS("/Commits"),
    SUMMARY("/Summary");

    private final String id;

    private ReportType(String id) {
        this.id = id;
    }

    public static ReportType fromString(String id) {
        for (ReportType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }
}
