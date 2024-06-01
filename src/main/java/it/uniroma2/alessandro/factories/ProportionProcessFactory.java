package it.uniroma2.alessandro.factories;

import it.uniroma2.alessandro.controllers.processors.proportion.IProportionProcessor;
import it.uniroma2.alessandro.controllers.processors.proportion.IncrementProportionProcessor;
import it.uniroma2.alessandro.controllers.processors.proportion.NewProportionProcessor;
import it.uniroma2.alessandro.enums.ProportionType;

import java.io.*;
import java.util.Properties;

public class ProportionProcessFactory {
    public IProportionProcessor createProportionProcessor() throws IOException {
        try(FileInputStream propFile = new FileInputStream("config.properties")) {
            Properties properties = new Properties();
            properties.load(propFile);
            ProportionType proportionType = ProportionType.valueOf(properties.getProperty("PROPORTION_TYPE"));
            return switch (proportionType) {
                case ProportionType.INCREMENT -> new IncrementProportionProcessor();
                case ProportionType.NEW -> new NewProportionProcessor();
            };
        }
    }
}
