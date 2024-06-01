package it.uniroma2.alessandro.enums;

public enum ProportionType {
    INCREMENT("INCREMENT"),
    NEW("NEW");

    private final String id;

    private ProportionType(String id) {
        this.id = id;
    }

    public static ProportionType fromString(String id) {
        for (ProportionType type : values()) {
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
