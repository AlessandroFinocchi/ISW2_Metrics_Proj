package it.uniroma2.alessandro.controllers.processors.sets;

import it.uniroma2.alessandro.controllers.scrapers.GitScraper;
import it.uniroma2.alessandro.enums.DatasetType;
import it.uniroma2.alessandro.enums.OutputFileType;
import it.uniroma2.alessandro.models.ProjectClass;
import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.models.Ticket;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class TrainingTestSetsProcessor {

    public static final String NAME_OF_THIS_CLASS = TrainingTestSetsProcessor.class.getName();
    private static final Logger logger = Logger.getLogger(NAME_OF_THIS_CLASS);

    private int walkForwardIterations = 0;

    public void processWalkForward(GitScraper gitScraper, List<Release> releaseList, List<Ticket> ticketList,
                                   List<ProjectClass> classList, String projName) throws IOException {
        walkForwardIterations++;

        //Last release cannot be used as a training set, otherwise there wouldn't be release in the testing set
        List<Release> trainingSetReleaseList = releaseList.stream()
                .filter(r -> r.getNumericID() < releaseList.getLast().getNumericID())
                .toList();

        List<Ticket> trainingSetTicketList = ticketList.stream()
                .filter(t -> t.getFixedVersion().getNumericID() < releaseList.getLast().getNumericID())
                .toList();

        List<ProjectClass> trainingSetClassList = classList.stream()
                .filter(c -> c.getRelease().getNumericID() < releaseList.getLast().getNumericID())
                .toList();

        processTrainingSet(gitScraper, trainingSetReleaseList, trainingSetTicketList, trainingSetClassList, projName);

        processTestingSet(gitScraper, releaseList, ticketList, classList, projName);
    }

    private void processTrainingSet(GitScraper gitScraper, List<Release> trainingSetReleaseList, List<Ticket> trainingSetTicketList,
                                    List<ProjectClass> trainingSetClassList, String projName) throws IOException {
        String loggerString;

        // Compute buggyness with information until current release
        gitScraper.completeClassesInfo(trainingSetTicketList, trainingSetClassList);

        // Build training set at current release
        DatasetsProcessor.writeDataset(projName, trainingSetReleaseList, trainingSetClassList,
                trainingSetReleaseList.getLast().getNumericID(), DatasetType.TRAINING, OutputFileType.ARFF);
        DatasetsProcessor.writeDataset(projName, trainingSetReleaseList, trainingSetClassList,
                trainingSetReleaseList.getLast().getNumericID(), DatasetType.TRAINING, OutputFileType.CSV);

        if(walkForwardIterations==1){
            loggerString = projName + " Training set built on first release\n";
        }else{
            loggerString = projName + " Training set build on releases [1, " + (trainingSetReleaseList.getLast().getNumericID() + 1) + "]\n";
        }
        logger.info(loggerString);
    }

    private void processTestingSet(GitScraper gitScraper, List<Release> releaseList, List<Ticket> currentTicketList,
                                   List<ProjectClass> classList, String projName) throws IOException {
        String loggerString;

        // Get the release to predict, the one after the current
        Release predictingRelease = releaseList.getLast();

        // Get the classes to predict
        List<ProjectClass> predictingClassList = classList.stream()
                .filter(c -> c.getRelease().getNumericID() == predictingRelease.getNumericID())
                .toList();

        // Compute buggyness with all information until the present
        gitScraper.completeClassesInfo(currentTicketList, predictingClassList);

        // Build testing set for the predicting release
        DatasetsProcessor.writeDataset(projName, releaseList, predictingClassList,
                predictingRelease.getNumericID() - 1,
                DatasetType.TESTING, OutputFileType.ARFF);
        DatasetsProcessor.writeDataset(projName, releaseList, predictingClassList,
                predictingRelease.getNumericID() - 1,
                DatasetType.TESTING, OutputFileType.CSV);


        loggerString = projName + " Testing set build on release " + predictingRelease.getNumericID() + "\n";
        logger.info(loggerString);
    }

    public int getWalkForwardIterations() {
        return walkForwardIterations;
    }
}
