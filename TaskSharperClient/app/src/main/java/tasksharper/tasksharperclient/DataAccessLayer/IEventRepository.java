package tasksharper.tasksharperclient.DataAccessLayer;

import android.content.Context;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import tasksharper.tasksharperclient.Models.Enums.UpdateState;
import tasksharper.tasksharperclient.Models.Event;

/**
 * Created by alminde on 02/12/2017.
 */

public interface IEventRepository {
    void SynchronizeEvents();
    void add(Event event);
    void update(Event event);

    void remove(String id, String dbId);

}
