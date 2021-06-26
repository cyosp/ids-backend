package com.cyosp.ids.rest.authentication.signin;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.cyosp.ids.configuration.IdsConfiguration.DATA_DIRECTORY_PATH;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.apache.commons.csv.CSVFormat.DEFAULT;

@Slf4j
@Component
public class LoggedService {
    private static final String LOGGED_PATH = DATA_DIRECTORY_PATH + "ids.logged.csv";

    private final SimpleDateFormat loggedDateFormat;

    public LoggedService() {
        loggedDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    }

    public void add(String email) {
        try {
            CSVPrinter csvPrinter = new CSVPrinter(
                    newBufferedWriter(get(LOGGED_PATH), CREATE, APPEND),
                    DEFAULT);
            csvPrinter.printRecord(email, loggedDateFormat.format(new Date()));
            csvPrinter.flush();
        } catch (Exception e) {
            log.warn("Fail to add: {} to: {}", email, LOGGED_PATH, e);
        }
    }
}
