package it.uniroma2.alessandro.controller.scraper;

import it.uniroma2.alessandro.controller.processor.MetricsProcessor;
import it.uniroma2.alessandro.exception.ReleaseNotFoundException;
import it.uniroma2.alessandro.model.Commit;
import it.uniroma2.alessandro.model.ProjectClass;
import it.uniroma2.alessandro.model.Release;
import it.uniroma2.alessandro.model.Ticket;
import it.uniroma2.alessandro.utilities.CustomLogger;
import it.uniroma2.alessandro.utilities.ReportUtility;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Retrieves the metric information online
 */
public class MetricsScraper {
    private final Logger logger;

    public MetricsScraper(){
        logger = Logger.getLogger(MetricsScraper.class.getName());
    }

    public void scrapeData(String projName, String projRepoUrl){
        try {
            String loggerProjString = projName + " project...\n";
            logger.info("Starting\n");

            JiraScraper jiraScraper = new JiraScraper(projName);
            logger.info("Scraping releases of " + loggerProjString);
            List<Release> jiraReleases = jiraScraper.scrapeReleases();

            logger.info("Cloning repository of " + loggerProjString);
            GitScraper gitScraper = new GitScraper(projName, projRepoUrl);

            logger.info("Scraping commits of " + loggerProjString);
            List<Commit> commitList = gitScraper.scrapeCommits(jiraReleases);

            logger.info("Scraping tickets of " + loggerProjString);
            List<Ticket> ticketList = jiraScraper.scrapeTickets(jiraReleases);

            logger.info("Filtering commits of " + loggerProjString);
            List<Commit> ticketedCommitList = gitScraper.filterCommits(commitList, ticketList);

            // If a ticket has no commits it means it isn't solved, so we don't care about it
            ticketList.removeIf(ticket -> ticket.getCommitList().isEmpty());

            logger.info("Extracting touched classes from " + loggerProjString);
            List<ProjectClass> classList = gitScraper.extractProjectClasses(jiraReleases, ticketList, commitList);

            logger.info("Extracting metrics from " + loggerProjString);
            MetricsProcessor metricsProcessor = new MetricsProcessor(ticketedCommitList, classList, gitScraper);
            metricsProcessor.processMetrics();

            logger.info("Reporting results from " + loggerProjString);
            ReportUtility.writeOnReportFiles(projName, jiraReleases, ticketList, commitList, ticketedCommitList);

            logger.info("Building training and test sets from " + loggerProjString);
            // Consider only the first half of releases
            LocalDate lastReleaseDate = jiraReleases.get(jiraReleases.size()/2).getReleaseDateTime();
            List<Release> firstHalfReleases = jiraReleases.stream()
                    .filter(release -> !release.getReleaseDateTime().isAfter(lastReleaseDate))
                    .toList();

            for (Release release : firstHalfReleases) {
                //todo: go on from here
            }

            logger.info("Finished\n");

        } catch (IOException | URISyntaxException | GitAPIException | ReleaseNotFoundException e) {
            logger.info(e.toString());
        }
    }
}
