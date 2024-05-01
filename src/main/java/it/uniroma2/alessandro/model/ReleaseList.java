package it.uniroma2.alessandro.model;

import it.uniroma2.alessandro.exception.ReleaseNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class ReleaseList {
    private static final Logger logger = Logger.getLogger(ReleaseList.class.getName());

    // Using maps to find a release having its release date or name
    private final HashMap<LocalDateTime, Release> releaseDateMap;
    private final HashMap<String, Release> releaseNamesMap;
    private final List<Release> releaseList;
    /**
     * Note that the order of releaseDates is not the same as releaseNames.
     * Use this array only to access the releaseDateMap.
     */
    private final ArrayList<LocalDateTime> releasesDates;

    /**
     * Note that the order of releaseNames is not the same as releaseDates.
     * Use this array only to access the releaseNamesMap.
     */
    private final ArrayList<String> releasesNames;

    public ReleaseList(){
        releaseDateMap = new HashMap<LocalDateTime, Release>();
        releaseNamesMap = new HashMap<String, Release>();
        releaseList = new ArrayList<>();
        releasesDates = new ArrayList<LocalDateTime>();
        releasesNames = new ArrayList<String>();
    }

    public ArrayList<String> getReleasesNames(){
        return releasesNames;
    }

    public ArrayList<LocalDateTime> getReleasesDates(){
        return releasesDates;
    }

    public void addRelease(Release release){
        LocalDate date = release.getReleaseDateTime();
        LocalDateTime dateTime = date.atStartOfDay();
        String releaseName = release.getReleaseName();
        if (!releasesDates.contains(dateTime) && !releasesNames.contains(releaseName)) {
            releasesDates.add(dateTime);
            releasesNames.add(releaseName);
            releaseList.add(release);
            releaseDateMap.put(dateTime, release);
            releaseNamesMap.put(releaseName, release);
        }
    }

    public Release getReleaseByName(String releaseName) throws ReleaseNotFoundException {
        if(releaseName.trim().contains(releaseName))
            return releaseNamesMap.get(releaseName);
        throw new ReleaseNotFoundException();
    }

    public Release getReleaseByDate(LocalDateTime releaseDate) throws ReleaseNotFoundException {
        if(releasesDates.contains(releaseDate))
            return releaseDateMap.get(releaseDate);
        throw new ReleaseNotFoundException();
    }

    public Release getReleaseByDate(String releaseDate) throws ReleaseNotFoundException {
        LocalDate date = LocalDate.parse(releaseDate);
        LocalDateTime dateTime = date.atStartOfDay();

        return getReleaseByDate(dateTime);
    }

    public List<Release> getReleaseList(){
        return releaseList;
    }

    public void sort(){
        Collections.sort(releasesNames);
        Collections.sort(releasesDates);
    }

}
