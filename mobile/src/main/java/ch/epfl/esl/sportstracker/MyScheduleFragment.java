package ch.epfl.esl.sportstracker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import static ch.epfl.esl.sportstracker.MyProfileFragment.USER_PROFILE;


public class MyScheduleFragment extends Fragment {

    private final String TAG = this.getClass().getSimpleName();

    private OnFragmentInteractionListener mListener;
    private ListView listView;
    private View fragmentView;
    private LessonAdapter adapter;
    private MyFirebaseRecordingListener mFirebaseRecordingListener;
    private DatabaseReference databaseRef;
    private String idUser;
    private Profile userProfile;
    private boolean showMenu = false;
    private ArrayList<String> rowLessonIds;
    private ArrayList<String> rowStudentIds;

    public MyScheduleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_add_lesson, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if(!showMenu) {
            menu.findItem(R.id.action_add).setVisible(false);
            menu.findItem(R.id.action_add).setEnabled(false);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_my_schedule, container, false);
        Intent intent = getActivity().getIntent();
        userProfile = (Profile) intent.getSerializableExtra(USER_PROFILE);
        if(userProfile.person == PersonType.TEACHER) {
            showMenu=true;
        }

        rowLessonIds = new ArrayList<String>();
        rowStudentIds = new ArrayList<String>();

        listView = fragmentView.findViewById(R.id.myScheduleList);
        adapter = new LessonAdapter(getActivity(), R.layout.row_schedule);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(!showMenu) {
                    Toast.makeText(getContext(), "Lesson: " + ((TextView) view.findViewById(R.id
                            .lessonName)).getText().toString() + " on " + ((TextView) view
                            .findViewById(R.id.lessonDateTime)).getText().toString(), Toast
                            .LENGTH_SHORT).show();
                }
                else {
                    final int pos = i;
                    AlertDialog.Builder adb=new AlertDialog.Builder(getActivity());
                    adb.setTitle("Delete?");
                    adb.setMessage("Are you sure you want to delete?");
                    adb.setNegativeButton("Cancel", null);
                    adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            databaseRef.child("Users").child(rowStudentIds.get(pos)).child("Lessons").child(rowLessonIds.get(pos)).removeValue();
                        }});
                    adb.show();
                }
            }
        });

        //idUser = getActivity().getIntent().getExtras().getString(MyProfileFragment.USER_ID);

        return fragmentView;
    }

    private void getListStudents() {

        DatabaseReference students = FirebaseDatabase.getInstance().getReference().child("Users");
        students.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<String> myStudents = new ArrayList<>();
                ArrayList<String> myStudentsID = new ArrayList<>();
                for(DataSnapshot students : dataSnapshot.child(userProfile.userID).child("Students").getChildren()){
                    String username = dataSnapshot.child(students.getValue(String.class)).child("username").getValue(String.class);
                    String userID = dataSnapshot.child(students.getValue(String.class)).child("userID").getValue(String.class);
                    myStudents.add(username);
                    myStudentsID.add(userID);
                }

                if(myStudents.isEmpty()){
                    Toast.makeText(getActivity().getApplicationContext(),  "You first need to have some students assigned", Toast.LENGTH_SHORT).show();
                }else {
                    Intent intent = new Intent(getActivity(), AddLessonActivity.class);
                    myStudentsID.add(0,userProfile.userID);
                    intent.putStringArrayListExtra("studentListID", myStudentsID);
                    startActivity(intent);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                getListStudents();
                break;
        }

        return super.onOptionsItemSelected(item);
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

    @Override
    public void onResume() {
        super.onResume();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseRecordingListener = new MyFirebaseRecordingListener();
        if(showMenu == false) {
            databaseRef.child("Users").child(userProfile.userID).child("Lessons").addValueEventListener
                    (mFirebaseRecordingListener);
        }else{
            databaseRef.child("Users").addValueEventListener
                    (mFirebaseRecordingListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(showMenu == false) {
            databaseRef.child("Users").child(userProfile.userID).child("Lessons").removeEventListener
                    (mFirebaseRecordingListener);
        }else{
            databaseRef.child("Users").removeEventListener
                    (mFirebaseRecordingListener);
        }

    }

    private class LessonAdapter extends ArrayAdapter<Lesson> {
        private int row_layout;

        LessonAdapter(FragmentActivity activity, int row_layout) {
            super(activity, row_layout);
            this.row_layout = row_layout;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            //Reference to the row View
            View row = convertView;

            if (row == null) {
                //Inflate it from layout
                row = LayoutInflater.from(getContext()).inflate(row_layout, parent, false);
            }

            SimpleDateFormat formatter = new SimpleDateFormat("EEE, MMM d, hh:mm", Locale
                    .getDefault());

            ((TextView) row.findViewById(R.id.lessonName)).setText(getItem(position)
                    .lessonName);
            ((TextView) row.findViewById(R.id.lessonDateTime)).setText(getItem(position).lessonDateTime);
            ((TextView) row.findViewById(R.id.rdv)).setText(getItem(position)
                    .lessonRdv);
            ((TextView) row.findViewById(R.id.duration)).setText(Integer.toString(getItem(position)
                    .lessonDuration));
            ((TextView) row.findViewById(R.id.studentName)).setText(getItem(position)
                    .lessonStudent);

            return row;
        }
    }

    private class MyFirebaseRecordingListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            adapter.clear();
            rowLessonIds.clear();
            rowStudentIds.clear();

            if(!showMenu){
                for (final DataSnapshot rec : dataSnapshot.getChildren()) {
                    final Lesson lesson = new Lesson();
                    lesson.lessonName = rec.child("exercise").getValue(String.class);
                    lesson.lessonDateTime = rec.child("dateHour").getValue(String.class);
                    lesson.lessonRdv = rec.child("place").getValue(String.class);
                    lesson.lessonDuration = Integer.parseInt(rec.child("duration").getValue(String.class));
                    lesson.lessonStudent = "";
                    adapter.add(lesson);
                }
            }
            else{
                for(DataSnapshot studentsId : dataSnapshot.child(userProfile.userID).child("Students").getChildren()){
                    for (final DataSnapshot rec : dataSnapshot.child(studentsId.getValue(String.class)).child("Lessons").getChildren()) {
                        final Lesson lesson = new Lesson();
                        lesson.lessonName = rec.child("exercise").getValue(String.class);
                        lesson.lessonDateTime = rec.child("dateHour").getValue(String.class);
                        lesson.lessonRdv = rec.child("place").getValue(String.class);
                        lesson.lessonDuration = Integer.parseInt(rec.child("duration").getValue(String.class));
                        lesson.lessonStudent = dataSnapshot.child(studentsId.getValue(String.class)).child("username").getValue(String.class);
                        adapter.add(lesson);
                        rowStudentIds.add(studentsId.getValue(String.class));
                        rowLessonIds.add(rec.getKey());
                    }
                }
            }



        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.v(TAG, databaseError.toString());
        }
    }
}
