package db;

import com.quorum.tessera.config.Config;
import com.quorum.tessera.test.UncheckedSQLException;
import config.ConfigDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import suite.ExecutionContext;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SetupDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetupDatabase.class);

    private ExecutionContext executionContext;

    public SetupDatabase(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public void setUp() throws Exception {

        URL ddl = executionContext.getDbType().getDdl();

        List<Connection> connections = getConnections();

        List<String> lines = Files.readAllLines(Paths.get(ddl.toURI()));

        for (Connection connection : connections) {

            try (Statement statement = connection.createStatement()) {
                for (String line : lines) {
                    LOGGER.trace("Create table SQL : {}", line);
                    statement.execute(line);
                }
            }
        }

        for (Connection connection : connections) {
            try {
                connection.close();
            } catch (SQLException ex) {
            }

        }

    }

    private List<Connection> getConnections() {
        return executionContext.getConfigs().stream()
            .map(ConfigDescriptor::getConfig)
            .map(Config::getJdbcConfig)
            .map(j -> {
                try {
                    LOGGER.info("{}", j.getUrl());
                    return DriverManager.getConnection(j.getUrl(), j.getUsername(), j.getPassword());
                } catch (SQLException ex) {
                    throw new UncheckedSQLException(ex);
                }

            })
            .collect(Collectors.toList());

    }

    public void dropAll() throws Exception {
        List<Connection> connections = getConnections();
        for (Connection connection : connections) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<String> tableNames = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(null, null, "%", null)) {

                while (rs.next()) {
                    tableNames.add(rs.getString(3));
                }
            }

            String dropStatement = "DROP TABLE %s";

            try (Statement statement = connection.createStatement()) {
                for (String tableName : tableNames) {
                    String line = String.format(dropStatement, tableName);
                    LOGGER.trace("Drop table SQL : {}", line);
                    try {
                        statement.execute(line);
                    } catch (SQLException ex) {
                    }
                }

            }


        }

        for (Connection connection : connections) {
            try {
                connection.close();
            } catch (SQLException ex) {
            }

        }

    }

}
