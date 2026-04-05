package com.magda.taxhelper.service;

import com.magda.taxhelper.domain.*;
import com.magda.taxhelper.exception.InsufficientBuyVolumeException;
import com.magda.taxhelper.repository.CSVTransactionRepository;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaxReportService {

    Logger logger = Logger.getLogger(TaxReportService.class.getName());
    private final TransactionHistory transactionHistory;

    public TaxReportService(String csvFileName, String symbol) {
        // Load the transaction history from the CSV file
        CSVTransactionRepository repository = new CSVTransactionRepository(csvFileName);
        this.transactionHistory = new TransactionHistory(repository, symbol);
    }

    private static Transaction processBuyEventAndReturnRemainingSellTransaction(Transaction transaction, Transaction buyTransaction, List<BuySellUnit> buySellEvents, Deque<Transaction> buyEvents) {
        if (buyTransaction.getQuantity() <= transaction.getQuantity()) {
            SplitTransactions splitTransactions = splitTransaction(buyTransaction, transaction);
            Transaction partialSellTransaction = splitTransactions.getUsedTransaction();

            // TODO confirm price vs value diff here!
            double purchaseValue = buyTransaction.getValue();
            double sellValue = partialSellTransaction.getValue();
            double totalFees = buyTransaction.getFees() + partialSellTransaction.getFees();

            buySellEvents.add(new BuySellUnit(
                    purchaseValue,
                    transaction.getQuantity(),
                    sellValue,
                    totalFees,
                    sellValue - purchaseValue - totalFees
                    ));

            // Set sell transaction as remainder!
            transaction = splitTransactions.getRemainder();
        } else {
            // Buy was larger than sell amount, split buy and add remaider to remaining buy events

            SplitTransactions splitTransactions = splitTransaction(transaction, buyTransaction);
            buyEvents.addFirst(splitTransactions.getRemainder());

            // TODO confirm price vs value diff here!
            double purchaseValue = splitTransactions.getUsedTransaction().getValue();
            double sellValue = transaction.getValue();
            double totalFees = splitTransactions.getUsedTransaction().getFees() + transaction.getFees();

            buySellEvents.add(new BuySellUnit(
                    purchaseValue,
                    transaction.getQuantity(),
                    sellValue,
                    totalFees,
                    sellValue - purchaseValue - totalFees
            ));
        }
        return transaction;
    }

    static List<Transaction> getBuyEventsForSellEvent(Transaction transaction, Deque<Transaction> buyEvents) {
        List<Transaction> relatedBuyEvents = new ArrayList<>();
        double soldQuantity = transaction.getQuantity();
        double boughtQuantity = 0;

        while (soldQuantity > boughtQuantity) {
            // Find the matching buy event
            Transaction firstBuyEvent = buyEvents.poll();

            if (firstBuyEvent == null) {
                throw new InsufficientBuyVolumeException("Buy events not found.");
            }

            boughtQuantity += firstBuyEvent.getQuantity();
            relatedBuyEvents.add(firstBuyEvent);
        }
        return relatedBuyEvents;
    }

    static SplitTransactions splitTransaction(Transaction transactionToMatch, Transaction transactionToSplit) {
        // Split transaction into parts, one matching the transactionToMatch
        double sellQuantity = transactionToMatch.getQuantity();
        double percentageSold = sellQuantity / transactionToSplit.getQuantity();
        double percentageNotSold = 1 - percentageSold;
        Transaction usedTransactionPortion = new Transaction(
                transactionToSplit.getType(),
                sellQuantity,
                transactionToSplit.getPrice(),
                transactionToSplit.getValue() * percentageSold,
                transactionToSplit.getFees() * percentageSold,
                transactionToSplit.getDate()
        );
        Transaction remainder = new Transaction(
                transactionToSplit.getType(),
                transactionToSplit.getQuantity() - sellQuantity,
                transactionToSplit.getPrice(),
                transactionToSplit.getValue() * percentageNotSold,
                transactionToSplit.getFees() * percentageNotSold,
                transactionToSplit.getDate()
        );
        return new SplitTransactions(usedTransactionPortion, remainder);
    }

    public TransactionHistory getTransactionHistory() {
        return transactionHistory;
    }

    public List<BuySellUnit> getBuySellEvents() {
        // Match buy and sell events following FIFO basis
        // If the amounts do not match, split the transaction into multiple buy-sell pairs
        Deque<Transaction> buyEvents = new ArrayDeque<>();
        List<BuySellUnit> buySellEvents = new ArrayList<>();

        for (Transaction transaction : transactionHistory.getTransactions()) {
            if (transaction.getType() == TransactionType.BUY) {
                buyEvents.add(transaction);
            } else {
                // Transaction was sell
                logger.log(Level.INFO, "Processing sell on date: {0}", transaction.getDate().toString());
                // 1. List up all needed buy events to match the sell
                List<Transaction> relatedBuyEvents = getBuyEventsForSellEvent(transaction, buyEvents);


                for (Transaction buyTransaction : relatedBuyEvents) {
                    logger.log(Level.INFO, "Processing buy on date: {0}", buyTransaction.getDate().toString());
                    transaction = processBuyEventAndReturnRemainingSellTransaction(transaction, buyTransaction, buySellEvents, buyEvents);
                }

            }
        }
        return buySellEvents;
    }
}
