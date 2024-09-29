package it.uniroma2.alessandro.controllers.processors.weka;

import it.uniroma2.alessandro.models.ClassifierResult;
import it.uniroma2.alessandro.models.ProjectClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static it.uniroma2.alessandro.controllers.scrapers.MetricsScraper.RESULT_DIRECTORY_NAME;


public class WekaProcessor {
    private final String projName;
    private final int walkForwardIterations; // Is the number of datasets for the considered project
    private final Logger logger = Logger.getLogger(WekaProcessor.class.getName());

    /**
     * @param projName the name of the project
     * @param walkForwardIterations the number of training/testing sets of the project
     */
    public WekaProcessor(String projName, int walkForwardIterations) {
        this.projName = projName;
        this.walkForwardIterations = walkForwardIterations;
    }

    public List<ClassifierResult> processClassifierResults() throws Exception {
        List<ClassifierResult> classifierResultList = new ArrayList<>();
        for(int i = 1; i < walkForwardIterations; i++){

            // Get the instances of training and testing sets
            String trainingSetLocation = RESULT_DIRECTORY_NAME + projName.toLowerCase()
                    + "/arffFiles/training/" + projName.toLowerCase() + "_trainingSet" + i + ".arff";
            String testingSetLocation = RESULT_DIRECTORY_NAME + projName.toLowerCase()
                    + "/arffFiles/testing/" + projName.toLowerCase() + "_testingSet" + i + ".arff";
            DataSource trainingSetDataSource = new DataSource(trainingSetLocation);
            DataSource testingSetDataSource = new DataSource(testingSetLocation);
            Instances trainingSetInstance = trainingSetDataSource.getDataSet();
            Instances testingSetInstance = testingSetDataSource.getDataSet();

            // Set the label is the last attribute
            int numAttr = trainingSetInstance.numAttributes();
            trainingSetInstance.setClassIndex(numAttr - 1);
            testingSetInstance.setClassIndex(numAttr - 1);

            // Get the list of all classifier to train
            ClassifiersProcessor classifiersProcessor = new ClassifiersProcessor();
            List<ProjectClassifier> projectClassifiers = classifiersProcessor.getClassifiers(trainingSetInstance.attributeStats(numAttr - 1));

            // For each classifier
            for (ProjectClassifier projectClassifier : projectClassifiers) {

                // Train model on training set
                Classifier classifier = projectClassifier.getClassifier();
                classifier.buildClassifier(trainingSetInstance);

                // Evaluate model on testing set
                Evaluation evaluator = new Evaluation(testingSetInstance);
                evaluator.evaluateModel(classifier, testingSetInstance);

                // Save the results
                ClassifierResult classifierResult = new ClassifierResult(i, projectClassifier, evaluator);

                // The training percent is the number of #Instances(TrS)/#Instances(TrS + TsS)
                classifierResult.setTrainingPercent(100.0 * trainingSetInstance.numInstances() / (trainingSetInstance.numInstances() + testingSetInstance.numInstances()));
                classifierResultList.add(classifierResult);
            }
        }
        return classifierResultList;
    }
}
