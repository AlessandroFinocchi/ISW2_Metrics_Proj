package it.uniroma2.alessandro.controllers.scrapers;

import it.uniroma2.alessandro.exceptions.ReleaseNotFoundException;
import it.uniroma2.alessandro.models.Commit;
import it.uniroma2.alessandro.models.ProjectClass;
import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.models.Ticket;
import it.uniroma2.alessandro.utilities.PropertyUtility;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class GitScraper {
    public static final String CLONE_DIR = "repos/";
    public static final String FAKE_RELEASE_PREFIX = "fake_release_";

    protected final Git git;
    private final Repository repository;

    private Release lastRelease = null;

    public GitScraper(String projName, String projRepoUrl) throws IOException, GitAPIException {
        String filename = CLONE_DIR + projName.toLowerCase() + "Clone";

        // Cloning repo and setting up instance properties: git clone <projRepoUrl>
        File directory = new File(filename);
        if(directory.exists()){
            repository = new FileRepositoryBuilder()
                    .setGitDir(new File(filename, ".git"))
                    .build();
            git = new Git(repository);
        }else{
            git = Git.cloneRepository()
                    .setURI(projRepoUrl)
                    .setDirectory(directory).call();
            repository = git.getRepository();
        }
    }

    public void checkoutSpecificRelease(Release release) throws GitAPIException {
        checkoutSpecificTag("release-" + release.getReleaseName());
    }

    private void checkoutSpecificTag(String tagName) throws GitAPIException {
        git.checkout().setName(tagName).call();
    }

    public void checkoutSpecificCommit(Commit commit) throws GitAPIException {
        git.checkout().setName(commit.getRevCommit().getName()).call();
    }

    public void checkoutLastRelease() throws GitAPIException {
        if(lastRelease == null) throw new IllegalArgumentException("Last release not set");
        checkoutSpecificTag("release-" + lastRelease.getReleaseName());
    }

    public List<Commit> scrapeCommits(List<Release> jiraReleases) throws GitAPIException, IOException {
        // All the commits extracted
        List<RevCommit> revCommitList = new ArrayList<>();

        // All useful information about all the commits
        List<Commit> commitList = new ArrayList<>();

        // Get list of branches: git branch -a
        List<Ref> branchList = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

        // Get all commits from all branches
        for (Ref branch : branchList) {
            String branchName = branch.getName();

            // Get list of commits: git log <branchName>
            Iterable<RevCommit> commits = git.log().add(repository.resolve(branchName)).call();
            for(RevCommit commit : commits){
                if(!revCommitList.contains(commit)) {
                    revCommitList.add(commit);
                }
            }
        }

        // Sort all the commits using the commit time
        revCommitList.sort(Comparator.comparing(RevCommit::getCommitTime));

        // Take the date of the first and last commit
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // Add releases until last commit
        boolean useFakeReleases = PropertyUtility.readBooleanProperty("USING_FAKE_RELEASES");
        if(useFakeReleases)
            completeReleaseList(formatter, revCommitList, jiraReleases);

        // Set all the commits for a release and set the release of a commit
        for (RevCommit revCommit : revCommitList) {
            // Get the date of a commit
            LocalDate commitDate = LocalDate.parse(formatter.format(revCommit.getCommitterIdent().getWhen()));
            LocalDate previusReleaseDate = LocalDate.parse(formatter.format(new Date(0))); // lowerBoundDate = 01/01/1970
            for(Release release: jiraReleases){
                // Get the date of a release
                LocalDate nextReleaseDate = release.getReleaseDateTime();
                // If a commit date is after the last release date considered and the next one being considered add it
                // to the next one being considered
                if (commitDate.isAfter(previusReleaseDate) && !commitDate.isAfter(nextReleaseDate)) {
                    Commit newCommit = new Commit(revCommit, release);
                    commitList.add(newCommit);
                    release.addCommit(newCommit);
                    break;
                }
                // Update the last release date to be the one currently considered in order to
                // go on with the next release to consider
                previusReleaseDate = nextReleaseDate;
            }
        }

        // Remove a release if it hasn't got any commit
        jiraReleases.removeIf(release -> release.getCommitList().isEmpty());

        // Order commits by date
        commitList.sort(Comparator.comparing(commit -> commit.getRevCommit().getCommitTime()));

        return commitList;

    }

    private void completeReleaseList(SimpleDateFormat formatter, List<RevCommit> revCommitList, List<Release> jiraReleases){
        // Get the dates
        LocalDate firstCommitDate = LocalDate.parse(formatter.format(revCommitList.getFirst().getCommitterIdent().getWhen()));
        LocalDate lastCommitDate = LocalDate.parse(formatter.format(revCommitList.getLast().getCommitterIdent().getWhen()));
        LocalDate lastReleaseDate = jiraReleases.getLast().getReleaseDateTime();

        // Get the time interval between the first commit and the last release
        long interval = ChronoUnit.DAYS.between(firstCommitDate, lastReleaseDate);

        // Get the average number of days between 2 releases
        int intervalInDays = (int) interval / jiraReleases.size();

        // Add a new dummy release, once every releaseIntervalInDays, in order to not discard commits after the last release
        LocalDate currentDate = jiraReleases.getLast().getReleaseDateTime().plusDays(intervalInDays);
        while(currentDate.isBefore(lastCommitDate)){
            jiraReleases.add(new Release(
                    FAKE_RELEASE_PREFIX + currentDate.toString(),
                    FAKE_RELEASE_PREFIX + currentDate.toString(),
                    currentDate.toString()));
            currentDate = currentDate.plusDays(intervalInDays);
        }
    }

    public List<ProjectClass> scrapeClasses(List<Release> releaseList,
                                            List<Commit> commitList) throws IOException {
        List<ProjectClass> classList = new ArrayList<>();
        List<Commit> lastCommitsList = new ArrayList<>();

        // For each release we want to take all its classes, so we check their last commit
        for (Release release : releaseList) {
            // Order every commit list for each release
            release.getCommitList().sort(Comparator.comparing(commit -> commit.getRevCommit().getCommitterIdent().getWhen()));
            lastCommitsList.add(release.getCommitList().getLast());
        }

        // For each last commit of each release...
        for(Commit lastCommit: lastCommitsList){
            // Get a map of class name to class code for the actual release
            Map<String, String> classesNameCodeMap = getClassesNameCodeInfos(lastCommit.getRevCommit());
            for(Map.Entry<String, String> classInfo : classesNameCodeMap.entrySet()){
                classList.add(new ProjectClass(classInfo.getKey(), classInfo.getValue(), lastCommit.getRelease()));
            }
        }

        // Set the commit list that touches the class for each class
        setTouchingClassesCommits(classList, commitList);

        // Order classes by name
        classList.sort(Comparator.comparing(ProjectClass::getName));

        return classList;
    }

    /**
     * From a commit takes the class name and code, excluding the test classes
     * @param revCommit the commit to take the classes from
     * @return a map of strings, the class names, to strings, the class code
     * @throws IOException for using jGit apis
     */
    private Map<String, String> getClassesNameCodeInfos(RevCommit revCommit) throws IOException {
        Map<String, String> allClasses = new HashMap<>();
        RevTree tree = revCommit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        while(treeWalk.next()) {
            if(treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/src/test/")) {
                allClasses.put(treeWalk.getPathString(), new String(repository.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8));
            }
        }
        treeWalk.close();
        return allClasses;
    }

    /**
     * Initialize buggyness to false, gets the commits that touch each class and with them sets the buggy attribute
     * @param ticketList the tickets where taking information
     * @param classList the classes to set information
     */
    public void completeClassesInfo(List<Ticket> ticketList, List<ProjectClass> classList) throws IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // Initialize buggyness to false
        for(ProjectClass projectClass : classList){
            projectClass.getMetrics().setBuggyness(false);
        }

        // For each ticket get its commits and the IV
        for(Ticket ticket: ticketList) {
            List<Commit> ticketCommits = ticket.getCommitList();
            Release injectedVersion = ticket.getInjectedVersion();

            // For each commit of the ticket
            for (Commit commit : ticketCommits) {
                RevCommit revCommit = commit.getRevCommit();
                LocalDate commitDate = LocalDate.parse(formatter.format(revCommit.getCommitterIdent().getWhen()));

                // Get the name list of classes touched by the commit
                List<String> modifiedClassesNames = getTouchedClassesNames(revCommit);

                // If the commit date is between the ticket creation and resolution date then it is valid
                if (!commitDate.isAfter(ticket.getResolutionDate()) && !commitDate.isBefore(ticket.getCreationDate())) {
                    // Get the release of that commit
                    Release releaseOfCommit = commit.getRelease();
                    for (String modifiedClass : modifiedClassesNames) {
                        // Set the buggyness of each class
                        labelBuggyClasses(modifiedClass, injectedVersion, releaseOfCommit, classList);
                    }
                }
            }
        }
    }

    /**
     * Get the class names touched by a commit
     * @param commit the commit that touches the classes
     * @return a list of touched class names
     * @throws IOException if there is some failure reading the classes
     */
    private List<String> getTouchedClassesNames(RevCommit commit) throws IOException  {
        List<String> touchedClassesNames = new ArrayList<>();

        // The diff formatter will format the differences between 2 commits
        try(DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            ObjectReader reader = repository.newObjectReader()) {
            RevCommit commitParent = commit.getParent(0);
            diffFormatter.setRepository(repository);

            // Get the current commit tree
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            ObjectId newTree = commit.getTree();
            newTreeIter.reset(reader, newTree);

            // Get the parent commit tree of the current one
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            ObjectId oldTree = commitParent.getTree();
            oldTreeIter.reset(reader, oldTree);

            // Get the names
            List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);
            for(DiffEntry entry : entries) {
                if(entry.getNewPath().contains(".java") && !entry.getNewPath().contains("/src/test/")) {
                    touchedClassesNames.add(entry.getNewPath());
                }
            }
        } catch(ArrayIndexOutOfBoundsException ignored) {
            // ignoring when no parent is found
        }
        return touchedClassesNames;
    }

    /**
     * Sets as buggy the classes between the IV and FV of a bug
     * @param modifiedClass The name of the modified class
     * @param injectedVersion The IV release
     * @param fixedVersion The FV release
     * @param classList All the classes of the project
     */
    private static void labelBuggyClasses(String modifiedClass, Release injectedVersion,
                                          Release fixedVersion, List<ProjectClass> classList) {
        for(ProjectClass projectClass: classList){
            if(// Get the class named correctly
                !projectClass.getName().equals(modifiedClass) ||
                // Check that the class release is before the FV
                projectClass.getRelease().getReleaseDateTime().isAfter(fixedVersion.getReleaseDateTime()) ||
                // Check that the class release is after the IV
                projectClass.getRelease().getReleaseDateTime().isBefore(injectedVersion.getReleaseDateTime())
            ) continue;

            // Then the class is buggy
            projectClass.getMetrics().setBuggyness(true);
        }
    }

    /**
     * Adds each commit to the touching commit list of each class
     * @param classList list of classes to set the touching commit list
     * @param commitList list of commits that touches the classes in classList
     * @throws IOException if there is any failure taking the touched class names
     */
    private void setTouchingClassesCommits(List<ProjectClass> classList, List<Commit> commitList) throws IOException {
        List<ProjectClass> tempProjClasses;

        for(Commit commit: commitList){
            Release release = commit.getRelease();
            tempProjClasses = new ArrayList<>(classList);

            // Get the class list containing only the class of the current commit release
            tempProjClasses.removeIf(tempProjClass -> !tempProjClass.getRelease().equals(release));

            // Get the classes modified by the current commit
            List<String> modifiedClassesNames = getTouchedClassesNames(commit.getRevCommit());

            // For each class touched by the current commit, add the commit to its touching commit list
            for(String modifiedClass: modifiedClassesNames){
                for(ProjectClass projectClass: tempProjClasses){
                    if(projectClass.getName().equals(modifiedClass) && !projectClass.getTouchingClassCommitList().contains(commit)) {
                        projectClass.addTouchingClassCommit(commit);
                    }
                }
            }
        }
    }

    /**
     * Set for every class the added LOC and removed LOC
     * @param projectClass the classes to set the LOCs metrics
     * @throws IOException in case of failures by the diff formatter
     */
    public void extractAddedAndRemovedLOC(ProjectClass projectClass) throws IOException {
        for(Commit commit : projectClass.getTouchingClassCommitList()) {
            RevCommit revCommit = commit.getRevCommit();

            // Get the diff formatter with the output stream disabled because they don't need to be printed anywhere
            try(DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

                // Get the first parent
                RevCommit parentCommit = revCommit.getParent(0);
                diffFormatter.setRepository(repository);

                // The default comparator compares the text without any special treatment
                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);

                // Get the differences between the files
                List<DiffEntry> diffEntries = diffFormatter.scan(parentCommit.getTree(), revCommit.getTree());
                for(DiffEntry diffEntry : diffEntries) {
                    if(diffEntry.getNewPath().equals(projectClass.getName())) {
                        projectClass.addAddedLOC(getAddedLines(diffFormatter, diffEntry));
                        projectClass.addRemovedLOC(getDeletedLines(diffFormatter, diffEntry));
                    }
                }
            } catch(ArrayIndexOutOfBoundsException ignored) {
                //ignoring when no parent is found
            }
        }
    }

    private int getAddedLines(DiffFormatter diffFormatter, DiffEntry entry) throws IOException {
        int addedLines = 0;
        for(Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
            addedLines += edit.getEndB() - edit.getBeginB();
        }
        return addedLines;
    }

    private int getDeletedLines(DiffFormatter diffFormatter, DiffEntry entry) throws IOException {
        int deletedLines = 0;
        for(Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
            deletedLines += edit.getEndA() - edit.getBeginA();
        }
        return deletedLines;
    }

    public LocalDate getReleaseDateFromGithub(String releaseName) throws GitAPIException, ReleaseNotFoundException, IOException {
        RevWalk walk = new RevWalk(repository);
        List<Ref> refs = git.tagList().call();
        for(Ref ref: refs) {
            if(ref.getName().equals("refs/tags/release-" + releaseName)){
                return walk.parseTag(ref.getObjectId()).getTaggerIdent().getWhen().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }
        throw new ReleaseNotFoundException();
    }

    public void setLastRelease(Release lastRelease) {
        this.lastRelease = lastRelease;
    }

    /**
     * Removes all the releases in the list that are not present on the relative GitHub repository, and set the
     * numeric ID for the remaining ones
     * @param releases list of jira release
     * @throws GitAPIException in case the checkout command doesn't work
     */
    public void filterTaggedReleases(List<Release> releases) throws GitAPIException {
        List<Ref> tags = git.tagList().call();
        List<String> tagNames = tags.stream().map(Ref::getName).toList();
        List<Release> releaseToRemove = new ArrayList<>();

        for(Release release: releases) {
            if(!tagNames.contains("refs/tags/release-" + release.getReleaseName()))
                releaseToRemove.add(release);
        }

        releases.removeAll(releaseToRemove);
    }
}
