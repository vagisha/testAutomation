/*
 * Copyright (c) 2012-2015 LabKey Corporation
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
package org.labkey.test.tests;

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.util.List;

import static org.junit.Assert.*;

@Category({DailyB.class, Reports.class, Charting.class})
public class TimeChartVisitBasedTest extends TimeChartTest
{
    private static final String VISIT_REPORT_NAME = "TimeChartTest Visit Report";
    private static final String REPORT_DESCRIPTION = "This is a report generated by the TimeChartDateBasedTest";
    private static final String VISIT_CHART_TITLE = "APX-1: Abbreviated Physical Exam";
    private static final String QUERY_MEASURE_DATASET = "APX-1 (APX-1: Abbreviated Physical Exam)";

    private static final String[] VISIT_STRINGS = {"1 week Post-V#1", "Int. Vis. %{S.1.1} .%{S.2.1}", "Grp1:F/U/Grp2:V#2", "G1: 6wk/G2: 2wk", "6 week Post-V#2", "1 wk Post-V#2/V#3", "6 wk Post-V#2/V#3"};

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doCreateSteps()
    {
        configureVisitStudy();
    }

    @Override
    public void doVerifySteps()
    {
        visitBasedChartTest();
        filteredViewQueryMeasureTest();
        errorMessageTest();
    }

    @LogMethod public void visitBasedChartTest()
    {
        log("Create multi-measure time chart.");
        clickFolder(VISIT_FOLDER_NAME);
        goToManageViews();
        clickAddReport("Time Chart");
        clickChooseInitialMeasure();
        _ext4Helper.clickGridRowText("1. Weight", 0);
        clickButton("Select", 0);
        waitForText(WAIT_FOR_JAVASCRIPT, "Days Since Contact Date");

        // TODO: migrate usage to TimeChartWizard.changeXAxisToVisitBased
        goToSvgAxisTab("Days Since Contact Date");
        _ext4Helper.selectRadioButton("Chart Type:", "Visit Based Chart");
        assertElementPresent(Locator.xpath("//table[//label[text() = 'Draw x-axis as:'] and contains(@class, 'x4-item-disabled')]"));
        assertElementPresent(Locator.xpath("//table[//label[text() = 'Calculate time interval(s) relative to:'] and contains(@class, 'x4-item-disabled')]"));
        assertElementPresent(Locator.xpath("//table[//label[text() = 'Range:'] and contains(@class, 'x4-item-disabled')]"));
        applyChanges();
        waitForElementToDisappear(Locator.css("svg").containing("Days Since Contact Date"));
        waitForElement(Locator.css("svg").containing("6 week Post-V#2"));
        assertTextPresentInThisOrder(VISIT_STRINGS);

        log("Check visit data.");
        clickButton("View Data", 0);
        waitForElement(Locator.paginationText(19));

        // verify that other toolbar buttons have been hidden
        assertElementNotPresent(Locator.button("Export"));
        assertElementNotPresent(Locator.button("Measures"));
        assertElementNotPresent(Locator.button("Grouping"));
        assertElementNotPresent(Locator.button("Options"));
        assertElementNotPresent(Locator.button("Developer"));

        String tableId = getAttribute(Locator.xpath("//table[starts-with(@id, 'dataregion_') and contains(@class, 'labkey-data-region')]"), "id");
        String tableName = tableId.substring(tableId.indexOf('_') + 1, tableId.length());
        DataRegionTable table = new DataRegionTable(tableName, this, false, false);
        List displayOrders = table.getColumnDataAsText("Study APX1Abbreviated Physical Exam Mouse Visit Visit Display Order");
        for (Object str : displayOrders)
        {
            assertEquals("Display order should default to zero.", "0", str.toString());
        }

        List<String> visits = table.getColumnDataAsText("Visit Label");
        for( String str : VISIT_STRINGS )
        {
            assertTrue("Not all visits present in data table. Missing: " + str, visits.contains(str));
        }

        clickButton("View Chart(s)", 0);
        waitForElementToDisappear(Locator.paginationText(19));
        waitForCharts(1);
        log("Revert to Date-based chart to check axis panel state.");
        goToSvgAxisTab("Visit");
        _ext4Helper.selectRadioButton("Chart Type:", "Date Based Chart");
        assertElementPresent(Locator.xpath("//table[//label[text() = 'Draw x-axis as:'] and not(contains(@class, 'x4-item-disabled'))]"));
        assertElementPresent(Locator.xpath("//table[//label[text() = 'Calculate time interval(s) relative to:'] and not(contains(@class, 'x4-item-disabled'))]"));
        applyChanges();
        waitForTextToDisappear(VISIT_STRINGS[0]);
        assertTextNotPresent(VISIT_STRINGS);

        log("Back to visit-based chart for save.");
        goToSvgAxisTab("Days Since Contact Date");
        _ext4Helper.selectRadioButton("Chart Type:", "Visit Based Chart");
        applyChanges();
        waitForElement(Locator.css("svg").containing("6 week Post-V#2"));

        openSaveMenu();
        setFormElement(Locator.name("reportName"), VISIT_REPORT_NAME);
        setFormElement(Locator.name("reportDescription"), REPORT_DESCRIPTION);
        saveReport(true);
        waitForText(WAIT_FOR_JAVASCRIPT, VISIT_CHART_TITLE);
    }

    @LogMethod public void filteredViewQueryMeasureTest()
    {
        log("Create query over " + QUERY_MEASURE_DATASET + " dataset.");
        clickFolder(VISIT_FOLDER_NAME);
        goToModule("Query");
        createNewQuery("study");
        setFormElement(Locator.name("ff_newQueryName"), "My APX Query");
        selectOptionByText(Locator.name("ff_baseTableName"), QUERY_MEASURE_DATASET);
        clickButton("Create and Edit Source");
        setCodeEditorValue("queryText", "SELECT x.MouseId, x.MouseVisit, x.SequenceNum, x.APXtempc, x.sfdt_136 FROM \"APX-1: Abbreviated Physical Exam\" AS x");
        clickButton("Save & Finish");
        waitForElement(Locator.paginationText(47));

        // verify filtered view issue 16498
        log("Filter the default view of the query");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewFilter("sfdt_136", "Contains One Of", "1;2;");
        _customizeViewsHelper.saveCustomView();
        waitForElement(Locator.paginationText(31));

        log("Create a Time Chart from the measure in the new query");
        _extHelper.clickMenuButton("Charts", "Create Time Chart");
        clickChooseInitialMeasure();
        waitForText("My APX Query");
        _ext4Helper.clickGridRowText("2. Body Temp", 0);
        clickButton("Select", 0);
        waitForText(WAIT_FOR_JAVASCRIPT, "No calculated interval values (i.e. Days, Months, etc.) for the selected 'Measure Date' and 'Interval Start Date'.");
        goToSvgAxisTab("Days Since Contact Date");
        _ext4Helper.selectRadioButton("Chart Type:", "Visit Based Chart");
        applyChanges();
        waitForText(WAIT_FOR_JAVASCRIPT, "My APX Query");
        _ext4Helper.clickParticipantFilterGridRowText("999320016", 0);
        waitForText(WAIT_FOR_JAVASCRIPT, "4 wk Post-V#2/V#3"); // last visit from ptid 999320016
        assertTextPresent("2. Body Temp: ", 12); // hover text label (6 for chart (twice for each data point) and 6 for thumbnail in save dialog)
        clickButton("View Data", 0);
        waitForElement(Locator.paginationText(9));
        assertTextNotPresent("801.0", "G1: 6wk/G2: 2wk"); // sequenceNum filtered out by default view filter
        clickButton("View Chart(s)", 0);
        waitForElement(Locator.css("svg").withText("My APX Query"), WAIT_FOR_JAVASCRIPT, false);

        openSaveMenu();
        setFormElement(Locator.name("reportName"), VISIT_REPORT_NAME + " 2");
        saveReport(true);
        waitForText(WAIT_FOR_JAVASCRIPT, "My APX Query");
    }

    @LogMethod private void errorMessageTest()
    {
        log("Test renaming time chart measure");
        clickAndWait(Locator.linkWithText("Clinical and Assay Data"));
        waitForText(VISIT_REPORT_NAME);
        clickAndWait(Locator.linkWithText(VISIT_REPORT_NAME));
        waitForElement(Locator.css("svg").containing("6 week Post-V#2"));
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText("APX-1"));
        clickButton("Edit Definition");
        waitForElement(Locator.xpath("//input[@name='dsName']"));
        assertEquals("APXwtkg", getFormElement(Locator.name("ff_name1")));
        setFormElement(Locator.name("ff_name1"), "APXwtkgCHANGED");
        clickButton("Save");
        clickAndWait(Locator.linkWithText("Clinical and Assay Data"));
        waitForText(VISIT_REPORT_NAME);
        clickAndWait(Locator.linkWithText(VISIT_REPORT_NAME));
        waitForText("Error: Unable to find field APXwtkg in study.APX-1.");
        assertTextPresent("The field may have been deleted, renamed, or you may not have permissions to read the data.");

        log("Test deleting time chart measure's dataset");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText("APX-1"));
        clickButton("Delete Dataset", 0);
        assertTrue(acceptAlert().contains("Are you sure you want to delete this dataset?"));
        waitForText("The study schedule defines"); // text on the Manage Datasets page
        clickAndWait(Locator.linkWithText("Clinical and Assay Data"));
        waitForText(VISIT_REPORT_NAME);
        clickAndWait(Locator.linkWithText(VISIT_REPORT_NAME));
        waitForText("Error: Unable to find table study.APX-1.");
        assertTextPresent("The table may have been deleted, or you may not have permissions to read the data.");

        log("Delete My APX Query so it doesn't fail query validation");
        goToSchemaBrowser();
        selectQuery("study", "My APX Query");
        clickAndWait(Locator.linkWithText("Delete Query"));
        waitForText("Are you sure you want to delete the query 'My APX Query'?");
        clickButton("OK");
    }
}
