package ch.epfl.esl.sportstracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;

public class viewStudent extends AppCompatActivity {

    private Profile userProfile;
    public static final String USER_PROFILE = "USER_PROFILE";
    public static final String NEW_LEVEL = "NEW_LEVEL";
    String level;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_view_student);

        Intent intent = getIntent();
        userProfile = (Profile) intent.getSerializableExtra(USER_PROFILE);

        Spinner productname_spinner =(Spinner) findViewById(R.id.spinner);

        productname_spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                        Object item = parent.getItemAtPosition(pos);
                        level = item.toString();
                    }
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        setUserImageAndProfileInfo();

    }

    private void setUserImageAndProfileInfo() {

        /*final InputStream imageStream;
        try {
            imageStream = new FileInputStream(userProfile.photoPath);
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            ImageView imageView = findViewById(R.id.studentImage);
            imageView.setImageBitmap(selectedImage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        if(userProfile.photoPath != "") {
            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl
                    (userProfile.photoPath);
            storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    final Bitmap selectedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes
                            .length);
                    ImageView imageView = findViewById(R.id.studentImage);
                    imageView.setImageBitmap(selectedImage);
                }
            });
        }

        TextView usernameTextView = findViewById(R.id.emailValue);
        usernameTextView.setText(userProfile.email);

        TextView passwordTextView = findViewById(R.id.usernameValue);
        passwordTextView.setText(userProfile.username);

        TextView heightTextView = findViewById(R.id.phoneValue);
        heightTextView.setText(String.valueOf(userProfile.phoneNumber));

        Spinner spinner = findViewById(R.id.spinner);
        //spinner.setPrompt(userProfile.userLevel);
        String[] levelArray = getResources().getStringArray(R.array.spinnerItems);
        spinner.setSelection(Arrays.asList(levelArray).indexOf(userProfile.userLevel));

        level = userProfile.userLevel;

    }

    public void clickedOKCallback(View view) {
        FirebaseDatabase.getInstance().getReference().child("Users").child(userProfile.userID).child("userLevel").setValue(level);
        finish();
    }

    public void monitorCallback(View view) {
        Intent intentStart = new Intent(viewStudent.this,
                LessonActivity.class);
        intentStart.putExtra("userID", userProfile.userID);
        intentStart.putExtra("isTeacher",true);
        startActivity(intentStart);
    }
}
