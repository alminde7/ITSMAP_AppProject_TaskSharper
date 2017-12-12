package tasksharper.tasksharperclient;

import android.app.DatePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.util.LogWriter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;

import java.util.Calendar;
import java.util.List;

import tasksharper.tasksharperclient.Models.AuthErrorEvent;
import tasksharper.tasksharperclient.Models.Enums.EventType;
import tasksharper.tasksharperclient.Models.Event;
import tasksharper.tasksharperclient.Models.MessageEvent;
import tasksharper.tasksharperclient.Models.NoInternetEvent;
import tasksharper.tasksharperclient.Service.CalendarService;
import tasksharper.tasksharperclient.Utils.EventIcon;
import tasksharper.tasksharperclient.Utils.Globals;

/**
 * Created by Mads X1 Carbon on 07-12-2017.
 */

public class EventsFragment extends Fragment {
    public static final String ARG_VIEW_SELECTED = "view_selected";

    private org.greenrobot.eventbus.EventBus bus = org.greenrobot.eventbus.EventBus.getDefault();

    EventType eventType;
    EditText editFromDate, editToDate;
    private int mFromYear, mFromMonth, mFromDay, mToYear, mToMonth, mToDay;

    View rootView;
    private SwipeRefreshLayout eventList;
    private View recyclerView;
    private CalendarService serviceBinder;
    private ServiceConnection serviceConnection;
    private Intent serviceIntent;
    private FloatingActionButton fabAdd;

    public EventsFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_events, container, false);

        ((MainActivity)getActivity()).getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_bars);
        editFromDate = rootView.findViewById(R.id.editFromDate);
        editToDate = rootView.findViewById(R.id.editToDate);

        // Declarations
        fabAdd = rootView.findViewById(R.id.fabAdd);
        eventList = rootView.findViewById(R.id.event_list_swipe_layout);
        recyclerView = rootView.findViewById(R.id.event_list);
        assert recyclerView != null;

        Bundle bundle = getArguments();
        if (bundle != null){
            try {
                eventType = EventType.valueOf(getArguments().getString(ARG_VIEW_SELECTED));
            } catch (IllegalArgumentException ex){
                eventType = EventType.Task; // Setting to task as default just in case...
            }
            switch (eventType){
                case Task:
                    getActivity().setTitle(R.string.tasks);
                    break;
                case Appointment:
                    getActivity().setTitle(R.string.appointments);
                    break;
                case None:
                    Log.d("Error", "Cannot instantiate fragment with EventType.None.");
                    getFragmentManager().popBackStackImmediate();
                    break;
            }
        }

        setupServiceConnection();
        serviceIntent = new Intent(getActivity(), CalendarService.class);
        getActivity().startService(serviceIntent);

        if (savedInstanceState != null){
            mFromDay = savedInstanceState.getInt(Globals.EVENTS_DATE_START_DAY);
            mFromMonth = savedInstanceState.getInt(Globals.EVENTS_DATE_START_MONTH);
            mFromYear = savedInstanceState.getInt(Globals.EVENTS_DATE_START_YEAR);
            mToDay = savedInstanceState.getInt(Globals.EVENTS_DATE_END_DAY);
            mToMonth = savedInstanceState.getInt(Globals.EVENTS_DATE_END_MONTH);
            mToYear = savedInstanceState.getInt(Globals.EVENTS_DATE_END_YEAR);
        }

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new EventModifyFragment();
                Bundle args = new Bundle();
                args.putString(EventModifyFragment.ARG_EVENT_TYPE, eventType.toString());
                fragment.setArguments(args);

                android.support.v4.app.FragmentManager fragmentManager = getFragmentManager();
                android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.content_frame, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        // Date and time pickers inspired by https://www.journaldev.com/9976/android-date-time-picker-dialog
        //region DateTime pickers
        editFromDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get Current Date
                final Calendar c = Calendar.getInstance();
                if (mFromYear <= 0){
                    mFromYear = c.get(Calendar.YEAR);
                }
                if (mFromMonth <= 0){
                    mFromMonth = c.get(Calendar.MONTH);
                }
                if (mFromDay <= 0){
                    mFromDay = c.get(Calendar.DAY_OF_MONTH);
                }


                DatePickerDialog datePickerDialog = new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        mFromYear = year;
                        mFromMonth = monthOfYear;
                        mFromDay = dayOfMonth;
                        editFromDate.setText(String.format("%02d-%02d-%04d", mFromDay, mFromMonth + 1, mFromYear));

                        if (mToYear > 0 && mToYear >= mFromYear && mToMonth >= 0 && mToMonth >= mFromMonth && mToDay > 0 && mToDay >= mFromDay){
                            Calendar from = Calendar.getInstance();
                            Calendar to = Calendar.getInstance();
                            from.set(mFromYear, mFromMonth, mFromDay);
                            to.set(mToYear, mToMonth, mToDay);

                            switch (eventType){
                                case Task:
                                    updateEventList(serviceBinder.getTasksBetweenDates(from.getTime(), to.getTime()));
                                    break;
                                case Appointment:
                                    updateEventList(serviceBinder.getAppointmentsBetweenDates(from.getTime(), to.getTime()));
                                    break;
                            }
                        }

                    }
                }, mFromYear, mFromMonth, mFromDay);
                datePickerDialog.show();
            }
        });
        editToDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get Current Date
                final Calendar c = Calendar.getInstance();
                if (mToYear <= 0){
                    mToYear = c.get(Calendar.YEAR);
                }
                if (mToMonth <= 0){
                    mToMonth = c.get(Calendar.MONTH);
                }
                if (mToDay <= 0){
                    mToDay = c.get(Calendar.DAY_OF_MONTH);
                }


                DatePickerDialog datePickerDialog = new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        mToYear = year;
                        mToMonth = monthOfYear;
                        mToDay = dayOfMonth;
                        editToDate.setText(String.format("%02d-%02d-%04d", mToDay, mToMonth + 1, mToYear));

                        if (mFromYear > 0 && mFromYear <= mToYear && mFromMonth >= 0 && mFromMonth <= mToMonth && mFromDay > 0 && mFromDay <= mToDay){
                            Calendar from = Calendar.getInstance();
                            Calendar to = Calendar.getInstance();
                            from.set(mFromYear, mFromMonth, mFromDay);
                            to.set(mToYear, mToMonth, mToDay);

                            switch (eventType){
                                case Task:
                                    updateEventList(serviceBinder.getTasksBetweenDates(from.getTime(), to.getTime()));
                                    break;
                                case Appointment:
                                    updateEventList(serviceBinder.getAppointmentsBetweenDates(from.getTime(), to.getTime()));
                                    break;
                            }
                        }

                    }
                }, mToYear, mToMonth, mToDay);
                datePickerDialog.show();
            }
        });
        //endregion

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
    public void onNewDataEvent(MessageEvent event){
        updateView();
        // Stop refresh animation
        eventList.setRefreshing(false);
    }

    @Subscribe()
    public void onAuthErrorEvent(AuthErrorEvent event){
        eventList.setRefreshing(false);
    }

    @Subscribe
    public void onNoInternetEvent(NoInternetEvent event){
        eventList.setRefreshing(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        bus.register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unbindService(serviceConnection);
        bus.unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(Globals.EVENTS_DATE_START_DAY, mFromDay);
        outState.putInt(Globals.EVENTS_DATE_START_MONTH, mFromMonth);
        outState.putInt(Globals.EVENTS_DATE_START_YEAR, mFromYear);
        outState.putInt(Globals.EVENTS_DATE_END_DAY, mToDay);
        outState.putInt(Globals.EVENTS_DATE_END_MONTH, mToMonth);
        outState.putInt(Globals.EVENTS_DATE_END_YEAR, mToYear);
    }

    private void setupServiceConnection(){
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                serviceBinder = ((CalendarService.CalendarServiceBinder)service).getService();
                updateView();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                serviceBinder = null;
            }
        };
    }

    private void updateView(){
        if (mToYear > 0 && mFromYear > 0 && mToYear >= mFromYear && mToMonth >= 0 && mFromMonth >= 0 && mToMonth >= mFromMonth && mToDay > 0 && mFromDay > 0 && mToDay >= mFromDay){
            Calendar from = Calendar.getInstance();
            Calendar to = Calendar.getInstance();
            from.set(mFromYear, mFromMonth, mFromDay);
            to.set(mToYear, mToMonth, mToDay);

            switch (eventType){
                case Task:
                    updateEventList(serviceBinder.getTasksBetweenDates(from.getTime(), to.getTime()));
                    break;
                case Appointment:
                    updateEventList(serviceBinder.getAppointmentsBetweenDates(from.getTime(), to.getTime()));
                    break;
            }
        } else {
            switch (eventType){
                case Task:
                    updateEventList(serviceBinder.getAllTasks());
                    break;
                case Appointment:
                    updateEventList(serviceBinder.getAllAppointments());
                    break;
            }
        }
    }

    private void updateEventList(List<Event> eventList){
        setupRecyclerView((RecyclerView)recyclerView, eventList);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView, List<Event> eventList) {
        recyclerView.setAdapter(new EventsFragment.SimpleItemRecyclerViewAdapter(eventList));
    }

    public class SimpleItemRecyclerViewAdapter extends RecyclerView.Adapter<EventsFragment.SimpleItemRecyclerViewAdapter.ViewHolder>{

        private final List<Event> eventList;

        public SimpleItemRecyclerViewAdapter(List<Event> items){
            eventList = items;
        }

        @Override
        public EventsFragment.SimpleItemRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.event_list_content, parent, false);
            return new EventsFragment.SimpleItemRecyclerViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final EventsFragment.SimpleItemRecyclerViewAdapter.ViewHolder holder, final int position) {
            holder.mItem = eventList.get(position);
            holder.mEventView.setText(eventList.get(position).title);
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            start.setTime(eventList.get(position).start);
            end.setTime(eventList.get(position).end);
            final String timeTxt = String.format("%02d-%02d-%04d: %02d:%02d", start.get(Calendar.DAY_OF_MONTH), start.get(Calendar.MONTH) + 1, start.get(Calendar.YEAR), start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE)) + " - " + String.format("%02d:%02d", end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE));
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

                    Fragment fragment = new EventDetailsFragment();
                    Bundle args = new Bundle();
                    args.putString(EventDetailsFragment.ARG_EVENT_ID, intentEvent.id);
                    fragment.setArguments(args);

                    android.support.v4.app.FragmentManager fragmentManager = getFragmentManager();
                    android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.content_frame, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
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
