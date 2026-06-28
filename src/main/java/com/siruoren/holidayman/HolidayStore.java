package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.XmlFile;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HolidayStore {

    private static final Logger LOGGER = Logger.getLogger(HolidayStore.class.getName());
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static volatile HolidayStore instance;

    private final Map<String, HolidayDate> holidayMap = new ConcurrentHashMap<>();
    private final XmlFile configFile;

    private HolidayStore() {
        File dir = new File(Jenkins.get().getRootDir(), "holiday-management");
        dir.mkdirs();
        this.configFile = new XmlFile(new File(dir, "holidays.xml"));
        load();
    }

    public static synchronized HolidayStore getInstance() {
        if (instance == null) {
            instance = new HolidayStore();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    private void load() {
        holidayMap.clear();
        if (configFile.exists()) {
            try {
                ConfigData data = (ConfigData) configFile.unmarshal(ConfigData.class);
                if (data != null && data.entries != null) {
                    for (HolidayDate hd : data.entries) {
                        holidayMap.put(hd.getDate(), hd);
                    }
                }
                LOGGER.info("Loaded " + holidayMap.size() + " holiday entries from storage");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load holiday data", e);
            }
        }
    }

    private void save() {
        try {
            ConfigData data = new ConfigData();
            data.entries = new ArrayList<>(holidayMap.values());
            Collections.sort(data.entries);
            configFile.write(data);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save holiday data", e);
        }
    }

    public HolidayDate getHolidayDate(@NonNull LocalDate date) {
        return holidayMap.get(date.format(FORMATTER));
    }

    public List<HolidayDate> getHolidaysForYear(int year) {
        String prefix = year + "-";
        List<HolidayDate> result = new ArrayList<>();
        for (HolidayDate hd : holidayMap.values()) {
            if (hd.getDate().startsWith(prefix)) {
                result.add(hd);
            }
        }
        Collections.sort(result);
        return result;
    }

    public void saveHolidays(int year, @NonNull List<HolidayDate> holidays) {
        // Remove existing entries for this year
        String prefix = year + "-";
        holidayMap.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        // Add new entries
        for (HolidayDate hd : holidays) {
            holidayMap.put(hd.getDate(), hd);
        }
        save();
    }

    public void addHoliday(@NonNull HolidayDate hd) {
        holidayMap.put(hd.getDate(), hd);
        save();
    }

    public boolean removeHoliday(@NonNull String date) {
        HolidayDate removed = holidayMap.remove(date);
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public List<Integer> getAvailableYears() {
        Set<Integer> years = new TreeSet<>();
        for (HolidayDate hd : holidayMap.values()) {
            years.add(hd.getYear());
        }
        return new ArrayList<>(years);
    }

    public boolean removeYear(int year) {
        String prefix = year + "-";
        boolean removed = holidayMap.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        if (removed) {
            save();
        }
        return removed;
    }

    public int getTotalCount() {
        return holidayMap.size();
    }

    private static class ConfigData {
        private List<HolidayDate> entries;

        @SuppressWarnings("unused")
        public ConfigData() {
        }

        @SuppressWarnings("unused")
        public List<HolidayDate> getEntries() {
            return entries;
        }

        @SuppressWarnings("unused")
        public void setEntries(List<HolidayDate> entries) {
            this.entries = entries;
        }
    }
}
