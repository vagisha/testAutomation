/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.Random;

/**
 * User: kevink
 * Date: Jan 6, 2011
 */
public class TargetStudyTest extends AbstractAssayTestWD
{
    private static final String ASSAY_NAME = "Assay";
    private static final String STUDY1_LABEL = "AwesomeStudy1";
    private static final String STUDY2_LABEL = "AwesomeStudy2";
    private static final String STUDY3_LABEL = "AwesomeStudy3";

    protected static final String TEST_RUN1 = "FirstRun";
    protected static final String TEST_RUN1_DATA1 =
            "specimenID\tparticipantID\tvisitID\tTargetStudy\n" +
            // study 1: full container path
            "AAA07XK5-05\t\t\t" + ("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1) + "\n" +
            // study 1: container id
            "AAA07XMC-02\t\t\t${Study1ContainerID}\n" +
            // study 1: study label
            "AAA07XMC-04\t\t\t${Study1Label}" + "\n" +
            // fake study / no study
            "AAA07XSF-02\t\t\tStudyNotExist\n" +
            // study 2
            "AAA07YGN-01\t\t\t" +("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY2) + "\n" +
            // study 3
            "AAA07YGN-02\t\t\t" +("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY3) + "\n"
            ;

    private String _study1ContainerId = null;
    private String _study1Label = null;
    private String _study2Label = null;
    private String _study3Label = null;

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_SECURITY;
    }


    @Override
    protected void runUITests() throws Exception
    {
        log("** Setup");
        setupEnvironment();
        setupSpecimens();
        setupLabels();
        setupAssay();

        clickFolder(TEST_ASSAY_PRJ_SECURITY);
        _study1ContainerId = getContainerId("/project/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1 + "/begin.view");
        log("** Study 1 container ID = " + _study1ContainerId);
        Assert.assertNotNull(_study1ContainerId);

        uploadRuns();
        copyToStudy();
    }


    @LogMethod
    protected void setupSpecimens()
    {
        log("** Import specimens into Study 1 and Study 2");
        setupPipeline(TEST_ASSAY_PRJ_SECURITY);
        SpecimenImporter importer1 = new SpecimenImporter(getTestTempDir(), new File(getLabKeyRoot(), "/sampledata/study/specimens/sample_a.specimens"), new File(getTestTempDir(), "specimensSubDir"), TEST_ASSAY_FLDR_STUDY1, 1);
        importer1.startImport();

        SpecimenImporter importer2 = new SpecimenImporter(getTestTempDir(), new File(getLabKeyRoot(), "/sampledata/study/specimens/sample_a.specimens"), new File(getTestTempDir(), "specimensSubDir"), TEST_ASSAY_FLDR_STUDY2, 1);
        importer2.startImport();

        importer1.waitForComplete();
        importer2.waitForComplete();

    }

    @LogMethod
    protected void setupLabels()
    {
        // Using a random label helps uniqueify the study when there is another "AwesomeStudy3" from a previous test run.
        Random r = new Random();
        _study1Label = STUDY1_LABEL + " " + r.nextInt();
        _study2Label = STUDY2_LABEL + " " + r.nextInt();
        _study3Label = STUDY3_LABEL + " " + r.nextInt();

        log("** Set some awesome study labels");
        beginAt("/study/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1 + "/manageStudyProperties.view");
        waitForElement(Locator.name("Label"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("Label"), _study1Label);
        clickButton("Submit", 0);
//        _extHelper.waitForExtDialog("Status");
//        Locator.css(".ext-el-mask").waitForElmementToDisappear(_driver, WAIT_FOR_JAVASCRIPT); // Mask doesn't have 'block' style
        waitForPageToLoad();

        beginAt("/study/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY2 + "/manageStudyProperties.view");
        waitForElement(Locator.name("Label"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("Label"), _study2Label);
        clickButton("Submit", 0);
        // Save is via AJAX, but redirects to the general study settings page when it's done
        waitForText("General Study Settings");

        beginAt("/study/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY3 + "/manageStudyProperties.view");
        waitForElement(Locator.name("Label"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("Label"), _study3Label);
        clickButton("Submit", 0);
        // Save is via AJAX, but redirects to the general study settings page when it's done
        waitForText("General Study Settings");
    }

    public boolean isFileUploadTest()
    {
        return true;
    }
    protected void setupAssay()
    {

//        log("** Define GPAT Assay");
        clickFolder(TEST_ASSAY_PRJ_SECURITY);
        if (!isLinkPresentWithText("Assay List"))
            addWebPart("Assay List");
        _assayHelper.uploadXarFileAsAssayDesign(getSampledataPath() + "/TargetStudy/Assay.xar", 1, "Assay.xar");
//        clickButton("Manage Assays");
//        clickButton("New Assay Design");
//        checkRadioButton("providerName", "General");
//        clickButton("Next");
//        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
//
//        selenium.type("//input[@id='AssayDesignerName']", ASSAY_NAME);
//        sleep(1000);
//
//        // Remove ParticipantVisitResolver and TargetStudy from the Batch domain
//        deleteField("Batch Fields", 0);
//        deleteField("Batch Fields", 0);
//
//        // Add TargetStudy to the end of the default list of Results domain
//        addField("Data Fields", 4, "TargetStudy", "Target Study", ListHelper.ListColumnType.String);
//
//        clickButton("Save", 0);
//        waitForText("Save successful.", 20000);

    }

    protected void uploadRuns()
    {
        log("** Upload Data");
        clickFolder(TEST_ASSAY_PRJ_SECURITY);

        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");

        setFormElement(Locator.name("name"), TEST_RUN1);
        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        String data1 = TEST_RUN1_DATA1
                .replace("${Study1ContainerID}", _study1ContainerId)
                .replace("${Study1Label}", _study1Label);
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), data1);
        clickButton("Save and Finish");
        assertTextPresent("Couldn't resolve TargetStudy 'StudyNotExist' to a study folder.");

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        String data2 = data1.replace("StudyNotExist", "");
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), data2);
        clickButton("Save and Finish");
        assertNoLabkeyErrors();

        log("** Test the TargetStudy renderer resolved all studies");
        clickAndWait(Locator.linkWithText(TEST_RUN1));
        // all target study values should render as either [None] or the name of the study
        assertTextNotPresent("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1);
        assertTextNotPresent("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY2);
        assertTextNotPresent("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY3);

        DataRegionTable table = new DataRegionTable("Data", this);
        Assert.assertEquals(_study1Label, table.getDataAsText(0, "Target Study"));
        Assert.assertEquals(_study1Label, table.getDataAsText(1, "Target Study"));
        Assert.assertEquals(_study1Label, table.getDataAsText(2, "Target Study"));
        //BUGBUG: target study renders as "" instead of "[None]"
        //Assert.assertEquals("[None]", table.getDataAsText(3, "Target Study"));
        Assert.assertEquals(" ", table.getDataAsText(3, "Target Study"));
        Assert.assertEquals(_study2Label, table.getDataAsText(4, "Target Study"));
        Assert.assertEquals(_study3Label, table.getDataAsText(5, "Target Study"));

        log("** Check SpecimenID resolved the PTID in the study");
        Assert.assertEquals("999320812", table.getDataAsText(0, "Participant ID"));
        Assert.assertEquals("999320396", table.getDataAsText(1, "Participant ID"));
        Assert.assertEquals("999320396", table.getDataAsText(2, "Participant ID"));
        Assert.assertEquals(" ", table.getDataAsText(3, "Participant ID"));
        Assert.assertEquals("999320706", table.getDataAsText(4, "Participant ID"));
        Assert.assertEquals(" ", table.getDataAsText(5, "Participant ID"));
    }

    protected void copyToStudy()
    {
        DataRegionTable table = new DataRegionTable("Data", this);
        table.checkAllOnPage();
        clickButton("Copy to Study");

        log("** Check TargetStudy dropdowns");
        assertOptionEquals(table.xpath(0, 0).child("select[@name='targetStudy']"), "/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1 + " (" + _study1Label + ")");
        assertOptionEquals(table.xpath(1, 0).child("select[@name='targetStudy']"), "/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1 + " (" + _study1Label + ")");
        assertOptionEquals(table.xpath(2, 0).child("select[@name='targetStudy']"), "/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1 + " (" + _study1Label + ")");
        assertOptionEquals(table.xpath(3, 0).child("select[@name='targetStudy']"), "[None]");
        assertOptionEquals(table.xpath(4, 0).child("select[@name='targetStudy']"), "/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY2 + " (" + _study2Label + ")");
        assertOptionEquals(table.xpath(5, 0).child("select[@name='targetStudy']"), "/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY3 + " (" + _study3Label + ")");

        log("** Check ptid/visit matches for rows 0-2 and 4, no match for rows 3 and 5");
        assertAttributeContains(table.xpath(0, 1).child("img"), "src", "check.png");
        assertAttributeContains(table.xpath(1, 1).child("img"), "src", "check.png");
        assertAttributeContains(table.xpath(2, 1).child("img"), "src", "check.png");
        assertAttributeContains(table.xpath(3, 1).child("img"), "src", "cancel.png");
        assertAttributeContains(table.xpath(4, 1).child("img"), "src", "check.png");
        assertAttributeContains(table.xpath(5, 1).child("img"), "src", "cancel.png");

        clickButton("Re-Validate");
        assertTextPresent("You must specify a Target Study for all selected rows.");

        log("** Uncheck row 3 and 5");
        table.uncheckCheckbox(3);
        table.uncheckCheckbox(5);
        clickButton("Re-Validate");
        assertTextNotPresent("You must specify a Target Study for all selected rows.");

        log("** Copy to studies");
        clickButton("Copy to Study");

        beginAt("/study/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1 + "/dataset.view?datasetId=5001");
        DataRegionTable dataset = new DataRegionTable("Dataset", this);
        Assert.assertEquals(3, dataset.getDataRowCount());
        Assert.assertEquals("999320396", dataset.getDataAsText(0, "Participant ID"));
        Assert.assertEquals("999320396", dataset.getDataAsText(1, "Participant ID"));
        Assert.assertEquals("999320812", dataset.getDataAsText(2, "Participant ID"));

        beginAt("/study/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY2 + "/dataset.view?datasetId=5001");
        Assert.assertEquals(1, dataset.getDataRowCount());
        Assert.assertEquals("999320706", dataset.getDataAsText(0, "Participant ID"));

        beginAt("/study/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY3 + "/dataset.view?datasetId=5001");
        Assert.assertEquals(404, getResponseCode());
    }

}
