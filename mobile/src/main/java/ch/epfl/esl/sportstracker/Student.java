package ch.epfl.esl.sportstracker;

public class Student extends Person {

    String level;
    Teacher teacher;

    Student(){
        this.level = "Level: 0 (Beginner)";
        this.teacher = new Teacher();
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
