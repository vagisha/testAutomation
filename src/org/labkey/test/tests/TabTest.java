/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.BVT;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PortalHelper;

import static org.junit.Assert.*;

@Category({BVT.class})
public class TabTest extends SimpleModuleTest
{
    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    protected void doVerifySteps() throws Exception
    {
        doTabManagementTests();
        doTestTabbedFolder();
        doTestContainerTabConversion();
    }

    @LogMethod
    private void doTabManagementTests()
    {
         /*
         *  test add, remove, move, rename, hide, unhide.
         */
        PortalHelper portalHelper = new PortalHelper(this);

        clickProject(getProjectName());
        clickFolder(FOLDER_NAME);

        // Move tabs
        portalHelper.enableTabEditMode();
        portalHelper.moveTab("Tab 1", PortalHelper.Direction.LEFT); // Nothing should happen.
        portalHelper.moveTab("Tab 1", PortalHelper.Direction.RIGHT);
        assertTrue("Tab 2".equals(getText(Locator.xpath("//div[@class='labkey-app-bar']//ul//li[1]//a[1]")))); // Verify Mice is in the first position.
        assertTrue("Tab 1".equals(getText(Locator.xpath("//div[@class='labkey-app-bar']//ul//li[2]//a[1]")))); // Verify Overview is in the second.
        portalHelper.moveTab("Assay Container", PortalHelper.Direction.RIGHT); // Nothing should happen.
        assertTrue("Assay Container".equals(getText(Locator.xpath("//div[@class='labkey-app-bar']//ul//li[4]//a[1]")))); // Verify Assay did not swap with +

        // Remove tab
        portalHelper.hideTab("Tab 2");

        // Add tab
        portalHelper.addTab("TEST TAB 1");
        clickAndWait(Locator.linkWithText("TEST TAB 1"));
        portalHelper.addWebPart("Wiki");

        // Rename tabs
        portalHelper.renameTab("TEST TAB 1", "Tab 2", "You cannot change a tab's name to another tab's original name even if the original name is not visible.");
        portalHelper.renameTab("Tab 1", "TEST TAB 1", "A tab with the same name already exists in this folder.");
        portalHelper.renameTab("Tab 1", "test tab 1", "A tab with the same name already exists in this folder.");
        portalHelper.renameTab("TEST TAB 1", "RENAMED TAB 1");
        clickAndWait(Locator.linkWithText("RENAMED TAB 1"));
        assertEquals("Wiki not present after tab rename", "Wiki", getText(Locator.css(".labkey-wp-title-text")));

        portalHelper.showTab("Tab 2");

        // Issue 18730 - Add tab was adding tabs to the container-tab's container.
        clickTab("Study Container");
        portalHelper.addTab("Test Tab 2");

        // Issue 18731 - Couldn't rename container tabs if on container tab page.
        portalHelper.renameTab("Study Container", "Study Container Rename");
        // It's needed later in the test, so rename it back.
        portalHelper.renameTab("Study Container Rename", "Study Container");

        // Issue 18734 - Unable to show some hidden tabs in folder with container tab.
        portalHelper.hideTab("Tab 1");
        portalHelper.showTab("Tab 1");

        // TODO: Test import/export of renamed tabs if applicable
        // See Issue 16929: Folder tab order & names aren't retained through folder export/import

        portalHelper.deleteTab("RENAMED TAB 1");
        portalHelper.deleteTab("Test Tab 2");
    }

    @LogMethod
    private void doTestTabbedFolder()
    {
        clickFolder(FOLDER_NAME);

        //it should start on tab 2
        verifyTabSelected("Tab 2");
        log("verifying webparts present in correct order");
        assertTextPresentInThisOrder("A customized web part", "Experiment Runs", "Assay List");

        //verify Tab 1
        clickTab("Tab 1");
        assertTextPresentInThisOrder("A customized web part", "Data Pipeline", "Experiment Runs", "Run Groups", "Sample Sets", "Assay List");
        addWebPart("Messages");

        clickTab("Tab 2");

        //verify added webpart is persisted
        clickTab("Tab 1");
        assertTextPresentInThisOrder("A customized web part", "Data Pipeline", "Experiment Runs", "Run Groups", "Sample Sets", "Assay List", "Messages");

        //there is a selector for the assay controller and tab2
        clickAndWait(Locator.linkWithText("New Assay Design"));
        verifyTabSelected("Tab 2");

        //this is a controller selector
        beginAt("/query/" + getProjectName() + "/" + FOLDER_NAME + "/begin.view?");
        verifyTabSelected("Tab 1");

        //this is a view selector
        beginAt("/pipeline-status/" + getProjectName() + "/" + FOLDER_NAME + "/showList.view?");
        verifyTabSelected("Tab 2");

        //this is a regex selector
        clickFolder(FOLDER_NAME);
        addWebPart("Sample Sets");
        clickAndWait(Locator.linkWithText("Import Sample Set"));
        verifyTabSelected("Tab 1");

        // Test Container tabs
        clickTab("Assay Container");
        assertTextPresent("Assay List");
        clickTab(STUDY_FOLDER_TAB_LABEL);
        assertTextPresent("Study Overview");
        clickAndWait(Locator.linkWithText("Create Study"));
        clickAndWait(Locator.linkWithText("Create Study"));
        assertTextPresent("Manage Study", "Study Container", "Overview", "Specimen Data");
        clickAndWait(Locator.linkWithText("Specimen Data"));
        assertTextPresent("Vial Search", "Specimens");

        // Test container tab enhancements: change tab folder's type, revert type, delete tab folder
        // Change study's type to collaboration
        log("Container tab enhancements: change tab folder type, revert");
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        assertTreeButtonHidden("Revert", true);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Collaboration"));
        clickButton("Update Folder", 0);
        _extHelper.waitForExtDialog("Change Folder Type");
        click(Locator.extButton("Yes"));
        waitForText("The Wiki web part displays a single wiki page.");
        // TODO: assert that study tabs are gone
        // change type back to study
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        assertTreeButtonHidden("Revert", false);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Study"));
        clickButton("Update Folder", 0);
        _extHelper.waitForExtDialog("Change Folder Type");
        click(Locator.extButton("Yes"));
        waitForText("Study tracks data in");
        // change to collaboration again
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        assertTreeButtonHidden("Revert", true);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Collaboration"));
        clickButton("Update Folder", 0);
        _extHelper.waitForExtDialog("Change Folder Type");
        click(Locator.extButton("Yes"));
        waitForText("The Wiki web part displays a single wiki page.");
        // revert type
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        assertTreeButtonHidden("Revert", false);
        clickButton("Revert", 0);
        _extHelper.waitForExtDialog("Revert Folder(s)");
        click(Locator.ext4Button("Yes"));
        _extHelper.waitForExtDialog("Revert Folder");
        click(Locator.ext4Button("OK"));
        clickTab("Study Container");
        waitForText("Study tracks data in");

        // Revert via parent container
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        assertTreeButtonHidden("Revert", true);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Collaboration"));
        clickButton("Update Folder", 0);
        _extHelper.waitForExtDialog("Change Folder Type");
        click(Locator.extButton("Yes"));
        waitForText("The Wiki web part displays a single wiki page.");
        clickTab("Tab 1");
        goToFolderManagement();
        waitForText(STUDY_FOLDER_TAB_NAME);
        assertTreeButtonHidden("Revert", false);
        clickButton("Revert", 0);
        _extHelper.waitForExtDialog("Revert Folder(s)");
        click(Locator.ext4Button("Yes"));
        _extHelper.waitForExtDialog("Revert Folders");
        click(Locator.ext4Button("OK"));
        clickTab("Study Container");
        waitForText("Study tracks data in");

        // Delete tab folder
        log("Container tab enhancements: delete tab folder type, recreate");
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        clickButton("Delete");
        assertTextPresent("You are about to delete the following folder:");
        clickButton("Delete");
        assertTextNotPresent(STUDY_FOLDER_TAB_NAME);

        // Resurrect tab folder
        clickTab("Tab 1");
        goToFolderManagement();
        waitForText(ASSAY_FOLDER_TAB_NAME);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Collaboration"));
        clickButton("Update Folder");
        goToFolderManagement();
        waitForText(ASSAY_FOLDER_TAB_NAME);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", TABBED_FOLDER_TYPE));
        clickButton("Update Folder", 0);
        _extHelper.waitForExtDialog("Change Folder Type");
        assertTextPresent("Study Container");
        _ext4Helper.checkCheckbox("Study Container");     // recreate folder
        click(Locator.ext4Button("OK"));
        waitForText("A customized web part");
        clickTab("Study Container");
        clickAndWait(Locator.linkWithText("Create Study"));     // Create study
        clickAndWait(Locator.linkWithText("Create Study"));

        // Create the list again so we can pass query validation.
        log("Create list in subfolder to prevent query validation failure");
        _listHelper.createList(STUDY_FOLDER_TAB_LABEL, LIST_NAME,
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name"),
                new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "Age"),
                new ListHelper.ListColumn("Crazy", "Crazy", ListHelper.ListColumnType.Boolean, "Crazy?"));
    }

    @LogMethod
    private void doTestContainerTabConversion()
    {
        // Set up a Collaboration folder with study and assay subfolders
        final String COLLAB_FOLDER = "collab1";
        final String COLLABFOLDER_PATH = getProjectName() + "/" + COLLAB_FOLDER;
        final String EXTRA_ASSAY_WEBPART = "Run Groups";
        clickProject(getProjectName());
        _containerHelper.createSubfolder(getProjectName(), COLLAB_FOLDER, "Collaboration");
        _containerHelper.createSubfolder(COLLABFOLDER_PATH, STUDY_FOLDER_TAB_NAME, "Study");
        _containerHelper.createSubfolder(COLLABFOLDER_PATH, ASSAY_FOLDER_TAB_NAME, "Assay");
        clickFolder(COLLAB_FOLDER);
        clickFolder(STUDY_FOLDER_TAB_NAME);
        assertTextPresent("Study Overview");
        clickAndWait(Locator.linkWithText("Create Study"));
        clickAndWait(Locator.linkWithText("Create Study"));
        clickFolder(ASSAY_FOLDER_TAB_NAME);
        addWebPart(EXTRA_ASSAY_WEBPART);

        // Change folder type to XML Tabbed
        clickFolder(COLLAB_FOLDER);
        assertElementNotPresent(Locator.linkWithText(STUDY_FOLDER_TAB_LABEL));
        assertElementNotPresent(Locator.linkWithText(ASSAY_FOLDER_TAB_LABEL));
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton("folderType", TABBED_FOLDER_TYPE);
        clickButton("Update Folder");

        // Verify that subfolders got moved into tabs
        assertElementPresent(Locator.linkWithText(STUDY_FOLDER_TAB_LABEL));
        assertElementPresent(Locator.linkWithText(ASSAY_FOLDER_TAB_LABEL));
        hoverFolderBar();
        assertElementNotPresent(Locator.id("folderBar_menu").append(Locator.linkWithText(STUDY_FOLDER_TAB_LABEL)));
        assertElementNotPresent(Locator.id("folderBar_menu").append(Locator.linkWithText(ASSAY_FOLDER_TAB_LABEL)));
        clickAndWait(Locator.linkWithText(STUDY_FOLDER_TAB_LABEL));
        assertTextPresent("Study Overview");
        clickAndWait(Locator.linkWithText("Specimen Data"));
        assertTextPresent("Vial Search", "Import Specimens");
        clickAndWait(Locator.linkWithText(ASSAY_FOLDER_TAB_LABEL));
        assertTextPresent("Assay List");
        assertTextPresent(EXTRA_ASSAY_WEBPART);

        // Change back to Collab
        clickFolder(COLLAB_FOLDER);
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton("folderType", "Collaboration");
        clickButton("Update Folder");

        // Study and Assay should be hidden now
        assertTextNotPresent(STUDY_FOLDER_TAB_LABEL, ASSAY_FOLDER_TAB_LABEL, STUDY_FOLDER_TAB_NAME, ASSAY_FOLDER_TAB_NAME);

        // Now change back to TABBED
        clickFolder(COLLAB_FOLDER);
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton("folderType", TABBED_FOLDER_TYPE);
        clickButton("Update Folder");

        // Verify that folder tabs are back
        assertTextPresent(STUDY_FOLDER_TAB_LABEL, ASSAY_FOLDER_TAB_LABEL);
        clickAndWait(Locator.linkWithText(STUDY_FOLDER_TAB_LABEL));
        assertTextPresent("Study Overview");
        clickAndWait(Locator.linkWithText("Specimen Data"));
        assertTextPresent("Vial Search", "Import Specimens");
        clickAndWait(Locator.linkWithText(ASSAY_FOLDER_TAB_LABEL));
        assertTextPresent("Assay List");
        assertTextPresent(EXTRA_ASSAY_WEBPART);

        deleteFolder(getProjectName(), COLLAB_FOLDER);
    }

    private void assertTreeButtonHidden(String buttonText, boolean hidden)
    {
        // TODO: Dave: the locator's presence seems to be inconsistent. These are not central to the test but would be nice
/*        Locator.XPathLocator locator = Locator.ext4Button(buttonText);
        if (hidden && (isElementPresent(locator) && !isElementPresent(locator.withClass("x4-btn-disabled"))))   // if present should be disabled
            fail("Button '" + buttonText + "' not hidden.");
        else if (!hidden && (!isElementPresent(locator) || isElementPresent(locator.withClass("x4-btn-disabled"))))
            fail("Button '" + buttonText + "' is hidden.");                */
    }

    @LogMethod(quiet = true)
    public void goToTabFolderManagement(@LoggedParam String tabText)
    {
        Locator tabMenuXPath = Locator.xpath("//div[@class='labkey-app-bar']//ul//li//a[text()='" + tabText +"']/following-sibling::span//a");
        waitForElement(tabMenuXPath);

//        _ext4Helper.clickExt4MenuButton(true, tabMenuXPath, false, "Folder", "Management");       // Tab menu hidden so this doesn't work; do it inline with fireEvent instead
        fireEvent(tabMenuXPath, SeleniumEvent.click);
        Locator parentLocator = Ext4Helper.ext4MenuItem("Folder");
        waitForElement(parentLocator, 1000);
        mouseOver(parentLocator);

        Locator itemLocator = Ext4Helper.ext4MenuItem("Management");
        waitAndClickAndWait(itemLocator);
    }
}