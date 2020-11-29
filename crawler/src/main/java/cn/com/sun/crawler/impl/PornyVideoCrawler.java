package cn.com.sun.crawler.impl;

import cn.com.sun.crawler.entity.Video;
import cn.com.sun.crawler.util.FileAccessManager;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description : 91porny.com网站视频爬虫
 * @Author : mockingbird
 * @Date : 2020/9/6 14:02
 */
public class PornyVideoCrawler extends AbstractVideoCrawler {
    private static final String URL_PREFIX = "https://91porny.com";

    public PornyVideoCrawler(FileAccessManager fileAccessManager) {
        super(fileAccessManager);
    }

    @Override
    protected List<Video> getVideoBaseInfo(Document document) {
        Elements elements = document.select(".colVideoList").select(".video-elem");
        List<Video> videoList = new ArrayList<>();
        for (Element content : elements) {
            Video video = new Video();
            Element first = content.select("a").first();
            Element second = content.select("a").last();
            // id
            String style = first.select(".img").first().attr("style");
            int start = style.lastIndexOf("_");
            int end = style.lastIndexOf(".jpg");
            String id = "";
            if (start == -1) {
                start = style.lastIndexOf("/") + 1;
                id = "playvthumb_" + style.substring(start, end);
            } else {
                id = "playvthumb" + style.substring(start, end);
            }
            video.setId(id);
            // pageUrl
            String pageUrl = URL_PREFIX + second.attr("href");
            video.setPageUrl(pageUrl);
            // coverUrl
            // video.setCoverUrl(a.select("img").first().attr("src"));
            // duration
            Element duration = content.select(".layer").first();
            String durationStr = duration.text().trim();
            int minutes = Integer.parseInt(durationStr.split(":")[0]);
            int seconds = Integer.parseInt(durationStr.split(":")[1]);
            //Duration duration = Duration.ofSeconds(minutes * 60 + seconds);
            video.setDuration(minutes * 60 + seconds);
            // title
            video.setTitle(second.text());
//            // author
//            String ownText = content.ownText();
//            String text = ownText.substring(ownText.lastIndexOf(" 前 ") + 3);
//            video.setAuthor(text.split(" ")[0]);
//            // watchNum
//            video.setWatchNum(Integer.parseInt(text.split(" ")[1].trim()));
//            // storeNum
//            video.setStoreNum(Integer.parseInt(text.split(" ")[2].trim()));
            videoList.add(video);
        }

        return videoList;
    }

    @Override
    protected String getVideoDownloadUrl(Document document) {
        String downloadUrl = "";
        Element source = document.select("#video-play").select("source").first();
        if (source != null) {
            downloadUrl = source.attr("src");
            //logger.info("get video {} download url by page：{}", video.getTitle(), downloadUrl);
        }
        return downloadUrl;
    }
}