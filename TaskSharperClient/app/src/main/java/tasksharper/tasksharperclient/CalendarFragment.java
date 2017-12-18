package tasksharper.tasksharperclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;

import java.util.Calendar;
import java.util.List;

import tasksharper.tasksharperclient.Models.AuthErrorEvent;
import tasksharper.tasksharperclient.Models.Enums.EventType;
import tasksharper.tasksharperclient.Models.Event;
import tasksharper.tasksharperclient.Models.NewDataEvent;
import tasksharper.tasksharperclient.Models.NoConnectionEvent;
import tasksharper.tasksharperclient.Service.CalendarService;
import tasksharper.tasksharperclient.Utils.EventIcon;
import tasksharper.tasksharperclient.Utils.Globals;

/**
 * Created by Mads X1 Carbon on 06-12-2017.
 */

public class CalendarFragment extends Fragment {
    public static final String ARG_VIEW_SELECTED = "view_selected";

    View rootView;
    CalendarView calendarView;
    Calendar selectedDate;
    private SwipeRefreshLayout eventList;
    private View recyclerView;
    private IntentFilter filter;
    private FloatingActionButton fabAdd;
    private FrameLayout fragmentContainerDetails;
    private Fragment detailsFragment;
    private org.greenrobot.eventbus.EventBus bus = org.greenrobot.eventbus.EventBus.getDefault();

    private CalendarService serviceBinder;
    private ServiceConnection serviceConnection;
    private Intent serviceIntent;

    public CalendarFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_calendar, container, false);

        ((MainActivity)getActivity()).getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_bars);
        getActivity().setTitle(R.string.calendar);

        // Start service
        setupServiceConnection();
        serviceIntent = new Intent(getActivity(), CalendarService.class);
        getActivity().startService(serviceIntent);

        // Declarations
        calendarView = rootView.findViewById(R.id.calendarView);
        fabAdd = rootView.findViewById(R.id.fabAdd);
        eventList = rootView.findViewById(R.id.event_list_swipe_layout);
        recyclerView = rootView.findViewById(R.id.event_list);
        assert recyclerView != null;
        if (selectedDate == null){
            selectedDate = Calendar.getInstance();
        }

        if (savedInstanceState != null) {
            int day = savedInstanceState.getInt(Globals.CALENDAR_SELECTED_DAY, 8);
            int month = savedInstanceState.getInt(Globals.CALENDAR_SELECTED_MONTH, 11);;
            int year = savedInstanceState.getInt(Globals.CALENDAR_SELECTED_YEAR, 2017);;
            selectedDate.set(year, month, day);
            calendarView.setDate(selectedDate.getTimeInMillis(), true, true);
        } else {
            calendarView.setDate(selectedDate.getTimeInMillis(), true, true);
        }

        fragmentContainerDetails = rootView.findViewById(R.id.fragment_container_details);

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, final int dayOfMonth) {
                selectedDate.set(year, month, dayOfMonth);
                updateEventList(serviceBinder.getEventsByDate(selectedDate.getTime()));
            }
        });

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new EventModifyFragment();
                Bundle args = new Bundle();
                args.putInt(EventModifyFragment.ARG_EVENT_DATE_YEAR, selectedDate.get(Calendar.YEAR));
                args.putInt(EventModifyFragment.ARG_EVENT_DATE_MONTH, selectedDate.get(Calendar.MONTH));
                args.putInt(EventModifyFragment.ARG_EVENT_DATE_DATE, selectedDate.get(Calendar.DAY_OF_MONTH));
                fragment.setArguments(args);

                android.support.v4.app.FragmentManager fragmentManager = getFragmentManager();
                android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.content_frame, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        // http://sapandiwakar.in/pull-to-refresh-for-android-recyclerview-or-any-other-vertically-scrolling-view/
        eventList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items
                serviceBinder.synchronizeEventsWithGoogleCalendar(getContext());
            }
        });



        return rootView;
    }

    @Subscribe()
    public void onNewDataEvent(NewDataEvent event){
        updateEventList(serviceBinder.getEventsByDate(selectedDate.getTime()));
        // Stop refresh animation
        eventList.setRefreshing(false);
    }

    @Subscribe()
    public void onAuthErrorEvent(AuthErrorEvent event){
        eventList.setRefreshing(false);
    }

    @Subscribe
    public void onNoInternetEvent(NoConnectionEvent event){
        eventList.setRefreshing(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(Globals.CALENDAR_SELECTED_DAY, selectedDate.get(Calendar.DAY_OF_MONTH));
        outState.putInt(Globals.CALENDAR_SELECTED_MONTH, selectedDate.get(Calendar.MONTH));
        outState.putInt(Globals.CALENDAR_SELECTED_YEAR, selectedDate.get(Calendar.YEAR));
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        bus.register(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        getActivity().unbindService(serviceConnection);
        bus.unregister(this);
    }

    private void setupServiceConnection(){
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                serviceBinder = ((CalendarService.CalendarServiceBinder)service).getService();
                updateEventList(serviceBinder.getEventsByDate(selectedDate.getTime()));
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                serviceBinder = null;
            }
        };
    }

    public void updateEventList(List<Event> eventList){
        setupRecyclerView((RecyclerView)recyclerView, eventList);
        if (detailsFragment != null) {
            android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.remove(detailsFragment);
            transaction.commit();
        }
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView, List<Event> eventList) {
        recyclerView.setAdapter(new CalendarFragment.SimpleItemRecyclerViewAdapter(eventList));
    }

    public class SimpleItemRecyclerViewAdapter extends RecyclerView.Adapter<CalendarFragment.SimpleItemRecyclerViewAdapter.ViewHolder>{

        private final List<Event> eventList;

        public SimpleItemRecyclerViewAdapter(List<Event> items){
            eventList = items;
        }

        @Override
        public CalendarFragment.SimpleItemRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.event_list_content, parent, false);
            return new CalendarFragment.SimpleItemRecyclerViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final CalendarFragment.SimpleItemRecyclerViewAdapter.ViewHolder holder, final int position) {
            holder.mItem = eventList.get(position);
            holder.mEventView.setText(eventList.get(position).title);
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            start.setTime(eventList.get(position).start);
            end.setTime(eventList.get(position).end);
            final String timeTxt = String.format("%02d:%02d", start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE)) + " - " + String.format("%02d:%02d", end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE));
            holder.mTimeView.setText(timeTxt);
            holder.imgEvent.setImageResource(EventIcon.setIcon(eventList.get(position).type));

            if (eventList.get(position).type.equals(EventType.Appointment)){
                holder.checkMarkedAsComplete.setVisibility(View.GONE);
            } else if (eventList.get(position).type.equals(EventType.Task)){
                holder.checkMarkedAsComplete.setChecked(eventList.get(position).markedAsDone);
            }

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Event intentEvent = eventList.get(position);

                    detailsFragment = new EventDetailsFragment();
                    Bundle args = new Bundle();
                    args.putString(EventDetailsFragment.ARG_EVENT_ID, intentEvent.id);
                    detailsFragment.setArguments(args);

                    android.support.v4.app.FragmentManager fragmentManager = getFragmentManager();

                    // Inspired by https://developer.android.com/guide/practices/tablets-and-handsets.html
                    if (fragmentContainerDetails != null){
                        android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
                        transaction.replace(R.id.fragment_container_details, detailsFragment);
                        transaction.commit();
                    } else {
                        android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
                        transaction.replace(R.id.content_frame, detailsFragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }
                }
            });

            holder.checkMarkedAsComplete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Event intentEvent = eventList.get(position);
                    intentEvent.markedAsDone = holder.checkMarkedAsComplete.isChecked();

                    serviceBinder.updateEvent(intentEvent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return eventList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mEventView;
            public final TextView mTimeView;
            public final ImageView imgEvent;
            public final CheckBox checkMarkedAsComplete;
            public Event mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mEventView = (TextView) view.findViewById(R.id.textEvent);
                mTimeView = (TextView) view.findViewById(R.id.textTime);
                imgEvent = (ImageView) view.findViewById(R.id.imageEvent);
                checkMarkedAsComplete = view.findViewById(R.id.checkMarkedAsComplete);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mEventView.getText() + "'";
            }
        }
    }
}
