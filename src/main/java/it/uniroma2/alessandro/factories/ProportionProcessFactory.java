package it.uniroma2.alessandro.factories;

import it.uniroma2.alessandro.controllers.processors.proportion.IProportionProcessor;
import it.uniroma2.alessandro.controllers.processors.proportion.IncrementProportionProcessor;
import it.uniroma2.alessandro.controllers.processors.proportion.NewProportionProcessor;
import it.uniroma2.alessandro.enums.ProportionType;
import it.uniroma2.alessandro.utilities.PropertyUtility;

import java.io.*;

public class ProportionProcessFactory {
    public IProportionProcessor createProportionProcessor() throws IOException {
        ProportionType proportionType = ProportionType.valueOf(PropertyUtility.readStringProperty("PROPORTION_TYPE"));
        return switch (proportionType) {
            case ProportionType.INCREMENT -> new IncrementProportionProcessor();
            case ProportionType.NEW -> new NewProportionProcessor();
        };
    }
}
