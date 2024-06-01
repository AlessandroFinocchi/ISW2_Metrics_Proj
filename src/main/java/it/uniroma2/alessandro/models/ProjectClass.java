package it.uniroma2.alessandro.models;

import java.util.ArrayList;
import java.util.List;

public class ProjectClass {
    private final String name;
    private final String contentOfClass;
    private final Release release;
    private final MetricList metrics;
    private final List<Commit> touchingClassCommitList;
    private final List<Integer> addedLOCList;
    private final List<Integer> removedLOCList;

    public ProjectClass(String name, String contentOfClass, Release release) {
        this.name = name;
        this.contentOfClass = contentOfClass;
        this.release = release;
        metrics = new MetricList();
        touchingClassCommitList = new ArrayList<>();
        addedLOCList = new ArrayList<>();
        removedLOCList = new ArrayList<>();
    }

    public List<Commit> getTouchingClassCommitList() {
        return touchingClassCommitList;
    }

    public void addTouchingClassCommit(Commit commit) {
        this.touchingClassCommitList.add(commit);
    }

    public Release getRelease() {
        return release;
    }

    public String getContentOfClass() {
        return contentOfClass;
    }

    public String getName() {
        return name;
    }

    public MetricList getMetrics() {
        return metrics;
    }

    public List<Integer> getAddedLOCList() {
        return addedLOCList;
    }

    public void addAddedLOC(Integer lOCAddedByEntry) {
        addedLOCList.add(lOCAddedByEntry);
    }

    public List<Integer> getRemovedLOCList() {
        return removedLOCList;
    }

    public void addRemovedLOC(Integer lOCRemovedByEntry) {
        removedLOCList.add(lOCRemovedByEntry);
    }

}
