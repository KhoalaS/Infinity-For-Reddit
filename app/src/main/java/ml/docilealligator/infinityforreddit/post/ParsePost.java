package ml.docilealligator.infinityforreddit.post;

import android.net.Uri;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;

import com.google.common.io.BaseEncoding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ml.docilealligator.infinityforreddit.postfilter.PostFilter;
import ml.docilealligator.infinityforreddit.utils.JSONUtils;
import ml.docilealligator.infinityforreddit.utils.Utils;

/**
 * Created by alex on 3/21/18.
 */

public class ParsePost {
    public static LinkedHashSet<Post> parsePostsSync(String response, int nPosts, PostFilter postFilter, List<String> readPostList) {
        LinkedHashSet<Post> newPosts = new LinkedHashSet<>();
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray allData = jsonResponse.getJSONObject(JSONUtils.DATA_KEY).getJSONObject("postFeed").getJSONObject("elements").getJSONArray("edges");

            //Posts listing
            int size;
            if (nPosts < 0 || nPosts > allData.length()) {
                size = allData.length();
            } else {
                size = nPosts;
            }

            HashSet<String> readPostHashSet = null;
            if (readPostList != null) {
                readPostHashSet = new HashSet<>(readPostList);
            }
            for (int i = 0; i < size; i++) {
                try {
                    JSONObject data = allData.getJSONObject(i).getJSONObject("node");
                    if (data.getString("__typename").equals("SubredditPost")) {
                        Post post = parseBasicData(data);
                        if (readPostHashSet != null && readPostHashSet.contains(post.getId())) {
                            post.markAsRead();
                        }
                        if (PostFilter.isPostAllowed(post, postFilter)) {
                            newPosts.add(post);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return newPosts;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getLastItem(String response) {
        try {
            JSONObject object = new JSONObject(response).getJSONObject("data").getJSONObject("postFeed").getJSONObject("elements").getJSONObject("pageInfo");
            if(object.isNull("endCursor")){
                return  null;
            } else{
                return object.getString("endCursor");
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void parsePost(Executor executor, Handler handler, String response, ParsePostListener parsePostListener) {
        PostFilter postFilter = new PostFilter();
        postFilter.allowNSFW = true;

        executor.execute(() -> {
            try {
                JSONArray allData = new JSONArray(response).getJSONObject(0).getJSONObject(JSONUtils.DATA_KEY).getJSONArray(JSONUtils.CHILDREN_KEY);
                if (allData.length() == 0) {
                    handler.post(parsePostListener::onParsePostFail);
                    return;
                }
                JSONObject data = allData.getJSONObject(0).getJSONObject(JSONUtils.DATA_KEY);
                Post post = parseBasicData(data);
                handler.post(() -> parsePostListener.onParsePostSuccess(post));
            } catch (JSONException e) {
                e.printStackTrace();
                handler.post(parsePostListener::onParsePostFail);
            }
        });
    }

    public static void parseRandomPost(Executor executor, Handler handler, String response, boolean isNSFW,
                                       ParseRandomPostListener parseRandomPostListener) {
        executor.execute(() -> {
            try {
                JSONArray postsArray = new JSONObject(response).getJSONObject(JSONUtils.DATA_KEY).getJSONArray(JSONUtils.CHILDREN_KEY);
                if (postsArray.length() == 0) {
                    handler.post(parseRandomPostListener::onParseRandomPostFailed);
                } else {
                    JSONObject post = postsArray.getJSONObject(0).getJSONObject(JSONUtils.DATA_KEY);
                    String subredditName = post.getString(JSONUtils.SUBREDDIT_KEY);
                    String postId;
                    if (isNSFW) {
                        postId = post.getString(JSONUtils.ID_KEY);
                    } else {
                        postId = post.getString(JSONUtils.LINK_ID_KEY).substring("t3_".length());
                    }
                    handler.post(() -> parseRandomPostListener.onParseRandomPostSuccess(postId, subredditName));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                handler.post(parseRandomPostListener::onParseRandomPostFailed);
            }
        });
    }

    public static long getUnixTime(String timestamp) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ";
        String timestampAsString = "2023-07-05T12:47:50.355000+0000";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        try {
            return formatter.parse(timestampAsString).getTime();
        } catch (ParseException e) {
            return new Date().getTime();
        }
    }

    public static Post parseBasicData(JSONObject data) throws JSONException {
        String fullName = data.getString(JSONUtils.ID_KEY);
        String id = fullName.replace("t3_", "");
        String[] permaLinkSplit = data.getString("permalink").split("/");
        String subredditName = permaLinkSplit[2];
        String subredditNamePrefixed = permaLinkSplit[1] + "/" + permaLinkSplit[2];
        String author = data.getJSONObject("authorInfo").getString("name");
        StringBuilder authorFlairHTMLBuilder = new StringBuilder();
        if (!data.isNull("authorFlair")) {
            JSONObject flair = data.getJSONObject("authorFlair");
            //JSONArray flairArray = data.getJSONArray(JSONUtils.AUTHOR_FLAIR_RICHTEXT_KEY);
            JSONArray flairArray = new JSONArray(flair.getString("richtext"));

            for (int i = 0; i < flairArray.length(); i++) {
                JSONObject flairObject = flairArray.getJSONObject(i);
                String e = flairObject.getString(JSONUtils.E_KEY);
                if (e.equals("text")) {
                    authorFlairHTMLBuilder.append(flairObject.getString(JSONUtils.T_KEY));
                } else if (e.equals("emoji")) {
                    authorFlairHTMLBuilder.append("<img src=\"").append(Html.escapeHtml(flairObject.getString(JSONUtils.U_KEY))).append("\">");
                }
            }
        }
        String authorFlair = data.isNull("authorFlair") ? "" : data.getJSONObject("authorFlair").getString("text");
        String distinguished = data.getString("distinguishedAs");
        String suggestedSort = data.has("suggestedCommentSort") ? data.getString("suggestedCommentSort") : null;
        long postTime = getUnixTime(data.getString("createdAt"));
        String title = data.getString(JSONUtils.TITLE_KEY);
        int score = (int) data.getDouble(JSONUtils.SCORE_KEY);
        int voteType;
        int nComments = data.getInt("commentCount");
        int upvoteRatio = (int) (data.getDouble("upvoteRatio") * 100);
        boolean hidden = data.getBoolean("isHidden");
        boolean spoiler = data.getBoolean("isSpoiler");
        boolean nsfw = data.getBoolean("isNsfw");
        boolean stickied = data.getBoolean("isStickied");
        boolean archived = data.getBoolean("isArchived");
        boolean locked = data.getBoolean("isLocked");
        boolean saved = data.getBoolean("isSaved");

        // TODO I did not find these in gql
        boolean deleted = !data.isNull(JSONUtils.REMOVED_BY_CATEGORY_KEY) && data.getString(JSONUtils.REMOVED_BY_CATEGORY_KEY).equals("deleted");
        boolean removed = !data.isNull(JSONUtils.REMOVED_BY_CATEGORY_KEY) && data.getString(JSONUtils.REMOVED_BY_CATEGORY_KEY).equals("moderator");

        StringBuilder postFlairHTMLBuilder = new StringBuilder();
        String flair = "";
        if (!data.isNull("flair")) {
            JSONObject flairs = data.getJSONObject("flair");
            if(!flairs.isNull("richtext")){
                JSONArray flairArray = new JSONArray(flairs.getString("richtext"));
                for (int i = 0; i < flairArray.length(); i++) {
                    JSONObject flairObject = flairArray.getJSONObject(i);
                    String e = flairObject.getString(JSONUtils.E_KEY);
                    if (e.equals("text")) {
                        postFlairHTMLBuilder.append(Html.escapeHtml(flairObject.getString(JSONUtils.T_KEY)));
                    } else if (e.equals("emoji")) {
                        postFlairHTMLBuilder.append("<img src=\"").append(Html.escapeHtml(flairObject.getString(JSONUtils.U_KEY))).append("\">");
                    }
                }
                flair = postFlairHTMLBuilder.toString();
            }
        }

        if (flair.equals("") && data.has("flair") && !data.isNull("flair") && !data.getJSONObject("flair").isNull("richtext")) {
            flair = data.getJSONObject("flair").getString("text");
        }
        
        StringBuilder awardingsBuilder = new StringBuilder();
        JSONArray awardingsArray = data.getJSONArray("awardings");
        int nAwards = 0;
        for (int i = 0; i < awardingsArray.length(); i++) {
            JSONObject awardData = awardingsArray.getJSONObject(i);
            JSONObject award = awardData.getJSONObject("award");
            int count = awardData.getInt("total");
            nAwards += count;

            String iconUrl = award.getJSONObject("icon_32").getString(JSONUtils.URL_KEY);
            awardingsBuilder.append("<img src=\"").append(Html.escapeHtml(iconUrl)).append("\"> ").append("x").append(count).append(" ");
        }

        if (data.isNull(JSONUtils.LIKES_KEY)) {
            voteType = 0;
        } else {
            voteType = data.getBoolean(JSONUtils.LIKES_KEY) ? 1 : -1;
            score -= voteType;
        }

        String permalink = Html.fromHtml(data.getString(JSONUtils.PERMALINK_KEY)).toString();

        ArrayList<Post.Preview> previews = new ArrayList<>();
        if (!data.isNull("media")) {
            JSONObject images = data.getJSONObject("media").getJSONObject("still");
            String previewUrl = images.getJSONObject(JSONUtils.SOURCE_KEY).getString(JSONUtils.URL_KEY);
            int previewWidth = images.getJSONObject(JSONUtils.SOURCE_KEY).getJSONObject("dimensions").getInt(JSONUtils.WIDTH_KEY);
            int previewHeight = images.getJSONObject(JSONUtils.SOURCE_KEY).getJSONObject("dimensions").getInt(JSONUtils.HEIGHT_KEY);
            previews.add(new Post.Preview(previewUrl, previewWidth, previewHeight, "", ""));

            String[] resolutions = {"small", "medium", "large", "xlarge", "xxlarge", "xxxlarge"};
            for (String res : resolutions) {
                if(images.isNull(res)){
                    continue;
                }
                JSONObject thumbnailPreview = images.getJSONObject(res);
                String thumbnailPreviewUrl = thumbnailPreview.getString(JSONUtils.URL_KEY);
                int thumbnailPreviewWidth = thumbnailPreview.getJSONObject("dimensions").getInt(JSONUtils.WIDTH_KEY);
                int thumbnailPreviewHeight = thumbnailPreview.getJSONObject("dimensions").getInt(JSONUtils.HEIGHT_KEY);

                previews.add(new Post.Preview(thumbnailPreviewUrl, thumbnailPreviewWidth, thumbnailPreviewHeight, "", ""));
            }
        }
        if (!data.isNull("crosspostRoot")) {
            //Cross post
            //data.getJSONArray(JSONUtils.CROSSPOST_PARENT_LIST).getJSONObject(0) out of bounds????????????
            data = data.getJSONObject("crosspostRoot").getJSONObject("post");
            Post crosspostParent = parseBasicData(data);
            Post post = parseData(data, permalink, id, fullName, subredditName, subredditNamePrefixed,
                    author, authorFlair, authorFlairHTMLBuilder.toString(),
                    postTime, title, previews,
                    score, voteType, nComments, upvoteRatio, flair, awardingsBuilder.toString(), nAwards, hidden,
                    spoiler, nsfw, stickied, archived, locked, saved, deleted, removed, true,
                    distinguished, suggestedSort);
            post.setCrosspostParentId(crosspostParent.getId());
            return post;
        } else {
            return parseData(data, permalink, id, fullName, subredditName, subredditNamePrefixed,
                    author, authorFlair, authorFlairHTMLBuilder.toString(),
                    postTime, title, previews,
                    score, voteType, nComments, upvoteRatio, flair, awardingsBuilder.toString(), nAwards, hidden,
                    spoiler, nsfw, stickied, archived, locked, saved, deleted, removed, false,
                    distinguished, suggestedSort);
        }
    }

    // TODO
    private static Post parseData(JSONObject data, String permalink, String id, String fullName,
                                  String subredditName, String subredditNamePrefixed, String author,
                                  String authorFlair, String authorFlairHTML,
                                  long postTimeMillis, String title, ArrayList<Post.Preview> previews,
                                  int score, int voteType, int nComments, int upvoteRatio, String flair,
                                  String awards, int nAwards, boolean hidden, boolean spoiler,
                                  boolean nsfw, boolean stickied, boolean archived, boolean locked,
                                  boolean saved, boolean deleted, boolean removed, boolean isCrosspost,
                                  String distinguished, String suggestedSort) throws JSONException {
        Post post;

        boolean isVideo = data.getString("postHint").equals("HOSTED_VIDEO");
        String url = Html.fromHtml(data.getString(JSONUtils.URL_KEY)).toString();
        Uri uri = Uri.parse(url);
        String path = uri.getPath();

        if(data.getBoolean("isSelfPost")){
            //Text post
            int postType = Post.TEXT_TYPE;
            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                    authorFlair, authorFlairHTML, postTimeMillis, title, permalink, score, postType,
                    voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden, spoiler, nsfw,
                    stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);
            if(!previews.isEmpty()){
                post.setPreviews(previews);
            }
        } else if(data.getString("postHint").equals("IMAGE")){
            //Image post

            int postType = Post.IMAGE_TYPE;

            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                    authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                    postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                    spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);

            if (previews.isEmpty()) {
                previews.add(new Post.Preview(url, 0, 0, "", ""));
            }
            post.setPreviews(previews);
        } else if (data.getString("postHint").equals("HOSTED_VIDEO") || data.get("postHint").equals("RICH_VIDEO")){
            JSONObject redditVideoObject = data.getJSONObject(JSONUtils.MEDIA_KEY).getJSONObject("streaming");
            if(data.getJSONObject(JSONUtils.MEDIA_KEY).isNull("streaming")){
                int x = 10;
            }
            JSONObject download = data.getJSONObject(JSONUtils.MEDIA_KEY).getJSONObject("download");
            int postType = Post.VIDEO_TYPE;
            String videoUrl = Html.fromHtml(redditVideoObject.getString("hlsUrl")).toString();
            String videoDownloadUrl = download.getString("url");

            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author, authorFlair,
                    authorFlairHTML, postTimeMillis, title, permalink, score, postType, voteType,
                    nComments, upvoteRatio, flair, awards, nAwards, hidden, spoiler, nsfw, stickied,
                    archived, locked, saved, isCrosspost, distinguished, suggestedSort);

            post.setPreviews(previews);
            post.setVideoUrl(videoUrl);
            post.setVideoDownloadUrl(videoDownloadUrl);
        } else if (data.getString("postHint").equals("LINK")){
            //No preview link post
            int postType = Post.LINK_TYPE;
            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                    authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                    postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                    spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);
            if (data.getBoolean("isSelfPost")) {
                post.setSelfText(Utils.modifyMarkdown(Utils.trimTrailingWhitespace(data.getJSONObject("content").getString("markdown"))));
            } else {
                post.setSelfText("");
            }
            if(!previews.isEmpty()){
                post.setPreviews(previews);
            }
            setText(post, data);
        }else if (!data.isNull("gallerys")) {
            int postType = Post.LINK_TYPE;
            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                    authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                    postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                    spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);

            JSONArray galleryIdsArray = data.getJSONObject(JSONUtils.GALLERY_DATA_KEY).getJSONArray(JSONUtils.ITEMS_KEY);
            JSONObject galleryObject = data.getJSONObject(JSONUtils.MEDIA_METADATA_KEY);
            ArrayList<Post.Gallery> gallery = new ArrayList<>();
            for (int i = 0; i < galleryIdsArray.length(); i++) {
                String galleryId = galleryIdsArray.getJSONObject(i).getString(JSONUtils.MEDIA_ID_KEY);
                JSONObject singleGalleryObject = galleryObject.getJSONObject(galleryId);
                String mimeType = singleGalleryObject.getString(JSONUtils.M_KEY);
                String galleryItemUrl;
                if (mimeType.contains("jpg") || mimeType.contains("png")) {
                    galleryItemUrl = singleGalleryObject.getJSONObject(JSONUtils.S_KEY).getString(JSONUtils.U_KEY);
                } else {
                    JSONObject sourceObject = singleGalleryObject.getJSONObject(JSONUtils.S_KEY);
                    if (mimeType.contains("gif")) {
                        galleryItemUrl = sourceObject.getString(JSONUtils.GIF_KEY);
                    } else {
                        galleryItemUrl = sourceObject.getString(JSONUtils.MP4_KEY);
                    }
                }

                JSONObject galleryItem = galleryIdsArray.getJSONObject(i);
                String galleryItemCaption = "";
                String galleryItemCaptionUrl = "";
                if (galleryItem.has(JSONUtils.CAPTION_KEY)) {
                    galleryItemCaption = galleryItem.getString(JSONUtils.CAPTION_KEY).trim();
                }

                if (galleryItem.has(JSONUtils.CAPTION_URL_KEY)) {
                    galleryItemCaptionUrl = galleryItem.getString(JSONUtils.CAPTION_URL_KEY).trim();
                }

                if (previews.isEmpty() && (mimeType.contains("jpg") || mimeType.contains("png"))) {
                    previews.add(new Post.Preview(galleryItemUrl, singleGalleryObject.getJSONObject(JSONUtils.S_KEY).getInt(JSONUtils.X_KEY),
                            singleGalleryObject.getJSONObject(JSONUtils.S_KEY).getInt(JSONUtils.Y_KEY), galleryItemCaption, galleryItemCaptionUrl));
                }

                Post.Gallery postGalleryItem = new Post.Gallery(mimeType, galleryItemUrl, "", subredditName + "-" + galleryId + "." + mimeType.substring(mimeType.lastIndexOf("/") + 1), galleryItemCaption, galleryItemCaptionUrl);

                // For issue #558
                // Construct a fallback image url
                if (!TextUtils.isEmpty(galleryItemUrl) && !TextUtils.isEmpty(mimeType) && (mimeType.contains("jpg") || mimeType.contains("png"))) {
                    postGalleryItem.setFallbackUrl("https://i.redd.it/" + galleryId + "." +  mimeType.substring(mimeType.lastIndexOf("/") + 1));
                    postGalleryItem.setHasFallback(true);
                }

                gallery.add(postGalleryItem);
            }

            if (!gallery.isEmpty()) {
                post.setPostType(Post.GALLERY_TYPE);
                post.setGallery(gallery);
                post.setPreviews(previews);
            }

        }else{
            //Text post
            int postType = Post.TEXT_TYPE;
            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                    authorFlair, authorFlairHTML, postTimeMillis, title, permalink, score, postType,
                    voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden, spoiler, nsfw,
                    stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);
            if(!previews.isEmpty()){
                post.setPreviews(previews);
            }
        }
        return post;
    }

    public static void setText(Post post, JSONObject data){
        try {
                if (data.isNull("isSelfPost")) {
                    post.setSelfText("");
                } else {
                    JSONObject content = data.getJSONObject("content");
                    String selfText = Utils.modifyMarkdown(Utils.trimTrailingWhitespace(content.getString("markdown")));
                    post.setSelfText(selfText);
                    if (data.isNull(content.getString("html"))) {
                        post.setSelfTextPlainTrimmed("");
                    } else {
                        String selfTextPlain = Utils.trimTrailingWhitespace(
                                Html.fromHtml(data.getString(content.getString("html")))).toString();
                        post.setSelfTextPlain(selfTextPlain);
                        if (selfTextPlain.length() > 250) {
                            selfTextPlain = selfTextPlain.substring(0, 250);
                        }
                        if (!selfText.equals("")) {
                            Pattern p = Pattern.compile(">!.+!<");
                            Matcher m = p.matcher(selfText.substring(0, Math.min(selfText.length(), 400)));
                            if (m.find()) {
                                post.setSelfTextPlainTrimmed("");
                            } else {
                                post.setSelfTextPlainTrimmed(selfTextPlain);
                            }
                        } else {
                            post.setSelfTextPlainTrimmed(selfTextPlain);
                        }
                    }
                }

        }catch (JSONException e){

        }
    }

    public interface ParsePostsListingListener {
        void onParsePostsListingSuccess(LinkedHashSet<Post> newPostData, String lastItem);
        void onParsePostsListingFail();
    }

    public interface ParsePostListener {
        void onParsePostSuccess(Post post);
        void onParsePostFail();
    }

    public interface ParseRandomPostListener {
        void onParseRandomPostSuccess(String postId, String subredditName);
        void onParseRandomPostFailed();
    }
}
