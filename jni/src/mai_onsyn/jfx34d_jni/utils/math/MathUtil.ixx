module;
#include <vector>
export module MathUtil;
import Types;

export List<Pair<Int32, Int32>> findAllFactors(const Int32 n) {
    List<Pair<Int32, Int32>> primes;
    if (n < 1) return primes;
    for (Int32 i = 1; i * i <= n; i++) {
        if (n % i == 0) {
            primes.emplace_back(i, n / i);
        }
    }

    return primes;
}
