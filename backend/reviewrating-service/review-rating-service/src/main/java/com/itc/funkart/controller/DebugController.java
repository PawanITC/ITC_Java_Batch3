package com.itc.funkart.controller;



import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@RestController
public class DebugController {

    private final DataSource dataSource;

    public DebugController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/db")
    public String dbInfo() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            String version = conn.getMetaData().getDatabaseProductVersion();
            return "Connected to: " + url + "\nVersion: " + version;
        }
    }

    @GetMapping("/db2")
    public String db2() throws Exception {
        return dataSource.getConnection().getMetaData().getURL();
    }

    }

