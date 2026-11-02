package com.example.dsp

import java.util.concurrent.atomic.AtomicInteger

/**
 * High-performance lock-free Single-Producer Single-Consumer (SPSC) circular buffer
 * for audio sample streaming without memory allocations or thread locks.
 */
class LockFreeRingBuffer(private val capacity: Int) {
    private val buffer = FloatArray(capacity)
    private val head = AtomicInteger(0)
    private val tail = AtomicInteger(0)

    fun push(data: FloatArray, offset: Int, count: Int): Boolean {
        val currentHead = head.get()
        val currentTail = tail.get()
        val available = if (currentTail > currentHead) {
            currentTail - currentHead - 1
        } else {
            capacity - currentHead + currentTail - 1
        }

        if (count > available) return false

        for (i in 0 until count) {
            buffer[(currentHead + i) % capacity] = data[offset + i]
        }
        head.set((currentHead + count) % capacity)
        return true
    }

    fun pop(destination: FloatArray, offset: Int, count: Int): Boolean {
        val currentHead = head.get()
        val currentTail = tail.get()
        val available = if (currentHead >= currentTail) {
            currentHead - currentTail
        } else {
            capacity - currentTail + currentHead
        }

        if (count > available) return false

        for (i in 0 until count) {
            destination[offset + i] = buffer[(currentTail + i) % capacity]
        }
        tail.set((currentTail + count) % capacity)
        return true
    }

    fun availableRead(): Int {
        val currentHead = head.get()
        val currentTail = tail.get()
        return if (currentHead >= currentTail) {
            currentHead - currentTail
        } else {
            capacity - currentTail + currentHead
        }
    }

    fun availableWrite(): Int {
        val currentHead = head.get()
        val currentTail = tail.get()
        return if (currentTail > currentHead) {
            currentTail - currentHead - 1
        } else {
            capacity - currentHead + currentTail - 1
        }
    }

    fun clear() {
        head.set(0)
        tail.set(0)
    }
}
