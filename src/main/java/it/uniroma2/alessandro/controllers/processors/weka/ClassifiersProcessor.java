package it.uniroma2.alessandro.controllers.processors.weka;

import it.uniroma2.alessandro.models.ProjectClassifier;
import it.uniroma2.alessandro.utilities.PropertyUtility;
import weka.attributeSelection.BestFirst;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.AttributeStats;
import weka.core.SelectedTag;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.SMOTE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClassifiersProcessor {

    public static final String NO_SELECTION = "NoSelection";
    public static final String NO_SAMPLING = "NoSampling";
    public final double falsePositiveWeight;
    public final double falseNegativeWeight;
    public final boolean usingSampling;

    public ClassifiersProcessor() throws IOException {
        falsePositiveWeight = PropertyUtility.readIntegerProperty("FALSE_POSITIVE_WEIGHT");
        falseNegativeWeight = PropertyUtility.readIntegerProperty("FALSE_NEGATIVE_WEIGHT");
        usingSampling = PropertyUtility.readBooleanProperty("USING_SAMPLING");
    }

    public List<ProjectClassifier> getClassifiers(AttributeStats isBuggyAttributeStats) {
        // Get the models to train
        List<Classifier> classifierList = new ArrayList<>(List.of(new RandomForest(), new NaiveBayes(), new IBk()));
        List<ProjectClassifier> projectClassifiersList = new ArrayList<>();

        // Get feature selection filters
        List<AttributeSelection> featureSelectionFilters = getFeatureSelectionFilters();

        //NO FEATURE SELECTION NO SAMPLING NO COST SENSITIVE
        createSimpleClassifiers(classifierList, projectClassifiersList);

        //YES FEATURE SELECTION NO SAMPLING NO COST SENSITIVE
        createFeatureSelectedClassifiers(classifierList, featureSelectionFilters, projectClassifiersList);

        //NO FEATURE SELECTION NO SAMPLING YES COST SENSITIVE
        createCostSensitiveClassifiers(classifierList, projectClassifiersList);

        //YES FEATURE SELECTION NO SAMPLING YES COST SENSITIVE
        createFeatureSelectedAndCostSensitiveClassifiers(classifierList, featureSelectionFilters, projectClassifiersList);

        if (usingSampling){
            createSamplingClassifiers(isBuggyAttributeStats, classifierList, featureSelectionFilters, projectClassifiersList);
    }

        return projectClassifiersList;
    }

    /**
     * Get a Forward selection, a Ranker and a Best first Attribute selection filters
     * @return a list of the attribute selection filters
     */
    private List<AttributeSelection> getFeatureSelectionFilters() {

        AttributeSelection bestFirstAS = new AttributeSelection();
        BestFirst bestFirst = new BestFirst();
        bestFirst.setDirection(new SelectedTag(2, bestFirst.getDirection().getTags()));
        bestFirstAS.setSearch(bestFirst);

        return new ArrayList<>(List.of(bestFirstAS));
    }

    /**
     * Adds to the projClassifierList the classifiers in classifierList with all the infos about them
     * @param classifierList List of models
     * @param projectClassifiersList List of models with all their useful infos
     */
    private void createSimpleClassifiers(List<Classifier> classifierList, List<ProjectClassifier> projectClassifiersList) {
        for (Classifier classifier : classifierList) {
            projectClassifiersList.add(
                    new ProjectClassifier(classifier, classifier.getClass().getSimpleName(), NO_SELECTION, NO_SAMPLING, false));
        }
    }

    /**
     * Adds to the projClassifierList the classifiers in classifierList with the feature selection and all the infos about them
     * @param classifierList List of models
     * @param featureSelectionFilters Filters of feature selection
     * @param projectClassifiersList List of models with all their useful infos
     */
    private void createFeatureSelectedClassifiers(List<Classifier> classifierList, List<AttributeSelection> featureSelectionFilters, List<ProjectClassifier> projectClassifiersList) {
        for (AttributeSelection featureSelectionFilter : featureSelectionFilters) {
            for (Classifier classifier : classifierList) {
                FilteredClassifier filteredClassifier = new FilteredClassifier();
                filteredClassifier.setClassifier(classifier);
                filteredClassifier.setFilter(featureSelectionFilter);

                projectClassifiersList.add(new ProjectClassifier(filteredClassifier, classifier.getClass().getSimpleName(),
                        featureSelectionFilter.getSearch().getClass().getSimpleName(), NO_SAMPLING, false));
            }
        }
    }

    /**
     * Adds to the projClassifierList the classifiers in classifierList adding cost-sensitiveness and all the infos about them
     * @param classifierList List of models
     * @param projectClassifiersList List of models with all their useful infos
     */
    private void createCostSensitiveClassifiers(List<Classifier> classifierList, List<ProjectClassifier> projectClassifiersList) {
        for (Classifier classifier : classifierList) {
            List<CostSensitiveClassifier> costSensitiveFilters = getCostSensitiveFilters();
            for (CostSensitiveClassifier costSensitiveClassifier : costSensitiveFilters) {
                costSensitiveClassifier.setClassifier(classifier);

                projectClassifiersList.add(new ProjectClassifier(costSensitiveClassifier, classifier.getClass().getSimpleName(),
                        NO_SELECTION, NO_SAMPLING, true));
            }
        }
    }

    /**
     * Adds to the projClassifierList the classifiers in classifierList with the feature selection, the cost
     * sensitiveness and all the infos about them
     * @param classifierList List of models
     * @param featureSelectionFilters Filters of feature selection
     * @param projectClassifiersList List of models with all their useful infos
     */
    private void createFeatureSelectedAndCostSensitiveClassifiers(List<Classifier> classifierList, List<AttributeSelection> featureSelectionFilters,
                                                                  List<ProjectClassifier> projectClassifiersList) {
        for (Classifier classifier : classifierList) {
            List<CostSensitiveClassifier> costSensitiveFilters = getCostSensitiveFilters();
            for(CostSensitiveClassifier costSensitiveClassifier: costSensitiveFilters){
                costSensitiveClassifier.setClassifier(classifier);
                for (AttributeSelection featureSelectionFilter : featureSelectionFilters) {
                    FilteredClassifier filteredCostSensitiveClassifier = new FilteredClassifier();
                    filteredCostSensitiveClassifier.setFilter(featureSelectionFilter);
                    filteredCostSensitiveClassifier.setClassifier(costSensitiveClassifier);

                    projectClassifiersList.add(new ProjectClassifier(filteredCostSensitiveClassifier, classifier.getClass().getSimpleName(),
                            featureSelectionFilter.getSearch().getClass().getSimpleName(), NO_SAMPLING, true));
                }
            }
        }
    }

    private List<CostSensitiveClassifier> getCostSensitiveFilters() {
        CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
        costSensitiveClassifier.setMinimizeExpectedCost(false);
        CostMatrix costMatrix = getCostMatrix();
        costSensitiveClassifier.setCostMatrix(costMatrix);
        return new ArrayList<>(List.of(costSensitiveClassifier));
    }

    private CostMatrix getCostMatrix() {
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, falsePositiveWeight);
        costMatrix.setCell(0, 1, falseNegativeWeight);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }

    private void createSamplingClassifiers(AttributeStats isBuggyAttributeStats, List<Classifier> classifierList,
                                           List<AttributeSelection> featureSelectionFilters,
                                           List<ProjectClassifier> projectClassifiersList) {
        int majorityClassSize = isBuggyAttributeStats.nominalCounts[1];
        int minorityClassSize = isBuggyAttributeStats.nominalCounts[0];
        List<Filter> samplingFilters = getSamplingFilters(majorityClassSize, minorityClassSize);

        // ONLY SAMPLING
        onlySamplingClassifiers(classifierList, samplingFilters, projectClassifiersList);

        // FEATURE SELECTION AND SAMPLING
        featureSelectionAndSamplingClassifiers(classifierList, featureSelectionFilters, samplingFilters, projectClassifiersList);

        // COST SENSITIVITY AND SAMPLING
        costSensitiveAndSamplingClassifiers(classifierList, samplingFilters, projectClassifiersList);

        // FEATURE SELECTION, SAMPLING, AND COST SENSITIVITY
        allCombinationClassifiers(classifierList, featureSelectionFilters, samplingFilters, projectClassifiersList);
    }

    private List<Filter> getSamplingFilters(int majorityClassSize, int minorityClassSize) {
        double percentSMOTE;
        if(minorityClassSize==0 || minorityClassSize > majorityClassSize){
            percentSMOTE = 0;
        }else{
            percentSMOTE = (100.0*(majorityClassSize-minorityClassSize))/minorityClassSize;
        }
        List<Filter> filterList = new ArrayList<>();

        SMOTE smote = new SMOTE();
        smote.setClassValue("1");
        smote.setPercentage(percentSMOTE);
        filterList.add(smote);
        return filterList;
    }

    private void onlySamplingClassifiers(List<Classifier> classifierList, List<Filter> samplingFilters, List<ProjectClassifier> projectClassifiersList) {
        for (Filter samplingFilter : samplingFilters) {
            for (Classifier classifier : classifierList) {
                FilteredClassifier filteredClassifier = new FilteredClassifier();
                filteredClassifier.setClassifier(classifier);
                filteredClassifier.setFilter(samplingFilter);

                projectClassifiersList.add(new ProjectClassifier(filteredClassifier, classifier.getClass().getSimpleName(),NO_SELECTION, samplingFilter.getClass().getSimpleName(), false));
            }
        }
    }

    private void featureSelectionAndSamplingClassifiers(List<Classifier> classifierList, List<AttributeSelection> featureSelectionFilters, List<Filter> samplingFilters, List<ProjectClassifier> projectClassifiersList) {
        for (AttributeSelection featureSelectionFilter : featureSelectionFilters) {
            for (Filter samplingFilter : samplingFilters) {
                for (Classifier classifier : classifierList) {

                    FilteredClassifier innerClassifier = new FilteredClassifier();
                    innerClassifier.setClassifier(classifier);
                    innerClassifier.setFilter(featureSelectionFilter);

                    FilteredClassifier externalClassifier = new FilteredClassifier();
                    externalClassifier.setFilter(samplingFilter);
                    externalClassifier.setClassifier(innerClassifier);

                    projectClassifiersList.add(new ProjectClassifier(externalClassifier, classifier.getClass().getSimpleName(),
                            featureSelectionFilter.getSearch().getClass().getSimpleName(), samplingFilter.getClass().getSimpleName(), false));
                }
            }
        }
    }

    private void costSensitiveAndSamplingClassifiers(List<Classifier> classifierList, List<Filter> samplingFilters,
                                                     List<ProjectClassifier> projectClassifiersList) {
        List<CostSensitiveClassifier> costSensitiveFilters = getCostSensitiveFilters();
        for (Classifier classifier : classifierList) {
            for (Filter samplingFilter : samplingFilters) {
                for(CostSensitiveClassifier costSensitiveClassifier: costSensitiveFilters){
                    FilteredClassifier innerClassifier = new FilteredClassifier();
                    innerClassifier.setClassifier(classifier);
                    innerClassifier.setFilter(samplingFilter);

                    costSensitiveClassifier.setClassifier(innerClassifier);
                    projectClassifiersList.add(new ProjectClassifier(costSensitiveClassifier, classifier.getClass().getSimpleName(),
                            NO_SELECTION, samplingFilter.getClass().getSimpleName(), true));

                }
            }
        }
    }

    private void allCombinationClassifiers(List<Classifier> classifierList, List<AttributeSelection> featureSelectionFilters,
                                           List<Filter> samplingFilters, List<ProjectClassifier> projectClassifiersList) {
        List<CostSensitiveClassifier> costSensitiveFilters = getCostSensitiveFilters();
        for (Classifier classifier : classifierList) {
            for (Filter samplingFilter : samplingFilters) {
                for (AttributeSelection featureSelectionFilter : featureSelectionFilters) {
                    for (CostSensitiveClassifier costSensitiveClassifier : costSensitiveFilters) {
                        FilteredClassifier innerClassifier = new FilteredClassifier();
                        innerClassifier.setClassifier(classifier);
                        innerClassifier.setFilter(featureSelectionFilter);

                        FilteredClassifier externalClassifier = new FilteredClassifier();
                        externalClassifier.setFilter(samplingFilter);
                        externalClassifier.setClassifier(innerClassifier);

                        costSensitiveClassifier.setClassifier(externalClassifier);
                        projectClassifiersList.add(new ProjectClassifier(costSensitiveClassifier, classifier.getClass().getSimpleName(),
                                featureSelectionFilter.getSearch().getClass().getSimpleName(), samplingFilter.getClass().getSimpleName(), true));
                    }
                }
            }
        }
    }
}
