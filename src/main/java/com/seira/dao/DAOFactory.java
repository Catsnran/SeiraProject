package com.seira.dao;

import com.seira.dao.impl.*;
import com.seira.dao.interfaces.*;
import com.seira.models.Transaction;

/**
 * Factory untuk mendapatkan instance DAO.
 * Semua controller mengakses data lewat class ini.
 *
 * Contoh pemakaian:
 *   DAOFactory.getTransactionDAO().add(t);
 *   DAOFactory.getReportDAO().getTotalIncome(userId, period);
 */
public class DAOFactory {

    private static final UserDAO          userDAO          = new UserDAO();
    private static final TransactionDAO   transactionDAO   = new TransactionDAO();
    private static final CategoryDAO      categoryDAO      = new CategoryDAO();
    private static final PaymentMethodDAO paymentMethodDAO = new PaymentMethodDAO();
    private static final BudgetDAO        budgetDAO        = new BudgetDAO();
    private static final ReportDAO        reportDAO        = new ReportDAO();

    public static IUserDAO          getUserDAO()          { return userDAO; }
    public static ITransactionDAO   getTransactionDAO()   { return transactionDAO; }
    public static ICategoryDAO      getCategoryDAO()      { return categoryDAO; }
    public static IPaymentMethodDAO getPaymentMethodDAO() { return paymentMethodDAO; }
    public static IBudgetDAO        getBudgetDAO()        { return budgetDAO; }
    public static IReportDAO        getReportDAO()        { return reportDAO; }
}
