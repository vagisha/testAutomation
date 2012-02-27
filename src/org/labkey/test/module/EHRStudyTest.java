/*
 * Copyright (c) 2011-2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.test.module;

import org.apache.james.mime4j.field.datetime.DateTime;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.tests.SimpleApiTest;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.PasswordUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Mar 21, 2011
 * Time: 1:59:12 PM
 */
public class EHRStudyTest extends SimpleApiTest
{
    // Project/folder names are hard-coded into some links in the module.
    private static final String PROJECT_NAME = "WNPRC";
    private static final String FOLDER_NAME = "EHR";
    private static final String CONTAINER_PATH = PROJECT_NAME + "/" + FOLDER_NAME;
    private static final String STUDY_ZIP = "/sampledata/study/EHR Study Anon.zip";
    private static final String SCRIPT_TEMPLATE = "/server/test/data/api/ehr-security-template.xml";

    //note: changed by BNB
    private static final String PROJECT_ID = "640991"; // project with one participant
    private static final String DUMMY_PROTOCOL = "g00000"; // need a protocol to create table entry
    private static final String PROJECT_MEMBER_ID = "test2312318"; // PROJECT_ID's single participant
    private static final String ROOM_ID = "6824778"; // room of PROJECT_MEMBER_ID
    private static final String CAGE_ID = "4434662"; // cage of PROJECT_MEMBER_ID

    private static final String AREA_ID = "A1/AB190"; // arbitrary area
    private static final String PROTOCOL_PROJECT_ID = "795644"; // Project with exactly 3 members
    private static final String PROTOCOL_ID = "g00101";
    private static final String[] PROTOCOL_MEMBER_IDS = {"test3997535", "test4551032", "test5904521"}; //{"test2008446", "test3804589", "test4551032", "test5904521", "test6390238"}; // Protocol members, sorted ASC alphabetically
    private static final String[] MORE_ANIMAL_IDS = {"test1020148","test1099252","test1112911","test727088","test4564246"}; // Some more, distinct, Ids
    private static final String DEAD_ANIMAL_ID = "test9118022";
    private static final EHRUser DATA_ADMIN = new EHRUser("admin@ehrstudy.test", "EHR Administrators", EHRRole.DATA_ADMIN);
    private static final EHRUser REQUESTER = new EHRUser("requester@ehrstudy.test", "EHR Requestors", EHRRole.REQUESTER);
    private static final EHRUser BASIC_SUBMITTER = new EHRUser("basicsubmitter@ehrstudy.test", "EHR Basic Submitters", EHRRole.BASIC_SUBMITTER);
    private static final EHRUser FULL_SUBMITTER = new EHRUser("fullsubmitter@ehrstudy.test", "EHR Full Submitters", EHRRole.FULL_SUBMITTER);
    private static final String TASK_TITLE = "Test weight task";
    private static final String MPR_TASK_TITLE = "Test MPR task";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    //xpath fragment
    public static final String VISIBLE = "not(ancestor-or-self::*[contains(@style,'visibility: hidden') or contains(@class, 'x-hide-display')])";

    private static class EHRUser
    {
        private final String _userId;
        private final String _groupName;
        private final EHRRole _role;
        
        public EHRUser(String userId, String groupName, EHRRole role)
        {
            _userId = userId;
            _groupName = groupName;
            _role = role;
        }
        
        public String getUser()
        {
            return _userId;
        }
        
        public String getGroup()
        {
            return _groupName;
        }
        
        public EHRRole getRole()
        {
            return _role;
        }
    }

    @Override
    protected boolean isDatabaseSupported(DatabaseInfo info)
    {
        return info.productName.equals("PostgreSQL");
    }
       
    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/ehr";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public boolean enableLinkCheck()
    {
        if ( super.enableLinkCheck() )
            log("EHR test has too many hard coded links and special actions to crawl effectively. Skipping crawl.");
        return false;
    }

    @Override
    public void validateQueries()
    {
        log("Skipping query validation.");
    }

    @Override
    protected Pattern[] getIgnoredElements()
    {
        return new Pattern[] {
            Pattern.compile("qcstate", Pattern.CASE_INSENSITIVE),//qcstate IDs aren't predictable
            Pattern.compile("stacktrace", Pattern.CASE_INSENSITIVE)
        };
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[0];
    }

    @Override
    public void doCleanup()
    {
        long startTime = System.currentTimeMillis();
        try{deleteRecords();}catch(Throwable T){}
        try {deleteProject(PROJECT_NAME);} catch (Throwable t) { /*ignore*/ }
        if(isTextPresent(PROJECT_NAME))
        {
            log("Wait extra long for folder to finish deleting.");
            while (isTextPresent(PROJECT_NAME) && System.currentTimeMillis() - startTime < 300000) // 5 minutes max.
            {
                sleep(5000);
                refresh();
            }
            if (!isTextPresent(PROJECT_NAME)) log("Test Project deleted in " + (System.currentTimeMillis() - startTime) + "ms");
            else fail("Test Project not finished deleting after 5 minutes");
        }
        goToHome();
        try{deleteUser(DATA_ADMIN.getUser());}catch(Throwable T){}
        try{deleteUser(REQUESTER.getUser());}catch(Throwable T){}
        try{deleteUser(BASIC_SUBMITTER.getUser());}catch(Throwable T){}
        try{deleteUser(FULL_SUBMITTER.getUser());}catch(Throwable T){}
    }

    @Override
    public void runUITests()
    {
        initProject();
        setupEhrPermissions();
        defineQCStates();

        animalHistoryTest();
        quickSearchTest();
        weightDataEntryTest();
        //mprDataEntryTest(); // TODO: Blocked: Can't fill out form, SNOMED list is empty
        /* super.runApiTests() */
    }

    private void initProject()
    {
        enableEmailRecorder();

        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "Collaboration", new String[]{"EHR", "Pipeline", "Study"});
        enableModule(PROJECT_NAME, "EHR");
        clickLinkWithText(FOLDER_NAME);
        beginAt(getBaseURL()+"/ehr/"+PROJECT_NAME+"/"+FOLDER_NAME+"/_initEHR.view");
        clickNavButton("Delete All", 0);
        waitForText("Delete Complete", 120000);
        clickNavButton("Populate All", 0);
        waitForText("Populate Complete", 120000);
        goToModule("Study");
        importStudyFromZip(new File(getLabKeyRoot() + STUDY_ZIP).getPath());
        try
        {
            deleteRecords();
            populateRecords();
        }
        catch (Throwable e)
        {
            //ignore for now
            log("There was an error");
        }

        log("Remove all webparts");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
//        clickWebpartMenuItem("Pages", false, "Layout", "Remove From Page");
        removeWebPart("Wiki");
        removeWebPart("Messages");
        addWebPart("EHR Datasets");

        // TODO: Menu Bar causing permission dialog to appear when impersonating/stopping impersonation
        // TODO: Should use menu bar to access Animal History, Quick Search, and Data Entry pages.
        //log("Setup EHR Menu Bar.");
        //clickAdminMenuItem("Manage Project", "Project Settings");
        //clickLinkWithText("Menu Bar");
        //clickLinkWithText("Turn On Custom Menus");
        addWebPart("Electronic Health Record");
        addWebPart("Quick Search");
    }

    private void populateRecords() throws Exception
    {
        log("Inserting initial records into EHR hard tables");

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        //first ehr.protocol
        InsertRowsCommand insertCmd = new InsertRowsCommand("ehr", "protocol");
        Map<String,Object> rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", PROTOCOL_ID);
        insertCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", DUMMY_PROTOCOL);
        insertCmd.addRow(rowMap);
        SaveRowsResponse saveResp = insertCmd.execute(cn, CONTAINER_PATH);

        //then ehr.project
        insertCmd = new InsertRowsCommand("ehr", "project");
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROTOCOL_PROJECT_ID);
        rowMap.put("protocol", PROTOCOL_ID);
        insertCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROJECT_ID);
        rowMap.put("protocol", DUMMY_PROTOCOL);
        insertCmd.addRow(rowMap);
        saveResp = insertCmd.execute(cn, CONTAINER_PATH);
    }

    private void deleteRecords() throws Exception
    {
        log("Deleting initial records from EHR hard tables");

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        //first ehr.protocol
        DeleteRowsCommand deleteCmd = new DeleteRowsCommand("ehr", "protocol");
        Map<String,Object> rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", PROTOCOL_ID);
        deleteCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", DUMMY_PROTOCOL);
        deleteCmd.addRow(rowMap);
        SaveRowsResponse deleteResp = deleteCmd.execute(cn, CONTAINER_PATH);

        //then ehr.project
        deleteCmd = new DeleteRowsCommand("ehr", "project");
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROTOCOL_PROJECT_ID);
        rowMap.put("protocol", PROTOCOL_ID);
        deleteCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROJECT_ID);
        rowMap.put("protocol", DUMMY_PROTOCOL);
        deleteCmd.addRow(rowMap);
        deleteResp = deleteCmd.execute(cn, CONTAINER_PATH);
    }

    private void animalHistoryTest()
    {
        String dataRegionName;
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);

        waitAndClick(Locator.linkWithText("Animal History"));
        waitForPageToLoad();

        log("Verify Single animal history");
        setFormElement("subjectBox", PROTOCOL_MEMBER_IDS[0]);
        sleep(200);//weird timing issue.  Nothing to wait for, so we just pause for a moment.
        clickNavButton("Refresh", 0);
        waitForElement(Locator.linkWithText(PROTOCOL_MEMBER_IDS[0]), WAIT_FOR_JAVASCRIPT);

        //crawlReportTabs(); // TOO SLOW. TODO: Enable when performance is better.

        log("Verify Entire colony history");
        checkRadioButton("selector", "renderColony");
        clickNavButton("Refresh", 0);
        dataRegionName = getDataRegionName("Abstract");
        assertEquals("Did not find the expected number of Animals", 44, getDataRegionRowCount(dataRegionName));

        log("Verify location based history");
        checkRadioButton("selector", "renderRoomCage");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='areaField']/.."), AREA_ID);
        setFormElement("roomField", ROOM_ID);
        setFormElement("cageField", CAGE_ID);
        clickNavButton("Refresh", 0);
        // No results expected due to anonymized cage info.
        waitForText("No records found", WAIT_FOR_JAVASCRIPT);

        log("Verify Project search");
        checkRadioButton("selector", "renderMultiSubject");
        waitAndClick(Locator.xpath("//table[text()='[Search By Project/Protocol]']"));
        ExtHelper.waitForExtDialog(this, "Search By Project/Protocol");
        ExtHelper.selectComboBoxItem(this, "Project", PROJECT_ID);
        clickNavButton("Submit", 0);
        waitForElement(Locator.button(PROJECT_MEMBER_ID + " (X)"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Refresh", 0);
        waitForElement(Locator.linkWithText(PROJECT_MEMBER_ID), WAIT_FOR_JAVASCRIPT);

        log("Verify Protocol search");
        checkRadioButton("selector", "renderMultiSubject");
        waitAndClick(Locator.xpath("//table[text()='[Search By Project/Protocol]']"));
        ExtHelper.waitForExtDialog(this, "Search By Project/Protocol");
        ExtHelper.selectComboBoxItem(this, "Protocol", PROTOCOL_ID);
        clickNavButton("Submit", 0);
        waitForElement(Locator.button(PROTOCOL_MEMBER_IDS[0] + " (X)"), WAIT_FOR_JAVASCRIPT);

        // Check protocol search results.
        clickNavButton("Refresh", 0);
        dataRegionName = getDataRegionName("Abstract");
        assertEquals("Did not find the expected number of Animals", PROTOCOL_MEMBER_IDS.length, getDataRegionRowCount(dataRegionName));
        assertLinkPresentWithText(PROTOCOL_MEMBER_IDS[0]);

        // Check animal count after removing one from search.
        waitAndClick(Locator.button(PROTOCOL_MEMBER_IDS[0] + " (X)"));
        waitForElementToDisappear(Locator.button(PROTOCOL_MEMBER_IDS[0] + " (X)"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Refresh", 0);
        dataRegionName = getDataRegionName("Abstract");
        assertEquals("Did not find the expected number of Animals", PROTOCOL_MEMBER_IDS.length - 1, getDataRegionRowCount(dataRegionName));
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[0]);

        // Re-add animal.
        setFormElement("subjectBox",  PROTOCOL_MEMBER_IDS[0]);
        clickNavButton("  Append -->", 0);
        waitForElement(Locator.button(PROTOCOL_MEMBER_IDS[0] + " (X)"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Refresh", 0);
        dataRegionName = getDataRegionName("Abstract");
        waitForText(PROTOCOL_MEMBER_IDS[0]);
        assertEquals("Did not find the expected number of Animals", PROTOCOL_MEMBER_IDS.length, getDataRegionRowCount(dataRegionName));

        log("Verify custom actions");
        log("Return Distinct Values - no selections");
        clickMenuButtonAndContinue("More Actions", "Return Distinct Values");
        assertAlert("No records selected");

        log("Return Distinct Values");
        dataRegionName = getDataRegionName("Weight");
        checkAllOnPage(dataRegionName);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Return Distinct Values");
        ExtHelper.waitForExtDialog(this, "Return Distinct Values");
        ExtHelper.selectComboBoxItem(this, "Select Field", "Animal Id");
        clickNavButton("Submit", 0);
        ExtHelper.waitForExtDialog(this, "Distinct Values");
        assertFormElementEquals("distinctValues", PROTOCOL_MEMBER_IDS[0]+"\n"+PROTOCOL_MEMBER_IDS[1]+"\n"+PROTOCOL_MEMBER_IDS[2]);
        clickNavButton("Close", 0);

        log("Return Distinct Values - filtered");
        setFilterAndWait(dataRegionName, "Id", "Does Not Equal", PROTOCOL_MEMBER_IDS[1], 0);
        waitForText("Filter: (Id <> " + PROTOCOL_MEMBER_IDS[1], WAIT_FOR_JAVASCRIPT);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Return Distinct Values");
        ExtHelper.waitForExtDialog(this, "Return Distinct Values");
        ExtHelper.selectComboBoxItem(this, "Select Field", "Animal Id");
        clickNavButton("Submit", 0);
        ExtHelper.waitForExtDialog(this, "Distinct Values");
        assertFormElementEquals("distinctValues", PROTOCOL_MEMBER_IDS[0]+"\n"+PROTOCOL_MEMBER_IDS[2]);
        clickNavButton("Close", 0);

        log("Compare Weights - no selection");
        uncheckAllOnPage(dataRegionName);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        assertAlert("No records selected");

        log("Compare Weights - one selection");
        checkDataRegionCheckbox(dataRegionName, 0);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        ExtHelper.waitForExtDialog(this, "Weights");
        clickNavButton("OK", 0);

        log("Compare Weights - two selections");
        checkDataRegionCheckbox(dataRegionName, 1);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        ExtHelper.waitForExtDialog(this, "Weights");
        clickNavButton("OK", 0);

        log("Compare Weights - three selections");
        checkDataRegionCheckbox(dataRegionName, 2);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        ExtHelper.waitForExtDialog(this, "Error"); // After error dialog.
        clickNavButton("OK", 0);

        log("Jump to Other Dataset - no selection");
        uncheckAllOnPage(dataRegionName);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Jump To Other Dataset");
        assertAlert("No records selected");

        log("Jump to Other Dataset - two selection");
        dataRegionName = getDataRegionName("Abstract");
        checkDataRegionCheckbox(dataRegionName, 0); // PROTOCOL_MEMBER_IDS[0]
        checkDataRegionCheckbox(dataRegionName, 2); // PROTOCOL_MEMBER_IDS[2]
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Jump To Other Dataset");
        ExtHelper.selectComboBoxItem(this, "Dataset", "Blood Draws");
        ExtHelper.selectComboBoxItem(this, "Filter On", "Animal Id");
        clickNavButton("Submit");
        waitForElement(Locator.linkWithText(PROTOCOL_MEMBER_IDS[0]), WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[1]);

        log("Jump to History");
        checkDataRegionCheckbox("query", 0); // PROTOCOL_MEMBER_IDS[0]
        clickMenuButton("More Actions", "Jump To History");
        assertTitleContains("Animal History");
        clickNavButton("  Append -->", 0);
        setFormElement("subjectBox", PROTOCOL_MEMBER_IDS[2]);
        clickNavButton("  Append -->", 0);
        clickNavButton("Refresh", 0);
        dataRegionName = getDataRegionName("Abstract");
        assertEquals("Did not find the expected number of Animals", 2, getDataRegionRowCount(dataRegionName));
        assertTextPresent(PROTOCOL_MEMBER_IDS[0], PROTOCOL_MEMBER_IDS[2]);

        log("Check subjectBox parsing");
        setFormElement("subjectBox",  MORE_ANIMAL_IDS[0]+","+MORE_ANIMAL_IDS[1]+";"+MORE_ANIMAL_IDS[2]+" "+MORE_ANIMAL_IDS[3]+"\n"+MORE_ANIMAL_IDS[4]);
        clickNavButton("  Replace -->", 0);
        clickNavButton("Refresh", 0);
        dataRegionName = getDataRegionName("Abstract");
        assertEquals("Did not find the expected number of Animals", 5, getDataRegionRowCount(dataRegionName));
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[1]);
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[2]);
                                      
        clickNavButton(" Clear ", 0);
        clickNavButton("Refresh", 0);
        assertAlert("Must Enter At Least 1 Animal ID");
        assertElementNotPresent(Locator.buttonContainingText("(X)"));
    }

    private void quickSearchTest()
    {
        log("Quick Search - Show Animal");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        setFormElement("animal", MORE_ANIMAL_IDS[0]);
        clickNavButton("Show Animal");
        assertTitleContains("Animal - "+MORE_ANIMAL_IDS[0]);

        log("Quick Search - Show Group");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='animalGroup']/.."), "Alive, at WNPRC");
        clickNavButton("Show Group");
        waitForText("1 - 36 of 36", WAIT_FOR_JAVASCRIPT);

        log("Quick Search - Show Project");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='projectField']/.."), PROJECT_ID);
        clickNavButton("Show Project");
        waitForElement(Locator.linkWithText(PROJECT_ID), WAIT_FOR_JAVASCRIPT);

        log("Quick Search - Show Protocol");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='protocolField']/.."), PROTOCOL_ID);
        clickNavButton("Show Protocol");
        waitForElement(Locator.linkWithText(PROTOCOL_ID), WAIT_FOR_JAVASCRIPT);

        log("Quick Search - Show Room");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        setFormElement("room", ROOM_ID);
        clickNavButton("Show Room");
        waitForElement(Locator.linkWithText(PROJECT_MEMBER_ID), WAIT_FOR_JAVASCRIPT);
    }

    private void setupEhrPermissions()
    {
        clickLinkWithText(PROJECT_NAME);
        createUserAndNotify(DATA_ADMIN.getUser(), "");
        clickLinkWithText(PROJECT_NAME);
        createUserAndNotify(REQUESTER.getUser(), "");
        clickLinkWithText(PROJECT_NAME);
        createUserAndNotify(BASIC_SUBMITTER.getUser(), "");
        clickLinkWithText(PROJECT_NAME);
        createUserAndNotify(FULL_SUBMITTER.getUser(), "");
        clickLinkWithText(PROJECT_NAME);
        setInitialPassword(DATA_ADMIN.getUser(), PasswordUtil.getPassword());
        setInitialPassword(REQUESTER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(BASIC_SUBMITTER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(FULL_SUBMITTER.getUser(), PasswordUtil.getPassword());

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        pushLocation();
        createPermissionsGroup(DATA_ADMIN.getGroup(), DATA_ADMIN.getUser());
        createPermissionsGroup(REQUESTER.getGroup(), REQUESTER.getUser());
        createPermissionsGroup(BASIC_SUBMITTER.getGroup(), BASIC_SUBMITTER.getUser());
        createPermissionsGroup(FULL_SUBMITTER.getGroup(), FULL_SUBMITTER.getUser());
        popLocation();
        enterPermissionsUI();
        uncheckInheritedPermissions();
        setPermissions(DATA_ADMIN.getGroup(), "Editor");
        setPermissions(REQUESTER.getGroup(), "Editor");
        setPermissions(BASIC_SUBMITTER.getGroup(), "Editor");
        setPermissions(FULL_SUBMITTER.getGroup(), "Editor");
        savePermissions();
        ExtHelper.clickExtTab(this, "Study Security");
        waitAndClickNavButton("Study Security");

        checkRadioButton(getRadioButtonLocator(DATA_ADMIN.getGroup(), "READOWN"));
        checkRadioButton(getRadioButtonLocator(REQUESTER.getGroup(), "READOWN"));
        checkRadioButton(getRadioButtonLocator(BASIC_SUBMITTER.getGroup(), "READOWN"));
        checkRadioButton(getRadioButtonLocator(FULL_SUBMITTER.getGroup(), "READOWN"));
        clickAndWait(Locator.id("groupUpdateButton"));

        //"set all to..." combo-boxes don't work through selenium.
        log("Set per-dataset permissions individually");
        setPDP(DATA_ADMIN);
        setPDP(BASIC_SUBMITTER);
        setPDP(FULL_SUBMITTER);
        setPDP(REQUESTER);

        waitFor(new Checker(){
            public boolean check(){
                return "EHR Data Admin".equals(getSelectedOptionText(Locator.name("dataset.1061", 0))) &&
                       "EHR Basic Submitter".equals(getSelectedOptionText(Locator.name("dataset.1061", 1))) &&
                       "EHR Full Submitter".equals(getSelectedOptionText(Locator.name("dataset.1061", 2))) &&
                       "EHR Requestor".equals(getSelectedOptionText(Locator.name("dataset.1061", 3)));
            }
        }, "Per-dataset permission not set", WAIT_FOR_JAVASCRIPT);

        clickNavButton("Save");
    }

    private void setPDP(EHRUser user)
    {
        int col = selenium.getXpathCount("//table[@id='datasetSecurityFormTable']//th[.='"+user.getGroup()+"']/preceding-sibling::*").intValue() + 1;
        int rowCt = getTableRowCount("datasetSecurityFormTable");
        for (int i = 3; i <= rowCt; i++) // xpath indexing is 1 based
        {
            selectOptionByText(Locator.xpath("//table[@id='datasetSecurityFormTable']/tbody/tr["+i+"]/td["+col+"]//select"), user.getRole().toString());
        }
    }

    private void weightDataEntryTest()
    {
        log("Test weight data entry");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        saveLocation();
        impersonate(FULL_SUBMITTER.getUser());
        recallLocation();
        waitAndClick(Locator.linkWithText("Enter Data"));
        waitForPageToLoad();

        log("Create weight measurement task.");
        waitAndClick(Locator.linkWithText("Enter Weights"));
        waitForPageToLoad();
        waitForElement(Locator.name("title"), WAIT_FOR_JAVASCRIPT);
        setFormElement("title", TASK_TITLE);
        ExtHelper.selectComboBoxItem(this, "Assigned To", BASIC_SUBMITTER.getGroup() + "\u00A0"); // appended with a nbsp (Alt+0160)

        log("Add blank weight entries");
        clickButton("Add Record", 0);
        waitForElement(Locator.xpath("//input[@name='Id' and not(contains(@class, 'disabled'))]"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.setExtFormElementByLabel(this, "Id:", "noSuchAnimal");
        waitForText("Id not found", WAIT_FOR_JAVASCRIPT);
        ExtHelper.setExtFormElementByLabel(this, "Id:", DEAD_ANIMAL_ID);
        waitForText(DEAD_ANIMAL_ID, WAIT_FOR_JAVASCRIPT);

        waitForElement(Locator.button("Add Batch"), WAIT_FOR_JAVASCRIPT);
        clickButton("Add Batch", 0);
        ExtHelper.waitForExtDialog(this, "");
        ExtHelper.setExtFormElementByLabel(this, "", "Room(s):", ROOM_ID);
        ExtHelper.clickExtButton(this, "", "Submit", 0);
        waitForText(PROJECT_MEMBER_ID, WAIT_FOR_JAVASCRIPT);
        clickButton("Add Batch", 0);
        ExtHelper.waitForExtDialog(this, "");
        ExtHelper.setExtFormElementByLabel(this, "", "Id(s):", MORE_ANIMAL_IDS[0]+","+MORE_ANIMAL_IDS[1]+";"+MORE_ANIMAL_IDS[2]+" "+MORE_ANIMAL_IDS[3]+"\n"+MORE_ANIMAL_IDS[4]);
        ExtHelper.clickExtButton(this, "", "Submit", 0);
        waitForText(MORE_ANIMAL_IDS[0], WAIT_FOR_JAVASCRIPT);
        waitForText(MORE_ANIMAL_IDS[1], WAIT_FOR_JAVASCRIPT);
        waitForText(MORE_ANIMAL_IDS[2], WAIT_FOR_JAVASCRIPT);
        waitForText(MORE_ANIMAL_IDS[3], WAIT_FOR_JAVASCRIPT);
        waitForText(MORE_ANIMAL_IDS[4], WAIT_FOR_JAVASCRIPT);

        selectRecord("weight", MORE_ANIMAL_IDS[0], true);
        selectRecord("weight", MORE_ANIMAL_IDS[1], true);
        selectRecord("weight", MORE_ANIMAL_IDS[2], true);
        clickNavButton("Delete Selected", 0);
        ExtHelper.waitForExtDialog(this, "Confirm");
        ExtHelper.clickExtButton(this, "Yes", 0);
        waitForElementToDisappear(Locator.tagWithText("div", PROTOCOL_MEMBER_IDS[0]), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.tagWithText("div", MORE_ANIMAL_IDS[0]), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.tagWithText("div", MORE_ANIMAL_IDS[1]), WAIT_FOR_JAVASCRIPT);

        //TODO: Test duplicate record
        selectRecord("weight", MORE_ANIMAL_IDS[4], true);
        clickButton("Duplicate Selected", 0);
        ExtHelper.waitForExtDialog(this, "Duplicate Records");
        ExtHelper.clickExtButton(this, "Duplicate Records", "Submit", 0);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);

        clickNavButton("Save & Close");

        waitForText("No data to show.", WAIT_FOR_JAVASCRIPT);
        ExtHelper.clickExtTab(this, "All Tasks");
        waitForElement(Locator.xpath("//div[contains(@class, 'all-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Incorrect number of task rows.", 1, selenium.getXpathCount("//div[contains(@class, 'all-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        ExtHelper.clickExtTab(this, "Tasks By Room");
        waitForElement(Locator.xpath("//div[contains(@class, 'room-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Incorrect number of task rows.", 3, selenium.getXpathCount("//div[contains(@class, 'room-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        ExtHelper.clickExtTab(this, "Tasks By Id");
        waitForElement(Locator.xpath("//div[contains(@class, 'id-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Incorrect number of task rows.", 3, selenium.getXpathCount("//div[contains(@class, 'id-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));

        stopImpersonating();

        log("Fulfil measurement task");
        impersonate(BASIC_SUBMITTER.getUser());
        recallLocation();
        waitAndClick(Locator.linkWithText("Enter Data"));
        waitForPageToLoad();
        waitForElement(Locator.xpath("//div[contains(@class, 'my-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);

        String href = getAttribute(Locator.linkWithText(TASK_TITLE), "href");
        beginAt(href); // Clicking link opens in another window.
        waitForElement(Locator.xpath("/*//*[contains(@class,'ehr-weight-records-grid')]"), WAIT_FOR_JAVASCRIPT);
        waitForTextToDisappear("Loading...", WAIT_FOR_JAVASCRIPT);
        selectRecord("weight", MORE_ANIMAL_IDS[4], false);
        waitForElement(Locator.linkWithText(MORE_ANIMAL_IDS[4]), WAIT_FOR_JAVASCRIPT);
        clickButton("Delete Selected", 0); // Delete duplicate record. It has served its purpose.
        ExtHelper.waitForExtDialog(this, "Confirm");
        ExtHelper.clickExtButton(this, "Yes", 0);
        waitForText("No Animal Selected", WAIT_FOR_JAVASCRIPT);
        selectRecord("weight", PROJECT_MEMBER_ID, false);
        ExtHelper.setExtFormElementByLabel(this, "Weight (kg):", "3.333");
        selectRecord("weight", MORE_ANIMAL_IDS[3], false);
        ExtHelper.setExtFormElementByLabel(this, "Weight (kg):", "4.444");
        selectRecord("weight", MORE_ANIMAL_IDS[4], false);
        ExtHelper.setExtFormElementByLabel(this, "Weight (kg):", "5.555");

        clickButton("Submit for Review", 0);
        ExtHelper.waitForExtDialog(this, "Submit For Review");
        ExtHelper.selectComboBoxItem(this, "Assign To", DATA_ADMIN.getGroup());
        ExtHelper.clickExtButton(this, "Submit For Review", "Submit");

        sleep(1000); // Weird
        stopImpersonating();

        log("Verify Measurements");
        sleep(1000); // Weird 
        impersonate(DATA_ADMIN.getUser());
        recallLocation();
        waitAndClick(Locator.linkWithText("Enter Data"));
        waitForPageToLoad();
        waitForElement(Locator.xpath("//div[contains(@class, 'my-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.clickExtTab(this, "Review Required");
        waitForElement(Locator.xpath("//div[contains(@class, 'review-requested-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Incorrect number of task rows.", 1, selenium.getXpathCount("//div[contains(@class, 'review-requested-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        String href2 = getAttribute(Locator.linkWithText(TASK_TITLE), "href");
        beginAt(href2); // Clicking opens in a new window.
        waitForElement(Locator.xpath("/*//*[contains(@class,'ehr-weight-records-grid')]"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Validate", 0);
        waitForElement(Locator.xpath("//button[text() = 'Submit Final' and "+Locator.ENABLED+"]"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Submit Final", 0);
        ExtHelper.waitForExtDialog(this, "Finalize Form");
        ExtHelper.clickExtButton(this, "Finalize Form", "Yes");

        sleep(1000); // Weird
        stopImpersonating();
        sleep(1000); // Weird
        
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitAndClick(Locator.linkWithText("Browse All Datasets"));
        waitForPageToLoad();
        waitAndClick(Locator.xpath("//a[contains(@href, 'queryName=Weight') and text() = 'View All Records']"));
        waitForPageToLoad();

        setFilter("query", "date", "Equals", DATE_FORMAT.format(new Date()));
        assertTextPresent("3.333", "4.444", "5.555");
        assertTextPresent("Completed", 3);
    }

    private void mprDataEntryTest()
    {
        log("Test MPR data entry.");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        saveLocation();
        impersonate(FULL_SUBMITTER.getUser());
        recallLocation();
        waitAndClick(Locator.linkWithText("Enter Data"));
        waitForPageToLoad();

        log("Create weight measurement task.");
        waitAndClick(Locator.linkWithText("Enter MPR"));
        waitForPageToLoad();
        // Wait for page to fully render.
        waitForText("Treatments", WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.name("Id"), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.name("title"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.setExtFormElementByLabel(this, "Id:", PROJECT_MEMBER_ID);
        waitForElement(Locator.linkWithText(PROJECT_MEMBER_ID), WAIT_FOR_JAVASCRIPT);
        setFormElement("title", MPR_TASK_TITLE);
        ExtHelper.selectComboBoxItem(this, "Assigned To", BASIC_SUBMITTER.getGroup() + "\u00A0"); // appended with a nbsp (Alt+0160)

        sleep(1000);

        clickNavButton("Save & Close");

        waitForText("No data to show.", WAIT_FOR_JAVASCRIPT);
        ExtHelper.clickExtTab(this, "All Tasks");
        waitForElement(Locator.xpath("//div[contains(@class, 'all-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Incorrect number of task rows.", 1, selenium.getXpathCount("//div[contains(@class, 'all-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        ExtHelper.clickExtTab(this, "Tasks By Room");
        waitForElement(Locator.xpath("//div[contains(@class, 'room-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Incorrect number of task rows.", 1, selenium.getXpathCount("//div[contains(@class, 'room-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        ExtHelper.clickExtTab(this, "Tasks By Id");
        waitForElement(Locator.xpath("//div[contains(@class, 'id-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Incorrect number of task rows.", 1, selenium.getXpathCount("//div[contains(@class, 'id-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        stopImpersonating();

        log("Fulfil MPR task");
        impersonate(BASIC_SUBMITTER.getUser());
        recallLocation();
        waitAndClick(Locator.linkWithText("Enter Data"));
        waitForPageToLoad();
        waitForElement(Locator.xpath("//div[contains(@class, 'my-tasks-marker') and "+VISIBLE+"]//table"), WAIT_FOR_JAVASCRIPT);
        String href = getAttribute(Locator.linkWithText(MPR_TASK_TITLE), "href");
        beginAt(href);

        // Wait for page to fully render.
        waitForText("Treatments", WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.name("Id"), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.name("title"), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.xpath("/*//*[contains(@class,'ehr-drug_administration-records-grid')]"), WAIT_FOR_JAVASCRIPT);
        ExtHelper.selectComboBoxItem(this, "Project", PROJECT_ID + " (" + PROTOCOL_ID + ")\u00A0");
        ExtHelper.selectComboBoxItem(this, "Type", "Physical Exam\u00A0");
        setFormElement("remark", "Bonjour");
        setFormElement("performedby", BASIC_SUBMITTER.getUser());

        log("Add treatments record.");
        waitForElement(Locator.xpath("/*//*[contains(@class,'ehr-drug_administration-records-grid')]"), WAIT_FOR_JAVASCRIPT);
        clickVisibleButton("Add Record");
        setFormElement(Locator.xpath("//div[./div/span[text()='Treatments & Procedures']]//input[@name='enddate']/..//input[contains(@id, 'date')]"), DATE_FORMAT.format(new Date()));
        ExtHelper.selectComboBoxItem(this, "Code", "Antibiotic");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='code']/.."), "amoxicillin (c-54620)\u00a0");
        ExtHelper.selectComboBoxItem(this, "Route", "oral\u00a0");
        setFormElement("concentration", "5");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@name='conc_units']/.."), "mg/tablet\u00a0");
        //TODO: assert units
        setFormElement("dosage", "2");
        click(Locator.xpath("//img["+VISIBLE+" and contains(@class, 'x-form-search-trigger')]"));
        waitForElement(Locator.xpath("//div[@class='x-form-invalid-msg']"), WAIT_FOR_JAVASCRIPT);
        setMPRField("Treatments", "remark", "Yum");
        setMPRField("Treatments", "performedby", BASIC_SUBMITTER.getUser());

        //TODO: Test more procedures.
//        log("Add blood draw record.");
//        ExtHelper.clickExtTab(this, "Blood Draws");
//        waitForElement(Locator.xpath("//*["+VISIBLE+" and contains(@class,'ehr-blood_draws-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");
//
//        log("Add recovery observation");
//        ExtHelper.clickExtTab(this, "Recovery Observations");
//        waitForElement(Locator.xpath("//*["+VISIBLE+" and contains(@class,'ehr-clinical_observations-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");
//
//        log("Add procedure code");
//        ExtHelper.clickExtTab(this, "Procedure Codes");
//        waitForElement(Locator.xpath("//*["+VISIBLE+" and contains(@class,'ehr-procedure_codes-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");
//
//        log("Add housing record.");
//        ExtHelper.clickExtTab(this, "Housing Moves/Restraint");
//        waitForElement(Locator.xpath("//*["+VISIBLE+" and contains(@class,'ehr-housing-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");
//
//        log("Add weight record.");
//        ExtHelper.clickExtTab(this, "Weight");
//        waitForElement(Locator.xpath("//*["+VISIBLE+" and contains(@class,'ehr-weight-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");
//
//        log("Add charge");
//        ExtHelper.clickExtTab(this, "Charges");
//        waitForElement(Locator.xpath("/*//*["+VISIBLE+" and not(contains(@class, 'x-hide-display')) and contains(@class,'ehr-charges-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");

        clickNavButton("Save & Close");

        stopImpersonating();
    }

    private void setMPRField(String tabName, String fieldName, String value)
    {
        setFormElement(Locator.xpath("//div[./div/span[text()='"+tabName+"']]//*[(self::input or self::textarea) and @name='"+fieldName+"']"), value);
        fireEvent(Locator.xpath("//div[./div/span[text()='"+tabName+"']]//*[(self::input or self::textarea) and @name='"+fieldName+"']"), SeleniumEvent.blur);
    }

    @Override
    public void runApiTests() throws Exception
    {
        testUserAgainstAllStates(DATA_ADMIN);
        // TODO: Fix. Connection drops on expected failures.
        //testUserAgainstAllStates(REQUESTER);
        //testUserAgainstAllStates(BASIC_SUBMITTER);
        //testUserAgainstAllStates(FULL_SUBMITTER);
    }

    private void testUserAgainstAllStates(EHRUser user) throws Exception
    {
        File[] scriptFiles = new File[EHRQCState.values().length];

        int i = 0;
        for(EHRQCState qcState : EHRQCState.values())
        {
            scriptFiles[i++] = prepareScript(user, qcState);
        }

        super.runApiTests(scriptFiles, user.getUser(), PasswordUtil.getPassword(), true);
    }

    private File prepareScript(EHRUser user, EHRQCState qcState) throws java.io.IOException
    {
        File preparedFile = new File(System.getProperty("java.io.tmpdir"), "ehr-"+user.getRole()+"-"+qcState.label.replace(":", "_")+"-insert.xml");

        BufferedReader reader = new BufferedReader(new FileReader(getLabKeyRoot() + SCRIPT_TEMPLATE));
        BufferedWriter writer = new BufferedWriter(new FileWriter(preparedFile));

        String line;
        boolean permitted = successExpected(user.getRole(), qcState);
        DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss");

        while ( (line = reader.readLine()) != null)
        {
            line = line.replace("${AnimalId}", MORE_ANIMAL_IDS[2]);
            line = line.replace("${QCState}", qcState.label);
            line = line.replace("${Role}", user.getRole().toString());
            Calendar now = Calendar.getInstance();
            now.add(Calendar.MINUTE, 1); // avoid creating records "in the past"
            line = line.replace("${Date}", dateFormat.format(now.getTime()));
            line = line.replace(permitted ? "successresponse>" : "failresponse>", "response>");

            writer.write(line + "\n");
        }

        writer.close();
        return preparedFile;
    }

    private void crawlReportTabs()
    {
        String tabs[] = {/*"-Assay", "MHC SSP Typing", "Viral Loads", */ //Bad queries on test server.
                         "-Assignments", "Active Assignments", "Assignment History",
                         "-Clin Path", "Bacteriology", "Chemistry:By Panel", "Clinpath Runs", "Hematology:By Panel", "Immunology:By Panel", "Parasitology", "Urinalysis:By Panel", "Viral Challenges", "Virology",
                         "-Clinical", "Abstract:Active Assignments", "Clinical Encounters", "Clinical Remarks", "Diarrhea Calendar", "Full History", "Full History Plus Obs", "Irregular Obs:Irregular Observations", "Problem List", "Procedure Codes", "Surgical History", "Tasks", "Treatment Orders", "Treatments", "Treatment Schedule", "Weights:Weight",
                         "-Colony Management", "Behavior Remarks", "Birth Records", "Housing - Active", "Housing History", "Inbreeding Coefficients", "Kinship", "Menses Calendar", "Menses Observations:Irregular Observations", "Pedigree:Offspring", /*"Pedigree Plot",*/ "Pregnancies", "TB Tests",
                         "-Pathology", "Biopsies", "Histology", "Morphologic Diagnosis", "Necropsies",
                         "-Physical Exam", "Alopecia", "Body Condition", "Dental Status", "Exams", "PE Findings", "Teeth", "Vitals",
                         "-Today At WNPRC", "Irregular Observations", "Obs/Treatment:Obs/Treatments", "Problem List", /*"Today's History",*/ "Treatments - Morning", "Treatments - Afternoon", "Treatments - Evening", "Treatments - Master", "Unresolved Problem List", /*"Today's Blood Draws",*/
                         "-General", "Arrival/Departure:Arrivals", "Blood Draw History", "Charges", "Current Blood", "Deaths", "Demographics", "Major Events", "Notes", "Abstract:Active Assignments"};

        log("Check all Animal History report tabs");
        for (String tab : tabs)
        {
            if(tab.startsWith("-")) // High level tab
            {
                ExtHelper.clickExtTab(this, tab.substring(1));
            }
            else
            {
                if(tab.contains(":"))
                {
                    ExtHelper.clickExtTab(this, tab.split(":")[0]);
                    getDataRegionName(tab.split(":")[1]);
                }
                else
                {
                    ExtHelper.clickExtTab(this, tab);
                    getDataRegionName(tab);
                }
            }
        }

        //Clear out lingering text on report pages
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitAndClick(Locator.linkWithText("Animal History"));
        waitForPageToLoad();
    }

    private boolean successExpected(EHRRole role, EHRQCState qcState)
    {
        // Expand to other request types once we start testing them. Insert only for now.
        return allowedActions.contains(new Permission(role, qcState, "insert"));
    }

    private static final ArrayList<Permission> allowedActions = new ArrayList<Permission>()
    {
        {
            // Data Admin - Users with this role are permitted to make any edits to datasets
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.ABNORMAL, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.COMPLETED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.DELETE_REQUESTED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.IN_PROGRESS, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_APPROVED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_COMPLETE, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REVIEW_REQUIRED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.SCHEDULED, "insert"));

            // Requester - Users with this role are permitted to submit requests, but not approve them
            add(new Permission(EHRRole.REQUESTER, EHRQCState.REQUEST_PENDING, "insert"));

            // Full Submitter - Users with this role are permitted to submit and approve records.  They cannot modify public data.
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.ABNORMAL, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.COMPLETED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.DELETE_REQUESTED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.IN_PROGRESS, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_APPROVED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_COMPLETE, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REVIEW_REQUIRED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.SCHEDULED, "insert"));

            // Basic Submitter - Users with this role are permitted to submit and edit non-public records, but cannot alter public ones
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.IN_PROGRESS, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REVIEW_REQUIRED, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.DELETE_REQUESTED, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_COMPLETE, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.SCHEDULED, "insert"));
        }
    };

    private static class Permission 
    {
        EHRRole role;
        EHRQCState qcState;
        String action;
        public Permission(EHRRole role, EHRQCState qcState, String action)
        {
            this.role = role;
            this.qcState = qcState;
            this.action = action;
        }

        @Override
        public boolean equals(Object other)
        {
            return other.getClass().equals(Permission.class) &&
                this.role == ((Permission)other).role &&
                this.qcState == ((Permission)other).qcState &&
                this.action.equals(((Permission)other).action);
        }
    }

    private static enum EHRRole
    {
        DATA_ADMIN ("EHR Data Admin"),
        REQUESTER ("EHR Requestor"),
        BASIC_SUBMITTER ("EHR Basic Submitter"),
        FULL_SUBMITTER ("EHR Full Submitter");
        private final String name;
        private EHRRole (String name)
        {this.name = name;}
        public String toString()
        {return name;}
    }

    private static enum EHRQCState
    {
        ABNORMAL("Abnormal", "Value is abnormal", true, false, false),
        COMPLETED("Completed", "Data has been approved for public release", true, false, false),
        DELETE_REQUESTED("Delete Requested", "Records are requested to be deleted", true, true, false),
        IN_PROGRESS("In Progress", "Draft Record, not public", false, true, false),
        REQUEST_APPROVED("Request: Approved", "Request has been approved", true, true, true),
        REQUEST_COMPLETE("Request: Complete", "Request has been completed", true, false, true),
        REQUEST_DENIED("Request: Denied", "Request has been denied", true, false, true),
        REQUEST_PENDING("Request: Pending", "Part of a request that has not been approved", false, false, true),
        REVIEW_REQUIRED("Review Required", "Review is required prior to public release", false, false, false),
        SCHEDULED("Scheduled", "Record is scheduled, but not performed", true, true, false);

        public final String label;
        public final String description;
        public final boolean publicData;

        public final boolean draftData;
        public final boolean isRequest;

        EHRQCState(String label, String description, boolean publicData, boolean draftData, boolean isRequest)
        {
            this.label = label;
            this.description = description;
            this.publicData = publicData;
            this.draftData = draftData;
            this.isRequest = isRequest;
        }
    }

    private void defineQCStates()
    {
        log("Define QC states for EHR study");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        goToModule("Study");
        clickLinkWithText("Manage Dataset QC States");

        for(EHRQCState qcState : EHRQCState.values())
        {
            setFormElement("newLabel", qcState.label);
            setFormElement("newDescription", qcState.description);
            if(!qcState.publicData) uncheckCheckbox("newPublicData");
            clickNavButton("Save");
        }

        setFormElement("showPrivateDataByDefault", "true");
        clickNavButton("Done");
    }

    private Locator getRadioButtonLocator(String groupName, String setting)
    {
        //not sure why the radios are in TH elements, but they are...
        return Locator.xpath("//form[@id='groupUpdateForm']/table/tbody/tr/td[text()='"
                + groupName + "']/../th/input[@value='" + setting + "']");
    }

    private String getDataRegionName(String title)
    {
        // Specific to the EHR Animal History page.
        waitForElement(Locator.xpath("//table[@name='webpart' and ./*/*/*/a//span[text()='"+title+"' or starts-with(text(), '"+title+":')]]//table[starts-with(@id,'dataregion_') and not(contains(@id, 'header'))]"), WAIT_FOR_JAVASCRIPT);
        return getAttribute(Locator.xpath("//table[@name='webpart' and ./*/*/*/a//span[text()='"+title+"' or starts-with(text(), '"+title+":')]]//table[starts-with(@id,'dataregion_') and not(contains(@id, 'header'))]"), "id").substring(11);
    }

    private void selectRecord(String query, String Id, boolean keepExisting)
    {
        getWrapper().getEval("selenium.selectExtGridItem('Id','" + Id + "', -1, 'ehr-" + query + "-records-grid', "+keepExisting+");");
        if(!keepExisting)waitForElement(Locator.xpath("//div[@id='Id']/a[text()='"+Id+"']"), WAIT_FOR_JAVASCRIPT);
    }

    private void clickVisibleButton(String text)
    {
        click(Locator.xpath("//button[text()='"+text+"' and "+VISIBLE+" and not(contains(@class, 'x-hide-display'))]"));
    }
}
