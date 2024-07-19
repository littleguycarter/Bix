package com.cf.bix.properties;

import com.cf.bix.properties.types.StringKey;
import com.cf.bix.properties.types.WholeNumberKey;

public interface PropertyKey<T> {
    PropertyKey<String> USERNAME = (StringKey) () -> "user";
    PropertyKey<String> PASSWORD = (StringKey) () -> "password";
    PropertyKey<String> JDBC_URL = (StringKey) () -> "jdbc_url";
    PropertyKey<Integer> POOL_SIZE = (WholeNumberKey) () -> "pool_size";

    String key();

    Class<T> getType();

    void verifyProperty(T property);
}
