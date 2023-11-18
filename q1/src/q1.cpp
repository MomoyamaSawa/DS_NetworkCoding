#include <iostream>
#include <fstream>
#include <vector>
#include <random>
#include <chrono>

const std::string FILENAME = "h2q1.dat";
const int SEED = 244;

double generateRandomDouble(double min, double max, std::mt19937 &gen)
{
    // 分布对象std::uniform_real_distribution<double> 是C++标准库 <random> 中的一个模板类，它描述了一个在指定范围内的均匀分布的实数。
    std::uniform_real_distribution<double> dis(min, max);
    return dis(gen);
}

int main()
{
    /*
    std::mt19937 是一个实现了Mersenne Twister算法的随机数生成器。
    它生成的随机数序列具有很高的质量和非常大的周期（2^19937-1），
    但它比一些其他的随机数生成器慢一些。
    */
    std::mt19937 gen(SEED);
    std::vector<std::pair<double, double>> data;
    std::vector<std::pair<double, double>> group1, group2, group3;
    // uint8_t是一字节整数，范围是0~255
    std::vector<uint8_t> byteData;

    double d1, d2;
    // 提供了最高精度的时钟功能。这个类通常用于测量代码段的执行时间，或者在需要高精度时间戳的情况下使用。
    auto start = std::chrono::high_resolution_clock::now();
    while (true)
    {
        d1 = generateRandomDouble(0.0, 1.0, gen);
        d2 = generateRandomDouble(0.0, 1.0, gen);
        auto pair = std::make_pair(d1, d2);
        data.push_back(pair);
        if (pair.first < 0.46 && pair.second < 0.46)
            group1.push_back(pair);
        else if (pair.first > 0.72 && pair.second > 0.72)
            group3.push_back(pair);
        else
            group2.push_back(pair);

        // reinterpret_cast是强制类型转换
        uint8_t *pD1 = reinterpret_cast<uint8_t *>(&d1);
        uint8_t *pD2 = reinterpret_cast<uint8_t *>(&d2);
        byteData.insert(byteData.end(), pD1, pD1 + sizeof(double));
        byteData.insert(byteData.end(), pD2, pD2 + sizeof(double));

        if (d1 > 0.91 && d2 > 0.91)
            break;
    }

    std::ofstream file(FILENAME, std::ios::binary);
    if (!file.is_open())
    {
        std::cout << "Failed to open the file." << std::endl;
        return 1;
    }

    uint32_t group1Pairs = group1.size();
    uint32_t group2Pairs = group2.size();
    uint32_t group3Pairs = group3.size();
    uint32_t totalPairs = group1Pairs + group2Pairs + group3Pairs;

    // 这边把那个图片上的空的位置也预留了出来，所以文件头是 8*4 = 32 个字节
    uint32_t group1StartPos = sizeof(uint32_t) * 8;
    uint32_t group2StartPos = group1StartPos + sizeof(double) * group1Pairs * 2;
    uint32_t group3StartPos = group2StartPos + sizeof(double) * group2Pairs * 2;

    file.write(reinterpret_cast<char *>(&totalPairs), sizeof(totalPairs));
    file.write(reinterpret_cast<char *>(&group1Pairs), sizeof(group1Pairs));
    file.write(reinterpret_cast<char *>(&group1StartPos), sizeof(group1StartPos));
    file.write(reinterpret_cast<char *>(&group2Pairs), sizeof(group2Pairs));
    file.write(reinterpret_cast<char *>(&group2StartPos), sizeof(group2StartPos));
    file.write(reinterpret_cast<char *>(&group3Pairs), sizeof(group3Pairs));
    file.write(reinterpret_cast<char *>(&group3StartPos), sizeof(group3StartPos));

    file.write(reinterpret_cast<char *>(group1.data()), group1.size());
    file.write(reinterpret_cast<char *>(group2.data()), group2.size());
    file.write(reinterpret_cast<char *>(group3.data()), group3.size());

    file.close();

    auto end = std::chrono::high_resolution_clock::now();
    std::chrono::duration<double> diff = end - start;

    std::cout << "Total time taken: " << diff.count() << " s\n";

    int countValues = 0, countPairs = 0;
    for (const auto &pair : data)
    {
        if (pair.first > 0.5 || pair.second > 0.5)
            countValues++;
        if (pair.first > 0.5 && pair.second > 0.5)
            countPairs++;
    }

    std::cout << "Number of values > 0.5: " << countValues << "\n";
    std::cout << "Number of pairs > 0.5: " << countPairs << "\n";

    return 0;
}