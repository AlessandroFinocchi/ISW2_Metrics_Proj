package it.uniroma2.alessandro.controllers.scrapers;

import it.uniroma2.alessandro.controllers.processors.MetricsProcessor;
import it.uniroma2.alessandro.exceptions.IncorrectProportionException;
import it.uniroma2.alessandro.exceptions.ReleaseNotFoundException;
import it.uniroma2.alessandro.models.Commit;
import it.uniroma2.alessandro.models.ProjectClass;
import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.models.Ticket;
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
            String loggerString;
            String projString = projName + " project...\n";
            logger.info("Starting\n");

            JiraScraper jiraScraper = new JiraScraper(projName);
            loggerString = "Scraping releases of " + projString;
            logger.info(loggerString);
            List<Release> jiraReleases = jiraScraper.scrapeReleases();

            loggerString = "Cloning repository of " + projString;
            logger.info(loggerString);
            GitScraper gitScraper = new GitScraper(projName, projRepoUrl);

            loggerString = "Scraping commits of " + projString;
            logger.info(loggerString);
            List<Commit> commitList = gitScraper.scrapeCommits(jiraReleases);

            loggerString = "Scraping tickets of " + projString;
            logger.info(loggerString);
            List<Ticket> ticketList = jiraScraper.scrapeTickets(jiraReleases);

            loggerString = "Filtering commits of " + projString;
            logger.info(loggerString);
            List<Commit> ticketedCommitList = gitScraper.filterCommits(commitList, ticketList);

            // If a ticket has no commits it means it isn't solved, so we don't care about it
            ticketList.removeIf(ticket -> ticket.getCommitList().isEmpty());

            loggerString = "Extracting touched classes from " + projString;
            logger.info(loggerString);
            List<ProjectClass> classList = gitScraper.extractProjectClasses(jiraReleases, ticketList, commitList);

            loggerString = "Extracting metrics from " + projString;
            logger.info(loggerString);
            MetricsProcessor metricsProcessor = new MetricsProcessor(ticketedCommitList, classList, gitScraper);
            metricsProcessor.processMetrics();

            // todo:CANNOT WORK UNTIL FINISH PROPORTION
            loggerString = "Reporting results from " + projString;
            logger.info(loggerString);
            ReportUtility.writeOnReportFiles(projName, jiraReleases, ticketList, commitList, ticketedCommitList);

            loggerString = "Building training and test sets from " + projString;
            logger.info(loggerString);
            // Consider only the first half of releases
            LocalDate lastReleaseDate = jiraReleases.get(jiraReleases.size()/2).getReleaseDateTime();
            List<Release> firstHalfReleases = jiraReleases.stream()
                    .filter(release -> !release.getReleaseDateTime().isAfter(lastReleaseDate))
                    .toList();

            for (Release release : firstHalfReleases) {
                // todo: create training and testing set
            }

            logger.info("Finished\n");

        } catch (IOException | URISyntaxException | GitAPIException | ReleaseNotFoundException |
                 IncorrectProportionException e) {
            logger.info(e.toString());
        }
    }
}
