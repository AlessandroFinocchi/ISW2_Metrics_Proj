package it.uniroma2.alessandro.models;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.Date;

public class Commit {
    private final RevCommit revCommit;
    private Ticket ticket;
    private final Release release;

    private final String message;
    private final Date date;

    public Commit(RevCommit revCommit, Release release) {
        this.revCommit = revCommit;
        this.release = release;
        this.message = revCommit.getFullMessage();
        this.date = revCommit.getCommitterIdent().getWhen();
        ticket = null;
    }

    public RevCommit getRevCommit() {
        return revCommit;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Release getRelease() {
        return release;
    }
}
