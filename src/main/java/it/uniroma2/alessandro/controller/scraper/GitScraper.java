package it.uniroma2.alessandro.controller.scraper;

import it.uniroma2.alessandro.model.Commit;
import it.uniroma2.alessandro.model.ProjectClass;
import it.uniroma2.alessandro.model.Release;
import it.uniroma2.alessandro.model.Ticket;
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
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class GitScraper {
    private static final Logger logger = Logger.getLogger(GitScraper.class.getName());
    private static final String CLONE_DIR = "repos/";

    private final String projName;
    private final String projRepoUrl;
    protected final Git git;
    private final Repository repository;

    public GitScraper(String projName, String projRepoUrl) throws IOException, GitAPIException {
        String filename = CLONE_DIR + projName.toLowerCase() + "Clone";
        this.projName = projName;
        this.projRepoUrl = projRepoUrl;


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

        // Set all the commits for a release and set the release of a commit
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
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

    /***
     * Filter commits that have a ticket id inside their message, setting the ticket of a commit and the list of
     * commits for each ticket
     * @param commitList commits to filter
     * @param ticketList tickets to take ids from
     * @return a list of commits that reference a ticket
     */
    public List<Commit> filterCommits(List<Commit> commitList, List<Ticket> ticketList) {
        List<Commit> filteredCommitList = new ArrayList<>();
        for (Commit commit : commitList) {
            String commitFullMessage = commit.getRevCommit().getFullMessage();
            for (Ticket ticket : ticketList) {
                String ticketKey = ticket.getTicketKey();
                if (matchRegex(commitFullMessage, ticketKey)) {
                    filteredCommitList.add(commit);
                    ticket.addCommit(commit);
                    commit.setTicket(ticket);
                }
            }
        }
        return filteredCommitList;
    }


    public List<ProjectClass> extractProjectClasses(List<Release> releaseList, List<Ticket> ticketList,
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

        // Complete the project classes infos
        completeClassesInfo(ticketList, classList);

        // Set the commit list that touches the class for each class
        setTouchingClassesCommits(classList, commitList);

        // Order classes by name
        classList.sort(Comparator.comparing(ProjectClass::getName));

        return classList;
    }

    private boolean matchRegex(String s, String regex){
        Pattern pattern = Pattern.compile(regex + "\\b");
        return pattern.matcher(s).find();
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

    /***
     * Get the commits that touch each class and set the buggy attribute
     * @param ticketList the tickets where taking information
     * @param classList the classes to set information
     */
    private void completeClassesInfo(List<Ticket> ticketList, List<ProjectClass> classList) throws IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // For each ticket get its commits and the IV
        for(Ticket ticket: ticketList) {
            List<Commit> ticketCommits = ticket.getCommitList();
            Release injectedVersion = ticket.getInjectedVersion();

            // For each commit of the ticket
            for (Commit commit : ticketCommits) {
                RevCommit revCommit = commit.getRevCommit();
                LocalDate commitDate = LocalDate.parse(formatter.format(revCommit.getCommitterIdent().getWhen()));

                // If the commit date is between the ticket creation and resolution date then it is valid
                if (!commitDate.isAfter(ticket.getResolutionDate()) && !commitDate.isBefore(ticket.getCreationDate())) {
                    // Get a list of touched class names
                    List<String> modifiedClassesNames = getTouchedClassesNames(revCommit);
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
     * @param classList the classes to set the LOCs metrics
     * @throws IOException in case of failures by the diff formatter
     */
    public void extractAddedAndRemovedLOC(ProjectClass classList) throws IOException {
        for(Commit commit : classList.getTouchingClassCommitList()) {
            RevCommit revCommit = commit.getRevCommit();

            // Get the diff formatter with the output stream disabled because they don't need to be printed anywhere
            try(DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

                // Get the first parent
                RevCommit parentComm = revCommit.getParent(0);
                diffFormatter.setRepository(repository);

                // The default comparator compares the text without any special treatment
                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);

                // Get the differences between the files
                List<DiffEntry> diffEntries = diffFormatter.scan(parentComm.getTree(), revCommit.getTree());
                for(DiffEntry diffEntry : diffEntries) {
                    if(diffEntry.getNewPath().equals(classList.getName())) {
                        classList.addAddedLOC(getAddedLines(diffFormatter, diffEntry));
                        classList.addRemovedLOC(getDeletedLines(diffFormatter, diffEntry));
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
}
