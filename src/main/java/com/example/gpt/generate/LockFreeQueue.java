package com.example.gpt.generate;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeQueue<T> {
    private final AtomicMarkableReference<Node<T>> head;
    private final AtomicMarkableReference<Node<T>> tail;

    public LockFreeQueue() {
        Node<T> dummy = new Node<>(null);
        head = new AtomicMarkableReference<>(dummy, false);
        tail = new AtomicMarkableReference<>(dummy, false);
    }

    public void enqueue(T value) {
        Node<T> newNode = new Node<>(value);
        Node<T> prevTail;
        Node<T> curTail;

        do {
            prevTail = tail.getReference();
            curTail = prevTail.next.getReference();
            if (curTail != null) {
                tail.compareAndSet(prevTail, curTail, false, false);
            }
        } while (!prevTail.next.compareAndSet(curTail, newNode, false, false));

        tail.compareAndSet(prevTail, newNode, false, false);
    }

    public T dequeue() {
        Node<T> curHead;
        Node<T> nextHead;

        do {
            curHead = head.getReference();
            if (curHead == tail.getReference()) {
                return null;
            }
            nextHead = curHead.next.getReference();
        } while (!head.compareAndSet(curHead, nextHead, false, false));

        return nextHead.value;
    }
}

class Node<T> {
    final T value;
    final AtomicMarkableReference<Node<T>> next;

    public Node(T value) {
        this.value = value;
        this.next = new AtomicMarkableReference<>(null, false);
    }
}