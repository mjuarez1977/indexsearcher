package com.mjuarez.pandora.utils;

import java.util.BitSet;

import static com.mjuarez.pandora.utils.Utils.crc32MultiHash;

/**
 * Basic implementation of a bloom filter for caching results
 * Uses modified CRC32 multiple-hashing to get the job done.
 * For a production environment, I'd probably use an FNV or Murmur variant
 */
public class BloomFilter <K> {
    private final BitSet bitSet;
    private final int hashes;
    private final int sizeBits;

    public BloomFilter(int sizeBits, int hashes) {
        this.bitSet = new BitSet(sizeBits);
        this.sizeBits = sizeBits;
        this.hashes = hashes;
        if (hashes < 1) {
            throw new IllegalArgumentException("Number of hashes has to be positive");
        }
    }

    public void add(K key) {
        for (int i = 0; i < hashes; i++) {
            bitSet.set(getBit(crc32MultiHash(key.toString(), i)));
        }
    }

    public boolean maybeContains(K key) {
        for (int i = 0; i < hashes; i++) {
            if (!bitSet.get(getBit(crc32MultiHash(key.toString(), i)))) {
                return false;
            }
        }
        return true;
    }

    private int getBit(long hashValue) {
        return (int) (hashValue % sizeBits);
    }
}
