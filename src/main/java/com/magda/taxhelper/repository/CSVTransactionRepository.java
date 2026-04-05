package com.magda.taxhelper.repository;

import com.magda.taxhelper.domain.Transaction;
import com.magda.taxhelper.domain.TransactionType;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CSVTransactionRepository implements TransactionRepository {

    private static final int EXPECTED_COLUMNS = 7;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a", Locale.ENGLISH);

    private final String filePath;

    public CSVTransactionRepository(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public List<Transaction> findAll(String symbol) {
        List<Transaction> transactions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                if (lineNumber == 1 && line.startsWith("Symbol")) {
                    continue;
                }
                try {
                    List<String> fields = parseCsvLine(line);
                    if (fields.size() != EXPECTED_COLUMNS) {
                        throw new IllegalArgumentException(
                                "expected " + EXPECTED_COLUMNS + " columns, got " + fields.size());
                    }
                    if (fields.get(0).trim().equals(symbol.trim())) {
                        transactions.add(parseTransaction(fields));
                    }
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(
                            "CSV line " + lineNumber + ": invalid date — \"" + line + "\"", e);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "CSV line " + lineNumber + ": invalid number — \"" + line + "\"", e);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "CSV line " + lineNumber + ": " + e.getMessage() + " — \"" + line + "\"", e);
                }
            }
            return transactions;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read transactions from CSV file: " + filePath, e);
        }
    }

    private static Transaction parseTransaction(List<String> fields) {
        TransactionType type;
        try {
            type = TransactionType.valueOf(fields.get(1).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "type must be Buy or Sell, got: " + fields.get(1).trim(), e);
        }
        double quantity = Double.parseDouble(fields.get(2).trim());
        double price = parseMoney(fields.get(3));
        double value = parseMoney(fields.get(4));
        double fees = parseMoney(fields.get(5));
        String dateRaw = normalizeDateSpaces(fields.get(6).trim());
        LocalDateTime date = LocalDateTime.parse(dateRaw, DATE_TIME);
        return new Transaction(type, quantity, price, value, fees, date);
    }

    /** CSV exports often use NBSP / narrow NBSP before AM/PM; the formatter expects ASCII space. */
    private static String normalizeDateSpaces(String raw) {
        return raw.replace('\u202f', ' ').replace('\u00a0', ' ');
    }

    private static double parseMoney(String raw) {
        String normalized = raw.trim().replace("€", "").replace("\u00a0", "").replace(",", "").trim();
        return Double.parseDouble(normalized);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
