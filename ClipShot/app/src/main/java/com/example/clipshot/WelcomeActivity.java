package com.example.clipshot;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WelcomeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE =1;

    // Declaring Variables
    Uri imageUri;
    ImageView img;
    FirebaseStorage imageStorage;
    StorageReference storageReference;
    String userUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    ProgressBar progressBar;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Call TopBar
        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.welcome_action_bar);
        // Set Elevation to Top Bar
        getSupportActionBar().setElevation(50f); // Float == px

        // Declaring interface components
        img = findViewById(R.id.image);
        EditText name = findViewById(R.id.realName);
        EditText username = findViewById(R.id.displayName);
        AppCompatImageView iconDone = findViewById(R.id.iconDone);
        EditText bio = findViewById(R.id.bio);
        EditText steamInput = findViewById(R.id.steamInput);
        EditText originInput = findViewById(R.id.originInput);
        EditText psnInput = findViewById(R.id.psnInput);
        EditText xboxInput = findViewById(R.id.xboxInput);
        EditText nintendoInput = findViewById(R.id.switchInput);
        TextView errorUsername = findViewById(R.id.labelErrorUsername);

        // Only allows user to input 3 lines into bio
        bio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // limit to 3 lines
                if (bio.getLayout().getLineCount() > 3)
                    bio.getText().delete(bio.getText().length() - 1, bio.getText().length());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Automatically fill avatar with Google Account Image and real name
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {

            String personName = acct.getDisplayName();

            name.setText(personName);
        }

        Glide.with(this).load(R.drawable.default_avatar).into(img);

        // Listener that will check if username is not empty, if not the check button will appear and allow user go to feed page
        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.toString().trim().length()==0){

                    iconDone.setVisibility(View.INVISIBLE);
                }
                else iconDone.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Listener to call method to pick an image from gallery
        img.setOnClickListener(v -> pickImageFromGallery());
        progressBar = findViewById(R.id.progress_circular);

        // Listener that inserts data into database and changes to the Main Activity
        iconDone.setOnClickListener(v -> {

            // Declaring variables
            String dataName, dataUsername, dataBio, dataSteam, dataOrigin, dataPsn, dataXbox, dataNintendo, dataGamifyTitle, email;
            FirebaseFirestore db;

            // Gets the user google email that will create a collection with that email
            assert acct != null;
            email = acct.getEmail();

            // Firestore instance
            db = FirebaseFirestore.getInstance();
            imageStorage = FirebaseStorage.getInstance();
            storageReference = imageStorage.getReference();

            // Declaring variables that will be inserted in Firestore
            dataUsername = username.getText().toString().toLowerCase();
            dataName = name.getText().toString();
            dataBio = bio.getText().toString();
            dataSteam = steamInput.getText().toString();
            dataOrigin = originInput.getText().toString();
            dataPsn = psnInput.getText().toString();
            dataXbox = xboxInput.getText().toString();
            dataNintendo = nintendoInput.getText().toString();
            dataGamifyTitle = "Beginner";

           CollectionReference usersRef = db.collection("users");
            Query query = usersRef.whereEqualTo("Username", dataUsername);
            query.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot documentSnapshot : Objects.requireNonNull(task.getResult())) {
                        String user = documentSnapshot.getString("Username");

                        assert user != null;
                        if (user.equals(dataUsername)) {

                            errorUsername.setVisibility(View.VISIBLE);

                            TextView labelRealName = findViewById(R.id.labelRealName);

                            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) labelRealName.getLayoutParams();
                            lp.setMargins(130, 90, 0, 0);
                            labelRealName.setLayoutParams(lp);
                        }
                    }

                    if (task.getResult().size() == 0) {

                        errorUsername.setVisibility(View.INVISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                        iconDone.setVisibility(View.INVISIBLE);

                        // Map that will fill our database with values
                        Map<String, Object> Userdata = new HashMap<>();
                        Userdata.put("Username", dataUsername);
                        Userdata.put("Name", dataName);
                        Userdata.put("Bio", dataBio);
                        Userdata.put("Steam", dataSteam);
                        Userdata.put("Origin", dataOrigin);
                        Userdata.put("Psn", dataPsn);
                        Userdata.put("Xbox", dataXbox);
                        Userdata.put("Nintendo", dataNintendo);
                        Userdata.put("GamifyTitle", dataGamifyTitle);
                        Userdata.put("Email", email);
                        Userdata.put("UserUID", userUid);
                        Userdata.put("Followers", "0");
                        Userdata.put("Following", "0");
                        String[] followersArray = new String[0];
                        Userdata.put("UsersFollowers", Arrays.asList(followersArray));
                        String[] followingArray = new String[0];
                        Userdata.put("UsersFollowing", Arrays.asList(followingArray));

                        // Call the method to upload image
                        uploadImage(email);

                        // On success data is inserted in database and user go to MainActivity
                        db.collection("users").document(userUid).set(Userdata).addOnSuccessListener(aVoid -> {
                            Intent goToFeed = new Intent(WelcomeActivity.this, MainActivity.class);
                            startActivity(goToFeed);
                        });
                    }
                }
            });
        });
    }

    // Method to go to gallery
    private void pickImageFromGallery(){

        Intent gallery = new Intent(Intent.ACTION_PICK);
        gallery.setType("image/*");
        startActivityForResult(Intent.createChooser(gallery, "Select Video"), PICK_IMAGE);
    }

    // Fill welcome avatar image with another from gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {

            imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                img.setImageBitmap(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Create a folder in Firebase Storage with the user email and upload the image from gallery
    public void uploadImage(String email){

        if (imageUri != null){
            StorageReference ref = storageReference.child(email+"/" + userUid);
            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                    });
        }
    }

    // Not calling **super**, disables back button in current screen.
    @Override
    public void onBackPressed() {
        // super.onBackPressed();
    }
}