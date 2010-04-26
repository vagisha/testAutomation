/*
 * Copyright (c) 2009-2010 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;

import java.io.File;
import java.io.FilenameFilter;

/*
* User: adam
* Date: Aug 17, 2009
* Time: 1:42:07 PM
*/

// Provides some helpful utilities used in study-related tests.  Subclasses provide all study creation and
// verification steps.
public abstract class StudyBaseTest extends BaseSeleniumWebTest
{
    abstract protected void doCreateSteps();

    abstract protected void doVerifySteps();

    public String getAssociatedModuleDirectory()
    {
        return "study";
    }

    protected static String getSampleDataPath()
    {
        return "/sampledata/study/";
    }

    protected String getPipelinePath()
    {
        return getLabKeyRoot() + getSampleDataPath();
    }

    protected String getProjectName()
    {
        return "StudyVerifyProject";
    }

    protected String getStudyLabel()
    {
        return "Study 001";
    }

    protected String getFolderName()
    {
        return "My Study";
    }

    protected void initializeFolder()
    {
        if (!isLinkPresentWithText(getProjectName()))
            createProject(getProjectName());
        createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", null, true);
    }

    protected final void doTestSteps()
    {
        doCreateSteps();
        doVerifySteps();
    }

    protected void doCleanup() throws Exception
    {
        try { deleteProject(getProjectName()); } catch (Throwable e) {}

        deleteLogFiles(".");
        deleteLogFiles("datasets");
        deleteDir(new File(getPipelinePath(), "assaydata"));
        deleteDir(new File(getPipelinePath(), "reports_temp"));
    }

    private void deleteLogFiles(String directoryName)
    {
        File dataRoot = new File(getPipelinePath() + directoryName);
        File[] logFiles = dataRoot.listFiles(new FilenameFilter(){
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".log");
            }
        });
        for (File f : logFiles)
            if (!f.delete())
                log("WARNING: couldn't delete log file " + f.getAbsolutePath());
    }

    protected void importStudy()
    {
        initializeFolder();
        initializePipeline();

        // Start importing study.xml to create the study and load all the datasets.  We'll wait for this import to
        // complete before doing any further tests.
        clickLinkWithText(getFolderName());
        clickNavButton("Process and Import Data");
        ExtHelper.waitForImportDataEnabled(this);
        ExtHelper.clickFileBrowserFileCheckbox(this, "study.xml");
        selectImportDataAction("Import Study");
    }

    private void initializePipeline()
    {
        clickLinkWithText("Folder Settings");
        toggleCheckboxByTitle("Pipeline");
        submit();
        addWebPart("Data Pipeline");
        clickNavButton("Setup");
        setPipelineRoot(getPipelinePath());
    }

    // Must be on study home page or "manage study" page
    protected void setDemographicsBit(String datasetName, boolean demographics)
    {
        clickLinkWithText("Manage Datasets");
        clickLinkWithText(datasetName);
        clickButtonContainingText("Edit Definition");
        waitForElement(Locator.name("description"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        if (demographics)
            checkCheckbox("demographicData");
        else
            uncheckCheckbox("demographicData");

        clickNavButton("Save");
    }

    // Must be on study home page or "manage study" page
    protected void setVisibleBit(String datasetName, boolean showByDefault)
    {
        clickLinkWithText("Manage Datasets");
        clickLinkWithText(datasetName);
        clickButtonContainingText("Edit Definition");
        waitForElement(Locator.name("description"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        if (showByDefault)
            checkCheckbox("showByDefault");
        else
            uncheckCheckbox("showByDefault");

        clickNavButton("Save");
    }

    protected void createReport(String reportType)
    {
        // click the create button dropdown
        String id = ExtHelper.getExtElementId(this, "btn_createView");
        click(Locator.id(id));

        id = ExtHelper.getExtElementId(this, reportType);
        click(Locator.id(id));
        waitForPageToLoad();
    }

    protected void selectOption(String name, int i, String value)
    {
        selectOptionByValue(Locator.tagWithName("select", name).index(i), value);
    }

    protected void assertSelectOption(String name, int i, String expected)
    {
        assertEquals(selenium.getSelectedValue(Locator.tagWithName("select", name).index(i).toString()), expected);
    }
}
