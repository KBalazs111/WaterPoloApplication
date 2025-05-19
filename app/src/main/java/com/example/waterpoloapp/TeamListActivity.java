package com.example.waterpoloapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waterpoloapp.adapter.TeamAdapter;
import com.example.waterpoloapp.model.Team;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeamListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TeamAdapter teamAdapter;
    private List<Team> teams;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FloatingActionButton addTeamFab;

    private FirebaseFirestore firestore;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_list);


        isAdmin = getIntent().getBooleanExtra("isAdmin", false);


        firestore = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        addTeamFab = findViewById(R.id.addTeamFab);


        teams = new ArrayList<>();
        teamAdapter = new TeamAdapter(this, teams, isAdmin);


        int spanCount = getResources().getConfiguration().orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        recyclerView.setAdapter(teamAdapter);


        addTeamFab.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        addTeamFab.setOnClickListener(v -> {
            Intent intent = new Intent(TeamListActivity.this, TeamDetailActivity.class);
            intent.putExtra("isAdmin", isAdmin);
            intent.putExtra("isNewTeam", true);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadTeams();
    }

    private void loadTeams() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        // Query 3: Csapatok lekérése név szerinti rendezéssel
        firestore.collection("teams")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    teams.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Team team = document.toObject(Team.class);
                        teams.add(team);
                    }

                    teamAdapter.updateTeams(teams);
                    progressBar.setVisibility(View.GONE);

                    if (teams.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(TeamListActivity.this,
                            "Adatok betöltése sikertelen: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
