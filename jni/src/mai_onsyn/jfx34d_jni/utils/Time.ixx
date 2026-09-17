module;
#include <chrono>
#include <iomanip>
export module Time;
import Types;

typedef std::tm TimeBuffer;

using namespace std::chrono;
using std::chrono::duration_cast;

export Int64 millisTime() {
    const auto now = system_clock::now();
    const auto millis = duration_cast<milliseconds>(now.time_since_epoch());
    return millis.count();
}

export Int64 microTime() {
    const auto now = steady_clock::now();
    const auto nanos = duration_cast<microseconds>(now.time_since_epoch());
    return nanos.count();
}

export Int64 nanoTime() {
    const auto now = steady_clock::now();
    const auto nanos = duration_cast<nanoseconds>(now.time_since_epoch());
    return nanos.count();
}

static TimeBuffer toLocalTime(const Int64 millis) {
    auto sec = millis / 1000;
#ifdef _WIN32
    TimeBuffer tmBuf{};
    localtime_s(&tmBuf, &sec);
    return tmBuf;
#else
    TimeBuffer tmBuf{};
    localtime_r(&sec, &tmBuf);
    return tmBuf;
#endif
}

export String formatTimeMillis(const Int64 time, const String &format = "yyyy-MM-dd HH:mm:ss.SSS") {
    // 1. 分离毫秒部分
    const Int64 millisPart = time % 1000;
    const TimeBuffer tm = toLocalTime(time);

    // 辅助 lambda：根据重复次数格式化数字
    auto formatNumber = [](const int value, const int width) -> String {
        if (width <= 0) return std::to_string(value);
        std::ostringstream oss;
        oss << std::setw(width) << std::setfill('0') << value;
        return oss.str();
    };

    String result;
    size_t i = 0;
    const size_t len = format.size();

    while (i < len) {
        char ch = format[i];
        // 判断当前字符是否是模式字母
        bool isPattern = (ch == 'y' || ch == 'M' || ch == 'd' || ch == 'H' ||
                          ch == 'h' || ch == 'm' || ch == 's' || ch == 'S');
        if (!isPattern) {
            // 普通字符直接追加
            result.push_back(ch);
            ++i;
            continue;
        }

        // 统计连续相同字母的个数
        int count = 0;
        while (i < len && format[i] == ch) {
            ++count;
            ++i;
        }

        // 根据模式字母输出
        switch (ch) {
            case 'y': {
                int year = tm.tm_year + 1900;
                if (count == 2) {
                    year %= 100;       // 两位年份
                } else if (count != 4) {
                    // 如果既不是2也不是4，输出完整的年份（不补零）
                    result += std::to_string(year);
                    break;
                }
                result += formatNumber(year, count);
                break;
            }
            case 'M': {
                int month = tm.tm_mon + 1;   // 月份 1-12
                result += formatNumber(month, count);
                break;
            }
            case 'd': {
                int day = tm.tm_mday;
                result += formatNumber(day, count);
                break;
            }
            case 'H': {
                int hour = tm.tm_hour;       // 0-23
                result += formatNumber(hour, count);
                break;
            }
            case 'h': {
                int hour12 = tm.tm_hour % 12;
                if (hour12 == 0) hour12 = 12;
                result += formatNumber(hour12, count);
                break;
            }
            case 'm': {
                int minute = tm.tm_min;
                result += formatNumber(minute, count);
                break;
            }
            case 's': {
                int second = tm.tm_sec;
                result += formatNumber(second, count);
                break;
            }
            case 'S': {
                // 毫秒部分固定 0-999，若 count>3 则忽略多余的宽度限制
                int width = (count > 3) ? 3 : count;
                result += formatNumber(static_cast<int>(millisPart), width);
                break;
            }
            default:
                // 不会执行到这里
                break;
        }
    }
    return result;
}