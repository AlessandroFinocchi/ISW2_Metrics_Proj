package it.uniroma2.alessandro.controllers.processors.proportion;

import it.uniroma2.alessandro.enums.ProportionType;
import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.models.Ticket;
import it.uniroma2.alessandro.utilities.FileWriterUtility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;

public class NewProportionProcessor extends ProportionProcessor{

    public float processProportion(List<Ticket> ticketList, List<Release> releaseList, String projName)
            throws URISyntaxException {
        float proportion = 0;

        return proportion;
    }

    private float processProportion(List<Ticket> filteredTicketsList, Ticket ticket, boolean doActualComputation) {
        float proportion = 0;

        return proportion;
    }

    //todo: check if proportion denominator can be changed to (old_denominator+1)
    // usa entrambi i labeling per le iv che conosci e vedi quale si avvicina di più, non pui rifare il paper
    // e la bontà del labeling la puoi misurare solo per le iV che conosci
    private float getProportion(List<Ticket> filteredTicketsList) {
        float proportion = 0;

        return proportion;
    }
}
