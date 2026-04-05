package com.magda.taxhelper.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CSVTransactionRepositoryTest {

    @Test
    void findAll_skipsHeaderBlankLines_andReturnsParsedRows(@TempDir Path tempDir) throws IOException {
        Path testFilePath = tempDir.resolve("tx.csv");
        String content =
                """
                Symbol,Type,Quantity,Price,Value,Fees,Date
                ETH,Buy,0.06214945,"€1,609.02",€100.00,€1.49,"Apr 26, 2025, 10:41:36 AM"
                BCH,Buy,0.30206866,€331.05,€100.00,€1.48,"Apr 26, 2025, 10:43:02 AM"
                ETH,Buy,0.06114805,"€1,635.38",€100.00,€1.49,"May 3, 2025, 10:34:22 AM"
                """;
        Files.writeString(testFilePath, content);

        CSVTransactionRepository repo = new CSVTransactionRepository(testFilePath.toString());

        assertEquals(2, repo.findAll("ETH").size());
    }

    @Test
    void findAll_typeIsCaseInsensitive(@TempDir Path tempDir) throws IOException {
        Path testFilePath = tempDir.resolve("tx.csv");
        Files.writeString(
                testFilePath,
                """
                Symbol,Type,Quantity,Price,Value,Fees,Date
                X,buy,1,€1,€1,€0,"Mar 3, 2025, 1:00:00 PM"
                """);

        assertEquals(1, new CSVTransactionRepository(testFilePath.toString()).findAll("X").size());
    }

    @Test
    void findAll_throwsWhenFileMissing() {
        CSVTransactionRepository repo =
                new CSVTransactionRepository("/nonexistent/path/does-not-exist.csv");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> repo.findAll("ETH"));
        assertTrue(ex.getMessage().contains("Failed to read transactions from CSV file"));
    }

    @Test
    void findAll_throwsWhenColumnCountWrong(@TempDir Path tempDir) throws IOException {
        Path testFilePath = tempDir.resolve("bad.csv");
        Files.writeString(testFilePath, "Symbol,Type,Quantity\nonly,three,cols\n");

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new CSVTransactionRepository(testFilePath.toString()).findAll("BTC"));
        assertTrue(ex.getMessage().contains("expected 7 columns"));
    }
}
