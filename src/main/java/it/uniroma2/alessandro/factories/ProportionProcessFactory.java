package it.uniroma2.alessandro.factories;

import it.uniroma2.alessandro.controllers.processors.proportion.IProportionProcessor;
import it.uniroma2.alessandro.controllers.processors.proportion.IncrementProportionProcessor;
import it.uniroma2.alessandro.controllers.processors.proportion.NewProportionProcessor;
import it.uniroma2.alessandro.enums.ProportionType;
import it.uniroma2.alessandro.exceptions.IncorrectProportionException;

import java.io.*;
import java.util.Properties;

public class ProportionProcessFactory {
    public IProportionProcessor createProportionProcessor() throws IOException, IncorrectProportionException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("config.properties"));
        ProportionType proportionType = ProportionType.valueOf(properties.getProperty("PROPORTION_TYPE"));
        return switch (proportionType) {
            case ProportionType.INCREMENT -> new IncrementProportionProcessor();
            case ProportionType.NEW -> new NewProportionProcessor();
        };
    }

    public IncrementProportionProcessor createIncrementProportionProcessor(){
        return new IncrementProportionProcessor();
    }

    public NewProportionProcessor createNewProportionProcessor(){
        return new NewProportionProcessor();
    }
}
