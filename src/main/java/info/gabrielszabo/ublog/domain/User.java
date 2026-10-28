package info.gabrielszabo.ublog.domain;

import java.util.Properties;

public class User {
    private String username;
    private final String name;
    private final String password;
    private final String status;
    private final Boolean editor;

    public User(Properties props) {
        this.name = props.getProperty("name");
        this.password = props.getProperty("password");
        this.status = props.getProperty("status");
        try {
            this.editor = Boolean.valueOf(props.getProperty("editor", "false"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value for editor property", e);
        }
    }

    public User(String name, String password, String status) {
        this(name, password, status, Boolean.FALSE);
    }

    public User(String name, String password, String status, Boolean editor) {
        this.name = name;
        this.password = password;
        this.status = status;
        this.editor = editor;
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

    public Boolean getEditor() {
        return editor;
    }
}
