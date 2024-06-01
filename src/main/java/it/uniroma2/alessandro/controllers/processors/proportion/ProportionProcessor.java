package it.uniroma2.alessandro.controllers.processors.proportion;


import it.uniroma2.alessandro.enums.ProportionType;
import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.models.Ticket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.Math.max;


public abstract class ProportionProcessor implements IProportionProcessor{

    protected static final String NAME_OF_THIS_CLASS = ProportionProcessor.class.getName();
    protected static final String STARTING_SEPARATOR = "----------------------\n[";
    protected static final String ENDING_SEPARATOR = "]\n----------------------\n";
    protected static final String NORMAL_SEPARATOR = "\n----------------------\n";

    protected static final Logger logger = Logger.getLogger(NAME_OF_THIS_CLASS);
    protected static final StringBuilder outputToFile = new StringBuilder();


    /**
     * Set, given the proportion and the release list, the IV of a ticket
     * @param ticket the ticket to assign the IV to
     * @param releasesList the list to extract the IV from
     * @param proportion the proportion for the formula
     */
    protected void computeInjectedVersion(Ticket ticket, List<Release> releasesList, float proportion) {
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
    protected void computeAffectedVersionsList(Ticket ticket, List<Release> releasesList) {
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
