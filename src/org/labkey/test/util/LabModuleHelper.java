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
package org.labkey.test.util;

import junit.framework.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import java.net.URISyntaxException;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 5/28/12
 * Time: 8:56 PM
 */
public class LabModuleHelper
{
    private BaseWebDriverTest _test;
    private final Random _random = new Random(System.currentTimeMillis());

    public LabModuleHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void defineAssay(String provider, String label)
    {
        _test.log("Defining a test assay at the project level");
        //define a new assay at the project level
        //the pipeline must already be setup
        _test.goToProjectHome();

        //copied from old test
        _test.goToManageAssays();
        _test.clickButton("New Assay Design");
        _test.checkRadioButton("providerName", provider);
        _test.clickButton("Next");

        Locator l = Locator.xpath("//input[@id='AssayDesignerName']");
        _test.waitForElement(l, _test.WAIT_FOR_JAVASCRIPT);

        //why is this so flaky??
        _test.setFormElement(l, label);
        _test.sleep(500);
        _test.setFormElement(l, label);

        _test.sleep(500);
        _test.clickButton("Save", 0);
        _test.waitForText("Save successful.", 20000);
        _test.assertTextNotPresent("Unknown");
    }

    public static Locator getNavPanelItem(String label, String itemText)
    {
        //NOTE: this should return only visible items
        return Locator.xpath("//span[text() = '" + label + "']/../../../div[not(contains(@style, 'display: none'))]//a[text() = '" + itemText + "']");
    }

    public void clickNavPanelItem(String label, String itemText)
    {
        Locator l = getNavPanelItem(label, itemText);
        _test.waitForElement(l);
        _test.waitAndClick(l);
    }

    public void clickNavPanelItem(String itemText)
    {
        Locator l = Locator.xpath("//a[contains(text(), '" + itemText + "')]");
        _test.waitForElement(l);
        _test.waitAndClick(l);
    }

    public static Locator getNavPanelRow(String label)
    {
        return Locator.xpath("//div[descendant::span[text() = '" + label + "']]");
    }

    public void goToLabHome()
    {
        _test.goToProjectHome();
        _test.waitForText("Types of Data:");
    }

    public void verifyNavPanelRowItemPresent(String label)
    {
        _test.log("Verifying NavPanel row present with label: " + label);
        Assert.assertTrue("Row missing: " + label, _test.isElementPresent(getNavPanelRow(label)));
    }

    public static Locator webpartTitle(String title)
    {
        return Locator.xpath("//span[contains(@class, 'labkey-wp-title-text') and text() = '" + title + "']");
    }

    public String createWorkbook(String workbookTitle, String workbookDescription)
    {
        _test.clickTab("Workbooks");
        _test.clickButton("Create New Workbook", 0);
        _test.waitForElement(Ext4Helper.ext4Window("Create Workbook"));
        _test.setFormElement(Locator.name("title"), workbookTitle);
        _test.setFormElement(Locator.name("description"), workbookDescription);
        _test.clickButton("Submit");
        _test.waitForPageToLoad();

        try
        {
            String path = _test.getURL().toURI().getPath();
            path = path.replaceAll(".*/workbook-", "");
            path = path.replaceAll("/begin.view", "");
            return path;
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }

    public int getRandomInt()
    {
        return _random.nextInt(10000);
    }

    public String getNameForQueryWebpart(String title)
    {
        Locator l = Locator.xpath("//table[@name='webpart' and ./*/*/*/a//span[text()='" + title + "' or starts-with(text(), '" + title + ":')]]//table[starts-with(@id,'dataregion_') and not(contains(@id, 'header'))]");
        _test.waitForElement(l, _test.WAIT_FOR_JAVASCRIPT * 3);
        return _test.getAttribute(l, "id").substring(11);
    }

    public DataRegionTable getDrForQueryWebpart(String title)
    {
        return new DataRegionTable(getNameForQueryWebpart(title), _test);
    }

    public void setFormField(String name, String value)
    {
        _test.setFormElement(Locator.name(name), value);
        //there is a deliberate delay after user input for a change to commit in the Ext store
        _test.sleep(250);
    }
}
