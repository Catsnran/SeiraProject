package com.seira.dao.interfaces;

import com.seira.models.Transaction;

import java.time.LocalDate;
import java.util.List;

public interface ITransactionDAO {
    boolean add(Transaction t);
    boolean update(Transaction t);
    boolean delete(int id);
    Transaction findById(int id);
    List<Transaction> findAll(int userId, String type, LocalDate from, LocalDate to, String search, List<String> references);
    default List<Transaction> findAll(int userId, String type, LocalDate from, LocalDate to, String search) {
        return findAll(userId, type, from, to, search, null);
    };
}
