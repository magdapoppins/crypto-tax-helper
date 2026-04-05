package com.magda.taxhelper.domain;

import java.time.LocalDateTime;

public class Transaction {
    private TransactionType type;
    // Quantity is the number of assets bought or sold
    private double quantity;
    // Price is the price of the asset at the time of the transaction
    // Only one currency is supported for now
    private double price;
    // Value is the total value of the transaction
    private double value;
    // Fees are the fees associated with the transaction
    private double fees;
    // Datetime of the transaction - timezone is UTC
    private LocalDateTime date;

    public Transaction(
            TransactionType type,
            double quantity,
            double price,
            double value,
            double fees,
            LocalDateTime date) {
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.value = value;
        this.fees = fees;
        this.date = date;
    }

    public TransactionType getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getValue() {
        return value;
    }

    public double getFees() {
        return fees;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
