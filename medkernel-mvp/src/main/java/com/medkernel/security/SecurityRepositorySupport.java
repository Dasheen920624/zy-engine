package com.medkernel.security;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
abstract class SecurityRepositorySupport {
    protected final EnginePersistenceProperties properties;
    protected final DataSource dataSource;
    protected SecurityRepositorySupport(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }
    protected Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
    protected Long nextId() {
        return Ids.next();
    }
}
