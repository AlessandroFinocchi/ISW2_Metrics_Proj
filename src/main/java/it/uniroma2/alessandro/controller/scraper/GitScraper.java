package it.uniroma2.alessandro.controller.scraper;

import it.uniroma2.alessandro.model.Commit;
import it.uniroma2.alessandro.model.Release;
import it.uniroma2.alessandro.model.ReleaseList;
import it.uniroma2.alessandro.model.Ticket;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class GitScraper {
    private static final Logger logger = Logger.getLogger(GitScraper.class.getName());
    private static final String CLONE_DIR = "repos/";

    private final String projName;
    private final String projRepoUrl;
    protected final Git git;
    private final Repository repository;

    public GitScraper(String projName, String projRepoUrl, ReleaseList jiraReleases) throws IOException, GitAPIException {
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

    public List<Commit> scrapeCommits(ReleaseList jiraReleases) throws GitAPIException, IOException {
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
        for (RevCommit revCommit : revCommitList) {
            // Get the date of a commit
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            LocalDate commitDate = LocalDate.parse(formatter.format(revCommit.getCommitterIdent().getWhen()));
            LocalDate previusReleaseDate = LocalDate.parse(formatter.format(new Date(0))); // lowerBoundDate = 01/01/1970
            for(Release release: jiraReleases.getReleaseList()){
                // Get the date of a release
                LocalDate nextReleaseDate = release.getReleaseDateTime();
                // If a commit date is after the last release date considered and the next one being considered add it
                // to the next one being considered
                if (commitDate.isAfter(previusReleaseDate) && !commitDate.isAfter(nextReleaseDate)) {
                    Commit newCommit = new Commit(revCommit, release);
                    commitList.add(newCommit);
                    release.addCommit(newCommit);
                }
                // Update the last release date to be the one currently considered in order to
                // go on with the next release to consider
                previusReleaseDate = nextReleaseDate;
            }
        }

        // Remove a release if it hasn't got any commit
        jiraReleases.getReleaseList().removeIf(release -> release.getCommitList().isEmpty());

        // Order commits by date
        commitList.sort(Comparator.comparing(commit -> commit.getRevCommit().getCommitTime()));

        return commitList;

    }

    /***
     * Filter commits that have a ticket id inside their message, setting the ticket of a commit and the list of
     * commits for each ticket, end eventually deletes ticket without any commit
     * @param commitList commits to filter
     * @param ticketList tickets to take ids from
     * @return a list of commits that reference a ticket
     */
    public List<Commit> filterCommits(List<Commit> commitList, List<Ticket> ticketList) {
        List<Commit> filteredCommitList = new ArrayList<>();
        for (Commit commit : commitList) {
            for (Ticket ticket : ticketList) {
                String commitFullMessage = commit.getRevCommit().getFullMessage();
                String ticketKey = ticket.getTicketKey();
                if (matchRegex(commitFullMessage, ticketKey)) { //matchRegex(commitFullMessage, ticketKey)
                    filteredCommitList.add(commit);
                    ticket.addCommit(commit);
                    commit.setTicket(ticket);
                }
            }
        }

        // If a ticket has no commits it means it isn't solved, so we don't care about it
        ticketList.removeIf(ticket -> ticket.getCommitList().isEmpty());
        return filteredCommitList;
    }

    private boolean matchRegex(String s, String regex){
        Pattern pattern = Pattern.compile(regex + "\\b");
        return pattern.matcher(s).find();
    }
}
