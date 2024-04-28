package it.uniroma2.alessandro.model;

import java.time.LocalDate;

public class Release {
    private final String releaseName;
    private final String releaseDateString;
    private final LocalDate releaseDateTime;

    public Release(String releaseName, String releaseDateString) {
        this.releaseName = releaseName;
        this.releaseDateString = releaseDateString;
        this.releaseDateTime = LocalDate.parse(releaseDateString);
    }

    public String getReleaseName() {
        return releaseName;
    }
    public String getReleaseDateString() {
        return releaseDateString;
    }

    public LocalDate getReleaseDateTime(){
        return releaseDateTime;
    }
}
