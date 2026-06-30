package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class HolidayDate implements Serializable, Comparable<HolidayDate> {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String date;
    private String name;
    private String type; // HOLIDAY or WORKDAY

    public HolidayDate() {
    }

    public HolidayDate(@NonNull String date, @NonNull String name, @NonNull String type) {
        setDate(date);
        setName(name);
        setType(type);
    }

    public static HolidayDate holiday(@NonNull LocalDate date, @NonNull String name) {
        return new HolidayDate(date.format(FORMATTER), sanitizeName(name), "HOLIDAY");
    }

    public static HolidayDate workday(@NonNull LocalDate date, @NonNull String name) {
        return new HolidayDate(date.format(FORMATTER), sanitizeName(name), "WORKDAY");
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        if (date == null || date.isEmpty()) {
            throw new IllegalArgumentException("Date cannot be null or empty");
        }
        try {
            LocalDate.parse(date, FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + date + ", expected yyyy-MM-dd");
        }
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? sanitizeName(name) : "";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null || (!"HOLIDAY".equals(type) && !"WORKDAY".equals(type))) {
            throw new IllegalArgumentException("Invalid type: " + type + ", must be HOLIDAY or WORKDAY");
        }
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

    private static String sanitizeName(@NonNull String name) {
        if (name == null) return "";
        // Remove potentially dangerous characters
        return name.replaceAll("[<>\"'&\n\r\t]", "").trim();
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
        return Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }

    @Override
    public String toString() {
        return "HolidayDate{" +
                "date='" + date + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
