package com.cf.bix.properties.types;

import com.cf.bix.properties.PropertyKey;

public interface StringKey extends PropertyKey<String> {
    @Override
    default Class<String> getType() {
        return String.class;
    }

    @Override
    default void verifyProperty(String property) {
    }
}
