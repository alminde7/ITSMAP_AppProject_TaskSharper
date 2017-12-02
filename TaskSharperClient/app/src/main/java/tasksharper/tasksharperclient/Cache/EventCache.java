package tasksharper.tasksharperclient.Cache;

import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.Date;
import java.util.List;

import tasksharper.tasksharperclient.Models.Enums.EventType;
import tasksharper.tasksharperclient.Models.Enums.UpdateState;
import tasksharper.tasksharperclient.Models.Event;
import tasksharper.tasksharperclient.Models.EventDbModel;
import tasksharper.tasksharperclient.Utils.EventParser;

/**
 * Created by alminde on 02/12/2017.
 */

public class EventCache implements IEventCache {


    @Override
    public List<Event> getAll() {
        List<EventDbModel> events = EventDbModel.listAll(EventDbModel.class);

        return EventParser.ParseDbModels(events);
    }

    @Override
    public List<Event> getTasks() {
        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("type").eq(EventType.Task)).list();

        return EventParser.ParseDbModels(events);
    }

    @Override
    public List<Event> getAppointments() {
        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("type").eq(EventType.Apointment)).list();

        return EventParser.ParseDbModels(events);
    }

    @Override
    public Event getById(String id) {
        EventDbModel event = Select.from(EventDbModel.class).where(Condition.prop("id").eq(id)).first();

        return EventParser.Parse(event);
    }

    @Override
    public List<Event> getOfflineEvents() {
        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("state").eq(UpdateState.Offline)).list();

        return EventParser.ParseDbModels(events);
    }

    @Override
    public void add(Event event, UpdateState state) {
        EventDbModel eventDbModel = EventParser.Parse(event);

        eventDbModel.state = state;
        eventDbModel.lastUpdated = new Date();

        long result = eventDbModel.save();
    }

    @Override
    public void update(Event event, UpdateState state) {
        EventDbModel eventDbModel = EventParser.Parse(event);

        eventDbModel.state = state;
        eventDbModel.lastUpdated = new Date();

        long result = eventDbModel.update();
    }

    @Override
    public void remove(Event event) {
        EventDbModel eventDbModel = EventParser.Parse(event);

        boolean result = eventDbModel.delete();
    }
}
