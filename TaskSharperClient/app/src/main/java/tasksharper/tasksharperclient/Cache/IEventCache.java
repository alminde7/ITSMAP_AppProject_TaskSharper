package tasksharper.tasksharperclient.Cache;

import java.util.List;

import tasksharper.tasksharperclient.Models.Enums.UpdateState;
import tasksharper.tasksharperclient.Models.Event;

/**
 * Created by alminde on 02/12/2017.
 */

public interface IEventCache {

    List<Event> getAll();

    List<Event> getTasks();

    List<Event> getAppointments();

    Event getById(String id);

    List<Event> getOfflineEvents();

    void add(Event event, UpdateState state);

    void update(Event event, UpdateState state);

    void remove(Event event);
}
