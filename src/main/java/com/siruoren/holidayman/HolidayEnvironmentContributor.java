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
 * Contributes HOLIDAY_IS_WORKDAY and HOLIDAY_IS_HOLIDAY environment variables
 * to builds, allowing Freestyle and Pipeline jobs to check holiday status.
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
            if (name != null) {
                envs.put("HOLIDAY_NAME", name);
            }
        }
    }
}
