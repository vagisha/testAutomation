/*
 * Copyright (c) 2009-2011 LabKey Corporation
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
package org.labkey.test.drt;

import org.apache.commons.lang.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;

import java.util.LinkedList;
import java.util.List;

/**
 * User: adam
 * Date: Apr 3, 2009
 * Time: 9:18:32 AM
 */
public class StudyTest extends StudyEmptyTest
{
    protected static final String DEMOGRAPHICS_DESCRIPTION = "This is the demographics dataset, dammit. Here are some \u2018special symbols\u2019 - they help test that we're roundtripping in UTF-8.";

    protected String _tsv = "participantid\tsequencenum\tvisitdate\tSampleId\tDateField\tNumberField\tTextField\treplace\taliasedColumn\n" +
        "1234\t1\t1/1/2006\t1234_A\t2/1/2006\t1.2\ttext\t\taliasedData\n" +
        "1234\t1\t1/1/2006\t1234_B\t2/1/2006\t1.2\ttext\t\taliasedData\n";

    // specimen comment constants
    private static final String PARTICIPANT_CMT_DATASET = "Mouse Comments";
    private static final String PARTICIPANT_VISIT_CMT_DATASET = "Mouse Visit Comments";
    private static final String COMMENT_FIELD_NAME = "comment";
    private static final String PARTICIPANT_COMMENT_LABEL = "mouse comment";
    private static final String PARTICIPANT_VISIT_COMMENT_LABEL = "mouse visit comment";

    protected static final String VISIT_IMPORT_MAPPING = "Name\tSequenceNum\n" +
        "Cycle 10\t10\n" +
        "Soc Imp Log #%{S.3.2}\t5500\n" +
        "ConMeds Log #%{S.3.2}\t9002\n" +
        "All Done\t9999";
    
    protected void doVerifySteps()
    {
        verifyStudyAndDatasets();
        waitForSpecimenImport();
        verifySpecimens();
        verifyParticipantComments();
    }

    protected void verifyStudyAndDatasets()
    {
        verifyDemographics();
        verifyVisitMapPage();
        verifyManageDatasetsPage();
        verifyHiddenVisits();
        verifyVisitImportMapping();
        verifyCohorts();

        // configure QC state management before importing duplicate data
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Dataset QC States");
        setFormElement("newLabel", "unknown QC");
        setFormElement("newDescription", "Unknown data is neither clean nor dirty.");
        clickCheckboxById("dirty_public");
        clickCheckbox("newPublicData");
        clickNavButton("Save");
        selectOptionByText("defaultDirectEntryQCState", "unknown QC");
        selectOptionByText("showPrivateDataByDefault", "Public data");
        clickNavButton("Save");

        // return to dataset import page
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("verifyAssay");
        assertTextPresent("QC State");
        assertTextNotPresent("1234_B");
        clickMenuButton("QC State", "All data");
        clickButton("QC State", 0);
        assertTextPresent("unknown QC");
        assertTextPresent("1234_B");

        //Import duplicate data
        clickNavButton("Import Data");
        setFormElement("tsv", _tsv);
        clickNavButton("Import Data");
        assertTextPresent("Duplicates were found");
        //Now explicitly replace, using 'mouseid' instead of 'participantid'
        _tsv = "mouseid\tsequencenum\tvisitdate\tSampleId\tDateField\tNumberField\tTextField\treplace\n" +
                "1234\t1\t1/1/2006\t1234_A\t2/1/2006\t5000\tnew text\tTRUE\n" +
                "1234\t1\t1/1/2006\t1234_B\t2/1/2006\t5000\tnew text\tTRUE\n";
        setFormElement("tsv", _tsv);
        clickNavButton("Import Data");
        assertTextPresent("5000.0");
        assertTextPresent("new text");
        assertTextPresent("QC State");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "QCState", "QC State");
        CustomizeViewsHelper.applyCustomView(this);
        assertTextPresent("unknown QC");
    }

    protected void verifySpecimens()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Blood (Whole)");
        clickMenuButton("Page Size", "Show All");
        assertTextNotPresent("DRT000XX-01");
        assertTextPresent("GAA082NH-01");
        clickLinkWithText("Hide Vial Info");
        assertTextPresent("Total:");
        assertTextPresent("466");

        assertTextNotPresent("BAD");

        clickLinkWithText("Show Vial Info");
        clickLinkContainingText("history");
        // verify that we're correctly parsing frozen time, which is a date with a time portion only:
        assertTextPresent("15:30:00");
        assertTextPresent("2.0&nbsp;ML");
        assertTextNotPresent("Added Comments");
        // confirm collection location:
        assertTextPresent("KCMC, Moshi, Tanzania");
        // confirm historical locations:
        assertTextPresent("Contract Lab Services, Johannesburg, South Africa");
        assertTextPresent("Aurum Health KOSH Lab, Orkney, South Africa");

        clickLinkWithText("Specimen Overview");
        clickLinkWithText("By Vial");
        DataRegionTable table = new DataRegionTable("SpecimenDetail", this);
        table.setFilter("QualityControlFlag", "Equals", "true");
        table.setSort("GlobalUniqueId", SortDirection.ASC);
        assertEquals("AAA07XK5-02", table.getDataAsText(0, "Global Unique Id"));
        assertEquals("Conflicts found: AdditiveTypeId, DerivativeTypeId, PrimaryTypeId", table.getDataAsText(0, "Quality Control Comments"));
        assertEquals("", table.getDataAsText(0, "Primary Type"));
        assertEquals("", table.getDataAsText(0, "Additive Type"));

        assertEquals("ABH00LT8-01", table.getDataAsText(2, "Global Unique Id"));
        assertEquals("Conflicts found: VolumeUnits", table.getDataAsText(2, "Quality Control Comments"));
        assertEquals("", table.getDataAsText(2, "Volume Units"));

        clickLinkContainingText("history");
        assertTextPresent("Blood (Whole)");
        assertTextPresent("Vaginal Swab");
        assertTextPresent("Vial is flagged for quality control");
        clickLinkWithText("update");
        setFormElement("qualityControlFlag", "false");
        setFormElement("comments", "Manually removed flag");
        clickNavButton("Save Changes");
        assertTextPresent("Manually removed flag");
        assertTextPresent("Conflicts found: AdditiveTypeId, DerivativeTypeId, PrimaryTypeId");
        assertTextNotPresent("Vial is flagged for quality control");
        clickLinkWithText("return to vial view");
        assertTextNotPresent("AAA07XK5-02");
        assertTextPresent("KBH00S5S-01");
    }

    private void verifyParticipantComments()
    {
        log("creating the participant/visit comment dataset");
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");

        setFormElement("typeName", PARTICIPANT_CMT_DATASET);
        clickNavButton("Next");
        waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_JAVASCRIPT);

        // set the demographic data checkbox
        checkCheckbox(Locator.xpath("//input[@name='demographicData']"));

        // add a comment field
        ListHelper.setColumnName(this, 0, COMMENT_FIELD_NAME);
        ListHelper.setColumnLabel(this, 0, PARTICIPANT_COMMENT_LABEL);
        ListHelper.setColumnType(this, 0, ListHelper.ListColumnType.MutliLine);
        clickNavButton("Save");

        log("creating the participant/visit comment dataset");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");

        setFormElement("typeName", PARTICIPANT_VISIT_CMT_DATASET);
        clickNavButton("Next");
        waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_JAVASCRIPT);

        // add a comment field
        ListHelper.setColumnName(this, 0, COMMENT_FIELD_NAME);
        ListHelper.setColumnLabel(this, 0, PARTICIPANT_VISIT_COMMENT_LABEL);
        ListHelper.setColumnType(this, 0, ListHelper.ListColumnType.MutliLine);
        clickNavButton("Save");

        log("configure comments");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Comments");
        if (isTextPresent("Comments can only be configured for studies with editable datasets"))
        {
            log("configure editable datasets");
            clickLinkWithText("Manage Study");
            clickLinkWithText("Manage Security");
            selectOptionByText("securityString", "Basic security with editable datasets");
            waitForPageToLoad();
            clickNavButton("Update");

            log("configure comments");
            clickLinkWithText(getStudyLabel());
            clickLinkWithText("Manage Study");
            clickLinkWithText("Manage Comments");
        }
        selectOptionByText("participantCommentDataSetId", PARTICIPANT_CMT_DATASET);
        waitForPageToLoad();
        selectOptionByText("participantCommentProperty", PARTICIPANT_COMMENT_LABEL);

        selectOptionByText("participantVisitCommentDataSetId", PARTICIPANT_VISIT_CMT_DATASET);
        waitForPageToLoad();
        selectOptionByText("participantVisitCommentProperty", PARTICIPANT_VISIT_COMMENT_LABEL);
        clickNavButton("Save");

        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Blood (Whole)");
        clickNavButton("Enable Comments/QC");
        log("manage participant comments directly");
        clickMenuButton("Comments and QC", "Manage Mouse Comments");

        clickNavButton("Insert New");
        setFormElement("quf_MouseId", "999320812");
        setFormElement("quf_" + COMMENT_FIELD_NAME, "Mouse Comment");
        clickNavButton("Submit");

        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Blood (Whole)");
        setFilter("SpecimenDetail", "MouseId", "Equals", "999320812");

        assertTextPresent("Mouse Comment");
        clearAllFilters("SpecimenDetail", "MouseId");

        log("verify copying and moving vial comments");
        setFilter("SpecimenDetail", "GlobalUniqueId", "Equals", "AAA07XK5-01");
        selenium.click(".toggle");
        clickNavButton("Enable Comments/QC");
        clickMenuButton("Comments and QC", "Set Vial Comment or QC State for Selected");
        setFormElement("comments", "Vial Comment");
        clickNavButton("Save Changes");

        selenium.click(".toggle");
        clickMenuButton("Comments and QC", "Set");
        clickMenuButton("Copy or Move Comment(s)", "Copy", "To Mouse", "999320812");
        setFormElement("quf_" + COMMENT_FIELD_NAME, "Copied PTID Comment");
        clickNavButton("Submit");
        assertTextPresent("Copied PTID Comment");

        selenium.click(".toggle");
        clickMenuButton("Comments and QC", "Set Vial Comment or QC State for Selected");
        clickMenuButtonAndContinue("Copy or Move Comment(s)", "Move", "To Mouse", "999320812");
        getConfirmationAndWait();
        setFormElement("quf_" + COMMENT_FIELD_NAME, "Moved PTID Comment");
        clickNavButton("Submit");
        assertTextPresent("Moved PTID Comment");
        assertTextNotPresent("Mouse Comment");
        assertTextNotPresent("Vial Comment");
    }

    private void verifyDemographics()
    {
        clickLinkWithText(getFolderName());
        clickLinkWithText("Study Navigator");
        clickLinkWithText("24");
        assertTextPresent(DEMOGRAPHICS_DESCRIPTION);
        assertTextPresent("Male");
        assertTextPresent("African American or Black");
        clickLinkWithText("999320016");
        clickLinkWithText("125: EVC-1: Enrollment Vaccination", false);
        assertTextPresent("right deltoid");
    }

    protected void verifyVisitMapPage()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Visits");

        // test optional/required/not associated
        clickLinkWithText("edit", 0);
        selectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 1, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 2, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 3, "OPTIONAL");
        selectOption("dataSetStatus", 4, "OPTIONAL");
        selectOption("dataSetStatus", 5, "OPTIONAL");
        selectOption("dataSetStatus", 6, "REQUIRED");
        selectOption("dataSetStatus", 7, "REQUIRED");
        selectOption("dataSetStatus", 8, "REQUIRED");
        clickNavButton("Save");
        clickLinkWithText("edit", 0);
        selectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 1, "OPTIONAL");
        selectOption("dataSetStatus", 2, "REQUIRED");
        selectOption("dataSetStatus", 3, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 4, "OPTIONAL");
        selectOption("dataSetStatus", 5, "REQUIRED");
        selectOption("dataSetStatus", 6, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 7, "OPTIONAL");
        selectOption("dataSetStatus", 8, "REQUIRED");
        clickNavButton("Save");
        clickLinkWithText("edit", 0);
        assertSelectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 1, "OPTIONAL");
        assertSelectOption("dataSetStatus", 2, "REQUIRED");
        assertSelectOption("dataSetStatus", 3, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 4, "OPTIONAL");
        assertSelectOption("dataSetStatus", 5, "REQUIRED");
        assertSelectOption("dataSetStatus", 6, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 7, "OPTIONAL");
        assertSelectOption("dataSetStatus", 8, "REQUIRED");
    }

    protected void verifyManageDatasetsPage()
    {
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Datasets");

        clickLinkWithText("489");
        assertTextPresent("ESIdt");
        assertTextPresent("Form Completion Date");
        assertTableCellTextEquals("details", 4, 1, "false");     // "Demographics Data" should be false

        // Verify that "Demographics Data" is checked and description is set
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("DEM-1: Demographics");
        assertTableCellTextEquals("details", 4, 1, "true");
        assertTableCellTextEquals("details", 3, 3, DEMOGRAPHICS_DESCRIPTION);

        // "Demographics Data" bit needs to be false for the rest of the test
        setDemographicsBit("DEM-1: Demographics", false);
    }

    private void verifyHiddenVisits()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Study Navigator");
        assertTextNotPresent("Screening Cycle");
        assertTextNotPresent("Cycle 1");
        assertTextPresent("Pre-exist Cond");
        clickLinkWithText("Show Hidden Data");
        assertTextPresent("Screening Cycle");
        assertTextPresent("Cycle 1");
        assertTextPresent("Pre-exist Cond");
    }

    private void verifyVisitImportMapping()
    {
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Visits");
        clickLinkWithText("Visit Import Mapping");
        assertTableRowsEqual("customMapping", 2, VISIT_IMPORT_MAPPING.replace("SequenceNum", "Sequence Number"));

        assertEquals("Incorrect number of gray cells", countTableCells(null, true), 40);
        assertEquals("Incorrect number of non-gray \"Int. Vis. %{S.1.1} .%{S.2.1}\" cells", countTableCells("Int. Vis. %{S.1.1} .%{S.2.1}", false), 1);
        assertEquals("Incorrect number of gray \"Int. Vis. %{S.1.1} .%{S.2.1}\" cells", countTableCells("Int. Vis. %{S.1.1} .%{S.2.1}", true), 18);
        assertEquals("Incorrect number of non-gray \"Soc Imp Log #%{S.3.2}\" cells", countTableCells("Soc Imp Log #%{S.3.2}", false), 1);
        assertEquals("Incorrect number of gray \"Soc Imp Log #%{S.3.2}\" cells", countTableCells("Soc Imp Log #%{S.3.2}", true), 1);
        assertEquals("Incorrect number of non-gray \"ConMeds Log #%{S.3.2}\" cells", countTableCells("ConMeds Log #%{S.3.2}", false), 1);
        assertEquals("Incorrect number of gray \"ConMeds Log #%{S.3.2}\" cells", countTableCells("ConMeds Log #%{S.3.2}", true), 1);

        // Replace custom visit mapping and verify
        String replaceMapping = "Name\tSequenceNum\nBarBar\t4839\nFoofoo\t9732";
        clickLinkWithText("Replace Custom Mapping");
        setLongTextField("tsv", replaceMapping);
        clickNavButton("Submit");
        assertTableRowsEqual("customMapping", 2, replaceMapping.replace("SequenceNum", "Sequence Number"));
        assertTextNotPresent("Cycle 10");
        assertTextNotPresent("All Done");

        assertEquals("Incorrect number of gray cells", countTableCells(null, true), 36);
        assertEquals("Incorrect number of non-gray \"Int. Vis. %{S.1.1} .%{S.2.1}\" cells", countTableCells("Int. Vis. %{S.1.1} .%{S.2.1}", false), 1);
        assertEquals("Incorrect number of gray \"Int. Vis. %{S.1.1} .%{S.2.1}\" cells", countTableCells("Int. Vis. %{S.1.1} .%{S.2.1}", true), 18);
        assertEquals("Incorrect number of non-gray \"Soc Imp Log #%{S.3.2}\" cells", countTableCells("Soc Imp Log #%{S.3.2}", false), 1);
        assertEquals("Incorrect number of gray \"Soc Imp Log #%{S.3.2}\" cells", countTableCells("Soc Imp Log #%{S.3.2}", true), 0);
        assertEquals("Incorrect number of non-gray \"ConMeds Log #%{S.3.2}\" cells", countTableCells("ConMeds Log #%{S.3.2}", false), 1);
        assertEquals("Incorrect number of gray \"ConMeds Log #%{S.3.2}\" cells", countTableCells("ConMeds Log #%{S.3.2}", true), 0);

        // Clear custom visit mapping and verify
        clickLinkWithText("Clear Custom Mapping");
        clickLinkWithText("OK");
        assertTextPresent("The custom mapping is currently empty");
        assertNavButtonPresent("Import Custom Mapping");
        assertNavButtonNotPresent("Replace Custom Mapping");
        assertNavButtonNotPresent("Clear Custom Mapping");
        assertTextNotPresent("BarBar");
        assertTextNotPresent("FooFoo");
        assertTextNotPresent("Cycle 10");
        assertTextNotPresent("All Done");
    }

    // Either param can be null
    private int countTableCells(String text, Boolean grayed)
    {
        List<String> parts = new LinkedList<String>();

        if (null != text)
            parts.add("contains(text(), '" + text + "')");

        if (null != grayed)
        {
            if (grayed)
                parts.add("contains(@class, 'labkey-mv')");
            else
                parts.add("not(contains(@class, 'labkey-mv'))");
        }

        String path = "//td[" + StringUtils.join(parts, " and ") + "]";
        return getXpathCount(Locator.xpath(path));
    }

    private void verifyCohorts()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Study Navigator");
        clickLinkWithText("24");

        // verify that cohorts are working
        assertTextPresent("999320016");
        assertTextPresent("999320518");

        clickMenuButton("Cohorts", "Group 1");
        assertTextPresent("999320016");
        assertTextNotPresent("999320518");

        clickMenuButton("Cohorts", "Group 2");
        assertTextNotPresent("999320016");
        assertTextPresent("999320518");

        // verify that the participant view respects the cohort filter:
        setSort("Dataset", "MouseId", SortDirection.ASC);
        clickLinkWithText("999320518");
        clickLinkWithText("125: EVC-1: Enrollment Vaccination", false);
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Mouse");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Mouse");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Mouse");
    }
}
