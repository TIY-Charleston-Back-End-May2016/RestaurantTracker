package com.theironyard;

/**
 * Created by zach on 6/7/16.
 */
public class Restaurant {
    int id;
    String name;
    String location;
    int rating;
    String comment;
    //boolean isAuthor;
    //String author;

    public Restaurant(String name, String location, int rating, String comment) {
        this.name = name;
        this.location = location;
        this.rating = rating;
        this.comment = comment;
        //isAuthor = false;
        //this.author = author;
    }

    public Restaurant(int id, String name, String location, int rating, String comment) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.rating = rating;
        this.comment = comment;
        //isAuthor = false;
        //this.author = author;
    }

    public Restaurant() {

    }
}
