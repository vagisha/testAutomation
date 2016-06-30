/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.test;

import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.categories.Continue;
import org.labkey.test.categories.Test;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SuiteBuilder
{
    private static SuiteBuilder _instance = null;
    private static Map<String, Set<Class>> _suites;

    private SuiteBuilder()
    {
        _suites = new CaseInsensitiveHashMap<>();
        loadSuites();
    }

    public static SuiteBuilder getInstance()
    {
        if (_instance == null)
        {
            _instance = new SuiteBuilder();
        }

        return _instance;
    }

    private void loadSuites()
    {
        List<String> testPackages = new ArrayList<>(Arrays.asList(System.getProperty("test.packages", "org.labkey.test").split("[^0-9A-Za-z\\._]+")));
        List<String> extraTestPackages = Arrays.asList(System.getProperty("extra.test.packages", "").split("[^0-9A-Za-z\\._]+"));
        testPackages.addAll(extraTestPackages);

        Set<Class<?>> tests = new HashSet<>();

        for (String testPackage : testPackages)
        {
            Reflections reflections = new Reflections(testPackage);
            tests.addAll(reflections.getTypesAnnotatedWith(Category.class));
        }

        _suites.put(Continue.class.getSimpleName(), new HashSet<>()); // Not actually a suite, used to continue interrupted suite

        for (Class test : tests)
        {
            for (Class category : ((Category)test.getAnnotation(Category.class)).value())
            {
                addTestToSuite(test, category.getSimpleName());
                Class supercategory = category.getSuperclass();

                while (Test.class.isAssignableFrom(supercategory))
                {
                    addTestToSuite(test, supercategory.getSimpleName());
                    supercategory = supercategory.getSuperclass();
                }
            }

            // parse test package, add module-derived suites. We expect these to follow the pattern
            //    <testPackage>.tests.<moduleName>.<testClassName>
            List<String> packageNameParts = Arrays.asList(test.getPackage().getName().split("\\."));
            int testsPkgIndex = packageNameParts.indexOf("tests");
            for (int i = testsPkgIndex + 1; testsPkgIndex > -1 && i < packageNameParts.size(); i++)
            {
                addTestToSuite(test, packageNameParts.get(i));
            }
            addTestToSuite(test, Test.class.getSimpleName()); // Make sure test is in the master "Test" suite
        }
    }

    private void addTestToSuite(Class test, String suiteName)
    {
        if (!_suites.containsKey(suiteName.toLowerCase()))
            _suites.put(suiteName, new HashSet<>());

        _suites.get(suiteName).add(test);
    }

    public TestSet getTestSet(String suiteName)
    {
        return new TestSet(_suites.get(suiteName),suiteName);
    }

    public Set<String> getSuites()
    {
        return _suites.keySet();
    }
}
