package com.reposcout.server;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;

@Service 
public class ScanCache {
    private final Lock lock;
    private final HashMap<String, Node> cache;
    private Node currPointer;
    private final int MAX_SIZE;
    
    public ScanCache(int MAX_SIZE) 
    {
        lock = new ReentrantLock();
        cache = new HashMap<>();
        currPointer = null;
        this.MAX_SIZE = MAX_SIZE;
    }
    public List<Opportunity> get(String repoKey) {
        lock.lock();
        try {
            Node resNode = cache.get(repoKey);
        if (resNode != null) {
            resNode.referenced = true;
            return resNode.opporList;
        }
        else {
            return null;
        }
        } finally {
            lock.unlock();
        }      
    }
    public void put(String key, List<Opportunity> oppurList) {
        lock.lock();
        try {
            Node resNode = cache.get(key);
            if (resNode != null) {
                resNode.referenced = true;
                resNode.opporList = oppurList;
            }
            else if (cache.size() < MAX_SIZE) {
                //add to the cache
                //create a new node for the circular linked list
                //update this to the currptr for the circular linked lsit 
                Node newNode = new Node(key, oppurList);
                cache.put(key, newNode);
                newNode.referenced = true;
                //first element
                if (currPointer == null) {
                    currPointer = newNode;
                    currPointer.next = newNode;
                }
                else {
                    Node next = currPointer.next;
                    currPointer.next = newNode;
                    newNode.next = next;
                    //cache stays put at the old node
                }
            }
            else  {
                //evict using clock algorithman
                Node curr = currPointer;
                while (curr.referenced != false) {
                    curr.referenced = false;
                    curr = curr.next;
                }
                //we reach a non-referenced so we can evict this node
                //update this node with the new info
                String oldKey = curr.key;
                curr.key = key;
                curr.opporList = oppurList;
                //add this new key in the spot of the orginal in cache
                cache.remove(oldKey);
                cache.put(curr.key, curr);

                //change the refreecned bit and move it othe next element
                curr.referenced = true;
                currPointer = curr.next;   
            }
        } 
        finally {
            lock.unlock();
        }
    }
    public static class Node {
         String key; 
         boolean referenced;
         List<Opportunity> opporList;
         Node next;

        public Node(String key, List<Opportunity> opportunities) {
            this.key = key;
            referenced = true;
            this.opporList = opportunities;
            next = null;
        }
    }

}