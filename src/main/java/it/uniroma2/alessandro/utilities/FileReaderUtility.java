package it.uniroma2.alessandro.utilities;

import it.uniroma2.alessandro.models.ComplexityMetrics;
import it.uniroma2.alessandro.models.ProjectClass;

import java.awt.geom.IllegalPathStateException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FileReaderUtility {
    private static final Logger logger = Logger.getLogger(FileReaderUtility.class.getName());
    private static final int CLASS_FILE_NAME_INDEX = 0;
    private static final int CBO_INDEX = 3;
    private static final int FAN_IN_INDEX = 5;
    private static final int FAN_OUT_INDEX = 6;
    private static final int PUBLIC_METHODS_QTY_INDEX = 17;

    private FileReaderUtility() {
        throw new IllegalStateException("Utility class");
    }

    // Consider that in the complexity file for each file in the arf files there are multiple classes in the complexity
    // files, so take just the first one
    public static void readComplexityMetrics(String filePath, List<ProjectClass> classList, String projName) {
        String csvSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip the header
            Stream<String> lines = br.lines().skip(1);

            lines.forEachOrdered(line -> {
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
                ComplexityMetrics complexityMetrics = new ComplexityMetrics(cbo, fanIn, fanOut, publicMethodsQty);

                List<ProjectClass> currentProjectClassList = classList.stream()
                        .filter(c -> c.getName().equals(classFileNameParsed))
                        .toList();

                if(currentProjectClassList.size() == 1){
                    ProjectClass currentProjectClass = currentProjectClassList.getFirst();
                    currentProjectClass.getMetrics().setComplexityMetrics(complexityMetrics);
                }

            });

        } catch (IOException e) {
            logger.info(e.getMessage());
        }
    }
}
