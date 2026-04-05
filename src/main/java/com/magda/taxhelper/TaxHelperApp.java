package com.magda.taxhelper;

import com.magda.taxhelper.domain.BuySellUnit;
import com.magda.taxhelper.service.TaxReportService;

import java.util.List;

public class TaxHelperApp {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java -cp target/classes com.magda.taxhelper.TaxHelperApp <csv-file-path> <symbol>");
            System.exit(1);
        }

        String csvPath = args[0];
        String symbol = args[1];

        TaxReportService taxReportService = new TaxReportService(csvPath, symbol);
        List<BuySellUnit> buySellEvents = taxReportService.getBuySellEvents();

        System.out.println("FIFO-matched buy/sell pairs of " + symbol + " (" + buySellEvents.size() + "):");
        System.out.println("-".repeat(72));
        for (int i = 0; i < buySellEvents.size(); i++) {
            System.out.printf("%3d  %s%n", i + 1, buySellEvents.get(i));
        }
    }
}