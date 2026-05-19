package com.seira.dao.interfaces;

import com.seira.models.Budget;

import java.time.YearMonth;
import java.util.List;

public interface IBudgetDAO {
    List<Budget> findAll(int userId, YearMonth period);
    boolean save(Budget b);
    boolean delete(int id);
}
