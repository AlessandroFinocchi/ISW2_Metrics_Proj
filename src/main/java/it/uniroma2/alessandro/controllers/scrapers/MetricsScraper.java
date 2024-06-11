package it.uniroma2.alessandro.controllers.scrapers;

import it.uniroma2.alessandro.controllers.processors.ACUMEProcessor;
import it.uniroma2.alessandro.controllers.processors.MetricsProcessor;
import it.uniroma2.alessandro.controllers.processors.sets.TrainingTestSetsProcessor;
import it.uniroma2.alessandro.controllers.processors.weka.WekaProcessor;
import it.uniroma2.alessandro.models.*;
import it.uniroma2.alessandro.utilities.ReportUtility;

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
            setupSystem();

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
            List<Commit> ticketedCommitList = gitScraper.applyFilters(commitList, ticketList);

            loggerString = "Extracting touched classes from " + projString;
            logger.info(loggerString);
            //todo: giusto usare commitList e non ticketedCommitList?
            List<ProjectClass> classList = gitScraper.scrapeClasses(jiraReleases, ticketList, commitList);

            loggerString = "Extracting metrics from " + projString;
            logger.info(loggerString);
            MetricsProcessor metricsProcessor = new MetricsProcessor(ticketedCommitList, classList, gitScraper);
            metricsProcessor.processMetrics();

            loggerString = "Reporting results from " + projString;
            logger.info(loggerString);
            //todo: giusto usare commitList e non ticketedCommitList?
            ReportUtility.writeOnReportFiles(projName, jiraReleases, ticketList, commitList, ticketedCommitList);

            loggerString = "Starting walk forward to build training and testing sets for " + projString;
            logger.info(loggerString);
            TrainingTestSetsProcessor setsProcessor = new TrainingTestSetsProcessor();
            int walkForwardIterations = setsProcessor.processWalkForward(gitScraper, jiraReleases, ticketList, classList, projName);

            loggerString = "Training WEKA classifiers for " + projString;
            logger.info(loggerString);
            WekaProcessor wekaProcessor = new WekaProcessor(projName, walkForwardIterations);
            List<ClassifierResult> results = wekaProcessor.processClassifierResults();

            loggerString = "Writing results for " + projString;
            logger.info(loggerString);
            ReportUtility.writeFinalResults(projName, results);

            loggerString = "Writing ACUME files for " + projString;
            logger.info(loggerString);
            ACUMEProcessor acumeProcessor = new ACUMEProcessor(projName);
            acumeProcessor.processACUMEFiles(results);

            loggerString = "Finished work for " + projString;
            logger.info(loggerString);

        } catch (Exception e) {
            logger.info(e.toString());
        }
    }

    /**
     * Setup properties
     */
    private void setupSystem() {
        // Weka can use these libraries to ease computation when building machine learning models so that
        // when runs on systems without the libraries it uses the pure java implementations.
        System.setProperty("com.github.fommil.netlib.BLAS", "com.github.fommil.netlib.F2jBLAS");
        System.setProperty("com.github.fommil.netlib.LAPACK", "com.github.fommil.netlib.F2jLAPACK");
        System.setProperty("com.github.fommil.netlib.ARPACK", "com.github.fommil.netlib.F2jARPACK");
    }
}
