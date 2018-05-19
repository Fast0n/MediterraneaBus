package com.fast0n.mediterraneabus.timetables;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.fast0n.mediterraneabus.MainActivity;
import com.fast0n.mediterraneabus.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class TimetablesActivity extends AppCompatActivity {

    ActionBar actionBar;
    AdView mAdView;
    ArrayList<DataTimetables> dataHours;
    ListView listView;
    ProgressBar loading;
    SharedPreferences settings;
    SharedPreferences.Editor editor;
    String departure, arrival, period;
    String sort;
    Toolbar toolbar;
    Vibrator v;
    private Animation fab_open, fab_close, rotate_forward, rotate_backward;
    private Boolean isFabOpen = false;
    private CustomAdapterTimetables adapter;
    private FloatingActionButton fab, fab1;
    private InterstitialAd mInterstitialAd;
    final int[] select = { 1 };

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set title activity in the toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.solutions));

        // set color/text/icon in the task
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground);
        ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(getString(R.string.solutions),
                bm, getResources().getColor(R.color.task));
        TimetablesActivity.this.setTaskDescription(taskDesc);

        // set row icon in the toolbar
        actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        // java addresses
        fab = findViewById(R.id.fab);
        fab1 = findViewById(R.id.fab1);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        listView = findViewById(R.id.list);
        loading = findViewById(R.id.progressBar);
        mAdView = findViewById(R.id.adView1);
        rotate_backward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_backward);
        rotate_forward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_forward);
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        settings = getSharedPreferences("sharedPreferences", 0);
        editor = settings.edit();

        // banner && interstitialAd
        MobileAds.initialize(this, "ca-app-pub-9646303341923759~6818726547");
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-9646303341923759/3726801956");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });

        // hide hours
        loading.setVisibility(View.VISIBLE);

        final Bundle extras = getIntent().getExtras();
        assert extras != null;
        departure = extras.getString("departure");
        arrival = extras.getString("arrival");
        period = extras.getString("period");

        final String url = "https://mediterraneabus-api.herokuapp.com/?periodo=invernale&percorso_linea=" + departure
                + "&percorso_linea1=" + arrival + "&sort_by=time";

        get(url);

        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String share = settings.getString("share", null);

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, share);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);

                Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.solutions));
                actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
                fab.hide();
                animateFAB();

                final String url = "https://mediterraneabus-api.herokuapp.com/?periodo=invernale&percorso_linea="
                        + departure + "&percorso_linea1=" + arrival + "&sort_by=time";

                get(url);
                select[0] = 1;
                editor.putString("share", null);
                editor.apply();

            }
        });

    }

    public void get(String url) {

        url = url.replaceAll(" ", "%20");
        RequestQueue queue = Volley.newRequestQueue(this);

        try {

            URL url43 = new URL(url);
            HttpURLConnection http = (HttpURLConnection) url43.openConnection();
            int statusCode = http.getResponseCode();

            Log.e("error", String.valueOf(statusCode));

        } catch (Exception ignored) {
        }

        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            dataHours = new ArrayList<>();

                            JSONObject json_raw = new JSONObject(response.toString());
                            String linee = json_raw.getString("linee");
                            JSONArray arrayLinee = new JSONArray(linee);

                            for (int i = 0; i < arrayLinee.length(); i++) {
                                String corse = arrayLinee.getString(i);

                                JSONObject scorroCorse = new JSONObject(corse);

                                String nomeCorsa = scorroCorse.getString("corsa");
                                String orari = scorroCorse.getString("orari");

                                JSONArray arrayOrari = new JSONArray(orari);
                                for (int j = 0; j < arrayOrari.length(); j++) {

                                    String partenza2 = arrayOrari.getString(j);
                                    JSONObject scorrOrari = new JSONObject(partenza2);

                                    String partenza = scorrOrari.getString("partenza");
                                    String arrivo = scorrOrari.getString("arrivo");

                                    @SuppressLint("SimpleDateFormat")
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                                    Date startDate = simpleDateFormat.parse(partenza);
                                    Date endDate = simpleDateFormat.parse(arrivo);

                                    long difference = endDate.getTime() - startDate.getTime();
                                    if (difference < 0) {
                                        Date dateMax = simpleDateFormat.parse("24:00");
                                        Date dateMin = simpleDateFormat.parse("00:00");
                                        difference = (dateMax.getTime() - startDate.getTime())
                                                + (endDate.getTime() - dateMin.getTime());
                                    }
                                    int days = (int) (difference / (1000 * 60 * 60 * 24));
                                    int hours = (int) ((difference - (1000 * 60 * 60 * 24 * days)) / (1000 * 60 * 60));
                                    int min = (int) (difference - (1000 * 60 * 60 * 24 * days)
                                            - (1000 * 60 * 60 * hours)) / (1000 * 60);

                                    int length = String.valueOf(hours).length();
                                    String ora = null;

                                    if (length == 1) {
                                        ora = "0" + hours;
                                    }

                                    dataHours.add(new DataTimetables(nomeCorsa, partenza, arrivo, departure, arrival,
                                            ora + ":" + min));

                                }

                            }

                            adapter = new CustomAdapterTimetables(dataHours, getApplicationContext());

                            listView.setAdapter(adapter);

                            loading.setVisibility(View.INVISIBLE);

                        } catch (JSONException | ParseException ignored) {}
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        int error_code = error.networkResponse.statusCode;

                        if (error_code == 503) {
                            String url2 = "https://mediterraneabus-api.glitch.me/?periodo=invernale&percorso_linea="
                                    + departure + "&percorso_linea1=" + arrival + "&sort_by=time";
                            get(url2);
                        } else if (error_code == 404) {
                            dataHours = new ArrayList<>();
                            dataHours.add(new DataTimetables(getString(R.string.timetable_not_found), "00:00", "00:00",
                                    departure, arrival, "00:00"));
                            adapter = new CustomAdapterTimetables(dataHours, getApplicationContext());

                            listView.setAdapter(adapter);

                            loading.setVisibility(View.INVISIBLE);
                        }

                    }
                });

        // add it to the RequestQueue
        queue.add(getRequest);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (select[0] == 1) {
                    select[0] = 2;

                    vibrator();
                    adapterView.getChildAt(position).setBackgroundColor(Color.parseColor("#e0e0e0"));

                    Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.share));
                    actionBar.setHomeAsUpIndicator(R.drawable.ic_close);

                    TextView ride = view.findViewById(R.id.ride);
                    TextView time = view.findViewById(R.id.time);
                    TextView name_time = view.findViewById(R.id.name_time);
                    TextView time1 = view.findViewById(R.id.time1);
                    TextView name_time1 = view.findViewById(R.id.name_time1);

                    String share = "🚏 " + getString(R.string.departure) + " "
                            + name_time.getText().toString().toUpperCase() + "\n" + ride.getText().toString() + "\n🕜 "
                            + getString(R.string.timetables) + " " + time.getText().toString() + " --> "
                            + time1.getText().toString() + "\n" + getString(R.string.arrival) + " "
                            + name_time1.getText().toString().toUpperCase();

                    editor.putString("share", share);
                    editor.commit();

                    fab.show();
                    animateFAB();
                }

                return false;
            }
        });

    }

    public void animateFAB() {

        if (isFabOpen) {

            fab.startAnimation(rotate_backward);
            fab1.startAnimation(fab_close);
            fab1.setClickable(false);
            isFabOpen = false;

        } else {

            fab.startAnimation(rotate_forward);
            fab1.startAnimation(fab_open);
            fab1.setClickable(true);
            isFabOpen = true;

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.sort_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.sort) {

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            }

            sort = settings.getString("sort", null);
            new MaterialDialog.Builder(this).title(getString(R.string.sort)).items(R.array.preference_values)
                    .itemsCallbackSingleChoice(Integer.parseInt(sort), new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                            final Bundle extras = getIntent().getExtras();
                            switch (which) {
                            case 0:

                                assert extras != null;
                                departure = extras.getString("departure");
                                arrival = extras.getString("arrival");

                                final String url2 = "https://mediterraneabus-api.herokuapp.com/?periodo=invernale&percorso_linea="
                                        + departure + "&percorso_linea1=" + arrival + "&sort_by=time";

                                get(url2);

                                editor.putString("sort", "0");
                                editor.commit();

                                break;
                            case 1:

                                assert extras != null;
                                departure = extras.getString("departure");
                                arrival = extras.getString("arrival");

                                final String url = "https://mediterraneabus-api.herokuapp.com/?periodo=invernale&percorso_linea="
                                        + departure + "&percorso_linea1=" + arrival + "&sort_by=line";

                                get(url);

                                editor.putString("sort", "1");
                                editor.commit();
                                break;
                            }

                            return true;
                        }
                    }).show();

            return true;
        }

        switch (item.getItemId()) {
        case android.R.id.home:

            closeApplication();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void vibrator() {

        // Vibrate for 50 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            // deprecated in API 26
            v.vibrate(50);
        }
    }

    @Override
    public void onBackPressed() {
        closeApplication();
    }

    public void closeApplication() {
        final String share = settings.getString("share", null);
        if (share != null) {

            Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.solutions));
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            fab.hide();
            animateFAB();

            final String url = "https://mediterraneabus-api.herokuapp.com/?periodo=invernale&percorso_linea="
                    + departure + "&percorso_linea1=" + arrival + "&sort_by=time";

            get(url);

            editor.putString("share", null);
            select[0] = 1;
            editor.apply();

        } else {
            finish();
            settings.edit().remove("sort").apply();

            Intent mainActivity = new Intent(TimetablesActivity.this, MainActivity.class);
            mainActivity.putExtra("departure", departure);
            mainActivity.putExtra("arrival", arrival);
            mainActivity.putExtra("period", period);
            startActivity(mainActivity);
        }
    }
}
