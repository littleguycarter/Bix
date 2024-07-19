package com.cf.bix.util;

import java.io.IOException;

public interface SQLConsumer<T> {
    void accept(T obj) throws IOException;
}
