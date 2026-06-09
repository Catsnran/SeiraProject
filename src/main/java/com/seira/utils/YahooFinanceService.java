package com.seira.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// api yahoo finance -> jujur mau tak masukin ke package sendiri tp kek buang buang cuma 1 service
public class YahooFinanceService {
    public static class StockSearchResult {
        private final String symbol;
        private final String name;
        private final String exchange;

        public StockSearchResult(String symbol, String name, String exchange) {
            this.symbol = symbol;
            this.name = name;
            this.exchange = exchange;
        }

        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public String getExchange() { return exchange; }

        @Override
        public String toString() {
            return symbol + " — " + name + " (" + exchange + ")";
        }
    }

    public static class StockPricePoint {
        private final long time;
        private final double price;

        public StockPricePoint(long time, double price) {
            this.time = time;
            this.price = price;
        }

        public long getTime() { return time; }
        public double getPrice() { return price; }
    }

    public static class StockChartData {
        private final String symbol;
        private final String currency;
        private final List<StockPricePoint> prices;

        public StockChartData(String symbol, String currency, List<StockPricePoint> prices) {
            this.symbol = symbol;
            this.currency = currency;
            this.prices = prices;
        }

        public String getSymbol() { return symbol; }
        public String getCurrency() { return currency; }
        public List<StockPricePoint> getPrices() { return prices; }
    }

    // get auto complete search 
    public static List<StockSearchResult> searchStocks(String query) {
        List<StockSearchResult> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url = "https://query1.finance.yahoo.com/v1/finance/search?q=" + encodedQuery;

            HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (root.has("quotes")) {
                    JsonArray quotes = root.getAsJsonArray("quotes");
                    for (JsonElement el : quotes) {
                        JsonObject q = el.getAsJsonObject();
                        
                        // Validasi tipe quote (hanya Equity / Saham)
                        String type = q.has("quoteType") ? q.get("quoteType").getAsString() : "";
                        if (!"EQUITY".equalsIgnoreCase(type)) {
                            continue;
                        }

                        String symbol = q.has("symbol") ? q.get("symbol").getAsString() : "";
                        String name = q.has("longname") ? q.get("longname").getAsString() : 
                                     (q.has("shortname") ? q.get("shortname").getAsString() : symbol);
                        String exchange = q.has("exchange") ? q.get("exchange").getAsString() : "";

                        if (!symbol.isEmpty()) {
                            results.add(new StockSearchResult(symbol, name, exchange));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memanggil Yahoo Finance Search API: " + e.getMessage());
        }
        return results;
    }

    // fetch graphical data
    public static StockChartData getChartData(String symbol) {
        List<StockPricePoint> prices = new ArrayList<>();
        String currency = "USD"; // default
        if (symbol == null || symbol.trim().isEmpty()) {
            return new StockChartData("", currency, prices);
        }
        try {
            String encodedSymbol = URLEncoder.encode(symbol.trim(), StandardCharsets.UTF_8);
            // Ambil data chart harian (interval=1d) untuk 1 bulan terakhir (range=1mo)
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + encodedSymbol + "?interval=1d&range=1mo";

            HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (root.has("chart")) {
                    JsonObject chart = root.getAsJsonObject("chart");
                    Gson gson = new Gson();
                    System.out.println(gson.toJson(root));
                    if (chart.has("result") && !chart.get("result").isJsonNull()) {
                        JsonArray resultArr = chart.getAsJsonArray("result");
                        if (resultArr.size() > 0) {
                            JsonObject result = resultArr.get(0).getAsJsonObject();
                            if (result.has("meta")) {
                                JsonObject meta = result.getAsJsonObject("meta");
                                if (meta.has("currency")) {
                                    currency = meta.get("currency").getAsString();
                                }
                            }
                            if (result.has("timestamp") && result.has("indicators")) {
                                JsonArray timestamps = result.getAsJsonArray("timestamp");
                                JsonObject indicators = result.getAsJsonObject("indicators");
                                if (indicators.has("quote")) {
                                    JsonArray quoteArr = indicators.getAsJsonArray("quote");
                                    if (quoteArr.size() > 0) {
                                        JsonObject quote = quoteArr.get(0).getAsJsonObject();
                                        if (quote.has("close")) {
                                            JsonArray closes = quote.getAsJsonArray("close");
                                            int size = Math.min(timestamps.size(), closes.size());
                                            for (int i = 0; i < size; i++) {
                                                if (timestamps.get(i).isJsonNull() || closes.get(i).isJsonNull()) {
                                                    continue;
                                                }
                                                long time = timestamps.get(i).getAsLong();
                                                double price = closes.get(i).getAsDouble();
                                                prices.add(new StockPricePoint(time, price));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memanggil Yahoo Finance Chart API: " + e.getMessage());
        }
        return new StockChartData(symbol, currency, prices);
    }

    private static double cachedRate = 16000.0;
    private static long lastCacheTime = 0;

    public static double getUsdIdrRate() {
        long now = System.currentTimeMillis();
        // Cache rate for 1 hour to avoid excessive calls
        if (now - lastCacheTime < 3600000 && cachedRate > 0) {
            return cachedRate;
        }
        try {
            StockChartData data = getChartData("USDIDR=X");
            if (!data.getPrices().isEmpty()) {
                double rate = data.getPrices().get(data.getPrices().size() - 1).getPrice();
                if (rate > 0) {
                    cachedRate = rate;
                    lastCacheTime = now;
                    return rate;
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal mengambil kurs USD/IDR: " + e.getMessage());
        }
        return cachedRate;
    }

    public static double convertPrice(double price, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            return price;
        }
        fromCurrency = fromCurrency.toUpperCase();
        toCurrency = toCurrency.toUpperCase();
        if (fromCurrency.equals(toCurrency)) {
            return price;
        }

        double rate = getUsdIdrRate();

        if ("USD".equals(fromCurrency) && "IDR".equals(toCurrency)) {
            return price * rate;
        } else if ("IDR".equals(fromCurrency) && "USD".equals(toCurrency)) {
            return price / rate;
        }
        return price; // fallback if currency is unsupported
    }
}
