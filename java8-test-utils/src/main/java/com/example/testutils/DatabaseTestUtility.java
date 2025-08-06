package com.example.testutils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseTestUtility {
    private Connection connection;
    private String databaseUrl;
    private String username;
    private String password;
    
    public DatabaseTestUtility() {
        this("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", "");
    }
    
    public DatabaseTestUtility(String databaseUrl, String username, String password) {
        this.databaseUrl = databaseUrl;
        this.username = username;
        this.password = password;
    }
    
    public void setup() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 driver not found", e);
        }
        
        connection = DriverManager.getConnection(databaseUrl, username, password);
        connection.setAutoCommit(true);
    }
    
    public void setupFromSqlFile(String sqlFilePath) throws SQLException, IOException {
        setup();
        executeSqlFile(sqlFilePath);
    }
    
    public void executeSqlFile(String sqlFilePath) throws SQLException, IOException {
        if (connection == null) {
            throw new SQLException("Database connection not established. Call setup() first.");
        }
        
        StringBuilder sqlContent = new StringBuilder();
        // Try to load as resource first, then as file
        java.io.InputStream inputStream = getClass().getClassLoader().getResourceAsStream(sqlFilePath);
        if (inputStream != null) {
            // Load from resource
            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("--")) {
                        sqlContent.append(line).append(" ");
                    }
                }
            }
        } else {
            // If resource not found, try as file path
            try (BufferedReader reader = new BufferedReader(new FileReader(sqlFilePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("--")) {
                        sqlContent.append(line).append(" ");
                    }
                }
            }
        }
        
        String[] statements = sqlContent.toString().split(";");
        try (Statement stmt = connection.createStatement()) {
            for (String sql : statements) {
                sql = sql.trim();
                if (!sql.isEmpty()) {
                    stmt.execute(sql);
                }
            }
        }
    }
    
    public int executeUpdate(String sql, Object... parameters) throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection not established. Call setup() first.");
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }
            return stmt.executeUpdate();
        }
    }
    
    public List<Map<String, Object>> executeQuery(String sql, Object... parameters) throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection not established. Call setup() first.");
        }
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                int columnCount = rs.getMetaData().getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rs.getMetaData().getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
        }
        
        return results;
    }
    
    public boolean recordExists(String tableName, String whereClause, Object... parameters) throws SQLException {
        String sql = "SELECT COUNT(*) as cnt FROM " + tableName;
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        
        List<Map<String, Object>> results = executeQuery(sql, parameters);
        if (!results.isEmpty()) {
            Object count = results.get(0).get("CNT");
            if (count == null) {
                count = results.get(0).get("cnt");
            }
            return count != null && ((Number) count).intValue() > 0;
        }
        return false;
    }
    
    public void insertRecord(String tableName, Map<String, Object> data) throws SQLException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder values = new StringBuilder("VALUES (");
        
        String[] columns = data.keySet().toArray(new String[0]);
        Object[] parameters = new Object[columns.length];
        
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sql.append(", ");
                values.append(", ");
            }
            sql.append(columns[i]);
            values.append("?");
            parameters[i] = data.get(columns[i]);
        }
        
        sql.append(") ").append(values).append(")");
        
        executeUpdate(sql.toString(), parameters);
    }
    
    public void updateRecord(String tableName, Map<String, Object> data, String whereClause, Object... whereParameters) throws SQLException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        
        String[] columns = data.keySet().toArray(new String[0]);
        Object[] allParameters = new Object[columns.length + whereParameters.length];
        
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(columns[i]).append(" = ?");
            allParameters[i] = data.get(columns[i]);
        }
        
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
            System.arraycopy(whereParameters, 0, allParameters, columns.length, whereParameters.length);
        }
        
        executeUpdate(sql.toString(), allParameters);
    }
    
    public void deleteRecord(String tableName, String whereClause, Object... parameters) throws SQLException {
        String sql = "DELETE FROM " + tableName;
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        
        executeUpdate(sql, parameters);
    }
    
    public void clearTable(String tableName) throws SQLException {
        executeUpdate("DELETE FROM " + tableName);
    }
    
    public void teardown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
            connection = null;
        }
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}