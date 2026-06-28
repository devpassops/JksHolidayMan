package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class HolidayDate implements Serializable, Comparable<HolidayDate> {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String date;
    private String name;
    private String type; // HOLIDAY or WORKDAY

    public HolidayDate() {
    }

    public HolidayDate(@NonNull String date, @NonNull String name, @NonNull String type) {
        this.date = date;
        this.name = name;
        this.type = type;
    }

    public static HolidayDate holiday(@NonNull LocalDate date, @NonNull String name) {
        return new HolidayDate(date.format(FORMATTER), name, "HOLIDAY");
    }

    public static HolidayDate workday(@NonNull LocalDate date, @NonNull String name) {
        return new HolidayDate(date.format(FORMATTER), name, "WORKDAY");
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isHoliday() {
        return "HOLIDAY".equals(type);
    }

    public boolean isWorkday() {
        return "WORKDAY".equals(type);
    }

    public LocalDate toLocalDate() {
        return LocalDate.parse(date, FORMATTER);
    }

    public int getYear() {
        return toLocalDate().getYear();
    }

    @Override
    public int compareTo(@NonNull HolidayDate o) {
        return this.date.compareTo(o.date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HolidayDate that = (HolidayDate) o;
        return date != null ? date.equals(that.date) : that.date == null;
    }

    @Override
    public int hashCode() {
        return date != null ? date.hashCode() : 0;
    }
}
