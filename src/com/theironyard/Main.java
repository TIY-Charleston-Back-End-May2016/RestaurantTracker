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




    static HashMap<String, User> users = new HashMap<>();

    public static void main(String[] args) throws SQLException {
        //Create the Connection
        // and execute a query to create a restaurants table that stores the restaurant name and other attributes.
        Server.createWebServer().start();
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
                        User user = users.get(username);
                        m.put("allRestaurants", selectAllRestaurants(conn));
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
                        throw new Exception("Name or pass not sent.");
                    }

                    User user = users.get(name);
                    if (user == null) {
                        user = new User(name, pass);
                        users.put(name, user);
                    } else if (!pass.equals(user.password)) {
                        throw new Exception("wrong password");
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
                        throw new Exception("Not Logged In");
                    }
                    String name = request.queryParams("name");
                    String location = request.queryParams("location");
                    int rating = Integer.valueOf(request.queryParams("rating"));
                    String comment = request.queryParams("comment");
                    if (name == null || location == null || comment == null) {
                        throw new Exception("Invalid Fields");
                    }

                    User user = users.get(username);
                    if (user == null) {
                        throw new Exception("User does not exist");
                    }
                    Restaurant r = new Restaurant(name, location, rating, comment);

                    //                    user.restaurants.add(r);

                    insertRestaurant(conn, name, location, rating, comment);

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

                    int id = Integer.valueOf(request.queryParams("id"));

                    deleteRestaurant(conn, id);
                    response.redirect("/");
                    return "";
                }
        );

        Spark.post(
                "/edit",
                (request, response) -> {
                    int id = Integer.valueOf(request.queryParams("id"));
                    String newName = request.queryParams("newName");
                    String newLocation = request.queryParams("newLocation");
                    int newRating = Integer.valueOf(request.queryParams("newRating"));
                    String newComment = request.queryParams("newComment");

                    deleteRestaurant(conn, id);
                    insertRestaurant(conn, newName, newLocation, newRating, newComment);

                    response.redirect("/");
                    return "";
                }
        );

        Spark.get(
                "/edit",
                (request, response) -> {
                    HashMap d = new HashMap();
                    return new ModelAndView(d, "shoppingList.html");
                },
                new MustacheTemplateEngine()
        );

    }


    public static void insertRestaurant(Connection conn, String name, String location, int rating, String comment) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO restaurants VALUES (Null, ?, ?, ?, ?)");
        stmt.setString(1, name);
        stmt.setString(2, location);
        stmt.setInt(3, rating);
        stmt.setString(4, comment);
        stmt.execute();
    }

    public static ArrayList<Restaurant> selectAllRestaurants (Connection conn) throws SQLException {
        ArrayList<Restaurant> allRestaurants = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM restaurants");
        ResultSet results = stmt.executeQuery();

        while (results.next()){
            int id = results.getInt("id");
            String name = results.getString("name");
            String location = results.getString("location");
            int rating = results.getInt("rating");
            String comment = results.getString("comment");
            Restaurant r = new Restaurant(id, name, location, rating, comment);
            allRestaurants.add(r);
        }
        return allRestaurants;
    }

    public static void deleteRestaurant (Connection conn, int id) throws SQLException {
        PreparedStatement stmt3 = conn.prepareStatement("DELETE FROM restaurants WHERE id = ?");
        stmt3.setInt(1, id);
        stmt3.execute();
    }

}
