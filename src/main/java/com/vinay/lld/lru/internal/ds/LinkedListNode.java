package com.vinay.lld.lru.internal.ds;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkedListNode<K, V> {
    private LinkedListNode next;
    private K key;
    private V value;
}
