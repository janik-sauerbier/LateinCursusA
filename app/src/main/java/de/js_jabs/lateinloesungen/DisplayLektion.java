package de.js_jabs.lateinloesungen;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.InterstitialCallbacks;
import com.crashlytics.android.Crashlytics;

public class DisplayLektion extends AppCompatActivity {

    private final String COPY_MESSAGE = "Hol dir die Cursus A App im Playstore :D\nbit.ly/CursusA-App\n\n";
    private TextView text;
    private FloatingActionButton fab;

    private DataStorage ds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_lektion);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Lösungen");
        setSupportActionBar(toolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ds = DataStorage.getInstance();
        if(ds.firstStart){
            finish();
            return;
        }

        text = findViewById(R.id.textView3);
        fab = findViewById(R.id.fab);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(ds.removeAds || ds.surveyRemoveAds){
            Log.d(ds.LOG_TAG, "Werbung wird nicht angezeigt");
        }else {
            Log.d(ds.LOG_TAG, "Werbung wird angezeigt");

            Appodeal.setBannerViewId(R.id.dl_banner);
            Appodeal.show(this, Appodeal.BANNER_VIEW);
            Appodeal.setInterstitialCallbacks(new InterstitialCallbacks() {
                @Override
                public void onInterstitialLoaded(boolean b) {}

                @Override
                public void onInterstitialFailedToLoad() {
                    Crashlytics.logException(new Throwable("DisplayLektion: InterstitialFailedToLoad()"));
                }

                @Override
                public void onInterstitialShown() {}

                @Override
                public void onInterstitialClicked() {}

                @Override
                public void onInterstitialClosed() {
                    finish();
                }
            });
        }

        toolbar.setTitle("Lektion " + (ds.currentLektion + 1));
        text.setText(ds.lektions[ds.currentLektion].data);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    String text = COPY_MESSAGE + "Lektion " + (ds.currentLektion + 1) + "\n\n" + ds.lektions[ds.currentLektion].data;
                    i.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(i, "Text teilen über..."));
                } catch(Exception e) {
                    Crashlytics.logException(e);
                }
            }
        });
    }

    @Override
    public void onBackPressed(){
        if(Appodeal.isLoaded(Appodeal.INTERSTITIAL) && !ds.surveyRemoveAds && !ds.removeAds)
            Appodeal.show(this, Appodeal.INTERSTITIAL);
        else
            finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Appodeal.onResume(this, Appodeal.BANNER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
