package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.apache.commons.fileupload.FileItem;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerProxy;
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
public class HolidayManagementLink extends ManagementLink implements StaplerProxy {

    private static final Logger LOGGER = Logger.getLogger(HolidayManagementLink.class.getName());
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String getIconFileName() {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            return null;
        }
        return "/plugin/holiday-management/images/calendar.svg";
    }

    @Override
    public Object getTarget() {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        return this;
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

    @Override
    public Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
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
        validateYear(year);
        try {
            int count = HolidayService.getInstance().importFromApi(year);
            LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " imported " + count + 
                " holidays for year " + year);
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
            
            // Validate file size (max 2MB)
            if (fileItem.getSize() > 2 * 1024 * 1024) {
                throw new IllegalArgumentException("File too large. Maximum size is 2MB");
            }
            
            String json = new String(fileItem.get(), StandardCharsets.UTF_8);
            
            // Strip BOM if present
            if (json.length() > 0 && json.charAt(0) == '\uFEFF') {
                json = json.substring(1);
            }
            
            // Strip any non-printable characters at the start
            json = json.trim();
            
            if (json.isEmpty()) {
                throw new IllegalArgumentException("File content is empty after trimming");
            }
            
            LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " uploaded file: " + 
                fileItem.getName() + ", size: " + json.length() + " chars, first 50: " + 
                json.substring(0, Math.min(50, json.length())));
            
            int count = HolidayService.getInstance().importFromJson(json);
            LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " imported " + 
                count + " entries from uploaded file");
            return new HttpRedirect("index");
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid import file: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import holidays from file", e);
            throw e;
        }
    }

    /**
     * Export: download holiday data for a specific year as JSON.
     */
    public void doExportYear(@QueryParameter int year, StaplerResponse rsp) throws IOException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        validateYear(year);
        String json = HolidayService.getInstance().exportYearToJson(year);
        LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " exported holidays for year " + year);
        writeJsonDownload(rsp, json, "holidays-" + year + ".json");
    }

    /**
     * Export: download all holiday data as JSON.
     */
    public void doExportAll(StaplerResponse rsp) throws IOException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER); // Require admin for full export
        String json = HolidayService.getInstance().exportAllToJson();
        LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " exported all holiday data");
        writeJsonDownload(rsp, json, "holidays-all.json");
    }

    @POST
    public HttpResponse doAddHoliday(StaplerRequest req) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        String date = req.getParameter("date");
        String name = req.getParameter("name");
        String type = req.getParameter("type");
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Date is required");
        }
        date = date.trim();
        validateDate(date);
        if (type == null || type.trim().isEmpty()) {
            type = "HOLIDAY";
        }
        type = type.trim();
        validateType(type);
        HolidayDate hd = new HolidayDate(date, name != null ? name.trim() : "", type);
        HolidayService.getInstance().addHoliday(hd);
        LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " added holiday: " + date);
        return new HttpRedirect("index");
    }

    @POST
    public HttpResponse doDeleteHoliday(@QueryParameter String date) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        validateDate(date);
        boolean removed = HolidayService.getInstance().removeHoliday(date);
        if (removed) {
            LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " deleted holiday: " + date);
        }
        return new HttpRedirect(".");
    }

    @POST
    public HttpResponse doDeleteYear(@QueryParameter int year) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        validateYear(year);
        boolean removed = HolidayService.getInstance().removeYear(year);
        if (removed) {
            LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " deleted all holidays for year " + year);
        }
        return new HttpRedirect(".");
    }

    public String getHolidaysJson(int year) {
        validateYear(year);
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
        rsp.setContentType("application/json;charset=UTF-8");
        rsp.getWriter().write(getHolidaysJson(year));
    }

    private void writeJsonDownload(StaplerResponse rsp, String json, String filename) throws IOException {
        rsp.setContentType("application/json;charset=UTF-8");
        rsp.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        rsp.getWriter().write(json);
    }

    private void validateYear(int year) {
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Invalid year: " + year + ", must be between 1900 and 2100");
        }
    }

    private void validateDate(String date) {
        if (date == null || date.isEmpty()) {
            throw new IllegalArgumentException("Date cannot be null or empty");
        }
        try {
            LocalDate.parse(date, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + date + ", expected yyyy-MM-dd");
        }
    }

    private void validateType(String type) {
        if (type != null && !"HOLIDAY".equals(type) && !"WORKDAY".equals(type)) {
            throw new IllegalArgumentException("Invalid type: " + type + ", must be HOLIDAY or WORKDAY");
        }
    }
}
