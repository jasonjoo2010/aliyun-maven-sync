package com.yoloho.tools.aliyun;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpHead;

import com.yoloho.enhanced.common.util.HttpClientUtil;

public class Sync {
    private static final String CENTRAL_BASE = "http://repo1.maven.org/maven2/";
    private static final String ALIYUN_BASE = "https://maven.aliyun.com/repository/public/";
    private static final Pattern ENTRY_PATTERN = Pattern.compile("<a href=\"([^\"]+)\" title=\"[^\"]+\">[^<]+</a>",
            Pattern.CASE_INSENSITIVE);
    
    /**
     * Touch it to make it cached, that means, a request of HEAD is sent.
     * 
     * @param fullUrl
     */
    private static void touch(String fullUrl) {
        System.out.println("Touching " + fullUrl);
        HttpHead httpHead = new HttpHead(fullUrl);
        try {
            HttpClientUtil.executeRequest(httpHead);
        } catch (Exception e) {
        }
    }

    /**
     * Dig the structure of directory recursively
     * 
     * @param url
     */
    private static void digPath(String url) {
        if (!url.endsWith("/")) {
            url += "/";
        }
        String body = null;
        int retry = 3;
        while (retry -- > 0) {
            try {
                body = HttpClientUtil.getRequest(url);
                break;
            } catch (Exception e) {
                System.out.println("Retry to query central");
            }
        }
        Matcher m = ENTRY_PATTERN.matcher(body);
        while (m.find()) {
            String entry = m.group(1);
            if (entry.endsWith("/")) {
                // directory
                digPath(url + entry);
            } else {
                // file
                String centralUrl = url + entry;
                String aliyunUrl = centralUrl.replace(CENTRAL_BASE, ALIYUN_BASE);
                touch(aliyunUrl);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: Sync <base package>");
            System.out.println("eg. Sync com.springframework");
            System.exit(1);
        }
        String base = args[0];
        if (StringUtils.isBlank(base)) {
            System.out.println("Package is blank");
            System.exit(2);
        }
        Pattern packagePattern = Pattern.compile("^[a-zA-Z0-9._\\-]+$");
        Matcher m = packagePattern.matcher(base);
        if (!m.find()) {
            System.out.println("Package is not legal.");
            System.exit(3);
        }
        String baseUrl = CENTRAL_BASE + base.replace('.', '/');
        digPath(baseUrl);
    }
}
