package com.seira.dao.interfaces;

import java.time.YearMonth;
import java.util.List;

public interface IReportDAO {
    double getTotalIncome(int userId, YearMonth period);
    double getTotalExpense(int userId, YearMonth period);
    List<double[]> getMonthlyTrend(int userId, int months);
    List<double[]> getNetWorthTrend(int userId, int months);
    List<Object[]> getCategoryBreakdown(int userId, YearMonth period);
}
