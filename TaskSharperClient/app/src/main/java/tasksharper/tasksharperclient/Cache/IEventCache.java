package tasksharper.tasksharperclient.Cache;

import java.util.Date;
import java.util.List;

import tasksharper.tasksharperclient.Models.Enums.UpdateState;
import tasksharper.tasksharperclient.Models.Event;
import tasksharper.tasksharperclient.Models.EventDbModel;

/**
 * Created by alminde on 02/12/2017.
 */

public interface IEventCache {

    List<Event> getAll();

    List<Event> getByDate(Date date);

    List<Event> getTasks();
    List<Event> getTasksBetweenDates(Date fromDate, Date toDate);

    List<Event> getAppointments();
    List<Event> getAppointmentsBetweenDates(Date fromDate, Date toDate);

    Event getById(String id);

    EventDbModel getById(long id);

    Event getByRecordId(String recordId);

    List<Event> getOfflineEvents();

    List<Event> getOfflineToBeCreatedEvents();

    List<Event> getOfflineToBeUpdatedEvents();

    List<Event> getOfflineToBeDeletedEvents();

    void add(Event event, UpdateState state);

    void update(Event event, UpdateState state);

    void setToBeRemoved(String id, UpdateState state);

    void remove(String id);
    void removeAllEvents();
}
