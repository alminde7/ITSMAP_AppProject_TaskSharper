package tasksharper.tasksharperclient;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

import tasksharper.tasksharperclient.Models.Event;
import tasksharper.tasksharperclient.R;
import tasksharper.tasksharperclient.Service.CalendarService;
import tasksharper.tasksharperclient.Utils.Globals;

/**
 * Created by Mads X1 Carbon on 06-12-2017.
 */

public class EventDetailsFragment extends Fragment {
    public static final String ARG_EVENT_ID = "event_id";

    View rootView;
    private CalendarService serviceBinder;
    private ServiceConnection serviceConnection;
    private Intent serviceIntent;

    Event event;

    TextView textTitle, textType, textDateTime, textDescription, textDate;
    FloatingActionButton fabEdit, fabDelete;
    private String id, recordId;
    private Context mContext;

    public EventDetailsFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_event_details, container, false);
        Bundle bundle = getArguments();
        if (bundle != null){
            id = getArguments().getString(ARG_EVENT_ID);
        }

        getActivity().setTitle(R.string.details);

        // Declarations
        mContext = rootView.getContext();
        textTitle = rootView.findViewById(R.id.textTitle);
        textType = rootView.findViewById(R.id.textType);
        textDateTime = rootView.findViewById(R.id.textDateTime);
        textDescription = rootView.findViewById(R.id.textDescription);
        textDate = rootView.findViewById(R.id.textDate);
        fabEdit = rootView.findViewById(R.id.FABEdit);
        fabDelete = rootView.findViewById(R.id.FABDelete);
        if (id != null){
            setupServiceConnection();
            serviceIntent = new Intent(getActivity(), CalendarService.class);
            getActivity().startService(serviceIntent);

            // Click handlers for floating action buttons
            fabEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Fragment fragment = new EventModifyFragment();

                    Bundle args = new Bundle();
                    args.putString(EventModifyFragment.ARG_EVENT_ID, id);
                    args.putString(EventModifyFragment.ARG_EVENT_TYPE, event.type.toString());
                    fragment.setArguments(args);

                    android.support.v4.app.FragmentManager fragmentManager = getFragmentManager();
                    android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.content_frame, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            });

            fabDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setCancelable(true);
                    builder.setTitle(R.string.confirm);
                    builder.setMessage(getString(R.string.are_you_sure_delete, textTitle.getText()));
                    builder.setPositiveButton(R.string.confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    serviceBinder.deleteEvent(event.id, event.recordId);
                                    getFragmentManager().popBackStackImmediate();
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }


        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (id != null){
            getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (id != null) {
            getActivity().unbindService(serviceConnection);
        }
    }

    private void setupServiceConnection(){
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                serviceBinder = ((CalendarService.CalendarServiceBinder)service).getService();
                event = serviceBinder.getEventById(id);
                updateView(event);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                serviceBinder = null;
            }
        };
    }

    public void updateView(Event event){
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.setTime(event.start);
        end.setTime(event.end);

        textTitle.setText(event.title);
        textType.setText(event.type.toString());
        textDateTime.setText(String.format("%02d:%02d", start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE)) + " - " + String.format("%02d:%02d", end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE)));
        textDescription.setText(event.description);
        textDate.setText(String.format("%02d-%02d-%04d", start.get(Calendar.DAY_OF_MONTH), start.get(Calendar.MONTH) + 1, start.get(Calendar.YEAR)));
    }
}
