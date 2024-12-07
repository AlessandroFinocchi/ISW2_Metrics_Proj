package it.uniroma2.alessandro.utilities;


import it.uniroma2.alessandro.models.ClassifierResult;
import it.uniroma2.alessandro.models.Commit;
import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.models.Ticket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jgit.revwalk.RevCommit;

import it.uniroma2.alessandro.enums.ReportType;

import static it.uniroma2.alessandro.controllers.scrapers.MetricsScraper.RESULT_DIRECTORY_NAME;


public class ReportUtility {

    public static final String CLOSE_BRACKET_AND_NEW_LINE = "]\n\n";
    public static final String NAME_OF_THIS_CLASS = ReportUtility.class.getName();
    private static final Logger logger = Logger.getLogger(NAME_OF_THIS_CLASS);

    private ReportUtility() {
    }

    public static void writeOnReportFiles(String projName, List<Release> releaseList, List<Ticket> ticketList,
                                          List<Commit> commitList, List<Commit> filteredCommitsOfIssues) {
        try {
            File file = new File(RESULT_DIRECTORY_NAME + projName.toLowerCase() + "/reportFiles/");
            if (!file.exists()) {
                boolean created = file.mkdirs();
                if (!created) {
                    throw new IOException();
                }
            }
            for(ReportType reportType: ReportType.values()){
                file = new File(RESULT_DIRECTORY_NAME + projName.toLowerCase() + "/reportFiles/" + reportType.getId() + ".txt");
                try(FileWriter fileWriter = new FileWriter(file)) {
                    fileWriter.append("---------- ")
                            .append(String.valueOf(reportType))
                            .append(" List/ ---------\n\n");
                    switch (reportType) {
                        case COMMITS -> appendCommitsInfo(commitList, fileWriter);
                        case RELEASES -> appendReleasesInfo(releaseList, fileWriter);
                        case SUMMARY -> appendSummaryInfo(releaseList, ticketList, commitList, filteredCommitsOfIssues, fileWriter);
                        case TICKETS -> appendTicketsInfo(ticketList, fileWriter);
                    }
                    FileWriterUtility.flushAndCloseFW(fileWriter, logger, NAME_OF_THIS_CLASS);
                }
            }
        } catch (IOException e) {
            logger.info("Error in writeOnReportFiles when trying to create directory");
        }
    }

    private static void appendSummaryInfo(List<Release> releaseList, List<Ticket> ticketList, List<Commit> commitList, List<Commit> filteredCommitsOfIssues, FileWriter fileWriter) throws IOException {
        fileWriter.append("----------------------------------------------------------\n")
                .append("EXTRACTION INFO:\n")
                .append(String.valueOf(releaseList.size())).append(" RELEASES \n")
                .append(String.valueOf(ticketList.size())).append(" TICKETS \n")
                .append(String.valueOf(commitList.size())).append(" TOTAL COMMITS \n")
                .append(String.valueOf(filteredCommitsOfIssues.size())).append(" COMMITS CONTAINING BUG-ISSUES")
                .append("\n----------------------------------------------------------\n\n");
    }

    private static void appendCommitsInfo(List<Commit> commitList, FileWriter fileWriter) throws IOException {
        for (Commit commit: commitList){
            RevCommit revCommit = commit.getRevCommit();
            Ticket ticket = commit.getTicket();
            Release release = commit.getRelease();
            fileWriter.append("Commit[ID= ").append(revCommit.getName())
                    //.append(", committer= ").append(revCommit.getCommitterIdent().getName())
                    //.append(", message= ").append(revCommit.getFullMessage())
                    .append((ticket == null) ? "" : ", ticket= " + commit.getTicket().getTicketKey())
                    .append(", release= ").append(release.getReleaseName())
                    .append(", creationDate= ").append(String.valueOf(LocalDate.parse((new SimpleDateFormat("yyyy-MM-dd").format(revCommit.getCommitterIdent().getWhen()))))).append(CLOSE_BRACKET_AND_NEW_LINE);
        }
    }

    private static void appendTicketsInfo(List<Ticket> ticketList, FileWriter fileWriter) throws IOException {
        List<Ticket> ticketOrderedByCreation = new ArrayList<>(ticketList);
        ticketOrderedByCreation.sort(Comparator.comparing(Ticket::getCreationDate));
        for (Ticket ticket : ticketOrderedByCreation) {
            List<String> releaseNames = new ArrayList<>();
            for(Release release : ticket.getAffectedVersions()) {
                releaseNames.add(release.getReleaseName());
            }
            fileWriter.append("Ticket[key= ").append(ticket.getTicketKey())
                    .append(", openingVersion= ").append(ticket.getOV().getReleaseName())
                    .append(", fixedVersion= ") .append(ticket.getFV().getReleaseName())
                    .append(", affectedVersions= ").append(String.valueOf(releaseNames))
                    .append(", numOfCommits= ").append(String.valueOf(ticket.getCommitList().size()))
                    .append(", creationDate= ").append(String.valueOf(ticket.getCreationDate()))
                    .append(", resolutionDate= ").append(String.valueOf(ticket.getResolutionDate())).append(CLOSE_BRACKET_AND_NEW_LINE);
        }
    }

    private static void appendReleasesInfo(List<Release> releaseList, FileWriter fileWriter) throws IOException {
        for (Release release : releaseList) {
            fileWriter.append("Release[id= ").append(String.valueOf(release.getReleaseID()))
                    .append(", releaseName= ").append(release.getReleaseName())
                    .append(", releaseDate= ").append(String.valueOf(release.getReleaseDateString()))
                    .append(", numOfCommits= ").append(String.valueOf(release.getCommitList().size())).append(CLOSE_BRACKET_AND_NEW_LINE);
        }
    }

    public static void writeFinalResults(String projName, List<ClassifierResult> finalResultsList){
        try {
            // Create directory
            String directoryName = RESULT_DIRECTORY_NAME + projName.toLowerCase() + "/finalResults/";
            File file = new File(directoryName);
            if (!file.exists() && !file.mkdirs()) throw new IOException();

            String pathname = directoryName + projName.toLowerCase() + "_finalResults.csv";
            file = new File(pathname);
            try(FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.append("DATASET," +
                        "#TRAINING_RELEASES," +
                        "%TRAINING_INSTANCES," +
                        "CLASSIFIER," +
                        "FEATURE_SELECTION," +
                        "BALANCING," +
                        "COST_SENSITIVE," +
                        "PRECISION," +
                        "RECALL," +
                        "AREA_UNDER_ROC," +
                        "KAPPA," +
                        "TRUE_POSITIVES," +
                        "FALSE_POSITIVES," +
                        "TRUE_NEGATIVES," +
                        "FALSE_NEGATIVES").append("\n");
                for(ClassifierResult resultOfClassifier: finalResultsList){
                    fileWriter.append(projName).append(",")
                            .append(String.valueOf(resultOfClassifier.getWalkForwardIteration())).append(",")
                            .append(String.valueOf(resultOfClassifier.getTrainingPercent())).append(",")
                            .append(resultOfClassifier.getClassifierName()).append(",");
                    if(resultOfClassifier.hasFeatureSelection()){
                        fileWriter.append(resultOfClassifier.getCustomClassifier().getFeatureSelectionFilterName()).append(",");
                    }else {
                        fileWriter.append("None").append(",");
                    }
                    if(resultOfClassifier.hasSampling()){
                        fileWriter.append(resultOfClassifier.getCustomClassifier().getSamplingFilterName()).append(",");
                    }else {
                        fileWriter.append("None").append(",");
                    }
                    if (resultOfClassifier.hasCostSensitive()){
                        fileWriter.append("SensitiveLearning").append(",");
                    }else {
                        fileWriter.append("None").append(",");
                    }
                    fileWriter.append(String.valueOf(resultOfClassifier.getPrecision())).append(",")
                            .append(String.valueOf(resultOfClassifier.getRecall())).append(",")
                            .append(String.valueOf(resultOfClassifier.getAreaUnderROC())).append(",")
                            .append(String.valueOf(resultOfClassifier.getKappa())).append(",")
                            .append(String.valueOf(resultOfClassifier.getTruePositives())).append(",")
                            .append(String.valueOf(resultOfClassifier.getFalsePositives())).append(",")
                            .append(String.valueOf(resultOfClassifier.getTrueNegatives())).append(",")
                            .append(String.valueOf(resultOfClassifier.getFalseNegatives())).append("\n");
                }
                FileWriterUtility.flushAndCloseFW(fileWriter, logger, NAME_OF_THIS_CLASS);
            }
        } catch (IOException e) {
            logger.info("Error in .csv creation when trying to create directory");
        }
    }
}
