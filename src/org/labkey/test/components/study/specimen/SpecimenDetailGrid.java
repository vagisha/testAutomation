package org.labkey.test.components.study.specimen;

import org.labkey.test.pages.study.specimen.ShowCreateSpecimenRequestPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

public class SpecimenDetailGrid extends DataRegionTable
{
    public SpecimenDetailGrid(WebDriver driver)
    {
        super("SpecimenDetail", driver);
    }

    public void viewExistingRequests()
    {
        clickHeaderMenu("Request Options", "View Existing Requests");
    }

    public ShowCreateSpecimenRequestPage createNewRequest()
    {
        clickHeaderMenu("Request Options", "Create New Request");
        return new ShowCreateSpecimenRequestPage(getDriver());
    }

    public void addToExistingRequest()
    {
        clickHeaderMenu("Request Options", "Add To Existing Request");
    }
}
