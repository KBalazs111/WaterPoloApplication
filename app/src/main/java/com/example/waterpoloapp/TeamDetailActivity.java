package com.example.waterpoloapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.waterpoloapp.model.Team;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TeamDetailActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CAMERA = 101;
    private static final int PERMISSION_REQUEST_STORAGE = 102;

    private EditText teamNameEditText;
    private ImageView teamLogoImageView;
    private Button takePhotoButton, chooseFromGalleryButton, saveButton, deleteButton;
    private ProgressBar progressBar;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private String teamId;
    private boolean isAdmin;
    private boolean isNewTeam;
    private String currentPhotoPath;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_detail);


        teamId = getIntent().getStringExtra("teamId");
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);
        isNewTeam = getIntent().getBooleanExtra("isNewTeam", false);


        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(isNewTeam ? R.string.add_team : R.string.team_details);


        teamNameEditText = findViewById(R.id.teamNameEditText);
        teamLogoImageView = findViewById(R.id.teamLogoImageView);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        chooseFromGalleryButton = findViewById(R.id.chooseFromGalleryButton);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);
        progressBar = findViewById(R.id.progressBar);


        Animation buttonAnimation = AnimationUtils.loadAnimation(this, R.anim.button_animation);

        takePhotoButton.setOnClickListener(v -> {
            v.startAnimation(buttonAnimation);
            checkCameraPermission();
        });


        chooseFromGalleryButton.setOnClickListener(v -> {
            v.startAnimation(buttonAnimation);
            checkStoragePermission();
        });


        saveButton.setOnClickListener(v -> {
            v.startAnimation(buttonAnimation);
            saveTeam();
        });


        deleteButton.setOnClickListener(v -> {
            v.startAnimation(buttonAnimation);
            showDeleteConfirmationDialog();
        });


        setFieldsEnabled(isAdmin);

        if (!isNewTeam) {
            loadTeamDetails();
        }
    }

    private void loadTeamDetails() {
        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("teams")
                .document(teamId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (documentSnapshot.exists()) {
                        Team team = documentSnapshot.toObject(Team.class);


                        teamNameEditText.setText(team.getName());


                        if (team.getLogoUrl() != null && !team.getLogoUrl().isEmpty()) {
                            Glide.with(this)
                                    .load(team.getLogoUrl())
                                    .placeholder(R.drawable.logo_placeholder)
                                    .error(R.drawable.logo_placeholder)
                                    .into(teamLogoImageView);
                        }


                        deleteButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(TeamDetailActivity.this,
                                "A csapat nem található", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(TeamDetailActivity.this,
                            "Csapat betöltése sikertelen: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_STORAGE);
        } else {
            openGallery();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Hiba a kép fájl létrehozásakor",
                        Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.waterpoloapp.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );


        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Kamera engedély megtagadva",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Tárhely hozzáférés megtagadva",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {

                File f = new File(currentPhotoPath);
                selectedImageUri = Uri.fromFile(f);
                Glide.with(this).load(selectedImageUri).into(teamLogoImageView);
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                selectedImageUri = data.getData();
                Glide.with(this).load(selectedImageUri).into(teamLogoImageView);
            }
        }
    }

    private void saveTeam() {
        String teamName = teamNameEditText.getText().toString().trim();


        if (teamName.isEmpty()) {
            teamNameEditText.setError("Csapat nevének megadása kötelező");
            teamNameEditText.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        if (selectedImageUri != null) {

            String fileName = "team_logos/" + System.currentTimeMillis() + ".jpg";
            StorageReference fileRef = storageRef.child(fileName);

            UploadTask uploadTask = fileRef.putFile(selectedImageUri);
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                return fileRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();

                    saveTeamToFirestore(teamName, downloadUri.toString());
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(TeamDetailActivity.this,
                            "Kép feltöltése sikertelen: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {

            if (isNewTeam) {
                saveTeamToFirestore(teamName, "");
            } else {

                firestore.collection("teams")
                        .document(teamId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Team team = documentSnapshot.toObject(Team.class);
                                saveTeamToFirestore(teamName, team.getLogoUrl());
                            } else {
                                saveTeamToFirestore(teamName, "");
                            }
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(TeamDetailActivity.this,
                                    "Adatok lekérése sikertelen: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    private void saveTeamToFirestore(String teamName, String logoUrl) {
        Map<String, Object> teamData = new HashMap<>();
        teamData.put("name", teamName);
        teamData.put("logoUrl", logoUrl);

        if (isNewTeam) {

            firestore.collection("teams")
                    .add(teamData)
                    .addOnSuccessListener(documentReference -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TeamDetailActivity.this,
                                "Csapat sikeresen hozzáadva", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TeamDetailActivity.this,
                                "Csapat hozzáadása sikertelen: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {

            firestore.collection("teams")
                    .document(teamId)
                    .update(teamData)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TeamDetailActivity.this,
                                "Csapat sikeresen frissítve", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TeamDetailActivity.this,
                                "Csapat frissítése sikertelen: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteTeam() {
        progressBar.setVisibility(View.VISIBLE);


        firestore.collection("matches")
                .whereEqualTo("team1Id", teamId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots1 -> {
                    if (!queryDocumentSnapshots1.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TeamDetailActivity.this,
                                "Nem törölhető a csapat, mert mérkőzés van hozzárendelve!",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    firestore.collection("matches")
                            .whereEqualTo("team2Id", teamId)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                if (!queryDocumentSnapshots2.isEmpty()) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(TeamDetailActivity.this,
                                            "Nem törölhető a csapat, mert mérkőzés van hozzárendelve!",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }


                                firestore.collection("teams")
                                        .document(teamId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(TeamDetailActivity.this,
                                                    "Csapat sikeresen törölve",
                                                    Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(TeamDetailActivity.this,
                                                    "Csapat törlése sikertelen: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(TeamDetailActivity.this,
                                        "Adatok lekérése sikertelen: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(TeamDetailActivity.this,
                            "Adatok lekérése sikertelen: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirmation)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteTeam())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void setFieldsEnabled(boolean enabled) {
        teamNameEditText.setEnabled(enabled);
        takePhotoButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
        chooseFromGalleryButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
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
