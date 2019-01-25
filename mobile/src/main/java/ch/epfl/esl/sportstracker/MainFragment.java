package ch.epfl.esl.sportstracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static android.app.Activity.RESULT_OK;
import static ch.epfl.esl.sportstracker.MyProfileFragment.USER_PROFILE;

public class MainFragment extends Fragment {

    private final String TAG = this.getClass().getSimpleName();
    private View fragmentView;

    private OnFragmentInteractionListener mListener;
    private Profile userProfile;
    PersonAdapter adapter;
    private ArrayList<Person> listPerson;
    private ArrayList<Profile> listUsers;
    private DatabaseReference user_db;
    private Random r;
    private ArrayList<String> needHelp;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        // Inflate the layout for this fragment

        fragmentView = inflater.inflate(R.layout.fragment_main, container, false);

        Intent intent = getActivity().getIntent();
        userProfile = (Profile) intent.getSerializableExtra(USER_PROFILE);
        listPerson = new ArrayList<Person>();
        listUsers = new ArrayList<Profile>();
        needHelp = new ArrayList<String>();
        r= new Random();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Ski school";
            String description = "Ski school channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("SKISCH",
                    name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }


        RecyclerView rvPersons = (RecyclerView) fragmentView.findViewById(R.id.recyclerView);


        adapter = new PersonAdapter(listPerson);

        user_db = FirebaseDatabase.getInstance().getReference().child("Users");
        user_db.addValueEventListener(new DBListener());

        rvPersons.setAdapter(adapter);
        rvPersons.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        adapter.setOnItemClickListener(new PersonAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                //Toast.makeText(getActivity().getApplicationContext(),  " was clicked!", Toast.LENGTH_SHORT).show();
                if(userProfile.person == PersonType.TEACHER) {
                    Intent intent = new Intent(getActivity(), viewStudent.class);
                    Profile send = listUsers.get(position);
                    intent.putExtra(MyProfileFragment.USER_PROFILE, send);
                    startActivity(intent);
                }
            }
        });

        TextView personTypeTextView = fragmentView.findViewById(R.id.personTypeText);
        personTypeTextView.setText(userProfile.person == PersonType.STUDENT ? "Student" : "Teacher");
        TextView levelTextView = fragmentView.findViewById(R.id.levelText);
        levelTextView.setText(userProfile.person == PersonType.STUDENT ? userProfile.userLevel : "");


        return fragmentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main_fragment, menu);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //setHasOptionsMenu(true);
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

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == STUDENT_VIEW && resultCode == RESULT_OK && data != null) {
//
//        }
//    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating
     * .html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void makeNotification(String title, String content) {
        int m = r.nextInt(9999 - 1000) + 1000;
        Context cont = getContext();
        if(cont != null) {
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(getContext());
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder
                    (getContext(), "SKISCH").setContentTitle(title).setContentText(content).setSmallIcon(R.drawable.alert).setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true);
            notificationManager.notify(m, mBuilder.build());
        }
    }

    private class DBListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // ...
            adapter.clear();
            listUsers.clear();
            if(userProfile.person == PersonType.STUDENT){
                String teacherID = dataSnapshot.child(userProfile.userID).child("myTeacher").getValue(String.class);
                listPerson.add(new Person(dataSnapshot.child(teacherID).child("username").getValue(String.class)));
                userProfile.userLevel = dataSnapshot.child(userProfile.userID).child("userLevel").getValue(String.class);
                TextView levelTextView = fragmentView.findViewById(R.id.levelText);
                levelTextView.setText(userProfile.userLevel);
            }else{
                final ArrayList<String> newStuds = new ArrayList<String>();
                final ArrayList<String> newStudNames = new ArrayList<String>();
                for (DataSnapshot student : dataSnapshot.child(userProfile.userID).child("Students").getChildren()){
                    if(dataSnapshot.child(student.getValue(String.class)).hasChild("userID")) {
                        String username = dataSnapshot.child(student.getValue(String.class)).child("username").getValue(String.class);
                        String phone = dataSnapshot.child(student.getValue(String.class)).child("number").getValue(String.class);
                        String email = dataSnapshot.child(student.getValue(String.class)).child("email").getValue(String.class);
                        String photo = dataSnapshot.child(student.getValue(String.class)).child("photo").getValue(String.class);
                        String level = dataSnapshot.child(student.getValue(String.class)).child("userLevel").getValue(String.class);
                        String userID = dataSnapshot.child(student.getValue(String.class)).child("userID").getValue(String.class);

                        Profile studentProfile = new Profile(email, PersonType.STUDENT);
                        studentProfile.username = username;
                        studentProfile.phoneNumber = phone;
                        studentProfile.photoPath = photo;
                        studentProfile.userLevel = level;
                        studentProfile.userID = userID;
                        listUsers.add(studentProfile);
                        listPerson.add(new Person(username));

                        if (dataSnapshot.child(student.getValue(String.class)).child("current_training").hasChild("emergency")) {
                            boolean alarmWasReceived = dataSnapshot.child(student.getValue(String.class)).child("current_training").child("emergency").getValue(Boolean.class);
                            if (alarmWasReceived && needHelp.indexOf(student.getValue(String.class)) == -1) {
                                needHelp.add(student.getValue(String.class));
                                makeNotification("EMERGENCY", username + " needs help");
                            }
                        }
                        if (!dataSnapshot.child(userProfile.userID).child("alreadyNotifiedStudent").hasChild(student.getValue(String.class))) {
                            newStuds.add(student.getValue(String.class));
                            newStudNames.add(username);
                        }
                    }
                }
                if(newStuds.size()>0) {
                    user_db.runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull
                                                                        MutableData mutableData) {
                            for(String stud: newStuds) {
                                mutableData.child(userProfile.userID).child("alreadyNotifiedStudent").
                                        child(stud).setValue(stud);
                            }

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError
                                                       databaseError, boolean b,
                                               @Nullable DataSnapshot
                                                       dataSnapshot) {
                            for(String stud: newStudNames) {
                                makeNotification("NEW Student", stud);
                            }
                        }
                    });
                }
            }

            adapter.notifyDataSetChanged();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // ...
        }
    }
}
