module;
#include <iostream>
export module Logger;
import Types;
import Time;
import Format;

using std::cout;
using std::endl;

Mutex mtx;
ConditionVariable cv;

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
};

String toString(const LogLevel level) {
    switch (level) {
        case LogLevel::DEBUG:
            return "DEBUG";
        case LogLevel::INFO:
            return "INFO";
        case LogLevel::WARN:
            return "WARN";
        case LogLevel::ERROR:
            return "ERROR";
    }
    return "LOG";
}

export class Log {
    template<typename... Args>
    static void log(const String& msg, const LogLevel level, Args&&... args) {
        {
            LockGuard lock(mtx);
            cout << formatTimeMillis(millisTime(), "[HH:mm:ss] [")
                << toString(level)
                << "] ";
            const String formatted = format(msg, args...);
            cout << formatted;
            cout << "\n";
        }
        cv.notify_all();
    }
public:
    template<typename... Args>
    static void debug(const String& msg, Args&&... args) {
        log(msg, LogLevel::DEBUG, args...);
    }

    template<typename... Args>
    static void info(const String& msg, Args&&... args) {
        log(msg, LogLevel::INFO, args...);
    }

    template<typename... Args>
    static void warn(const String& msg, Args&&... args) {
        log(msg, LogLevel::WARN, args...);
    }

    template<typename... Args>
    static void error(const String& msg, Args&&... args) {
        log(msg, LogLevel::ERROR, args...);
    }
};
