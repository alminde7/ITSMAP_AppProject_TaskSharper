package tasksharper.tasksharperclient.Models;

import java.util.Date;

import tasksharper.tasksharperclient.Models.Enums.EventStatus;
import tasksharper.tasksharperclient.Models.Enums.EventType;

/**
 * Created by alminde on 02/12/2017.
 */

public class Event {

    public String id;
    public String title;
    public String description;

    public Date start;
    public Date end;

    public EventStatus status;
    public EventType type;

    public boolean markedAsDone;
}
