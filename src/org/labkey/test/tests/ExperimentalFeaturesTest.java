/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DevModeOnlyTest;
import org.labkey.test.util.ListHelper;

import java.util.HashMap;

/**
 * User: tchadick
 * Date: 9/26/12
 * Time: 1:37 PM
 */

public class ExperimentalFeaturesTest extends BaseWebDriverTest implements DevModeOnlyTest
{
    private static final String TEST_GROUP = "HiddenEmail Test group";
    private static final String ADMIN_USER = "experimental_admin@experimental.test";
    private static final String IMPERSONATED_USER = "experimental_user@experimental.test";
    private static final String CHECKED_USER = "experimental_user2@experimental.test";
    private static final String EMAIL_TEST_LIST = "My Users";
    private static final String EMAIL_VIEW = "emailView";


    @Override
    protected String getProjectName()
    {
        return "Experimental Features Test";
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);

        deleteUsers(afterTest, IMPERSONATED_USER, CHECKED_USER, ADMIN_USER);
        try{deleteGroup(TEST_GROUP);}catch(Throwable t){/**/}
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    protected void doTestSteps() throws Exception
    {

        doHiddenEmailTest();
    }

    private void doHiddenEmailTest()
    {
        setupHiddenEmailTest();
        verifyHiddenEmailTest();

    }
    private void setupHiddenEmailTest()
    {
        // Create users and groups
        createUser(ADMIN_USER, null);
        addUserToGroup("Site Administrators", ADMIN_USER);
        impersonate(ADMIN_USER); // Use created user to ensure we have a known 'Modified by' column for created users
        createGlobalPermissionsGroup(TEST_GROUP, IMPERSONATED_USER, CHECKED_USER);
        _containerHelper.createProject(getProjectName(), null);
        setSiteGroupPermissions(TEST_GROUP, "Reader");
        clickButton("Save and Finish");
        stopImpersonating();
        impersonate(CHECKED_USER);
        goToMyAccount();
        clickButton("Edit");
        setFormElement(Locator.name("quf_FirstName"), displayNameFromEmail(CHECKED_USER));
        clickButton("Submit");
        stopImpersonating();

        // Create list
        // TODO: possibly insert as ADMIN_USER if list's ModifiedBy row should be hidden (it is not)
        ListHelper.ListColumn userColumn = new ListHelper.ListColumn("user", "user", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(getProjectName(), "core", "Users"));
        _listHelper.createList(getProjectName(), EMAIL_TEST_LIST, ListHelper.ListColumnType.AutoInteger, "Key", userColumn);
        clickButton("Done");
        clickLinkWithText(EMAIL_TEST_LIST);
        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_user"), displayNameFromEmail(CHECKED_USER));
        clickButton("Submit");
        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_user"), displayNameFromEmail(ADMIN_USER));
        clickButton("Submit");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("user/Email", "Email");
        _customizeViewsHelper.addCustomizeViewColumn("user/ModifiedBy/Email", "Email");
        _customizeViewsHelper.addCustomizeViewColumn("ModifiedBy/Email", "Email");
        _customizeViewsHelper.saveCustomView(EMAIL_VIEW, true);

        // Create query webpart
        clickFolder(getProjectName());
        addWebPart("Query");
        selectOptionByValue(Locator.name("schemaName"), "core");
        clickRadioButtonById("selectQueryContents");
        selectOptionByValue(Locator.name("queryName"), "Users");
        submit();
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("ModifiedBy/Email", "Email");
        _customizeViewsHelper.saveCustomView(EMAIL_VIEW, true);

        // Enable limited email visibility
        setExperimentalFeature(ExperimentalFeature.HIDDEN_EMAIL, true);

        // Verify user permissions for hidden emails
        goToSiteGroups();
        _ext4Helper.clickExt4Tab("Permissions");
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        assertElementNotPresent(Locator.permissionButton(TEST_GROUP, "SeeEmailAddresses"));
        assertElementNotPresent(Locator.permissionButton(IMPERSONATED_USER, "SeeEmailAddresses"));
        clickButton("Save and Finish");
    }

    private void verifyHiddenEmailTest()
    {
        impersonate(IMPERSONATED_USER);
        clickFolder(getProjectName());

        log("Verify that emails cannot be seen in query webpart");
        clickMenuButton("Views", EMAIL_VIEW);
        assertTextNotPresent(CHECKED_USER, ADMIN_USER);

        log("Verify that emails cannot be seen in list via lookup");
        clickLinkWithText(EMAIL_TEST_LIST);
        clickMenuButton("Views", EMAIL_VIEW);
        assertTextNotPresent(CHECKED_USER, ADMIN_USER);

        stopImpersonating();
    }

    private void setExperimentalFeature(ExperimentalFeature feature, boolean enable)
    {
        goToExperimentalFeatures();
        Locator.XPathLocator toggleLink = Locator.xpath("id('labkey-experimental-feature-" + feature + "')");

        if (enable && "Enable".equals(getText(toggleLink)))
        {
            if(!_initialFeatureStates.containsKey(feature))
            {
                _initialFeatureStates.put(feature, false);
            }
            click(toggleLink);
            waitForElement(toggleLink.append("[text()='Disable']"));
        }
        if (!enable && "Disable".equals(getText(toggleLink)))
        {
            if(!_initialFeatureStates.containsKey(feature))
            {
                _initialFeatureStates.put(feature, false);
            }
            click(toggleLink);
            waitForElement(toggleLink.append("[text()='Enable']"));
        }
    }

    private void goToExperimentalFeatures()
    {
        if (!selenium.getTitle().equals("Experimental Features"))
        {
            goToAdminConsole();
            clickLinkWithText("experimental features");
        }
    }

    HashMap<ExperimentalFeature, Boolean> _initialFeatureStates = new HashMap<ExperimentalFeature, Boolean>();
    private enum ExperimentalFeature
    {
        JS_DOC ("jsdoc"),
        DETAILS_URL ("details-url"),
        CONTAINER_REL_URLS ("containerRelativeURL"),
        JS_MOTHERSHITP ("javascriptMothership"),
        HIDDEN_EMAIL ("permissionToSeeEmailAddresses"),
        ISSUES_ACTIVITY ("issuesactivity");

        private final String title;
        private ExperimentalFeature (String title)
        {this.title = title;}
        public String toString()
        {return title;}
    }

    public void tearDown() throws Exception
    {
        for (ExperimentalFeature feature : ExperimentalFeature.values())
        {
            if (_initialFeatureStates.containsKey(feature))
                setExperimentalFeature(feature, _initialFeatureStates.get(feature));
        }
        super.tearDown();
    }
}
