package it.uniroma2.alessandro.utilities;

import it.uniroma2.alessandro.controllers.processors.weka.WekaProcessor;
import it.uniroma2.alessandro.models.ComplexityMetrics;
import it.uniroma2.alessandro.models.ProjectClass;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class FileReaderUtility {
    private final static Logger logger = Logger.getLogger(WekaProcessor.class.getName());
    private final static int CLASS_FILE_NAME_INDEX = 0;
    private final static int CBO_INDEX = 3;
    private final static int FAN_IN_INDEX = 5;
    private final static int FAN_OUT_INDEX = 6;
    private final static int PUBLIC_METHODS_QTY_INDEX = 17;

    // Consider that in the complexity file for each file in the arf files there are multiple classes in the complexity
    // files, so take just the first one
    public static void readComplexityMetrics(String filePath, List<ProjectClass> classList, String projName) {
        String line;
        String csvSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip the header
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] values = line.split(csvSplitBy);

                // Get the file name without the absolute part of the path: want to eliminate ".../bookkeeperClone/"
                String classFileName = values[CLASS_FILE_NAME_INDEX];
                String target = projName+"Clone";
                int index = classFileName.indexOf(target);
                String classFileNameParsed = classFileName.substring(index + target.length() + 1);

                // Get metrics
                int cbo = Integer.parseInt(values[CBO_INDEX]);
                int fanIn = Integer.parseInt(values[FAN_IN_INDEX]);
                int fanOut = Integer.parseInt(values[FAN_OUT_INDEX]);
                int publicMethodsQty = Integer.parseInt(values[PUBLIC_METHODS_QTY_INDEX]);
                ComplexityMetrics record = new ComplexityMetrics(cbo, fanIn, fanOut, publicMethodsQty);

                List<ProjectClass> currentProjectClassList = classList.stream()
                        .filter(c -> c.getName().equals(classFileNameParsed))
                        .toList();

                if(currentProjectClassList.size() == 1){
                    ProjectClass currentProjectClass = currentProjectClassList.getFirst();
                    currentProjectClass.getMetrics().setComplexityMetrics(record);
                }

            }

        } catch (IOException e) {
            logger.info(e.getMessage());
        }
    }
}
