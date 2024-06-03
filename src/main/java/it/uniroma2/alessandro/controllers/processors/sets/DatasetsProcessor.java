package it.uniroma2.alessandro.controllers.processors.sets;


import it.uniroma2.alessandro.enums.DatasetType;
import it.uniroma2.alessandro.enums.OutputFileType;
import it.uniroma2.alessandro.models.ProjectClass;
import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.utilities.FileWriterUtility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class DatasetsProcessor {

    public static final String NAME_OF_THIS_CLASS = DatasetsProcessor.class.getName();
    private static final Logger logger = Logger.getLogger(NAME_OF_THIS_CLASS);

    private DatasetsProcessor() {}
    
    public static void writeDataset(String projName, List<Release> releaseList, List<ProjectClass> classList, int iterationNumber,
                                        DatasetType datasetType, OutputFileType extension) throws IOException {
        File file = new File("outputFiles/" + extension.getId().toLowerCase() + "Files/" + projName + "/" + datasetType.getId());
        if (!file.exists() && !file.mkdirs())  throw new IOException();

        StringBuilder fileName = new StringBuilder();
        fileName.append(projName).append("_").append(iterationNumber).append("_").append(datasetType.getId()).append("Set");

        fileName.append(".").append(extension.getId().toLowerCase());
        file = new File("outputFiles/" + extension.getId().toLowerCase() + "Files/" + projName + "/"
                + datasetType.getId() + "/" + projName + "_" + iterationNumber + "_" + datasetType.getId() + "Set."
                + extension.getId().toLowerCase());

        try(FileWriter fileWriter = new FileWriter(file)) {
            appendOnFile(releaseList, classList, extension.equals(OutputFileType.ARFF), fileName.toString(), fileWriter);
        }
    }

    private static void appendOnFile(List<Release> releaseList, List<ProjectClass> allProjectClasses, boolean isArff, String fileName, FileWriter fileWriter) throws IOException {
        if(isArff){
            fileWriter.append("@relation ").append(fileName).append("\n\n")
                    .append("""
                        @attribute SIZE numeric
                        @attribute LOC_ADDED numeric
                        @attribute LOC_ADDED_AVG numeric
                        @attribute LOC_ADDED_MAX numeric
                        @attribute LOC_REMOVED numeric
                        @attribute LOC_REMOVED_AVG numeric
                        @attribute LOC_REMOVED_MAX numeric
                        @attribute CHURN numeric
                        @attribute CHURN_AVG numeric
                        @attribute CHURN_MAX numeric
                        @attribute LOC_TOUCHED numeric
                        @attribute LOC_TOUCHED_AVG numeric
                        @attribute LOC_TOUCHED_MAX numeric
                        @attribute NUMBER_OF_REVISIONS numeric
                        @attribute NUMBER_OF_DEFECT_FIXES numeric
                        @attribute NUMBER_OF_AUTHORS numeric
                        @attribute IS_BUGGY {'YES', 'NO'}
                        
                        @data
                        """);
            appendByRelease(releaseList, allProjectClasses, fileWriter, true);
        }else{
            fileWriter.append("RELEASE_ID," +
                    "FILE_NAME," +
                    "SIZE," +
                    "LOC_ADDED,LOC_ADDED_AVG,LOC_ADDED_MAX," +
                    "LOC_REMOVED,LOC_REMOVED_AVG,LOC_REMOVED_MAX," +
                    "LOC_TOUCHED,LOC_TOUCHED_AVG,LOC_TOUCHED_MAX," +
                    "CHURN,CHURN_AVG,CHURN_MAX," +
                    "NUMBER_OF_REVISIONS," +
                    "NUMBER_OF_DEFECT_FIXES," +
                    "NUMBER_OF_AUTHORS," +
                    "IS_BUGGY").append("\n");
            appendByRelease(releaseList, allProjectClasses, fileWriter, false);
        }
        FileWriterUtility.flushAndCloseFW(fileWriter, logger, NAME_OF_THIS_CLASS);
    }

    private static void appendByRelease(List<Release> releaseList, List<ProjectClass> allProjectClasses, FileWriter fileWriter, boolean isArff) throws IOException {
        for (Release release : releaseList) {
            for (ProjectClass projectClass : allProjectClasses) {
                if (projectClass.getRelease().getNumericID() == release.getNumericID()) {
                    appendEntriesLikeCSV(fileWriter, release, projectClass, isArff);
                }
            }
        }
    }

    private static void appendEntriesLikeCSV(FileWriter fileWriter, Release release, ProjectClass projectClass, boolean isArff) throws IOException {
        String releaseID = Integer.toString(release.getNumericID());
        String isClassBugged = projectClass.getMetrics().getBuggyness() ? "YES" : "NO" ;
        String sizeOfClass = String.valueOf(projectClass.getMetrics().getSize());
        String addedLOC = String.valueOf(projectClass.getMetrics().getAddedLOCMetrics().getVal());
        String avgAddedLOC = String.valueOf(projectClass.getMetrics().getAddedLOCMetrics().getAvgVal());
        String maxAddedLOC = String.valueOf(projectClass.getMetrics().getAddedLOCMetrics().getMaxVal());
        String removedLOC = String.valueOf(projectClass.getMetrics().getRemovedLOCMetrics().getVal());
        String avgRemovedLOC = String.valueOf(projectClass.getMetrics().getRemovedLOCMetrics().getAvgVal());
        String maxRemovedLOC = String.valueOf(projectClass.getMetrics().getRemovedLOCMetrics().getMaxVal());
        String touchedLOC = String.valueOf(projectClass.getMetrics().getTouchedLOCMetrics().getVal());
        String avgTouchedLOC = String.valueOf(projectClass.getMetrics().getTouchedLOCMetrics().getAvgVal());
        String maxTouchedLOC = String.valueOf(projectClass.getMetrics().getTouchedLOCMetrics().getMaxVal());
        String churn = String.valueOf(projectClass.getMetrics().getChurnMetrics().getVal());
        String avgChurn = String.valueOf(projectClass.getMetrics().getChurnMetrics().getAvgVal());
        String maxChurn = String.valueOf(projectClass.getMetrics().getChurnMetrics().getMaxVal());
        String nRevisions = String.valueOf(projectClass.getMetrics().getNumberOfRevisions());
        String nDefectFixes = String.valueOf(projectClass.getMetrics().getNumberOfDefectFixes());
        String nAuthors = String.valueOf(projectClass.getMetrics().getNumberOfAuthors());

        String className = projectClass.getName();
        if(!isArff){
            fileWriter.append(releaseID).append(",")
                    .append(className).append(",");
        }
        fileWriter.append(sizeOfClass).append(",")
                .append(addedLOC).append(",")
                .append(avgAddedLOC).append(",")
                .append(maxAddedLOC).append(",")
                .append(removedLOC).append(",")
                .append(avgRemovedLOC).append(",")
                .append(maxRemovedLOC).append(",")
                .append(touchedLOC).append(",")
                .append(avgTouchedLOC).append(",")
                .append(maxTouchedLOC).append(",")
                .append(churn).append(",")
                .append(avgChurn).append(",")
                .append(maxChurn).append(",")
                .append(nRevisions).append(",")
                .append(nDefectFixes).append(",")
                .append(nAuthors).append(",")
                .append(isClassBugged).append("\n");
    }
}
