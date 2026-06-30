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
import java.util.logging.Level;
import java.util.logging.Logger;

public class HolidayApiClient {

    private static final Logger LOGGER = Logger.getLogger(HolidayApiClient.class.getName());
    private static final String API_URL = "https://timor.tech/api/holiday/year/";
    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;

    public List<HolidayDate> fetchHolidays(int year) throws Exception {
        String url = API_URL + year;
        LOGGER.info("Fetching holiday data from: " + url);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("Accept", "application/json");

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

    private List<HolidayDate> parseApiResponse(@NonNull String json, int year) throws Exception {
        List<HolidayDate> result = new ArrayList<>();
        JSONObject root = JSONObject.fromObject(json);

        if (root.has("code") && root.getInt("code") != 0) {
            throw new RuntimeException("API error: " + root.optString("msg", "Unknown error"));
        }

        JSONObject holidayNode = root.optJSONObject("holiday");
        if (holidayNode == null || holidayNode.isEmpty()) {
            LOGGER.warning("No holiday data found in API response for year " + year);
            return result;
        }

        Iterator<?> keys = holidayNode.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            String dateStr = year + "-" + key;
            JSONObject detail = holidayNode.optJSONObject(key);

            try {
                LocalDate date = LocalDate.parse(dateStr, INPUT_FORMAT);
                String name = detail != null ? detail.optString("name", "") : "";
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
}
