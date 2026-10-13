package com.irms.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class DatabaseLifecycle implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Database.close();
    }
}
