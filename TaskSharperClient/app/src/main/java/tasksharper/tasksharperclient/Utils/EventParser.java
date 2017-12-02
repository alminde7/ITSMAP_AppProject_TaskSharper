package tasksharper.tasksharperclient.Utils;

import java.util.ArrayList;
import java.util.List;

import tasksharper.tasksharperclient.Models.Event;
import tasksharper.tasksharperclient.Models.EventDbModel;

/**
 * Created by alminde on 02/12/2017.
 */

public class EventParser {

    public static Event Parse(EventDbModel eventDbModel){
        Event event = new Event();

        event.id = eventDbModel.id;
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

        eventDbModel.id = event.id;
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
