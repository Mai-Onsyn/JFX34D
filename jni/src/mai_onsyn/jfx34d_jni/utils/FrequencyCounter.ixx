module;
#include <algorithm>
#include <immintrin.h>
export module FrequencyCounter;
import Types;
import Time;
import Thread;

export class FrequencyCounter {
    const UInt64 capacity;

    UInt64 head = 0;
    UInt64 count = 0;

    UniquePtr<Int64[]> timestamps;
    UniquePtr<UInt32[]> deltaTimes;

    Int64 lastTickTime = 0;

    UInt64 getIndexOffset(const UInt64 backwardOffset) const {
        return (head + capacity - 1 - backwardOffset) % capacity;
    }

public:
    explicit FrequencyCounter(const UInt64 capacity)
        : capacity(capacity),
          timestamps(make_unique<Int64[]>(capacity)),
          deltaTimes(make_unique<UInt32[]>(capacity)) {}

    FrequencyCounter(const FrequencyCounter&) = delete;
    FrequencyCounter& operator=(const FrequencyCounter&) = delete;

    void tick() {
        const Int64 currentTime = microTime();

        const UInt32 delta = (lastTickTime == 0) ? 0 : static_cast<UInt32>(currentTime - lastTickTime);

        timestamps[head] = currentTime;
        deltaTimes[head] = delta;

        head = (head + 1) % capacity;
        if (count < capacity) {
            count++;
        }

        lastTickTime = currentTime;
    }

    Int64 getLastTickTime() const { return lastTickTime; }

    Float getAverageFrequency(const Float lastNSeconds) const {
        if (count < 2) return 0.0;

        const Int64 now = microTime();
        const Int64 cutoffTime = now - static_cast<Int64>(lastNSeconds * 1'000'000.0);

        UInt64 validSamples = 0;
        for (UInt64 i = 0; i < count; ++i) {
            const UInt64 idx = getIndexOffset(i);
            if (timestamps[idx] < cutoffTime) {
                break;
            }
            validSamples++;
        }

        if (validSamples < 2) return 0.0;

        const Int64 newestTime = timestamps[getIndexOffset(0)];
        const Int64 oldestTime = timestamps[getIndexOffset(validSamples - 1)];

        const Float totalSeconds = static_cast<Float>(newestTime - oldestTime) / 1'000'000.0f;
        if (totalSeconds <= 0.0f) return 0.0f;

        return static_cast<Float>(validSamples - 1) / totalSeconds;
    }

    Float getOnePercentLowFrequency(const Float lastNSeconds) const {
        if (count < 2) return 0.0f;

        const Int64 now = microTime();
        const Int64 cutoffTime = now - static_cast<Int64>(lastNSeconds * 1'000'000.0);

        List<Int64> localDeltas;
        localDeltas.reserve(count);

        for (UInt64 i = 0; i < count; ++i) {
            const UInt64 idx = getIndexOffset(i);
            if (timestamps[idx] < cutoffTime) break;

            if (deltaTimes[idx] > 0) {
                localDeltas.push_back(deltaTimes[idx]);
            }
        }

        if (localDeltas.empty()) return 0.0f;

        const UInt64 lowCount = std::max(static_cast<UInt64>(1), static_cast<UInt64>(localDeltas.size() * 0.01f));
        const UInt64 targetIndex = localDeltas.size() - lowCount;

        std::ranges::nth_element(localDeltas, localDeltas.begin() + targetIndex);

        UInt32 totalDeltaMicros = 0;
        for (UInt64 i = targetIndex; i < localDeltas.size(); ++i) {
            totalDeltaMicros += localDeltas[i];
        }

        const Float avgDeltaSeconds = (static_cast<Float>(totalDeltaMicros) / static_cast<Float>(lowCount)) / 1'000'000.0f;

        return (avgDeltaSeconds > 0.0f) ? (1.0f / avgDeltaSeconds) : 0.0f;
    }

    void clear() {
        head = 0;
        count = 0;
        lastTickTime = 0;
    }
};

export template <UInt64 TargetFrequency = 0>
class ThrottledFrequencyCounter {
    static constexpr Int64 targetFrameTimeMicros = (TargetFrequency > 0) ? (1'000'000 / TargetFrequency) : 0;

    UniquePtr<FrequencyCounter> counter;

    const Int64 throttleIntervalMicros;
    const Float targetWindowSeconds;

    mutable Int64 lastCachedTime = 0;
    mutable Float cachedAvgFreq = 0.0f;
    mutable Float cached1PercentLowFreq = 0.0f;

    void updateCache(const Int64 now) const {
        cachedAvgFreq = counter->getAverageFrequency(targetWindowSeconds);
        cached1PercentLowFreq = counter->getOnePercentLowFrequency(targetWindowSeconds);
        lastCachedTime = now;
    }

public:
    ThrottledFrequencyCounter(const UInt64 capacity, const Float targetWindowSeconds, const Float throttleSeconds = 1.0f)
        : counter(make_unique<FrequencyCounter>(capacity)),
          throttleIntervalMicros(static_cast<Int64>(throttleSeconds * 1'000'000.0f)),
          targetWindowSeconds(targetWindowSeconds) {}

    void tick() {
        if constexpr (TargetFrequency > 0) {
            const Int64 now = microTime();
            const Int64 elapsedMicros = now - counter->getLastTickTime();

            if (elapsedMicros < targetFrameTimeMicros) {
                Int64 remainingMicros = targetFrameTimeMicros - elapsedMicros;
                while (remainingMicros > 0) {
                    Thread::yield();
                    _mm_pause();
                    remainingMicros = targetFrameTimeMicros - (microTime() - counter->getLastTickTime());
                }
            }
        }

        counter->tick();
    }

    Float getAverageFrequency() const {
        const Int64 now = microTime();
        if ((lastCachedTime == 0) || (now - lastCachedTime >= throttleIntervalMicros)) {
            updateCache(now);
        }
        return cachedAvgFreq;
    }

    Float getOnePercentLowFrequency() const {
        const Int64 now = microTime();
        if ((lastCachedTime == 0) || (now - lastCachedTime >= throttleIntervalMicros)) {
            updateCache(now);
        }
        return cached1PercentLowFreq;
    }

    void clear() {
        counter->clear();
        lastCachedTime = 0;
        cachedAvgFreq = 0.0f;
        cached1PercentLowFreq = 0.0f;
    }
};