#include "syscall.h"
#include "strlen.c"

int main()
{
    char* filename = "file_test.txt";
    int fd_file_test = creat(filename);

    if (-1 == fd_file_test)
    {
        char* errorInfo = "Creat file failed!";
        write(fdStandardOutput, errorInfo, 15);
    }
    else
    {
        // 向标准输出设备写入（屏幕显示）
        char* succInfo = "File created!\n";
        write(fdStandardOutput, succInfo, 12);

        // 向文件中写入
        char texts[] = "Hello, world!\nHappy every day!\n";
        write(fd_file_test, texts, strlen(texts));

        // 关闭文件
        close(fd_file_test);

        // 再次打开文件
        fd_file_test = open(filename);

        if ( -1 != fd_file_test)
        {
            // 打开成功，从文件读入
            char text_from_file[100];
            int amount = read(fd_file_test, text_from_file, 15);

            // 屏显
            if (amount != 15)
            {
                write(fdStandardOutput, "read error", 10);
            }
            else
            {
                write(fdStandardOutput, text_from_file, 15);
            }
        }

        close(fd_file_test);
        unlink(filename);
    }
    halt();
}
