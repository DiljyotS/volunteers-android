package vola.systers.com.android.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import vola.systers.com.android.R;
import vola.systers.com.android.activities.EventDetailViewActivity;
import vola.systers.com.android.activities.MenuActivity;
import vola.systers.com.android.activities.SignInActivity;
import vola.systers.com.android.adapter.EventListAdapter;
import vola.systers.com.android.adapter.ScheduleEventsListAdapter;
import vola.systers.com.android.model.Event;
import vola.systers.com.android.utils.NetworkConnectivity;

public class ScheduleFragment extends Fragment {

    public ScheduleFragment() {
    }

    private String TAG = ScheduleFragment.class.getSimpleName();
    final static FirebaseDatabase database = FirebaseDatabase.getInstance();

    private ProgressDialog pDialog;
    private CoordinatorLayout coordinatorLayout;
    private ListView eventsListView;
    static String startDate, endDate, id,name,startTime,endTime,locationName,description,latitude,longitude,status="",max_attendees,city,country;
    public static String userToken="";
    private static ScheduleEventsListAdapter eventListAdapter;
    private TextView eventsLabel;

    ArrayList<Event> eventList;
    Map<String,String> registeredEvents = new HashMap<String, String>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.schedule_list_fragment, container, false);
        eventList = new ArrayList<>();
        eventsListView = (ListView) rootView.findViewById(R.id.list);
        coordinatorLayout = (CoordinatorLayout) rootView.findViewById(R.id.coordinator_layout);
        eventsLabel=(TextView)rootView.findViewById(R.id.noEventsLabel);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(! new NetworkConnectivity().checkConnectivity(getActivity())) {
            Snackbar snackbar = Snackbar
                    .make(coordinatorLayout, "Please Make Sure You are Connected to Internet!", Snackbar.LENGTH_LONG);
            View sbView = snackbar.getView();
            sbView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
            snackbar.show();
        }

        if (user != null) {
            userToken = user.getUid();

            DatabaseReference usersRef = database.getReference("event_registrations").child(userToken);
            ValueEventListener vs = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        Log.i("EVENT IDS", ds.getKey().toString());
                        registeredEvents.put(ds.getKey().toString(), ds.child("attendee_type").getValue().toString());
                    }
                    if (registeredEvents.size() != 0) {
                        eventsLabel.setVisibility(View.GONE);
                    }
                    new GetEvents().execute();

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "Failed to read value.", databaseError.toException());
                }
            };
            usersRef.addValueEventListener(vs);
        }
        else {
            eventsLabel.setVisibility(View.GONE);
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(getActivity());
            } else {
                builder = new AlertDialog.Builder(getActivity());
            }
            builder.setTitle("Login Required!")
                    .setMessage("You Need to Login to see your registered Events. Do you want to Login?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(getActivity(), SignInActivity.class);
                            startActivity(i);
                            ((Activity) getActivity()).overridePendingTransition(0,0);
                            getActivity().finish();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(getActivity(), MenuActivity.class);
                            startActivity(i);
                            ((Activity) getActivity()).overridePendingTransition(0,0);
                            getActivity().finish();
                        }
                    })
                    .show();
        }

        return rootView;
    }

    private class GetEvents extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            FirebaseDatabase eventsDatabase = FirebaseDatabase.getInstance();
            final DatabaseReference eventsRef = eventsDatabase.getReference("events");

            ValueEventListener valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot data_snap : dataSnapshot.getChildren()) {

                        id = data_snap.getKey();
                        if(registeredEvents.containsKey(id)) {
                            name = data_snap.child("name").getValue().toString();
                            startDate = data_snap.child("startdate").getValue().toString();
                            endDate = data_snap.child("enddate").getValue().toString();
                            startTime = data_snap.child("starttime").getValue().toString();
                            endTime = data_snap.child("endtime").getValue().toString();
                            locationName = data_snap.child("location").child("name").getValue().toString();
                            description = data_snap.child("description").getValue().toString();
                            city = data_snap.child("location").child("city").getValue().toString();
                            country = data_snap.child("location").child("country").getValue().toString();
                            latitude = data_snap.child("location").child("latitude").getValue().toString();
                            longitude = data_snap.child("location").child("longitude").getValue().toString();
                            status=registeredEvents.get(id);
                            eventList.add(new Event(id, name, startDate, endDate, startTime, endTime, locationName, description, city, country, latitude, longitude,status));
                        }

                    }
                    if (getContext()!=null){
                        eventListAdapter = new ScheduleEventsListAdapter(eventList,getContext());
                        eventsListView.setAdapter(eventListAdapter);
                        eventsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    Event selectedEvent = eventList.get(position);
                                    Intent intent = new Intent(getActivity(), EventDetailViewActivity.class);
                                    intent.putExtra("selectedEvent", selectedEvent);
                                    startActivity(intent);
                                }
                            });
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "Failed to read value.", databaseError.toException());
                }
            };
            eventsRef.addValueEventListener(valueEventListener);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

    }

}