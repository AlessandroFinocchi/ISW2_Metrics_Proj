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
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Retrieves the metric information online
 */
public class MetricsScraper {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MetricsScraper.class);
    private final Logger logger;

    public MetricsScraper(){
        logger = Logger.getLogger(MetricsScraper.class.getName());
    }

    public void scrapeData(String projName, String projRepoUrl){
        try {
            String loggerString;
            String ProjString = projName + " project...\n";
            logger.info("Starting\n");

            JiraScraper jiraScraper = new JiraScraper(projName);
            loggerString = "Scraping releases of " + ProjString;
            logger.info(loggerString);
            List<Release> jiraReleases = jiraScraper.scrapeReleases();

            loggerString = "Cloning repository of " + ProjString;
            logger.info(loggerString);
            GitScraper gitScraper = new GitScraper(projName, projRepoUrl);

            loggerString = "Scraping commits of " + ProjString;
            logger.info(loggerString);
            List<Commit> commitList = gitScraper.scrapeCommits(jiraReleases);

            loggerString = "Scraping tickets of " + ProjString;
            logger.info(loggerString);
            List<Ticket> ticketList = jiraScraper.scrapeTickets(jiraReleases);

            loggerString = "Filtering commits of " + ProjString;
            logger.info(loggerString);
            List<Commit> ticketedCommitList = gitScraper.filterCommits(commitList, ticketList);

            // If a ticket has no commits it means it isn't solved, so we don't care about it
            ticketList.removeIf(ticket -> ticket.getCommitList().isEmpty());

            loggerString = "Extracting touched classes from " + ProjString;
            logger.info(loggerString);
            List<ProjectClass> classList = gitScraper.extractProjectClasses(jiraReleases, ticketList, commitList);

            loggerString = "Extracting metrics from " + ProjString;
            logger.info(loggerString);
            MetricsProcessor metricsProcessor = new MetricsProcessor(ticketedCommitList, classList, gitScraper);
            metricsProcessor.processMetrics();

            loggerString = "Reporting results from " + ProjString;
            logger.info(loggerString);
            ReportUtility.writeOnReportFiles(projName, jiraReleases, ticketList, commitList, ticketedCommitList);

            loggerString = "Building training and test sets from " + ProjString;
            logger.info(loggerString);
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
