package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class HolidayJobProperty extends JobProperty<Job<?, ?>> {

    private HolidayPolicy holidayPolicy;
    private boolean enabled;

    @DataBoundConstructor
    public HolidayJobProperty(HolidayPolicy holidayPolicy, boolean enabled) {
        this.holidayPolicy = holidayPolicy != null ? holidayPolicy : HolidayPolicy.EXCLUDE_HOLIDAYS;
        this.enabled = enabled;
    }

    public HolidayPolicy getHolidayPolicy() {
        return holidayPolicy != null ? holidayPolicy : HolidayPolicy.EXCLUDE_HOLIDAYS;
    }

    public void setHolidayPolicy(HolidayPolicy holidayPolicy) {
        this.holidayPolicy = holidayPolicy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Extension
    @Symbol("holidayManagement")
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.HolidayJobProperty_displayName();
        }

        public HolidayPolicy[] getHolidayPolicies() {
            return HolidayPolicy.values();
        }

        public HolidayPolicy getDefaultPolicy() {
            return HolidayPolicy.EXCLUDE_HOLIDAYS;
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return ParameterizedJobMixIn.ParameterizedJob.class.isAssignableFrom(jobType);
        }
    }
}
