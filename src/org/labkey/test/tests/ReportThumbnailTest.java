/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
public class ReportThumbnailTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ReportThumbnailTest";
    private static final String PROJECT_NAME_ROUNDTRIP = "ReportThumbnailTest_Roundtrip";
    private static final File TEST_STUDY = TestFileUtils.getSampleData("studies/LabkeyDemoStudyWithCharts.folder.zip");
    private static final File TEST_THUMBNAIL = TestFileUtils.getSampleData("Microarray/test1.jpg");
    private static final File TEST_ICON = TestFileUtils.getSampleData("fileTypes/jpg_sample.jpg");
    private static final String BOX_PLOT = "Example Box Plot";
    private static final String SCATTER_PLOT = "Example Scatter Plot";
    private static final String R_PARTICIPANT_VIEWS = "R Participant Views: Physical Exam";
    private static final String R_REGRESSION_BP_ALL = "R Regression: Blood Pressure: All";
    private static final String R_REGRESSION_BP_MEANS = "R Regression: Blood Pressure: Means";
    private static final String CROSSTAB_REPORT = "Crosstab: Gender and Group Counts";
    private static final String ATTACHMENT_REPORT_1 = "Attachment Report 1";
    private static final String ATTACHMENT_REPORT_2 = "Attachment Report 2";
    private static final String ATTACHMENT_REPORT_3 = "Attachment Report 3";

    private String THUMBNAIL_DATA;
    private String ICON_DATA;

    private final RReportHelper _rReportHelper = new RReportHelper(this);

    //
    // expected values for roundtrip test
    //
    private String ICON_CUSTOM_DATA; // custom value same across all views
    private String ICON_PLOT_NONE_DATA; // stock icons differ per report type
    private String ICON_R_NONE_DATA;

    private String THUMBNAIL_R_AUTO_DATA; // autogenerated thumbnails specific to the report being viewed
    private String THUMBNAIL_CUSTOM_DATA; // custom thumbnail same across all views
    private String THUMBNAIL_R_NONE_DATA; // stock thumbnails differ per report type
    private String _currentProject = PROJECT_NAME;
    private int CORE_REV_NUM;

    @Override
    protected String getProjectName()
    {
        return _currentProject;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(PROJECT_NAME, afterTest);
        // besides the default project, the test also creates a project to verify
        // roundtrip (import/export) of thumbs
        deleteProject(PROJECT_NAME_ROUNDTRIP, afterTest);
    }

    @Test
    public void testSteps()
    {
        doSetup();
        doVerifySteps();
    }

    protected void doVerifySteps()
    {
        testGenericChartThumbnails();
        testCustomIcon();
        testRThumbnails();
        testThumbnailRoundtrip();
        testOfficeXmlThumbnails();
    }

    private void testOfficeXmlThumbnails()
    {
        final String ADD_REPORT_MENU = "Add Report";
        final String ATTACH_REPORT_OPTION = "Attachment Report";
        final String ATTACH_REPORT_NAME_ELMNT = "viewName";
        final String ATTACH_FILE_NAME_ELMNT = "filePath";
        final String ATTACH_TYPE_LABEL = "Attachment Type:";
        final String ATTACH_TYPE_SERVER_PATH = "Full file path on server";
        final String CLINIC_ASSAY_TAB = "Clinical and Assay Data";
        final String GENERATED_THUMBNAIL_NAME = "thumbnail.view";

        final String ATTACH_SAMPLE_DATA_PATH = TestFileUtils.getSampledataPath() + "/query/attachments/";

        goToDataViews();
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));

        // Create pptx attachment report
        _extHelper.clickExtMenuButton(true, Locator.linkContainingText(ADD_REPORT_MENU), ATTACH_REPORT_OPTION);
        setFormElement(Locator.name(ATTACH_REPORT_NAME_ELMNT), ATTACHMENT_REPORT_1);
        _ext4Helper.selectRadioButton(ATTACH_TYPE_LABEL, ATTACH_TYPE_SERVER_PATH);
        setFormElement(Locator.name(ATTACH_FILE_NAME_ELMNT), ATTACH_SAMPLE_DATA_PATH + "PowerPoint_JPEG_Thumbnail.pptx");
        clickButton("Save", defaultWaitForPage);

        // Create xlsx attachment report
        _extHelper.clickExtMenuButton(true, Locator.linkContainingText(ADD_REPORT_MENU), ATTACH_REPORT_OPTION);
        setFormElement(Locator.name(ATTACH_REPORT_NAME_ELMNT), ATTACHMENT_REPORT_2);
        _ext4Helper.selectRadioButton(ATTACH_TYPE_LABEL, ATTACH_TYPE_SERVER_PATH);
        setFormElement(Locator.name(ATTACH_FILE_NAME_ELMNT), ATTACH_SAMPLE_DATA_PATH + "Excel_Document_JPEG_Thumbnail.xlsx");
        clickButton("Save", defaultWaitForPage);

        // Create docx attachment report
        _extHelper.clickExtMenuButton(true, Locator.linkContainingText(ADD_REPORT_MENU), ATTACH_REPORT_OPTION);
        setFormElement(Locator.name(ATTACH_REPORT_NAME_ELMNT), ATTACHMENT_REPORT_3);
        _ext4Helper.selectRadioButton(ATTACH_TYPE_LABEL, ATTACH_TYPE_SERVER_PATH);
        setFormElement(Locator.name(ATTACH_FILE_NAME_ELMNT), ATTACH_SAMPLE_DATA_PATH + "Word_Document_JPEG_Thumbnail.docx");
        clickButton("Save", defaultWaitForPage);

        // Verify generated thumbnail images exist and not stock pptx, xlsx and docx images
        clickTab(CLINIC_ASSAY_TAB);
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        DataViewsTest.clickCustomizeView(ATTACHMENT_REPORT_1, this);
        _ext4Helper.clickExt4Tab("Images");
        waitForElement(Locator.xpath("//div[@class='thumbnail']"));
        assertElementPresent(Locator.xpath("//img[contains(@src,'" + GENERATED_THUMBNAIL_NAME + "')]"));
        assertElementNotPresent(Locator.xpath("//img[contains(@src,'presentation.png')]"));
        clickButton("Save", 0);
        _ext4Helper.waitForMaskToDisappear();

        clickTab(CLINIC_ASSAY_TAB);
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        DataViewsTest.clickCustomizeView(ATTACHMENT_REPORT_2, this);
        _ext4Helper.clickExt4Tab("Images");
        waitForElement(Locator.xpath("//div[@class='thumbnail']"));
        assertElementPresent(Locator.xpath("//img[contains(@src,'" + GENERATED_THUMBNAIL_NAME + "')]"));
        assertElementNotPresent(Locator.xpath("//img[contains(@src,'spreadsheet.png')]"));
        clickButton("Save", 0);
        _ext4Helper.waitForMaskToDisappear();

        clickTab(CLINIC_ASSAY_TAB);
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        DataViewsTest.clickCustomizeView(ATTACHMENT_REPORT_3, this);
        _ext4Helper.clickExt4Tab("Images");
        waitForElement(Locator.xpath("//div[@class='thumbnail']"));
        assertElementPresent(Locator.xpath("//img[contains(@src,'" + GENERATED_THUMBNAIL_NAME + "')]"));
        assertElementNotPresent(Locator.xpath("//img[contains(@src,'wordprocessing.png')]"));
        clickButton("Save", 0);
        _ext4Helper.waitForMaskToDisappear();
    }

    private void testGenericChartThumbnails()
    {
        goToDataViews();
        setThumbnailSRC(BOX_PLOT);
        toggleThumbnailType(BOX_PLOT, true);
        assertNewThumbnail(BOX_PLOT);
        assignCustomThumbnail(BOX_PLOT, TEST_THUMBNAIL, 1, 2);
        assertNewThumbnail(BOX_PLOT);

        goToDataViews();
        setThumbnailSRC(SCATTER_PLOT);
        toggleThumbnailType(SCATTER_PLOT, true);
        assertNewThumbnail(SCATTER_PLOT);
        assignCustomThumbnail(SCATTER_PLOT, TEST_THUMBNAIL, 1, 2);
        assertNewThumbnail(SCATTER_PLOT);
        THUMBNAIL_CUSTOM_DATA = THUMBNAIL_DATA;
    }

    private void testRThumbnails()
    {
        goToDataViews();
        setThumbnailSRC(R_REGRESSION_BP_ALL);
        THUMBNAIL_R_NONE_DATA = THUMBNAIL_DATA;

        goToDataViews();
        generateRThumbnail(R_PARTICIPANT_VIEWS);

        goToDataViews();
        setThumbnailSRC(R_PARTICIPANT_VIEWS);
        THUMBNAIL_R_AUTO_DATA = THUMBNAIL_DATA;

        goToDataViews();
        assignCustomThumbnail(R_REGRESSION_BP_MEANS, TEST_THUMBNAIL, CORE_REV_NUM, 1);
        verifyThumbnail(R_REGRESSION_BP_MEANS, THUMBNAIL_CUSTOM_DATA);

        goToDataViews();
        assignCustomThumbnail(CROSSTAB_REPORT, TEST_THUMBNAIL, CORE_REV_NUM, 1);
        verifyThumbnail(CROSSTAB_REPORT, THUMBNAIL_CUSTOM_DATA);

        // setup icons
        goToDataViews();
        setIconSRC(R_PARTICIPANT_VIEWS);
        ICON_R_NONE_DATA = ICON_DATA;

        goToDataViews();
        assignCustomIcon(R_REGRESSION_BP_ALL, TEST_ICON, 1);
        verifyIcon(R_REGRESSION_BP_ALL, ICON_CUSTOM_DATA);

        goToDataViews();
        assignCustomIcon(R_REGRESSION_BP_MEANS, TEST_ICON, 1);
        verifyIcon(R_REGRESSION_BP_MEANS, ICON_CUSTOM_DATA);
    }

    private void testCustomIcon()
    {
        goToDataViews();
        setIconSRC(BOX_PLOT);
        assignCustomIcon(BOX_PLOT, TEST_ICON, 1);
        assertNewIcon(BOX_PLOT);
        ICON_CUSTOM_DATA = ICON_DATA;

        goToDataViews();
        setIconSRC(SCATTER_PLOT);
        ICON_PLOT_NONE_DATA = ICON_DATA;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }

    @LogMethod
    protected void doSetup()
    {
        getCurrentCoreRevNumber();
        _rReportHelper.ensureRConfig();
        _containerHelper.createProject(PROJECT_NAME, "Study");
        importStudyFromZip(TEST_STUDY);
        goToDataViews();
    }

    protected void getCurrentCoreRevNumber()
    {
        String imgSrc = Locator.xpath("//td[contains(@class, 'labkey-main-icon')]//a//img").findElement(getDriver()).getAttribute("src");
        String rev = imgSrc.substring(imgSrc.lastIndexOf("&")+1);
        CORE_REV_NUM = Integer.parseInt(rev.split("=")[1]);
    }

    protected void goToDataViews()
    {
        clickFolder(_currentProject);
        waitAndClickAndWait(Locator.linkWithText("Clinical and Assay Data"));
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT); // Lots of stuff on this page. Can take a while to load.
    }

    @LogMethod
    protected void toggleThumbnailType(String chart, boolean custom)
    {
        clickAndWait(Locator.linkWithText(chart));
        waitForElement(Locator.css("svg"));
        clickButton("Edit");
        waitForElement(Locator.css("svg"));
        waitForTextToDisappear("loading data...");
        clickButton("Save", 0);
        if(!custom)
        {
            waitAndClick(Locator.xpath("//input[@type='button' and ../label[text()='None']]"));
        }
        else
        {
            waitAndClick(Locator.xpath("//input[@type='button' and ../label[text()='Auto-generate']]"));
        }

        _ext4Helper.clickWindowButton("Save", "Save", 0, 0);
        _extHelper.waitForExtDialog("Saved");
    }

    @LogMethod
    protected void generateRThumbnail(String report)
    {
        waitForElement(Locator.linkWithText(report));
        clickAndWait(Locator.linkWithText(report));
        _rReportHelper.clickSourceTab();
        prepForPageLoad();
        _rReportHelper.saveReport(null);
        waitForPageToLoad();
    }


    @LogMethod
    protected void setThumbnailSRC(String chart)
    {
        waitForElement(Locator.linkWithText(chart));
        mouseOver(Locator.linkWithText(chart));
        Locator.XPathLocator thumbnail = Locator.xpath("//div[@class='thumbnail']/img").notHidden();
        waitForElement(thumbnail);
        THUMBNAIL_DATA = WebTestHelper.getHttpResponse(getAttribute(thumbnail, "src")).getResponseBody();
    }

    @LogMethod
    protected void assertNewThumbnail(String chart)
    {
        verifyThumbnail(chart, null);
    }

    @LogMethod
    protected void verifyThumbnail(String chart, String expected)
    {
        goToDataViews();
        waitForElement(Locator.xpath("//a[text()='"+chart+"']"));
        mouseOver(Locator.xpath("//a[text()='"+chart+"']"));
        Locator.XPathLocator thumbnail = Locator.xpath("//div[@class='thumbnail']/img").notHidden();
        waitForElement(thumbnail);
        String thumbnailData;
        thumbnailData = WebTestHelper.getHttpResponse(getAttribute(thumbnail, "src")).getResponseBody();

        if (null == expected)
            assertFalse("Thumbnail was was still default", THUMBNAIL_DATA.equals(thumbnailData));
        else
            assertTrue("Thumbnail wasn't persisted correctly", expected.equals(thumbnailData) ||
                    StringUtils.getLevenshteinDistance(expected.substring(0, 5000), thumbnailData.substring(0, 5000)) <= 1); // Might be slightly different

        THUMBNAIL_DATA = thumbnailData;
    }

    @LogMethod
    protected void assignCustomThumbnail(String chart, File thumbnail, int currentRevNum, int nextRevNum)
    {
        // NOTE: the checkRevisionNumber here makes an assumption about this being a new thumbnail with no previous thumbnails.
        goToDataViews();
        waitAndClick(Locator.xpath("//span[@title='Edit']"));
        DataViewsTest.clickCustomizeView(chart, this);
        waitForElement(Locator.name("viewName"));
        _ext4Helper.clickExt4Tab("Images");
        waitForElement(Locator.id("customThumbnail"));
        assertTrue("Thumbnail Revision number is not correct", checkRevisionNumber(Locator.xpath("//div[contains(@class, 'thumbnail')]//img"), currentRevNum));
        setFormElement(Locator.xpath("//input[@id='customThumbnail-button-fileInputEl']"), thumbnail);
        _ext4Helper.clickWindowButton(chart, "Save", 0, 0);
        waitForTextToDisappear("Saving...");
        // go back and check revision number real quick
        DataViewsTest.clickCustomizeView(chart, this);
        waitForElement(Locator.name("viewName"));
        _ext4Helper.clickExt4Tab("Images");
        waitForElement(Locator.id("customThumbnail"));
        assertTrue("Thumbnail Revision number is not correct", checkRevisionNumber(Locator.xpath("//div[contains(@class, 'thumbnail')]//img"), nextRevNum));
        _ext4Helper.clickWindowButton(chart, "Save", 0, 0);
        waitForTextToDisappear("Saving...");
    }

    protected boolean checkRevisionNumber(Locator loc, int num)
    {
        waitForElement(loc);
        String uri = loc.findElement(getDriver()).getAttribute("src");

        String query;
        try
        {
            query = new URL(uri).getQuery();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }

        //if (query.contains("&"))
        //{
        for (String param : query.split("&"))
        {
            String[] data = param.split("=");
            if(data[0].equals("revision"))
                return (num == Integer.parseInt(data[1]));
        }
        //}
        /*else {
            String[] data = query.split("=");
            if(data[0].equals("revision"))
                return (num == Integer.parseInt(data[1]));
        }*/
        return false;
    }

    @LogMethod
    protected void setIconSRC(String chart)
    {
        waitForElement(Locator.linkWithText(chart));
        waitAndClick(Locator.xpath("//span[@title='Edit']"));
        DataViewsTest.clickCustomizeView(chart, this);
        waitForElement(Locator.name("viewName"));
        ICON_DATA = getIconDataFromPropertiesPanel();
        _ext4Helper.clickWindowButton(chart, "Save", 0, 0);
        waitForTextToDisappear("Saving...");
    }

    @LogMethod
    protected void assertNewIcon(String chart)
    {
        verifyIcon(chart, null);
    }

    @LogMethod
    protected void verifyIcon(String chart, String expected)
    {
        goToDataViews();
        waitForElement(Locator.xpath("//a[text()='"+chart+"']"));
        waitAndClick(Locator.xpath("//span[@title='Edit']"));
        DataViewsTest.clickCustomizeView(chart, this);
        waitForElement(Locator.name("viewName"));
        String iconData = getIconDataFromPropertiesPanel();

        if (null == expected)
            assertFalse("Icon was still default", ICON_DATA.equals(iconData));
        else
            assertEquals("Unexpected icon", expected, iconData);

        ICON_DATA = iconData;
        _ext4Helper.clickWindowButton(chart, "Save", 0, 0);
        waitForTextToDisappear("Saving...");
    }

    private String getIconDataFromPropertiesPanel()
    {
        _ext4Helper.clickExt4Tab("Images");
        WebElement iconRow = waitForElement(Ext4Helper.Locators.formItemWithLabel("Icon").notHidden());
        try
        {
            String iconSrc = waitForElement(Locator.xpath("//div[@class=\"icon\"]/img").notHidden()).getAttribute("src");
            return WebTestHelper.getHttpResponse(iconSrc).getResponseBody();
        }
        catch (NoSuchElementException fontIcon) // font-awesome icon
        {
            return Locator.css("div.x4-form-display-field span").findElement(iconRow).getAttribute("class"); // e.g. "fa fa-sliders fa-rotate-90"
        }
    }

    @LogMethod
    protected void assignCustomIcon(String chart, File icon, int customRevNum)
    {
        goToDataViews();
        waitAndClick(Locator.xpath("//span[@title='Edit']"));
        DataViewsTest.clickCustomizeView(chart, this);
        waitForElement(Locator.name("viewName"));
        _ext4Helper.clickExt4Tab("Images");
        waitForElement(Locator.id("customIcon"));
        setFormElement(Locator.xpath("//input[@id='customIcon-button-fileInputEl']"), icon);
        _ext4Helper.clickWindowButton(chart, "Save", 0, 0);
        waitForTextToDisappear("Saving...");
        // go back and check revision number real quick
        DataViewsTest.clickCustomizeView(chart, this);
        waitForElement(Locator.name("viewName"));
        _ext4Helper.clickExt4Tab("Images");
        waitForElement(Locator.id("customIcon"));
        assertTrue("Icon Revision number is not correct", checkRevisionNumber(Locator.xpath("//div[contains(@class, 'icon')]//img"), customRevNum));
        _ext4Helper.clickWindowButton(chart, "Save", 0, 0);
        waitForTextToDisappear("Saving...");
    }

    /*@LogMethod
    protected File getExportFolderZip()
    {
        goToProjectHome();
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Export"));
        clickButton("Export", 0);

        waitFor(new BaseWebDriverTest.Checker() {
            @Override
            public boolean check()
            {
                File[] exportFiles = getDownloadDir().listFiles();
                return (exportFiles != null && exportFiles.length > 0);
            }
        }, "failed to export study", WAIT_FOR_JAVASCRIPT);

        File[] exportFiles = getDownloadDir().listFiles();
        return exportFiles[0];
    }*/

    @LogMethod
    protected void importFolder(File importZip)
    {
        _containerHelper.createProject(PROJECT_NAME_ROUNDTRIP, "Study");
        _currentProject = PROJECT_NAME_ROUNDTRIP;
        importStudyFromZip(importZip);
    }

    @LogMethod
    protected void testThumbnailRoundtrip()
    {
        // export
        File exportZip = exportFolderToBrowserAsZip();
        importFolder(exportZip);

        // BOX_PLOT has a custom icon and custom thumbnail
        verifyIcon(BOX_PLOT, ICON_CUSTOM_DATA);
        verifyThumbnail(BOX_PLOT, THUMBNAIL_CUSTOM_DATA);

        // SCATTER_PLOT has no icon and custom thumbnail
        verifyIcon(SCATTER_PLOT, ICON_PLOT_NONE_DATA);
        verifyThumbnail(SCATTER_PLOT, THUMBNAIL_CUSTOM_DATA);

        // R_PARTICIPANT_VIEWS has no icon, auto thumbnail
        verifyIcon(R_PARTICIPANT_VIEWS, ICON_R_NONE_DATA);
        verifyThumbnail(R_PARTICIPANT_VIEWS, THUMBNAIL_R_AUTO_DATA);

        // R_REGRESSION_BP_ALL has custom icon, no thumbnail
        verifyIcon(R_REGRESSION_BP_ALL, ICON_CUSTOM_DATA);
        verifyThumbnail(R_REGRESSION_BP_ALL, THUMBNAIL_R_NONE_DATA);

        // R_REGRESSION_BP_MEANS has custom icon, custom thumbnail
        verifyIcon(R_REGRESSION_BP_MEANS, ICON_CUSTOM_DATA);
        verifyThumbnail(R_REGRESSION_BP_MEANS, THUMBNAIL_CUSTOM_DATA);
    }
}
