package com.seira.models;

// model saham 
public class StockAsset {
    private int id;
    private int userId;
    private String stockSymbol; // e.g AAPL, BBCA
    private String stockName; // e.g Apple Inc, Bank Central Asia Tbk
    private int totalLot; // jumlah lot
    private String createdAt;

    public StockAsset() {}
    // instance when add new
    public StockAsset(int id, int userId, String stockSymbol, String stockName, int totalLot, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.stockSymbol = stockSymbol;
        this.stockName = stockName;
        this.totalLot = totalLot;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }

    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }

    public int getTotalLot() { return totalLot; }
    public void setTotalLot(int totalLot) { this.totalLot = totalLot; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
