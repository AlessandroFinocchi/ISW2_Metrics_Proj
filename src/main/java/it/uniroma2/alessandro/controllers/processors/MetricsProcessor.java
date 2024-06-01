package it.uniroma2.alessandro.controllers.processors;

import it.uniroma2.alessandro.controllers.scrapers.GitScraper;
import it.uniroma2.alessandro.models.Commit;
import it.uniroma2.alessandro.models.LOCMetrics;
import it.uniroma2.alessandro.models.ProjectClass;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MetricsProcessor {
    List<Commit> ticketedCommitList;
    List<ProjectClass> classList;
    GitScraper gitScraper;

    public MetricsProcessor(List<Commit> ticketedCommitList, List<ProjectClass> classList, GitScraper gitScraper) {
        this.ticketedCommitList = ticketedCommitList;
        this.classList = classList;
        this.gitScraper = gitScraper;
    }

    public void processMetrics() throws IOException {
        processSize();
        processNumberOfRevisions();
        processNumberOfDefectFixes();
        processNumberOfAuthors();
        processLOCMetrics();
    }

    private void processSize() {
        for (ProjectClass currentClass : classList) {
            String[] lines = currentClass.getContentOfClass().split("\r\n|\r|\n");
            currentClass.getMetrics().setSize(lines.length);
        }
    }

    private void processNumberOfRevisions() {
        for (ProjectClass currentClass : classList) {
            currentClass.getMetrics().setNumberOfRevisions(currentClass.getTouchingClassCommitList().size());
        }
    }

    // todo: check if ok
    /**
     * For each class of the project in the classList computes the number of fixes as the number of commits that
     * touches the class
     */
    private void processNumberOfDefectFixes() {
        int numberOfFixes;
        for (ProjectClass projectClass : classList) {
            numberOfFixes = 0;
            for (Commit touchingClassCommit : projectClass.getTouchingClassCommitList()) {
                if (ticketedCommitList.contains(touchingClassCommit)) {
                    numberOfFixes++;
                }
            }
            projectClass.getMetrics().setNumberOfDefectFixes(numberOfFixes);
        }
    }

    private void processNumberOfAuthors() {
        for (ProjectClass projectClass : classList) {
            List<String> authorsOfClass = new ArrayList<>();
            for (Commit commit : projectClass.getTouchingClassCommitList()) {
                RevCommit revCommit = commit.getRevCommit();
                if (!authorsOfClass.contains(revCommit.getAuthorIdent().getName())) {
                    authorsOfClass.add(revCommit.getAuthorIdent().getName());
                }
            }
            projectClass.getMetrics().setNumberOfAuthors(authorsOfClass.size());
        }
    }

    private void processLOCMetrics() throws IOException {
        // metrics max, avg and actual value are zero by default
        LOCMetrics removedLOC = new LOCMetrics();
        LOCMetrics churnLOC = new LOCMetrics();
        LOCMetrics addedLOC = new LOCMetrics();
        LOCMetrics touchedLOC = new LOCMetrics();
        int i;
        for (ProjectClass currentClass : classList) {
            // Set the added and removed metric LOCs for every class
            gitScraper.extractAddedAndRemovedLOC(currentClass);

            List<Integer> locAddedByClass = currentClass.getAddedLOCList();
            List<Integer> locRemovedByClass = currentClass.getRemovedLOCList();

            // The sizes of LOC added and LOC removed are the same
            for (i = 0; i < locAddedByClass.size(); i++) {

                int addedLineOfCode = locAddedByClass.get(i);
                int removedLineOfCode = locRemovedByClass.get(i);
                int churningFactor = Math.abs(locAddedByClass.get(i) - locRemovedByClass.get(i));
                int touchedLinesOfCode = locAddedByClass.get(i) + locRemovedByClass.get(i);

                // Set the values
                addedLOC.addToVal(addedLineOfCode);
                removedLOC.addToVal(removedLineOfCode);
                churnLOC.addToVal(churningFactor);
                touchedLOC.addToVal(touchedLinesOfCode);

                // Set the max values
                if (addedLineOfCode > addedLOC.getMaxVal()) {
                    addedLOC.setMaxVal(addedLineOfCode);
                }
                if (removedLineOfCode > removedLOC.getMaxVal()) {
                    removedLOC.setMaxVal(removedLineOfCode);
                }
                if (churningFactor > churnLOC.getMaxVal()) {
                    churnLOC.setMaxVal(churningFactor);
                }
                if (touchedLinesOfCode > touchedLOC.getMaxVal()) {
                    touchedLOC.setMaxVal(touchedLinesOfCode);
                }
            }

            processAverageLOCMetrics(currentClass, locAddedByClass, locRemovedByClass,
                    addedLOC, removedLOC, churnLOC, touchedLOC);
        }
    }

    private void processAverageLOCMetrics(ProjectClass currentClass, List<Integer> locAddedByClass, List<Integer> locRemovedByClass,
                                          LOCMetrics addedLOC, LOCMetrics removedLOC, LOCMetrics churnLOC, LOCMetrics touchedLOC){
        // Set the average values
        int nRevisions = currentClass.getMetrics().getNumberOfRevisions();
        if(!locAddedByClass.isEmpty()) {
            addedLOC.setAvgVal(1.0* addedLOC.getVal()/ nRevisions);
        }
        if(!locRemovedByClass.isEmpty()) {
            removedLOC.setAvgVal(1.0* removedLOC.getVal()/ nRevisions);
        }
        if(!locAddedByClass.isEmpty() || !locRemovedByClass.isEmpty()) {
            churnLOC.setAvgVal(1.0* churnLOC.getVal()/ nRevisions);
            touchedLOC.setAvgVal(1.0* touchedLOC.getVal()/nRevisions);
        }
        currentClass.getMetrics().setAddedLOCMetrics(addedLOC.getVal(), addedLOC.getMaxVal(), addedLOC.getAvgVal());
        currentClass.getMetrics().setRemovedLOCMetrics(removedLOC.getVal(), removedLOC.getMaxVal(), removedLOC.getAvgVal());
        currentClass.getMetrics().setChurnMetrics(churnLOC.getVal(), churnLOC.getMaxVal(), churnLOC.getAvgVal());
        currentClass.getMetrics().setTouchedLOCMetrics(touchedLOC.getVal(), touchedLOC.getMaxVal(), touchedLOC.getAvgVal());
    }
}
