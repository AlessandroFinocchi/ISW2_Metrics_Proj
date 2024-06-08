package it.uniroma2.alessandro;

import it.uniroma2.alessandro.controllers.scrapers.MetricsScraper;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    public static final List<Map.Entry<String, String>> projects = List.of(
            Map.entry("BOOKKEEPER", "https://github.com/AlessandroFinocchi/bookkeeper.git")
            //, Map.entry("AVRO", "https://github.com/AlessandroFinocchi/avro.git")
    );

    public static void main(String[] args) throws FileNotFoundException {
        for(Map.Entry<String, String> project: projects){
            MetricsScraper metricsScraper = new MetricsScraper();
            metricsScraper.scrapeData(project.getKey(), project.getValue());
        }
    }
}

