package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;

public class FileBrowserHelperWD implements FileBrowserHelperParams
{
    BaseWebDriverTest _test;
    public FileBrowserHelperWD(BaseWebDriverTest test)
    {
        _test = test;
    }

    @Override
    public void expandFileBrowserRootNode()
    {
        // expand root tree node
        _test.waitAndClick(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='/']"));
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'tree-selected') and @*='/']"), WAIT_FOR_JAVASCRIPT);
        // TODO: here are the Ext4 versions
        //        _test.waitAndClick(Locator.xpath("//tr[contains(@class, 'x4-grid-tree-node') and @*='/']//div[contains(@class, 'x4-grid-cell-inner-treecolumn')]"));
        //        _test.waitForElement(Locator.xpath("//tr[contains(@class, 'x4-grid-row-selected') and @*='/']//div[contains(@class, 'x4-grid-cell-inner-treecolumn')]"), WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void selectFileBrowserItem(@LoggedParam String path)
    {
        try
        {
            Thread.sleep(500);
        }
        catch(InterruptedException e)
        {
            _test.log("Sleep for file browser item selection interupted.");
        }

        String[] parts = {};
        StringBuilder nodeId = new StringBuilder();
        if (path.startsWith("/"))
            path = path.substring(1);
        if (!path.equals(""))
        {
            parts = path.split("/");
            nodeId.append('/');
        }
        waitForFileGridReady();

        if (parts.length > 1)
        {
            expandFileBrowserRootNode();
        }

        for (int i = 0; i < parts.length; i++)
        {
            waitForFileGridReady();

            nodeId.append(parts[i]).append('/');

            if (i == parts.length - 1 && !path.endsWith("/")) // Trailing '/' indicates directory
            {
                // select last item: click on tree node name
                try
                {
                    Thread.sleep(500);
                }
                catch(InterruptedException e)
                {
                    _test.log("Sleep for file browser item selection interupted.");
                }
                clickFileBrowserFileCheckbox(parts[i]);
            }
            else
            {
                // expand tree node: click on expand/collapse icon
                _test.waitAndClick(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='" + nodeId + "']"));
                _test.waitForElement(Locator.xpath("//div[contains(@class, 'tree-selected') and @*='" + nodeId + "']"), WAIT_FOR_JAVASCRIPT);
                // TODO: here is the Ext4 version
                //_test.waitForElement(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"));
                //LabKeyExpectedConditions.animationIsDone(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"));
                //_test.scrollIntoView(Locator.xpath("//tr[contains(@id, '"+nodeId+"')]//span[contains(@class, 'x4-tree-node') and text()='"+parts[i]+"']"));
                //_test.clickAt(Locator.xpath("//tr[contains(@id, '" + nodeId + "')]//span[contains(@class, 'x4-tree-node') and text()='" + parts[i] + "']"), 1, 1, 0);

            }
        }
    }

    @Override
    public void clickFileBrowserFileCheckbox(@LoggedParam String fileName)
    {
        waitForFileGridReady();
        _test.waitForElement(Locator.css("div.labkey-filecontent-grid"));
        _test.waitForElement(ExtHelperWD.locateBrowserFileName(fileName));
        Locator rowSelected = Locator.css(".labkey-filecontent-grid div.x-grid3-row-selected > table > tbody > tr > td > div").withText(fileName);
        Boolean wasChecked = _test.isElementPresent(rowSelected);

        if (_test.isElementPresent(ExtHelperWD.locateGridRowCheckbox(fileName)))
            _test.click(Locator.xpath("//div").withClass("labkey-filecontent-grid").append(ExtHelperWD.locateGridRowCheckbox(fileName)));
        else
            _test.click(Locator.xpath("//div").withClass("labkey-filecontent-grid").append("//td").withClass("x-grid3-td-1").withText(fileName));

        if (wasChecked)
            _test.waitForElementToDisappear(rowSelected);
        else
            _test.waitForElement(rowSelected);
    }

    @Override
    public void selectFileBrowserRoot()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void selectAllFileBrowserFiles()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void renameFile(String currentName, String newName)
    {
        _test.click(Locator.xpath("//div[text()='" + currentName + "']"));
        _test.click(Locator.css("button.iconRename"));
        _test.waitForDraggableMask();
        _test._extHelper.setExtFormElementByLabel("Filename:", newName);
        _test.click(Locator.extButton("Rename"));
        _test.waitForText(newName);
    }

    @Override
    public void moveFile(String fileName, String destinationPath)
    {
        selectFileBrowserItem(fileName);
        _test.click(Locator.css("button.iconMove"));
        _test._extHelper.waitForExtDialog("Choose Destination");
        //TODO:  this doesn't yet support nested folders
        Locator folder =Locator.xpath("(//a/span[contains(text(),'" + destinationPath + "')])[2]");
        _test.waitForElement(folder);  //if it still isn't coming up, that's a product bug
        _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(Locator.css(".x-window ul.x-tree-node-ct").withText(destinationPath)));
        _test.click(folder);
        _test.clickButton("Move", BaseWebDriverTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
    }

    @Override
    public void createFolder(String folderName)
    {
        _test.clickButton("Create Folder", BaseWebDriverTest.WAIT_FOR_EXT_MASK_TO_APPEAR);
        _test.setFormElement(Locator.name("folderName"), folderName);
        _test.clickButton("Submit", BaseWebDriverTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForElement(Locator.css("#fileBrowser td.x-grid3-cell").withText(folderName));
    }

    @Override
    public void addToolbarButton(String buttonName)
    {
        _test.dragAndDrop(Locator.xpath("//td[contains(@class, 'x-table-layout-cell')]//button[text()='" + buttonName + "']"),
                Locator.xpath("//div[contains(@class, 'test-custom-toolbar')]"));
        _test.waitForElement(Locator.css(".test-custom-toolbar .iconFolderNew"), _test.defaultWaitForPage);
    }

    @Override
    public void removeToolbarButton(String buttonName)
    {
        // the button should appear twice, once in the full button list and again in the toolbar. we want the 2nd one.
        _test.click(Locator.buttonContainingText(buttonName).index(2));
        _test.click(Locator.tagContainingText("span", "remove"));
    }

    @Override
    public void goToConfigureButtonsTab()
    {
        goToAdminMenu();

        _test._extHelper.clickExtTab("Toolbar and Grid Settings");
        _test.waitForText("Configure Grid columns and Toolbar");
    }

    @Override
    public void goToAdminMenu()
    {
        try
        {
            _test.assertElementVisible(_test.getButtonLocator("Admin"));
            waitForFileAdminEnabled();
            _test.clickButton("Admin", 0);
        }
        catch(AssertionError e)
        {
            _test.click(Locator.xpath("//span[contains(@class, 'x4-toolbar-more-icon')]"));
            _test.click(Locator.xpath("//span[text()='Admin' and contains(@class, 'x4-menu-item-text')]"));
        }

        _test._extHelper.waitForExtDialog("Manage File Browser Configuration");
        // TODO: enable this in place of waitForExtDialog above
        // _test.waitForElement(Ext4HelperWD.Locators.window("Manage File Browser Configuration"));
    }

    @Override
    public void selectImportDataAction(@LoggedParam String actionName)
    {
        waitForFileGridReady();
        waitForImportDataEnabled();
        _test.clickButton("Import Data", 0);
        _test.waitAndClick(Locator.xpath("//input[@type='radio' and @name='importAction' and not(@disabled)]/../label[text()=" + Locator.xq(actionName) + "]"));
        _test.clickButton("Import");
    }

    @Override
    public void uploadFile(File file)
    {
        uploadFile(file, null, null);
    }

    @Override
    public void uploadFile(File file, @Nullable String description, @Nullable List<FileBrowserExtendedProperty> fileProperties)
    {
        // TODO: convert to Ext4 when new file webpart is enabled

        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            public boolean check()
            {
                return _test.getFormElement(Locator.xpath("//label[./span[text() = 'Choose a File:']]//..//input[contains(@class, 'x-form-file-text')]")).equals("");
            }
        }, "Upload field did not clear after upload.", WAIT_FOR_JAVASCRIPT);

        chooseSingleFileUpload(file);
        if (description != null)
            _test._extHelper.setExtFormElementByLabel("Description:", description);

        _test.clickButton("Upload", 0);

        if (fileProperties != null && fileProperties.size() > 0)
        {
            _test._extHelper.waitForExtDialog("Extended File Properties");
            for (FileBrowserExtendedProperty prop : fileProperties)
            {
                if (prop.isCombobox())
                    _test._extHelper.selectComboBoxItem(prop.getName(), prop.getValue());
                else
                    _test.setFormElement(Locator.name(prop.getName()), prop.getValue());
            }
            _test.clickButton("Done", 0);
            _test._extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);

            for (FileBrowserExtendedProperty prop : fileProperties)
            {
                _test.waitForText(prop.getValue());
            }
        }

        _test.waitForElement(Locator.css("#fileBrowser div.x-grid3-col-2").withText(file.getName()));
        if (description != null)
            _test.waitForText(description);
    }

    private void chooseSingleFileUpload(File file)
    {
        WebElement displayField = Locator.css(".single-upload-panel input.x-form-file-text").findElement(_test.getDriver());
        _test.executeScript("arguments[0].style.display = 'none';", displayField);
        _test.setFormElement(Locator.css(".single-upload-panel input[type=file]"), file);
        _test.executeScript("arguments[0].style.display = '';", displayField);
    }

    @Override
    public void importFile(String filePath, String importAction)
    {
        selectFileBrowserItem(filePath);
        selectImportDataAction(importAction);
    }

    @Override
    public void clickFileBrowserButton(@LoggedParam String actionName)
    {
        waitForFileGridReady();
        try
        {
            _test.assertElementVisible(Locator.ext4ButtonContainingText(actionName));
            _test.click(Locator.ext4ButtonContainingText(actionName));
        }
        catch(AssertionError e)
        {
            _test.click(Locator.xpath("//span[contains(@class, 'x4-toolbar-more-icon')]"));
            _test.click(Locator.xpath("//span[text()='"+actionName+"' and contains(@class, 'x4-menu-item-text')]"));
        }
    }

    @Override
    public void waitForFileGridReady()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-file-grid-initialized')]"), 6 * WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void waitForImportDataEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-import-enabled')]"), 6 * WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void waitForFileAdminEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-admin-enabled')]"), 6 * WAIT_FOR_JAVASCRIPT);
    }
}
