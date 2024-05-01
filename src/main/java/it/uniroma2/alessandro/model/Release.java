package it.uniroma2.alessandro.model;

import it.uniroma2.alessandro.exception.ReleaseNotFoundException;
import org.json.JSONArray;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Release {
    private final String releaseID;
    private final String releaseName;
    private final String releaseDateString;
    private final LocalDate releaseDateTime;
    private final List<Commit> commitList;

    public Release(String releaseID ,String releaseName, String releaseDateString) {
        if(releaseID == null || releaseName == null || releaseDateString == null){
            throw new IllegalArgumentException();
        }
        this.releaseID = releaseID;
        this.releaseName = releaseName;
        this.releaseDateString = releaseDateString;
        this.releaseDateTime = LocalDate.parse(releaseDateString);
        commitList = new ArrayList<>();
    }

    public String getReleaseID() {
        return releaseID;
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

    public List<Commit> getCommitList() {
        return commitList;
    }

    public void addCommit(Commit commit){
        commitList.add(commit);
    }

    /***
     * Finds the release which date is the nearest greater than the one specified
     * @param specificDate the date to compare with the release date
     * @param releases list of release to compare
     * @return the release with the closest greater date if exists, null otherwise
     */
    public static Release getReleaseAfterOrEqualDate(LocalDate specificDate, ReleaseList releases) {
        List<Release> releasesList = releases.getReleaseList();
        releasesList.sort(Comparator.comparing(Release::getReleaseDateTime));
        for (Release release : releasesList) {
            if (!release.getReleaseDateTime().isBefore(specificDate)) {
                return release;
            }
        }
        return null;
    }


    /***
     * Get from a JSon response from jira all the AV releases ordered by date
     * @param affectedVersionsArray JSon object containing the AVs
     * @param releasesList list of releases
     * @return list of AV releases ordered by date
     */
    public static List<Release> returnValidAffectedVersions(JSONArray affectedVersionsArray, ReleaseList releasesList) throws ReleaseNotFoundException {
        List<Release> existingAffectedVersions = new ArrayList<>();
        for (int i = 0; i < affectedVersionsArray.length(); i++) {
            String affectedVersionName = affectedVersionsArray.getJSONObject(i).get("name").toString();
            Release release = releasesList.getReleaseByName(affectedVersionName);
            existingAffectedVersions.add(release);
        }
        existingAffectedVersions.sort(Comparator.comparing(Release::getReleaseDateTime));
        return existingAffectedVersions;
    }
}
