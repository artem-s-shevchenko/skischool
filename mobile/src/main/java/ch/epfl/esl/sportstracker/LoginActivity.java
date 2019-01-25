package ch.epfl.esl.sportstracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    private static final int REGISTER_PROFILE = 1;

    private Profile userProfile;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseAuth.getInstance().signOut();
        userProfile = null;
        Button rButton = findViewById(R.id.RegisterButton);
        rButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intentEditProfile = new Intent(LoginActivity.this, EditProfileActivity
                        .class);
                startActivityForResult(intentEditProfile, REGISTER_PROFILE);
            }
        });
    }


    public void clickedLoginButtonXmlCallback(View view) {

        TextView email = findViewById(R.id.Studentemail);
        final TextView password = findViewById(R.id.userName);

        if(!TextUtils.isEmpty(email.getText()) && !TextUtils.isEmpty(password.getText())) {
            mAuth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithEmail:success");
                                String user_id = task.getResult().getUser().getUid();
                                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
                                readUserProfile(current_user_db);

                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                TextView mTextView = findViewById(R.id.LoginMessage);
                                mTextView.setText(R.string.not_registered_yet);
                                mTextView.setTextColor(Color.RED);
                            }

                            // ...
                        }
                    });

            Log.d("myTag", "jambon " + userProfile);
        }else{
            TextView mTextView = findViewById(R.id.LoginMessage);
            mTextView.setText(R.string.not_registered_yet);
            mTextView.setTextColor(Color.RED);
        }

    }

    private void readUserProfile(final DatabaseReference current_user_db) {
        current_user_db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                current_user_db.removeEventListener(this);
                final TextView password = findViewById(R.id.userName);
                String email_db = dataSnapshot.child("email").getValue(String.class);
                String number_db = dataSnapshot.child("number").getValue(String.class);
                String username_db = dataSnapshot.child("username").getValue(String.class);
                String userID = dataSnapshot.child("userID").getValue(String.class);
                String teacherID = dataSnapshot.child("myTeacher").getValue(String.class);
                String userLevel = dataSnapshot.child("userLevel").getValue(String.class);
                PersonType personType_db = dataSnapshot.child("personType").getValue(PersonType.class);
                String photo = dataSnapshot.child("photo").getValue(String.class);
                userProfile = new Profile(email_db, personType_db);
                userProfile.phoneNumber = number_db;
                userProfile.username = username_db;
                userProfile.photoPath = photo;
                userProfile.userID = userID;
                userProfile.myTeacher = teacherID;
                userProfile.userLevel = userLevel;
                if(userProfile != null) {
                    userProfile.password = password.getText().toString();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra(MyProfileFragment.USER_PROFILE, userProfile);
                    startActivity(intent);
                }
                Log.d("myTag", userProfile.phoneNumber + userProfile.email + userProfile.photoPath + userProfile.person);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
// Empty
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REGISTER_PROFILE && resultCode == RESULT_OK && data != null) {
            userProfile = (Profile) data.getSerializableExtra(MyProfileFragment.USER_PROFILE);
            if (userProfile != null) {
                TextView email = findViewById(R.id.Studentemail);
                email.setText(userProfile.email);
                TextView password = findViewById(R.id.userName);
                password.setText(userProfile.password);
            }
        }
    }
}
