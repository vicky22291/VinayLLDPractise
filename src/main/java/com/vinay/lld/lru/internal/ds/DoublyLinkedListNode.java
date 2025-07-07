package com.vinay.lld.lru.internal.ds;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DoublyLinkedListNode<K, V> extends LinkedListNode<K, V> {
    private DoublyLinkedListNode<K, V> prev;
}
