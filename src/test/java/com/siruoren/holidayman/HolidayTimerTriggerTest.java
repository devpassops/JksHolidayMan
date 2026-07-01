package com.siruoren.holidayman;

import antlr.ANTLRException;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class HolidayTimerTriggerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testTriggerCreation() throws ANTLRException {
        HolidayTimerTrigger trigger = new HolidayTimerTrigger("H H * * *", HolidayPolicy.EXCLUDE_HOLIDAYS);
        assertEquals("H H * * *", trigger.getSpec());
        assertEquals(HolidayPolicy.EXCLUDE_HOLIDAYS, trigger.getHolidayPolicy());
    }

    @Test
    public void testTriggerDefaultPolicy() throws ANTLRException {
        HolidayTimerTrigger trigger = new HolidayTimerTrigger("H H * * *", null);
        assertEquals(HolidayPolicy.EXCLUDE_HOLIDAYS, trigger.getHolidayPolicy());
    }

    @Test
    public void testTriggerSetPolicy() throws ANTLRException {
        HolidayTimerTrigger trigger = new HolidayTimerTrigger("H H * * *", HolidayPolicy.EXCLUDE_HOLIDAYS);
        trigger.setHolidayPolicy(HolidayPolicy.ONLY_HOLIDAYS);
        assertEquals(HolidayPolicy.ONLY_HOLIDAYS, trigger.getHolidayPolicy());
    }

    @Test
    public void testTriggerWithIncludeHolidays() throws ANTLRException {
        HolidayTimerTrigger trigger = new HolidayTimerTrigger("H H * * *", HolidayPolicy.INCLUDE_HOLIDAYS);
        assertEquals(HolidayPolicy.INCLUDE_HOLIDAYS, trigger.getHolidayPolicy());
    }

    @Test
    public void testTriggerDescriptor() {
        HolidayTimerTrigger.DescriptorImpl desc = j.jenkins.getDescriptorByType(HolidayTimerTrigger.DescriptorImpl.class);
        assertNotNull(desc);
        assertNotNull(desc.getDisplayName());

        HolidayPolicy[] policies = desc.getHolidayPolicies();
        assertEquals(3, policies.length);

        assertEquals(HolidayPolicy.EXCLUDE_HOLIDAYS, desc.getDefaultPolicy());
    }

    @Test
    public void testTriggerAddedToProject() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-project");
        HolidayTimerTrigger trigger = new HolidayTimerTrigger("H H * * *", HolidayPolicy.EXCLUDE_HOLIDAYS);
        project.addTrigger(trigger);
        project.save();

        // Verify trigger is persisted
        j.jenkins.reload();
        FreeStyleProject reloaded = j.jenkins.getItemByFullName("test-project", FreeStyleProject.class);
        assertNotNull(reloaded);

        HolidayTimerTrigger reloadedTrigger = reloaded.getTrigger(HolidayTimerTrigger.class);
        assertNotNull(reloadedTrigger);
        assertEquals(HolidayPolicy.EXCLUDE_HOLIDAYS, reloadedTrigger.getHolidayPolicy());
    }

    @Test
    public void testTriggerWithOnlyHolidaysPersisted() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("holiday-only-project");
        HolidayTimerTrigger trigger = new HolidayTimerTrigger("H 9 * * *", HolidayPolicy.ONLY_HOLIDAYS);
        project.addTrigger(trigger);
        project.save();

        j.jenkins.reload();
        FreeStyleProject reloaded = j.jenkins.getItemByFullName("holiday-only-project", FreeStyleProject.class);
        HolidayTimerTrigger reloadedTrigger = reloaded.getTrigger(HolidayTimerTrigger.class);
        assertNotNull(reloadedTrigger);
        assertEquals(HolidayPolicy.ONLY_HOLIDAYS, reloadedTrigger.getHolidayPolicy());
        assertEquals("H 9 * * *", reloadedTrigger.getSpec());
    }

    @Test
    public void testHolidayManagementLinkAccessible() throws Exception {
        Jenkins jenkins = j.jenkins;
        HolidayManagementLink link = jenkins.getExtensionList(HolidayManagementLink.class).get(0);
        assertNotNull(link);
        assertEquals("holiday-management", link.getUrlName());
        assertNotNull(link.getDisplayName());
    }
}
