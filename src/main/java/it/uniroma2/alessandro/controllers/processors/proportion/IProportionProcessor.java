package it.uniroma2.alessandro.controllers.processors.proportion;

import it.uniroma2.alessandro.models.Release;
import it.uniroma2.alessandro.models.Ticket;

import java.io.IOException;
import java.util.List;

public interface IProportionProcessor {
    void processProportion(List<Ticket> fixedTicketsList, List<Release> releaseList, String projName)
            throws IOException;
}
