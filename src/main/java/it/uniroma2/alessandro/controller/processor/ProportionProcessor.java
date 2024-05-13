package it.uniroma2.alessandro.controller.processor;


import it.uniroma2.alessandro.model.Ticket;
import it.uniroma2.alessandro.utilities.FileWriterUtility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;


public class ProportionProcessor {

    private static final String NAME_OF_THIS_CLASS = ProportionProcessor.class.getName();
    private static final String STARTING_SEPARATOR = "----------------------\n[";
    private static final String ENDING_SEPARATOR = "]\n----------------------\n";
    private static final String NORMAL_SEPARATOR = "\n----------------------\n";

    private static final Logger logger = Logger.getLogger(NAME_OF_THIS_CLASS);
    private static final StringBuilder outputToFile = new StringBuilder();

    private ProportionProcessor() {}

    public static float computeProportion(List<Ticket> fixedTicketsList, String projName, Ticket ticket, boolean doActualComputation) throws URISyntaxException {
        float proportion = 0;
        try {
            File file = new File("outputFiles/reportFiles/" + projName);
            if (!file.exists()) {
                boolean created = file.mkdirs();
                if (!created) {
                    throw new IOException();
                }
            }
            file = new File("outputFiles/reportFiles/" + projName + "/Proportion.txt");
            try(FileWriter fileWriter = new FileWriter(file)) {
                proportion = processIncrementalProportion(fixedTicketsList, ticket, doActualComputation);
                fileWriter.append(outputToFile.toString());
                FileWriterUtility.flushAndCloseFW(fileWriter, logger, NAME_OF_THIS_CLASS);
            }
        } catch(IOException e){
            logger.info("Error in ComputeProportion when trying to create directory");
        }
        return proportion;
    }

    private static float processIncrementalProportion(List<Ticket> filteredTicketsList, Ticket ticket,
                                                      boolean doActualComputation) {
        outputToFile.append("\n[*]PROPORTION[*]-----------------------------------------------\n")
                .append(STARTING_SEPARATOR)
                .append(ticket.getTicketKey())
                .append(ENDING_SEPARATOR);

        // If the computation doesn't have to be done, it just appends a line
        if(!doActualComputation) {
            if (!ticket.getFixedVersion().getReleaseID().equals(ticket.getOpeningVersion().getReleaseID()))
                outputToFile.append("PROPORTION: WILL USE PROPORTION AS IT IS!");
            else
                outputToFile.append("PROPORTION: WILL SET DENOMINATOR=1!");

            outputToFile.append(NORMAL_SEPARATOR);

            return 0;
        }

        // Order the tickets by date
        filteredTicketsList.sort(Comparator.comparing(Ticket::getResolutionDate));

        // Compute the proportion
        float proportion = getIncrementalProportion(filteredTicketsList);

        // Write report
        outputToFile.append("SIZE_OF_FILTERED_TICKET_LIST: ")
                .append(filteredTicketsList.size())
                .append("\n")
                .append("PROPORTION : ")
                .append(proportion)
                .append("\n")
                .append("----------------------------------------------------------\n");
        return proportion;
    }

    //todo: check if proportion denominator can be changed to (old_denominator+1)
    // usa entrambi i labeling per le iv che conosci e vedi quale si avvicina di più, non pui rifare il paper
    // e la bontà del labeling la puoi misurare solo per le iV che conosci
    private static float getIncrementalProportion(List<Ticket> filteredTicketsList) {
        float totalProportion = 0.0F;
        float denominator;

        // For each ticket...
        for (Ticket correctTicket : filteredTicketsList) {

            // If the OV != FV the denominator can be computed
            if (!correctTicket.getOpeningVersion().getReleaseID().equals(correctTicket.getFixedVersion().getReleaseID())) {
                denominator = ((float) correctTicket.getFixedVersion().getNumericID() - (float) correctTicket.getOpeningVersion().getNumericID());
            }
            // Since the denominator would be zero, it is indeed set to 1
            else{
                denominator = 1;
            }

            // Compute the proportion fot the ticket
            float propForTicket = ((float) correctTicket.getFixedVersion().getNumericID() - (float) correctTicket.getInjectedVersion().getNumericID())
                    / denominator;

            totalProportion+=propForTicket;
        }
        return totalProportion / filteredTicketsList.size();
    }

}
