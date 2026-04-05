package com.magda.taxhelper.domain;

import com.magda.taxhelper.repository.TransactionRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionHistory {
    private final List<Transaction> transactions;

    public TransactionHistory(TransactionRepository repository, String symbol) {
        this.transactions = new ArrayList<>(repository.findAll(symbol));
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }
}