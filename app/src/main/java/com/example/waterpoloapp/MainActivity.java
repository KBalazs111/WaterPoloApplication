package com.example.waterpoloapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waterpoloapp.adapter.MatchAdapter;
import com.example.waterpoloapp.model.Match;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MatchAdapter matchAdapter;
    private List<Match> matches;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FloatingActionButton addMatchFab;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        addMatchFab = findViewById(R.id.addMatchFab);

        matches = new ArrayList<>();
        matchAdapter = new MatchAdapter(this, matches, isAdmin);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(matchAdapter);



        addMatchFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MatchDetailActivity.class);
            intent.putExtra("isAdmin", isAdmin);
            intent.putExtra("isNewMatch", true);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        checkIfUserIsAdmin();
    }



    @Override
    protected void onResume() {
        super.onResume();

        loadMatches();
    }

    private void checkIfUserIsAdmin() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("isAdmin")) {
                        isAdmin = documentSnapshot.getBoolean("isAdmin");
                        matchAdapter = new MatchAdapter(this, matches, isAdmin);
                        recyclerView.setAdapter(matchAdapter);
                        addMatchFab.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    }
                    loadMatches();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this,
                            "Hiba történt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMatches() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        // Query 1: Rendezés dátum szerint, limitálás
        firestore.collection("matches")
                .orderBy("matchDate", Query.Direction.ASCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    matches.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Match match = document.toObject(Match.class);
                        matches.add(match);

                        scheduleMatchNotification(match);
                    }

                    matchAdapter.updateMatches(matches);
                    progressBar.setVisibility(View.GONE);

                    if (matches.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this,
                            "Adatok betöltése sikertelen: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void scheduleMatchNotification(Match match) {
        if (match.getMatchDate() == null || match.getMatchDate().before(new Date())) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, MatchNotificationReceiver.class);
        intent.putExtra("matchId", match.getId());
        intent.putExtra("team1Name", match.getTeam1Name());
        intent.putExtra("team2Name", match.getTeam2Name());


        long notificationTime = match.getMatchDate().getTime() - (60 * 60 * 1000);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                match.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_teams) {
            Intent intent = new Intent(MainActivity.this, TeamListActivity.class);
            intent.putExtra("isAdmin", isAdmin);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        } else if (id == R.id.action_logout) {
            firebaseAuth.signOut();
            navigateToLogin();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}