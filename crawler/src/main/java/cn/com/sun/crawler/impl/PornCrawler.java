package cn.com.sun.crawler.impl;

import cn.com.sun.crawler.AbstractVideoCrawler;
import cn.com.sun.crawler.CrawlerConfig;
import cn.com.sun.crawler.VideoCrawler;
import cn.com.sun.crawler.entity.Video;
import cn.com.sun.crawler.util.HttpClient;
import cn.com.sun.crawler.util.VideoHandler;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class PornCrawler extends AbstractVideoCrawler {

    private static final Logger logger = LoggerFactory.getLogger(PornyCrawler.class);
    private static Browser browser;
    private static CountDownLatch browserStarted = new CountDownLatch(1);
    private VideoHandler videoHandler = new VideoHandler();

    @Override
    public List<Video> getVideoBaseInfo(Document document) {
        Elements elements = document.select(".well.well-sm");
        List<Video> videoList = new ArrayList<>();
        for (Element content : elements) {
            Video video = new Video();
            Element a = content.select("a").first();
            // id
            video.setId(a.select("div").first().attr("id"));
            // singlePageUrl
            video.setPageUrl(a.attr("href"));
            // title
            video.setTitle(a.select(".video-title").first().text());
            videoList.add(video);
        }
        return videoList;
    }

    @Override
    protected Callable<Boolean> createDownloadTask(Video video) {
        if (video.getDownloadUrl().contains(".mp4?")) {
            return () -> HttpClient.downloadVideoToFs(video, CrawlerConfig.workspace);
        } else {
            return () -> videoHandler.downloadFromM3U8(video, CrawlerConfig.workspace);
        }
    }

    @Override
    public VideoCrawler parseVideoExtInfo() {
        for (Video video : videoList) {
            String pageHtml = HttpClient.getHtmlByHttpClient(video.getPageUrl());
            Document document = Jsoup.parse(pageHtml);
            Elements infos = document.selectFirst(".boxPart").select(".info");
            for (int index = 0; index < infos.size(); index++) {
                Element info = infos.get(index);
                // duration
                if (index == 0) {
                    String durationStr = info.child(0).text().trim();
                    int minutes = Integer.parseInt(durationStr.split(":")[0]);
                    int seconds = Integer.parseInt(durationStr.split(":")[1]);
                    video.setDuration(minutes * 60 + seconds);
                } else if (index == 1) {
                    // watchNum
                    video.setWatchNum(Integer.parseInt(info.child(0).text().trim()));
                } else if (index == 3) {
                    // storeNum
                    video.setStoreNum(Integer.parseInt(info.child(0).text().trim()));
                } else continue;
            }
            // author
            video.setAuthor(document.select(".title-yakov").last().selectFirst(".title").text());
            // shareUrl
            video.setShareUrl(document.selectFirst("#linkForm2 #fm-video_link").text());
        }
        return this;
    }

    @Override
    public VideoCrawler parseDownloadUrl() {
        for (Video video : videoList) {
            String downloadUrl = "";
            // 页面里面取
            downloadUrl = fromShareUrl(video);
            // 从分享链接里面取
            if (downloadUrl.isEmpty()) {
                downloadUrl = fromPageUrl(video);
            }
            if (downloadUrl.isEmpty()) {
                logger.warn("get {} download url failed", video.getTitle());
                continue;
            }
            video.setDownloadUrl(downloadUrl);
            logger.info("get {} download url: {}", video.getTitle(), video.getDownloadUrl());
            downloadList.add(video);
        }
        // 关闭浏览器
        if (CefApp.getState() == CefApp.CefAppState.INITIALIZED) {
            getBrowser().destroy();
        }
        return this;
    }

    private String fromPageUrl(Video video) {
        String downloadUrl = "";
        for (int i = 0; i < 5; i++) {
            String pageHtml = getBrowser().getHtml(video.getPageUrl());
            Element document = Jsoup.parse(pageHtml);
            if (document.selectFirst("source") == null) {
                continue;
            } else {
                downloadUrl = document.selectFirst("source").attr("src");
                break;
            }
        }
        return downloadUrl;
    }

    private String fromShareUrl(Video video) {
        String downloadUrl = "";
        // 分享链接里面取
        if (!video.getShareUrl().isEmpty()) {
            for (int i = 0; i < 5; i++) {
                String shareHtml = getBrowser().getHtml(video.getShareUrl());
                Element shareDocument = Jsoup.parse(shareHtml);
                if (shareDocument.selectFirst("source") == null) {
                    continue;
                } else {
                    downloadUrl = shareDocument.selectFirst("source").attr("src");
                    break;
                }
            }
        }
        return downloadUrl;
    }

    private Browser getBrowser() {
        if (browser == null) {
            browser = new Browser();
            try {
                browserStarted.await();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return browser;
    }

    private class Browser {
        private CefBrowser browser;
        private String currentHtml = "";
        private JFrame jFrame;
        private CefApp cefApp;
        private AtomicReference<CountDownLatch> countDownLatch = new AtomicReference<>();

        private Browser() {
            jFrame = new JFrame();
            cefApp = CefApp.getInstance();
            CefClient cefClient = cefApp.createClient();
            cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadEnd(CefBrowser cefBrowser, CefFrame cefFrame, int i) {
                    cefBrowser.getSource(html -> {
                        if (browserStarted.getCount() > 0) {
                            // 解除等待
                            browserStarted.countDown();
                            // 最小化窗口
                            jFrame.setExtendedState(Frame.ICONIFIED);
                            return;
                        }
                        currentHtml = html;
                        countDownLatch.get().countDown();
                    });
                }
            });
            //
            browser = cefClient.createBrowser("file:///D:/Program/workspace/IDEA/java/JavaDemo/crawler/src/main/resources/index.html", OS.isLinux(), false);
            jFrame.getContentPane().add(browser.getUIComponent(), BorderLayout.CENTER);
            jFrame.pack();
            jFrame.setSize(500, 500);
            jFrame.setResizable(false);
            jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            jFrame.setVisible(true);
        }

        public String getHtml(String url) {
            // 初始化
            currentHtml = "";
            logger.info("browser loading {}", url);
            browser.loadURL(url);
            CountDownLatch latch = new CountDownLatch(1);
            countDownLatch.set(latch);
            try {
                latch.await();
                logger.info("browser load {} success", url);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
            return currentHtml;
        }

        public void destroy() {
            CefApp.getInstance().dispose();
            jFrame.dispose();
        }
    }
}