package com.seira.dao.interfaces;

import com.seira.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public interface IPaymentMethodDAO {
    List<PaymentMethod> findAll(int userId);
    PaymentMethod findById(int id);
    boolean add(PaymentMethod pm);
    boolean updateBalance(int id, BigDecimal balance);
    boolean delete(int id);
    double getTotalLiquidity(int userId);
    double getLiquidityByType(int userId, String type);
    double getLiquidityExcludingType(int userId, String excludeType);
}
