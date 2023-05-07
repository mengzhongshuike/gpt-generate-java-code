package com.example.gpt.generate;

import org.junit.Assert;
import org.junit.Test;

public class LockFreeQueueTest {
    @Test
    public void test() {
        LockFreeQueue<String> lockFreeQueue = new LockFreeQueue<>();
        lockFreeQueue.enqueue("1");
        lockFreeQueue.enqueue("2");
        lockFreeQueue.enqueue("3");

        Assert.assertEquals("1", lockFreeQueue.dequeue());
        Assert.assertEquals("2", lockFreeQueue.dequeue());
        Assert.assertEquals("3", lockFreeQueue.dequeue());
        Assert.assertNull(lockFreeQueue.dequeue());
    }
}
