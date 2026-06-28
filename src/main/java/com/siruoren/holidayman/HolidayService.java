package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.model.Jenkins;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class HolidayService {

    private static final Logger LOGGER = Logger.getLogger(HolidayService.class.getName());

    public static HolidayService getInstance() {
        return Jenkins.get().getExtensionList(HolidayService.class).get(0);
    }

    /**
     * Check if the given date is a holiday (explicitly marked as HOLIDAY).
     */
    public boolean isHoliday(@NonNull LocalDate date) {
        HolidayStore store = HolidayStore.getInstance();
        HolidayDate hd = store.getHolidayDate(date);
        return hd != null && hd.isHoliday();
    }

    /**
     * Check if the given date is a workday (including 调休 workdays on weekends).
     */
    public boolean isWorkday(@NonNull LocalDate date) {
        HolidayStore store = HolidayStore.getInstance();
        HolidayDate hd = store.getHolidayDate(date);
        if (hd != null) {
            return hd.isWorkday();
        }
        // Not in holiday list - check if weekend
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    /**
     * Check if the given date is a regular weekend (Sat/Sun) that is NOT a 调休 workday.
     */
    public boolean isWeekend(@NonNull LocalDate date) {
        HolidayStore store = HolidayStore.getInstance();
        HolidayDate hd = store.getHolidayDate(date);
        if (hd != null && hd.isWorkday()) {
            return false; // 调休 workday
        }
        if (hd != null && hd.isHoliday()) {
            return false; // explicit holiday, not a regular weekend
        }
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /**
     * Determine if a scheduled build should run on the given date based on policy.
     * EXCLUDE_HOLIDAYS: skip holidays and weekends (unless 调休), run on workdays
     * INCLUDE_HOLIDAYS: run on all days
     * ONLY_HOLIDAYS: run only on holidays (explicitly marked HOLIDAY)
     */
    public boolean shouldRun(@NonNull LocalDate date, @NonNull HolidayPolicy policy) {
        if (policy == HolidayPolicy.INCLUDE_HOLIDAYS) {
            return true;
        }
        if (policy == HolidayPolicy.ONLY_HOLIDAYS) {
            return isHoliday(date);
        }
        // EXCLUDE_HOLIDAYS: only run on workdays
        return isWorkday(date);
    }

    /**
     * Get the holiday name for a date, or null if not a holiday.
     */
    public String getHolidayName(@NonNull LocalDate date) {
        HolidayStore store = HolidayStore.getInstance();
        HolidayDate hd = store.getHolidayDate(date);
        if (hd != null && hd.isHoliday()) {
            return hd.getName();
        }
        return null;
    }

    /**
     * Get all holiday dates for a specific year.
     */
    public List<HolidayDate> getHolidaysForYear(int year) {
        HolidayStore store = HolidayStore.getInstance();
        return store.getHolidaysForYear(year);
    }

    /**
     * Import holiday data for a specific year from the official API.
     */
    public int importFromApi(int year) throws Exception {
        HolidayApiClient client = new HolidayApiClient();
        List<HolidayDate> holidays = client.fetchHolidays(year);
        if (holidays.isEmpty()) {
            LOGGER.warning("No holiday data returned for year " + year);
            return 0;
        }
        HolidayStore store = HolidayStore.getInstance();
        store.saveHolidays(year, holidays);
        LOGGER.info("Imported " + holidays.size() + " holiday entries for year " + year);
        return holidays.size();
    }

    /**
     * Add a single holiday entry.
     */
    public void addHoliday(@NonNull HolidayDate holidayDate) {
        HolidayStore store = HolidayStore.getInstance();
        store.addHoliday(holidayDate);
        LOGGER.info("Added holiday: " + holidayDate.getDate() + " - " + holidayDate.getName() + " (" + holidayDate.getType() + ")");
    }

    /**
     * Remove a holiday entry by date.
     */
    public boolean removeHoliday(@NonNull String date) {
        HolidayStore store = HolidayStore.getInstance();
        boolean removed = store.removeHoliday(date);
        if (removed) {
            LOGGER.info("Removed holiday entry for date: " + date);
        }
        return removed;
    }

    /**
     * Get all years that have holiday data.
     */
    public List<Integer> getAvailableYears() {
        HolidayStore store = HolidayStore.getInstance();
        return store.getAvailableYears();
    }

    /**
     * Remove all holiday data for a specific year.
     */
    public boolean removeYear(int year) {
        HolidayStore store = HolidayStore.getInstance();
        return store.removeYear(year);
    }
}
