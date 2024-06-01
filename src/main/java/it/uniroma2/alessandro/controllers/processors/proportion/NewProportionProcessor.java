package it.uniroma2.alessandro.controllers.processors.proportion;

import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.models.Ticket;
import it.uniroma2.alessandro.utilities.FileWriterUtility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NewProportionProcessor extends ProportionProcessor{

    public void processProportion(List<Ticket> ticketList, List<Release> releaseList, String projName) {
        List<Ticket> ticketForProportionList = new ArrayList<>(); // List of tickets already with IV
        List<Ticket> ticketToProportionList = new ArrayList<>(); // List of ticket without IV
        float proportion = 0;
        try {
            File file = new File("outputFiles/reportFiles/" + projName);
            if (!file.exists() && !file.mkdirs()) throw new IOException();

            // Separate tickets with and without AV, and set IV for those ticket with AVs: IV = AV[0]
            for(Ticket ticket: ticketList){
                if(ticket.isCorrect()){
                    ticketForProportionList.add(ticket);
                    ticket.setInjectedVersion(ticket.getAffectedVersions().getFirst());
                    }
                else
                    ticketToProportionList.add(ticket);
            }

            // Process proportion from the tickets with IV
            proportion = getProportion(ticketForProportionList);

            // Process IV for tickets without IV with the proportion processes
            processProportion(ticketToProportionList, releaseList, proportion);

            ticketList.sort(Comparator.comparing(Ticket::getResolutionDate));

            file = new File("outputFiles/reportFiles/" + projName + "/Proportion.txt");
            try(FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.append(outputToFile.toString());
                FileWriterUtility.flushAndCloseFW(fileWriter, logger, NAME_OF_THIS_CLASS);
            }
        } catch(IOException e){
            logger.info("Error in ComputeProportion when trying to create directory");
        }
    }

    private float getProportion(List<Ticket> ticketList) {
        float totalProportion = 0.0F;
        float denominator;
        float propForTicket;

        // For each ticket...
        for (Ticket correctTicket : ticketList) {
            outputToFile.append("\n[*]PROPORTION[*]-----------------------------------------------\n")
                    .append(STARTING_SEPARATOR)
                    .append(correctTicket.getTicketKey())
                    .append(ENDING_SEPARATOR);
            propForTicket = 0.0F;

            outputToFile.append(NORMAL_SEPARATOR);

            // If the OV != FV the denominator can be computed, otherwise proportion is 0
            if (!correctTicket.getOpeningVersion().getReleaseID().equals(correctTicket.getFixedVersion().getReleaseID())) {
                denominator = ((float) correctTicket.getFixedVersion().getNumericID() - (float) correctTicket.getOpeningVersion().getNumericID());
                propForTicket = ((float) correctTicket.getFixedVersion().getNumericID() - (float) correctTicket.getInjectedVersion().getNumericID())
                        / denominator;
            }

            totalProportion+=propForTicket;
        }
        return totalProportion / ticketList.size();
    }

    private void processProportion(List<Ticket> ticketList, List<Release> releaseList, float proportion) {
        outputToFile.append("TICKET WITHOUT IV: ")
                .append(ticketList.size())
                .append("\n")
                .append("PROPORTION : ")
                .append(proportion)
                .append("\n")
                .append("----------------------------------------------------------\n");
        for(Ticket ticket: ticketList){
            outputToFile.append("\n[*]PROPORTION[*]-----------------------------------------------\n")
                    .append(STARTING_SEPARATOR)
                    .append(ticket.getTicketKey())
                    .append(ENDING_SEPARATOR);
            computeInjectedVersion(ticket, releaseList, proportion);
            computeAffectedVersionsList(ticket, releaseList);
        }
    }
}
