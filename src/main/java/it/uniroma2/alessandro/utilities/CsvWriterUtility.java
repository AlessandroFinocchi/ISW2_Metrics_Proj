package it.uniroma2.alessandro.utilities;

import java.io.FileWriter;
import java.io.IOException;

public class CsvWriterUtility {
    private static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * Writes a String to a file.
     *
     * @param filePath The path to the file to write to.
     * @param content The String content to write.
     * @param append Whether to append to the existing content (true) or overwrite (false). Defaults to false.
     * @throws IOException If there's an issue writing to the file.
     */
    public static void writeToFile(String filePath, String content, boolean append) throws IOException {
        try (FileWriter writer = new FileWriter(filePath, append)) {
            writer.write(content);
        }
    }

    /**
     * Writes a String to a file, appending by default.
     *
     * @param filePath The path to the file to write to.
     * @param content The String content to write.
     * @throws IOException If there's an issue writing to the file.
     */
    public static void writeToFile(String filePath, String content) throws IOException {
        writeToFile(filePath, content, true);
    }
}
