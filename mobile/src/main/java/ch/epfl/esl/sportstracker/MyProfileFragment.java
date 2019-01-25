package ch.epfl.esl.sportstracker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MyProfileFragment extends Fragment {

    public static final String USER_PROFILE = "USER_PROFILE";
    public static final String USER_ID = "USER_ID";
    private final String TAG = this.getClass().getSimpleName();

    private static final int EDIT_PROFILE_INFO = 1;

    private OnFragmentInteractionListener mListener;
    private Profile userProfile;
    private View fragmentView;

    public MyProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_my_profile, container, false);
        Button startButton = fragmentView.findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    getActivity().getPackageManager().getPackageInfo("com.google.android.wearable.app", PackageManager.GET_META_DATA);
                    startMonitoringOnWear();
                    Intent intentStart = new Intent(getActivity(),
                            LessonActivity.class);
                    intentStart.putExtra("userID", userProfile.userID);
                    intentStart.putExtra("isTeacher",false);
                    startActivity(intentStart);
                } catch (PackageManager.NameNotFoundException e) {
                    //android wear app is not installed
                    Toast.makeText(getContext(), "The Android Wear App is NOT installed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Intent intent = getActivity().getIntent();
        userProfile = (Profile) intent.getSerializableExtra(USER_PROFILE);
        setUserImageAndProfileInfo();
        //sendProfileToWatch();

        return fragmentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_my_profile, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                Intent intentEditProfile = new Intent(getActivity(), EditProfileActivity.class);
                intentEditProfile.putExtra(USER_PROFILE, userProfile);
                startActivityForResult(intentEditProfile, EDIT_PROFILE_INFO);
                break;
            case R.id.sign_out:
                FirebaseAuth.getInstance().signOut();
                userProfile = null;
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_PROFILE_INFO && resultCode == AppCompatActivity.RESULT_OK) {
            Profile uProfile = (Profile) data.getSerializableExtra(USER_PROFILE);
            userProfile.photoPath = uProfile.photoPath;
            userProfile.phoneNumber = uProfile.phoneNumber;
            userProfile.username = uProfile.username;
            if (userProfile != null) {
                setUserImageAndProfileInfo();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement " +
                    "OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);


    }

    private void setUserImageAndProfileInfo() {
        /*final InputStream imageStream;
        try {
            imageStream = new FileInputStream(userProfile.photoPath);
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            ImageView imageView = fragmentView.findViewById(R.id.studentImage);
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
                    if (isAdded()) {
                        final Bitmap selectedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes
                                .length);
                        ImageView imageView = fragmentView.findViewById(R.id.studentImage);
                        imageView.setImageBitmap(selectedImage);
                    }
                }
            });
        }

        TextView usernameTextView = fragmentView.findViewById(R.id.emailValue);
        usernameTextView.setText(userProfile.email);

        TextView passwordTextView = fragmentView.findViewById(R.id.usernameValue);
        passwordTextView.setText(userProfile.password);

        TextView heightTextView = fragmentView.findViewById(R.id.phoneValue);
        heightTextView.setText(String.valueOf(userProfile.phoneNumber));

        TextView weightTextView = fragmentView.findViewById(R.id.levelValue);
        weightTextView.setText(String.valueOf(userProfile.username));
    }

    private void sendProfileToWatch() {
        Intent intentWear = new Intent(getActivity(), WearService.class);
        intentWear.setAction(WearService.ACTION_SEND.PROFILE_SEND.name());
        intentWear.putExtra(WearService.PROFILE, userProfile);
        getActivity().startService(intentWear);
    }


    private void startMonitoringOnWear() {
        Intent intentStartRec = new Intent(getActivity(), WearService.class);
        intentStartRec.setAction(WearService.ACTION_SEND.STARTACTIVITY.name());
        intentStartRec.putExtra(WearService.ACTIVITY_TO_START, BuildConfig
                .W_excerciseactivity);
        getActivity().startService(intentStartRec);
    }
}
