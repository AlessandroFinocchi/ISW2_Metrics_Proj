package it.uniroma2.alessandro.controllers.processors.acume;

import it.uniroma2.alessandro.models.ACUMEInstance;
import it.uniroma2.alessandro.models.ClassifierResult;
import org.jetbrains.annotations.NotNull;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static it.uniroma2.alessandro.controllers.scrapers.MetricsScraper.RESULT_DIRECTORY_NAME;


public class ACUMEProcessor {
    private final String directoryName;
    private final String projName;

    public ACUMEProcessor(String projName) throws IOException {
        // Create directory
        this.projName = projName;
        this.directoryName = RESULT_DIRECTORY_NAME + projName.toLowerCase() + "/" + projName.toLowerCase() + "AcumeFiles/";
        File file = new File(directoryName);
        if (!file.exists() && !file.mkdirs())  throw new IOException();
    }

    public void processACUMEFiles(List<ClassifierResult> results) throws Exception {
        for (ClassifierResult result : results) {
            List<ACUMEInstance> acumeInstances = new ArrayList<>();

            String trainingSetLocation = RESULT_DIRECTORY_NAME + projName.toLowerCase()
                    + "/arffFiles/training/" + projName.toLowerCase() + "_trainingSet" + result.getWalkForwardIteration()  + ".arff";
            String testingSetLocation = RESULT_DIRECTORY_NAME + projName.toLowerCase()
                    + "/arffFiles/testing/" + projName.toLowerCase() + "_testingSet" + result.getWalkForwardIteration()  + ".arff";

            ConverterUtils.DataSource trainingSetDataSource = new ConverterUtils.DataSource(trainingSetLocation);
            ConverterUtils.DataSource testingSetDataSource = new ConverterUtils.DataSource(testingSetLocation);

            Instances trainingSetInstance = trainingSetDataSource.getDataSet();
            Instances testingSetInstance = testingSetDataSource.getDataSet();

            // Set the label is the last attribute
            int numAttr = testingSetInstance.numAttributes();
            trainingSetInstance.setClassIndex(numAttr - 1);
            testingSetInstance.setClassIndex(numAttr - 1);

            result.getCustomClassifier().getClassifier().buildClassifier(trainingSetInstance);
            Evaluation eval = new Evaluation(testingSetInstance);
            eval.evaluateModel(result.getCustomClassifier().getClassifier(), testingSetInstance);

            int sizeIndex = testingSetInstance.attribute("SIZE").index();
            int isBuggyIndex = testingSetInstance.classAttribute().index();
            int trueIsBuggyIndex = testingSetInstance.classAttribute().indexOfValue("YES");

            if(trueIsBuggyIndex != -1){
                for (int i = 0; i < testingSetInstance.numInstances(); i++) {
                    int sizeValue = (int) testingSetInstance.instance(i).value(sizeIndex);
                    int valueIndex = (int) testingSetInstance.instance(i).value(isBuggyIndex);
                    String buggyness =  testingSetInstance.attribute(isBuggyIndex).value(valueIndex);
                    double[] distribution = result.getCustomClassifier().getClassifier().distributionForInstance(testingSetInstance.instance(i));
                    ACUMEInstance acumeUtils = new ACUMEInstance(i, sizeValue, distribution[trueIsBuggyIndex], buggyness);
                    acumeInstances.add(acumeUtils);
                }
            }
            
            processACUMEFile(acumeInstances, result);
        }
    }

    private void processACUMEFile(List<ACUMEInstance> acumeInstances, ClassifierResult classifierResult) throws IOException {
        // Create file
        File file = getFile(classifierResult);

        // Write file
        try(FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write("ID, Size, Predicted, Actual\n");

            for(ACUMEInstance acumeInstance : acumeInstances) {
                fileWriter.write(acumeInstance.getId() + ",");
                fileWriter.write(acumeInstance.getSize() + ",");
                fileWriter.write(acumeInstance.getPredicted() + ",");
                fileWriter.write(acumeInstance.getActual() + "\n");
            }
        }

    }

    private @NotNull File getFile(ClassifierResult classifierResult) {
        String costSensitive = classifierResult.getCustomClassifier().isCostSensitive() ? "yesCostSensitive" : "noCostSensitive";
        String filename =  directoryName + projName.toLowerCase() +
                "_" + classifierResult.getClassifierName() +
                "_" + classifierResult.getCustomClassifier().getFeatureSelectionFilterName() +
                "_" + classifierResult.getCustomClassifier().getSamplingFilterName() +
                "_" + costSensitive +
                "_" + classifierResult.getWalkForwardIteration() +
                ".csv";

        return new File(filename);
    }
}
