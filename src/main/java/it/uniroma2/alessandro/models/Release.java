package it.uniroma2.alessandro.models;

import it.uniroma2.alessandro.controllers.scrapers.GitScraper;
import it.uniroma2.alessandro.exceptions.ReleaseNotFoundException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONArray;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Release {
    private int numericID;
    private final String releaseID;
    private final String releaseName;
    private final String releaseDateString;
    private final LocalDate releaseDateTime;
    private final List<Commit> commitList;

    public Release(String releaseID ,String releaseName, String releaseDateString) {
        if(releaseID == null || releaseName == null || releaseDateString == null){
            throw new IllegalArgumentException();
        }
        this.numericID = 0;
        this.releaseID = releaseID;
        this.releaseName = releaseName;
        this.releaseDateString = releaseDateString;
        this.releaseDateTime = LocalDate.parse(releaseDateString);
        commitList = new ArrayList<>();
    }

    public Release(GitScraper gitScraper, String releaseID , String releaseName, String releaseDateString) throws GitAPIException, ReleaseNotFoundException, IOException {
        if(releaseDateString == null){
            releaseDateString = String.valueOf(gitScraper.getReleaseDateFromGithub(releaseName));
        }
        this.numericID = 0;
        this.releaseID = releaseID;
        this.releaseName = releaseName;
        this.releaseDateString = releaseDateString;
        this.releaseDateTime = LocalDate.parse(releaseDateString);
        commitList = new ArrayList<>();
    }

    public void setNumericID(int numericID){
        if(this.numericID == 0)
            this.numericID = numericID;
    }

    public int getNumericID() {
        return numericID;
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
    public static Release getReleaseAfterOrEqualDate(LocalDate specificDate, List<Release> releases) {
        releases.sort(Comparator.comparing(Release::getReleaseDateTime));
        for (Release release : releases) {
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
    public static List<Release> getValidAffectedVersions(JSONArray affectedVersionsArray, List<Release> releasesList) {
        List<Release> existingAffectedVersions = new ArrayList<>();
        for (int i = 0; i < affectedVersionsArray.length(); i++) {
            String affectedVersionName = affectedVersionsArray.getJSONObject(i).get("name").toString();
            Release release = getReleaseByName(releasesList, affectedVersionName);

            // If release is null it means that is not in the list, and since releases are deleted only when they have
            // no commits, it means that the release had no commits and thus it's not interesting, so we can ignore it
            if(release != null)
               existingAffectedVersions.add(release);
        }
        existingAffectedVersions.sort(Comparator.comparing(Release::getReleaseDateTime));
        return existingAffectedVersions;
    }

    /***
     * Finds a release in a list by its name
     * @param releaseList list of releases
     * @param releaseName name of the release
     * @return the release named as wanted if it exists, null otherwise
     */
    private static Release getReleaseByName(List<Release> releaseList, String releaseName) {
        for (Release release : releaseList) {
            if (Objects.equals(releaseName, release.getReleaseName())) return release;
        }
        return null;    }
}

