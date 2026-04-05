package com.magda.taxhelper.domain;

public class SplitTransactions {
    Transaction usedTransaction;
    Transaction remainder;

    public SplitTransactions(
        Transaction usedTransaction,
        Transaction remainder
    ) {
        this.usedTransaction = usedTransaction;
        this.remainder = remainder;
    }

    public Transaction getUsedTransaction() {
        return usedTransaction;
    }

    public Transaction getRemainder() {
        return remainder;
    }
}
