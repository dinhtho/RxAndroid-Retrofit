package com.demo.retrofitrxjava.model;

import java.util.List;

/**
 * Created by PCPV on 03/08/2018.
 */

public class AndroidsAndPerson {
    private List<Android> androids;
    private Person person;

    public AndroidsAndPerson(List<Android> androids, Person person) {
        this.androids = androids;
        this.person = person;
    }

    public List<Android> getAndroids() {
        return androids;
    }

    public void setAndroids(List<Android> androids) {
        this.androids = androids;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
