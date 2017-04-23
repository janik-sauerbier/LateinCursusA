package de.js_jabs.lateinloesungen;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class UpdateActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseStorage firebaseStorage;
    private StorageReference storageRef;
    private StorageReference updateRef;

    private Button installBtn;
    private TextView infoTextView;

    private boolean updateIsReady = false;
    private boolean updateIsRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);
        getSupportActionBar().setTitle("Update");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        installBtn = (Button) findViewById(R.id.buttonInstallUpdate);
        installBtn.setOnClickListener(this);
        infoTextView = (TextView) findViewById(R.id.textViewUpdateInfo);

        setupUpdate();
    }

    private void setupUpdate(){
        firebaseStorage = FirebaseStorage.getInstance();
        storageRef = firebaseStorage.getReferenceFromUrl("gs://admob-app-id-6279012805.appspot.com");
        updateRef = storageRef.child("cursus.apk");

        infoTextView.setText(Html.fromHtml(FirebaseRemoteConfig.getInstance().getString("cursus_changelog")));

        updateRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata metadata) {
                updateIsReady = true;
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                FirebaseCrash.report(exception);
                finish();
            }
        });
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

    @Override
    public void onClick(View view) {
        if(view == installBtn){
            if(!updateIsRunning && updateIsReady){
                startDownload();
            }
        }
    }

    private void startDownload(){
        final Activity updateActivity = this;
        final DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        final File tempDownloadFile = new File(getExternalCacheDir() + "/latein_cursus_a.apk");

        updateRef.getFile(tempDownloadFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                downloadManager.addCompletedDownload("Latein Cursus A", "Update", false, "application/vnd.android.package-archive", tempDownloadFile.getPath(), tempDownloadFile.length(), true);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(tempDownloadFile), "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                updateActivity.startActivity(intent);
                installBtn.setText("Warten ...");
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                FirebaseCrash.report(exception);
                exception.printStackTrace();
                finish();
            }
        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                double done = taskSnapshot.getBytesTransferred();
                double toDo = taskSnapshot.getTotalByteCount();
                double progress = done/toDo;
                progress = progress*100;
                installBtn.setText("Lädt (" + (int) progress + "%)");
            }
        }).addOnPausedListener(new OnPausedListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onPaused(FileDownloadTask.TaskSnapshot taskSnapshot) {
                installBtn.setText("Lädt (pausiert)");
            }
        });
    }
}
