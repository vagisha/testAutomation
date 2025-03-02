/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.pages.assay;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

import java.util.Map;

public class AssayDataPage extends LabKeyPage<AssayDataPage.ElementCache>
{
    public AssayDataPage(WebDriver driver)
    {
        super(driver);
    }

    public static AssayDataPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static AssayDataPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("assay", containerPath, "assayResults"));
        return new AssayDataPage(driver.getDriver());
    }

    public static AssayDataPage beginAt(WebDriverWrapper driver, String containerPath, Integer protocolId)
    {
        driver.beginAt(WebTestHelper.buildURL("assay", containerPath, "assayResults", Map.of("rowId", protocolId)));
        return new AssayDataPage(driver.getDriver());
    }

    public DataRegionTable getDataTable()
    {
        return DataRegionTable.DataRegion(getDriver()).withName("Data").findWhenNeeded(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {

    }
}
