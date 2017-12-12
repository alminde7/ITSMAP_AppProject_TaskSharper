package tasksharper.tasksharperclient.Models;

import com.orm.SugarRecord;
import com.orm.dsl.Unique;

import java.util.Date;

import tasksharper.tasksharperclient.Models.Enums.EventStatus;
import tasksharper.tasksharperclient.Models.Enums.EventType;
import tasksharper.tasksharperclient.Models.Enums.ModificationState;
import tasksharper.tasksharperclient.Models.Enums.UpdateState;

/**
 * Created by alminde on 02/12/2017.
 */

public class EventDbModel extends SugarRecord {

    @Unique
    public long id;
    public String recordId;
    public String title;
    public String description;

    public Date start;
    public Date end;

    public EventStatus status;
    public EventType type;

    public boolean markedAsDone;

    public UpdateState state;
    public ModificationState modificationState;
    public Date lastUpdated;

    public EventDbModel(){

    }
}
