package it.uniroma2.alessandro;

import it.uniroma2.alessandro.controllers.scrapers.MetricsScraper;

import java.util.List;
import java.util.Map;

public class Main {

    public static final List<Map.Entry<String, String>> projects = List.of(
            Map.entry("BOOKKEEPER", "https://github.com/apache/bookkeeper.git")
//            ,
//            Map.entry("AVRO", "https://github.com/apache/avro.git")
    );

    public static void main(String[] args) {
        for(Map.Entry<String, String> project: projects){
            MetricsScraper metricsScraper = new MetricsScraper();
            metricsScraper.scrapeData(project.getKey(), project.getValue());
        }
    }
}

