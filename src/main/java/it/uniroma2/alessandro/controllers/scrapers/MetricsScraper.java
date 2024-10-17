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

/**
 * Retrieves the metric information online
 */
public class MetricsScraper {
    private final Logger logger;
    public static final String RESULT_DIRECTORY_NAME = "results/";

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

            // Wait for the fake releases to be added and then set the numeric ids
            setReleasesNumericID(jiraReleases);

            // Take half release
            List<Release> datasetReleases = jiraReleases.subList(0, jiraReleases.size()/2 + 1);
            TrainingTestSetsProcessor setsProcessor = new TrainingTestSetsProcessor();

            // Since it is time-consuming computing these files, and they are always th same, apart from the case where
            // new releases are published in Jira, compute them only if needed
            if(computeComplexityFiles){
                loggerString = "Extracting complexity metrics from " + projString;
                logger.info(loggerString);
                ComplexityMetricsProcessor complexityMetricsProcessor = new ComplexityMetricsProcessor(projName, datasetReleases, gitScraper);
                complexityMetricsProcessor.extractComplexityMetrics();
            }

            for (Release currentRelease: datasetReleases){

                //Skip first release
                if(currentRelease.getNumericID() == 1)
                    continue;

                List<Release> consideringReleases = getConsideringReleases(datasetReleases, currentRelease);
                List<Ticket> consideringTickets = getConsideringTickets(ticketList, currentRelease);
                List<Commit> consideringCommits = getConsideringCommits(commitList, currentRelease);
                List<Commit> consideringTicketedCommits = applyFilters(consideringTickets, consideringCommits);

                // Adjust the infos of the tickets setting their IVs with proportion
                Ticket.proportionTickets(consideringTickets, consideringReleases, projName);
                consideringTickets.sort(Comparator.comparing(Ticket::getResolutionDate));

                loggerString = "Extracting touched classes from " + projString;
                logger.info(loggerString);
                // Use the whole commit list to don't lose the last commit of a release to read their classes
                List<ProjectClass> classList = gitScraper.scrapeClasses(consideringReleases, consideringCommits);

                loggerString = "Extracting metrics from " + projString;
                logger.info(loggerString);
                MetricsProcessor metricsProcessor = new MetricsProcessor(consideringReleases, consideringTicketedCommits,
                        classList, gitScraper, projName);
                metricsProcessor.processMetrics();

                loggerString = "Starting walk forward to build training and testing sets for " + projString;
                logger.info(loggerString);
                setsProcessor.processWalkForward(gitScraper, consideringReleases, consideringTickets, classList, projName);
            }

            loggerString = "Training WEKA classifiers for " + projString;
            logger.info(loggerString);
            WekaProcessor wekaProcessor = new WekaProcessor(projName, setsProcessor.getWalkForwardIterations());
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
            logger.info(e.getMessage());
        }
    }

    private List<Release> getConsideringReleases(List<Release> jiraReleases, Release currentRelease) {
        return jiraReleases
                .stream()
                .filter(r-> r.getNumericID() <= currentRelease.getNumericID())
                .toList();
    }

    private List<Ticket> getConsideringTickets(List<Ticket> ticketList, Release currentRelease) {
        List<Ticket> consideringTicketList = ticketList
                .stream()
                .filter(t -> t.getFixedVersion().getNumericID() <= currentRelease.getNumericID())
                .toList();

        List<Ticket> returningTicketList = new ArrayList<>();

        for (Ticket ticket: consideringTicketList) {
            Ticket newTicket = ticket.cloneTicketAtRelease(ticket.getFixedVersion());
            returningTicketList.add(newTicket);
        }

        return returningTicketList;
    }

    private List<Commit> getConsideringCommits(List<Commit> commitList, Release currentRelease) {
        List<Commit> consideringCommitList = commitList
                .stream()
                .filter(c -> c.getRelease().getNumericID() <= currentRelease.getNumericID())
                .toList();

        List<Commit> returningCommitList = new ArrayList<>();

        for (Commit commit: consideringCommitList) {
            Commit newCommit = commit.cloneCommitAtRelease(commit.getRelease());
            returningCommitList.add(newCommit);
        }

        return returningCommitList;
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
    public List<Commit> applyFilters(List<Ticket> ticketList, List<Commit> commitList) {
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

        return filteredCommitList;
    }

    private void setReleasesNumericID(List<Release> releaseList) {
        releaseList.sort(Comparator.comparing(Release::getReleaseDateTime));
        for (int i = 0; i < releaseList.size(); i++){
            releaseList.get(i).setNumericID(i + 1);
        }
    }

    private boolean matchRegex(String s, String regex){
        Pattern pattern = Pattern.compile(regex + "\\b");
        return pattern.matcher(s).find();
    }
}
