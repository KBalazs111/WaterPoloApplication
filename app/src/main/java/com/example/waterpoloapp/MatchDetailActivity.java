package com.example.waterpoloapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.waterpoloapp.model.Match;
import com.example.waterpoloapp.model.Team;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MatchDetailActivity extends AppCompatActivity {

    private Spinner team1Spinner, team2Spinner;
    private Button dateButton, timeButton, saveButton, deleteButton;
    private TextView dateTimeTextView;
    private ProgressBar progressBar;

    private FirebaseFirestore firestore;
    private List<Team> teams;
    private List<String> teamNames;
    private Map<String, String> teamIdMap;

    private String matchId;
    private boolean isAdmin;
    private boolean isNewMatch;
    private Date selectedDate;
    private Calendar calendar;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd. HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_detail);

        matchId = getIntent().getStringExtra("matchId");
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);
        isNewMatch = getIntent().getBooleanExtra("isNewMatch", false);


        firestore = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(isNewMatch ? R.string.add_match : R.string.match_details);


        team1Spinner = findViewById(R.id.team1Spinner);
        team2Spinner = findViewById(R.id.team2Spinner);
        dateButton = findViewById(R.id.dateButton);
        timeButton = findViewById(R.id.timeButton);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        progressBar = findViewById(R.id.progressBar);


        Animation buttonAnimation = AnimationUtils.loadAnimation(this, R.anim.button_animation);


        teams = new ArrayList<>();
        teamNames = new ArrayList<>();
        teamIdMap = new HashMap<>();
        calendar = Calendar.getInstance();
        selectedDate = calendar.getTime();
        updateDateTimeTextView();


        dateButton.setOnClickListener(v -> {
            v.startAnimation(buttonAnimation);
            showDatePickerDialog();
        });


        timeButton.setOnClickListener(v -> {
            v.startAnimation(buttonAnimation);
            showTimePickerDialog();
        });


        saveButton.setOnClickListener(v -> {
            v.startAnimation(buttonAnimation);
            saveMatch();
        });


        deleteButton.setOnClickListener(v -> {
            v.startAnimation(buttonAnimation);
            showDeleteConfirmationDialog();
        });


        setFieldsEnabled(isAdmin);


        loadTeams();
    }

    private void loadTeams() {
        progressBar.setVisibility(View.VISIBLE);

        // Query 2: Csapatok lekérése név szerinti rendezéssel
        firestore.collection("teams")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    teams.clear();
                    teamNames.clear();
                    teamIdMap.clear();

                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        Team team = queryDocumentSnapshots.getDocuments().get(i).toObject(Team.class);
                        teams.add(team);
                        teamNames.add(team.getName());
                        teamIdMap.put(team.getName(), team.getId());
                    }

                    setupSpinners();

                    if (!isNewMatch) {
                        loadMatchDetails();
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MatchDetailActivity.this,
                            "Csapatok betöltése sikertelen: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, teamNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        team1Spinner.setAdapter(adapter);
        team2Spinner.setAdapter(adapter);

        if (teamNames.size() >= 2) {
            team1Spinner.setSelection(0);
            team2Spinner.setSelection(1);
        }
    }

    private void loadMatchDetails() {
        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("matches")
                .document(matchId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (documentSnapshot.exists()) {
                        Match match = documentSnapshot.toObject(Match.class);


                        if (match.getMatchDate() != null) {
                            selectedDate = match.getMatchDate();
                            calendar.setTime(selectedDate);
                            updateDateTimeTextView();
                        }

                        setSpinnerSelection(team1Spinner, match.getTeam1Name());
                        setSpinnerSelection(team2Spinner, match.getTeam2Name());


                        deleteButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(MatchDetailActivity.this,
                                "A mérkőzés nem található", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MatchDetailActivity.this,
                            "Mérkőzés betöltése sikertelen: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setSpinnerSelection(Spinner spinner, String teamName) {
        if (teamName != null) {
            for (int i = 0; i < teamNames.size(); i++) {
                if (teamNames.get(i).equals(teamName)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDate = calendar.getTime();
                    updateDateTimeTextView();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    selectedDate = calendar.getTime();
                    updateDateTimeTextView();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void updateDateTimeTextView() {
        dateTimeTextView.setText(dateFormat.format(selectedDate));
    }

    private void saveMatch() {

        if (team1Spinner.getSelectedItemPosition() == team2Spinner.getSelectedItemPosition()) {
            Toast.makeText(this, "A két csapat nem lehet ugyanaz", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);


        String team1Name = teamNames.get(team1Spinner.getSelectedItemPosition());
        String team2Name = teamNames.get(team2Spinner.getSelectedItemPosition());
        String team1Id = teamIdMap.get(team1Name);
        String team2Id = teamIdMap.get(team2Name);

        Map<String, Object> matchData = new HashMap<>();
        matchData.put("team1Id", team1Id);
        matchData.put("team2Id", team2Id);
        matchData.put("team1Name", team1Name);
        matchData.put("team2Name", team2Name);
        matchData.put("matchDate", selectedDate);


        if (isNewMatch) {

            firestore.collection("matches")
                    .add(matchData)
                    .addOnSuccessListener(documentReference -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MatchDetailActivity.this,
                                "Mérkőzés sikeresen hozzáadva", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MatchDetailActivity.this,
                                "Mérkőzés hozzáadása sikertelen: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {

            firestore.collection("matches")
                    .document(matchId)
                    .update(matchData)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MatchDetailActivity.this,
                                "Mérkőzés sikeresen frissítve", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MatchDetailActivity.this,
                                "Mérkőzés frissítése sikertelen: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteMatch() {
        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("matches")
                .document(matchId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MatchDetailActivity.this,
                            "Mérkőzés sikeresen törölve", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MatchDetailActivity.this,
                            "Mérkőzés törlése sikertelen: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirmation)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteMatch())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void setFieldsEnabled(boolean enabled) {
        team1Spinner.setEnabled(enabled);
        team2Spinner.setEnabled(enabled);
        dateButton.setEnabled(enabled);
        timeButton.setEnabled(enabled);
        saveButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
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
