package info.gabrielszabo.ublog.domain;

import java.util.Properties;

public class User {
    private String username;
    private final String name;
    private final String password;
    private final String status;

    public User(Properties props) {
        this.name = props.getProperty("name");
        this.password = props.getProperty("password");
        this.status = props.getProperty("status");
    }

    public User(String name, String password, String status) {
        this.name = name;
        this.password = password;
        this.status = status;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getStatus() {
        return status;
    }
}
