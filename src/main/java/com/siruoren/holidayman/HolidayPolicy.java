package com.siruoren.holidayman;

public enum HolidayPolicy {
    EXCLUDE_HOLIDAYS(Messages.HolidayPolicy_EXCLUDE_HOLIDAYS()),
    INCLUDE_HOLIDAYS(Messages.HolidayPolicy_INCLUDE_HOLIDAYS()),
    ONLY_HOLIDAYS(Messages.HolidayPolicy_ONLY_HOLIDAYS());

    private final String displayName;

    HolidayPolicy(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
