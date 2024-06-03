package it.uniroma2.alessandro.controllers.processors;

import it.uniroma2.alessandro.controllers.processors.sets.DatasetsProcessor;
import it.uniroma2.alessandro.controllers.scrapers.GitScraper;
import it.uniroma2.alessandro.enums.DatasetType;
import it.uniroma2.alessandro.enums.OutputFileType;
import it.uniroma2.alessandro.models.ProjectClass;
import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.models.Ticket;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

public class TrainingTestSetsProcessor {

    public static final String NAME_OF_THIS_CLASS = TrainingTestSetsProcessor.class.getName();
    private static final Logger logger = Logger.getLogger(NAME_OF_THIS_CLASS);

    public void processWalkForward(GitScraper gitScraper, List<Release> releaseList, List<Ticket> ticketList,
                                   List<ProjectClass> classList, String projName) throws IOException {

        // Consider only the first half of releases
        LocalDate lastReleaseDate = releaseList.get(releaseList.size()/2).getReleaseDateTime();
        List<Release> firstHalfReleases = releaseList.stream()
                .filter(release -> !release.getReleaseDateTime().isAfter(lastReleaseDate))
                .toList();

        //Last release cannot be used as a training set, otherwise there wouldn't be release in the testing set
        List<Release> trainingsetReleaseList = firstHalfReleases.stream()
                .filter(r -> r.getNumericID() != firstHalfReleases.getLast().getNumericID())
                .toList();

        for (Release currentRelease : trainingsetReleaseList) {
            // Take all the release until the current one
            List<Release> currentReleaseList = releaseList.stream()
                    .filter(r -> !r.getReleaseDateTime().isAfter(currentRelease.getReleaseDateTime()))
                    .toList();

            // Take all the tickets until the current release
            List<Ticket> currentTicketList = ticketList.stream()
                    .filter(t -> t.getFixedVersion().getNumericID() <= currentRelease.getNumericID())
                    .toList();

            processTrainingSet(gitScraper, currentReleaseList, currentTicketList, classList, currentRelease, projName);

            processTestingSet(gitScraper, releaseList, ticketList, classList, currentRelease, projName);
        }
    }

    private void processTrainingSet(GitScraper gitScraper, List<Release> currentReleaseList, List<Ticket> currentTicketList,
                                    List<ProjectClass> classList, Release currentRelease, String projName) throws IOException {
        String loggerString;

        // Take all the class until the current release
        List<ProjectClass> currentClassList = classList.stream()
                .filter(c -> c.getRelease().getNumericID() <= currentRelease.getNumericID())
                .toList();

        // Compute buggyness with information until current release
        gitScraper.completeClassesInfo(currentTicketList, currentClassList);

        // Build training set at current release
        DatasetsProcessor.writeDataset(projName, currentReleaseList, classList, currentRelease.getNumericID(),
                DatasetType.TRAINING, OutputFileType.ARFF);
        DatasetsProcessor.writeDataset(projName, currentReleaseList, classList, currentRelease.getNumericID(),
                DatasetType.TRAINING, OutputFileType.CSV);

        if(currentRelease.getNumericID()==1){
            loggerString = projName + " Training set built on first release\n";
        }else{
            loggerString = projName + " Training set build on releases [1, " + currentRelease.getNumericID() + "]\n";
        }
        logger.info(loggerString);
    }

    private void processTestingSet(GitScraper gitScraper, List<Release> releaseList, List<Ticket> currentTicketList,
                                   List<ProjectClass> classList, Release currentRelease, String projName) throws IOException {
        String loggerString;

        // Get the release to predict, the one after the current
        Release predictingRelease = releaseList.stream()
                .filter(r-> r.getNumericID() == currentRelease.getNumericID() + 1)
                .toList()
                .getFirst();

        // Get the classes to predict
        List<ProjectClass> predictingClassList = classList.stream()
                .filter(c -> c.getRelease().getNumericID() != predictingRelease.getNumericID())
                .toList();

        // Compute buggyness with all information until the present
        gitScraper.completeClassesInfo(currentTicketList, predictingClassList);

        // Build testing set for the predicting release
        DatasetsProcessor.writeDataset(projName, releaseList, classList, predictingRelease.getNumericID(),
                DatasetType.TESTING, OutputFileType.ARFF);
        DatasetsProcessor.writeDataset(projName, releaseList, classList, predictingRelease.getNumericID(),
                DatasetType.TESTING, OutputFileType.CSV);

        loggerString = projName + " Testing set build on release " + predictingRelease.getNumericID() + "\n";
        logger.info(loggerString);
    }
}
