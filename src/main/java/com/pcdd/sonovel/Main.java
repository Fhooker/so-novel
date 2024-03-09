package com.pcdd.sonovel;

import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.ConsoleTable;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.setting.dialect.Props;
import com.pcdd.sonovel.model.SearchResult;
import com.pcdd.sonovel.core.Crawler;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Scanner;

/**
 * @author pcdd
 * Created at 2021/6/10 16:18
 */
public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        printHint();

        while (true) {
            String keyword = scanner.nextLine().trim();
            if (keyword.isEmpty()) {
                Console.log("==> 请输入书名或作者：");
                continue;
            }
            if ("exit".equals(keyword)) {
                Console.log("<== bye bye ^-^");
                break;
            }

            List<SearchResult> results = Crawler.search(keyword);
            if (results.isEmpty()) {
                continue;
            }

            ConsoleTable consoleTable = ConsoleTable.create()
                    .addHeader("序号", "书名", "作者", "最新章节", "最后更新时间");
            // 打印搜索结果
            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
                consoleTable.addBody(String.valueOf(i),
                        r.getBookName(),
                        r.getAuthor(),
                        r.getLatestChapter(),
                        r.getLatestUpdate()
                );
            }
            Console.table(consoleTable);

            Console.log("==> 请输入下载序号（首列的数字）");
            int num = scanner.nextInt();

            Console.log("==> 0：下载全本");
            Console.log("==> 1：下载指定章节");
            int downloadPolicy = scanner.nextInt();
            int start = 1;
            int end = Integer.MAX_VALUE;
            if (downloadPolicy == 1) {
                Console.log("==> 请输起始章(最小为1)和结束章，用空格隔开");
                start = scanner.nextInt();
                end = scanner.nextInt();
            }

            double res = Crawler.crawl(results, num, start, end);

            Console.log("\n<== 下载完毕，总耗时 {} s\n", NumberUtil.round(res, 2));
        }

    }

    private static void printHint() {
        Props p = new Props("config.properties");
        Console.table(ConsoleTable.create()
                // 是否转为全角
                .setSBCMode(false)
                .addHeader("so-novel")
                .addHeader("版本：" + p.getStr("version"))
                .addBody("使用须知")
                .addBody("1. 下载速度受书源、网络、爬取间隔等因素影响，若下载失败可尝试修改爬取间隔")
                .addBody("2. 结束程序请输入 exit")
                .addBody("3. 请按要求输入")
        );
        Console.log("==> 请输入书名或作者：");
    }

}
