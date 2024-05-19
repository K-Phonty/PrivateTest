package org.god.ibatis.core.dataSource.pool;

public class DataSourceConfig {

    private String driver = "com.mysql.cj.jdbc.Driver";
    private String url = "jdbc:mysql://localhost:3306/powernode";
    private String username = "root";
    private String password = "3333";

    public DataSourceConfig getInstance(){
        return this;
    }

    public String getDriver() {
        return driver;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public DataSourceConfig() {
    }
}
