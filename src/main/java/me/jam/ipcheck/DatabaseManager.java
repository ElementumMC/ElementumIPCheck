package me.jam.ipcheck;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseManager {
    private Connection conn;

    DatabaseManager(String path) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:" + path);

        exec("CREATE TABLE IF NOT EXISTS log (\n" +
            "   id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "   UUID TEXT,\n" +
            "   username TEXT,\n" +
            "   IP TEXT,\n" +
            "   seen DATETIME DEFAULT CURRENT_TIMESTAMP\n" +
            ")");
    }

    public ResultSet query(String sql, String... args) {
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            for(int i = 0; i < args.length; i++) {
                String arg = args[i];
                stmt.setString(i+1, arg);
            }
            return stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void exec(String sql, String... args) {
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            for(int i = 0; i < args.length; i++) {
                String arg = args[i];
                stmt.setString(i+1, arg);
            }
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
