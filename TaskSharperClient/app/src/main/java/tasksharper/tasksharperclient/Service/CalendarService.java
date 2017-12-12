package tasksharper.tasksharperclient.Service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.orm.SchemaGenerator;
import com.orm.SugarContext;
import com.orm.SugarDb;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import tasksharper.tasksharperclient.Cache.EventCache;
import tasksharper.tasksharperclient.Cache.IEventCache;
import tasksharper.tasksharperclient.DataAccessLayer.EventRepository;
import tasksharper.tasksharperclient.DataAccessLayer.IEventRepository;
import tasksharper.tasksharperclient.Models.Enums.EventType;
import tasksharper.tasksharperclient.Models.Enums.UpdateState;
import tasksharper.tasksharperclient.Models.Event;
import tasksharper.tasksharperclient.Models.EventDbModel;
import tasksharper.tasksharperclient.Models.NoInternetEvent;
import tasksharper.tasksharperclient.Notification.EventNotification;
import tasksharper.tasksharperclient.Notification.IEventNotification;
import tasksharper.tasksharperclient.R;
import tasksharper.tasksharperclient.Utils.Globals;

/**
 * Created by alminde on 02/12/2017.
 */

public class CalendarService extends Service {
    private GoogleAccountCredential mCredential = null;
    private IEventCache mCache;
    private IEventNotification mNotification;
    private IEventRepository mEventRepository;

    private boolean mRunning;

    private Handler handler;
    private Runnable runnable;

    // Setup binder that can be returned to activities.
    private final IBinder binder = new CalendarServiceBinder();
    public class CalendarServiceBinder extends Binder {
        public CalendarService getService() {
            return CalendarService.this;
        }
    }

    public void setCredentials(GoogleAccountCredential credential){
        mCredential = credential;
        mEventRepository = new EventRepository(mCredential);
    }

    // Methods publicly available for the binder :)
    public void synchronizeEventsWithGoogleCalendar(Context context) {
        if (mCredential != null && checkForInternetConnection()) {
            mEventRepository.SynchronizeEvents();
        } else{
            Toast.makeText(context, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            EventBus.getDefault().post(new NoInternetEvent());
        }
    }

    public List<Event> getAllEvents(){
        return mCache.getAll();
    }

    public List<Event> getEventsByDate(Date date) { return mCache.getByDate(date); }
    public Event getEventById(String id) { return mCache.getById(id); }
    public Event getEventByRecordId(String recordId) { return mCache.getByRecordId(recordId); }

    public List<Event> getAllTasks(){
        return mCache.getTasks();
    }
    public List<Event> getTasksBetweenDates(Date fromDate, Date toDate) { return mCache.getTasksBetweenDates(fromDate, toDate); }

    public List<Event> getAllAppointments(){
        return mCache.getAppointments();
    }
    public List<Event> getAppointmentsBetweenDates(Date fromDate, Date toDate) { return mCache.getAppointmentsBetweenDates(fromDate, toDate); }
    public void deleteEvent(String id, String recordId){
        if (mCredential != null && checkForInternetConnection()){
            mEventRepository.remove(id, recordId);
        } else {
            mCache.setToBeRemoved(id, UpdateState.Offline);
        }

    }

    public void addEvent(Event newEvent){
        // add to API
        if (mCredential != null && checkForInternetConnection()){
            mEventRepository.add(newEvent);
        } else {
            mCache.add(newEvent, UpdateState.Offline);
        }
    }

    public void updateEvent(Event updatedEvent){
        // add to API
        if (mCredential != null && checkForInternetConnection()){
            mEventRepository.update(updatedEvent);
        } else {
            mCache.update(updatedEvent, UpdateState.Offline);
        }
    }
    // End public binder methods

    @Override
    public void onCreate() {
        super.onCreate();

        // Init DB connection - https://gamedevalgorithms.com/2017/07/07/getting-started-with-sugar-orm/
        SugarContext.init(getApplicationContext());

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

            // Start background task to synchronize every half hour...
            handler = new Handler();

            runnable = new Runnable() {
                @Override
                public void run() {
                    synchronizeEventsWithGoogleCalendar(getApplicationContext());

                    handler.postDelayed(runnable, Globals.POLL_INTERVAL_IN_MS);
                }
            };

            handler.post(runnable);
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

    private boolean checkForInternetConnection(){
        // https://stackoverflow.com/a/5474270/6796072
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected = true;
        }
        else
            connected = false;

        return connected;
    }
}
