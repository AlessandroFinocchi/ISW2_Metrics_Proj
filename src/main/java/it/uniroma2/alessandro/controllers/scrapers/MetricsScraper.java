package it.uniroma2.alessandro.controllers.scrapers;

import it.uniroma2.alessandro.controllers.processors.acume.ACUMEProcessor;
import it.uniroma2.alessandro.controllers.processors.metrics.ComplexityMetricsProcessor;
import it.uniroma2.alessandro.controllers.processors.metrics.MetricsProcessor;
import it.uniroma2.alessandro.controllers.processors.sets.TrainingTestSetsProcessor;
import it.uniroma2.alessandro.controllers.processors.weka.WekaProcessor;
import it.uniroma2.alessandro.models.*;
import it.uniroma2.alessandro.utilities.PropertyUtility;
import it.uniroma2.alessandro.utilities.ReportUtility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static it.uniroma2.alessandro.controllers.processors.sets.DatasetsProcessor.RESULT_DIRECTORY_NAME;

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
            boolean computeComplexityFiles = setupSystem(projName);

            String loggerString;
            String projString = projName + " project...\n";
            logger.info("Starting\n");

            loggerString = "Cloning repository of " + projString;
            logger.info(loggerString);
            GitScraper gitScraper = new GitScraper(projName, projRepoUrl);

            JiraScraper jiraScraper = new JiraScraper(projName);
            loggerString = "Scraping releases of " + projString;
            logger.info(loggerString);
            List<Release> jiraReleases = jiraScraper.scrapeReleases(gitScraper);

            loggerString = "Scraping commits of " + projString;
            logger.info(loggerString);
            List<Commit> commitList = gitScraper.scrapeCommits(jiraReleases);

            loggerString = "Scraping tickets of " + projString;
            logger.info(loggerString);
            List<Ticket> ticketList = jiraScraper.scrapeTickets(jiraReleases);

            loggerString = "Filtering commits of " + projString;
            logger.info(loggerString);
            List<Commit> ticketedCommitList = applyFilters(jiraReleases, ticketList, commitList);

            //todo: è giusto farlo qua???   no!

            // Adjust the infos of the tickets setting their IVs with proportion
            Ticket.proportionTickets(ticketList, jiraReleases, projName);
            ticketList.sort(Comparator.comparing(Ticket::getResolutionDate));

            // Since it is time-consuming computing these files, and they are always th same, apart from the case where
            // new releases are published in Jira, compute them only if needed
            if(computeComplexityFiles){
                loggerString = "Extracting complexity metrics from " + projString;
                logger.info(loggerString);
                ComplexityMetricsProcessor complexityMetricsProcessor = new ComplexityMetricsProcessor(projName, jiraReleases, gitScraper);
                complexityMetricsProcessor.extractComplexityMetrics();
            }

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
     * Setup system properties and returns if the complexity metrics files must be computed
     * @param projName the name of the current processing project
     * @return if the complexity metrics files must be computed
     * @throws IOException in case of error in reading the properties
     */
    private boolean setupSystem(String projName) throws IOException {
        // Weka can use these libraries to ease computation when building machine learning models so that
        // when runs on systems without the libraries it uses the pure java implementations.
        System.setProperty("com.github.fommil.netlib.BLAS", "com.github.fommil.netlib.F2jBLAS");
        System.setProperty("com.github.fommil.netlib.LAPACK", "com.github.fommil.netlib.F2jLAPACK");
        System.setProperty("com.github.fommil.netlib.ARPACK", "com.github.fommil.netlib.F2jARPACK");

        // If complexity metrics file do not exist, they have to be computed
        boolean computeComplexityFiles = PropertyUtility.readBooleanProperty("COMPUTE_COMPLEXITY_FILES");
        File file = new File(RESULT_DIRECTORY_NAME + projName.toLowerCase() + "/complexityFiles/");
        if(!file.exists())
            computeComplexityFiles = true;

        return computeComplexityFiles;

    }

    /**
     * Filter commits that have a ticket id inside their message, setting the ticket of a commit and the list of
     * commits for each ticket, and removes tickets without a commit
     * @param commitList commits to filter
     * @param ticketList tickets to take ids from
     * @return a list of commits that reference a ticket
     */
    public List<Commit> applyFilters(List<Release> releaseList, List<Ticket> ticketList, List<Commit> commitList) {
        // Filter commits
        List<Commit> filteredCommitList = new ArrayList<>();
        for (Commit commit : commitList) {
            String commitFullMessage = commit.getRevCommit().getFullMessage();
            for (Ticket ticket : ticketList) {
                String ticketKey = ticket.getTicketKey();
                if (matchRegex(commitFullMessage, ticketKey)) {
                    filteredCommitList.add(commit);
                    ticket.addCommit(commit);
                    commit.setTicket(ticket);
                }
            }
        }

        // If a ticket has no commits it means it isn't solved, so we don't care about it
        ticketList.removeIf(ticket -> ticket.getCommitList().isEmpty());

        for (int i = 0; i < releaseList.size(); i++){
            releaseList.get(i).setNumericID(i + 1);
        }

        return filteredCommitList;
    }

    private boolean matchRegex(String s, String regex){
        Pattern pattern = Pattern.compile(regex + "\\b");
        return pattern.matcher(s).find();
    }
}
