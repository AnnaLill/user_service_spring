package org.example;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.example.config.AppConfig;
import org.example.config.JpaConfig;
import org.example.config.WebConfig;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.File;

public class UserServiceApplication {

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getProperty("server.port", "8080"));

        Tomcat tomcat = new Tomcat();
        File baseDir = new File("target/tomcat");
        baseDir.mkdirs();
        tomcat.setBaseDir(baseDir.getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector();

        Context context = tomcat.addContext("", new File(".").getAbsolutePath());

        AnnotationConfigWebApplicationContext webContext = new AnnotationConfigWebApplicationContext();
        webContext.register(AppConfig.class, JpaConfig.class, WebConfig.class);
        webContext.setServletContext(context.getServletContext());
        webContext.refresh();

        DispatcherServlet dispatcherServlet = new DispatcherServlet(webContext);
        Wrapper wrapper = tomcat.addServlet(context, "dispatcher", dispatcherServlet);
        wrapper.setLoadOnStartup(1);
        context.addServletMappingDecoded("/*", "dispatcher");

        try {
            tomcat.start();
            System.out.println("Server started: http://localhost:" + port + "/users");
            tomcat.getServer().await();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start embedded Tomcat", e);
        }
    }
}

