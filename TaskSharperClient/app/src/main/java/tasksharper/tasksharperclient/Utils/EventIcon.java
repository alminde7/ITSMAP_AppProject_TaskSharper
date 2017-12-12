package tasksharper.tasksharperclient.Utils;

import tasksharper.tasksharperclient.Models.Enums.EventType;
import tasksharper.tasksharperclient.R;

/**
 * Created by Mads X1 Carbon on 04-12-2017.
 */

public class EventIcon {
    public static int setIcon(EventType eventType){
        switch (eventType){
            case Task:
                return R.drawable.ic_menu_task;
            case Appointment:
                return R.drawable.ic_menu_appointment;
            case None:
                return R.drawable.ic_menu_calendar;
        }
        return R.drawable.ic_menu_calendar;
    }
}
