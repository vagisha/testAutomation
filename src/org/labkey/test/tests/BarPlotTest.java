/*
 * Copyright (c) 2016 LabKey Corporation
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

import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.ChartLayoutDialog;
import org.labkey.test.components.ChartQueryDialog;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.ColumnChartRegion;
import org.labkey.test.components.LookAndFeelBarPlot;
import org.labkey.test.components.LookAndFeelScatterPlot;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.LogMethod;

import java.net.URL;

@Category({DailyC.class, Reports.class, Charting.class})
public class BarPlotTest extends GenericChartsTest
{

    private final String PREG_TEST_RESULTS = "17a. Preg. test result";
    private final String BP_DIASTOLIC = "3. BP diastolic /xxx";
    private final String BAR_PLOT_SAVE_NAME = "Simple Bar Plot test";
    private final String BAR_PLOT_SAVE_NAME_2 = "Simple Bar Plot test with manual ranges";

    final String TRICKY_CHART_TITLE = CHART_TITLE + TRICKY_CHARACTERS;
    private final String SIMPLE_BAR_PLOT_SVG_TEXT = "0\nNegative\n0\n5\n10\n15\n20\n25\n30\n35\n40\n45\nAPX-1: Abbreviated Physical Exam\n" + PREG_TEST_RESULTS;
    private final String SECOND_BAR_PLOT_SVG_TEXT = "0\nNegative\n0\n200\n400\n600\n800\n1000\n1200\n1400\n1600\n1800\n2000\n2200\n2400\n" + TRICKY_CHART_TITLE + "\n" + PREG_TEST_RESULTS + "\nSum of " + BP_DIASTOLIC;
    private final String THIRD_BAR_PLOT_SVG_TEXT = "0\nNegative\n-50\n-49\n-48\n-47\n-46\n-45\n-44\n-43\n-42\n-41\n-40\n"+ TRICKY_CHART_TITLE + "\n" + PREG_TEST_RESULTS + "\nSum of " + BP_DIASTOLIC;
    private final String FOURTH_BAR_PLOT_SVG_TEXT = "0\nNegative\n200\n400\n600\n800\n1000\n1200\n1400\n1600\n1800\n2000\n2200\n2400\n2600\n2800\n3000\n"+ TRICKY_CHART_TITLE + "\n" + PREG_TEST_RESULTS + "\nSum of " + BP_DIASTOLIC;

    @LogMethod
    protected void testPlots()
    {
        doBasicBarPlotTest();
        doColumnPlotClickThrough();
        doExportOfBarPlot();
        doQuickChart();
        doAxisManualRangeBarPlotTest();
    }

    @LogMethod
    private void doBasicBarPlotTest()
    {
        final String APX_1_QUERY = "APX-1 (APX-1: Abbreviated Physical Exam)";
        final String COLOR_RED = "FF0000";
        final String COLOR_BLUE = "0000FF";
        final String COLOR_GREEN = "00FF00";

        ChartQueryDialog queryDialog;
        ChartTypeDialog chartTypeDialog;
        LookAndFeelBarPlot lookAndFeelDialog;

        String strTemp, svgDefaultHeight, svgDefaultWidth;

        log("Create a bar chart and then set different values using the dialogs.");
        goToProjectHome();
        clickProject(getProjectName());
        clickFolder(getFolderName());
        chartTypeDialog = clickAddChart("study", APX_1_QUERY);

        log("Start simply by setting just an X category.");
        // note: this should default to a Bar plot when no render type provided
        chartTypeDialog.setXCategory(PREG_TEST_RESULTS)
                .clickApply();

        shortWait().until(LabKeyExpectedConditions.animationIsDone(Locator.css("svg")));

        log("Validate that the plot text is as expected.");
        assertSVG(SIMPLE_BAR_PLOT_SVG_TEXT);

        // Not always sure what the height and width of the chart will be so get the defaults.
        svgDefaultHeight = getAttribute(Locator.css("svg"), "height");
        svgDefaultWidth = getAttribute(Locator.css("svg"), "width");

        log("Change some of the look and feel settings.");
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelBarPlot(getDriver());
        lookAndFeelDialog.setPlotTitle(TRICKY_CHART_TITLE)
                .setFillColor(COLOR_RED)
                .setLineColor(COLOR_BLUE)
                .setLineWidth(5)
                .setOpacity(50)
                .setPlotWidth("750")
                .setPlotHeight("750");

        log("Validate that the label for the x-axis is as expected.");
        lookAndFeelDialog.clickXAxisTab();
        strTemp = lookAndFeelDialog.getXAxisLabel();
        Assert.assertEquals("X-Axis label is not as expected.", PREG_TEST_RESULTS, strTemp);

        log("Apply the changes made.");
        lookAndFeelDialog.clickApply();

        shortWait().until(LabKeyExpectedConditions.animationIsDone(Locator.css("svg")));

        log("Validate that the changes made are reflected in the bar plot.");
        Assert.assertEquals("Plot width not as expected.", "750", getAttribute(Locator.css("svg"), "width"));
        Assert.assertEquals("Plot height not as expected.", "750", getAttribute(Locator.css("svg"), "height"));
        Assert.assertEquals("Fill color not as expected.", "#" + COLOR_RED, getAttribute(Locator.css("svg a.bar-individual rect.bar-rect"), "fill"));
        Assert.assertEquals("fill-opacity not as expected.", "0.5", getAttribute(Locator.css("svg a.bar-individual rect.bar-rect"), "fill-opacity"));
        Assert.assertEquals("Stroke color not as expected.", "#" + COLOR_BLUE, getAttribute(Locator.css("svg a.bar-individual rect.bar-rect"), "stroke"));
        Assert.assertEquals("Stroke-width not as expected.", "5", getAttribute(Locator.css("svg a.bar-individual rect.bar-rect"), "stroke-width"));

        log("Add a y-axis value");
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setYAxis(BP_DIASTOLIC)
                .clickApply();

        shortWait().until(LabKeyExpectedConditions.animationIsDone(Locator.css("svg")));

        log("Validate that the plot text now shows the y-axis values.");
        assertSVG(SECOND_BAR_PLOT_SVG_TEXT);

        log("Change some more of the look and feel.");
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelBarPlot(getDriver());
        lookAndFeelDialog.setFillColor(COLOR_GREEN)
                .setLineColor(COLOR_RED)
                .setPlotWidth("")
                .setPlotHeight("");

        log("Validate that the y-axis label is as expected.");
        lookAndFeelDialog.clickYAxisTab();
        strTemp = lookAndFeelDialog.getYAxisLabel();
        Assert.assertEquals("Y-axis label not as expected.", "Sum of " + BP_DIASTOLIC, strTemp);

        lookAndFeelDialog.clickApply();

        log("Validate that the updated changes made are reflected in the bar plot.");
        Assert.assertEquals("Plot widht not as expected.", svgDefaultWidth, getAttribute(Locator.css("svg"), "width"));
        Assert.assertEquals("Plot height not as expected.", svgDefaultHeight, getAttribute(Locator.css("svg"), "height"));
        Assert.assertEquals("Fill color not as expected.", "#" + COLOR_GREEN, getAttribute(Locator.css("svg a.bar-individual rect.bar-rect"), "fill"));
        Assert.assertEquals("fill-opacity not as expected.", "0.5", getAttribute(Locator.css("svg a.bar-individual rect.bar-rect"), "fill-opacity"));
        Assert.assertEquals("Stroke color not as expected.", "#" + COLOR_RED, getAttribute(Locator.css("svg a.bar-individual rect.bar-rect"), "stroke"));
        Assert.assertEquals("Stroke-width not as expected.", "5", getAttribute(Locator.css("svg a.bar-individual rect.bar-rect"), "stroke-width"));

        log("Save the plot.");
        savePlot(BAR_PLOT_SAVE_NAME, "This is a bar plot from the simple bar plot test.");

    }

    @LogMethod
    private void doColumnPlotClickThrough()
    {
        final String DATA_SOURCE_1 = "ENR-1: Enrollment";
        final String COL_NAME_BAR = "ENRknow";
        final String COL_TEXT_BAR = "2j.Know someone/HIV";

        ColumnChartRegion plotRegion;
        DataRegionTable dataRegionTable;
        ChartTypeDialog chartTypeDialog;
        ChartLayoutDialog chartLayoutDialog;
        int expectedPlotCount = 0;
        String strTemp;

        log("Go to the '" + DATA_SOURCE_1 + "' grid to create a bar plot from one of the columns.");

        goToProjectHome();
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(DATA_SOURCE_1));
        click(Locator.linkWithText(DATA_SOURCE_1));

        dataRegionTable = new DataRegionTable("Dataset", getDriver());
        plotRegion = dataRegionTable.getColumnPlotRegion();

        log("If the plot view is visible, revert it.");
        if(plotRegion.isRegionVisible())
            plotRegion.revertView();

        log("Create a bar plot.");
        dataRegionTable.createBarChart(COL_NAME_BAR);
        expectedPlotCount++;

        log("Refresh the reference to the plotRegion object and get a count of the plots.");
        plotRegion = dataRegionTable.getColumnPlotRegion();
        Assert.assertEquals("Number of plots not as expected.", expectedPlotCount, plotRegion.getPlots().size());

        log("Now that a plot has been created, assert that the plot region is visible.");
        Assert.assertTrue("The plot region is not visible after a chart was created. It should be.", plotRegion.isRegionVisible());

        log("Click on the bar plot and validate that we are redirected to the plot wizard.");
        clickAndWait(plotRegion.getPlots().get(0), WAIT_FOR_PAGE);

        URL currentUrl = getURL();
        log("Current url path: " + currentUrl.getPath());
        Assert.assertTrue("It doesn't look like we navigated to the expected page.", currentUrl.getPath().toLowerCase().contains("visualization-genericchartwizard.view"));

        waitForElement(Locator.css("svg"));

        chartTypeDialog = clickChartTypeButton();

        strTemp = chartTypeDialog.getCategories();
        Assert.assertTrue("Categories field did not contain the expected value. Expected '" + COL_TEXT_BAR + "'. Found '" + strTemp + "'", strTemp.toLowerCase().equals(COL_TEXT_BAR.toLowerCase()));

        strTemp = chartTypeDialog.getMeasure();
        Assert.assertEquals("Measure field was not empty. Found '" + strTemp + "'", 0, strTemp.trim().length());

        chartTypeDialog.clickCancel();

        clickChartLayoutButton();
        chartLayoutDialog = new LookAndFeelBarPlot(getDriver());

        strTemp = chartLayoutDialog.getPlotTitle();
        Assert.assertTrue("Value for plot title not as expected. Expected '" + DATA_SOURCE_1 + "' found '" + strTemp + "'", strTemp.toLowerCase().equals(DATA_SOURCE_1.toLowerCase()));

        strTemp = chartLayoutDialog.getXAxisLabel();
        Assert.assertTrue("Value for plot x-axis label not as expected. Expected '" + COL_TEXT_BAR + "' found '" + strTemp + "'", strTemp.toLowerCase().equals(COL_TEXT_BAR.toLowerCase()));

        chartLayoutDialog.clickCancel();
    }

    @LogMethod
    private void doExportOfBarPlot()
    {
        final String EXPORTED_SCRIPT_CHECK_TYPE = "\"renderType\":\"bar_chart\"";
        final String EXPORTED_SCRIPT_CHECK_XAXIS = PREG_TEST_RESULTS;
        final String EXPORTED_SCRIPT_CHECK_YAXIS = "Sum of " + BP_DIASTOLIC;

        log("Validate that export of the bar plot works.");
        goToProjectHome();
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(BAR_PLOT_SAVE_NAME));
        clickAndWait(Locator.linkWithText(BAR_PLOT_SAVE_NAME), WAIT_FOR_PAGE);

        waitForElement(Locator.css("svg"));

        log("Export as PDF");
        clickExportPDFIcon("chart-render-div", 0);

        log("Export as PNG");
        clickExportPNGIcon("chart-render-div", 0);

        log("Export to script.");
        Assert.assertEquals("Unexpected number of export script icons", 1, getExportScriptIconCount("chart-render-div"));
        clickExportScriptIcon("chart-render-div", 0);
        String exportScript = _extHelper.getCodeMirrorValue("export-script-textarea");
        waitAndClick(Ext4Helper.Locators.ext4Button("Close"));

        log("Validate that the script is as expected.");
        Assert.assertTrue("Script did not contain expected text: '" + EXPORTED_SCRIPT_CHECK_TYPE + "' ", exportScript.toLowerCase().contains(EXPORTED_SCRIPT_CHECK_TYPE.toLowerCase()));
        Assert.assertTrue("Script did not contain expected text: '" + EXPORTED_SCRIPT_CHECK_XAXIS + "' ", exportScript.toLowerCase().contains(EXPORTED_SCRIPT_CHECK_XAXIS.toLowerCase()));
        Assert.assertTrue("Script did not contain expected text: '" + EXPORTED_SCRIPT_CHECK_YAXIS + "' ", exportScript.toLowerCase().contains(EXPORTED_SCRIPT_CHECK_YAXIS.toLowerCase()));

    }

    @LogMethod
    private void doQuickChart()
    {
        final String DATA_SOURCE_1 = "DEM-1: Demographics";
        final String COL_NAME_BAR = "DEMdt";
        final String COL_TEXT_BAR = "Contact Date";

        DataRegionTable dataRegionTable;
        ChartTypeDialog chartTypeDialog;
        ChartLayoutDialog chartLayoutDialog;
        String strTemp;

        log("Go to the '" + DATA_SOURCE_1 + "' grid to create a quick chart (bar plot) from one of the columns.");

        goToProjectHome();
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(DATA_SOURCE_1));
        click(Locator.linkWithText(DATA_SOURCE_1));

        dataRegionTable = new DataRegionTable("Dataset", getDriver());

        log("Create a quick chart and validate that we are redirected to the wizard.");
        dataRegionTable.createQuickChart(COL_NAME_BAR);

        URL currentUrl = getURL();
        log("Current url path: " + currentUrl.getPath());
        Assert.assertTrue("It doesn't look like we navigated to the expected page.", currentUrl.getPath().toLowerCase().contains("visualization-genericchartwizard.view"));

        waitForElement(Locator.css("svg"));

        chartTypeDialog = clickChartTypeButton();

        strTemp = chartTypeDialog.getCategories();
        Assert.assertTrue("Categories field did not contain the expected value. Expected '" + COL_TEXT_BAR + "'. Found '" + strTemp + "'", strTemp.toLowerCase().equals(COL_TEXT_BAR.toLowerCase()));

        strTemp = chartTypeDialog.getMeasure();
        Assert.assertEquals("Measure field was not empty. Found '" + strTemp + "'", 0, strTemp.trim().length());

        chartTypeDialog.clickCancel();

        clickChartLayoutButton();
        chartLayoutDialog = new LookAndFeelBarPlot(getDriver());

        strTemp = chartLayoutDialog.getPlotTitle();
        Assert.assertTrue("Value for plot title not as expected. Expected '" + DATA_SOURCE_1 + "' found '" + strTemp + "'", strTemp.toLowerCase().equals(DATA_SOURCE_1.toLowerCase()));

        strTemp = chartLayoutDialog.getXAxisLabel();
        Assert.assertTrue("Value for plot x-axis label not as expected. Expected '" + COL_TEXT_BAR + "' found '" + strTemp + "'", strTemp.toLowerCase().equals(COL_TEXT_BAR.toLowerCase()));

        chartLayoutDialog.clickCancel();
    }

    @LogMethod
    private void doAxisManualRangeBarPlotTest()
    {
        LookAndFeelScatterPlot lookAndFeelDialog;

        clickProject(getProjectName());
        clickFolder(getFolderName());
        openSavedPlotInEditMode(BAR_PLOT_SAVE_NAME);
        assertSVG(SECOND_BAR_PLOT_SVG_TEXT);

        // set y-axis manual range, make sure we can use negative manual range values
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());
        lookAndFeelDialog.setYAxisRangeMinMax("-50", "-40").clickApply();
        assertSVG(THIRD_BAR_PLOT_SVG_TEXT);

        // set y-axis manual range max only, and make sure decimals are allowed
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());
        lookAndFeelDialog.setYAxisRangeMinMax(null, "3000.5").clickApply();
        assertSVG(FOURTH_BAR_PLOT_SVG_TEXT);

        savePlot(BAR_PLOT_SAVE_NAME_2, null, true);
    }
}
