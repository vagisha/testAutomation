/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.test.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.util.JSONPObject;

import java.util.List;
import java.util.Map;

/**
 * Bean for an individual server's credentials
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Server
{
    @JsonProperty(required = true) private String key;
    private String host;
    private List<Login> logins;
    private List<ApiKey> apiKeys;
    private Map<String, Object> extraValues;

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public List<Login> getLogins()
    {
        return logins;
    }

    public void setLogins(List<Login> logins)
    {
        this.logins = logins;
    }

    public List<ApiKey> getApiKeys()
    {
        return apiKeys;
    }

    public void setApiKeys(List<ApiKey> apiKeys)
    {
        this.apiKeys = apiKeys;
    }

    public Map<String, Object> getExtraValues()
    {
        return extraValues;
    }

    public void setExtraValues(Map<String, Object> extraValues)
    {
        this.extraValues = extraValues;
    }
}
