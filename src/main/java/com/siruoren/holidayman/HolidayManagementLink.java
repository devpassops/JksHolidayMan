package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ManagementLink;
import jenkins.model.Jenkins;
import org.apache.commons.fileupload.FileItem;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.POST;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    /**
     * Online import: import from API by year.
     */
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

    /**
     * Offline import: import from uploaded JSON file.
     */
    @POST
    public HttpResponse doImportFile(StaplerRequest req) throws Exception {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        try {
            FileItem fileItem = req.getFileItem("file");
            if (fileItem == null || fileItem.getSize() == 0) {
                throw new IllegalArgumentException("No file uploaded or file is empty");
            }
            
            String json = new String(fileItem.get(), StandardCharsets.UTF_8);
            LOGGER.info("Received file: " + fileItem.getName() + ", size: " + json.length() + " chars");
            LOGGER.info("First 100 chars: " + json.substring(0, Math.min(100, json.length())));
            
            int count = HolidayService.getInstance().importFromJson(json);
            LOGGER.info("Imported " + count + " entries from uploaded file");
            return new HttpRedirect("index");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import holidays from file", e);
            throw e;
        }
    }

    /**
     * Export: download holiday data for a specific year as JSON.
     */
    public void doExportYear(@QueryParameter int year, StaplerResponse rsp) throws IOException {
        Jenkins.get().checkPermission(Jenkins.READ);
        String json = HolidayService.getInstance().exportYearToJson(year);
        writeJsonDownload(rsp, json, "holidays-" + year + ".json");
    }

    /**
     * Export: download all holiday data as JSON.
     */
    public void doExportAll(StaplerResponse rsp) throws IOException {
        Jenkins.get().checkPermission(Jenkins.READ);
        String json = HolidayService.getInstance().exportAllToJson();
        writeJsonDownload(rsp, json, "holidays-all.json");
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
    public void doHolidaysJson(@QueryParameter int year, StaplerResponse rsp) throws Exception {
        Jenkins.get().checkPermission(Jenkins.READ);
        rsp.setContentType("application/json;charset=UTF-8");
        rsp.getWriter().write(getHolidaysJson(year));
    }

    private void writeJsonDownload(StaplerResponse rsp, String json, String filename) throws IOException {
        rsp.setContentType("application/json;charset=UTF-8");
        rsp.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        rsp.getWriter().write(json);
    }
}
