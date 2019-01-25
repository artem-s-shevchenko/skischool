package ch.epfl.esl.sportstracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.widget.Toast.LENGTH_SHORT;

public class EditProfileActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    private static final int PICK_IMAGE = 1;
    private File imageFile;
    private Profile userProfile;

    private static final FirebaseDatabase database = FirebaseDatabase
            .getInstance();
    private static final DatabaseReference profileGetRef = database
            .getReference("profiles");
    private FirebaseAuth mAuth;
    private boolean isNewUser = true;
    private boolean isTeacherRegister = false;
    String teacherID;
    private Uri savedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Intent intent = getIntent();
        userProfile = (Profile) intent.getSerializableExtra(MyProfileFragment.USER_PROFILE);
        if (userProfile != null) {
            setProfileToEdit();
            TextView emailTextView = findViewById(R.id.editEmail);
            TextView passwordTextView = findViewById(R.id.editPassword);
            emailTextView.setFocusable(false);
            passwordTextView.setFocusable(false);
            emailTextView.setBackgroundColor(0x808080);
            passwordTextView.setBackgroundColor(0x808080);
            RadioButton b1 = (RadioButton) findViewById(R.id.radioStudent);
            RadioButton b2 = (RadioButton) findViewById(R.id.radioTeacher);
            b1.setEnabled(false);
            b2.setEnabled(false);
            isNewUser = false;
            if(userProfile.person == PersonType.STUDENT) {
                b1.setChecked(true);
            }
            else {
                b2.setChecked(true);
            }
        }

        if (savedInstanceState != null) {
            savedImageUri = savedInstanceState.getParcelable("ImageUri");
            if (savedImageUri != null) {
                try {
                    InputStream imageStream = getContentResolver().openInputStream(savedImageUri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    ImageView imageView = findViewById(R.id.studentImage);
                    imageView.setImageBitmap(selectedImage);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        mAuth = FirebaseAuth.getInstance();

    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("ImageUri", savedImageUri);
    }

    /*@Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_profile, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear:
                clearUser();
                break;
            case R.id.action_validate:
                editUser();
                //addProfileToFirebaseDB();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addProfileToFirebaseDB(final DatabaseReference profile) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) ((ImageView) findViewById(R.id
                .studentImage)).getDrawable();
        if (bitmapDrawable == null) {
            userProfile.photoPath = "";
            profile.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData
                                                                mutableData) {
                    mutableData.child("email").setValue(userProfile.email);
                    mutableData.child("number").setValue(userProfile.phoneNumber);
                    mutableData.child("username").setValue(userProfile.username);
                    RadioGroup radio = findViewById(R.id.radioGroup);
                    PersonType personType = (radio.getCheckedRadioButtonId() == R.id.radioTeacher) ? PersonType.TEACHER : PersonType.STUDENT;
                    mutableData.child("personType").setValue(personType);
                    mutableData.child("userID").setValue(userProfile.userID);
                    mutableData.child("myTeacher").setValue(userProfile.myTeacher);
                    mutableData.child("userLevel").setValue(userProfile.userLevel);
                    mutableData.child("photo").setValue(userProfile.photoPath);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError databaseError,
                                       boolean b, @Nullable DataSnapshot
                                               dataSnapshot) {
                    Intent intent = new Intent();
                    intent.putExtra(MyProfileFragment.USER_PROFILE, userProfile);
                    setResult(AppCompatActivity.RESULT_OK, intent);
                    finish();
                }
            });
        } else {
            Bitmap bitmap = bitmapDrawable.getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] data = baos.toByteArray();

            StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReference();
            StorageReference photoRef = storageRef.child("photos").child
                    (profile.getKey() + ".jpg");
            UploadTask uploadTask = photoRef.putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(EditProfileActivity.this, R.string
                            .photo_upload_failed, Toast.LENGTH_SHORT).show();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    taskSnapshot.getMetadata().getReference()
                            .getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(final Uri uri) {
                                    userProfile.photoPath = uri.toString();
                                    profile.runTransaction(new Transaction.Handler() {
                                        @NonNull
                                        @Override
                                        public Transaction.Result doTransaction(@NonNull MutableData
                                                                                        mutableData) {
                                            mutableData.child("email").setValue(userProfile.email);
                                            mutableData.child("number").setValue(userProfile.phoneNumber);
                                            mutableData.child("username").setValue(userProfile.username);
                                            RadioGroup radio = findViewById(R.id.radioGroup);
                                            PersonType personType = (radio.getCheckedRadioButtonId() == R.id.radioTeacher) ? PersonType.TEACHER : PersonType.STUDENT;
                                            mutableData.child("personType").setValue(personType);
                                            mutableData.child("userID").setValue(userProfile.userID);
                                            mutableData.child("myTeacher").setValue(userProfile.myTeacher);
                                            mutableData.child("userLevel").setValue(userProfile.userLevel);
                                            mutableData.child("photo").setValue(userProfile.photoPath);
                                            return Transaction.success(mutableData);
                                        }

                                        @Override
                                        public void onComplete(@Nullable DatabaseError databaseError,
                                                               boolean b, @Nullable DataSnapshot
                                                                       dataSnapshot) {
                                            Intent intent = new Intent();
                                            intent.putExtra(MyProfileFragment.USER_PROFILE, userProfile);
                                            setResult(AppCompatActivity.RESULT_OK, intent);
                                            finish();
                                        }
                                    });
                                }
                            });
                }
            });

        }


    }

    private void clearUser() {
        ImageView userImageView = findViewById(R.id.studentImage);
        TextView emailTextView = findViewById(R.id.editEmail);
        TextView passwordTextView = findViewById(R.id.editPassword);
        TextView numberTextView = findViewById(R.id.editNumber);
        TextView usernameTextView = findViewById(R.id.editUsername);

        userImageView.setImageDrawable(null);
        if(isNewUser == true) {
            emailTextView.setText("");
            passwordTextView.setText("");
        }
        numberTextView.setText("");
        usernameTextView.setText("");
    }

    private void setProfileToEdit() {
        ImageView userImageView = findViewById(R.id.studentImage);
        TextView usernameTextView = findViewById(R.id.editEmail);
        TextView passwordTextView = findViewById(R.id.editPassword);
        TextView numberTextView = findViewById(R.id.editNumber);
        TextView addressTextView = findViewById(R.id.editUsername);

        /*final InputStream imageStream;
        try {
            imageStream = new FileInputStream(userProfile.photoPath);
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            userImageView.setImageBitmap(selectedImage);
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
        usernameTextView.setText(userProfile.email);
        passwordTextView.setText(userProfile.password);
        numberTextView.setText(String.valueOf(userProfile.phoneNumber));
        addressTextView.setText(String.valueOf(userProfile.username));

    }

    public void chooseImage(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    private void editUser() {
        int correctInput = 0;
        TextView email = findViewById(R.id.editEmail);
        TextView password = findViewById(R.id.editPassword);
        RadioGroup radio = findViewById(R.id.radioGroup);
        final PersonType personType = (radio.getCheckedRadioButtonId() == R.id.radioTeacher) ? PersonType.TEACHER : PersonType.STUDENT;

        if (!isValidEmail(email.getText())) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.ToastEmail),
                    Toast.LENGTH_LONG);

            toast.show();

            email.setError(getString(R.string.ToastEmail));
        } else{
            correctInput++;
        }

        if(PasswordStrength.calculateStrength(password.getText().toString()). getValue() < PasswordStrength.STRONG.getValue()){
            Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.ToastPassword),
                    Toast.LENGTH_LONG);

            toast.show();

            password.setError(getString(R.string.ToastPassword));
        } else{
            correctInput++;
        }

        if(isNewUser == true)
            userProfile = new Profile(email.getText().toString(), password.getText().toString(), personType);

        TextView phoneNumber = findViewById(R.id.editNumber);
        TextView username = findViewById(R.id.editUsername);

        if(phoneNumber.getText().toString().length() != 10){
            Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.ToastPhone),
                    Toast.LENGTH_LONG);

            toast.show();

            phoneNumber.setError(getString(R.string.ToastPhone));
        } else{
            correctInput++;
        }

        if(isEmpty(username.getText())){
            Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.toastUsername),
                    Toast.LENGTH_LONG);

            toast.show();

            username.setError(getString(R.string.toastUsername));
        } else{
            correctInput++;
        }

        userProfile.username = username.getText().toString();

        userProfile.phoneNumber = phoneNumber.getText().toString();

        if (imageFile == null) {
            userProfile.photoPath = "";
        } else {
            userProfile.photoPath = imageFile.getPath();
        }

//        //Now we go back to LoginActivity, later this activity can be used
//        // also to edit the profile without going back to LoginActivity
//        // (there will be an if)
        if(correctInput == 4) {

            if (isNewUser == true){
                mAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d(TAG, "createUserWithEmail:success");
                                    DatabaseReference profiles = FirebaseDatabase.getInstance().getReference().child("Users");
                                    profiles.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            int minSizeStudent = Integer.MAX_VALUE;

                                            for (final DataSnapshot user : dataSnapshot.getChildren()) {
                                                PersonType personType_db = user.child("personType").getValue(PersonType.class);
                                                int count = 0;
                                                if(personType_db == PersonType.TEACHER){
                                                    isTeacherRegister = true;
                                                    for(final DataSnapshot students : user.child("Students").getChildren()){
                                                        count++;
                                                    }
                                                    if(count < minSizeStudent){
                                                        minSizeStudent = count;
                                                        teacherID = user.child("userID").getValue(String.class);
                                                    }
                                                }
                                            }

                                            FirebaseUser currentUser = mAuth.getCurrentUser();
                                            if(personType == PersonType.STUDENT) {
                                                Log.d("userprofile",personType.toString() + "STUDENT");
                                                if (isTeacherRegister == true){
                                                    Log.d("userprofile",userProfile.toString());
                                                    String user_id = currentUser.getUid();
                                                    DatabaseReference teacher_db = FirebaseDatabase.getInstance().getReference().child("Users").child(teacherID).child("Students");
                                                    DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
                                                    teacher_db.child(user_id).setValue(user_id);
                                                    userProfile.userID = user_id;
                                                    userProfile.myTeacher = teacherID;
                                                    userProfile.userLevel = "Beginner 1";
                                                    addProfileToFirebaseDB(current_user_db);

                                                }else{
                                                    Toast.makeText(getApplicationContext(), "A teacher needs to be register",
                                                            Toast.LENGTH_LONG).show();
                                                    currentUser.delete()
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        Log.d(TAG, "User account deleted.");
                                                                    }
                                                                }
                                                            });

                                                }
                                            }else{
                                                Log.d("userprofile",personType.toString() + "TEACHER");
                                                String user_id = currentUser.getUid();
                                                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
                                                userProfile.userID = user_id;
                                                userProfile.myTeacher = "";
                                                userProfile.userLevel = "";
                                                addProfileToFirebaseDB(current_user_db);

                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                        }
                                    });


                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(getApplicationContext(), "Authentication failed.",
                                            LENGTH_SHORT).show();
                                }

                                // ...
                            }
                        });
            }else{
                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child(userProfile.userID);
                addProfileToFirebaseDB(current_user_db);
            }

        }
    }

    public boolean isEmpty(CharSequence text){
        return TextUtils.isEmpty(text);
    }

    public enum PasswordStrength
    {

        WEAK(0, Color.RED), MEDIUM(1, Color.argb(255, 220, 185, 0)), STRONG(2, Color.GREEN), VERY_STRONG(3, Color.BLUE);

        //--------REQUIREMENTS--------
        static int REQUIRED_LENGTH = 6;
        static int MAXIMUM_LENGTH = 6;
        static boolean REQUIRE_SPECIAL_CHARACTERS = true;
        static boolean REQUIRE_DIGITS = true;
        static boolean REQUIRE_LOWER_CASE = true;
        static boolean REQUIRE_UPPER_CASE = true;

        int resId;
        int color;

        PasswordStrength(int resId, int color)
        {
            this.resId = resId;
            this.color = color;
        }

        public int getValue()
        {
            return resId;
        }

        public int getColor()
        {
            return color;
        }

        public static PasswordStrength calculateStrength(String password)
        {
            int currentScore = 0;
            boolean sawUpper = false;
            boolean sawLower = false;
            boolean sawDigit = false;
            boolean sawSpecial = false;

            for (int i = 0; i < password.length(); i++)
            {
                char c = password.charAt(i);

                if (!sawSpecial && !Character.isLetterOrDigit(c))
                {
                    currentScore += 1;
                    sawSpecial = true;
                }
                else
                {
                    if (!sawDigit && Character.isDigit(c))
                    {
                        currentScore += 1;
                        sawDigit = true;
                    }
                    else
                    {
                        if (!sawUpper || !sawLower)
                        {
                            if (Character.isUpperCase(c))
                                sawUpper = true;
                            else
                                sawLower = true;
                            if (sawUpper && sawLower)
                                currentScore += 1;
                        }
                    }
                }
            }

            if (password.length() > REQUIRED_LENGTH)
            {
                if ((REQUIRE_UPPER_CASE && !sawUpper) || (REQUIRE_LOWER_CASE && !sawLower) || (REQUIRE_DIGITS && !sawDigit))
                {
                    currentScore = 1;
                }
                else
                {
                    currentScore = 2;
                    if (password.length() > MAXIMUM_LENGTH)
                    {
                        currentScore = 3;
                    }
                }
            }
            else
            {
                currentScore = 0;
            }

            switch (currentScore)
            {
                case 0:
                    return WEAK;
                case 1:
                    return MEDIUM;
                case 2:
                    return STRONG;
                case 3:
                    return VERY_STRONG;
                default:
            }

            return VERY_STRONG;
        }

    }

    public final static boolean isValidEmail(CharSequence target) {
        if (target == null)
            return false;

        return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            imageFile = new File(getExternalFilesDir(null), "profileImage");
            try {
                copyImage(imageUri, imageFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            final InputStream imageStream;
            try {
                savedImageUri = Uri.fromFile(imageFile);
                imageStream = getContentResolver().openInputStream(Uri.fromFile(imageFile));
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                ImageView imageView = findViewById(R.id.studentImage);
                imageView.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyImage(Uri uriInput, File fileOutput) throws IOException {
        InputStream in = null;
        OutputStream out = null;

        try {
            in = getContentResolver().openInputStream(uriInput);
            out = new FileOutputStream(fileOutput);
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in.close();
            out.close();
        }
    }

}
