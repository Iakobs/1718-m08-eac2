package ibanez.jacob.cat.xtec.ioc.lectorrss.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represents a single entry from an RSS xml
 *
 * @author <a href="mailto:jacobibanez@jacobibanez.com">Jacob Ibáñez Sánchez</a>.
 */
public class RssItem implements Serializable {

    private static final long serialVersionUID = 5416260651526471310L;

    private String title;
    private String link;
    private String author;
    private String description;
    private String pubDate;
    private String categories;
    private String thumbnail;
    private String imagePathInCache;

    public RssItem(String title, String link, String author, String description,
                   String pubDate, String categories, String thumbnail, String imagePathInCache) {
        this.title = title;
        this.link = link;
        this.author = author;
        this.description = description;
        this.pubDate = pubDate;
        this.categories = categories;
        this.thumbnail = thumbnail;
        this.imagePathInCache = imagePathInCache;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getImagePathInCache() {
        return imagePathInCache;
    }

    public void setImagePathInCache(String imagePathInCache) {
        this.imagePathInCache = imagePathInCache;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RssItem item = (RssItem) o;
        return Objects.equals(title, item.title) &&
                Objects.equals(link, item.link) &&
                Objects.equals(author, item.author) &&
                Objects.equals(description, item.description) &&
                Objects.equals(pubDate, item.pubDate) &&
                Objects.equals(categories, item.categories) &&
                Objects.equals(thumbnail, item.thumbnail) &&
                Objects.equals(imagePathInCache, item.imagePathInCache);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, link, author, description, pubDate, categories, thumbnail, imagePathInCache);
    }

    @Override
    public String toString() {
        return "RssItem{" +
                "title='" + title + '\'' +
                ", link='" + link + '\'' +
                ", author='" + author + '\'' +
                ", description='" + description + '\'' +
                ", pubDate=" + pubDate +
                ", categories='" + categories + '\'' +
                ", thumbnail='" + thumbnail + '\'' +
                ", imagePathInCache='" + imagePathInCache + '\'' +
                '}';
    }
}
