package com.pcdd.sonovel.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.NumberUtil;
import com.pcdd.sonovel.core.Crawler;
import com.pcdd.sonovel.core.Source;
import com.pcdd.sonovel.model.AppConfig;
import com.pcdd.sonovel.model.Book;
import com.pcdd.sonovel.model.Chapter;
import com.pcdd.sonovel.model.SearchResult;
import com.pcdd.sonovel.parse.BookParser;
import com.pcdd.sonovel.parse.CatalogParser;
import com.pcdd.sonovel.parse.SearchResultParser;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;

import java.util.List;

import static org.fusesource.jansi.AnsiRenderer.render;

/**
 * @author pcdd
 * Created at 2024/11/10
 */
@AllArgsConstructor
public class DownloadAction {

    private final AppConfig config;

    public void downloadFromUrl(LineReader reader) {
        String bookUrl = reader.readLine(render("==> @|blue 请输入详情页 URL: |@")).strip();
        Book book = new BookParser(config).parse(bookUrl);
        SearchResult sr = SearchResult.builder()
                .url(book.getUrl())
                .bookName(book.getBookName())
                .author(book.getAuthor())
                .latestChapter(book.getLatestChapter())
                .latestUpdate(book.getLatestUpdate())
                .build();
        List<Chapter> catalogs = new CatalogParser(config).parse(sr.getUrl());
        Console.log("<== 《{}》({})，共计 {} 章", sr.getBookName(), sr.getAuthor(), catalogs.size());
        double res = new Crawler(config).crawl(sr, catalogs);
        Console.log("<== 完成！总耗时 {} s\n", NumberUtil.round(res, 2));
    }

    public void downloadByKeyword(LineReader reader) {
        // 1. 查询
        String keyword = reader.readLine(render("==> @|blue 请输入书名或作者（宁少字别错字）: |@")).strip();
        if (keyword.isEmpty()) return;
        List<SearchResult> results = new Crawler(config).search(keyword);
        if (results.isEmpty()) return;

        // 2. 打印搜索结果
        SearchResultParser.printSearchResult(results);

        int num;
        int action;
        SearchResult sr;
        List<Chapter> catalogs;
        // 3. 选择下载章节
        while (true) {
            String input = reader.readLine("==> 请输入下载序号（首列的数字，或输入 0 返回）：").strip();
            // 健壮性判断：必须为数字
            try {
                num = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                continue;
            }

            // 没有搜到想要的书，返回
            if (num == 0) return;
            // 健壮性判断：必须为首列的序号
            if (num < 0 || num > results.size()) continue;

            sr = results.get(num - 1);
            Console.log("<== 正在获取章节目录 ...");
            catalogs = new CatalogParser(config).parse(sr.getUrl(), 1, Integer.MAX_VALUE);

            Console.log("<== 你选择了《{}》({})，共计 {} 章", sr.getBookName(), sr.getAuthor(), catalogs.size());
            Console.log("==> 0: 重新选择功能");
            Console.log("==> 1: 下载全本");
            Console.log("==> 2: 下载指定范围章节");
            Console.log("==> 3: 下载最新章节");
            Console.log("==> 4: 重新输入序号");

            try {
                action = Integer.parseInt(reader.readLine("==> 请输入序号："));
            } catch (NumberFormatException e) {
                continue;
            }

            if (action != 4) break;
        }
        if (action == 0) return;
        if (action == 2) {
            try {
                String[] split = reader.readLine("==> 请输起始章(最小为1)和结束章，用空格隔开：").strip().split("\\s+");
                int start = Integer.parseInt(split[0]) - 1;
                int end = Integer.parseInt(split[1]);
                catalogs = CollUtil.sub(catalogs, start, end);
            } catch (Exception e) {
                return;
            }
        }
        if (action == 3) {
            try {
                int i = Integer.parseInt(reader.readLine("==> 请输入要下载最新章节的数量："));
                catalogs = CollUtil.sub(catalogs, catalogs.size() - i, catalogs.size());
            } catch (Exception e) {
                return;
            }
        }

        double res = new Crawler(config).crawl(sr, catalogs);
        Console.log("<== 完成！总耗时 {} s\n", NumberUtil.round(res, 2));
    }

    @SneakyThrows
    public void execute(Terminal terminal) {
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
        Source source = new Source(config.getSourceId());

        // URL 下载的小说，rule.json 删除 search
        if (source.rule.getSearch() == null) {
            downloadFromUrl(reader);
        } else {
            downloadByKeyword(reader);
        }
    }

}