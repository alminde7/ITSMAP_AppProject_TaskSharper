package tasksharper.tasksharperclient.Notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import tasksharper.tasksharperclient.Models.Event;
import tasksharper.tasksharperclient.R;
import tasksharper.tasksharperclient.Utils.AppConstants;

/**
 * Created by alminde on 02/12/2017.
 */

public class EventNotification implements IEventNotification {

    private NotificationManager mNotificationManager;
    private Resources mAppResources;
    private Context mAppContext;

    private Map<String, Timer> mEventTimers;

    public EventNotification(NotificationManager notificationManager, Resources appResources, Context appContext){
        this.mNotificationManager = notificationManager;
        this.mAppResources = appResources;
        this.mAppContext = appContext;

        mEventTimers = new HashMap<>();
    }

    @Override
    public void attach(Event event) {

        Timer timer = new Timer();
        timer.schedule(notificationCallback(event),event.start);

        mEventTimers.put(event.id, timer);
    }

    @Override
    public void detatch(Event event) {

        if(mEventTimers.containsKey(event.id)){
            Timer timerToRemove = mEventTimers.remove(event.id);
            timerToRemove.purge();
        }
    }

    private TimerTask notificationCallback(final Event event){

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                createNotification(event);
            }
        };
        return task;
    }

    private void createNotification(Event event){
        if (Build.VERSION.SDK_INT < 26) {
            // Make a notification (Following the guide: https://www.tutorialspoint.com/android/android_notifications.htm)
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mAppContext);
            mBuilder.setContentTitle(mAppResources.getString(R.string.app_name));
            mBuilder.setContentText(mAppResources.getString(R.string.app_name));

            mNotificationManager.notify(0, mBuilder.build());
        } else {
            // https://developer.android.com/guide/topics/ui/notifiers/notifications.html
            Notification.Builder notification = new Notification.Builder(mAppContext, AppConstants.NOTIFICATION_CHANNEL_ID);

            notification
                    .setContentTitle(mAppResources.getString(R.string.app_name))
                    .setContentText(mAppResources.getString(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_launcher_foreground);

            mNotificationManager.notify(0, notification.build());
        }
    }
}
