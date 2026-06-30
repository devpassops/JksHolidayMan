package com.siruoren.holidayman;

import antlr.ANTLRException;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.TimerTrigger;
import hudson.util.FormValidation;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.time.LocalDate;
import java.util.logging.Logger;

public class HolidayTimerTrigger extends TimerTrigger {

    private static final Logger LOGGER = Logger.getLogger(HolidayTimerTrigger.class.getName());

    private HolidayPolicy holidayPolicy;

    @DataBoundConstructor
    public HolidayTimerTrigger(String spec, HolidayPolicy holidayPolicy) throws ANTLRException {
        super(spec);
        this.holidayPolicy = holidayPolicy != null ? holidayPolicy : HolidayPolicy.EXCLUDE_HOLIDAYS;
    }

    public HolidayPolicy getHolidayPolicy() {
        return holidayPolicy != null ? holidayPolicy : HolidayPolicy.EXCLUDE_HOLIDAYS;
    }

    public void setHolidayPolicy(HolidayPolicy holidayPolicy) {
        this.holidayPolicy = holidayPolicy;
    }

    @Override
    public void run() {
        Item item = job;
        if (!(item instanceof Job)) {
            super.run();
            return;
        }
        Job<?, ?> j = (Job<?, ?>) item;

        LocalDate today = LocalDate.now();
        HolidayService service = HolidayService.getInstance();
        HolidayPolicy policy = getHolidayPolicy();

        // If no holiday data for current year, fall back to normal timer behavior
        int currentYear = today.getYear();
        if (!service.getAvailableYears().contains(currentYear)) {
            LOGGER.warning("No holiday data for year " + currentYear + " - falling back to normal timer for '" + j.getFullName() + "'");
            super.run();
            return;
        }

        if (!service.shouldRun(today, policy)) {
            String holidayName = service.getHolidayName(today);
            String reason = holidayName != null
                    ? "holiday: " + holidayName
                    : (service.isWeekend(today) ? "weekend" : "non-workday");
            LOGGER.info("Skipping scheduled build for '" + j.getFullName() + "' due to " + reason + " (" + today + ")");
            return;
        }

        LOGGER.fine("Triggering scheduled build for '" + j.getFullName() + "' on " + today);
        super.run();
    }

    @Extension
    @Symbol("holidayCron")
    public static class DescriptorImpl extends TimerTrigger.DescriptorImpl {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.HolidayTimerTrigger_displayName();
        }

        public FormValidation doCheckSpec(@AncestorInPath Item item, @QueryParameter String value) {
            return FormValidation.ok();
        }

        public HolidayPolicy[] getHolidayPolicies() {
            return HolidayPolicy.values();
        }

        public HolidayPolicy getDefaultPolicy() {
            return HolidayPolicy.EXCLUDE_HOLIDAYS;
        }

        public boolean isCurrentYearMissing() {
            int currentYear = LocalDate.now().getYear();
            return !HolidayService.getInstance().getAvailableYears().contains(currentYear);
        }
    }

    public static class HolidayTimerCause extends Cause {

        private final String reason;

        public HolidayTimerCause() {
            this.reason = Messages.HolidayTimerTrigger_causeDescription();
        }

        @Override
        @NonNull
        public String getShortDescription() {
            return reason;
        }
    }
}
