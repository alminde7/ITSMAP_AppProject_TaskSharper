package tasksharper.tasksharperclient.Cache;

import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import tasksharper.tasksharperclient.Models.Enums.EventType;
import tasksharper.tasksharperclient.Models.Enums.ModificationState;
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
    public List<Event> getByDate(Date date){
        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("modification_state").notEq(ModificationState.Deleted)).list();
        List<Event> parsedEvents = new ArrayList<>();

        for (Iterator<EventDbModel> i = events.iterator(); i.hasNext();){
            EventDbModel event = i.next();
            if (getZeroTimeDate(event.start).equals(getZeroTimeDate(date))){
                parsedEvents.add(EventParser.Parse(event));
            }
        }

        return parsedEvents;
    }

    @Override
    public List<Event> getTasks() {
        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("type").eq(EventType.Task)).where(Condition.prop("modification_state").notEq(ModificationState.Deleted)).list();

        return EventParser.ParseDbModels(events);
    }

    @Override
    public List<Event> getTasksBetweenDates(Date fromDate, Date toDate){
        Date zeroTimeFromDate = getZeroTimeDate(fromDate);
        Date zeroTimeToDate = getZeroTimeDate(toDate);

        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("type").eq(EventType.Task)).where(Condition.prop("modification_state").notEq(ModificationState.Deleted)).list();
        List<Event> parsedEvents = new ArrayList<>();

        for (Iterator<EventDbModel> i = events.iterator(); i.hasNext();){
            EventDbModel event = i.next();
            Date zeroTimeDate = getZeroTimeDate(event.start);
            if ((zeroTimeDate.after(zeroTimeFromDate) || zeroTimeDate.equals(zeroTimeFromDate)) && (zeroTimeDate.before(zeroTimeToDate) || zeroTimeDate.equals(zeroTimeToDate))){
                parsedEvents.add(EventParser.Parse(event));
            }
        }

        return parsedEvents;
    }

    @Override
    public List<Event> getAppointments() {
        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("type").eq(EventType.Appointment)).where(Condition.prop("modification_state").notEq(ModificationState.Deleted)).list();

        return EventParser.ParseDbModels(events);
    }

    @Override
    public List<Event> getAppointmentsBetweenDates(Date fromDate, Date toDate){
        Date zeroTimeFromDate = getZeroTimeDate(fromDate);
        Date zeroTimeToDate = getZeroTimeDate(toDate);

        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("type").eq(EventType.Appointment)).where(Condition.prop("modification_state").notEq(ModificationState.Deleted)).list();
        List<Event> parsedEvents = new ArrayList<>();

        for (Iterator<EventDbModel> i = events.iterator(); i.hasNext();){
            EventDbModel event = i.next();
            Date zeroTimeDate = getZeroTimeDate(event.start);
            if ((zeroTimeDate.after(zeroTimeFromDate) || zeroTimeDate.equals(zeroTimeFromDate)) && (zeroTimeDate.before(zeroTimeToDate) || zeroTimeDate.equals(zeroTimeToDate))){
                parsedEvents.add(EventParser.Parse(event));
            }
        }

        return parsedEvents;
    }

    @Override
    public Event getById(String id) {
        EventDbModel event = Select.from(EventDbModel.class).where(Condition.prop("id").eq(id)).first();

        return EventParser.Parse(event);
    }

    @Override
    public EventDbModel getById(long id){
        return Select.from(EventDbModel.class).where(Condition.prop("id").eq(id)).first();
    }

    @Override
    public Event getByRecordId(String recordId) {
        EventDbModel event = Select.from(EventDbModel.class).where(Condition.prop("record_id").eq(recordId)).first();
        if (event != null){
            return EventParser.Parse(event);
        }
        return null;
    }

    @Override
    public List<Event> getOfflineEvents() {
        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("state").eq(UpdateState.Offline)).list();

        return EventParser.ParseDbModels(events);
    }

    @Override
    public List<Event> getOfflineToBeCreatedEvents() {
        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("state").eq(UpdateState.Offline)).where(Condition.prop("modification_state").eq(ModificationState.Created)).list();

        return EventParser.ParseDbModels(events);
    }

    @Override
    public List<Event> getOfflineToBeUpdatedEvents() {
        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("state").eq(UpdateState.Offline)).where(Condition.prop("modification_state").eq(ModificationState.Updated)).list();

        return EventParser.ParseDbModels(events);
    }

    @Override
    public List<Event> getOfflineToBeDeletedEvents() {
        List<EventDbModel> events = Select.from(EventDbModel.class).where(Condition.prop("state").eq(UpdateState.Offline)).where(Condition.prop("modification_state").eq(ModificationState.Deleted)).list();

        return EventParser.ParseDbModels(events);
    }

    @Override
    public void add(Event event, UpdateState state) {
        EventDbModel eventDbModel = EventParser.Parse(event);

        eventDbModel.state = state;
        eventDbModel.modificationState = ModificationState.Created;
        eventDbModel.lastUpdated = new Date();

        long result = eventDbModel.save();
    }

    @Override
    public void update(Event event, UpdateState state) {
        EventDbModel eventDbModel = EventParser.Parse(event);

        eventDbModel.state = state;
        eventDbModel.modificationState = ModificationState.Updated;
        eventDbModel.lastUpdated = new Date();

        long result = eventDbModel.update();
    }

    @Override
    public void setToBeRemoved(String id, UpdateState state){
        long dbId;
        try {
            dbId = Long.parseLong(id, 10);
        } catch(NumberFormatException ex) {
            dbId = 0;
        }

        EventDbModel eventDbModel = getById(dbId);
        eventDbModel.state = state;
        eventDbModel.modificationState = ModificationState.Deleted;
        eventDbModel.lastUpdated = new Date();
        if (eventDbModel != null){
            long result = eventDbModel.update();
        }
    }

    @Override
    public void remove(String id) {
        long dbId;
        try {
            dbId = Long.parseLong(id, 10);
        } catch(NumberFormatException ex) {
            dbId = 0;
        }

        EventDbModel eventDbModel = getById(dbId);
        if (eventDbModel != null){
            boolean result = eventDbModel.delete();
        }
    }

    @Override
    public void removeAllEvents(){
        List<EventDbModel> events = EventDbModel.listAll(EventDbModel.class);
        for (Iterator<EventDbModel> i = events.iterator(); i.hasNext();){
            EventDbModel event = i.next();
            event.delete();
        }
    }

    private static Date getZeroTimeDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        date = calendar.getTime();
        return date;
    }
}
