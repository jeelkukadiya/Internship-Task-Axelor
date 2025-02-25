package com.example.dao;

import com.example.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.List;

public class UserDAO {
    public void saveUser(User user) {
        Transaction transaction = null;
        
        Configuration cfg = new Configuration();
        cfg.configure("hibernate.cfg.xml"); // Read configuration file
        SessionFactory factory = cfg.buildSessionFactory();
        
        
        try (Session session = factory.openSession();) {
            transaction = session.beginTransaction();
            session.save(user);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    public List<User> getAllUsers() {
    	
    	  Configuration cfg = new Configuration();
          cfg.configure("hibernate.cfg.xml"); // Read configuration file
          SessionFactory factory = cfg.buildSessionFactory();
          
        try (Session session = factory.openSession()) {
            return session.createQuery("from User", User.class).list();
        }
    }
}
