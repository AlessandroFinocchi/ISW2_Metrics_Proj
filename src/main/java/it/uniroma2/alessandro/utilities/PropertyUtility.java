package it.uniroma2.alessandro.utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyUtility {
    private static final String CONFIG_FILE = "config.properties";

    private PropertyUtility() {
        throw new IllegalStateException("Utility class");
    }
    public static boolean readBooleanProperty(String propertyName) throws IOException {
        try(FileInputStream propFile = new FileInputStream(CONFIG_FILE)) {
            Properties properties = new Properties();
            properties.load(propFile);
            return Boolean.parseBoolean(properties.getProperty(propertyName));
        }
    }

    public static int readIntegerProperty(String propertyName) throws IOException {
        try(FileInputStream propFile = new FileInputStream(CONFIG_FILE)) {
            Properties properties = new Properties();
            properties.load(propFile);
            return Integer.parseInt(properties.getProperty(propertyName));
        }
    }

    public static String readStringProperty(String propertyName) throws IOException {
        try(FileInputStream propFile = new FileInputStream(CONFIG_FILE)) {
            Properties properties = new Properties();
            properties.load(propFile);
            return properties.getProperty(propertyName);
        }
    }
}
