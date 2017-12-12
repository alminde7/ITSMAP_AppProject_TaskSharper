package tasksharper.tasksharperclient.Utils;

import com.orm.SugarRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import tasksharper.tasksharperclient.Models.Event;
import tasksharper.tasksharperclient.Models.EventDbModel;

/**
 * Created by alminde on 02/12/2017.
 */

public class EventParser {

    public static Event Parse(EventDbModel eventDbModel){
        Event event = new Event();

        event.id = Long.toString(eventDbModel.id);
        event.recordId = eventDbModel.recordId;
        event.title = eventDbModel.title;
        event.description = eventDbModel.description;
        event.start = eventDbModel.start;
        event.end = eventDbModel.end;
        event.status = eventDbModel.status;
        event.type = eventDbModel.type;
        event.markedAsDone = eventDbModel.markedAsDone;

        return event;
    }

    public static EventDbModel Parse(Event event){
        EventDbModel eventDbModel = new EventDbModel();

        try {
            eventDbModel.id = Long.parseLong(event.id, 10) ;
        } catch(NumberFormatException ex){
            // id is probably "null" (as a string...) in which case this can be ignored because the event is about to be created
        }

        if (event.recordId == null) {
            eventDbModel.recordId = UUID.randomUUID().toString();
        } else {
            eventDbModel.recordId = event.recordId;
        }
        eventDbModel.title = event.title;
        eventDbModel.description = event.description;
        eventDbModel.start = event.start;
        eventDbModel.end = event.end;
        eventDbModel.status = event.status;
        eventDbModel.type = event.type;
        eventDbModel.markedAsDone = event.markedAsDone;

        return eventDbModel;
    }

    public static List<Event> ParseDbModels(List<EventDbModel> eventDbModelList){
        List<Event> events = new ArrayList<Event>();

        for (EventDbModel eventDbModel: eventDbModelList){
            events.add(EventParser.Parse(eventDbModel));
        }

        return events;
    }

    public static List<EventDbModel> ParseEvents(List<Event> events){
        List<EventDbModel> eventDbModels = new ArrayList<EventDbModel>();

        for(Event event: events){
            eventDbModels.add(EventParser.Parse(event));
        }

        return eventDbModels;
    }

}
