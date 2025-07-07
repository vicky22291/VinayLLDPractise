package com.vinay.lld.lru.impls;

import com.vinay.lld.lru.Cache;
import com.vinay.lld.lru.internal.ds.DoublyLinkedListNode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LRUCache<K, V> implements Cache<K, V> {
    private final int capacity;
    private final ConcurrentHashMap<K, DoublyLinkedListNode> cache;
    private DoublyLinkedListNode head, tail;
    private final ReentrantReadWriteLock lock;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;

    public LRUCache(@Nonnull final int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock(true);
        this.readLock = this.lock.readLock();
        this.writeLock = this.lock.writeLock();
        this.head = new DoublyLinkedListNode();
        this.tail = new DoublyLinkedListNode();
        this.head.setNext(this.tail);
        this.tail.setPrev(this.head);
    }

    @Override
    public void put(@NotNull K key, @NotNull V value) {
        this.writeLock.lock();
        try {
            final DoublyLinkedListNode<K, V> existing = this.cache.get(key);
            if (Objects.nonNull(existing)) {
                existing.setValue(value);
                this.moveToHead(existing);
                return;
            }
            final DoublyLinkedListNode<K, V> newNode = new DoublyLinkedListNode<>();
            newNode.setValue(value);
            newNode.setKey(key);
            if (this.cache.size() >= this.capacity) {
                this.evict();
            }

            this.cache.put(key, newNode);
            addToHead(newNode);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public V get(@NotNull K key) {
        this.readLock.lock();
        try {
            DoublyLinkedListNode<K, V> node = this.cache.get(key);
            if (Objects.isNull(node)) return null;

            this.readLock.unlock();
            this.writeLock.lock();
            try {
                node = this.cache.get(key);

                if (Objects.isNull(node)) return null;

                moveToHead(node);
                return node.getValue();
            } finally {
                this.readLock.lock();
                this.writeLock.unlock();
            }
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public boolean delete(@Nonnull final K key) {
        this.writeLock.lock();
        try {
            final DoublyLinkedListNode<K, V> node = this.cache.remove(key);
            if (Objects.isNull(node)) return false;

            removeNode(node);
            return true;
        } finally {
            this.writeLock.unlock();
        }
    }

    private void evict() {
        final DoublyLinkedListNode<K, V> last = this.tail.getPrev();
        this.cache.remove(last.getKey());
        this.removeNode(last);
    }

    private void moveToHead(@Nonnull final DoublyLinkedListNode<K, V> node) {
        this.removeNode(node);
        this.addToHead(node);
    }

    private void addToHead(@Nonnull final DoublyLinkedListNode<K, V> node) {
        node.setPrev(this.head);
        node.setNext(this.head.getNext());
        if (Objects.nonNull(this.head.getNext())) {
            ((DoublyLinkedListNode) this.head.getNext()).setPrev(node);
        }
        this.head.setNext(node);
    }

    private void removeNode(@Nonnull final DoublyLinkedListNode<K, V> node) {
        node.getPrev().setNext(node.getNext());
        ((DoublyLinkedListNode) node.getNext()).setPrev(node.getPrev());
    }
}
