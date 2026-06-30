package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HolidayApiClient {

    private static final Logger LOGGER = Logger.getLogger(HolidayApiClient.class.getName());
    private static final String API_URL = "https://timor.tech/api/holiday/year/";
    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    
    // Rate limiting: track requests per minute
    private static final ConcurrentHashMap<Long, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private static volatile long lastCleanupTime = System.currentTimeMillis();

    public List<HolidayDate> fetchHolidays(int year) throws Exception {
        validateYear(year);
        checkRateLimit();
        
        String url = API_URL + year;
        LOGGER.info("Fetching holiday data from: " + url);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Jenkins-HolidayManagement/1.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("API returned HTTP " + responseCode);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            return parseApiResponse(response.toString(), year);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void checkRateLimit() throws Exception {
        long currentMinute = System.currentTimeMillis() / 60000;
        
        // Clean up old entries periodically
        if (System.currentTimeMillis() - lastCleanupTime > TimeUnit.MINUTES.toMillis(5)) {
            long fiveMinutesAgo = (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)) / 60000;
            requestCounts.keySet().removeIf(key -> key < fiveMinutesAgo);
            lastCleanupTime = System.currentTimeMillis();
        }
        
        AtomicInteger count = requestCounts.computeIfAbsent(currentMinute, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();
        
        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            count.decrementAndGet();
            throw new RuntimeException("Rate limit exceeded: Maximum " + MAX_REQUESTS_PER_MINUTE + 
                " requests per minute allowed. Please try again later.");
        }
    }

    private void validateYear(int year) {
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Invalid year: " + year + ", must be between 1900 and 2100");
        }
    }

    private List<HolidayDate> parseApiResponse(@NonNull String json, int year) throws Exception {
        if (json == null || json.isEmpty()) {
            throw new IllegalArgumentException("API response is empty");
        }
        
        List<HolidayDate> result = new ArrayList<>();
        JSONObject root;
        
        try {
            root = JSONObject.fromObject(json);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse JSON response from API", e);
            throw new RuntimeException("Invalid JSON response from API", e);
        }

        if (root.has("code") && root.getInt("code") != 0) {
            String errorMsg = root.optString("msg", "Unknown error");
            LOGGER.warning("API error for year " + year + ": " + errorMsg);
            throw new RuntimeException("API error: " + errorMsg);
        }

        JSONObject holidayNode = root.optJSONObject("holiday");
        if (holidayNode == null || holidayNode.isEmpty()) {
            LOGGER.warning("No holiday data found in API response for year " + year);
            return result;
        }

        Iterator<?> keys = holidayNode.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (key == null || key.isEmpty()) continue;
            
            String dateStr = year + "-" + key;
            JSONObject detail = holidayNode.optJSONObject(key);

            try {
                LocalDate date = LocalDate.parse(dateStr, INPUT_FORMAT);
                String name = detail != null ? sanitizeString(detail.optString("name", "")) : "";
                boolean isHoliday = detail != null ? detail.optBoolean("holiday", true) : true;

                HolidayDate hd;
                if (isHoliday) {
                    hd = HolidayDate.holiday(date, name);
                } else {
                    hd = HolidayDate.workday(date, name);
                }
                result.add(hd);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to parse holiday entry: " + dateStr, e);
            }
        }

        LOGGER.info("Parsed " + result.size() + " holiday entries for year " + year);
        return result;
    }

    private String sanitizeString(@NonNull String input) {
        if (input == null) return "";
        // Remove potentially dangerous characters
        return input.replaceAll("[<>\"'&]", "");
    }
}
