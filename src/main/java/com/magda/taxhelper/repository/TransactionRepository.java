package com.magda.taxhelper.repository;

import com.magda.taxhelper.domain.Transaction;
import java.util.List;

public interface TransactionRepository {
    List<Transaction> findAll(String symbol);
}