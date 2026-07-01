package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Contributes DAY_IS_WORKDAY, DAY_IS_HOLIDAY and HOLIDAY_NAME environment variables
 * to builds, allowing Freestyle and Pipeline jobs to check holiday status.
 * <p>
 * Available environment variables:
 * - DAY_IS_WORKDAY: true if today is a workday (regular weekday or adjusted workday)
 * - DAY_IS_HOLIDAY: true if today is an explicitly marked holiday
 * - HOLIDAY_NAME: the name of today's holiday (only set when DAY_IS_HOLIDAY is true)
 */
@Extension
public class HolidayEnvironmentContributor extends EnvironmentContributor {

    @Override
    public void buildEnvironmentFor(@NonNull Run run, @NonNull EnvVars envs, @NonNull TaskListener listener) throws IOException, InterruptedException {
        LocalDate today = LocalDate.now();
        HolidayService service = HolidayService.getInstance();

        boolean isWorkday = service.isWorkday(today);
        boolean isHoliday = service.isHoliday(today);

        envs.put("DAY_IS_WORKDAY", String.valueOf(isWorkday));
        envs.put("DAY_IS_HOLIDAY", String.valueOf(isHoliday));

        if (isHoliday) {
            String name = service.getHolidayName(today);
            envs.put("HOLIDAY_NAME", name != null ? name : "");
        } else {
            envs.put("HOLIDAY_NAME", "");
        }
    }
}
