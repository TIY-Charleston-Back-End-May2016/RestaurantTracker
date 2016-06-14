package com.theironyard;

import org.h2.tools.ChangeFileEncryption;
import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    static HashMap<String, User> users = new HashMap<>();


    public static void insertRestaurant(Connection conn, String name, String location, int rating, String comment) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO restaurants VALUES(NULL, ?, ?, ?, ?)");
        stmt.setString(1, name);
        stmt.setString(2, location);
        stmt.setInt(3, rating);
        stmt.setString(4, comment);
        stmt.execute();
    }
    public static void deleteRestaurant(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM restaurants WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }
    public static ArrayList<Restaurant> selectRestaurants(Connection conn) throws SQLException {
        ArrayList<Restaurant> restaurants = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM restaurants");
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
            int id = results.getInt("id");
            String name = results.getString("name");
            String location = results.getString("location");
            int rating = results.getInt("rating");
            String comment = results.getString("comment");
            restaurants.add(new Restaurant(id, name, location, rating, comment));
        }
        return restaurants;
    }

    public static void main(String[] args) throws SQLException {
        Server.createWebServer().start();
        Spark.staticFileLocation("public");
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS restaurants (id IDENTITY, name VARCHAR, location VARCHAR, rating INT, comment VARCHAR)");


        Spark.init();
        Spark.get(
                "/",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");

                    HashMap m = new HashMap();
                    if (username == null) {
                        return new ModelAndView(m, "login.html");
                    } else {

                        m.put("name", username);
                        m.put("restaurants", selectRestaurants(conn));
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

                    User user = users.get(name);
                    if (user == null) {
                        user = new User(name, pass);
                        users.put(name, user);
                    } else if (!pass.equals(user.password)) {
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

                    User user = users.get(username);
                    if (user == null) {
                        throw new Exception("User does not exist");
                    }

                    //Restaurant r = new Restaurant(name, location, rating, comment);
                    insertRestaurant(conn, name, location, rating, comment);
                    //user.restaurants.add(r);

                    response.redirect("/");
                    return "";
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
                    deleteRestaurant(conn, id);

                    response.redirect("/");
                    return "";
                }
        );
        Spark.get(
                "/edit-entry",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");

                    User user = users.get(username);
                    if (username == null) {
                        throw new Exception("you must log in first");
                    }

                    int id = (Integer.valueOf(request.queryParams("id")));
                    HashMap map = new HashMap();
                    Restaurant restaurant = selectRestaurant(conn, id);
                    map.put("restaurant", restaurant);


                    return new ModelAndView(map, "update-restaurant.html");
                },
                new MustacheTemplateEngine()

        );
        Spark.post(
                "/update-entry",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    User user = users.get(username);
                    if (username == null) {
                        throw new Exception("you must log in first");
                    }
                    int id = Integer.valueOf(request.queryParams("id"));
                    String name = request.queryParams("name");
                    String location = request.queryParams("location");
                    int rating = Integer.valueOf(request.queryParams("rating"));
                    String comment = request.queryParams("comment");
                    updateRestaurant(conn, name, location, rating, comment, id);

                    response.redirect("/");
                    return "";
                }
        );
    }
//        Spark.get(
//                "/search-restaurants",
//                (request, response) -> {
//                    ArrayList<Restaurant> searchList = new ArrayList<>();
//                    searchList.add
//                    searchList.get()
//                    HashMap map = new HashMap();
//
//
//                        map.put("search", );
//
//                    return new ModelAndView(map, "searchList.html");
//
//                }
//        );

    public static Restaurant selectRestaurant(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM restaurants WHERE id = ?");
        stmt.setInt(1, id);
        ResultSet results = stmt.executeQuery();

        if (results.next()) {
            String name = results.getString("name");
            String location = results.getString("location");
            int rating = results.getInt("rating");
            String comment = results.getString("comment");
            return new Restaurant(id, name, location, rating, comment);
        }
        return null;
    }
    public static Restaurant updateRestaurant(Connection conn, String name, String location, int rating, String comment, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE restaurants SET name = ?, location = ?, rating = ?, comment =? WHERE id = ?");
        stmt.setString(1, name);
        stmt.setString(2, location);
        stmt.setInt(3, rating);
        stmt.setString(4, comment);
        stmt.setInt(5, id);
        stmt.execute();
        return new Restaurant(id, name, location, rating, comment);
    }
}
