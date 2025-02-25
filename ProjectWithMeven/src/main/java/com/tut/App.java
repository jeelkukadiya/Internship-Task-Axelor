package com.tut;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args )
    {
        System.out.println( "project started!!" );


        Configuration cfg= new Configuration();
        cfg.configure("D:\\ProjectWithMeven\\src\\main\\java\\hibernate. cfg. xml");
        SessionFactory sf = cfg.buildSessionFactory();
        System.out.println(sf);
//        System.out.println(factory.close());
    }
}
