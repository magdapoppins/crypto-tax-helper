package com.magda.taxhelper.domain;

import java.util.Locale;

public class BuySellUnit {
    double purchasePrice;
    double quantity;
    double sellPrice;
    double fees;
    double proceeds;

    public BuySellUnit(double purchasePrice, double quantity, double sellPrice, double fees, double proceeds) {
        this.purchasePrice = purchasePrice;
        this.quantity = quantity;
        this.sellPrice = sellPrice;
        this.fees = fees;
        this.proceeds = proceeds;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "buy value=%.2f  qty=%.8f  sell value=%.2f  fees=%.2f  net=%.2f",
                purchasePrice, quantity, sellPrice, fees, proceeds);
    }
}