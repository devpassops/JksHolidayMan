package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.TimerTrigger;
import hudson.util.FormValidation;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;

import java.time.LocalDate;
import java.util.logging.Logger;

public class HolidayTimerTrigger extends TimerTrigger {

    private static final Logger LOGGER = Logger.getLogger(HolidayTimerTrigger.class.getName());

    private HolidayPolicy holidayPolicy;

    @DataBoundConstructor
    public HolidayTimerTrigger(String spec, HolidayPolicy holidayPolicy) {
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

        // Check per-job property override first
        HolidayPolicy effectivePolicy = getEffectivePolicy(j);

        if (!service.shouldRun(today, effectivePolicy)) {
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

    private HolidayPolicy getEffectivePolicy(@NonNull Job<?, ?> job) {
        HolidayJobProperty prop = job.getProperty(HolidayJobProperty.class);
        if (prop != null && prop.getHolidayPolicy() != null) {
            return prop.getHolidayPolicy();
        }
        return getHolidayPolicy();
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
