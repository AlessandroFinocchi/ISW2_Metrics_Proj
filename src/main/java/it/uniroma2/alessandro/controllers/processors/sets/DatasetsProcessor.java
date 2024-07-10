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

import static it.uniroma2.alessandro.controllers.scrapers.MetricsScraper.RESULT_DIRECTORY_NAME;

public class DatasetsProcessor {

    public static final String NAME_OF_THIS_CLASS = DatasetsProcessor.class.getName();
    private static final Logger logger = Logger.getLogger(NAME_OF_THIS_CLASS);

    private DatasetsProcessor() {}
    
    public static void writeDataset(String projName, List<Release> releaseList, List<ProjectClass> classList, int iterationNumber,
                                        DatasetType datasetType, OutputFileType extension) throws IOException {
        StringBuilder projNameDelimited = new StringBuilder(projName.toLowerCase()).append("/");
        StringBuilder datasetTypeDelimited = new StringBuilder(datasetType.getId().toLowerCase()).append("/");
        StringBuilder pathname = new StringBuilder(RESULT_DIRECTORY_NAME).append(projNameDelimited)
                .append(extension.getId().toLowerCase()).append("Files/").append(datasetType.getId().toLowerCase());
        File file = new File(pathname.toString());
        if (!file.exists() && !file.mkdirs())  throw new IOException();

        StringBuilder fileName = new StringBuilder();
        fileName.append(projName.toLowerCase()).append("_").append(datasetType.getId().toLowerCase()).append("Set").append(iterationNumber)
                .append(".").append(extension.getId().toLowerCase());
        pathname = new StringBuilder(RESULT_DIRECTORY_NAME).append(projNameDelimited)
                .append(extension.getId().toLowerCase()).append("Files/").append(datasetTypeDelimited).append(fileName);
        file = new File(pathname.toString());

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
                        @attribute NUMBER_OF_REVISIONS numeric
                        @attribute NUMBER_OF_DEFECT_FIXES numeric
                        @attribute NUMBER_OF_AUTHORS numeric
                        @attribute CBO numeric
                        @attribute FAN_IN numeric
                        @attribute FAN_OUT numeric
                        @attribute PUBLIC_METHODS_QTY numeric
                        @attribute IS_BUGGY {'YES', 'NO'}
                        
                        @data
                        """);
            appendByRelease(releaseList, allProjectClasses, fileWriter, true);
        }else{
            fileWriter.append(
                    "RELEASE_ID," +
                    "FILE_NAME," +
                    "SIZE," +
                    "LOC_ADDED,LOC_ADDED_AVG,LOC_ADDED_MAX," +
                    "LOC_REMOVED,LOC_REMOVED_AVG,LOC_REMOVED_MAX," +
                    "CHURN,CHURN_AVG,CHURN_MAX," +
                    "NUMBER_OF_REVISIONS," +
                    "NUMBER_OF_DEFECT_FIXES," +
                    "NUMBER_OF_AUTHORS," +
                    "CBO," +
                    "FAN_IN," +
                    "FAN_OUT," +
                    "PUBLIC_METHODS_QTY,"+
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
        String className = projectClass.getName();
        String sizeOfClass = String.valueOf(projectClass.getMetrics().getSize());
        String addedLOC = String.valueOf(projectClass.getMetrics().getAddedLOCMetrics().getVal());
        String avgAddedLOC = String.valueOf(projectClass.getMetrics().getAddedLOCMetrics().getAvgVal());
        String maxAddedLOC = String.valueOf(projectClass.getMetrics().getAddedLOCMetrics().getMaxVal());
        String removedLOC = String.valueOf(projectClass.getMetrics().getRemovedLOCMetrics().getVal());
        String avgRemovedLOC = String.valueOf(projectClass.getMetrics().getRemovedLOCMetrics().getAvgVal());
        String maxRemovedLOC = String.valueOf(projectClass.getMetrics().getRemovedLOCMetrics().getMaxVal());
        String churn = String.valueOf(projectClass.getMetrics().getChurnMetrics().getVal());
        String avgChurn = String.valueOf(projectClass.getMetrics().getChurnMetrics().getAvgVal());
        String maxChurn = String.valueOf(projectClass.getMetrics().getChurnMetrics().getMaxVal());
        String nRevisions = String.valueOf(projectClass.getMetrics().getNumberOfRevisions());
        String nDefectFixes = String.valueOf(projectClass.getMetrics().getNumberOfDefectFixes());
        String nAuthors = String.valueOf(projectClass.getMetrics().getNumberOfAuthors());
        String cbo = String.valueOf(projectClass.getMetrics().getComplexityMetrics().getCbo());
        String fanIn = String.valueOf(projectClass.getMetrics().getComplexityMetrics().getFanIn());
        String fanOut = String.valueOf(projectClass.getMetrics().getComplexityMetrics().getFanOut());
        String publicMethodsQty = String.valueOf(projectClass.getMetrics().getComplexityMetrics().getPublicMethodsQty());
        String isClassBugged = projectClass.getMetrics().getBuggyness() ? "YES" : "NO" ;

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
                .append(churn).append(",")
                .append(avgChurn).append(",")
                .append(maxChurn).append(",")
                .append(nRevisions).append(",")
                .append(nDefectFixes).append(",")
                .append(nAuthors).append(",")
                .append(cbo).append(",")
                .append(fanIn).append(",")
                .append(fanOut).append(",")
                .append(publicMethodsQty).append(",")
                .append(isClassBugged).append("\n");
    }
}
