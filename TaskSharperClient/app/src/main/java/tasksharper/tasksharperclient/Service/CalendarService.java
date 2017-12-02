package tasksharper.tasksharperclient.Service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.orm.SugarContext;

import java.util.List;

import tasksharper.tasksharperclient.Cache.EventCache;
import tasksharper.tasksharperclient.Cache.IEventCache;
import tasksharper.tasksharperclient.Models.Event;
import tasksharper.tasksharperclient.Notification.EventNotification;
import tasksharper.tasksharperclient.Notification.IEventNotification;

/**
 * Created by alminde on 02/12/2017.
 */

public class CalendarService extends Service {

    private IEventCache mCache;
    private IEventNotification mNotification;

    private boolean mRunning;

    // Setup binder that can be returned to activities.
    private final IBinder binder = new CalendarServiceBinder();
    public class CalendarServiceBinder extends Binder {
        CalendarService getService() {
            return CalendarService.this;
        }
    }

    // Methods publicly available for the binder :)
    public List<Event> getAllEvents(){
        return mCache.getAll();
    }

    public List<Event> getAllTasks(){
        return mCache.getTasks();
    }

    public List<Event> getAllAppointments(){
        return mCache.getAppointments();
    }

    public void addEvent(Event newEvent){

        // add to API

        // check network connectivity;

    }
    // End public binder methods

    @Override
    public void onCreate() {
        super.onCreate();

        // Init DB connection - https://gamedevalgorithms.com/2017/07/07/getting-started-with-sugar-orm/
        SugarContext.init(this);
        mCache = new EventCache();

        mNotification = new EventNotification(
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE),
                getResources(),
                this);

        mRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        if(!mRunning){
            mRunning = true;

            // do start stuff;
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy(){
        // Close DB connection
        SugarContext.terminate();

        super.onDestroy();
    }

}
