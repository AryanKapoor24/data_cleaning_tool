package com.example;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple MVC Controller that renders the upload page and handles the CSV upload.
 * It uses Apache Commons CSV to parse the file, then cleans the data by:
 * - trimming whitespace
 * - filling missing values with blank
 * - removing duplicate rows
 */
@Controller
public class CsvController {

    /**
     * Renders the upload page.
     */
    @GetMapping("/")
    public String home(Model model) {
        return "upload"; // src/main/resources/templates/upload.html
    }

    /**
     * Handles the file upload and processes the CSV.
     */
    @PostMapping("/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file, Model model) {
        // Validate that a file was uploaded and looks like CSV
        if (file == null || file.isEmpty() || !isCsv(file)) {
            model.addAttribute("errorMessage", "Please upload a valid CSV file.");
            return "upload";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Parse with header recognition. If header is missing, Apache Commons CSV will create headers automatically.
            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreSurroundingSpaces() // helps with trimming around quoted values
                    .parse(reader);

            // Preserve header order as provided in the file
            List<String> headers = new ArrayList<>(parser.getHeaderMap().keySet());

            // Storage for cleaned, de-duplicated rows
            List<List<String>> cleanedRows = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>(); // remembers insertion order

            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>(headers.size());
                for (String header : headers) {
                    // Get raw value, convert null to "", and trim whitespace
                    String value = record.isMapped(header) ? record.get(header) : "";
                    if (value == null) value = "";
                    value = value.trim();
                    row.add(value);
                }

                // Build a unique key for the row to remove duplicates
                String key = String.join("\u0001", row); // unlikely separator for uniqueness
                if (seen.add(key)) {
                    cleanedRows.add(row);
                }
            }

            // Add data to the model for rendering in the results page
            model.addAttribute("headers", headers);
            model.addAttribute("rows", cleanedRows);
            model.addAttribute("message", "CSV cleaned successfully!");
            return "result"; // src/main/resources/templates/result.html

        } catch (IOException ex) {
            model.addAttribute("errorMessage", "Please upload a valid CSV file.");
            return "upload";
        }
    }

    /**
     * A simple check to see if the file looks like CSV based on content type or file extension.
     */
    private boolean isCsv(MultipartFile file) {
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        boolean typeLooksCsv = contentType != null && (contentType.contains("csv") || contentType.equals("text/plain"));
        boolean nameLooksCsv = name != null && name.toLowerCase().endsWith(".csv");
        return typeLooksCsv || nameLooksCsv;
    }
}
