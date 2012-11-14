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
package org.labkey.test.tests;

import junit.framework.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;

import java.io.File;

/**
 * User: jeckels
 * Date: June 25, 2012
 *
 * Imports a SampleMinded data export (.xlsx) into the specimen repository.
 */
public class SampleMindedImportTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleMindedImportTest";

    private static final String FILE = "SampleMindedExport.xlsx";

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        File specimenDir = new File(getLabKeyRoot() + "/sampledata/study/specimens");
        File specimenArchive = new File(specimenDir, "SampleMindedExport.specimens");
        specimenArchive.delete();

        for (File file : specimenDir.listFiles())
        {
            if (file.getName().startsWith(FILE) && file.getName().endsWith(".log"))
            {
                file.delete();
            }
        }

        // Now delete the project
        super.doCleanup(afterTest);
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        _containerHelper.createProject(PROJECT_NAME, "Study");
        clickButton("Create Study");
        setFormElement(Locator.name("startDate"),"2011-01-01");
        click(Locator.radioButtonByNameAndValue("simpleRepository", "true"));
        clickButton("Create Study");

        clickLinkWithText("manage visits");
        clickLinkWithText("create new visit");
        setFormElement(Locator.name("label"),"Visit SE");
        setFormElement(Locator.name("sequenceNumMin"),"999.0000");
        setFormElement(Locator.name("sequenceNumMax"),"999.9999");
        selectOptionByValue(Locator.name("sequenceNumHandling"),"logUniqueByDate");
        clickLinkWithText("save");

        // "overview" is a dumb place for this link
        clickLinkWithText("Overview");
        clickLinkWithText("manage files");
        setPipelineRoot(getLabKeyRoot() + "/sampledata/study");
        clickLinkWithText(PROJECT_NAME + " Study");
        clickLinkWithText("Manage Files");

        clickButton("Process and Import Data");
        _extHelper.selectFileBrowserItem("specimens/" + FILE);
        selectImportDataActionNoWaitForGrid("Import Specimen Data");
        clickButton("Start Import");
        waitForPipelineJobsToComplete(1, "Import specimens: SampleMindedExport.xlsx", false);
        clickTab("Specimen Data");
        waitForElement(Locator.linkWithText("BAL"));
        assertLinkPresentWithText("BAL");
        assertLinkPresentWithText("Blood");
        clickLinkWithText("By Individual Vial");
        assertLinkPresentWithTextCount("P1000001", 6);
        assertLinkPresentWithTextCount("P2000001", 3);
        assertLinkPresentWithTextCount("P20043001", 5);
        assertTextPresent("20045467");
        assertTextPresent("45627879");
        assertTextPresent("1000001-21");

        clickTab("Specimen Data");
        waitForElement(Locator.linkWithText("NewSpecimenType"));
        clickLinkWithText("NewSpecimenType");
        assertTextPresent("EARL (003)");
        assertTextPresent("REF-A Cytoplasm Beaker");

        clickTab("Specimen Data");
        waitForElement(Locator.linkWithText("BAL"));
        clickLinkWithText("BAL");
        assertTextPresent("BAL Supernatant");
        assertTextPresent("FREE (007)");
        DataRegionTable specimenTable = new DataRegionTable("SpecimenDetail", this, true, true);
        Assert.assertEquals("Incorrect number of vials.", "Count:  5", specimenTable.getTotal("Global Unique Id"));

        clickLinkWithText("Group vials");
        assertLinkPresentWithTextCount("P20043001", 2);
        assertTextPresent("Visit SE");

        // add column sequencenum
        new CustomizeViewsHelper(this).openCustomizeViewPanel();
        new CustomizeViewsHelper(this).showHiddenItems();
        new CustomizeViewsHelper(this).addCustomizeViewColumn("SequenceNum");
        new CustomizeViewsHelper(this).applyCustomView();
        assertTextPresent("999.0138");
    }
}
