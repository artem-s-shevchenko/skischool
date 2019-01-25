package ch.epfl.esl.sportstracker;

import java.util.Vector;

public class Teacher extends Person {
    Vector<Student> students;

    Teacher(){
        this.name = "Mr.Peach";
    }

    public String getStudentsNames() {
        if(students!=null)
            return students.toString();
        else return "no students assigned yet";
    }

}
