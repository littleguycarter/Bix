package com.cf.bix;

import com.cf.bix.properties.PropertyKey;
import com.cf.bix.util.SQLConsumer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public final class Bix {
    public static class Properties {
        private final Map<String, Object> properties;
        private boolean locked;

        private Properties() {
            this.properties = new HashMap<>(8);
            this.locked = false;
        }

        private void verifyUnlocked() {
            if (locked) {
                throw new IllegalStateException("Cannot modify locked properties!");
            }
        }

        private void lock() {
            this.locked = true;
        }

        public void property(String key, Object value) {
            verifyUnlocked();
            properties.put(key, value);
        }

        public Object property(String key) {
            return properties.get(key);
        }

        public <T> T property(String key, Class<T> type) {
            return type.cast(property(key));
        }

        public java.util.Properties toDefaultProperties() {
            java.util.Properties defaultProperties = new java.util.Properties();

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                defaultProperties.setProperty(entry.getKey(), entry.getValue().toString());
            }

            return defaultProperties;
        }
    }

    public static class ConnectionProvider {
        private final java.util.Properties properties;

        private ConnectionProvider(java.util.Properties properties) {
            this.properties = properties;
        }

        public Connection get() throws SQLException {
            return DriverManager.getConnection(properties.getProperty("jdbc_url"), properties);
        }
    }

    public static class Bootstrap {
        private final Properties properties;

        public static Bootstrap forDriver(String driverClassName) throws ClassNotFoundException {
            Class.forName(driverClassName);
            return new Bootstrap();
        }

        public Bootstrap() {
            this.properties = new Properties();
        }

        public <T> Bootstrap property(PropertyKey<T> key, T property) {
            properties.property(key.key(), property);
            return this;
        }

        public Bootstrap property(String key, Object property) {
            properties.property(key, property);
            return this;
        }

        public Bix complete() throws SQLException {
            properties.lock();
            return new Bix(properties);
        }
    }

    private final int maxPoolSize;
    private final ConnectionProvider provider;
    private final Properties properties;
    private final LinkedBlockingQueue<Connection> pooledConnections;

    private final ReentrantLock connectionCreationLock;
    private int createdConnectionCount;

    private Bix(Properties properties) {
        this.maxPoolSize = properties.property("pool_size", Integer.class);
        this.provider = new ConnectionProvider(properties.toDefaultProperties());
        this.properties = properties;
        this.pooledConnections = new LinkedBlockingQueue<>(maxPoolSize);
        this.connectionCreationLock = new ReentrantLock();
        this.createdConnectionCount = 0;
    }

    public void useConnection(SQLConsumer<Connection> consumer, Runnable onTimeOut, long timeout, TimeUnit unit) throws SQLException, InterruptedException, IOException {
        Connection connection = null;

        connectionCreationLock.lock();
        if (createdConnectionCount < maxPoolSize && pooledConnections.isEmpty()) {
            try {
                connection = provider.get();
                createdConnectionCount++;
            } finally {
                connectionCreationLock.unlock();
            }
        }

        if (connection == null) {
            connection = timeout == -1 ? pooledConnections.poll() : pooledConnections.poll(timeout, unit);

            if (connection == null) {
                onTimeOut.run();
                return;
            }

            if (connection.isClosed()) {
                connection = provider.get();
            }
        }

        try {
            consumer.accept(connection);
        } finally {
            pooledConnections.offer(connection);
        }
    }

    public void useConnection(SQLConsumer<Connection> consumer) throws SQLException, IOException, InterruptedException {
        useConnection(consumer, () -> {}, -1, null);
    }

    public Properties properties() {
        return properties;
    }

    public ConnectionProvider connectionProvider() {
        return provider;
    }
}
