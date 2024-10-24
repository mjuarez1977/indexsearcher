package com.mjuarez.pandora.utils;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Basic implementation of an LRU cache for fronting heavily requested queries
 * Made thread-safe by using ReentrantReadWriteLock.
 */
public class LRUCache<K, V> {
    private final Map<K, V> cache;
    private final Deque<K> list;
    private final int maxSize;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public LRUCache(int maxSize) {
        if (maxSize < 10) {
            maxSize = 1_000;
        }
        this.cache = new ConcurrentHashMap<K, V>(maxSize);
        this.list = new ConcurrentLinkedDeque<K>();
        this.maxSize = maxSize;
    }

    public V get(K key) {
        V result = null;
        try {
            lock.readLock().lock();
            result = cache.get(key);
        } catch (Exception e) {
            Utils.log("Exception while trying to read cached value", e);
        } finally {
            lock.readLock().unlock();
        }

        if (result != null) {
            try {
                lock.writeLock().lock();
                // Send it to the head of the list, because it was just retrieved
                list.remove(key);
                list.add(key);
            } catch (Exception e) {
                Utils.log("Exception while trying to read cached value", e);
            } finally {
                lock.writeLock().unlock();
            }
        }
        return result;
    }

    public boolean put(K key, V value) {
        boolean result = false;
        lock.writeLock().lock();

        try {
            cache.put(key, value);
            list.add(key);
            while (cache.size() > maxSize) {
                K last = list.removeFirst();
                cache.remove(last);
            }
            result = true;
        } catch (Exception e) {
            Utils.log("Exception while trying to add cached value", e);
        } finally {
            lock.writeLock().unlock();
        }
        return result;
    }
}
