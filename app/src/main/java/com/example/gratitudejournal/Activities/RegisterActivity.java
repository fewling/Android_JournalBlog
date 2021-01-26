package com.example.gratitudejournal.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gratitudejournal.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


public class RegisterActivity extends AppCompatActivity {

    public static final int GALLERY_INTENT_REQUEST_CODE = 1;
    private static final String TAG = "Sign in tag";
    static int permissionRequestCode = 1;

    private ImageView mImgUserPhoto;
    private Uri pickedImgUri;
    private FirebaseAuth mAuth;
    private EditText mUserName, mUserEmail, mUserPassword, mUserPassword2;
    private ProgressBar mLoadingProgress;
    private Button mRegButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // initialize views
        mUserName = findViewById(R.id.regName);
        mUserEmail = findViewById(R.id.regMail);
        mUserPassword = findViewById(R.id.regPassword);
        mUserPassword2 = findViewById(R.id.regPassword2);

        mRegButton = findViewById(R.id.regButton);
        mRegButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mRegButton.setVisibility(View.INVISIBLE);
                mLoadingProgress.setVisibility(View.VISIBLE);

                final String name = mUserName.getText().toString();
                final String email = mUserEmail.getText().toString();
                final String password = mUserPassword.getText().toString();
                final String password2 = mUserPassword2.getText().toString();

                if (name.isEmpty() || email.isEmpty() || password.isEmpty() ||
                        password2.isEmpty() || !password.equals(password2)) {

                    // Something goes wrong: all fields must be filled
                    // we need to display an error message
                    showMessage("Please verify all fields");
                    mRegButton.setVisibility(View.VISIBLE);
                    mLoadingProgress.setVisibility(View.INVISIBLE);

                } else {
                    // everything is ok and we can start creating user account
                    createUserAccount(name, email, password);

                }


            }
        });

        mLoadingProgress = findViewById(R.id.regProgressBar);
        mLoadingProgress.setVisibility(View.INVISIBLE);

        mImgUserPhoto = findViewById(R.id.regUserPhoto);
        mImgUserPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 22) {
                    checkAndRequestForPermission();
                } else {
                    openGallery();
                }
            }
        });


        mAuth = FirebaseAuth.getInstance();
    }

    private void createUserAccount(String name, String email, String password) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            Log.d(TAG, "createUserWithEmail:success");
                            showMessage("createUserWithEmail:success");

                            // Update user's profile picture and name
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            updateUserInfo(name, pickedImgUri, currentUser);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            showMessage("Authentication failed.");
                            mRegButton.setVisibility(View.VISIBLE);
                            mLoadingProgress.setVisibility(View.INVISIBLE);

                        }
                    }
                });


    }

    private void updateUserInfo(String name, Uri pickedImgUri, FirebaseUser currentUser) {

        // First upload user photo to firebase storage and get url
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("users_photos");
        StorageReference imageFilePath = storageReference.child(pickedImgUri.getLastPathSegment());
        imageFilePath.putFile(pickedImgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                // Image uploaded successfully
                // Able to get image url

                imageFilePath.getDownloadUrl()
                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        // uri contain user image url

                        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .setPhotoUri(uri)
                                .build();

                        currentUser.updateProfile(profileUpdate)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful()) {
                                            // user info updated successfully
                                            showMessage("Register complete");
                                            updateUI();
                                        } else {
                                            showMessage("Register failed");
                                        }
                                    }
                                });

                    }
                });


            }
        });


    }

    private void updateUI() {
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(intent);
        finish();
    }


    @Override
    public void onStart() {
        super.onStart();

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.reload();
        }
    }


    private void checkAndRequestForPermission() {


        if (ContextCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(RegisterActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                showMessage("Please accept for required permission");

            } else {

                ActivityCompat.requestPermissions(RegisterActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        permissionRequestCode);

            }

        } else {

            openGallery();

        }

    }


    private void openGallery() {
        // TODO: open gallery intent and wait for user to pick an image
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/");
        startActivityForResult(galleryIntent, GALLERY_INTENT_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == GALLERY_INTENT_REQUEST_CODE
                && data != null) {

            // The user has successfully picked an image.
            // We need to save its reference to a Uri variable.
            pickedImgUri = data.getData();
            mImgUserPhoto.setImageURI(pickedImgUri);


        }
    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}