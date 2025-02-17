package com.pcdd.sonovel.core;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Opt;
import cn.hutool.json.JSONUtil;
import com.pcdd.sonovel.model.AppConfig;
import com.pcdd.sonovel.model.Rule;
import com.pcdd.sonovel.util.ConfigUtils;
import com.pcdd.sonovel.util.CrawlUtils;
import com.pcdd.sonovel.util.RandomUA;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import static org.jline.jansi.AnsiRenderer.render;

/**
 * @author pcdd
 * Created at 2024/3/27
 */
public class Source {

    public final Rule rule;
    public final AppConfig config;

    // 配置文件读取书源 id
    public Source(AppConfig config) {
        this(config.getSourceId(), config);
    }

    public Source(int id) {
        this(id, null);
    }

    // 自定义书源 id，用于测试
    public Source(int id, AppConfig config) {
        String jsonStr = null;

        try {
            // 根据 sourceId 获取对应书源规则
            jsonStr = ResourceUtil.readUtf8Str("rule/rule-" + id + ".json");
        } catch (Exception e) {
            Console.error(render("@|red 书源规则初始化失败，请检查配置项 source-id 是否正确|@"));
            Console.error(render("@|red 错误信息：{}|@"), e.getMessage());
            System.exit(1);
        }

        // json 封装进 Rule
        this.rule = JSONUtil.toBean(jsonStr, Rule.class);
        this.config = Opt.ofNullable(config).orElse(ConfigUtils.config());
    }

    public Connection jsoupConn(String url, int timeout) {
        String method = rule.getSearch() != null ? rule.getSearch().getMethod() : "GET";

        Connection conn = Jsoup.connect(url)
                .method(CrawlUtils.buildMethod(method))
                .header("User-Agent", RandomUA.generate())
                .timeout(timeout);

        // 启用配置文件的代理地址
        if (config.getProxyEnabled() == 1)
            conn.proxy(config.getProxyHost(), config.getProxyPort());

        return conn;
    }

}