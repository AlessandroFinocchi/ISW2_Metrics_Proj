package it.uniroma2.alessandro.controllers.scrapers;

import it.uniroma2.alessandro.exceptions.ReleaseNotFoundException;
import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.models.Ticket;
import it.uniroma2.alessandro.utilities.JsonUtility;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import static it.uniroma2.alessandro.utilities.JsonUtility.readJsonFromUrl;

public class JiraScraper {
    private final Logger logger = Logger.getLogger(MetricsScraper.class.getName());
    private final String projName;

    public JiraScraper(String projName) {
        this.projName = projName.toUpperCase();
    }

    public List<Release> scrapeReleases(GitScraper gitScraper) throws IOException, URISyntaxException, IllegalArgumentException, GitAPIException {
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        int i;

        List<Release> releases = new ArrayList<>();

        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        // Create the list of releases
        for(i = 0; i < versions.length(); i++) {
            String releaseID = null;
            String releaseName = null;
            String releaseDateString = null;
            JSONObject version = versions.getJSONObject(i);
            if(version.has("id")) releaseID = version.getString("id");
            if(version.has("name")) releaseName = version.getString("name");
            if(version.has("releaseDate")) releaseDateString = version.getString("releaseDate");
            try {
                Release newRelease = new Release(gitScraper, releaseID, releaseName, releaseDateString);
                releases.add(newRelease);
            }
            catch(ReleaseNotFoundException e){
                String loggerString = "Date not found for release with tag: " + releaseName;
                logger.info(loggerString);
            }
        }

        // Order temporally the list of releases
        releases.sort(Comparator.comparing(Release::getReleaseDateTime));

        // Set the last release
        gitScraper.setLastRelease(releases.getLast());

        return releases;
    }

    public List<Ticket> scrapeTickets(List<Release> releasesList)
            throws Exception {
        int total;
        int j;
        int i = 0;
        List<Ticket> ticketList = new ArrayList<>();

        do {
            // Get 1000 tickets per while cycle
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + this.projName + "%22AND%22issueType%22=%22Bug%22AND" +
                    "(%22status%22=%22Closed%22OR%22status%22=%22Resolved%22)" +
                    "AND%22resolution%22=%22Fixed%22&fields=key,versions,created,resolutiondate&startAt="
                    + i + "&maxResults=" + j;
            JSONObject json = JsonUtility.readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");

            //Iterate through each jira ticket
            for (; i < total && i < j; i++) {
                // Key is the name of the issue ticketed, like "BOOKKEEPER-1105"
                String key = issues.getJSONObject(i % 1000).get("key").toString();

                // Get the creation and resolution date
                JSONObject fields = issues.getJSONObject(i%1000).getJSONObject("fields");
                String creationDateString = fields.get("created").toString();
                String resolutionDateString = fields.get("resolutiondate").toString();
                LocalDate creationDate = LocalDate.parse(creationDateString.substring(0,10));
                LocalDate resolutionDate = LocalDate.parse(resolutionDateString.substring(0,10));

                // Get the versions affected by the issue
                JSONArray affectedVersionsArray = fields.getJSONArray("versions");

                // Get the OV of the issue
                Release openingVersion = Release.getReleaseAfterOrEqualDate(creationDate, releasesList);

                // Get the FV of the issue
                Release fixedVersion = Release.getReleaseAfterOrEqualDate(resolutionDate, releasesList);

                // Get the list of AV ordered by date
                List<Release> affectedVersionsList = Release.getValidAffectedVersions(affectedVersionsArray, releasesList);

                if( // If there are no OV or FV, ticket isn't needed
                    openingVersion == null || fixedVersion == null ||
                    // Check consistency of OV and FV dates: OV<=FV
                    openingVersion.getReleaseDateTime().isAfter(fixedVersion.getReleaseDateTime()) ||
                    // If there are AVs specified, check their consistency too
                    (!affectedVersionsList.isEmpty() &&
                        // Check that AV1<=OV: if the first OV is before the AV1, ticket is inconsistent, and thus isn't needed
                        (openingVersion.getReleaseDateTime().isBefore(affectedVersionsList.getFirst().getReleaseDateTime()) ||
                        // Check that AVN<FV: if the last AV is after the FV, ticket is inconsistent, and thus isn't needed
                        !fixedVersion.getReleaseDateTime().isAfter(affectedVersionsList.getLast().getReleaseDateTime()))
                    )
                ) continue;

                // Since the list of AVs is ordered, we already know that AV1 < AVN, thus at this point we got
                // the whole inequality chain consistent, that's to say: OV < AV1 < AVN < FV
                ticketList.add(new Ticket(key, creationDate, resolutionDate, openingVersion, fixedVersion, affectedVersionsList));
            }
        } while (i < total);

        // Sort tickets by resolution date
        ticketList.sort(Comparator.comparing(Ticket::getResolutionDate));

        //todo: è giusto farlo qua???

        // Adjust the infos of the tickets setting their IVs with proportion
        List<Ticket> proportionedTicketsList = Ticket.proportionTickets(ticketList, releasesList, projName);

        proportionedTicketsList.sort(Comparator.comparing(Ticket::getResolutionDate));

        return proportionedTicketsList;
    }
}
