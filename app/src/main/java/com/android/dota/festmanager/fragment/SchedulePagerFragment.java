package com.android.dota.festmanager.fragment;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.dota.festmanager.adapter.ScheduleAdapter;
import com.android.dota.festmanager.api.ApiClient;
import com.android.dota.festmanager.api.EventsInterface;
import com.android.dota.festmanager.api.TestApiClient;
import com.android.dota.festmanager.model.EventDetails;
import com.android.dota.festmanager.R;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class SchedulePagerFragment extends Fragment {
    private RecyclerView recyclerView;
    private ScheduleAdapter adapter;
    private List<EventDetails> list = new ArrayList<>();
    private List<String> realmlist = new ArrayList<>();
    public Realm realm;
    private String TAG = "SchedulePagerFragment";
    private int page;
    private String day;
    private int i;
    private Context context;
    private boolean isNetwork = false;
    private int start;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_pager, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Realm.init(context);
        realm = Realm.getDefaultInstance();
        page = getArguments().getInt("page", 0);
        start = getArguments().getInt("start", 0);
        Log.d(TAG,"start "+String.valueOf(start));
        recyclerView = view.findViewById(R.id.schedule_recyclerview);
        adapter = new ScheduleAdapter(realmlist, context, day);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);

        switch (page) {
            case 0:
                day = "26";
                break;
            case 1:
                day = "27";
                break;
            case 2:
                day = "28";
                break;
        }
        if (start == 0) {
            CallApi();
        } else {
            getDatafromRealm(realm);
        }

    }

    private void CallApi() {
        EventsInterface apiservice = TestApiClient.getClient().create(EventsInterface.class);
        Call<ArrayList<EventDetails>> call = apiservice.getEventSchedule();
        call.enqueue(new Callback<ArrayList<EventDetails>>() {
            @Override
            public void onResponse(Call<ArrayList<EventDetails>> call, Response<ArrayList<EventDetails>> response) {
                list = response.body();

                for (i = 0; i < list.size(); i++) {
                    addDatatoRealm(list.get(i));
                }
                isNetwork = true;
                getDatafromRealm(realm);

            }

            @Override
            public void onFailure(Call<ArrayList<EventDetails>> call, Throwable t) {
                Log.e(TAG, "Error in Connectivity");
                isNetwork = false;
                getDatafromRealm(realm);
            }
        });
    }

    private void addDatatoRealm(EventDetails details) {
        realm.beginTransaction();
        EventDetails model = realm.where(EventDetails.class).equalTo("id", details.getId()).findFirst();
        if (model == null) {
            EventDetails event = realm.createObject(EventDetails.class);
            event.setId(details.getId());
            event.setName(details.getName());
            event.setStartTime(getEventTime(details.getStartTime())[3] + ":" + getEventTime(details.getStartTime())[4]);
            event.setAbout(details.getAbout());
            event.setDate(getEventTime(details.getStartTime())[2]);
            event.setTagline(details.getTagline());
        } else {
            model.setName(details.getName());
            model.setStartTime(getEventTime(details.getStartTime())[3] + ":" + getEventTime(details.getStartTime())[4]);
            model.setAbout(details.getAbout());
            model.setDate(getEventTime(details.getStartTime())[2]);
            model.setTagline(details.getTagline());
        }
        realm.commitTransaction();
    }

    private void getDatafromRealm(Realm realm1) {
        if (realm1 != null) {
            realmlist = new ArrayList<>();
            RealmResults<EventDetails> results = realm1.where(EventDetails.class).equalTo("date", day).findAll();
            if (results.size() == 0) {
                if (!isNetwork) {
                    Toast.makeText(context, "No Internet", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (!isNetwork && start == 0) {
                    Toast.makeText(context, "No Network....Loading Offline Data", Toast.LENGTH_SHORT).show();
                }
            }
            for (int j = 0; j < results.size(); j++) {
                realmlist.add(results.get(j).getStartTime());
            }

            Set<String> set = new LinkedHashSet<>(realmlist);
            realmlist.clear();
            realmlist.addAll(set);
            recyclerView.setAdapter(new ScheduleAdapter(realmlist, getContext(), day));
        }

    }

    public String[] getEventTime(String time) {

        // The format of the startTime string is yyyy-MM-dd-HH-mm
        // HH-mm is the time in 24 hour format. Use this after conversion to 12 hour format.

        String pattern = "\\d{4}(-\\d{2}){4}";
        String[] parts = {"", "", "", "", ""};
        // testdate corresponds to 10:05 AM (10:05 hours), 11th August 2018
        String testdate = "2018-08-11-10-05"; // replace with details.getStartTime()

        // validation condition. If false, do not parse the time, and have a default fallback option
        if (time.matches(pattern)) {
            // Split the testdate String, to obtain the various parts of the time
            parts = time.split("-");
            // wrt to testdate
            // parts[0] => yyyy => 2018
            // parts[1] => MM => 08
            // parts[2] => DD => 11
            // parts[3] => HH => 10
            // parts[4] => mm => 5
            return parts;
        }

        return parts;

    }
}
