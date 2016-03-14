/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Component wrapper class for interacting with a LabKey Data Region (see clientapi/dom/DataRegion.js)
 */
public class DataRegionTable extends Component
{
    public static final String SELECTION_SIGNAL = "dataRegionSelectionChange";
    protected final String _regionName;
    public BaseWebDriverTest _test;
    private WebElement _tableElement;

    protected final Map<String, Integer> _mapColumns = new HashMap<>();
    protected final Map<String, Integer> _mapRows = new HashMap<>();
    protected final int _columnCount;
    protected final boolean _selectors;
    protected final boolean _floatingHeaders;

    private Elements _elements = new Elements();

    /**
     * @param test Necessary while DRT methods live in BWDT
     * @param table table element that contains data region
     */
    public DataRegionTable(BaseWebDriverTest test, WebElement table)
    {
        _test = test;
        _test.waitForElement(Locators.pageSignal(SELECTION_SIGNAL));

        _tableElement = table;
        _regionName = table.getAttribute("lk-region-name");
        _columnCount = _test.getTableColumnCount(getTableId());

        _selectors = !Locator.css(".labkey-selectors").findElements(table).isEmpty();
        _floatingHeaders = !Locator.css(".dataregion_column_header_row_spacer").findElements(table).isEmpty();
    }

    /**
     * @param regionName 'lk-region-name' of the table
     * @param test Necessary while DRT methods live in BWDT
     */
    public DataRegionTable(String regionName, BaseWebDriverTest test)
    {
        _test = test;
        _test.waitForElement(Locators.pageSignal(SELECTION_SIGNAL));

        _tableElement = new RefindingWebElement(Locators.dataRegion(regionName), test.getDriver());
        ((RefindingWebElement)_tableElement).addRefindListener((element -> clearCache()));

        _regionName = _tableElement.getAttribute("lk-region-name");
        _columnCount = _test.getTableColumnCount(getTableId());

        _selectors = !Locator.css(".labkey-selectors").findElements(_tableElement).isEmpty();
        _floatingHeaders = !Locator.css(".dataregion_column_header_row_spacer").findElements(_tableElement).isEmpty();
    }

    private WebElement getTableElement()
    {
        return _tableElement;
    }

    @Override
    public WebElement getComponentElement()
    {
        return getTableElement();
    }

    protected Elements elements()
    {
        return _elements;
    }

    protected void clearCache()
    {
        _elements = new Elements();
    }

    protected int getHeaderRowCount()
    {
        return 2 + (_floatingHeaders ? 2 : 0);
    }

    public static WebElement waitForDataRegion(BaseWebDriverTest test, String regionName)
    {
        return test.waitForElement(Locators.dataRegion(regionName));
    }

    public static DataRegionTable findDataRegion(BaseWebDriverTest test)
    {
        return findDataRegion(test, 0);
    }

    public static DataRegionTable findDataRegion(BaseWebDriverTest test, int index)
    {
        return findDataRegionWithin(test, test.getDriver(), index);
    }

    public static DataRegionTable findDataRegionWithin(BaseWebDriverTest test, SearchContext context)
    {
        return findDataRegionWithin(test, context, 0);
    }

    public static DataRegionTable findDataRegionWithin(BaseWebDriverTest test, SearchContext context, int index)
    {
        Locator dataRegionLoc = Locator.css("table[lk-region-name]").index(index);
        return new DataRegionTable(test, new RefindingWebElement(dataRegionLoc, context));
    }

    public static DataRegionTable findDataRegionWithinWebpart(BaseWebDriverTest test, String webPartTitle)
    {
        return findDataRegionWithin(test, new RefindingWebElement(PortalHelper.Locators.webPart(webPartTitle), test.getDriver()));
    }

    public String getTableName()
    {
        return _regionName;
    }

    public String getTableId()
    {
        return getTableElement().getAttribute("id");
    }

    public Locator.IdLocator locator()
    {
        return Locator.id(getTableId());
    }

    public int getColumnCount()
    {
        return _columnCount;
    }

    private boolean bottomBarPresent()
    {
        return _test.isElementPresent(Locator.tagWithId("table", getTableId() + "-footer"));
    }

    public boolean hasAggregateRow()
    {
        return _test.isElementPresent(Locator.xpath("//table[@id=" + Locator.xq(getTableId()) + "]//tr[contains(@class, 'labkey-col-total')]"));
    }

    public List<WebElement> getHeaderButtons()
    {
        String headerId = getTableId() + "-header";
        return Locator.css("#" + headerId + " a.labkey-button, #" + headerId + " a.labkey-menu-button").findElements(_test.getDriver());
    }

    public int getDataRowCount()
    {
        int rows = _test.getTableRowCount(getTableId()) - (getHeaderRowCount() + (bottomBarPresent() ? 1 : 0));
        if (hasAggregateRow())
            rows -= 1;

        if (rows == 1 && hasNoDataToShow())
            rows = 0;

        return rows;
    }

    public int getRow(String columnLabel, String value)
    {
        return getRow(getColumn(columnLabel), value);
    }

    public int getRow(int columnIndex, String value)
    {
        int rowCount = getDataRowCount();
        for (int i=0; i < rowCount; i++)
        {
            if (value.equals(getDataAsText(i, columnIndex)))
                return i;
        }
        return -1;
    }

    public String getTotal(String columnLabel)
    {
        return getTotal(getColumn(columnLabel));
    }

    public String getTotal(int columnIndex)
    {
        return _test.getText(Locator.css("#" + getTableId() + " tr.labkey-col-total > td:nth-of-type(" + (columnIndex + (_selectors ? 2 : 1)) + ")"));
    }

    /**
     * do nothing if column is already present, add it if it is not
     * @param columnName   name of column to add, if necessary
     */
    public void ensureColumnPresent(String columnName)
    {
        if (getColumn(columnName) < 0)
        {
            _test._customizeViewsHelper.openCustomizeViewPanel();
            _test._customizeViewsHelper.addCustomizeViewColumn(columnName);
            _test._customizeViewsHelper.applyCustomView();
        }
    }

    /**
     * check for presence of columns, add them if they are not already present
     * requires columns actually exist
     * @param names names of columns to add
     */
    public void ensureColumnsPresent(String... names)
    {
        boolean opened = false;
        for (String name: names)
        {
            if (getColumn(name) == -1)
            {
                if (!opened)
                {
                    _test._customizeViewsHelper.openCustomizeViewPanel();
                    opened = true;
                }
                _test._customizeViewsHelper.addCustomizeViewColumn(name);
            }
        }
        if (opened)
            _test._customizeViewsHelper.applyCustomView();
    }

    /**
     * returns index of the row of the first appearance of the specified data, in the specified column
     * @param data
     * @param column
     * @return
     */
    public int getIndexWhereDataAppears(String data, String column)
    {
        List<String> allData = getColumnDataAsText(column);
        return allData.indexOf(data);
    }

    public Locator.XPathLocator detailsXpath(int row)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(getTableId()) + "]/tbody/tr[" + (row + getHeaderRowCount() + 1) + "]/td[contains(@class, 'labkey-details')]");
    }

    public Locator.XPathLocator detailsLink(int row)
    {
        Locator.XPathLocator cell = detailsXpath(row);
        return cell.child("a[1]");
    }

    public Locator.XPathLocator updateXpath(int row)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(getTableId()) + "]/tbody/tr[" + (row + getHeaderRowCount() + 1) + "]/td[contains(@class, 'labkey-update')]");
    }

    public Locator.XPathLocator updateLink(int row)
    {
        Locator.XPathLocator cell = updateXpath(row);
        return cell.child("a[1]");
    }

    public Locator.XPathLocator xpath(int row, int col)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(getTableId()) + "]/tbody/tr[" + (row + getHeaderRowCount() + 1) + "]/td[" + (col + 1 + (_selectors ? 1 : 0)) + "]");
    }

    public Locator.XPathLocator link(int row, int col)
    {
        Locator.XPathLocator cell = xpath(row, col);
        return cell.child("a[1]");
    }

    public WebElement link(int row, String columnName)
    {
        int col = getColumn(columnName);
        if (col == -1)
            fail("Couldn't find column '" + columnName + "'");
        return findElement(link(row, col));
    }

    public int getColumn(String name)
    {
        name = name.replaceAll(" ", "");

        if (_mapColumns.containsKey(name))
            return _mapColumns.get(name);

        getColumnHeaders();

        if (_mapColumns.containsKey(name))
            return _mapColumns.get(name);

        _test.log("Column '" + name + "' not found");
        return -1;
    }

    public List<String> getColumnHeaders()
    {
        List<String> columnHeaders = new ArrayList<>();
        _mapColumns.clear(); // Start fresh

        for (int col = 0; col < _columnCount; col++)
        {
            String header = getDataAsText(-(getHeaderRowCount()/2), col);
            columnHeaders.add(header);
            if (header != null)
            {
                String headerName = header.split("\n")[0];
                headerName = headerName.replaceAll(" ", "");
                if (!StringUtils.isEmpty(headerName)
                        && !_mapColumns.containsKey(headerName)) // Remember only the first occurrence of each column label
                    _mapColumns.put(headerName, col);
            }
        }

        return columnHeaders;
    }

    public List<String> getColumnDataAsText(int col)
    {
        int rowCount = getDataRowCount();
        List<String> columnText = new ArrayList<>();

        if (col >= 0)
        {
            for (int row = 0; row < rowCount; row++)
            {
                columnText.add(getDataAsText(row, col));
            }
        }

        return columnText;
    }

    public List<String> getColumnDataAsText(String name)
    {
        int col = getColumn(name);
        return getColumnDataAsText(col);
    }

    public List<String> getRowDataAsText(int row)
    {
        final int colCount = getColumnCount();
        List<String> rowText = new ArrayList<>();

        for (int col = 0; col < colCount; col++)
        {
            rowText.add(getDataAsText(row, col));
        }

        return rowText;
    }

    /**
     * Get values for all specified columns for all pages of the table
     * preconditions:  must be on start page of table
     * postconditions:  at start of table
     */
    public List<List<String>> getFullColumnValues(String... columnNames)
    {
        boolean moreThanOnePage = _test.isElementPresent(Locator.linkWithText("Next"));

        if (moreThanOnePage)
        {
            showAll();
        }

        List<List<String>> columns = new ArrayList<>();
        for (String columnName : columnNames)
        {
            columns.add(getColumnDataAsText(columnName));
        }

        if (moreThanOnePage)
        {
            setPageSize(100);
        }

        return columns;
    }

    public List<List<String>> getRows(String...columnNames)
    {
        List<List<String>> fullColumnValues = getFullColumnValues(columnNames);
        return collateColumnsIntoRows(fullColumnValues);
    }

    @SafeVarargs
    public static List<List<String>> collateColumnsIntoRows(List<String>...columns)
    {
        return collateColumnsIntoRows(Arrays.asList(columns));
    }

    public static List<List<String>> collateColumnsIntoRows(List<List<String>> columns)
    {
        int rowCount = 0;
        for (int i = 0; i < columns.size() - 1; i++)
        {
            rowCount = columns.get(i).size();
            if (columns.get(i).size() != columns.get(i+1).size())
                throw new IllegalArgumentException("Columns not of equal sizes");
        }

        List<List<String>> rows = new ArrayList<>();

        for (int rowNum = 0; rowNum < rowCount; rowNum++)
        {
            List<String> row = new ArrayList<>(columns.size());
            for (List<String> column : columns)
            {
                row.add(column.get(rowNum));
            }
            rows.add(row);
        }

        return rows;
    }

    /** Find the row number for the given primary key. */
    public int getRow(String pk)
    {
        assertTrue("Need the selector checkbox's value to find the row with the given pk", _selectors);

        Integer cached = _mapRows.get(pk);
        if (cached != null)
            return cached.intValue();

        int row = 0;
        try
        {
            while (true)
            {
                String value = _test.getAttribute(Locator.xpath("//table[@id=" + Locator.xq(getTableId()) +"]//tr[" + (row+5) + "]//input[@name='.select']"), "value");
                _mapRows.put(value, row);
                if (value.equals(pk))
                    return row;
                row += 1;
            }
        }
        catch (NoSuchElementException ignore)
        {
            // Throws an exception, if row is out of bounds.
        }

        return -1;
    }

    private boolean hasNoDataToShow()
    {
        return "No data to show.".equals(_getDataAsText(getHeaderRowCount(), 0));
    }

    public WebElement findCell(int row, String column)
    {
        if (getColumn(column) < 0)
            throw new NoSuchElementException("No such column '" + column + "'");
        return findCell(row, getColumn(column));
    }

    public WebElement findCell(int row, int column)
    {
        row += getHeaderRowCount();
        column += _selectors ? 1 : 0;
        return Locator.css("tr:nth-of-type(" + (row + 1) + ") > td:nth-of-type(" + (column + 1) + ")").findElement(getTableElement());
    }

    public String getDataAsText(int row, int column)
    {
        return _getDataAsText(row + getHeaderRowCount(), column + (_selectors ? 1 : 0));
    }

    // Doesn't adjust for header rows or selector columns.
    private String _getDataAsText(int row, int column)
    {
        String ret = null;

        try
        {
            ret = _test.getTableCellText(getTableId(), row, column);
        }
        catch (NoSuchElementException ignore) {}

        return ret;
    }

    public String getDataAsText(int row, String columnName)
    {
        int col = getColumn(columnName);
        if (col == -1)
            return null;
        return getDataAsText(row, col);
    }

    public String getDataAsText(String pk, String columnName)
    {
        int row = getRow(pk);
        if (row == -1)
            return null;
        int col = getColumn(columnName);
        if (col == -1)
            return null;
        return getDataAsText(row, col);
    }

    public String getDetailsHref(int row)
    {
        Locator l = detailsLink(row);
        return _test.getAttribute(l, "href");
    }

    public String getUpdateHref(int row)
    {
        Locator l = updateLink(row);
        return _test.getAttribute(l, "href");
    }

    public String getHref(int row, int column)
    {
        // headerRows and selector offsets are applied in link() locator
        return _getHref(row, column);
    }

    private String _getHref(int row, int column)
    {
        Locator l = link(row, column);
        return _test.getAttribute(l, "href");
    }

    public String getHref(int row, String columnName)
    {
        int col = getColumn(columnName);
        if (col == -1)
            return null;
        return getHref(row, col);
    }

    public String getHref(String pk, String columnName)
    {
        int row = getRow(pk);
        if (row == -1)
            return null;
        int col = getColumn(columnName);
        if (col == -1)
            return null;
        return getHref(row, col);
    }

    public boolean hasHref(int row, int column)
    {
        // headerRows and selector offsets are applied in link() locator
        return _hasHref(row, column);
    }

    private boolean _hasHref(int row, int column)
    {
        // Check the td cell is present, but has no link.
        Locator.XPathLocator cell = xpath(row, column);
        _test.assertElementPresent(cell);

        Locator link = link(row, column);
        return _test.isElementPresent(link);
    }

    public boolean hasHref(int row, String columnName)
    {
        int col = getColumn(columnName);
        if (col == -1)
            fail("Column '" + columnName + "' not found.");
        return hasHref(row, col);
    }

    public void setSort(String columnName, SortDirection direction)
    {
        _test.setSort(_regionName, columnName, direction);
    }

    public void clearSort(String columnName)
    {
        _test.clearSort(_regionName, columnName);
    }

    public void openFilterDialog(String columnName)
    {
        Locator.XPathLocator menuLoc = DataRegionTable.Locators.columnHeader(getTableName(), columnName);
        String columnLabel = _test.getText(menuLoc);
        _test._ext4Helper.clickExt4MenuButton(false, menuLoc, false, "Filter...");

        final Locator.XPathLocator filterDialog = ExtHelper.Locators.window("Show Rows Where " + columnLabel + "...");
        _test.waitForElement(filterDialog);

        _test.waitFor(() -> _test.isElementPresent(filterDialog.append(Locator.linkWithText("[All]")).notHidden()) ||
                        _test.isElementPresent(filterDialog.append(Locator.tagWithId("input", "value_1").notHidden())),
                "Filter Dialog", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test._extHelper.waitForLoadingMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void setFilter(String columnName, String filterType)
    {
        setFilter(columnName, filterType, null, BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void setFilter(String columnName, String filterType, @Nullable String filter)
    {
        setFilter(columnName, filterType, filter, BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void setFilter(String columnName, String filterType, @Nullable String filter, int waitMillis)
    {
        setUpFilter(columnName, filterType, filter);
        _test.clickButton("OK", waitMillis);
    }

    public void setFilter(String columnName, String filterType, @Nullable String filter, @Nullable String filter2Type, @Nullable String filter2)
    {
        setUpFilter(columnName, filterType, filter, filter2Type, filter2);
        _test.clickButton("OK", BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void setFacetedFilter(String columnName, String... values)
    {
        setUpFacetedFilter(columnName, values);
        _test.clickButton("OK");
    }

    public void setUpFilter(String columnName, String filterType, String filter)
    {
        setUpFilter(columnName, filterType, filter, null, null);
    }

    public void setUpFilter(String columnName, String filter1Type, @Nullable String filter1, @Nullable String filter2Type, @Nullable  String filter2)
    {
        String log = "Setting filter in " + _regionName + " for " + columnName + " to " + filter1Type.toLowerCase() + (filter1 != null ? " " + filter1 : "");
        if (filter2Type != null)
        {
            log += " and " + filter2Type.toLowerCase() + (filter2 != null ? " " + filter2 : "");
        }
        _test.log(log);

        openFilterDialog(columnName);

        if (_test.isElementPresent(Locator.css("span.x-tab-strip-text").withText("Choose Values")))
        {
            _test.log("Switching to advanced filter UI");
            _test._extHelper.clickExtTab("Choose Filters");
            _test.waitForElement(Locator.xpath("//span[" + Locator.NOT_HIDDEN + " and text()='Filter Type:']"), WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }

        //Select combo box item
        _test._extHelper.selectComboBoxItem("Filter Type:", filter1Type);

        if (filter1 != null && !filter1Type.contains("Blank"))
        {
            _test.setFormElement(Locator.id("value_1"), filter1);
        }

        if (filter2Type != null && !filter2Type.contains("Blank"))
        {
            //Select combo box item
            _test._extHelper.selectComboBoxItem("and:", filter2Type);
            if (filter2 != null)
            {
                _test.setFormElement(Locator.id("value_2"), filter2);
            }
        }
    }

    public void setUpFacetedFilter(String columnName, String... values)
    {
        String log;
        if (values.length > 0)
        {
            log = "Setting filter in " + _regionName + " for " + columnName + " to one of: [";
            for (String v : values)
            {
                log += v + ", ";
            }
            log = log.substring(0, log.length() - 2) + "]";
        }
        else
        {
            log = "Clear filter in " + _regionName + " for " + columnName;
        }

        _test.log(log);

        openFilterDialog(columnName);
        String columnLabel = _test.getText(DataRegionTable.Locators.columnHeader(_regionName, columnName));

        _test.sleep(500);

        // Clear selections.
        assertEquals("Faceted filter tab should be selected.", "Choose Values", _test.getText(Locator.css(".x-tab-strip-active")));
        if (!_test.isElementPresent(Locator.xpath("//div[contains(@class, 'x-grid3-hd-checker-on')]")))
        {
            _test.click(Locator.linkWithText("[All]"));
        }
        _test.click(Locator.linkWithText("[All]"));

        if (values.length > 1)
        {
            for (String v : values)
            {
                _test.click(Locator.xpath(_test._extHelper.getExtDialogXPath("Show Rows Where " + columnLabel + "...") +
                        "//div[contains(@class,'x-grid3-row') and .//span[text()='" + v + "']]//div[@class='x-grid3-row-checker']"));
            }
        }
        else if (values.length == 1)
        {
            _test.click(Locator.xpath(_test._extHelper.getExtDialogXPath("Show Rows Where " + columnLabel + "...")+
                    "//div[contains(@class,'x-grid3-row')]//span[text()='" + values[0] + "']"));
        }
    }

    public void clearFilter(String columnName)
    {
        clearFilter(columnName, BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clearFilter(String columnName, int waitMillis)
    {
        _test.log("Clearing filter in " + _regionName + " for " + columnName);

        Runnable clickClearFilter = () ->
                _test._ext4Helper.clickExt4MenuButton(false, Locators.columnHeader(_regionName, columnName), false, "Clear Filter");

        if (waitMillis == 0)
            _test.doAndWaitForPageSignal(clickClearFilter, SELECTION_SIGNAL);
        else
            _test.doAndWaitForPageToLoad(clickClearFilter, waitMillis);

    }

    public void clearAllFilters(String columnName)
    {
        _test.log("Clearing filter in " + _regionName + " for " + columnName);
        openFilterDialog(columnName);
        _test.clickButton("CLEAR ALL FILTERS");
    }

    public void checkAllOnPage()
    {
        checkAll();
    }

    public void uncheckAllOnPage()
    {
        uncheckAll();
    }

    private WebElement getToggle()
    {
        return Locator.tagWithId("table", getTableId()).append(Locator.tagWithAttribute("input", "name", ".toggle")).findElement(_test.getDriver());
    }

    public void checkAll()
    {
        WebElement toggle = getToggle();
        if (!toggle.isSelected())
            _test.doAndWaitForPageSignal(toggle::click, SELECTION_SIGNAL);
    }

    public void uncheckAll()
    {
        WebElement toggle = getToggle();
        if (null != _test.doAndWaitForPageSignal(toggle::click, SELECTION_SIGNAL))
            _test.doAndWaitForPageSignal(toggle::click, SELECTION_SIGNAL);
    }

    // NOTE: this method would be better named checkCheckboxByPrimaryKey --> while it does take a string, this string will often be a string value
    public void checkCheckbox(String value)
    {
        WebElement checkbox = Locator.tagWithId("table", getTableId()).append(
                Locator.tagWithName("input", ".select").withAttribute("value", value)).
                findElement(_test.getDriver());
        if (!checkbox.isSelected())
            _test.doAndWaitForPageSignal(checkbox::click, SELECTION_SIGNAL);
    }

    public void checkCheckbox(int index)
    {
        WebElement checkbox = Locator.tagWithId("table", getTableId()).append(
                Locator.tagWithName("input", ".select")).index(index).
                findElement(_test.getDriver());
        if (!checkbox.isSelected())
            _test.doAndWaitForPageSignal(checkbox::click, SELECTION_SIGNAL);
    }

    public void uncheckCheckbox(int index)
    {
        WebElement checkbox = Locator.tagWithId("table", getTableId()).append(
                Locator.tagWithName("input", ".select")).index(index).
                findElement(_test.getDriver());
        if (checkbox.isSelected())
            _test.doAndWaitForPageSignal(checkbox::click, SELECTION_SIGNAL);
    }

    public void pageFirst()
    {
        _test.log("Clicking page first on data region '" + _regionName + "'");
        clickDataRegionPageLink("First Page");
    }

    public void pageLast()
    {
        _test.log("Clicking page last on data region '" + _regionName + "'");
        clickDataRegionPageLink("Last Page");
    }

    public void pageNext()
    {
        _test.log("Clicking page next on data region '" + _regionName + "'");
        clickDataRegionPageLink("Next Page");
    }

    public void pagePrev()
    {
        _test.log("Clicking page previous on data region '" + _regionName + "'");
        clickDataRegionPageLink("Previous Page");
    }

    public void clickDataRegionPageLink(String title)
    {
        String headerId = Locator.xq(getTableId() + "-header");
        _test.clickAndWait(Locator.xpath("//table[@id=" + headerId + "]//div/a[@title='" + title + "']"));
    }

    public void showAll()
    {
        clickHeaderButton("Page Size", "Show All");
    }

    public void setPageSize(int size)
    {
        clickHeaderButton("Page Size", size + " per page");
    }

    /**
     * Set the current offset by manipulating the url rather than using the pagination buttons.
     * @param offset
     */
    public void setOffset(int offset)
    {
        String url = replaceParameter(_regionName + ".offset", String.valueOf(offset));
        _test.beginAt(url);
    }

    /**
     * Set the page size by manipulating the url rather than using the "XXX per page" menu items.
     * @param size new page size
     */
    public void setMaxRows(int size)
    {
        String url = replaceParameter(_regionName + ".maxRows", String.valueOf(size));
        _test.beginAt(url);
    }

    @LogMethod
    public void createQuickChart(String columnName)
    {
        _test._ext4Helper.clickExt4MenuButton(true, DataRegionTable.Locators.columnHeader(_regionName, columnName), false, "Quick Chart");
        _test.waitForElement(Locator.css("svg"));
    }

    private String replaceParameter(String param, String newValue)
    {
        URL url = _test.getURL();
        String file = url.getFile();
        String encodedParam = EscapeUtil.encode(param);
        file = file.replaceAll("&" + Pattern.quote(encodedParam) + "=\\p{Alnum}+?", "");
        if (newValue != null)
            file += "&" + encodedParam + "=" + EscapeUtil.encode(newValue);

        try
        {
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), file);
        }
        catch (MalformedURLException mue)
        {
            throw new RuntimeException(mue);
        }
        return url.getFile();
    }

    // ======== Side facet panel =========

    public void openSideFilterPanel()
    {
        _test.click(Locators.headerButton(_regionName, "Filter"));
        _test.waitForElement(Locators.expandedFacetPanel(_regionName));
        _test.waitForElement(Locator.css(".lk-filter-panel-label"));
    }

    public void toggleAllFacetsCheckbox()
    {
        _test.click(Locator.xpath("//div").withClass("lk-filter-panel-label").withText("All"));
    }

    public void clickHeaderButtonByText(String buttonText)
    {
        _test.waitAndClick(Locators.headerButton(_regionName, buttonText));
    }

    public void clickHeaderButton(String buttonText, String ... subMenuLabels)
    {
        clickHeaderButton(buttonText, true, subMenuLabels);
    }

    public void clickHeaderButton(String buttonText, boolean wait, String ... subMenuLabels)
    {
        _test._ext4Helper.clickExt4MenuButton(wait, DataRegionTable.Locators.headerMenuButton(_regionName, buttonText), false, subMenuLabels);
    }

    public List<String> getHeaderButtonSubmenuText(String buttonText)
    {
        List<String> subMenuItems = new ArrayList<>();
        List<WebElement> buttonElements = _test._ext4Helper.getExt4MenuButtonSubMenu(DataRegionTable.Locators.headerMenuButton(_regionName, buttonText));
        for (WebElement buttonElement : buttonElements)
        {
            subMenuItems.add(buttonElement.getText());
        }
        return subMenuItems;
    }

    public static class Locators extends org.labkey.test.Locators
    {
        public static Locator.XPathLocator dataRegion()
        {
            return Locator.tagWithClass("table", "labkey-data-region").attributeStartsWith("id", "lk-region-");
        }

        public static Locator.XPathLocator dataRegion(String regionName)
        {
            return Locator.tagWithAttribute("table", "lk-region-name", regionName);
        }

        public static Locator.XPathLocator headerButton(String regionName, String text)
        {
            return dataRegion(regionName).append(Locator.tagWithClass("a", "labkey-button").withText(text));
        }

        public static Locator.XPathLocator headerMenuButton(String regionName, String text)
        {
            return dataRegion(regionName).append(Locator.tagWithClass("a", "labkey-menu-button").withText(text));
        }

        private static Locator.XPathLocator facetPanel(String regionName)
        {
            return Locator.tagWithAttribute("div", "lk-region-facet-name", regionName);
        }

        public static Locator.XPathLocator expandedFacetPanel(String regionName)
        {
            return facetPanel(regionName).withDescendant(Locator.xpath("div").withPredicate("not(contains(@class, 'x4-panel-collapsed'))").withClass("labkey-data-region-facet"));
        }

        public static Locator.XPathLocator collapsedFacetPanel(String regionName)
        {
            return facetPanel(regionName).withPredicate(Locator.xpath("div/div").withClass("x4-panel-collapsed").withClass("labkey-data-region-facet"));
        }

        public static Locator.XPathLocator facetRow(String category)
        {
            return Locator.xpath("//div").withClass("x4-grid-body").withPredicate(Locator.xpath("//div").withClass("lk-filter-panel-label").withText(category));
        }

        public static Locator.XPathLocator facetRow(String category, String group)
        {
            return facetRow(category).withPredicate(Locator.xpath("//div").withClass("lk-filter-panel-label").withText(group));
        }

        public static Locator.XPathLocator facetRowCheckbox(String category)
        {
            return facetRow(category).append(Locator.tag("div").withClass("x4-grid-row-checker"));
        }

        public static Locator.XPathLocator facetRowCheckbox(String category, String group)
        {
            return facetRow(category, group).append(Locator.tag("div").withClass("x4-grid-row-checker"));
        }

        public static Locator.XPathLocator columnHeader(String regionName, String fieldName)
        {
            return Locator.tagWithAttribute("td", "column-name", regionName + ":" + fieldName);
        }

        public static Locator.XPathLocator columnHeaderWithLabel(String regionName, String fieldLabel)
        {
            return Locator.id(regionName).append(Locator.tagWithClass("td", "labkey-column-header")).withText(fieldLabel);
        }
    }

    protected class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        private List<WebElement> rows;
        private List<List<WebElement>> cells;

        protected List<WebElement> getRows()
        {
            if (rows == null)
                rows = Locator.css(".labkey-alternate-row, .labkey-row").findElements(this);
            return rows;
        }

        protected WebElement getRow(int rowIndex)
        {
            return getRows().get(rowIndex);
        }

        protected List<WebElement> getCells(int row)
        {
            if (cells == null)
                cells = new ArrayList<>(getRows().size());
            if (cells.get(row) == null)
                cells.add(row, Locator.css("td").findElements(getRow(row)));
            return cells.get(row);
        }

        protected WebElement getCell(int row, int col)
        {
            return getCells(row).get(col);
        }
    }
}
