package fr.wildcodeschool.signinsignout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_GET_SINGLE_FILE = 412;
    private static final int REQUEST_TAKE_PHOTO = 1984;
    private static final String TYPE_IMAGE = "image/*";
    private FirebaseAuth mAuth;
    private UserModel mUser = null;
    private Uri mDownloadUri = null;
    private String mCurrentPhotoPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        Button btGallery = findViewById(R.id.button_gallery);
        btGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(TYPE_IMAGE);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_photo)), REQUEST_GET_SINGLE_FILE);
            }
        });
        Button btCamera = findViewById(R.id.button_camera);
        btCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        final EditText etName = findViewById(R.id.et_name);
        final EditText etAge = findViewById(R.id.et_age);
        final ImageView ivPhoto = findViewById(R.id.iv_photo);

        // load user infos
        final UserSingleton singleton = UserSingleton.getInstance();
        mUser = singleton.getUser();
        if (mUser != null) {
            etName.setText(mUser.getName());
            etAge.setText(String.valueOf(mUser.getAge()));
            String photo = mUser.getPhoto();
            if (photo != null && !photo.isEmpty()) {
                Glide.with(ProfileActivity.this).load(photo).into(ivPhoto);
            }
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference userRef = database.getReference("user");
        final String userId = mAuth.getUid();

        Button disconnect = findViewById(R.id.button_disconnect);
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                singleton.clear();
                startActivity(new Intent(ProfileActivity.this, SignUpActivity.class));
                finish();
            }
        });

        Button updateProfile = findViewById(R.id.button_update);
        updateProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = etName.getText().toString();
                String ageValue = etAge.getText().toString();

                if (ageValue.isEmpty() || name.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, R.string.error_fields, Toast.LENGTH_SHORT).show();
                    return;
                }
                int age = Integer.parseInt(ageValue);
                if (mUser == null) {
                    mUser = new UserModel();
                }
                mUser.setAge(age);
                mUser.setName(name);
                if (mDownloadUri != null) {
                    mUser.setPhoto(mDownloadUri.toString());
                }
                userRef.child(userId).setValue(mUser);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Uri uri = null;
            if (requestCode == REQUEST_GET_SINGLE_FILE) {
                uri = data.getData();
            }
            if (requestCode == REQUEST_TAKE_PHOTO) {
                File file = new File(mCurrentPhotoPath);
                uri = Uri.fromFile(file);
            }
            if (uri != null) {
                ImageView ivPhoto = findViewById(R.id.iv_photo);
                Glide.with(ProfileActivity.this).load(uri)
                        .into(ivPhoto);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                // TODO : gérer les cas d'erreurs
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "fr.wildcodeschool.signinsignout.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
}
