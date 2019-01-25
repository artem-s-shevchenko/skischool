package ch.epfl.esl.sportstracker;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;

public class AddLessonActivity extends Activity {

    public String teacherID;
    private Spinner spinnerStd;
    private ArrayList<String> studentListID;
    private String selectedStudent;
    private String selectedExercise;
    String dateHourP = null;
    ArrayList<String> studentsName;
    String nameLesson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lesson);

        Intent intent = getIntent();
        studentListID = intent.getStringArrayListExtra("studentListID");
        teacherID = studentListID.get(0);
        studentListID.remove(0);
        selectedStudent = studentListID.get(0);
        selectedExercise = getResources().getStringArray(R.array.exlist)[0];

        readDB();
        spinnersListenner();
        //finish();//for now
    }

    private void readDB() {
        studentsName = new ArrayList<>();
        DatabaseReference profiles = FirebaseDatabase.getInstance().getReference().child("Users");
        profiles.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (String studentId : studentListID){
                    studentsName.add(dataSnapshot.child(studentId).child("username").getValue(String.class));
                }
                addItemsOnSpinner();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void spinnersListenner() {
        Spinner spinnerStd =(Spinner) findViewById(R.id.spinnerstd);

        spinnerStd.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                        selectedStudent = studentListID.get(pos);
                    }
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

        Spinner spinnerEx = findViewById(R.id.spinnerex);

        spinnerEx.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                        Object item = parent.getItemAtPosition(pos);
                        selectedExercise = item.toString();
                    }
                    public void onNothingSelected(AdapterView<?> parent) {
                        //Object item = parent.getItemAtPosition(0);
                        //selectedExercise = item.toString();
                    }
                });
    }

    private void addItemsOnSpinner() {
        spinnerStd = (Spinner)findViewById(R.id.spinnerstd);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, studentsName);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStd.setAdapter(dataAdapter);
    }


    Calendar date;
    public void showDateTimePicker() {
        final Calendar currentDate = Calendar.getInstance();
        date = Calendar.getInstance();
        new DatePickerDialog(AddLessonActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
                date.set(year, monthOfYear, dayOfMonth);
                new TimePickerDialog(AddLessonActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        date.set(Calendar.MINUTE, minute);
                        TextView dateHour = findViewById(R.id.dateHour);
                        dateHourP = dayOfMonth + "/" + (monthOfYear+1) + "/" + year + " , " + hourOfDay + "h" + minute;
                        nameLesson = dayOfMonth + "" + monthOfYear + "" + year + "" + hourOfDay + "" + minute;
                        dateHour.setText(dateHourP);
                    }
                }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show();
            }
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }

    public void clickCancelCallback(View view) {
        finish();
    }

    public void clickSaveCallback(View view) {
        TextView duration = findViewById(R.id.editDuration);
        TextView place = findViewById(R.id.editPlace);
        String durationString = "";
        String placeString = "";

        int validInput = 0;

        if(isEmpty(duration.getText())){

        }else{
            validInput++;
            durationString = duration.getText().toString();
        }

        if(dateHourP == null){

        }else{
            validInput++;
        }

        if(isEmpty(place.getText())){

        }else{
            validInput++;
            placeString = place.getText().toString();
        }

        if(validInput == 3){
            final String pl = placeString;
            final String dur = durationString;
            DatabaseReference lessons = FirebaseDatabase.getInstance().getReference().child("Users").child(selectedStudent).child("Lessons");
            lessons.runTransaction(new Transaction.Handler() {
               @NonNull
               @Override
               public Transaction.Result doTransaction(@NonNull
                                                               MutableData mutableData) {
                   mutableData.child(nameLesson).child("place").setValue(pl);
                   mutableData.child(nameLesson).child("duration").setValue(dur);
                   mutableData.child(nameLesson).child("exercise").setValue(selectedExercise);
                   mutableData.child(nameLesson).child("dateHour").setValue(dateHourP);

                   return Transaction.success(mutableData);
               }

               @Override
               public void onComplete(@Nullable DatabaseError
                                              databaseError, boolean b,
                                      @Nullable DataSnapshot
                                              dataSnapshot) {
                   finish();
               }
           });

        } else{
            Toast.makeText(getApplicationContext(),
                    "Make sure you enter all fields correctly",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public boolean isEmpty(CharSequence text){
        return TextUtils.isEmpty(text);
    }

    public void clickDateCallback(View view) {
        showDateTimePicker();

    }
}
