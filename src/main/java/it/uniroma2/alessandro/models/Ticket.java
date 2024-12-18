package it.uniroma2.alessandro.models;

import it.uniroma2.alessandro.controllers.processors.proportion.IProportionProcessor;
import it.uniroma2.alessandro.factories.ProportionProcessFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class Ticket {
    private final String ticketKey;

    // IV < AVs < FV, IV < OV < FV
    private final LocalDate creationDate;
    private final LocalDate resolutionDate;
    private Release injectedVersion;
    private final Release openingVersion;
    private final Release fixedVersion;
    private List<Release> affectedVersions;
    private final List<Commit> commitList;

    /***
     *
     * @param ticketKey the name of the ticket
     * @param creationDate when the ticket was created
     * @param resolutionDate when the ticket was resolved
     * @param openingVersion the first release affected by the issue ticketed
     * @param fixedVersion the first release no more affected after the OV
     * @param affectedVersions the list of releases affected by the issue ticketed
     */
    public Ticket(String ticketKey, LocalDate creationDate, LocalDate resolutionDate, Release openingVersion,
                  Release fixedVersion, List<Release> affectedVersions) {
        this.ticketKey = ticketKey;
        this.creationDate = creationDate;
        this.resolutionDate = resolutionDate;
        if(affectedVersions.isEmpty()){
            // The tickets with null IV will be the one to predict
            injectedVersion = null;
        }else{
            // IV = AV[0] by definition
            injectedVersion = affectedVersions.getFirst();

        }
        this.openingVersion = openingVersion;
        this.fixedVersion = fixedVersion;
        this.affectedVersions = affectedVersions;
        commitList = new ArrayList<>();
    }

    public Release getInjectedVersion() {
        return injectedVersion;
    }

    public void setInjectedVersion(Release injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    public Release getOV() {
        return openingVersion;
    }

    public Release getFV() {
        return fixedVersion;
    }

    public List<Release> getAffectedVersions() {
        return affectedVersions;
    }

    public void setAffectedVersions(List<Release> affectedVersions) {
        this.affectedVersions = affectedVersions;
    }

    public String getTicketKey() {
        return ticketKey;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void addCommit(Commit newCommit) {
        if(!commitList.contains(newCommit)){
            commitList.add(newCommit);
        }
    }

    public List<Commit> getCommitList(){
        return commitList;
    }

    public LocalDate getResolutionDate() {
        return resolutionDate;
    }

    public boolean isCorrect(){
        return !getAffectedVersions().isEmpty();
    }

    public static void proportionTickets(List<Ticket> ticketsList, List<Release> releaseList, String projName) throws IOException {
        ProportionProcessFactory proportionProcessFactory = new ProportionProcessFactory();
        IProportionProcessor proportionProcessor = proportionProcessFactory.createProportionProcessor();

        proportionProcessor.processProportion(ticketsList, releaseList, projName);
    }

    public Ticket cloneTicketAtRelease(Release release) {
        List<Release> newAffectedVersions = affectedVersions
                .stream()
                .filter(av -> av.getNumericID() <= release.getNumericID())
                .toList();
        Release newFixedVersion = fixedVersion.getNumericID() <= release.getNumericID() ? fixedVersion : null;
        if(newFixedVersion == null) return null;

        return new Ticket(ticketKey, creationDate, resolutionDate, openingVersion, newFixedVersion, newAffectedVersions);
    }
}
