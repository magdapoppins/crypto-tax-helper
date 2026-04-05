package com.magda.taxhelper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.magda.taxhelper.domain.SplitTransactions;
import com.magda.taxhelper.domain.Transaction;
import com.magda.taxhelper.domain.TransactionType;
import com.magda.taxhelper.exception.InsufficientBuyVolumeException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TaxReportServiceTest {

    private static final LocalDateTime DATE = LocalDateTime.of(2025, 4, 26, 10, 41, 36);
    private static final double EPS = 1e-9;

    @Test
    void loadsTransactionHistoryFromCsvFile(@TempDir Path tempDir) throws IOException {
        Path csv = tempDir.resolve("tx.csv");
        Files.writeString(
                csv,
                """
                Symbol,Type,Quantity,Price,Value,Fees,Date
                ETH,Buy,0.06214945,"€1,609.02",€100.00,€1.49,"Apr 26, 2025, 10:41:36 AM"
                """);

        TaxReportService service = new TaxReportService(csv.toString(), "ETH");

        assertEquals(1, service.getTransactionHistory().getTransactions().size());
    }

    @Test
    void getBuyEventsForSellEvent_returnsSingleBuyWhenQuantityMatches() {
        Transaction sell = new Transaction(TransactionType.SELL, 1.0, 100, 100, 0, DATE);
        Transaction buy = new Transaction(TransactionType.BUY, 1.0, 90, 90, 0, DATE);
        Deque<Transaction> buyEvents = new ArrayDeque<>(List.of(buy));

        List<Transaction> related = TaxReportService.getBuyEventsForSellEvent(sell, buyEvents);

        assertEquals(1, related.size());
        assertSame(buy, related.get(0));
        assertTrue(buyEvents.isEmpty());
    }

    @Test
    void getBuyEventsForSellEvent_returnsMultipleBuysInFifoOrderUntilCumulativeCoversSell() {
        Transaction sell = new Transaction(TransactionType.SELL, 2.5, 100, 250, 0, DATE);
        Transaction buy1 = new Transaction(TransactionType.BUY, 1.0, 80, 80, 0, DATE);
        Transaction buy2 = new Transaction(TransactionType.BUY, 1.0, 85, 85, 0, DATE);
        Transaction buy3 = new Transaction(TransactionType.BUY, 2.0, 90, 180, 0, DATE);
        Deque<Transaction> buyEvents = new ArrayDeque<>(List.of(buy1, buy2, buy3));

        List<Transaction> related = TaxReportService.getBuyEventsForSellEvent(sell, buyEvents);

        assertEquals(3, related.size());
        assertSame(buy1, related.get(0));
        assertSame(buy2, related.get(1));
        assertSame(buy3, related.get(2));
        assertTrue(buyEvents.isEmpty());
    }

    @Test
    void getBuyEventsForSellEvent_stopsWhenSingleBuyExceedsSellQuantity() {
        Transaction sell = new Transaction(TransactionType.SELL, 0.5, 100, 50, 0, DATE);
        Transaction buy = new Transaction(TransactionType.BUY, 2.0, 90, 180, 0, DATE);
        Deque<Transaction> buyEvents = new ArrayDeque<>(List.of(buy));

        List<Transaction> related = TaxReportService.getBuyEventsForSellEvent(sell, buyEvents);

        assertEquals(1, related.size());
        assertSame(buy, related.get(0));
        assertTrue(buyEvents.isEmpty());
    }

    @Test
    void getBuyEventsForSellEvent_throwsWhenBuyQueueDoesNotCoverSellQuantity() {
        Transaction sell = new Transaction(TransactionType.SELL, 2.0, 100, 200, 0, DATE);
        Transaction buy = new Transaction(TransactionType.BUY, 1.0, 90, 90, 0, DATE);
        Deque<Transaction> buyEvents = new ArrayDeque<>(List.of(buy));

        assertThrows(InsufficientBuyVolumeException.class, () -> TaxReportService.getBuyEventsForSellEvent(sell, buyEvents));
        assertTrue(buyEvents.isEmpty());
    }

    void splitTransaction_splitsLargerBuyProportionallyByMatchQuantity() {
        Transaction match = new Transaction(TransactionType.SELL, 0.5, 100, 50, 2, DATE);
        Transaction toSplit = new Transaction(TransactionType.BUY, 2.0, 90, 180, 10, DATE);

        SplitTransactions split = TaxReportService.splitTransaction(match, toSplit);
        Transaction used = split.getUsedTransaction();
        Transaction remainder = split.getRemainder();

        assertEquals(TransactionType.BUY, used.getType());
        assertEquals(TransactionType.BUY, remainder.getType());
        assertEquals(0.5, used.getQuantity(), EPS);
        assertEquals(1.5, remainder.getQuantity(), EPS);
        assertEquals(90, used.getPrice(), EPS);
        assertEquals(90, remainder.getPrice(), EPS);
        assertEquals(45, used.getValue(), EPS);
        assertEquals(135, remainder.getValue(), EPS);
        assertEquals(2.5, used.getFees(), EPS);
        assertEquals(7.5, remainder.getFees(), EPS);
        assertEquals(DATE, used.getDate());
        assertEquals(DATE, remainder.getDate());
        assertEquals(0.5 + 1.5, toSplit.getQuantity(), EPS);
        assertEquals(45 + 135, toSplit.getValue(), EPS);
        assertEquals(2.5 + 7.5, toSplit.getFees(), EPS);
    }

    @Test
    void splitTransaction_splitsLargerSellProportionallyByMatchQuantity() {
        Transaction match = new Transaction(TransactionType.BUY, 1.0, 80, 80, 4, DATE);
        Transaction toSplit = new Transaction(TransactionType.SELL, 3.0, 100, 300, 9, DATE);

        SplitTransactions split = TaxReportService.splitTransaction(match, toSplit);
        Transaction used = split.getUsedTransaction();
        Transaction remainder = split.getRemainder();

        assertEquals(TransactionType.SELL, used.getType());
        assertEquals(TransactionType.SELL, remainder.getType());
        assertEquals(1.0, used.getQuantity(), EPS);
        assertEquals(2.0, remainder.getQuantity(), EPS);
        assertEquals(100, used.getPrice(), EPS);
        assertEquals(100, remainder.getPrice(), EPS);
        assertEquals(100, used.getValue(), EPS);
        assertEquals(200, remainder.getValue(), EPS);
        assertEquals(3, used.getFees(), EPS);
        assertEquals(6, remainder.getFees(), EPS);
    }

    @Test
    void splitTransaction_whenMatchEqualsSplitQuantity_remainderHasZeroQuantity() {
        Transaction match = new Transaction(TransactionType.BUY, 2.0, 90, 180, 10, DATE);
        Transaction toSplit = new Transaction(TransactionType.BUY, 2.0, 90, 180, 10, DATE);

        SplitTransactions split = TaxReportService.splitTransaction(match, toSplit);

        assertEquals(2.0, split.getUsedTransaction().getQuantity(), EPS);
        assertEquals(0.0, split.getRemainder().getQuantity(), EPS);
        assertEquals(180, split.getUsedTransaction().getValue(), EPS);
        assertEquals(0.0, split.getRemainder().getValue(), EPS);
        assertEquals(10, split.getUsedTransaction().getFees(), EPS);
        assertEquals(0.0, split.getRemainder().getFees(), EPS);
    }
}
