package it.uniroma2.alessandro.model;

import it.uniroma2.alessandro.controller.processor.ProportionProcessor;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;

import static java.lang.Math.max;

public class Ticket {
    private final String ticketKey;

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
    public Ticket(String ticketKey, LocalDate creationDate, LocalDate resolutionDate, Release openingVersion, Release fixedVersion, List<Release> affectedVersions) {
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

    public Release getOpeningVersion() {
        return openingVersion;
    }

    public Release getFixedVersion() {
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

    public static List<Ticket> proportionTickets(List<Ticket> ticketsList, List<Release> releaseList, String projName) throws URISyntaxException {
        List<Ticket> ticketsForProportionList = new ArrayList<>();
        List<Ticket> finalTicketList = new ArrayList<>();

        float proportion;

        // todo: check if ok
        // Since to compute the IV of th i-th ticket we use all the preceding tickets with an IV already given by Jira
        // (and not computed) (IV = AV[0]), if the first ticket has no AVs, then it should use the preceding tickets
        // to compute the proportion, but since they don't exist, it's impossible to compute it and then we discard it.
        // This argument is applied to the second, third, forth,... ticket, until we find the first with a list of IV
        LocalDate firstTicketWithIVDate = ticketsList
                .stream()
                .filter(Ticket::isCorrect)
                .toList()
                .getFirst().getResolutionDate();
        ticketsList.removeIf(t -> t.getResolutionDate().isBefore(firstTicketWithIVDate));

        // For each ticket...
        for(Ticket ticket : ticketsList){

            if(finalTicketList.size() == 173){
                System.out.println("wtf");
            }

            // If the ticket has a list of AVs
            if(ticket.isCorrect()){
                ProportionProcessor.computeProportion(ticketsForProportionList, projName, ticket, false);
                ticket.setInjectedVersion(ticket.getAffectedVersions().getFirst());

                // For proportion we use only the tickets with injected version
                // already known, not the one processed through proportion
                ticketsForProportionList.add(ticket);
            }
            // If the ticket doesn't have a list of AVs
            else{

                proportion = ProportionProcessor.computeProportion(ticketsForProportionList, projName, ticket, true);
                computeInjectedVersion(ticket, releaseList, proportion);
                computeAffectedVersionsList(ticket, releaseList);

            }

            finalTicketList.add(ticket);
        }

        finalTicketList.sort(Comparator.comparing(Ticket::getResolutionDate));

        return finalTicketList;
    }

    private boolean isCorrect(){
        return !getAffectedVersions().isEmpty();
    }

    /**
     * Set, given the proportion and the release list, the IV of a ticket
     * @param ticket the ticket to assign the IV to
     * @param releasesList the list to extract the IV from
     * @param proportion the proportion for the formula
     */
    private static void computeInjectedVersion(Ticket ticket, List<Release> releasesList, float proportion) {
        int injectedVersionId;

        // Predicted IV = max(1; FV-(FV-OV)*P), ma se FV = OV allora sostituisco FV - OV con 1
        // If ID(FV) == ID(OV) => ID(IV) = max{1, ID(FV) - 1 * p}
        if(ticket.getFixedVersion().getNumericID() == ticket.getOpeningVersion().getNumericID()){
            injectedVersionId = max(
                    1,
                    (int) (ticket.getFixedVersion().getNumericID() - proportion)
            );
        }
        // If ID(FV) != ID(OV) => ID(IV) = max{1, ID(FV) - [ID(FV) - IF(OV)] * p}
        else{
            injectedVersionId = max(
                    1,
                    (int) (ticket.getFixedVersion().getNumericID()-((ticket.getFixedVersion().getNumericID()-ticket.getOpeningVersion().getNumericID()) * proportion))
            );
        }

        // Assign the IV to the ticket
        ticket.setInjectedVersion(releasesList.stream()
                .filter(release -> release.getNumericID() == injectedVersionId)
                .toList()
                .getFirst()
        );
    }

    /**
     * Set, given the release list, the AVs of a ticket
     * @param ticket the ticket to assign the AVs to
     * @param releasesList the release list to extract the AVs from
     */
    private static void computeAffectedVersionsList(Ticket ticket, List<Release> releasesList) {
        List<Release> completeAffectedVersionsList = new ArrayList<>();

        // AV IDs are such that: IV <= AV(i) <= OV
        for(Release release: releasesList
                .stream()
                .filter(release ->
                        release.getNumericID() >= ticket.getInjectedVersion().getNumericID()
                        && release.getNumericID() <= ticket.getOpeningVersion().getNumericID())
                .toList()){
            completeAffectedVersionsList.add(new Release(release.getReleaseID(), release.getReleaseName(), release.getReleaseDateString()));
        }

        completeAffectedVersionsList.sort(Comparator.comparing(Release::getReleaseDateTime));
        ticket.setAffectedVersions(completeAffectedVersionsList);
    }
}
