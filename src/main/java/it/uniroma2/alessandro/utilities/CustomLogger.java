package it.uniroma2.alessandro.utilities;

import java.util.logging.Logger;

public class CustomLogger {

    public static void info(String message) {
        String callerName = Thread.currentThread().getStackTrace()[2].getFileName();
        Logger.getLogger("callerName").info(message);
    }
}
