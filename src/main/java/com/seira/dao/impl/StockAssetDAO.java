package com.seira.dao.impl;

import com.seira.dao.DBConnection;
import com.seira.dao.interfaces.IStockAssetDAO;
import com.seira.models.StockAsset;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockAssetDAO implements IStockAssetDAO {
    // select from user id
    @Override
    public List<StockAsset> findAll(int userId) {
        List<StockAsset> list = new ArrayList<>();
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "SELECT * FROM stock_assets WHERE user_id = ? ORDER BY id DESC"
            );
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                StockAsset sa = new StockAsset();
                sa.setId(rs.getInt("id"));
                sa.setUserId(rs.getInt("user_id"));
                sa.setStockSymbol(rs.getString("stock_symbol"));
                sa.setStockName(rs.getString("stock_name"));
                sa.setTotalLot(rs.getInt("total_lot"));
                sa.setCreatedAt(rs.getString("created_at"));
                list.add(sa);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    // add new stock 
    @Override
    public boolean add(StockAsset asset) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "INSERT INTO stock_assets (user_id, stock_symbol, stock_name, total_lot) VALUES (?,?,?,?)"
            );
            ps.setInt(1, asset.getUserId());
            ps.setString(2, asset.getStockSymbol());
            ps.setString(3, asset.getStockName());
            ps.setInt(4, asset.getTotalLot());
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // delete stock
    @Override
    public boolean delete(int id) {
        try {
            PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(
                "DELETE FROM stock_assets WHERE id = ?"
            );
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
