package ch.epfl.esl.sportstracker;

import java.io.Serializable;

public class Person  implements Serializable {
    String name;
    Person(){
        this.name = "unnamed";
    }

    Person(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
