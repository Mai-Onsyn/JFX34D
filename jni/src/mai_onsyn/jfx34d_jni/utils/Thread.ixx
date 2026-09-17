module;
#include <thread>
#include <utility>
export module Thread;
import Types;

using std::jthread;
using std::this_thread::sleep_for;
export typedef std::stop_token StopToken;

export class Runnable {
public:
    virtual ~Runnable() = default;
    virtual void run() = 0;
};

export class Thread {
public:
    virtual ~Thread() = default;
    virtual void start() = 0;
    virtual void join() = 0;
    virtual void interrupt() = 0;
    [[nodiscard]] virtual bool joinable() const = 0;

    static void sleep(const UInt64 ms) {
        sleep_for(std::chrono::milliseconds(ms));
    }

    static void sleepMicro(const UInt64 us) {
        sleep_for(std::chrono::microseconds(us));
    }

    static std::thread::id getID() {
        return std::this_thread::get_id();
    }

    static void yield() {
        std::this_thread::yield();
    }
};

export template<typename F>
class BasicThread final : public Thread {
    jthread t;
    bool joined = false;
    F body;
public:

    BasicThread(const BasicThread&) = delete;
    BasicThread& operator=(const BasicThread&) = delete;
    BasicThread(BasicThread&& other) noexcept = default;
    BasicThread& operator=(BasicThread&& other) noexcept = default;

    explicit BasicThread(F body) : body(std::move(body)) {}

    void start() override {
        if (!joined && !t.joinable()) {
            t = jthread([this](const StopToken& st) {
                body(st);
            });
        }
    }

    void join() override {
        if (!joined && t.joinable()) {
            t.join();
            joined = true;
        }
    }

    void interrupt() override {
        if (t.joinable()) {
            t.request_stop();
        }
    }

    [[nodiscard]] bool joinable() const override {
        return t.joinable();
    }
};