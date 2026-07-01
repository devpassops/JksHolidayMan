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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HolidayStore {

    private static final Logger LOGGER = Logger.getLogger(HolidayStore.class.getName());
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static volatile HolidayStore instance;

    private final Map<String, HolidayDate> holidayMap = new ConcurrentHashMap<>();
    private final Map<Integer, List<HolidayDate>> yearCache = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final XmlFile configFile;
    private volatile long lastModified = 0;

    private HolidayStore() {
        File dir = new File(Jenkins.get().getRootDir(), "holiday-management");
        dir.mkdirs();
        this.configFile = new XmlFile(new File(dir, "holidays.xml"));
        load();
    }

    public static HolidayStore getInstance() {
        HolidayStore result = instance;
        if (result == null) {
            synchronized (HolidayStore.class) {
                result = instance;
                if (result == null) {
                    instance = result = new HolidayStore();
                }
            }
        }
        return result;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    private void load() {
        lock.writeLock().lock();
        try {
            holidayMap.clear();
            yearCache.clear();
            if (configFile.exists()) {
                try {
                    ConfigData data = (ConfigData) configFile.unmarshal(ConfigData.class);
                    if (data != null && data.entries != null) {
                        for (HolidayDate hd : data.entries) {
                            holidayMap.put(hd.getDate(), hd);
                        }
                    }
                    lastModified = configFile.getFile().lastModified();
                    LOGGER.info("Loaded " + holidayMap.size() + " holiday entries from storage");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Failed to load holiday data", e);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void save() {
        lock.writeLock().lock();
        try {
            ConfigData data = new ConfigData();
            data.entries = new ArrayList<>(holidayMap.values());
            Collections.sort(data.entries);
            configFile.write(data);
            lastModified = System.currentTimeMillis();
            yearCache.clear(); // Invalidate cache on save
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save holiday data", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public HolidayDate getHolidayDate(@NonNull LocalDate date) {
        lock.readLock().lock();
        try {
            return holidayMap.get(date.format(FORMATTER));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<HolidayDate> getHolidaysForYear(int year) {
        lock.readLock().lock();
        try {
            // Check cache first
            List<HolidayDate> cached = yearCache.get(year);
            if (cached != null) {
                return new ArrayList<>(cached);
            }

            // Build cache entry
            String prefix = year + "-";
            List<HolidayDate> result = new ArrayList<>();
            for (HolidayDate hd : holidayMap.values()) {
                if (hd.getDate().startsWith(prefix)) {
                    result.add(hd);
                }
            }
            Collections.sort(result);
            
            // Cache the result (immutable copy)
            List<HolidayDate> immutableResult = Collections.unmodifiableList(result);
            yearCache.put(year, immutableResult);
            
            return new ArrayList<>(result);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void saveHolidays(int year, @NonNull List<HolidayDate> holidays) {
        validateHolidayDates(holidays);
        lock.writeLock().lock();
        try {
            // Remove existing entries for this year
            String prefix = year + "-";
            holidayMap.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
            yearCache.remove(year); // Invalidate cache
            
            // Add new entries
            for (HolidayDate hd : holidays) {
                holidayMap.put(hd.getDate(), hd);
            }
            save();
            LOGGER.info("Saved " + holidays.size() + " holiday entries for year " + year);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addHoliday(@NonNull HolidayDate hd) {
        validateHolidayDate(hd);
        lock.writeLock().lock();
        try {
            holidayMap.put(hd.getDate(), hd);
            // Invalidate year cache
            int year = hd.getYear();
            yearCache.remove(year);
            save();
            LOGGER.info("Added holiday: " + hd.getDate() + " - " + hd.getName());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean removeHoliday(@NonNull String date) {
        validateDate(date);
        lock.writeLock().lock();
        try {
            HolidayDate removed = holidayMap.remove(date);
            if (removed != null) {
                // Invalidate year cache
                yearCache.remove(removed.getYear());
                save();
                LOGGER.info("Removed holiday: " + date);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Integer> getAvailableYears() {
        lock.readLock().lock();
        try {
            Set<Integer> years = new TreeSet<>();
            for (HolidayDate hd : holidayMap.values()) {
                years.add(hd.getYear());
            }
            return new ArrayList<>(years);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean removeYear(int year) {
        validateYear(year);
        lock.writeLock().lock();
        try {
            String prefix = year + "-";
            boolean removed = holidayMap.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
            if (removed) {
                yearCache.remove(year);
                save();
                LOGGER.info("Removed all holidays for year " + year);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getTotalCount() {
        lock.readLock().lock();
        try {
            return holidayMap.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void validateHolidayDate(@NonNull HolidayDate hd) {
        if (hd.getDate() == null || hd.getDate().isEmpty()) {
            throw new IllegalArgumentException("Holiday date cannot be null or empty");
        }
        validateDate(hd.getDate());
        if (hd.getType() == null || (!"HOLIDAY".equals(hd.getType()) && !"WORKDAY".equals(hd.getType()))) {
            throw new IllegalArgumentException("Invalid holiday type: " + hd.getType());
        }
    }

    private void validateHolidayDates(@NonNull List<HolidayDate> holidays) {
        for (HolidayDate hd : holidays) {
            validateHolidayDate(hd);
        }
    }

    private void validateDate(@NonNull String date) {
        try {
            LocalDate.parse(date, FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + date + ", expected yyyy-MM-dd");
        }
    }

    private void validateYear(int year) {
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Invalid year: " + year + ", must be between 1900 and 2100");
        }
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
