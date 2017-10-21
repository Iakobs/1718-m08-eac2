package ibanez.jacob.cat.xtec.ioc.lectorrss;

import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ibanez.jacob.cat.xtec.ioc.lectorrss.model.RssItem;

/**
 * @author <a href="mailto:jacobibanez@jacobibanez.com">Jacob Ibáñez Sánchez</a>.
 */
public class RssItemParser {

    //no namespaces used
    private static final String ns = null;

    //rss elements
    private static final String RSS_ROOT = "rss";
    private static final String RSS_CHANNEL = "channel";
    private static final String RSS_ITEM = "item";
    private static final String RSS_TITLE = "title";
    private static final String RSS_LINK = "link";
    private static final String RSS_DESCRIPTION = "description";
    private static final String RSS_PUB_DATE = "pubDate";
    private static final String RSS_AUTHOR = "author";
    private static final String RSS_THUMBNAIL = "media:thumbnail";
    private static final String RSS_KEYWORDS = "media:keywords";

    private Context mContext;

    public RssItemParser(Context context) {
        this.mContext = context;
    }

    /**
     * @param in
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    public List<RssItem> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            //Get parser
            XmlPullParser parser = Xml.newPullParser();
            //Set no namespaces
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            //Set input
            parser.setInput(in, null);
            //get first tag
            parser.nextTag();
            //get the item list
            return readRss(parser);
        } finally {
            in.close();
        }
    }

    private List<RssItem> readRss(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<RssItem> items = new ArrayList<>();

        //check if tag is required one
        parser.require(XmlPullParser.START_TAG, ns, RSS_ROOT);

        //while end tag is not reached
        while (parser.next() != XmlPullParser.END_TAG) {
            //ignore all tags which are not start
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            //get tag name
            String name = parser.getName();

            if (name.equals(RSS_CHANNEL)) {
                items = readChannel(parser);
            } else {
                skip(parser);
            }
        }

        return items;
    }

    private List<RssItem> readChannel(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<RssItem> items = new ArrayList<>();

        //check if tag is required one
        parser.require(XmlPullParser.START_TAG, ns, RSS_CHANNEL);

        //while end tag is not reached
        while (parser.next() != XmlPullParser.END_TAG) {
            //ignore all tags which are not start
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            //get tag name
            String name = parser.getName();

            if (name.equals(RSS_ITEM)) {
                items.add(readItem(parser));
            } else {
                skip(parser);
            }
        }

        return items;
    }

    private RssItem readItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        String title = null;
        String link = null;
        String author = null;
        String description = null;
        String pubDate = null;
        String categories = null;
        String thumbnail = null;
        String imageCachePath = null;

        //check if tag is required one
        parser.require(XmlPullParser.START_TAG, ns, RSS_ITEM);

        //while end tag is not reached
        while (parser.next() != XmlPullParser.END_TAG) {
            //ignore all tags which are not start
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            //get tag name
            String name = parser.getName();

            switch (name) {
                case RSS_TITLE:
                    title = readText(parser, RSS_TITLE);
                    break;
                case RSS_LINK:
                    link = readText(parser, RSS_LINK);
                    break;
                case RSS_AUTHOR:
                    author = readText(parser, RSS_AUTHOR);
                    break;
                case RSS_DESCRIPTION:
                    description = readText(parser, RSS_DESCRIPTION);
                    break;
                case RSS_PUB_DATE:
                    pubDate = readText(parser, RSS_PUB_DATE);
                    break;
                case RSS_KEYWORDS:
                    categories = readText(parser, RSS_KEYWORDS);
                    break;
                case RSS_THUMBNAIL:
                    thumbnail = readAttribute(parser, "url", RSS_THUMBNAIL);
                    imageCachePath = getCachePath(thumbnail);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        return new RssItem(title, link, author, description, pubDate, categories, thumbnail, imageCachePath);
    }

    /**
     *
     * @param imageUrl
     * @return
     */
    private String getCachePath(String imageUrl) {
        String imagePathInCache = null;

        if (imageUrl != null) {
            String imageName = imageUrl.substring(imageUrl.lastIndexOf("/"), imageUrl.length());
            imagePathInCache = mContext.getCacheDir().toString() + imageName;
        }

        return imagePathInCache;
    }

    private String readAttribute(XmlPullParser parser, String attribute, String requiredTag)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, requiredTag);

        String attributeValue = parser.getAttributeValue(null, attribute);
        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, ns, requiredTag);

        return attributeValue;
    }

    private String readText(XmlPullParser parser, String requiredTag) throws XmlPullParserException,
            IOException {
        parser.require(XmlPullParser.START_TAG, ns, requiredTag);

        String text = null;

        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
            parser.nextTag();
        }

        parser.require(XmlPullParser.END_TAG, ns, requiredTag);

        return text;
    }

    /**
     * This skips a tag and all of its nested tags
     *
     * @param parser The parser
     * @throws XmlPullParserException If an error occurs
     * @throws IOException            If an error occurs
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        //if not starting tag : ERROR
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;

        //check number of start and end tags are the same
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    //if a tag is closed, subtract 1
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    //if a tag is opened, sum 1
                    depth++;
                    break;
            }
        }
    }
}
