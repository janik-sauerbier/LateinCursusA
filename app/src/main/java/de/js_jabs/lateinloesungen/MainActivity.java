package de.js_jabs.lateinloesungen;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.appodeal.ads.Appodeal;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.pollfish.interfaces.PollfishClosedListener;
import com.pollfish.interfaces.PollfishSurveyCompletedListener;
import com.pollfish.interfaces.PollfishSurveyNotAvailableListener;
import com.pollfish.interfaces.PollfishSurveyReceivedListener;
import com.pollfish.interfaces.PollfishUserNotEligibleListener;
import com.pollfish.main.PollFish;
import com.pollfish.main.PollFish.ParamsBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, Button.OnClickListener, CheckBox.OnCheckedChangeListener, AdapterView.OnItemSelectedListener {

    public static final String SHARED_PREF = "privateSharedPreferences";
    public static final String REMOVEADS_ITEM_SKU = "de.js_labs.lateinloesungen.removeads";
    public static final String CURRENT_LEKTION = "currentLektion";
    public static final String EXTRA_FORMS = "extraForms";
    public static final String PROVE_INPUT = "proveInput";
    public static final String IGNORE_CASE = "ignoreCase";
    public static final String DEV_MODE = "devMode";
    public static final String SURVEY_TIMESTAMP = "surveyTimeStamp";
    public static final String DATA_TIMESTAMP = "dataTimeStamp";
    public static final String DATA_FORMS_TIMESTAMP = "dataFormsTimeStamp";

    public static final String ANALYTICS_REMOVED_ADS = "removed_ads";
    public static final String ANALYTICS_SURVEY_REMOVED_ADS = "survey_removed_ads";
    public static final String ANALYTICS_TYPE_TEST_VOC = "Test Voc";
    public static final String ANALYTICS_TYPE_SHOW_VOC = "Show Voc";
    public static final String ANALYTICS_TYPE_SHOW_LEKTION = "Show Lektion";
    public static final String ANALYTICS_TYPE_MENU_ACTION = "Menu Action";

    public Toolbar toolbar;
    public Button sendEntryButton;
    public CheckBox nutzungsbedingungenCheckBox;
    public CheckBox erwähnenCheckBox;
    public EditText nameEditText;
    public EditText inhaltEditText;
    public TextView rightsInfoTextView;
    public ViewGroup appBarMain;
    public View contentSend;
    public View contentHome;
    public View contentRightsInfo;
    public FloatingActionButton surveyTimerFab;
    public NavigationView navigationView;
    public StableArrayAdapter adapterListView;
    private AlertDialog.Builder alertBuilder;
    private AlertDialog surveyDialogBuilder;

    public SharedPreferences sharedPreferences;
    public SharedPreferences.Editor sharedEditor;

    public ParamsBuilder PFparamsBuilder;

    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageRef;
    private StorageReference databaseRef;
    private FirebaseRemoteConfig firebaseRemoteConfig;
    private String databaseStatus = "Status wird geladen...";
    private int databaseStatusColor = Color.GRAY;

    private DataStorage ds;

    private int devCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Home");

        setupDataStorage();

        setupNavigation();

        setupHomeMenu();

        if(!isDataLoaded()){
            loadDataFile();
            ds.firstStart = false;
        }

        setupFirebase();

        setupPollfish();

        setupAppodeal();

        checkDatabase();
    }

    private void setupDataStorage(){
        ds = DataStorage.getInstance();

        sharedPreferences = this.getSharedPreferences(SHARED_PREF, 0);
        sharedEditor = sharedPreferences.edit();
        sharedEditor.commit();
        ds.removeAds = sharedPreferences.getBoolean(REMOVEADS_ITEM_SKU, false);
        ds.currentLektion = sharedPreferences.getInt(CURRENT_LEKTION, 0);
        ds.extraForms = sharedPreferences.getBoolean(EXTRA_FORMS, false);
        ds.proveInput = sharedPreferences.getBoolean(PROVE_INPUT, false);
        ds.ignoreCase = sharedPreferences.getBoolean(IGNORE_CASE, true);
        ds.devMode = sharedPreferences.getBoolean(DEV_MODE, false);
        ds.surveyTimeStamp = sharedPreferences.getLong(SURVEY_TIMESTAMP, 0);

        ds.testVocBuffer = new ArrayList<>();
        for(int i = 1; i < 51; i++){
            ds.lektions[i - 1] = new Lektion(i);
        }

        if(ds.extraForms){
            ds.dataTimeStamp = sharedPreferences.getLong(DATA_FORMS_TIMESTAMP, 0);
        }else {
            ds.dataTimeStamp = sharedPreferences.getLong(DATA_TIMESTAMP, 0);
        }

        Log.d(ds.LOG_TAG, "Remove Ads: " + Boolean.toString(ds.removeAds));
        Log.d(ds.LOG_TAG, "Survey Remove Ads: " + Boolean.toString(ds.surveyRemoveAds) + " / Timestamp: " + ds.surveyTimeStamp);
    }

    private void setupNavigation(){
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);

        if(ds.removeAds){
            navigationView.getMenu().removeItem(R.id.nav_remove_ads);
            Log.d(ds.LOG_TAG, "Remove 'Werbung entfernen' in the Menu");
        }

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        appBarMain = findViewById(R.id.appBarMainRl);
        contentSend = inflater.inflate(R.layout.content_send_main, (ViewGroup) findViewById(R.id.contentSendRl));
        contentHome = inflater.inflate(R.layout.content_home_main, (ViewGroup) findViewById(R.id.contentHomeRl));
        contentRightsInfo = inflater.inflate(R.layout.content_rightsinfo_main, (ViewGroup) findViewById(R.id.contentRightsInfoRl));
        appBarMain.addView(contentHome);
    }

    private void setupHomeMenu(){
        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.letionen, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(ds.currentLektion);
        spinner.setOnItemSelectedListener(this);

        final ListView listview = findViewById(R.id.listView);
        final String[] values = new String[] { "Übersetzungstext, Blauer Kasten und Aufgaben", "Vokabeln abfragen", "Vokabeln anzeigen", "Datenbank aktualisieren", "Teilen nicht vergessen :D" };

        adapterListView = new StableArrayAdapter(this, values);
        listview.setAdapter(adapterListView);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                adapterListView.notifyDataSetChanged();
                listViewAction(position);
            }

        });


        alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Wähle die Vokabeln");
        alertBuilder.setPositiveButton("Starten", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent i = new Intent(MainActivity.this, DisplayTestVoc.class);

                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, Integer.toString(ds.currentLektion+1));
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ANALYTICS_TYPE_TEST_VOC);
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                startActivity(i);
            }
        });
        alertBuilder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        surveyTimerFab = findViewById(R.id.fab_survey_timer);
        surveyTimerFab.setOnClickListener(this);
    }

    private void setupFirebase(){
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        firebaseAnalytics.setUserProperty(ANALYTICS_REMOVED_ADS, Boolean.toString(ds.removeAds));
        firebaseAnalytics.setAnalyticsCollectionEnabled(!ds.devMode);

        firebaseStorage = FirebaseStorage.getInstance();
        storageRef = firebaseStorage.getReferenceFromUrl("gs://admob-app-id-6279012805.appspot.com");
        if(ds.extraForms){
            databaseRef = storageRef.child("databaseForms.xml");
        }else {
            databaseRef = storageRef.child("database.xml");
        }

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        firebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        firebaseRemoteConfig.activateFetched();
        firebaseRemoteConfig.fetch(14400);

        ds.cursus_surveys = firebaseRemoteConfig.getBoolean("cursus_surveys");
        ds.cursus_survey_reward_multiplier = (int) firebaseRemoteConfig.getLong("cursus_survey_reward_multiplier");
        ds.rights_info = firebaseRemoteConfig.getString("cursus_rights_info");

        if(ds.devMode){
            ds.cursus_surveys = true;
        }

        Log.d("Pollfish", "cursus_surveys: " + ds.cursus_surveys);
        Log.d("Pollfish", "cursus_survey_reward_multiplier: " + ds.cursus_survey_reward_multiplier);
    }

    private void setupPollfish(){
        if(ds.cursus_surveys){
            PFparamsBuilder = new ParamsBuilder("79a97698-aa4f-4a68-a2b4-2c0a0bc07198")
                    .pollfishClosedListener(new PollfishClosedListener() {
                        @Override
                        public void onPollfishClosed(){
                            ds.received_survey = false;
                            if(!PollFish.isPollfishPresent()){
                                PollFish.initWith(MainActivity.this, PFparamsBuilder);
                                PollFish.hide();
                            }
                        }
                    }).pollfishSurveyNotAvailableListener(new PollfishSurveyNotAvailableListener() {
                        @Override
                        public void onPollfishSurveyNotAvailable(){
                            Log.d("Pollfish", "Survey Not Available!");
                            ds.received_survey = false;
                        }
                    }).pollfishUserNotEligibleListener(new PollfishUserNotEligibleListener() {
                        @Override
                        public void onUserNotEligible(){
                            Bundle bundle = new Bundle();
                            firebaseAnalytics.logEvent("user_not_eligible", bundle);

                            Log.d("Pollfish", "User Not Eligible!");
                            ds.received_survey = false;
                            if(ds.surveyTimeStamp < System.currentTimeMillis()){
                                ds.surveyTimeStamp = System.currentTimeMillis() + 20 * 3600000;
                            }else {
                                ds.surveyTimeStamp = ds.surveyTimeStamp + 20 * 3600000;
                            }
                            PollFish.hide();
                            Toast.makeText(MainActivity.this, "Du bist leider nicht teilnahmeberechtigt. Deshalb erhälst du nur für 20 Stunden keine werbung. Trotzdem Danke :D", Toast.LENGTH_SHORT).show();
                            sharedEditor.putLong(SURVEY_TIMESTAMP, ds.surveyTimeStamp);
                            sharedEditor.commit();
                            testSurveyTimestamp();
                        }
                    }).pollfishSurveyReceivedListener(new PollfishSurveyReceivedListener() {
                        @Override
                        public void onPollfishSurveyReceived(final boolean playfulSurvey, final int surveyPrice) {
                            double DsurveyPrice = surveyPrice;
                            DsurveyPrice = DsurveyPrice / 100;

                            Bundle bundle = new Bundle();
                            bundle.putDouble(FirebaseAnalytics.Param.VALUE, DsurveyPrice);
                            bundle.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
                            firebaseAnalytics.logEvent("received_survey", bundle);

                            Log.d("Pollfish", "Survey Received!");
                            ds.currentSurveyPrice = surveyPrice;
                            ds.received_survey = true;
                        }
                    }).pollfishSurveyCompletedListener(new PollfishSurveyCompletedListener() {
                        @Override
                        public void onPollfishSurveyCompleted(final boolean playfulSurvey, final int surveyPrice) {
                            double DsurveyPrice = surveyPrice;
                            DsurveyPrice = DsurveyPrice / 100;

                            Bundle bundle = new Bundle();
                            bundle.putDouble(FirebaseAnalytics.Param.VALUE, DsurveyPrice);
                            bundle.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
                            firebaseAnalytics.logEvent("survey_completed", bundle);

                            Log.d("Pollfish", "Survey Completed! Playful: " + playfulSurvey + " / Price: " + DsurveyPrice + " USD");
                            ds.received_survey = false;
                            if(ds.surveyTimeStamp < System.currentTimeMillis()){
                                ds.surveyTimeStamp =  System.currentTimeMillis() + (surveyPrice*ds.cursus_survey_reward_multiplier) * 3600000;
                            }else {
                                ds.surveyTimeStamp = ds.surveyTimeStamp + (surveyPrice*ds.cursus_survey_reward_multiplier) * 3600000;
                            }
                            sharedEditor.putLong(SURVEY_TIMESTAMP, ds.surveyTimeStamp);
                            sharedEditor.commit();
                            PollFish.hide();
                            testSurveyTimestamp();
                        }
                    }).customMode(false).releaseMode(!ds.devMode).build();

            PollFish.initWith(this, PFparamsBuilder);
            PollFish.hide();
        } else {
            ds.received_survey = false;
            ds.surveyRemoveAds = false;
        }

        testSurveyTimestamp();
    }

    private void showSurveyDialog(int price){
        int days = (price*ds.cursus_survey_reward_multiplier)/24;
        int hours = price*ds.cursus_survey_reward_multiplier - days*24;

        surveyDialogBuilder = new AlertDialog.Builder(this)
                .setTitle("Neue Umfrage")
                .setMessage(Html.fromHtml("Nimm an einer kurzen Umfrage teil und <B>entferne die Werbung</B> für (weitere): <br/><br/><h1>" + days + " Tage " + hours + " Stunden</h1><B>Hinweis:</B> Vor deiner ersten Umfrage werden einmalig demografische Fragen gestellt. Sollte die Umfrage wegen deiner Angaben nicht mehr zu dir passen wird die Werbung nicht entfernt. Nachdem du die demografischen Fragen einmal beantwortet hast erhälst du nur noch passende Umfragen."))
                .setPositiveButton("Teilnehmen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(PollFish.isPollfishPresent()){
                            PollFish.show();
                        }else {
                            PollFish.initWith(MainActivity.this, PFparamsBuilder);
                        }

                        Bundle bundle = new Bundle();
                        firebaseAnalytics.logEvent("survey_participation", bundle);
                    }
                })
                .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        surveyDialogBuilder.cancel();
                        ds.received_survey = false;

                        Bundle bundle = new Bundle();
                        firebaseAnalytics.logEvent("survey_canceled", bundle);
                    }
                })
                .setCancelable(false)
                .setIcon(R.drawable.ic_info)
                .show();
    }

    private void testSurveyTimestamp(){
        if(ds.surveyTimeStamp < System.currentTimeMillis()){
            ds.surveyRemoveAds = false;
            firebaseAnalytics.setUserProperty(ANALYTICS_SURVEY_REMOVED_ADS, Boolean.toString(false));
            surveyTimerFab.setVisibility(View.INVISIBLE);
        } else {
            ds.surveyRemoveAds = true;
            firebaseAnalytics.setUserProperty(ANALYTICS_SURVEY_REMOVED_ADS, Boolean.toString(true));
            surveyTimerFab.setVisibility(View.VISIBLE);
            Snackbar.make(surveyTimerFab, "Werbung für " + ((ds.surveyTimeStamp - System.currentTimeMillis()) / 3600000 + 1) + " Stunden entfernt.", Snackbar.LENGTH_LONG).show();
        }
    }

    private void setupAppodeal() {
        if(!ds.surveyRemoveAds && !ds.removeAds){
            Appodeal.disableLocationPermissionCheck();
            Appodeal.disableWriteExternalStoragePermissionCheck();
            Appodeal.setTesting(ds.devMode);
            Appodeal.initialize(this, "260f63a6337ed63fc2aeed562b70c64a792db12db4812a54", Appodeal.BANNER | /*Appodeal.NATIVE |*/ Appodeal.INTERSTITIAL);
            Appodeal.set728x90Banners(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isDataLoaded()){
            recreate();
        }
        if(ds.cursus_surveys && PFparamsBuilder != null){
            PollFish.initWith(this, PFparamsBuilder);
            PollFish.hide();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //                            HOME MENU/NAVIGATION
    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            toolbar.setTitle("Home");
            appBarMain.removeView(contentRightsInfo);
            appBarMain.removeView(contentHome);
            appBarMain.removeView(contentSend);
            appBarMain.addView(contentHome);
        } else if (id == R.id.nav_send) {
            toolbar.setTitle("Beitrag einsenden/Kontakt");
            appBarMain.removeView(contentRightsInfo);
            appBarMain.removeView(contentHome);
            appBarMain.removeView(contentSend);
            appBarMain.addView(contentSend);

            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            contentSend.setMinimumWidth(displaymetrics.widthPixels);
            contentSend.setMinimumHeight(displaymetrics.heightPixels);
            sendEntryButton = findViewById(R.id.buttonSendEntry);
            nutzungsbedingungenCheckBox = findViewById(R.id.checkBoxNutzungsbedingungen);
            erwähnenCheckBox = findViewById(R.id.checkBoxNameErwähnen);
            nameEditText = findViewById(R.id.editTextAbsenderName);
            inhaltEditText = findViewById(R.id.editTextInhalt);
            sendEntryButton.setOnClickListener(this);
            nutzungsbedingungenCheckBox.setOnCheckedChangeListener(this);
            erwähnenCheckBox.setOnCheckedChangeListener(this);
            if(!nutzungsbedingungenCheckBox.isChecked()){
                sendEntryButton.setEnabled(false);
                sendEntryButton.setBackgroundColor(Color.GRAY);
            }

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "open kontact UI");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ANALYTICS_TYPE_MENU_ACTION);
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        } else if (id == R.id.nav_rightsinfo){
            toolbar.setTitle("Rechtliche Informationen");
            appBarMain.removeView(contentRightsInfo);
            appBarMain.removeView(contentHome);
            appBarMain.removeView(contentSend);
            appBarMain.addView(contentRightsInfo);

            rightsInfoTextView = (TextView) findViewById(R.id.textViewRightsInfo);
            ds.rights_info = ds.rights_info.replace("\\n", "\n");
            rightsInfoTextView.setText(ds.rights_info);

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "open rights info UI");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ANALYTICS_TYPE_MENU_ACTION);
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        } else if (id == R.id.nav_remove_ads){
            Intent i = new Intent(this, RemoveAds.class);

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "start remove ads activity");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ANALYTICS_TYPE_MENU_ACTION);
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

            startActivity(i);
        } else if (id == R.id.nav_settings){
            Intent i = new Intent(this, SettingsActivity.class);

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "start settings activity");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ANALYTICS_TYPE_MENU_ACTION);
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

            startActivity(i);
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void listViewAction(int id){
        if(id == 0){
            if(isDataLoaded()){
                if(!PollFish.isPollfishPresent() || !ds.received_survey || ds.removeAds){
                    Intent i = new Intent(this, DisplayLektion.class);

                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, Integer.toString(ds.currentLektion+1));
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ANALYTICS_TYPE_SHOW_LEKTION);
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                    startActivity(i);
                }else{
                    showSurveyDialog(ds.currentSurveyPrice);
                }
            }else {
                recreate();
            }
        }else if(id == 1){
            if(isDataLoaded()){
                if(!PollFish.isPollfishPresent() || !ds.received_survey || ds.removeAds){
                    loadVocToDialog();
                    AlertDialog dialog = alertBuilder.create();
                    dialog.show();
                }else{
                    showSurveyDialog(ds.currentSurveyPrice);
                }
            }else {
                recreate();
            }
        }else if(id == 2){
            if(isDataLoaded()){
                if(!PollFish.isPollfishPresent() || !ds.received_survey || ds.removeAds){
                    Intent i = new Intent(this, DisplayVoc.class);

                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, Integer.toString(ds.currentLektion+1));
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ANALYTICS_TYPE_SHOW_VOC);
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                    startActivity(i);
                }else{
                    showSurveyDialog(ds.currentSurveyPrice);
                }
            }else {
                recreate();
            }
        } else if (id == 3) {
            if(!ds.dataIsLeast){
                startUpdateData();
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "update database");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ANALYTICS_TYPE_MENU_ACTION);
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            }
        }else if(id == 4){
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "rate app");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ANALYTICS_TYPE_MENU_ACTION);
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

            try {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                String sAux = "Latein Cursus App\n\nÜbersetzungstexte und Vokabeltrainer für Latein Cursus A\n\nhttp://rebrand.ly/CursusA-App";
                i.putExtra(Intent.EXTRA_TEXT, sAux);
                startActivity(Intent.createChooser(i, "Teilen"));
            } catch(Exception e) {
                Crashlytics.logException(e);
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //                            BUTTON LISTENER
    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(buttonView == nutzungsbedingungenCheckBox){
            if(isChecked){
                sendEntryButton.setEnabled(true);
                sendEntryButton.setBackgroundColor(Color.parseColor("#FF4D00"));
            }else{
                sendEntryButton.setEnabled(false);
                sendEntryButton.setBackgroundColor(Color.GRAY);
            }
        }
        if(buttonView == erwähnenCheckBox){
            devCounter++;
            if(devCounter > 20){
                if(ds.devMode){
                    ds.devMode = false;
                    sharedEditor.putBoolean(DEV_MODE, false);
                    sharedEditor.apply();
                    Toast.makeText(this, "Dev-Modus deaktiviert", Toast.LENGTH_SHORT).show();
                }else {
                    ds.devMode = true;
                    sharedEditor.putBoolean(DEV_MODE, true);
                    sharedEditor.apply();
                    Toast.makeText(this, "Dev-Modus aktiviert", Toast.LENGTH_SHORT).show();
                }
                devCounter = 0;
            }
        }
    }

    @Override
    public void onClick(View v) {
        if(v == sendEntryButton) {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:js-labs@web.de"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Latein Cursus A: Beitrag von " + nameEditText.getText() + " Erwähnen: " + erwähnenCheckBox.isChecked());
            emailIntent.putExtra(Intent.EXTRA_TEXT, inhaltEditText.getText());

            startActivity(Intent.createChooser(emailIntent, "Email senden mit..."));
        }
        if(v == surveyTimerFab){
            testSurveyTimestamp();
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //                            LOADING DATA
    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private boolean isDataLoaded(){
        if(ds.lektions != null ){
            if(ds.lektions[ds.currentLektion].vokablels != null){
                return true;
            }else{
                return false;
            }
        }else {
            return false;
        }
    }

    public void loadVocToDialog(){
        ds.testVocBuffer.clear();
        for(int i = 0; i < ds.lektions[ds.currentLektion].vokablels.length; i++){
            ds.testVocBuffer.add(ds.lektions[ds.currentLektion].vokablels[i]);
        }
        String[] array = new String[ds.lektions[ds.currentLektion].vokablels.length];
        for(int i = 0; i < array.length; i++){
            array[i] = ds.lektions[ds.currentLektion].vokablels[i].latein;
        }
        int L = array.length;
        boolean[] b2 = new boolean[L];
        for(int i=0 ; i<L ; i++){
            b2[i] =true;
        }
        alertBuilder.setMultiChoiceItems(array, b2, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    ds.testVocBuffer.add(ds.lektions[ds.currentLektion].vokablels[which]);
                } else if (ds.testVocBuffer.contains(ds.lektions[ds.currentLektion].vokablels[which])) {
                    ds.testVocBuffer.remove(ds.lektions[ds.currentLektion].vokablels[which]);
                }
            }
        });
    }

    public void loadDataFile(){
        final File dataFile;

        if(ds.extraForms){
            dataFile = new File(getFilesDir() + "/dataForms.xml");
        }else {
            dataFile = new File(getFilesDir() + "/data.xml");
        }

        if(dataFile.exists()){
            try{
                FileInputStream fis = new FileInputStream(dataFile);
                loadDataFromStream(fis);
            }catch (IOException e) {
                Crashlytics.logException(e);
                dataFile.delete();

                System.exit(0);
            }
        } else {
            loadDataAssats();
        }
    }

    public void loadDataAssats(){
        InputStream fis;
        try {
            if(ds.extraForms){
                fis = getAssets().open("databaseForms.xml");
            }else{
                fis = getAssets().open("database.xml");
            }
            loadDataFromStream(fis);
        } catch (IOException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }
    }

    private void loadDataFromStream(InputStream fis){
        ArrayList<String> data = new ArrayList<>();
        String dataPart = null;
        boolean doText = false;
        try {
            InputStreamReader isr = new InputStreamReader(fis);
            char [] inputBuffer = new char[fis.available()];
            isr.read(inputBuffer);
            dataPart = new String(inputBuffer);
            isr.close();
            fis.close();
        } catch (FileNotFoundException e3) {
            Crashlytics.logException(e3);
            e3.printStackTrace();
        } catch (IOException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }
        XmlPullParserFactory factory = null;
        try {
            factory = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e2) {
            Crashlytics.logException(e2);
            e2.printStackTrace();
        }
        factory.setNamespaceAware(true);
        XmlPullParser xpp = null;
        try {
            xpp = factory.newPullParser();
        } catch (XmlPullParserException e2) {
            Crashlytics.logException(e2);
            e2.printStackTrace();
        }
        try {
            xpp.setInput(new StringReader(dataPart));
        } catch (XmlPullParserException e1) {
            Crashlytics.logException(e1);
            e1.printStackTrace();
        }
        int eventType = 0;
        try {
            eventType = xpp.getEventType();
        } catch (XmlPullParserException e1) {
            Crashlytics.logException(e1);
            e1.printStackTrace();
        }
        while (eventType != XmlPullParser.END_DOCUMENT){
            if (eventType == XmlPullParser.START_DOCUMENT) {
            } else if (eventType == XmlPullParser.START_TAG) {
            } else if (eventType == XmlPullParser.END_TAG) {
            } else if(eventType == XmlPullParser.TEXT) {
                if(doText){
                    data.add(xpp.getText());
                    doText = false;
                }else{
                    doText = true;
                }
            }
            try {
                eventType = xpp.next();
            } catch (XmlPullParserException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            } catch (IOException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            }
        }
        for(int i = 0; i < ds.lektions.length;i++){
            data.set(i, data.get(i).replaceAll("NEWLINE","\n"));
            ds.lektions[i].setData(data.get(i));
        }
        for(int i = ds.lektions.length; i < data.size();i++){
            ds.lektions[i - ds.lektions.length].setVoc(data.get(i));
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //                            ONLINE DATABASE / UPDATES
    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private void checkDatabase(){
        databaseRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata metadata) {
                if(ds.dataTimeStamp > metadata.getUpdatedTimeMillis()){
                    databaseStatus = "Aktuell";
                    databaseStatusColor = Color.rgb(0,153,0);
                    adapterListView.notifyDataSetChanged();
                    Log.d(ds.LOG_TAG, "Datenbank: Aktuell");
                    ds.dataIsLeast = true;
                }else {
                    databaseStatus = "Nicht Aktuell";
                    databaseStatusColor = Color.RED;
                    adapterListView.notifyDataSetChanged();
                    Log.d(ds.LOG_TAG, "Datenbank: Nicht Aktuell");
                    ds.dataIsLeast = false;
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                if(((StorageException)exception).getErrorCode() == StorageException.ERROR_RETRY_LIMIT_EXCEEDED){
                    databaseStatusColor = Color.GRAY;
                    databaseStatus = "Offline";
                    adapterListView.notifyDataSetChanged();
                }else{
                    databaseStatusColor = Color.GRAY;
                    databaseStatus = "Status konnte nicht geladen werden!";
                    adapterListView.notifyDataSetChanged();
                    Crashlytics.logException(exception);
                }
            }
        });
    }

    private void startUpdateData(){
        final File dataFile;
        final File tempDownloadFile = new File(getCacheDir() + "/data.download");

        if(ds.extraForms){
            dataFile = new File(getFilesDir() + "/dataForms.xml");
        }else {
            dataFile = new File(getFilesDir() + "/data.xml");
        }

        if(databaseRef.getActiveDownloadTasks().size() == 0){
            ds.dataIsLeast = true;
            databaseRef.getFile(tempDownloadFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    databaseStatus = "Aktuell";
                    databaseStatusColor = Color.rgb(0,153,0);
                    adapterListView.notifyDataSetChanged();
                    ds.dataTimeStamp = System.currentTimeMillis();
                    if(ds.extraForms)
                        sharedEditor.putLong(DATA_FORMS_TIMESTAMP, ds.dataTimeStamp);
                    else
                        sharedEditor.putLong(DATA_TIMESTAMP, ds.dataTimeStamp);
                    sharedEditor.apply();
                    ds.dataIsLeast = true;
                    tempDownloadFile.renameTo(dataFile);
                    loadDataFile();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    databaseStatus = "Aktualisieren fehlgeschlagen!";
                    databaseStatusColor = Color.RED;
                    adapterListView.notifyDataSetChanged();
                    Crashlytics.logException(exception);
                }
            }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    databaseStatusColor = Color.rgb(255,140,0);
                    double done = taskSnapshot.getBytesTransferred();
                    double toDo = taskSnapshot.getTotalByteCount();
                    double progress = done/toDo;
                    progress = progress*100;
                    databaseStatus = "Wird aktualisiert (" + (int) progress + "%)";
                    adapterListView.notifyDataSetChanged();
                    Log.d(ds.LOG_TAG, "Datenbank wird aktualisiert... (" + taskSnapshot.getBytesTransferred() + "/" + taskSnapshot.getTotalByteCount() + ")");
                }
            }).addOnPausedListener(new OnPausedListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onPaused(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    databaseStatusColor = Color.rgb(255,140,0);
                    databaseStatus = "Wird aktualisiert (pausiert)";
                    adapterListView.notifyDataSetChanged();
                    Log.d(ds.LOG_TAG, "Datenbank wird aktualisiert... (pausiert)");
                }
            });
        }
    }


    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //                            SPINNER
    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ds.currentLektion = position;
        sharedEditor.putInt(CURRENT_LEKTION, position);
        sharedEditor.commit();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public class StableArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final String[] values;

        public StableArrayAdapter(Context context, String[] values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View listItemView = inflater.inflate(R.layout.listview_item, parent, false);
            TextView textViewTitle = listItemView.findViewById(R.id.listViewTitle);
            ImageView imageView = listItemView.findViewById(R.id.icon);
            textViewTitle.setText(values[position]);
            String s = values[position];
            if (s.equals("Übersetzungstext, Blauer Kasten und Aufgaben")) {
                imageView.setImageResource(R.drawable.show_lektion_icon);
            } else if(s.equals("Vokabeln abfragen")) {
                imageView.setImageResource(R.drawable.test_voc_icon);
            } else if(s.equals("Vokabeln anzeigen")){
                imageView.setImageResource(R.drawable.show_voc_icon);
            }else if(s.equals("Datenbank aktualisieren")){
                View listItemViewDatabase = inflater.inflate(R.layout.listview_item_database, parent, false);
                ImageView imageViewDatabase = listItemViewDatabase.findViewById(R.id.icon);
                TextView textViewTitleDatabase = listItemViewDatabase.findViewById(R.id.listViewTitle);
                TextView databaseStatusTextView = listItemViewDatabase.findViewById(R.id.listViewSubTitle);
                databaseStatusTextView.setTextColor(databaseStatusColor);
                databaseStatusTextView.setText(databaseStatus);
                textViewTitleDatabase.setText(values[position]);
                imageViewDatabase.setImageResource(R.drawable.database_icon);
                return listItemViewDatabase;
            }else{
                textViewTitle.setTextColor(Color.parseColor("#2E2E2E"));
                imageView.setImageResource(R.drawable.share_icon);
            }

            return listItemView;
        }
    }
}

