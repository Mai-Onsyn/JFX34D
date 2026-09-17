module;
#include <condition_variable>
#include <latch>
#include <map>
#include <memory>
#include <semaphore>
#include <set>
#include <string>
#include <vector>
export module Types;

// 数据类型
export typedef uint8_t UInt8;
export typedef uint16_t UInt16;
export typedef uint32_t UInt32;
export typedef uint64_t UInt64;
export typedef int8_t Int8;
export typedef int16_t Int16;
export typedef int32_t Int32;
export typedef int64_t Int64;
export typedef float Float;
export typedef double Double;
export typedef bool Boolean;
export typedef std::string String;

// 渲染缓冲区
export typedef std::unique_ptr<UInt8[]> UInt8Buffer;
export typedef std::shared_ptr<UInt8[]> SharedUInt8Buffer;
export typedef std::unique_ptr<Float[]> FloatBuffer;

export typedef std::mutex Mutex;
export typedef std::condition_variable ConditionVariable;
export typedef std::lock_guard<Mutex> LockGuard;
export typedef std::unique_lock<Mutex> UniqueLock;
export typedef std::binary_semaphore Semaphore;
export typedef std::latch Latch;

export typedef std::runtime_error RuntimeError;

export inline UInt8Buffer makeUInt8Buffer(const UInt32 size) {
    return std::make_unique_for_overwrite<UInt8[]>(size);
}

export inline SharedUInt8Buffer makeSharedUInt8Buffer(const UInt32 size) {
    return std::make_shared<UInt8[]>(size);
}

export inline FloatBuffer makeFloatBuffer(const UInt32 size) {
    return std::make_unique_for_overwrite<Float[]>(size);
}

export template<typename T> using List = std::vector<T>;
export template<typename T> using Atomic = std::atomic<T>;
export template<typename K, typename V> using Map = std::map<K, V>;
export template<typename T> using Set = std::set<T>;
export template<typename K, typename V> using Pair = std::pair<K, V>;
export template<typename T> using UniquePtr = std::unique_ptr<T>;
export template<typename T> using SharedPtr = std::shared_ptr<T>;

export using std::make_unique;
export using std::make_unique_for_overwrite;
export using std::move;
export using std::stoi;
export using std::stof;
export using std::stoll;