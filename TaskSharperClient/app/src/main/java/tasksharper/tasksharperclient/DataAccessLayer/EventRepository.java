package tasksharper.tasksharperclient.DataAccessLayer;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.model.*;
import com.google.api.services.calendar.model.Event;

import android.os.AsyncTask;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import tasksharper.tasksharperclient.Cache.EventCache;
import tasksharper.tasksharperclient.Cache.IEventCache;
import tasksharper.tasksharperclient.Models.AuthErrorEvent;
import tasksharper.tasksharperclient.Models.Enums.EventType;
import tasksharper.tasksharperclient.Models.Enums.UpdateState;
import tasksharper.tasksharperclient.Models.NewDataEvent;


/**
 * Created by alminde on 02/12/2017.
 */

public class EventRepository implements IEventRepository  {
    private com.google.api.services.calendar.Calendar mService = null;
    private Exception mLastError = null;

    public EventRepository(GoogleAccountCredential credential){
        // Initialize credentials and service object.
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("TaskSharper")
                .build();
    }

    @Override
    public void SynchronizeEvents() {
        final IEventCache cache = new EventCache();
        AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... objects) {


                Events events = null;
                try {
                    List<tasksharper.tasksharperclient.Models.Event> offlineEventsToBeCreated = cache.getOfflineToBeCreatedEvents();
                    List<tasksharper.tasksharperclient.Models.Event> offlineEventsToBeUpdated = cache.getOfflineToBeUpdatedEvents();
                    List<tasksharper.tasksharperclient.Models.Event> offlineEventsToBeDeleted = cache.getOfflineToBeDeletedEvents();

                    for (Iterator<tasksharper.tasksharperclient.Models.Event> i = offlineEventsToBeCreated.iterator(); i.hasNext();) {
                        tasksharper.tasksharperclient.Models.Event item = i.next();
                        item.recordId = null;
                        Event createdEvent = mService.events().insert("primary", ConvertFromModelToGoogleEvent(item)).execute();
                        cache.remove(item.id);
                    }

                    for (Iterator<tasksharper.tasksharperclient.Models.Event> i = offlineEventsToBeUpdated.iterator(); i.hasNext();) {
                        tasksharper.tasksharperclient.Models.Event item = i.next();
                        Event createdEvent = mService.events().update("primary", item.recordId, ConvertFromModelToGoogleEvent(item)).execute();
                        cache.remove(item.id);
                    }

                    for (Iterator<tasksharper.tasksharperclient.Models.Event> i = offlineEventsToBeDeleted.iterator(); i.hasNext();) {
                        tasksharper.tasksharperclient.Models.Event item = i.next();
                        mService.events().delete("primary", item.recordId).execute();
                        cache.remove(item.id);
                    }

                    events = mService.events().list("primary")
                            .setOrderBy("startTime")
                            .setSingleEvents(true)
                            .setShowDeleted(true)
                            .execute();

                    List<Event> items = events.getItems();
                    for (Iterator<Event> i = items.iterator(); i.hasNext();) {
                        Event item = i.next();
                        tasksharper.tasksharperclient.Models.Event ev = ConvertFromGoogleToModelEvent(item);
                        tasksharper.tasksharperclient.Models.Event cachedEvent = cache.getByRecordId(ev.recordId);
                        if (item.getStatus().equals("cancelled")){
                            if (cachedEvent != null){
                                cache.remove(cachedEvent.id);
                            }
                        } else {
                            if (cachedEvent == null){
                                cache.add(ev, UpdateState.Online);
                            } else {
                                cachedEvent.title = ev.title;
                                cachedEvent.description = ev.description;
                                cachedEvent.type = ev.type;
                                cachedEvent.start = ev.start;
                                cachedEvent.end = ev.end;
                                cachedEvent.markedAsDone = ev.markedAsDone;
                                cache.update(cachedEvent, UpdateState.Online);
                            }
                        }

                    }
                    return items;
                } catch (IOException e) {
                    try {
                        EventBus.getDefault().post(new AuthErrorEvent((UserRecoverableAuthIOException)e));
                    } catch(Exception ex){
                        ex.printStackTrace();
                    }

                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object result){
                EventBus.getDefault().post(new NewDataEvent());
            }
        };

            task.execute();
    }

    @Override
    public void add(final tasksharper.tasksharperclient.Models.Event event) {
        final IEventCache cache = new EventCache();
        AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... objects) {


                Event insertedEvent = null;
                try {
                    insertedEvent = mService.events().insert("primary", ConvertFromModelToGoogleEvent(event)).execute();
                    if (insertedEvent != null){
                        tasksharper.tasksharperclient.Models.Event modelEvent = ConvertFromGoogleToModelEvent(insertedEvent);
                        tasksharper.tasksharperclient.Models.Event cachedEvent = cache.getByRecordId(modelEvent.recordId);
                        if (cachedEvent == null){
                            cache.add(modelEvent, UpdateState.Online);
                        } else {
                            cachedEvent.title = modelEvent.title;
                            cachedEvent.description = modelEvent.description;
                            cachedEvent.type = modelEvent.type;
                            cachedEvent.start = modelEvent.start;
                            cachedEvent.end = modelEvent.end;
                            cachedEvent.markedAsDone = modelEvent.markedAsDone;
                            cache.update(cachedEvent, UpdateState.Online);
                        }

                    }
                    return insertedEvent;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object result){
                EventBus.getDefault().post(new NewDataEvent());
            }
        };

        task.execute();
    }

    @Override
    public void update(final tasksharper.tasksharperclient.Models.Event event) {
        final IEventCache cache = new EventCache();
        AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... objects) {

                Event updatedEvent = null;
                try {
                    updatedEvent = mService.events().update("primary", event.recordId, ConvertFromModelToGoogleEvent(event)).execute();
                    if (updatedEvent != null){
                        tasksharper.tasksharperclient.Models.Event modelEvent = ConvertFromGoogleToModelEvent(updatedEvent);
                        tasksharper.tasksharperclient.Models.Event cachedEvent = cache.getByRecordId(modelEvent.recordId);
                        if (cachedEvent == null){
                            cache.add(ConvertFromGoogleToModelEvent(updatedEvent), UpdateState.Online);
                        } else {
                            cachedEvent.title = modelEvent.title;
                            cachedEvent.description = modelEvent.description;
                            cachedEvent.type = modelEvent.type;
                            cachedEvent.start = modelEvent.start;
                            cachedEvent.end = modelEvent.end;
                            cachedEvent.markedAsDone = modelEvent.markedAsDone;
                            cache.update(cachedEvent, UpdateState.Online);
                        }
                    }
                    return updatedEvent;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object result){
                EventBus.getDefault().post(new NewDataEvent());
            }
        };

        task.execute();
    }

    @Override
    public void remove(final String id, final String recordId) {
        final IEventCache cache = new EventCache();
        AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... objects) {

                try {
                    mService.events().delete("primary", recordId).execute();
                    cache.remove(id);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object result){
                EventBus.getDefault().post(new NewDataEvent());
            }
        };

        task.execute();
    }

    private tasksharper.tasksharperclient.Models.Event ConvertFromGoogleToModelEvent(Event event){
        tasksharper.tasksharperclient.Models.Event newEvent = new tasksharper.tasksharperclient.Models.Event();
        newEvent.title = event.getSummary();
        newEvent.description = event.getDescription();
        Event.ExtendedProperties properties = event.getExtendedProperties();
        String type;
        String markedAsDone;
        if (properties != null){
            type = properties.getShared().get("Type");
            markedAsDone = properties.getShared().get("MarkedAsDone");
        } else {
            type = "None";
            markedAsDone = "False";
        }

        newEvent.type = EventType.valueOf(type);
        newEvent.markedAsDone = Boolean.valueOf(markedAsDone);
        newEvent.recordId = event.getId();
        EventDateTime start = event.getStart();
        EventDateTime end = event.getEnd();
        if (start.getDateTime() != null) {
            newEvent.start = new Date(start.getDateTime().getValue());
        } else {
            newEvent.start = new Date(start.getDate().getValue());
        }
        if (end.getDateTime() != null){
            newEvent.end = new Date(end.getDateTime().getValue());
        } else {
            newEvent.end = new Date(end.getDate().getValue());
        }

        return newEvent;
    }

    private Event ConvertFromModelToGoogleEvent(tasksharper.tasksharperclient.Models.Event event){
        Event newEvent = new Event();
        newEvent.setSummary(event.title);
        newEvent.setDescription(event.description);
        newEvent.setId(event.recordId);
        newEvent.setStart(new EventDateTime().setDateTime(new DateTime(event.start.getTime())));
        newEvent.setEnd(new EventDateTime().setDateTime(new DateTime(event.end.getTime())));
        newEvent.setExtendedProperties(new Event.ExtendedProperties());
        newEvent.getExtendedProperties().setShared(new HashMap<String, String>());
        newEvent.getExtendedProperties().getShared().put("Type", event.type.toString());
        newEvent.getExtendedProperties().getShared().put("MarkedAsDone", Boolean.toString(event.markedAsDone));

        return newEvent;
    }
}
