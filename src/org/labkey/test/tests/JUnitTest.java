/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.Runner;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONValue;
import junit.framework.TestSuite;
import junit.framework.TestResult;
import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.io.IOException;

/**
 * User: brittp
 * Date: Nov 30, 2005
 * Time: 10:53:59 PM
 */
public class JUnitTest extends TestSuite
{
    public JUnitTest() throws Exception
    {
    }

    @Override
    public void run(TestResult testResult)
    {
        log("\n\n=============== Starting " + getClass().getSimpleName() + Runner.getProgress() + " =================");
        try
        {
            super.run(testResult);
        }
        finally
        {
            log("=============== Completed " + getClass().getSimpleName() + Runner.getProgress() + " =================");
        }
    }

    // used when writing JUnitTest class name to the remainingTests.txt log file
    public String toString()
    {
        return getClass().getName();
    }

    private static class JUnitSeleniumHelper extends BaseWebDriverTest
    {
        public void unfail()
        {
            _testFailed = false;
        }

        protected String getProjectName() {return null;}
        protected void doTestSteps() throws Exception { }
        protected void doCleanup(boolean afterTest) { }
        public String getAssociatedModuleDirectory() { return null; }
    }

    // uck. use BaseSeleniumWebTest to ensure we're upgraded
    private static void upgradeHelper() throws Exception
    {
        // TODO: remove upgrade helper from JUnitTest and run before suite starts.
        JUnitSeleniumHelper helper = new JUnitSeleniumHelper();
        try
        {
            // sign in performs upgrade if necessary
            helper.signIn();
            helper.unfail();
        }
        catch (Exception e)
        {
            helper.dump();
            throw e;
        }
        catch (AssertionError a)
        {
            helper.dump();
            throw a;
        }
        finally
        {
            helper.tearDown();
        }
    }

    public static TestSuite suite() throws Exception
    {
        return suite(false);
    }

    public static TestSuite suite(boolean secondAttempt) throws Exception
    {
        HttpClient client = WebTestHelper.getHttpClient();
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpGet method = null;
        HttpResponse response = null;
        try
        {
            String url = WebTestHelper.getBaseURL() + "/junit/testlist.view?";
            method = new HttpGet(url);
            response = client.execute(method, context);
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK)
            {
                TestSuite remotesuite = new JUnitTest();

                String responseBody = WebTestHelper.getHttpResponseBody(response);
                if (responseBody.isEmpty())
                    throw new AssertionFailedError("Failed to fetch remote junit test list: empty response");

                Object json = JSONValue.parse(responseBody);
                if (json == null &&
                        responseBody.contains("<title>Upgrade Status</title>") ||
                        responseBody.contains("<title>Upgrade Modules</title>") ||
                        responseBody.contains("<title>Account Setup</title>") ||
                        responseBody.contains("This server is being upgraded to a new version of LabKey Server."))
                {
                    if (secondAttempt)
                        throw new AssertionFailedError("Failed to update or bootstrap on second attempt: " + responseBody);

                    // perform upgrade then try to fetch the list again
                    upgradeHelper();
                    return suite(true);
                }

                if (json == null || !(json instanceof Map))
                    throw new AssertionFailedError("Can't parse or cast json response: " + responseBody);

                Map<String, List<String>> obj = (Map<String, List<String>>)json;
                for (Map.Entry<String, List<String>> entry : obj.entrySet())
                {
                    String suiteName = entry.getKey();
                    List<String> arr = entry.getValue();
                    TestSuite testsuite = new TestSuite(suiteName);
                    for (String test : arr)
                    {
                        testsuite.addTest(new RemoteTest(test));
                    }
                    remotesuite.addTest(testsuite);
                }

                return remotesuite;
            }
            else
            {
                System.err.println("Getting unit test list from server failed with error code " + status + ". Error page content is:");
                response.getEntity().writeTo(System.err);
                throw new AssertionFailedError("Failed to fetch remote junit test list (" + status + " - " + response.getStatusLine() + "): " + url);
            }
        }
        finally
        {
            if (response != null)
                EntityUtils.consume(response.getEntity());
            if (client != null)
                client.getConnectionManager().shutdown();
        }
    }

    public static class RemoteTest extends TestCase
    {
        String _remoteClass;

        public RemoteTest(String remoteClass)
        {
            super(remoteClass);
            _remoteClass = remoteClass;
        }

        @Override
        protected void runTest() throws Throwable
        {
            HttpClient client = WebTestHelper.getHttpClient();
            HttpContext context = WebTestHelper.getBasicHttpContext();
            HttpGet method = null;
            HttpResponse response = null;
            try
            {
                String url = WebTestHelper.getBaseURL() + "/junit/go.view?testCase=" + _remoteClass;
                method = new HttpGet(url);
                response = client.execute(method, context);
                int status = response.getStatusLine().getStatusCode();
                String responseBody = WebTestHelper.getHttpResponseBody(response);

                if (status == HttpStatus.SC_OK)
                {
                    log("remote junit successful: " + _remoteClass);
                    log(dump(responseBody));
                }
                else
                {
                    log("remote junit failed: " + _remoteClass);
                    Assert.fail("remote junit failed: " + _remoteClass + "\n" + dump(responseBody));
                }
            }
            catch (IOException ioe)
            {
                Assert.fail("failed to run remote junit: " + ioe.getMessage());
            }
            finally
            {
                if (response != null)
                    EntityUtils.consume(response.getEntity());
                if (client != null)
                    client.getConnectionManager().shutdown();
            }
        }

        static String dump(String response)
        {
            Map<String, Object> json = null;
            try
            {
                json = (Map<String, Object>)JSONValue.parse(response);
            }
            catch (Exception e)
            {
                // ignore
            }

            if (json == null)
                return response;

            StringBuilder sb = new StringBuilder();
            sb.append("ran: ").append(json.get("runCount"));
//            sb.append(", errors: ").append(json.get("errorCount"));
            sb.append(", failed: ").append(json.get("failureCount")).append("\n");
//            dumpFailures(sb, (List<Map<String, Object>>) json.get("errors"));
            dumpFailures(sb, (List<Map<String, Object>>) json.get("failures"));
            return sb.toString();
        }

        static void dumpFailures(StringBuilder sb, List<Map<String, Object>> failures)
        {
            for (Map<String, Object> failure : failures)
            {
                if (failure.get("failedTest") != null)
                    sb.append(failure.get("failedTest")).append("\n");
                if (failure.get("exceptionMesage") != null)
                    sb.append("  ").append(failure.get("exceptionMessage")).append("\n");
                if (failure.get("trace") != null)
                    sb.append("  ").append(failure.get("trace")).append("\n");
                sb.append("\n");
            }
        }

    }

    static void log(String str)
    {
        if (str == null || str.length() == 0)
            return;
        String d = new SimpleDateFormat("HH:mm:ss,SSS").format(new Date());      // Include time with log entry.  Use format that matches labkey log.
        System.out.println(d + " " + str);
    }
}
