package tasksharper.tasksharperclient.Notification;

import tasksharper.tasksharperclient.Models.Event;

/**
 * Created by alminde on 02/12/2017.
 */

public interface IEventNotification {

    void attach(Event event);

    void detatch(Event event);
}
