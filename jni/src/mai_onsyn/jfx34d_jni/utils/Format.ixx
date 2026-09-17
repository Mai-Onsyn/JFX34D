module;
#include <string>
#include <sstream>
#include <iomanip>
export module Format;
import Types;

using namespace std;

template<typename T>
auto toPrintable(const T& value) {
    if constexpr (std::is_same_v<T, char> || std::is_same_v<T, unsigned char> || std::is_same_v<T, signed char>) {
        return static_cast<int>(value);
    } else {
        return value;
    }
}

template<typename T>
void append(stringstream& ss, const char* fmt, Int32& offset, const T& value) {
    while (fmt[offset] != '\0') {
        if (fmt[offset] == '%') {
            if (fmt[offset + 1] == '%') {
                ss << '%';
                offset += 2;
                continue;
            }
            break;
        }
        ss << fmt[offset++];
    }
    if (fmt[offset++] == '%') {
        if (fmt[offset] == '\0') return;
        switch (fmt[offset++]) {
            case 's':
                ss << value;
                break;
            case 'd':
                ss << dec << toPrintable(value);
                ss.unsetf(ios::fixed);
                break;
            case 'b':
                ss << boolalpha << value;
                ss.unsetf(ios::fixed);
                break;
            case '.':
                if (fmt[offset] >= '0' && fmt[offset] <= '9') {
                    char* endPtr = nullptr;
                    const auto precision = strtol(fmt + offset, &endPtr, 10);
                    const auto len = endPtr - (fmt + offset);
                    if (fmt[offset + len] == 'f') {
                        ss << fixed << setprecision(precision) << value;
                        ss.unsetf(ios::fixed);
                        offset += len + 1;
                    } else {
                        ss << value;
                    }
                } else {
                    ss << value;
                }
                break;
            case '<':
                if (fmt[offset] >= '0' && fmt[offset] <= '9') {
                    char* endPtr = nullptr;
                    const auto precision = strtol(fmt + offset, &endPtr, 10);
                    const auto len = endPtr - (fmt + offset);
                    if (fmt[offset + len] == 'f') {
                        ss << fixed << setprecision(precision) << value;
                        ss.unsetf(ios::fixed);
                        offset += len + 1;
                    } else {
                        ss << value;
                    }
                } else {
                    ss << value;
                }
                break;
            default: ss << value;
        }
    }
}

export template<typename... Args>
String format(const String &format, Args &&... args) {
    stringstream ss;
    const auto & fmt = format.c_str();
    Int32 writeOffset = 0;

    // 展开格式化
    (append(ss, fmt, writeOffset, args), ...);

    // 剩余部分
    append(ss, fmt, writeOffset, "");
    return ss.str();
}

export namespace Stringf {
    template<typename... Args>
    std::string format(const std::string &format, Args &&... args) {
        return ::format(String(format), args...);
    }
}