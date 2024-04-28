package it.uniroma2.alessandro.controller.scraper;

import it.uniroma2.alessandro.model.Release;
import it.uniroma2.alessandro.model.Ticket;
import it.uniroma2.alessandro.utilities.JsonUtility;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class JiraScraper {
    private final String projName;

    public JiraScraper(String projName) {
        this.projName = projName.toUpperCase();
    }

    public List<Release> getReleases() throws IOException, URISyntaxException {
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;

        List<Release> releases = new ArrayList<>();

        JSONObject json = JsonUtility.readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        // Create the list of releases
        for(int i = 0; i < versions.length(); i++) {
            String releaseName = "";
            String releaseDateString = "";
            JSONObject version = versions.getJSONObject(i);
            if(version.has("name")) releaseName = version.getString("name");
            if(version.has("releaseDate")) releaseDateString = version.getString("releaseDate");
            releases.add(new Release(releaseName, releaseDateString));
        }

        // Order temporally the list of releases
        releases.sort(Comparator.comparing(Release::getReleaseDateTime));

        return releases;
    }

    //todo: GetTicketId project
    public List<Ticket> getTickets() throws IOException, URISyntaxException {
        String url = "";
        List<Ticket> tickets = new ArrayList<>();

        return tickets;
    }
}
