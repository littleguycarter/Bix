package com.cf.bix.properties.types;

import com.cf.bix.properties.PropertyKey;

public interface WholeNumberKey extends PropertyKey<Integer> {
    @Override
    default Class<Integer> getType() {
        return Integer.class;
    }

    @Override
    default void verifyProperty(Integer property) {
        if (property < 0) {
            throw new IllegalArgumentException("Whole number property got " + property + "!");
        }
    }
}
