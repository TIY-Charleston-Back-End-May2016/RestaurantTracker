package com.theironyard;

import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static void insertRestaurant(Connection conn,String name,String location, int rating,String comment, int userID) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO restaurants VALUES(NULL,?,?,?,?,?)");
        stmt.setString(1, name);
        stmt.setString(2,location);
        stmt.setInt(3,rating);
        stmt.setString(4,comment);
        stmt.setInt(5,userID);
        stmt.execute();

    }

    public static void updaterestaurant(Connection conn, int id, String name, String location, int rating, String comment) throws SQLException {

        Restaurant r1 = new Restaurant(id,name,location,rating,comment);
        PreparedStatement stmt3 = conn.prepareStatement("UPDATE restaurants SET name=?, location=?,rating=?, comment=? WHERE id= ?");
        stmt3.setString(1,name);
        stmt3.setString(2,name);
        stmt3.setInt(3,rating);
        stmt3.setString(4,comment);
        stmt3.setString(5,String.valueOf(id));
        stmt3.execute();


    }

    public static void deleteRestaurant(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM restaurants WHERE id=?");
        stmt.setInt(1, id);
        stmt.execute();
    }


    // this method iterates through the database and returns the rest. info, adds it to a restaurant object and adds it to the array list.
    public static ArrayList<Restaurant> selectRestaurants(Connection conn, int userID) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM restaurants INNER JOIN users ON restaurants.user_id = users.id WHERE users.id=?");
        stmt.setInt(1,userID);
        ResultSet results = stmt.executeQuery();
        ArrayList<Restaurant> restaurantList = new ArrayList<>();
        while(results.next()){
            int id =results.getInt("id");
            String name = results.getString("name");
            String location = results.getString("location");
            int rating = results.getInt("rating");
            String comment = results.getString("comment");
            Restaurant r1 = new Restaurant(id,name,location,rating,comment);
            restaurantList.add(r1);
        }
        return restaurantList;
    }

    //Creating user in database so we don't need to rely on the HashMap anymore
    static void insertUser(Connection conn, String name, String password) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (null,?,?)");
        stmt.setString(1, name);
        stmt.setString(2, password);
        stmt.execute();

    }

    static User selectUser(Connection conn, String name) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");   //Searching by name in the user table
        stmt.setString(1, name);
        ResultSet results = stmt.executeQuery();
        if(results.next()){                                 //Only want one result so if is more appropriate than a while loop
            int id = results.getInt("id");
            String password = results.getString("password");
            User user = new User(id,name,password);
            return user;
        }

        return null;
    }




    public static void main(String[] args) throws SQLException {

        Server.createWebServer().start();

        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Statement stmt = conn.createStatement();

        stmt.execute("CREATE TABLE IF NOT EXISTS restaurants(id IDENTITY, name VARCHAR, location VARCHAR, rating INT, comment VARCHAR, user_id INT )");
        stmt.execute("CREATE TABLE IF NOT EXISTS users(id IDENTITY, name VARCHAR, password VARCHAR)");

        Spark.init();
        Spark.get(
                "/",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");

                    HashMap m = new HashMap();
                    if (username == null) {
                        return new ModelAndView(m, "login.html");
                    }
                    else {
                        User user = selectUser(conn,username);
                        m.put("restaurants", selectRestaurants(conn, user.id));  //Added select SQL method to link mustache to database
                        return new ModelAndView(m, "home.html");
                    }
                },
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/login",
                (request, response) -> {
                    String name = request.queryParams("username");
                    String pass = request.queryParams("password");
                    if (name == null || pass == null) {
                        throw new Exception("Name or pass not sent");
                    }

                    User user = selectUser(conn, name);
                    if (user == null) {
                        insertUser(conn, name, pass);
                    }
                    else if (!pass.equals(user.password)) {
                        throw new Exception("Wrong password");
                    }

                    Session session = request.session();
                    session.attribute("username", name);

                    response.redirect("/");
                    return "";
                }
        );
        Spark.post(
                "/create-restaurant",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    if (username == null) {
                        throw new Exception("Not logged in");
                    }

                    String name = request.queryParams("name");
                    String location = request.queryParams("location");
                    int rating = Integer.valueOf(request.queryParams("rating"));
                    String comment = request.queryParams("comment");

                    if (name == null || location == null || comment == null) {
                        throw new Exception("Invalid form fields");
                    }

                    User user = selectUser(conn, username);
                    if (user == null) {
                        throw new Exception("User does not exist");
                    }

                    insertRestaurant(conn, name,location,rating,comment, user.id);
//                    Restaurant r = new Restaurant( name, location, rating, comment);
//                    user.restaurants.add(r);

                    response.redirect("/");
                    return "";
                }
        );

        Spark.get(
                "/edit-restaurant",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    if (username == null) {
                        response.redirect("/");
                    }
                    String id = request.queryParams("id");
                    int newId = Integer.valueOf(id);

                    PreparedStatement stmt2 = conn.prepareStatement("SELECT * FROM restaurants WHERE id = ?");
                    stmt2.setInt(1, Integer.parseInt(id));
                    stmt2.execute();

                    ResultSet results = stmt2.executeQuery();

                    Restaurant r1 = new Restaurant();
                    while(results.next()){
                        String name = results.getString("name");
                        String location = results.getString("location");
                        int rating = results.getInt("rating");
                        String comment = results.getString("comment");
                        r1 = new Restaurant(newId,name,location,rating,comment);

                    }

                    HashMap m = new HashMap();
                    m.put("id", id);
                    m.put("name", r1.name);
                    m.put("location", r1.location);
                    m.put("rating", r1.rating);
                    m.put("comment", r1.comment);


                    return new ModelAndView(m, "edit-restaurant.html");
                },
                new MustacheTemplateEngine()
        );

        Spark.post(
                "/edit-restaurant",
                (request, response) -> {
                    Session session =request.session();
                    String username = session.attribute("username");

                    if (username==null) {
                        response.redirect("/");
                    }

                    int id = Integer.valueOf(request.queryParams("id"));  //retrieve user input
                    String name = request.queryParams("name");
                    String location = request.queryParams("location");
                    int rating = Integer.valueOf(request.queryParams("rating"));
                    String comment = request.queryParams("comment");

                    updaterestaurant(conn, id, name,location, rating, comment);
                    response.redirect("/");
                    return"";

                }
        );

        Spark.post(
                "/logout",
                (request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                }
        );
        Spark.post(
                "/delete-restaurant",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    if (username == null) {
                        throw new Exception("Not logged in");
                    }
                    int id = Integer.valueOf(request.queryParams("id"));

                    deleteRestaurant(conn,id);
                    response.redirect("/");
                    return "";
                }
        );
    }
}
