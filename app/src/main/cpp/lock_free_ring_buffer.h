#pragma once

#include <atomic>
#include <cstddef>

/**
 * LockFreeRingBuffer: Single-Producer Single-Consumer (SPSC) circular buffer
 * designed for zero-lock, real-time audio threads.
 */
template <typename T, size_t Capacity>
class LockFreeRingBuffer {
public:
    LockFreeRingBuffer() : head_(0), tail_(0) {}

    bool push(const T* data, size_t count) {
        size_t head = head_.load(std::memory_order_relaxed);
        size_t tail = tail_.load(std::memory_order_acquire);
        size_t available = (tail > head) ? (tail - head - 1) : (Capacity - head + tail - 1);
        
        if (count > available) return false;

        for (size_t i = 0; i < count; ++i) {
            buffer_[(head + i) % Capacity] = data[i];
        }
        head_.store((head + count) % Capacity, std::memory_order_release);
        return true;
    }

    bool pop(T* destination, size_t count) {
        size_t head = head_.load(std::memory_order_acquire);
        size_t tail = tail_.load(std::memory_order_relaxed);
        size_t available = (head >= tail) ? (head - tail) : (Capacity - tail + head);

        if (count > available) return false;

        for (size_t i = 0; i < count; ++i) {
            destination[i] = buffer_[(tail + i) % Capacity];
        }
        tail_.store((tail + count) % Capacity, std::memory_order_release);
        return true;
    }

    size_t getAvailableRead() const {
        size_t head = head_.load(std::memory_order_acquire);
        size_t tail = tail_.load(std::memory_order_relaxed);
        return (head >= tail) ? (head - tail) : (Capacity - tail + head);
    }

    void clear() {
        head_.store(0, std::memory_order_relaxed);
        tail_.store(0, std::memory_order_relaxed);
    }

private:
    T buffer_[Capacity];
    std::atomic<size_t> head_;
    std::atomic<size_t> tail_;
};
