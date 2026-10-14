package info.gabrielszabo.ublog.domain;

import java.util.Properties;

public class User {
    private final String name;
    private final String password;
    private final String security;
    private final String status;

    public User(Properties props) {
        this.name = props.getProperty("name");
        this.password = props.getProperty("password");
        this.security = props.getProperty("security");
        this.status = props.getProperty("status");
    }

    public User(String name, String password, String security, String status) {
        this.name = name;
        this.password = password;
        this.security = security;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getSecurity() {
        return security;
    }

    public String getStatus() {
        return status;
    }
}
