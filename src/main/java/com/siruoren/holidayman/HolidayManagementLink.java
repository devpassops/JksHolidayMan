package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ManagementLink;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.verb.POST;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class HolidayManagementLink extends ManagementLink {

    private static final Logger LOGGER = Logger.getLogger(HolidayManagementLink.class.getName());
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String getIconFileName() {
        return "/plugin/holiday-management/images/calendar.svg";
    }

    @Override
    public String getDisplayName() {
        return Messages.HolidayManagementLink_displayName();
    }

    @Override
    public String getUrlName() {
        return "holiday-management";
    }

    @Override
    public String getDescription() {
        return Messages.HolidayManagementLink_description();
    }

    public List<Integer> getAvailableYears() {
        return HolidayService.getInstance().getAvailableYears();
    }

    public List<HolidayDate> getHolidaysForYear(int year) {
        return HolidayService.getInstance().getHolidaysForYear(year);
    }

    public int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    @POST
    public HttpResponse doImportYear(@QueryParameter int year) throws Exception {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        try {
            int count = HolidayService.getInstance().importFromApi(year);
            return new HttpRedirect("index");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import holidays for year " + year, e);
            throw e;
        }
    }

    @POST
    public HttpResponse doAddHoliday(
            @QueryParameter String date,
            @QueryParameter String name,
            @QueryParameter String type) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        try {
            LocalDate.parse(date, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + date);
        }
        HolidayDate hd = new HolidayDate(date, name != null ? name : "", type != null ? type : "HOLIDAY");
        HolidayService.getInstance().addHoliday(hd);
        return new HttpRedirect("index");
    }

    @POST
    public HttpResponse doDeleteHoliday(@QueryParameter String date) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        HolidayService.getInstance().removeHoliday(date);
        return new HttpRedirect("index");
    }

    @POST
    public HttpResponse doDeleteYear(@QueryParameter int year) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        HolidayService.getInstance().removeYear(year);
        return new HttpRedirect("index");
    }

    public String getHolidaysJson(int year) {
        List<HolidayDate> holidays = getHolidaysForYear(year);
        JSONArray array = new JSONArray();
        for (HolidayDate hd : holidays) {
            JSONObject obj = new JSONObject();
            obj.put("date", hd.getDate());
            obj.put("name", hd.getName());
            obj.put("type", hd.getType());
            obj.put("isHoliday", hd.isHoliday());
            array.add(obj);
        }
        return array.toString();
    }

    /**
     * AJAX endpoint for fetching holiday data as JSON.
     */
    public void doHolidaysJson(@QueryParameter int year, StaplerResponse2 rsp) throws Exception {
        Jenkins.get().checkPermission(Jenkins.READ);
        rsp.setContentType("application/json;charset=UTF-8");
        rsp.getWriter().write(getHolidaysJson(year));
    }
}
