package it.uniroma2.alessandro.controller.scraper;

import it.uniroma2.alessandro.controller.processor.MetricsProcessor;
import it.uniroma2.alessandro.exception.ReleaseNotFoundException;
import it.uniroma2.alessandro.model.Commit;
import it.uniroma2.alessandro.model.ProjectClass;
import it.uniroma2.alessandro.model.Release;
import it.uniroma2.alessandro.model.Ticket;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Retrieves the metric information online
 */
public class MetricsScraper {
    private static final Logger logger = Logger.getLogger(MetricsScraper.class.getName());
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MetricsScraper.class);

    public static void scrapeData(String projName, String projRepoUrl){
        try {
            JiraScraper jiraScraper = new JiraScraper(projName);
            logger.info("Scraping releases of " + projName + " project...\n");
            List<Release> jiraReleases = jiraScraper.scrapeReleases();

            logger.info("Cloning repository of " + projName + " project...\n");
            GitScraper gitScraper = new GitScraper(projName, projRepoUrl);

            logger.info("Scraping commits of " + projName + " project...\n");
            List<Commit> commitList = gitScraper.scrapeCommits(jiraReleases);

            logger.info("Scraping tickets of " + projName + " project...\n");
            List<Ticket> ticketList = jiraScraper.scrapeTickets(jiraReleases);

            logger.info("Filtering commits of " + projName + " project...\n");
            List<Commit> ticketedCommitList = gitScraper.filterCommits(commitList, ticketList);

            // If a ticket has no commits it means it isn't solved, so we don't care about it
            ticketList.removeIf(ticket -> ticket.getCommitList().isEmpty());

            logger.info("Extracting touched classes from " + projName + " project...\n");
            List<ProjectClass> classList = gitScraper.extractProjectClasses(jiraReleases, ticketList, commitList);

            logger.info("Extracting metrics from " + projName + " project...\n");
            MetricsProcessor metricsProcessor = new MetricsProcessor(ticketedCommitList, classList, gitScraper);
            metricsProcessor.processMetrics();

            logger.info("Building training and test sets from " + projName + " project...\n");

        } catch (IOException | URISyntaxException | GitAPIException | ReleaseNotFoundException e) {
            logger.info(e.toString());
        }
    }
}
