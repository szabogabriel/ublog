package info.gabrielszabo.ublog.persistence;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import info.gabrielszabo.ublog.config.Config;

public class H2Database implements Closeable, Database {
        private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS LOG_ENTRIES (" +
            "ID BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "TS TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "CONTENT VARCHAR(4000) NOT NULL" +
            ")";

    private static final String INSERT_SQL = "INSERT INTO LOG_ENTRIES (CONTENT) VALUES (?)";

    private final Connection conn;

    public H2Database() {
        String filePath = Config.DB_FILE_PATH.value();
        String jdbcUrl = buildJdbcUrl(Objects.requireNonNull(filePath));
        System.out.println("Creating H2Database with URL: " + jdbcUrl);
        try {
            loadH2JdbcDriver();
            this.conn = DriverManager.getConnection(jdbcUrl, Config.DB_USERNAME.value(), Config.DB_PASSWORD.value());
            initSchema();

            startH2WebConsole();
        } catch (Exception e) {
            throw new RuntimeException("Failed to open H2 database", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    private static String buildJdbcUrl(String filePath) {
        return "jdbc:h2:file:" + filePath + ";DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
    }

    private void loadH2JdbcDriver() {
        // H2 driver auto-registers since JDBC 4, but explicit load doesn't hurt:
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 JDBC Driver not found", e);
        }
    }

    private void initSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(CREATE_TABLE_SQL);
        }
    }

    private void startH2WebConsole() throws SQLException {
        org.h2.tools.Server web = org.h2.tools.Server
                .createWebServer("-web", "-webPort", Config.H2_WEB_PORT.value(), "-webDaemon").start();

        System.out.println("H2 Web Console started and listening on port " + Config.H2_WEB_PORT.value());

        Runtime.getRuntime().addShutdownHook(new Thread(web::stop));
    }

    @Override
    public long log(String content) {
        Objects.requireNonNull(content, "content");
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, content);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new SQLException("No generated key returned for LOG_ENTRIES insert");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert log entry", e);
        }
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed())
                conn.close();
        } catch (SQLException ignored) {
        }
    }

}
