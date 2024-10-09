package it.uniroma2.alessandro.controllers.processors.metrics;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKNotifier;
import com.github.mauricioaniche.ck.ResultWriter;
import it.uniroma2.alessandro.controllers.scrapers.GitScraper;
import it.uniroma2.alessandro.models.Release;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static it.uniroma2.alessandro.controllers.scrapers.GitScraper.CLONE_DIR;
import static it.uniroma2.alessandro.controllers.scrapers.GitScraper.FAKE_RELEASE_PREFIX;
import static it.uniroma2.alessandro.controllers.scrapers.MetricsScraper.RESULT_DIRECTORY_NAME;

public class ComplexityMetricsProcessor {
    private final Logger logger = Logger.getLogger(ComplexityMetricsProcessor.class.getName());

    private final String projName;
    private final String projDirectory;
    private final List<Release> releaseList;
    private final GitScraper gitScraper;

    public ComplexityMetricsProcessor(String projName, List<Release> releaseList, GitScraper gitScraper){
        this.projName = projName.toLowerCase();
        this.projDirectory = CLONE_DIR + projName.toLowerCase() + "Clone";
        this.releaseList = releaseList;
        this.gitScraper = gitScraper;
    }

    public void extractComplexityMetrics() throws GitAPIException, IOException {
        for(Release release: releaseList){
            if(release.getReleaseName().contains(FAKE_RELEASE_PREFIX))
                gitScraper.checkoutSpecificCommit(release.getCommitList().getLast());
            else
                gitScraper.checkoutSpecificRelease(release);
            computeComplexityMetrics(release.getNumericID());
        }

        gitScraper.checkoutLastRelease();
    }

    private void computeComplexityMetrics(int releaseNum) throws IOException {
        boolean variablesAndFields = false;
        boolean useJars = false;
        int maxAtOnce = 0;
        String outputDirectory = RESULT_DIRECTORY_NAME + projName + "/complexityFiles/";

        File file = new File(outputDirectory);
        if (!file.exists() && !file.mkdirs())  throw new IOException();

        ResultWriter writer = new ResultWriter(
                outputDirectory + "ClassMetrics" + releaseNum + ".csv",
                outputDirectory + "MethodMetrics" + releaseNum + ".csv",
                outputDirectory + "VariableMetrics" + releaseNum + ".csv",
                outputDirectory + "FieldMetrics" + releaseNum + ".csv",
                variablesAndFields);

        Map<String, CKClassResult> results = new HashMap<>();

        new CK(useJars, maxAtOnce, variablesAndFields).calculate(this.projDirectory, new CKNotifier() {
            @Override
            public void notify(CKClassResult result) {

                // Store the metrics values from each component of the project in a HashMap
                results.put(result.getClassName(), result);
            }

            @Override
            public void notifyError(String sourceFilePath, Exception e) {
                String loggerString = "Error in " + sourceFilePath;
                logger.info(loggerString);
                logger.info(e.getMessage());
            }
        });

        // Write the metrics value of each component in the csv files
        for(Map.Entry<String, CKClassResult> entry : results.entrySet()){
            writer.printResult(entry.getValue());
        }

        writer.flushAndClose();
        String loggerString = "Metrics extracted for release number " + releaseNum;
        logger.info(loggerString);
    }
}
