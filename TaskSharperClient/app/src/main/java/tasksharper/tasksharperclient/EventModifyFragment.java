package tasksharper.tasksharperclient;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

import tasksharper.tasksharperclient.Models.Enums.EventType;
import tasksharper.tasksharperclient.Models.Event;
import tasksharper.tasksharperclient.Service.CalendarService;

/**
 * Created by Mads X1 Carbon on 06-12-2017.
 */

public class EventModifyFragment extends Fragment {
    public static final String ARG_EVENT_ID = "event_id";
    public static final String ARG_EVENT_TYPE = "event_type";
    public static final String ARG_EVENT_DATE_YEAR = "event_year";
    public static final String ARG_EVENT_DATE_MONTH = "event_month";
    public static final String ARG_EVENT_DATE_DATE = "event_date";

    View rootView;
    TextView textTitle;
    EditText editTitle, editDescription;
    EditText editDate, editStartTime, editEndTime;
    Spinner spinnerType;
    FloatingActionButton fabSave, fabBack;

    private int mYear, mMonth, mDay, mStartHour, mStartMinute, mEndHour, mEndMinute;
    private String id, recordId, eventType;

    private CalendarService serviceBinder;
    private ServiceConnection serviceConnection;
    private Intent serviceIntent;

    public EventModifyFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_event_modify, container, false);
        Bundle bundle = getArguments();
        if (bundle != null){
            id = getArguments().getString(ARG_EVENT_ID);
            eventType = getArguments().getString(ARG_EVENT_TYPE);
            mYear = getArguments().getInt(ARG_EVENT_DATE_YEAR);
            mMonth = getArguments().getInt(ARG_EVENT_DATE_MONTH);
            mDay = getArguments().getInt(ARG_EVENT_DATE_DATE);
        }

        // Declarations
        textTitle = rootView.findViewById(R.id.textTitle);
        editTitle = rootView.findViewById(R.id.editTitle);
        editDescription = rootView.findViewById(R.id.editDescription);
        editDate = rootView.findViewById(R.id.editFromDate);
        editStartTime = rootView.findViewById(R.id.editStartTime);
        editEndTime = rootView.findViewById(R.id.editEndTime);
        spinnerType = rootView.findViewById(R.id.spinnerType);
        fabSave = rootView.findViewById(R.id.FABSave);
        fabBack = rootView.findViewById(R.id.FABBack);

        // Start service
        setupServiceConnection();
        serviceIntent = new Intent(getActivity(), CalendarService.class);
        getActivity().startService(serviceIntent);

        if (id == null) {
            getActivity().setTitle(R.string.create);
            textTitle.setText(R.string.new_event);
        } else {
            getActivity().setTitle(R.string.edit);
            textTitle.setText(R.string.edit_event); // Setting this in case service doesn't start...
        }

        fabSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                start.set(mYear, mMonth, mDay, mStartHour, mStartMinute, 0);
                end.set(mYear, mMonth, mDay, mEndHour, mEndMinute, 0);

                Event event = new Event();
                if (recordId != null) {
                    event.recordId = recordId;
                }
                event.title = editTitle.getText().toString();
                event.description = editDescription.getText().toString();

                String type = spinnerType.getSelectedItem().toString();
                if (type.equals(getString(R.string.event_type_task))){
                    event.type = EventType.Task;
                } else if (type.equals(getString(R.string.event_type_appointment))){
                    event.type = EventType.Appointment;
                } else {
                    event.type = EventType.None;
                }

                event.start = start.getTime();
                event.end = end.getTime();

                // Either update or add the event
                if (id != null){
                    event.id = id;
                    serviceBinder.updateEvent(event);
                } else {
                    serviceBinder.addEvent(event);
                }

                getFragmentManager().popBackStackImmediate();
            }
        });

        fabBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStackImmediate();
            }
        });

        // Date and time pickers inspired by https://www.journaldev.com/9976/android-date-time-picker-dialog
        //region DateTime pickers
        editDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get Current Date
                final Calendar c = Calendar.getInstance();
                if (mYear <= 0){
                    mYear = c.get(Calendar.YEAR);
                }
                if (mMonth <= 0){
                    mMonth = c.get(Calendar.MONTH);
                }
                if (mDay <= 0){
                    mDay = c.get(Calendar.DAY_OF_MONTH);
                }


                DatePickerDialog datePickerDialog = new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        mYear = year;
                        mMonth = monthOfYear;
                        mDay = dayOfMonth;
                        editDate.setText(String.format("%02d-%02d-%04d", mDay, mMonth + 1, mYear));

                    }
                }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        });

        editStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get Current Time
                final Calendar c = Calendar.getInstance();
                if (mStartHour <= 0){
                    mStartHour = c.get(Calendar.HOUR_OF_DAY);
                }
                if (mStartMinute <= 0){
                    mStartMinute = c.get(Calendar.MINUTE);
                }

                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(v.getContext(),
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {
                                mStartHour = hourOfDay;
                                mStartMinute = minute;
                                editStartTime.setText(String.format("%02d:%02d", mStartHour, mStartMinute));
                            }
                        }, mStartHour, mStartMinute, true);
                timePickerDialog.show();
            }
        });

        editEndTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get Current Time
                final Calendar c = Calendar.getInstance();
                if (mEndHour <= 0){
                    mEndHour = c.get(Calendar.HOUR_OF_DAY);
                }
                if (mEndMinute <= 0){
                    mEndMinute = c.get(Calendar.MINUTE);
                }


                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(v.getContext(),
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {
                                mEndHour = hourOfDay;
                                mEndMinute = minute;
                                editEndTime.setText(String.format("%02d:%02d", mEndHour, mEndMinute));
                            }
                        }, mEndHour, mEndMinute, true);
                timePickerDialog.show();
            }
        });
        //endregion

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setupServiceConnection(){
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                serviceBinder = ((CalendarService.CalendarServiceBinder)service).getService();

                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();

                if (id != null){
                    Event event = serviceBinder.getEventById(id);
                    recordId = event.recordId;
                    textTitle.setText(getString(R.string.edit_event, event.title));
                    editTitle.setText(event.title);
                    editDescription.setText(event.description);

                    String type = event.type.toString();
                    if (type.equals(getString(R.string.event_type_task))){
                        spinnerType.setSelection(((ArrayAdapter)spinnerType.getAdapter()).getPosition(getString(R.string.event_type_task)));
                    } else if (type.equals(getString(R.string.event_type_appointment))){
                        spinnerType.setSelection(((ArrayAdapter)spinnerType.getAdapter()).getPosition(getString(R.string.event_type_appointment)));
                    } else {
                        spinnerType.setSelection(((ArrayAdapter)spinnerType.getAdapter()).getPosition(getString(R.string.event_type_task)));
                    }


                    // Setting start/end fields
                    start.setTime(event.start);
                    end.setTime(event.end);

                    // Setting the date field + backing values
                    mYear = start.get(Calendar.YEAR);
                    mMonth = start.get(Calendar.MONTH);
                    mDay = start.get(Calendar.DAY_OF_MONTH);
                    editDate.setText(String.format("%02d-%02d-%04d", mDay, mMonth + 1, mYear));

                    // Setting the start/end time fields + backing values
                    mStartHour = start.get(Calendar.HOUR_OF_DAY);
                    mStartMinute = start.get(Calendar.MINUTE);
                    mEndHour = end.get(Calendar.HOUR_OF_DAY);
                    mEndMinute = end.get(Calendar.MINUTE);
                    editStartTime.setText(String.format("%02d:%02d", mStartHour, mStartMinute));
                    editEndTime.setText(String.format("%02d:%02d", mEndHour, mEndMinute));
                } else {
                    // Setting the date field + backing values
                    if (mYear == 0 && mMonth == 0 && mDay == 0) {
                        mYear = start.get(Calendar.YEAR);
                        mMonth = start.get(Calendar.MONTH);
                        mDay = start.get(Calendar.DAY_OF_MONTH);
                    }

                    editDate.setText(String.format("%02d-%02d-%04d", mDay, mMonth + 1, mYear));

                    if (eventType != null){
                        if (eventType.equals(EventType.Task.toString())){
                            spinnerType.setSelection(((ArrayAdapter)spinnerType.getAdapter()).getPosition(getString(R.string.event_type_task)));
                        } else if (eventType.equals(EventType.Appointment.toString())){
                            spinnerType.setSelection(((ArrayAdapter)spinnerType.getAdapter()).getPosition(getString(R.string.event_type_appointment)));
                        } else {
                            spinnerType.setSelection(((ArrayAdapter)spinnerType.getAdapter()).getPosition(getString(R.string.event_type_task)));
                        }
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                serviceBinder = null;
            }
        };
    }

    public void updateView(String id){

    }
}
