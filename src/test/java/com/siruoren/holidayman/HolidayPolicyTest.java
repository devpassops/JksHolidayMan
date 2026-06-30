package com.siruoren.holidayman;

import org.junit.Test;
import static org.junit.Assert.*;

public class HolidayPolicyTest {

    @Test
    public void testThreePoliciesExist() {
        assertEquals(3, HolidayPolicy.values().length);
    }

    @Test
    public void testPolicyValues() {
        assertNotNull(HolidayPolicy.EXCLUDE_HOLIDAYS);
        assertNotNull(HolidayPolicy.INCLUDE_HOLIDAYS);
        assertNotNull(HolidayPolicy.ONLY_HOLIDAYS);
    }

    @Test
    public void testValueOf() {
        assertEquals(HolidayPolicy.EXCLUDE_HOLIDAYS, HolidayPolicy.valueOf("EXCLUDE_HOLIDAYS"));
        assertEquals(HolidayPolicy.INCLUDE_HOLIDAYS, HolidayPolicy.valueOf("INCLUDE_HOLIDAYS"));
        assertEquals(HolidayPolicy.ONLY_HOLIDAYS, HolidayPolicy.valueOf("ONLY_HOLIDAYS"));
    }

    @Test
    public void testDisplayNameNotNull() {
        for (HolidayPolicy policy : HolidayPolicy.values()) {
            assertNotNull(policy.getDisplayName());
            assertFalse(policy.getDisplayName().isEmpty());
        }
    }
}
