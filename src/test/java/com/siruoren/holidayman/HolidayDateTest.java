package com.siruoren.holidayman;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDate;

public class HolidayDateTest {

    private static final String DATE_2026_01_01 = "2026-01-01";
    private static final String DATE_2026_02_07 = "2026-02-07";

    @Test
    public void testHolidayFactory() {
        HolidayDate hd = HolidayDate.holiday(LocalDate.of(2026, 1, 1), "元旦");
        assertEquals(DATE_2026_01_01, hd.getDate());
        assertEquals("元旦", hd.getName());
        assertEquals("HOLIDAY", hd.getType());
        assertTrue(hd.isHoliday());
        assertFalse(hd.isWorkday());
    }

    @Test
    public void testWorkdayFactory() {
        HolidayDate wd = HolidayDate.workday(LocalDate.of(2026, 2, 7), "春节调休");
        assertEquals(DATE_2026_02_07, wd.getDate());
        assertEquals("春节调休", wd.getName());
        assertEquals("WORKDAY", wd.getType());
        assertTrue(wd.isWorkday());
        assertFalse(wd.isHoliday());
    }

    @Test
    public void testConstructor() {
        HolidayDate hd = new HolidayDate(DATE_2026_01_01, "元旦", "HOLIDAY");
        assertEquals(DATE_2026_01_01, hd.getDate());
        assertEquals("元旦", hd.getName());
        assertEquals("HOLIDAY", hd.getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidDate() {
        new HolidayDate("not-a-date", "test", "HOLIDAY");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyDate() {
        new HolidayDate("", "test", "HOLIDAY");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullDate() {
        new HolidayDate(null, "test", "HOLIDAY");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidType() {
        new HolidayDate(DATE_2026_01_01, "test", "INVALID");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullType() {
        new HolidayDate(DATE_2026_01_01, "test", null);
    }

    @Test
    public void testToLocalDate() {
        HolidayDate hd = HolidayDate.holiday(LocalDate.of(2026, 5, 1), "劳动节");
        assertEquals(LocalDate.of(2026, 5, 1), hd.toLocalDate());
    }

    @Test
    public void testGetYear() {
        HolidayDate hd = HolidayDate.holiday(LocalDate.of(2026, 10, 1), "国庆节");
        assertEquals(2026, hd.getYear());
    }

    @Test
    public void testEqualsSameDate() {
        HolidayDate hd1 = new HolidayDate(DATE_2026_01_01, "元旦", "HOLIDAY");
        HolidayDate hd2 = new HolidayDate(DATE_2026_01_01, "Different Name", "WORKDAY");
        assertEquals(hd1, hd2); // equality is based on date only
    }

    @Test
    public void testEqualsDifferentDate() {
        HolidayDate hd1 = HolidayDate.holiday(LocalDate.of(2026, 1, 1), "元旦");
        HolidayDate hd2 = HolidayDate.holiday(LocalDate.of(2026, 1, 2), "Other");
        assertNotEquals(hd1, hd2);
    }

    @Test
    public void testCompareTo() {
        HolidayDate hd1 = HolidayDate.holiday(LocalDate.of(2026, 1, 1), "元旦");
        HolidayDate hd2 = HolidayDate.holiday(LocalDate.of(2026, 1, 2), "Other");
        assertTrue(hd1.compareTo(hd2) < 0);
        assertTrue(hd2.compareTo(hd1) > 0);
        assertEquals(0, hd1.compareTo(hd1));
    }

    @Test
    public void testSanitizeName() {
        HolidayDate hd = new HolidayDate(DATE_2026_01_01, "<script>alert('xss')</script>", "HOLIDAY");
        assertFalse(hd.getName().contains("<"));
        assertFalse(hd.getName().contains(">"));
    }

    @Test
    public void testNullNameBecomesEmpty() {
        HolidayDate hd = new HolidayDate();
        hd.setDate(DATE_2026_01_01);
        hd.setName(null);
        hd.setType("HOLIDAY");
        assertEquals("", hd.getName());
    }

    @Test
    public void testHashCodeConsistency() {
        HolidayDate hd = HolidayDate.holiday(LocalDate.of(2026, 1, 1), "元旦");
        assertEquals(hd.hashCode(), hd.hashCode());
    }
}
