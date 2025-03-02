package org.labkey.remoteapi.announcements;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class MessageThreadResponse extends CommandResponse
{
    private final AnnouncementModel _announcementModel;

    public MessageThreadResponse(String text, int statusCode, String contentType, JSONObject json)
    {
        super(text, statusCode, contentType, json);
        _announcementModel = new AnnouncementModel((JSONObject) json.get("data"));
    }

    public AnnouncementModel getAnnouncementModel()
    {
        return _announcementModel;
    }
}
