package com.siruoren.holidayman;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.apache.commons.fileupload.FileItem;
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

    private List<HolidayDate> getHolidaysForYear(int year) {
        return HolidayService.getInstance().getHolidaysForYear(year);
    }

    public int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    /**
     * Online import: import from API by year.
     * Supports AJAX requests returning JSON, and traditional form submissions returning redirect.
     */
    @POST
    public void doImportYear(StaplerRequest req, StaplerResponse rsp) throws Exception {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        String yearStr = req.getParameter("year");
        if (yearStr == null || yearStr.isEmpty()) {
            yearStr = String.valueOf(LocalDate.now().getYear());
        }
        int year = Integer.parseInt(yearStr);
        validateYear(year);
        boolean isAjax = isAjaxRequest(req);
        try {
            int count = HolidayService.getInstance().importFromApi(year);
            LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " imported " + count + 
                " holidays for year " + year);
            if (isAjax) {
                writeJsonResult(rsp, true, "Successfully imported " + count + " holidays for year " + year, count);
            } else {
                rsp.sendRedirect2("index");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import holidays for year " + year, e);
            if (isAjax) {
                writeJsonResult(rsp, false, "Failed to import: " + e.getMessage(), 0);
            } else {
                throw e;
            }
        }
    }

    /**
     * Offline import: import from uploaded JSON file.
     * Supports AJAX requests returning JSON, and traditional form submissions returning redirect.
     */
    @POST
    public void doImportFile(StaplerRequest req, StaplerResponse rsp) throws Exception {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        boolean isAjax = isAjaxRequest(req);
        try {
            FileItem fileItem = req.getFileItem("file");
            if (fileItem == null || fileItem.getSize() == 0) {
                if (isAjax) {
                    writeJsonResult(rsp, false, "No file uploaded or file is empty", 0);
                    return;
                }
                throw new IllegalArgumentException("No file uploaded or file is empty");
            }
            
            // Validate file size (max 2MB)
            if (fileItem.getSize() > 2 * 1024 * 1024) {
                if (isAjax) {
                    writeJsonResult(rsp, false, "File too large. Maximum size is 2MB", 0);
                    return;
                }
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
                if (isAjax) {
                    writeJsonResult(rsp, false, "File content is empty after trimming", 0);
                    return;
                }
                throw new IllegalArgumentException("File content is empty after trimming");
            }
            
            LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " uploaded file: " + 
                fileItem.getName() + ", size: " + json.length() + " chars, first 50: " + 
                json.substring(0, Math.min(50, json.length())));
            
            int count = HolidayService.getInstance().importFromJson(json);
            LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " imported " + 
                count + " entries from uploaded file");
            if (isAjax) {
                writeJsonResult(rsp, true, "Successfully imported " + count + " holiday entries from file", count);
            } else {
                rsp.sendRedirect2("index");
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid import file: " + e.getMessage());
            if (isAjax) {
                writeJsonResult(rsp, false, e.getMessage(), 0);
            } else {
                throw e;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import holidays from file", e);
            if (isAjax) {
                writeJsonResult(rsp, false, "Failed to import: " + e.getMessage(), 0);
            } else {
                throw e;
            }
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
    public void doAddHoliday(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        boolean isAjax = isAjaxRequest(req);
        try {
            String date = req.getParameter("date");
            String name = req.getParameter("name");
            String type = req.getParameter("type");
            if (date == null || date.trim().isEmpty()) {
                if (isAjax) {
                    writeJsonResult(rsp, false, "Date is required", 0);
                    return;
                }
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
            if (isAjax) {
                writeJsonResult(rsp, true, "Successfully added holiday: " + date, 1);
            } else {
                rsp.sendRedirect2("index");
            }
        } catch (Exception e) {
            if (isAjax) {
                writeJsonResult(rsp, false, "Failed to add: " + e.getMessage(), 0);
            } else {
                throw e;
            }
        }
    }

    @POST
    public void doDeleteHoliday(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        boolean isAjax = isAjaxRequest(req);
        String date = req.getParameter("date");
        try {
            validateDate(date);
            boolean removed = HolidayService.getInstance().removeHoliday(date);
            if (removed) {
                LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " deleted holiday: " + date);
            }
            if (isAjax) {
                writeJsonResult(rsp, true, removed ? "Successfully deleted holiday: " + date : "Holiday not found: " + date, removed ? 1 : 0);
            } else {
                rsp.sendRedirect2(".");
            }
        } catch (Exception e) {
            if (isAjax) {
                writeJsonResult(rsp, false, "Failed to delete: " + e.getMessage(), 0);
            } else {
                throw e;
            }
        }
    }

    @POST
    public void doDeleteYear(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        boolean isAjax = isAjaxRequest(req);
        String yearStr = req.getParameter("year");
        try {
            int year = Integer.parseInt(yearStr);
            validateYear(year);
            boolean removed = HolidayService.getInstance().removeYear(year);
            if (removed) {
                LOGGER.info("User " + Jenkins.get().getAuthentication().getName() + " deleted all holidays for year " + year);
            }
            if (isAjax) {
                writeJsonResult(rsp, true, removed ? "Successfully deleted all holidays for year " + year : "No data found for year " + year, removed ? 1 : 0);
            } else {
                rsp.sendRedirect2(".");
            }
        } catch (Exception e) {
            if (isAjax) {
                writeJsonResult(rsp, false, "Failed to delete: " + e.getMessage(), 0);
            } else {
                throw e;
            }
        }
    }

    private String getHolidaysJson(int year) {
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
    public void doHolidaysJson(StaplerRequest req, StaplerResponse rsp) throws Exception {
        LOGGER.info("doHolidaysJson called with URL: " + req.getRequestURI());
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        String yearStr = req.getParameter("year");
        LOGGER.info("Year parameter: " + yearStr);
        if (yearStr == null || yearStr.isEmpty()) {
            yearStr = String.valueOf(LocalDate.now().getYear());
        }
        int year = Integer.parseInt(yearStr);
        validateYear(year);
        rsp.setContentType("application/json;charset=UTF-8");
        String json = getHolidaysJson(year);
        LOGGER.info("Returning JSON for year " + year + ": " + json.substring(0, Math.min(100, json.length())));
        rsp.getWriter().write(json);
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

    private boolean isAjaxRequest(StaplerRequest req) {
        String header = req.getHeader("X-Requested-With");
        return header != null && "XMLHttpRequest".equals(header);
    }

    private void writeJsonResult(StaplerResponse rsp, boolean success, String message, int count) throws IOException {
        rsp.setContentType("application/json;charset=UTF-8");
        JSONObject result = new JSONObject();
        result.put("success", success);
        result.put("message", message);
        result.put("count", count);
        rsp.getWriter().write(result.toString());
    }
}
