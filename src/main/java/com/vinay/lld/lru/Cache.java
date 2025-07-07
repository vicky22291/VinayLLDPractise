package com.vinay.lld.lru;

import javax.annotation.Nonnull;

public interface Cache<K, V> {
    void put(@Nonnull final K key, @Nonnull V value);
    V get(@Nonnull final K key);
    boolean delete(@Nonnull final K key);
}
