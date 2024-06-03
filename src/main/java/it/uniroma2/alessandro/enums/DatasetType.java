package it.uniroma2.alessandro.enums;

public enum DatasetType {
    TRAINING("TRAINING"),
    TESTING("TESTING");

    private final String id;

    private DatasetType(String id) {
        this.id = id;
    }

    public static DatasetType fromString(String id) {
        for (DatasetType type : values()) {
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
