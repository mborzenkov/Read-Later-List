package com.example.mborzenkov.readlaterlist;

// TODO: proper javadoc
public class ReadLaterItem {

    // TODO: spec
    // TODO: javadocs
    // TODO: rep invariants
    // TODO: test cases /w testing strategy
    // TODO: abstract function
    // TODO: checkRep
    // TODO: equals, hashcode, tostring, clone
    // TODO: safety from rep exposure
    // TODO: thread safety

    public final String label;
    public final String description;
    public final int color;

    public ReadLaterItem(String label, String description, int color) {
        this.label = label;
        this.description = description;
        this.color = color;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
