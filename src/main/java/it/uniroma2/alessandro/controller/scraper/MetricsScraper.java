package it.uniroma2.alessandro.controller.scraper;

import it.uniroma2.alessandro.model.Release;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Retrieves the metric information online
 */
public class MetricsScraper {
    private static final Logger logger = Logger.getLogger(MetricsScraper.class.getName());

    public static void scrapeAndComputeData(String projName, String projRepo){
        try {
            JiraScraper jiraScraper = new JiraScraper(projName);
            List<Release> jiraReleases = jiraScraper.getReleases();

            GitScraper gitScraper = new GitScraper(projRepo);
        } catch (IOException | URISyntaxException e) {
            logger.info(e.getMessage());
        }
    }
}
