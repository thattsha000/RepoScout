package com.reposcout.server;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;

@Service
public class RateLimiter {
    private final ConcurrentHashMap<String, Connection> all_connections;
    private final Lock lock;

    public RateLimiter() {
        all_connections = new ConcurrentHashMap<>();
        lock = new ReentrantLock();
    }

    public boolean checkIP(String ip) {
        Connection newConnection = null;
        lock.lock();
        //grab the lock for the check and adding to hashmap(course)
        try {
            newConnection = all_connections.get(ip);
            if (newConnection == null) {
                newConnection = new Connection();
                all_connections.put(ip, newConnection);
            }
         } 
        finally {
            lock.unlock();
        }
        //fine grained lock to add to the list 
        return newConnection.addToList(); 
    }

    public static class Connection {
        private Queue<Long> time_connected;
        private static final long WINDOW_SIZE = 3600000;
        private static final int MAX_REQUESTS = 10;
        public Connection() {
            time_connected = new ArrayDeque<>();
        }
        public synchronized boolean addToList() {
            long curr = System.currentTimeMillis();
            if (time_connected.size() >= MAX_REQUESTS) {
                while (!time_connected.isEmpty()) {
                    if (time_connected.peek() + WINDOW_SIZE < curr) {
                        time_connected.poll();
                    }
                    else {
                        break;
                    }
                }
            }
            if (time_connected.size() >= MAX_REQUESTS) {
                return false;
            }
            else {
                time_connected.add(curr);
                return true;
            }
        }
    }
}
