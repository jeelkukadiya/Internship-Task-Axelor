package com.example.controller;

import com.example.dao.UserDAO;
import com.example.model.User;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/users")
public class UserController {
    private UserDAO userDAO = new UserDAO();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void createUser(User user) {
        userDAO.saveUser(user);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }
}
