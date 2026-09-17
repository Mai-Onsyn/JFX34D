module;
#include <mutex>
#include <queue>
export module Queue;
import Types;

using std::move;
using std::queue;

export template<typename T>
class Queue {
    queue<T> queue;
    Mutex mtx;
    ConditionVariable cv;
    Boolean isClosed = false;
public:
    void push(const T& value) {
        {
            LockGuard lock(mtx);
            queue.push(value);
        }
        cv.notify_all();
    }

    void push(T&& value) {
        {
            LockGuard lock(mtx);
            queue.push(move(value));
        }
        cv.notify_all();
    }

    const T& front() {
        LockGuard lock(mtx);
        return queue.front();
    }

    T pop() {
        UniqueLock lock(mtx);
        T value = move(queue.front());
        queue.pop();

        return value;
    }

    UInt32 size() {
        LockGuard lock(mtx);
        return queue.size();
    }

    Boolean empty() {
        LockGuard lock(mtx);
        return queue.empty();
    }

    void close() {
        {
            LockGuard lock(mtx);
            isClosed = true;
        }
        cv.notify_all();
    }


    void clear() {
        {
            LockGuard lock(mtx);
            while (!queue.empty()) {
                queue.pop();
            }
        }
        cv.notify_all();
    }
};