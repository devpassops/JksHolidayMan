package com.siruoren.holidayman;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

public class HolidayServiceTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private HolidayService getService() {
        return HolidayService.getInstance();
    }

    @Before
    public void cleanup() {
        HolidayService service = getService();
        for (int year : service.getAvailableYears()) {
            service.removeYear(year);
        }
    }

    @Test
    public void testAddAndRetrieveHoliday() {
        HolidayService service = getService();
        HolidayDate hd = HolidayDate.holiday(LocalDate.of(2026, 1, 1), "元旦");
        service.addHoliday(hd);

        List<HolidayDate> holidays = service.getHolidaysForYear(2026);
        assertFalse(holidays.isEmpty());

        boolean found = holidays.stream().anyMatch(h -> "2026-01-01".equals(h.getDate()) && h.isHoliday());
        assertTrue(found);
    }

    @Test
    public void testAddWorkdayAdjustment() {
        HolidayService service = getService();
        // Feb 7 2026 (Saturday) is a workday due to Spring Festival adjustment
        HolidayDate wd = HolidayDate.workday(LocalDate.of(2026, 2, 7), "春节调休");
        service.addHoliday(wd);

        LocalDate date = LocalDate.of(2026, 2, 7);
        assertTrue(service.isWorkday(date));
        assertFalse(service.isWeekend(date));
        assertFalse(service.isHoliday(date));
    }

    @Test
    public void testIsHolidayTrue() {
        HolidayService service = getService();
        service.addHoliday(HolidayDate.holiday(LocalDate.of(2026, 10, 1), "国庆节"));

        assertTrue(service.isHoliday(LocalDate.of(2026, 10, 1)));
    }

    @Test
    public void testIsHolidayFalse() {
        HolidayService service = getService();
        assertFalse(service.isHoliday(LocalDate.of(2026, 3, 15)));
    }

    @Test
    public void testIsWorkdayNormalWeekday() {
        HolidayService service = getService();
        // March 16, 2026 is Monday
        LocalDate monday = LocalDate.of(2026, 3, 16);
        assertEquals(DayOfWeek.MONDAY, monday.getDayOfWeek());
        assertTrue(service.isWorkday(monday));
    }

    @Test
    public void testIsWorkdayNormalWeekend() {
        HolidayService service = getService();
        // March 15, 2026 is Sunday
        LocalDate sunday = LocalDate.of(2026, 3, 15);
        assertEquals(DayOfWeek.SUNDAY, sunday.getDayOfWeek());
        assertFalse(service.isWorkday(sunday));
    }

    @Test
    public void testIsWeekendRegularWeekend() {
        HolidayService service = getService();
        // March 14, 2026 is Saturday
        LocalDate saturday = LocalDate.of(2026, 3, 14);
        assertEquals(DayOfWeek.SATURDAY, saturday.getDayOfWeek());
        assertTrue(service.isWeekend(saturday));
    }

    @Test
    public void testIsWeekendNotForAdjustedWorkday() {
        HolidayService service = getService();
        // Mark Saturday as adjusted workday
        LocalDate saturday = LocalDate.of(2026, 3, 14);
        service.addHoliday(HolidayDate.workday(saturday, "调休工作日"));

        assertFalse(service.isWeekend(saturday));
    }

    @Test
    public void testIsWeekendNotForExplicitHoliday() {
        HolidayService service = getService();
        // Mark a Monday as holiday
        LocalDate monday = LocalDate.of(2026, 3, 16);
        service.addHoliday(HolidayDate.holiday(monday, "特殊假日"));

        assertFalse(service.isWeekend(monday));
    }

    @Test
    public void testShouldRunExcludeHolidays() {
        HolidayService service = getService();
        service.addHoliday(HolidayDate.holiday(LocalDate.of(2026, 1, 1), "元旦"));

        // Holiday - should NOT run
        assertFalse(service.shouldRun(LocalDate.of(2026, 1, 1), HolidayPolicy.EXCLUDE_HOLIDAYS));

        // Normal weekday - should run
        assertTrue(service.shouldRun(LocalDate.of(2026, 3, 16), HolidayPolicy.EXCLUDE_HOLIDAYS));

        // Normal weekend - should NOT run
        assertFalse(service.shouldRun(LocalDate.of(2026, 3, 14), HolidayPolicy.EXCLUDE_HOLIDAYS));
    }

    @Test
    public void testShouldRunExcludeHolidaysWithWorkdayAdjustment() {
        HolidayService service = getService();
        // Saturday is adjusted workday
        LocalDate saturday = LocalDate.of(2026, 3, 14);
        service.addHoliday(HolidayDate.workday(saturday, "调休工作日"));

        // Should run on adjusted workday even though it's Saturday
        assertTrue(service.shouldRun(saturday, HolidayPolicy.EXCLUDE_HOLIDAYS));
    }

    @Test
    public void testShouldRunIncludeHolidays() {
        HolidayService service = getService();

        // All dates should run with INCLUDE_HOLIDAYS
        assertTrue(service.shouldRun(LocalDate.of(2026, 1, 1), HolidayPolicy.INCLUDE_HOLIDAYS));
        assertTrue(service.shouldRun(LocalDate.of(2026, 3, 14), HolidayPolicy.INCLUDE_HOLIDAYS));
        assertTrue(service.shouldRun(LocalDate.of(2026, 3, 16), HolidayPolicy.INCLUDE_HOLIDAYS));
    }

    @Test
    public void testShouldRunOnlyHolidays() {
        HolidayService service = getService();
        service.addHoliday(HolidayDate.holiday(LocalDate.of(2026, 1, 1), "元旦"));

        // Holiday - should run
        assertTrue(service.shouldRun(LocalDate.of(2026, 1, 1), HolidayPolicy.ONLY_HOLIDAYS));

        // Normal weekday - should NOT run
        assertFalse(service.shouldRun(LocalDate.of(2026, 3, 16), HolidayPolicy.ONLY_HOLIDAYS));

        // Normal weekend - should NOT run
        assertFalse(service.shouldRun(LocalDate.of(2026, 3, 14), HolidayPolicy.ONLY_HOLIDAYS));
    }

    @Test
    public void testShouldRunOnlyHolidaysExcludesWorkdayAdjustments() {
        HolidayService service = getService();
        service.addHoliday(HolidayDate.workday(LocalDate.of(2026, 3, 14), "调休"));

        // Adjusted workday is NOT a holiday, should NOT run with ONLY_HOLIDAYS
        assertFalse(service.shouldRun(LocalDate.of(2026, 3, 14), HolidayPolicy.ONLY_HOLIDAYS));
    }

    @Test
    public void testGetHolidayName() {
        HolidayService service = getService();
        service.addHoliday(HolidayDate.holiday(LocalDate.of(2026, 5, 1), "劳动节"));

        assertEquals("劳动节", service.getHolidayName(LocalDate.of(2026, 5, 1)));
        assertNull(service.getHolidayName(LocalDate.of(2026, 5, 2)));
    }

    @Test
    public void testGetHolidayNameNullForWorkdayAdjustment() {
        HolidayService service = getService();
        service.addHoliday(HolidayDate.workday(LocalDate.of(2026, 2, 7), "春节调休"));

        // Workday adjustment is not a holiday
        assertNull(service.getHolidayName(LocalDate.of(2026, 2, 7)));
    }

    @Test
    public void testRemoveHoliday() {
        HolidayService service = getService();
        service.addHoliday(HolidayDate.holiday(LocalDate.of(2026, 12, 25), "圣诞节"));

        assertTrue(service.removeHoliday("2026-12-25"));
        assertFalse(service.isHoliday(LocalDate.of(2026, 12, 25)));
    }

    @Test
    public void testRemoveNonExistentHoliday() {
        HolidayService service = getService();
        assertFalse(service.removeHoliday("2099-12-31"));
    }

    @Test
    public void testRemoveYear() {
        HolidayService service = getService();
        service.addHoliday(HolidayDate.holiday(LocalDate.of(2026, 1, 1), "元旦"));
        service.addHoliday(HolidayDate.holiday(LocalDate.of(2026, 10, 1), "国庆节"));

        assertTrue(service.removeYear(2026));
        assertTrue(service.getHolidaysForYear(2026).isEmpty());
    }

    @Test
    public void testGetAvailableYears() {
        HolidayService service = getService();
        service.addHoliday(HolidayDate.holiday(LocalDate.of(2026, 1, 1), "元旦"));
        service.addHoliday(HolidayDate.holiday(LocalDate.of(2027, 1, 1), "元旦"));

        List<Integer> years = service.getAvailableYears();
        assertTrue(years.contains(2026));
        assertTrue(years.contains(2027));
    }

    @Test
    public void testImportFromJsonArrayFormat() throws Exception {
        HolidayService service = getService();
        String json = "[{\"date\":\"2026-06-01\",\"name\":\"儿童节\",\"type\":\"HOLIDAY\"}," +
                "{\"date\":\"2026-06-14\",\"name\":\"调休\",\"type\":\"WORKDAY\"}]";

        int count = service.importFromJson(json);
        assertEquals(2, count);

        assertTrue(service.isHoliday(LocalDate.of(2026, 6, 1)));
        assertTrue(service.isWorkday(LocalDate.of(2026, 6, 14)));
    }

    @Test
    public void testImportFromJsonYearMapFormat() throws Exception {
        HolidayService service = getService();
        String json = "{\"2026\":[{\"date\":\"2026-05-01\",\"name\":\"劳动节\",\"type\":\"HOLIDAY\"}]}";

        int count = service.importFromJson(json);
        assertEquals(1, count);
        assertTrue(service.isHoliday(LocalDate.of(2026, 5, 1)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImportFromJsonInvalidFormat() throws Exception {
        HolidayService service = getService();
        service.importFromJson("not json");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImportFromJsonEmpty() throws Exception {
        HolidayService service = getService();
        service.importFromJson("");
    }

    @Test
    public void testExportYearToJson() throws Exception {
        HolidayService service = getService();
        service.addHoliday(HolidayDate.holiday(LocalDate.of(2026, 1, 1), "元旦"));

        String json = service.exportYearToJson(2026);
        assertNotNull(json);
        assertTrue(json.contains("2026-01-01"));
        assertTrue(json.contains("元旦"));
        assertTrue(json.contains("HOLIDAY"));
    }

    @Test
    public void testExportAllToJson() throws Exception {
        HolidayService service = getService();
        service.addHoliday(HolidayDate.holiday(LocalDate.of(2026, 1, 1), "元旦"));

        String json = service.exportAllToJson();
        assertNotNull(json);
        assertTrue(json.contains("2026"));
    }
}
