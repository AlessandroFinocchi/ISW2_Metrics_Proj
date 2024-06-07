package it.uniroma2.alessandro.controllers.processors.proportion;

import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.models.Ticket;
import it.uniroma2.alessandro.utilities.FileWriterUtility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static it.uniroma2.alessandro.controllers.processors.sets.DatasetsProcessor.RESULT_DIRECTORY_NAME;

public class IncrementProportionProcessor extends ProportionProcessor {

    public void processProportion(List<Ticket> ticketList, List<Release> releaseList, String projName) {
        List<Ticket> ticketForProportionList = new ArrayList<>(); // List of tickets already with IV
        List<Ticket> finalTicketList = new ArrayList<>();
        float proportion = 0;
        try {
            File file = new File(RESULT_DIRECTORY_NAME + projName.toLowerCase() + "/reportFiles/");
            if (!file.exists() && !file.mkdirs()) throw new IOException();

            // We can start proportion from the first ticket with an IV, the preceding ones cannot be proportioned
            LocalDate firstTicketWithIVDate = ticketList
                    .stream()
                    .filter(Ticket::isCorrect)
                    .toList()
                    .getFirst().getResolutionDate();
            ticketList.removeIf(t -> t.getResolutionDate().isBefore(firstTicketWithIVDate));

            // For each ticket...
            for(Ticket ticket : ticketList){
                // If the ticket has a list of AVs
                if(ticket.isCorrect()){
                    getProportion(ticketForProportionList, ticket, false);
                    ticket.setInjectedVersion(ticket.getAffectedVersions().getFirst());

                    // For proportion, we use only the tickets with injected version
                    // already known, not the one processed through proportion
                    ticketForProportionList.add(ticket);
                }
                // If the ticket doesn't have a list of AVs
                else{
                    proportion = getProportion(ticketForProportionList, ticket, true);
                    computeInjectedVersion(ticket, releaseList, proportion);
                    computeAffectedVersionsList(ticket, releaseList);
                }

                finalTicketList.add(ticket);
            }

            finalTicketList.sort(Comparator.comparing(Ticket::getResolutionDate));

            file = new File(RESULT_DIRECTORY_NAME + projName + "/reportFiles/Proportion.txt");
            try(FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.append(outputToFile.toString());
                FileWriterUtility.flushAndCloseFW(fileWriter, logger, NAME_OF_THIS_CLASS);
            }
        } catch(IOException e){
            logger.info("Error in ComputeProportion when trying to create directory");
        }
    }

    /**
     * Appends information about how the proportion is computed for each ticket, and compute the proportion value
     * @param ticketForProportionList list of tickets valid to compute proportion
     * @param ticket the ticket to set the IV
     * @param doActualComputation if the ticket IV must be computed or not
     * @return the proportion
     */
    private float getProportion(List<Ticket> ticketForProportionList, Ticket ticket,
                                boolean doActualComputation) {
        outputToFile.append("\n[*]PROPORTION[*]-----------------------------------------------\n")
                .append(STARTING_SEPARATOR)
                .append(ticket.getTicketKey())
                .append(ENDING_SEPARATOR);

        // If the computation doesn't have to be done, it just appends a line
        if(!doActualComputation) return 0;

        // Order the tickets by date
        ticketForProportionList.sort(Comparator.comparing(Ticket::getResolutionDate));

        // Compute the proportion
        float proportion = computeProportion(ticketForProportionList);

        // Write report
        outputToFile.append("SIZE OF FILTERED TICKET LIST: ")
                .append(ticketForProportionList.size())
                .append("\n")
                .append("PROPORTION : ")
                .append(proportion)
                .append("\n")
                .append("----------------------------------------------------------\n");
        return proportion;
    }

    /**
     * Tickets with IVs are used to computer the proportion
     * @param ticketForProportionList list of tickets valid to compute proportion
     * @return the proportion
     */
    private float computeProportion(List<Ticket> ticketForProportionList) {
        float totalProportion = 0.0F;
        float denominator;
        float propForTicket;

        // For each ticket...
        for (Ticket correctTicket : ticketForProportionList) {
            propForTicket = 0.0F;

            // If the OV != FV the denominator can be computed, otherwise proportion is 0
            if (!correctTicket.getOpeningVersion().getReleaseID().equals(correctTicket.getFixedVersion().getReleaseID())) {
                denominator = ((float) correctTicket.getFixedVersion().getNumericID() - (float) correctTicket.getOpeningVersion().getNumericID());
                propForTicket = ((float) correctTicket.getFixedVersion().getNumericID() - (float) correctTicket.getInjectedVersion().getNumericID())
                        / denominator;
            }

            totalProportion+=propForTicket;
        }
        return totalProportion / ticketForProportionList.size();
    }
}
