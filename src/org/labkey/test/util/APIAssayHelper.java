/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.assay.AssayListCommand;
import org.labkey.remoteapi.assay.AssayListResponse;
import org.labkey.remoteapi.assay.Batch;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.assay.Run;
import org.labkey.remoteapi.assay.SaveAssayBatchCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class APIAssayHelper extends AbstractAssayHelper
{

    public APIAssayHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @LogMethod(quiet = true)
    public void importAssay(int assayID, File file, String projectPath, Map<String, Object> batchProperties)  throws CommandException, IOException
    {
        ImportRunCommand  irc = new ImportRunCommand(assayID, file);
        irc.setBatchProperties(batchProperties);
        irc.setTimeout(180000); // Wait 3 minutes for assay import
        irc.execute(_test.createDefaultConnection(false), "/" + projectPath);

    }

    public void importAssay(String assayName, File file, String projectPath) throws CommandException, IOException
    {
        importAssay(assayName, file, projectPath, Collections.<String, Object>singletonMap("ParticipantVisitResolver", "SampleInfo"));
    }

    public void importAssay(String assayName, File file, String projectPath, @Nullable Map<String, Object> batchProperties) throws CommandException, IOException
    {
        importAssay(getIdFromAssayName(assayName, projectPath), file, projectPath, batchProperties);
    }

    @Override
    protected void goToUploadXarPage()
    {
        _test.beginAt(WebTestHelper.buildURL("experiment", _test.getCurrentContainerPath(), "showAddXarFile"));
    }

    private int getIdFromAssayName(String assayName, String projectPath)
    {
        AssayListCommand alc = new AssayListCommand();
        alc.setName(assayName);
        AssayListResponse alr = null;
        try
        {
            alr = alc.execute(_test.createDefaultConnection(false), "/" + projectPath);
        }
        catch (CommandException | IOException e)
        {
            if(e.getMessage().contains("Not Found"))
                throw new AssertionError("Assay or project not found", e);
            else
                throw new RuntimeException(e);
        }

        if(alr.getDefinition(assayName)==null)
            fail("Assay not found");
        return ((Long) alr.getDefinition(assayName).get("id")).intValue();

    }

    public void saveBatch(String assayName, String runName, List<Map<String, Object>> resultRows, String projectName) throws IOException, CommandException
    {
        int assayId = getIdFromAssayName(assayName, projectName);

        Batch batch = new Batch();
        List<Run> runs = new ArrayList<>();
        Run run = new Run();
        run.setName(runName);
        run.setResultData(resultRows);
        runs.add(run);
        batch.setRuns(runs);

        saveBatch(assayId, batch, projectName);
    }

    public void saveBatch(int assayId, Batch batch, String projectPath) throws IOException, CommandException
    {
        SaveAssayBatchCommand cmd = new SaveAssayBatchCommand(assayId, batch);
        cmd.setTimeout(180000); // Wait 3 minutes for assay import
        cmd.execute(_test.createDefaultConnection(false), "/" + projectPath);
    }
}
