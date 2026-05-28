package com.seira.dao.interfaces;

import com.seira.models.StockAsset;
import java.util.List;

// interface stock asset
public interface IStockAssetDAO {
    List<StockAsset> findAll(int userId);
    boolean add(StockAsset asset);
    boolean delete(int id);
}
