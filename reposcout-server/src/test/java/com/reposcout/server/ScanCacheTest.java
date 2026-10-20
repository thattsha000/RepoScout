package com.reposcout.server;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

class ScanCacheTest {

    @Test
    void evictsOldestWhenFull() {
        ScanCache cache = new ScanCache(3);
        List<Opportunity> a = new ArrayList<>();
        List<Opportunity> b = new ArrayList<>();
        List<Opportunity> c = new ArrayList<>();
        List<Opportunity> d = new ArrayList<>();

        cache.put("A", a);
        cache.put("B", b);
        cache.put("C", c);
        cache.put("D", d);      // full: evicts A, the oldest

        assertNull(cache.get("A"));
        assertSame(b, cache.get("B"));
        assertSame(c, cache.get("C"));
        assertSame(d, cache.get("D"));
    }

    @Test
    void recentlyReadEntryGetsSecondChance() {
        ScanCache cache = new ScanCache(3);
        List<Opportunity> a = new ArrayList<>();
        List<Opportunity> b = new ArrayList<>();
        List<Opportunity> c = new ArrayList<>();
        List<Opportunity> d = new ArrayList<>();
        List<Opportunity> e = new ArrayList<>();

        cache.put("A", a);
        cache.put("B", b);
        cache.put("C", c);
        cache.put("D", d);      // evicts A; hand moves to C

        cache.get("C");         // sets C's bit
        cache.put("E", e);      // C is skipped, B is evicted

        assertNull(cache.get("B"));
        assertSame(c, cache.get("C"));
        assertSame(d, cache.get("D"));
        assertSame(e, cache.get("E"));
    }

    @Test
    void putSameKeyUpdatesInsteadOfAddingSecondEntry() {
        ScanCache cache = new ScanCache(3);
        List<Opportunity> oldList = new ArrayList<>();
        List<Opportunity> newList = new ArrayList<>();
        List<Opportunity> b = new ArrayList<>();
        List<Opportunity> c = new ArrayList<>();

        cache.put("A", oldList);
        cache.put("A", newList);   // same key: should replace, not take a second slot
        cache.put("B", b);
        cache.put("C", c);         // if A took two slots, this would trigger an eviction

        assertSame(newList, cache.get("A"));
        assertSame(b, cache.get("B"));
        assertSame(c, cache.get("C"));
    }
}