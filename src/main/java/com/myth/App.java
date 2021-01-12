package com.myth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    private static final String mappingFilePath = System.getProperty("user.dir") + "/mapping.txt";
    private static final String reportFilePath = System.getProperty("user.dir") + "/report.txt";

    public static void main(String[] args) {
        OOMReportReTracer reTracer = new OOMReportReTracer(mappingFilePath);
        try {
            String report = new String(Files.readAllBytes(Paths.get(reportFilePath)), StandardCharsets.UTF_8);
            System.out.println(reTracer.retrace(report));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
