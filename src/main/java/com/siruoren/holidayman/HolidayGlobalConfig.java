package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest2;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class HolidayGlobalConfig extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger(HolidayGlobalConfig.class.getName());

    private HolidayPolicy defaultPolicy;

    public HolidayGlobalConfig() {
        load();
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return Messages.HolidayGlobalConfig_displayName();
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
        defaultPolicy = HolidayPolicy.valueOf(json.optString("defaultPolicy", "EXCLUDE_HOLIDAYS"));
        save();
        return true;
    }

    public HolidayPolicy getDefaultPolicy() {
        return defaultPolicy != null ? defaultPolicy : HolidayPolicy.EXCLUDE_HOLIDAYS;
    }

    public void setDefaultPolicy(HolidayPolicy defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public HolidayPolicy[] getPolicies() {
        return HolidayPolicy.values();
    }

    public static HolidayGlobalConfig get() {
        return GlobalConfiguration.all().getInstance(HolidayGlobalConfig.class);
    }
}
