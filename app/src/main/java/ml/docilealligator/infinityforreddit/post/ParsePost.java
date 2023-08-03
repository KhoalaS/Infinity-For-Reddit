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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
            JSONArray allData = jsonResponse.getJSONObject(JSONUtils.DATA_KEY).getJSONArray(JSONUtils.CHILDREN_KEY);

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
                    if (allData.getJSONObject(i).getString(JSONUtils.KIND_KEY).equals("t3")) {
                        JSONObject data = allData.getJSONObject(i).getJSONObject(JSONUtils.DATA_KEY);
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
    public static LinkedHashSet<Post> parsePostsSyncGQL(String response, int nPosts, PostFilter postFilter, List<String> readPostList) {
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
                    String typename = data.getString("__typename");
                    if (typename.equals("SubredditPost") || typename.equals("ProfilePost")) {
                        Post post = parseBasicDataGQL(data);
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
            JSONObject object = new JSONObject(response).getJSONObject(JSONUtils.DATA_KEY);
            return object.isNull(JSONUtils.AFTER_KEY) ? null : object.getString(JSONUtils.AFTER_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getLastItemGQL(String response) {
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
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        try {
            return formatter.parse(timestamp).getTime();
        } catch (ParseException e) {
            return new Date().getTime();
        }
    }

    public static Post parseBasicData(JSONObject data) throws JSONException {
        String id = data.getString(JSONUtils.ID_KEY);
        String fullName = data.getString(JSONUtils.NAME_KEY);
        String subredditName = data.getString(JSONUtils.SUBREDDIT_KEY);
        String subredditNamePrefixed = data.getString(JSONUtils.SUBREDDIT_NAME_PREFIX_KEY);
        String author = data.getString(JSONUtils.AUTHOR_KEY);
        StringBuilder authorFlairHTMLBuilder = new StringBuilder();
        if (data.has(JSONUtils.AUTHOR_FLAIR_RICHTEXT_KEY)) {
            JSONArray flairArray = data.getJSONArray(JSONUtils.AUTHOR_FLAIR_RICHTEXT_KEY);
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
        String authorFlair = data.isNull(JSONUtils.AUTHOR_FLAIR_TEXT_KEY) ? "" : data.getString(JSONUtils.AUTHOR_FLAIR_TEXT_KEY);
        String distinguished = data.getString(JSONUtils.DISTINGUISHED_KEY);
        String suggestedSort = data.has(JSONUtils.SUGGESTED_SORT_KEY) ? data.getString(JSONUtils.SUGGESTED_SORT_KEY) : null;
        long postTime = data.getLong(JSONUtils.CREATED_UTC_KEY) * 1000;
        String title = data.getString(JSONUtils.TITLE_KEY);
        int score = data.getInt(JSONUtils.SCORE_KEY);
        int voteType;
        int nComments = data.getInt(JSONUtils.NUM_COMMENTS_KEY);
        int upvoteRatio = (int) (data.getDouble(JSONUtils.UPVOTE_RATIO_KEY) * 100);
        boolean hidden = data.getBoolean(JSONUtils.HIDDEN_KEY);
        boolean spoiler = data.getBoolean(JSONUtils.SPOILER_KEY);
        boolean nsfw = data.getBoolean(JSONUtils.NSFW_KEY);
        boolean stickied = data.getBoolean(JSONUtils.STICKIED_KEY);
        boolean archived = data.getBoolean(JSONUtils.ARCHIVED_KEY);
        boolean locked = data.getBoolean(JSONUtils.LOCKED_KEY);
        boolean saved = data.getBoolean(JSONUtils.SAVED_KEY);
        boolean deleted = !data.isNull(JSONUtils.REMOVED_BY_CATEGORY_KEY) && data.getString(JSONUtils.REMOVED_BY_CATEGORY_KEY).equals("deleted");
        boolean removed = !data.isNull(JSONUtils.REMOVED_BY_CATEGORY_KEY) && data.getString(JSONUtils.REMOVED_BY_CATEGORY_KEY).equals("moderator");
        StringBuilder postFlairHTMLBuilder = new StringBuilder();
        String flair = "";
        if (data.has(JSONUtils.LINK_FLAIR_RICHTEXT_KEY)) {
            JSONArray flairArray = data.getJSONArray(JSONUtils.LINK_FLAIR_RICHTEXT_KEY);
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

        if (flair.equals("") && data.has(JSONUtils.LINK_FLAIR_TEXT_KEY) && !data.isNull(JSONUtils.LINK_FLAIR_TEXT_KEY)) {
            flair = data.getString(JSONUtils.LINK_FLAIR_TEXT_KEY);
        }

        StringBuilder awardingsBuilder = new StringBuilder();
        JSONArray awardingsArray = data.getJSONArray(JSONUtils.ALL_AWARDINGS_KEY);
        int nAwards = 0;
        for (int i = 0; i < awardingsArray.length(); i++) {
            JSONObject award = awardingsArray.getJSONObject(i);
            int count = award.getInt(JSONUtils.COUNT_KEY);
            nAwards += count;
            JSONArray icons = award.getJSONArray(JSONUtils.RESIZED_ICONS_KEY);
            if (icons.length() > 4) {
                String iconUrl = icons.getJSONObject(3).getString(JSONUtils.URL_KEY);
                awardingsBuilder.append("<img src=\"").append(Html.escapeHtml(iconUrl)).append("\"> ").append("x").append(count).append(" ");
            } else if (icons.length() > 0) {
                String iconUrl = icons.getJSONObject(icons.length() - 1).getString(JSONUtils.URL_KEY);
                awardingsBuilder.append("<img src=\"").append(Html.escapeHtml(iconUrl)).append("\"> ").append("x").append(count).append(" ");
            }
        }

        if (data.isNull(JSONUtils.LIKES_KEY)) {
            voteType = 0;
        } else {
            voteType = data.getBoolean(JSONUtils.LIKES_KEY) ? 1 : -1;
            score -= voteType;
        }

        String permalink = Html.fromHtml(data.getString(JSONUtils.PERMALINK_KEY)).toString();

        ArrayList<Post.Preview> previews = new ArrayList<>();
        if (data.has(JSONUtils.PREVIEW_KEY)) {
            JSONObject images = data.getJSONObject(JSONUtils.PREVIEW_KEY).getJSONArray(JSONUtils.IMAGES_KEY).getJSONObject(0);
            String previewUrl = images.getJSONObject(JSONUtils.SOURCE_KEY).getString(JSONUtils.URL_KEY);
            int previewWidth = images.getJSONObject(JSONUtils.SOURCE_KEY).getInt(JSONUtils.WIDTH_KEY);
            int previewHeight = images.getJSONObject(JSONUtils.SOURCE_KEY).getInt(JSONUtils.HEIGHT_KEY);
            previews.add(new Post.Preview(previewUrl, previewWidth, previewHeight, "", ""));

            JSONArray thumbnailPreviews = images.getJSONArray(JSONUtils.RESOLUTIONS_KEY);
            for (int i = 0; i < thumbnailPreviews.length(); i++) {
                JSONObject thumbnailPreview = thumbnailPreviews.getJSONObject(i);
                String thumbnailPreviewUrl = thumbnailPreview.getString(JSONUtils.URL_KEY);
                int thumbnailPreviewWidth = thumbnailPreview.getInt(JSONUtils.WIDTH_KEY);
                int thumbnailPreviewHeight = thumbnailPreview.getInt(JSONUtils.HEIGHT_KEY);

                previews.add(new Post.Preview(thumbnailPreviewUrl, thumbnailPreviewWidth, thumbnailPreviewHeight, "", ""));
            }
        }
        if (data.has(JSONUtils.CROSSPOST_PARENT_LIST)) {
            //Cross post
            //data.getJSONArray(JSONUtils.CROSSPOST_PARENT_LIST).getJSONObject(0) out of bounds????????????
            data = data.getJSONArray(JSONUtils.CROSSPOST_PARENT_LIST).getJSONObject(0);
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
    public static Post parseBasicDataGQL(JSONObject data) throws JSONException {
        String fullName = data.getString(JSONUtils.ID_KEY);
        String id = fullName.replace("t3_", "");
        String[] permaLinkSplit = data.getString("permalink").split("/");
        String subredditName = permaLinkSplit[2];
        String subredditNamePrefixed = permaLinkSplit[1] + "/" + permaLinkSplit[2];
        String author = data.getJSONObject("authorInfo").getString("name");

        StringBuilder authorFlairHTMLBuilder = new StringBuilder();
        if (!data.isNull("authorFlair") && !data.getJSONObject("authorFlair").isNull("richtext")) {
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
        int score;
        if(data.isNull(JSONUtils.SCORE_KEY)){
            score = 0;
        }else{
             score = (int) data.getDouble(JSONUtils.SCORE_KEY);
        }
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
        if (!data.isNull("flair") && !data.getJSONObject("flair").isNull("richtext")) {
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

        if (flair.equals("") && !data.isNull("flair")) {
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
        voteType = 0;
        switch(data.getString("voteState")){
            case "NONE":
                voteType = 0;
                break;
            case "UP":
                voteType = 1;
                break;
            case "DOWN":
                voteType = -1;
                break;
        }

        String permalink = Html.fromHtml(data.getString(JSONUtils.PERMALINK_KEY)).toString();

        ArrayList<Post.Preview> previews = new ArrayList<>();
        if (!data.isNull("media")) {
            if(!data.getJSONObject(JSONUtils.MEDIA_KEY).isNull("still")){
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
            }else if (!data.getJSONObject(JSONUtils.MEDIA_KEY).isNull("thumbnail")){
                JSONObject thumbnail = data.getJSONObject(JSONUtils.MEDIA_KEY).getJSONObject("thumbnail");
                String thumbnailPreviewUrl = thumbnail.getString("url");
                int thumbnailPreviewWidth = thumbnail.getJSONObject("dimensions").getInt("width");
                int thumbnailPreviewHeight = thumbnail.getJSONObject("dimensions").getInt("height");
                previews.add(new Post.Preview(thumbnailPreviewUrl, thumbnailPreviewWidth, thumbnailPreviewHeight, "", ""));
            }

        }
        if (!data.isNull("crosspostRoot")) {
            //Cross post
            //data.getJSONArray(JSONUtils.CROSSPOST_PARENT_LIST).getJSONObject(0) out of bounds????????????
            data = data.getJSONObject("crosspostRoot").getJSONObject("post");
            Post crosspostParent = parseBasicDataGQL(data);
            Post post = parseDataGQL(data, permalink, id, fullName, subredditName, subredditNamePrefixed,
                    author, authorFlair, authorFlairHTMLBuilder.toString(),
                    postTime, title, previews,
                    score, voteType, nComments, upvoteRatio, flair, awardingsBuilder.toString(), nAwards, hidden,
                    spoiler, nsfw, stickied, archived, locked, saved, deleted, removed, true,
                    distinguished, suggestedSort);
            post.setCrosspostParentId(crosspostParent.getId());
            return post;
        } else {
            return parseDataGQL(data, permalink, id, fullName, subredditName, subredditNamePrefixed,
                    author, authorFlair, authorFlairHTMLBuilder.toString(),
                    postTime, title, previews,
                    score, voteType, nComments, upvoteRatio, flair, awardingsBuilder.toString(), nAwards, hidden,
                    spoiler, nsfw, stickied, archived, locked, saved, deleted, removed, false,
                    distinguished, suggestedSort);
        }
    }

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

        boolean isVideo = data.getBoolean(JSONUtils.IS_VIDEO_KEY);
        String url = Html.fromHtml(data.getString(JSONUtils.URL_KEY)).toString();
        Uri uri = Uri.parse(url);
        String path = uri.getPath();

        if (!data.has(JSONUtils.PREVIEW_KEY) && previews.isEmpty()) {
            if (url.contains(permalink)) {
                //Text post
                int postType = Post.TEXT_TYPE;
                post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                        authorFlair, authorFlairHTML, postTimeMillis, title, permalink, score, postType,
                        voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden, spoiler, nsfw,
                        stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);
            } else {
                if (path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".jpeg")) {
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
                } else {
                    if (isVideo) {
                        //No preview video post
                        JSONObject redditVideoObject = data.getJSONObject(JSONUtils.MEDIA_KEY).getJSONObject(JSONUtils.REDDIT_VIDEO_KEY);
                        int postType = Post.VIDEO_TYPE;
                        String videoUrl = Html.fromHtml(redditVideoObject.getString(JSONUtils.HLS_URL_KEY)).toString();
                        String videoDownloadUrl = redditVideoObject.getString(JSONUtils.FALLBACK_URL_KEY);

                        post = new Post(id, fullName, subredditName, subredditNamePrefixed, author, authorFlair,
                                authorFlairHTML, postTimeMillis, title, permalink, score, postType, voteType,
                                nComments, upvoteRatio, flair, awards, nAwards, hidden, spoiler, nsfw, stickied,
                                archived, locked, saved, isCrosspost, distinguished, suggestedSort);

                        post.setVideoUrl(videoUrl);
                        post.setVideoDownloadUrl(videoDownloadUrl);
                    } else {
                        //No preview link post
                        int postType = Post.NO_PREVIEW_LINK_TYPE;
                        post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                                authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                                postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                                spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);
                        if (data.isNull(JSONUtils.SELFTEXT_KEY)) {
                            post.setSelfText("");
                        } else {
                            post.setSelfText(Utils.modifyMarkdown(Utils.trimTrailingWhitespace(data.getString(JSONUtils.SELFTEXT_KEY))));
                        }

                        String authority = uri.getAuthority();

                        if (authority != null) {
                            if (authority.contains("gfycat.com")) {
                                String gfycatId = url.substring(url.lastIndexOf("/") + 1).toLowerCase();
                                post.setPostType(Post.VIDEO_TYPE);
                                post.setIsGfycat(true);
                                post.setVideoUrl(url);
                                post.setGfycatId(gfycatId);
                            } else if (authority.contains("redgifs.com")) {
                                String gfycatId = url.substring(url.lastIndexOf("/") + 1).toLowerCase();
                                post.setPostType(Post.VIDEO_TYPE);
                                post.setIsRedgifs(true);
                                post.setVideoUrl(url);
                                post.setGfycatId(gfycatId);
                            } else if (authority.equals("streamable.com")) {
                                String shortCode = url.substring(url.lastIndexOf("/") + 1);
                                post.setPostType(Post.VIDEO_TYPE);
                                post.setIsStreamable(true);
                                post.setVideoUrl(url);
                                post.setStreamableShortCode(shortCode);
                            }
                        }
                    }
                }
            }
        } else {
            if (previews.isEmpty()) {
                if (data.has(JSONUtils.PREVIEW_KEY)) {
                    JSONObject images = data.getJSONObject(JSONUtils.PREVIEW_KEY).getJSONArray(JSONUtils.IMAGES_KEY).getJSONObject(0);
                    String previewUrl = images.getJSONObject(JSONUtils.SOURCE_KEY).getString(JSONUtils.URL_KEY);
                    int previewWidth = images.getJSONObject(JSONUtils.SOURCE_KEY).getInt(JSONUtils.WIDTH_KEY);
                    int previewHeight = images.getJSONObject(JSONUtils.SOURCE_KEY).getInt(JSONUtils.HEIGHT_KEY);
                    previews.add(new Post.Preview(previewUrl, previewWidth, previewHeight, "", ""));

                    JSONArray thumbnailPreviews = images.getJSONArray(JSONUtils.RESOLUTIONS_KEY);
                    for (int i = 0; i < thumbnailPreviews.length(); i++) {
                        JSONObject thumbnailPreview = images.getJSONArray(JSONUtils.RESOLUTIONS_KEY).getJSONObject(i);
                        String thumbnailPreviewUrl = thumbnailPreview.getString(JSONUtils.URL_KEY);
                        int thumbnailPreviewWidth = thumbnailPreview.getInt(JSONUtils.WIDTH_KEY);
                        int thumbnailPreviewHeight = thumbnailPreview.getInt(JSONUtils.HEIGHT_KEY);

                        previews.add(new Post.Preview(thumbnailPreviewUrl, thumbnailPreviewWidth, thumbnailPreviewHeight, "", ""));
                    }
                }
            }

            if (isVideo) {
                //Video post
                JSONObject redditVideoObject = data.getJSONObject(JSONUtils.MEDIA_KEY).getJSONObject(JSONUtils.REDDIT_VIDEO_KEY);
                int postType = Post.VIDEO_TYPE;
                String videoUrl = Html.fromHtml(redditVideoObject.getString(JSONUtils.HLS_URL_KEY)).toString();
                String videoDownloadUrl = redditVideoObject.getString(JSONUtils.FALLBACK_URL_KEY);

                post = new Post(id, fullName, subredditName, subredditNamePrefixed, author, authorFlair,
                        authorFlairHTML, postTimeMillis, title, permalink, score, postType, voteType,
                        nComments, upvoteRatio, flair, awards, nAwards, hidden, spoiler, nsfw, stickied,
                        archived, locked, saved, isCrosspost, distinguished, suggestedSort);

                post.setPreviews(previews);
                post.setVideoUrl(videoUrl);
                post.setVideoDownloadUrl(videoDownloadUrl);
            } else if (data.has(JSONUtils.PREVIEW_KEY)) {
                if (data.getJSONObject(JSONUtils.PREVIEW_KEY).has(JSONUtils.REDDIT_VIDEO_PREVIEW_KEY)) {
                    int postType = Post.VIDEO_TYPE;
                    String authority = uri.getAuthority();
                    // The hls stream inside REDDIT_VIDEO_PREVIEW_KEY can sometimes lack an audio track
                    if (authority.contains("imgur.com") && (path.endsWith(".gifv") || path.endsWith(".mp4"))) {
                        if (path.endsWith(".gifv")) {
                            url = url.substring(0, url.length() - 5) + ".mp4";
                        }

                        post = new Post(id, fullName, subredditName, subredditNamePrefixed, author, authorFlair,
                                authorFlairHTML, postTimeMillis, title, permalink, score, postType, voteType,
                                nComments, upvoteRatio, flair, awards, nAwards, hidden, spoiler, nsfw, stickied,
                                archived, locked, saved, isCrosspost, distinguished, suggestedSort);
                        post.setPreviews(previews);
                        post.setVideoUrl(url);
                        post.setVideoDownloadUrl(url);
                        post.setIsImgur(true);
                    } else {
                        //Gif video post (HLS)

                        String videoUrl = Html.fromHtml(data.getJSONObject(JSONUtils.PREVIEW_KEY)
                                .getJSONObject(JSONUtils.REDDIT_VIDEO_PREVIEW_KEY).getString(JSONUtils.HLS_URL_KEY)).toString();
                        String videoDownloadUrl = data.getJSONObject(JSONUtils.PREVIEW_KEY)
                                .getJSONObject(JSONUtils.REDDIT_VIDEO_PREVIEW_KEY).getString(JSONUtils.FALLBACK_URL_KEY);

                        post = new Post(id, fullName, subredditName, subredditNamePrefixed, author, authorFlair,
                                authorFlairHTML, postTimeMillis, title, permalink, score, postType, voteType,
                                nComments, upvoteRatio, flair, awards, nAwards, hidden, spoiler, nsfw, stickied,
                                archived, locked, saved, isCrosspost, distinguished, suggestedSort);
                        post.setPreviews(previews);
                        post.setVideoUrl(videoUrl);
                        post.setVideoDownloadUrl(videoDownloadUrl);
                    }
                } else {
                    if (path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".jpeg")) {
                        //Image post
                        int postType = Post.IMAGE_TYPE;

                        post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                                authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                                postType, voteType, nComments, upvoteRatio, flair, awards, nAwards,
                                hidden, spoiler, nsfw, stickied, archived, locked, saved, isCrosspost,
                                distinguished, suggestedSort);

                        if (previews.isEmpty()) {
                            previews.add(new Post.Preview(url, 0, 0, "", ""));
                        }
                        post.setPreviews(previews);
                    } else if (path.endsWith(".gif")) {
                        //Gif post
                        int postType = Post.GIF_TYPE;
                        post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                                authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                                postType, voteType, nComments, upvoteRatio, flair, awards, nAwards,
                                hidden, spoiler, nsfw, stickied, archived, locked, saved, isCrosspost,
                                distinguished, suggestedSort);

                        post.setPreviews(previews);
                        post.setVideoUrl(url);
                    } else if (uri.getAuthority().contains("imgur.com") && (path.endsWith(".gifv") || path.endsWith(".mp4"))) {
                        // Imgur gifv/mp4
                        int postType = Post.VIDEO_TYPE;

                        if (url.endsWith("gifv")) {
                            url = url.substring(0, url.length() - 5) + ".mp4";
                        }

                        post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                                authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                                postType, voteType, nComments, upvoteRatio, flair, awards, nAwards,
                                hidden, spoiler, nsfw, stickied, archived, locked, saved, isCrosspost,
                                distinguished, suggestedSort);
                        post.setPreviews(previews);
                        post.setVideoUrl(url);
                        post.setVideoDownloadUrl(url);
                        post.setIsImgur(true);
                    } else if (path.endsWith(".mp4")) {
                        //Video post
                        int postType = Post.VIDEO_TYPE;

                        post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                                authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                                postType, voteType, nComments, upvoteRatio, flair, awards, nAwards,
                                hidden, spoiler, nsfw, stickied, archived, locked, saved, isCrosspost,
                                distinguished, suggestedSort);
                        post.setPreviews(previews);
                        post.setVideoUrl(url);
                        post.setVideoDownloadUrl(url);
                    } else {
                        if (url.contains(permalink)) {
                            //Text post but with a preview
                            int postType = Post.TEXT_TYPE;

                            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                                    authorFlair, authorFlairHTML, postTimeMillis, title, permalink, score,
                                    postType, voteType, nComments, upvoteRatio, flair, awards, nAwards,
                                    hidden, spoiler, nsfw, stickied, archived, locked, saved, isCrosspost,
                                    distinguished, suggestedSort);

                            //Need attention
                            post.setPreviews(previews);
                        } else {
                            //Link post
                            int postType = Post.LINK_TYPE;

                            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                                    authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                                    postType, voteType, nComments, upvoteRatio, flair, awards, nAwards,
                                    hidden, spoiler, nsfw, stickied, archived, locked, saved, isCrosspost,
                                    distinguished, suggestedSort);
                            if (data.isNull(JSONUtils.SELFTEXT_KEY)) {
                                post.setSelfText("");
                            } else {
                                post.setSelfText(Utils.modifyMarkdown(Utils.trimTrailingWhitespace(data.getString(JSONUtils.SELFTEXT_KEY))));
                            }

                            post.setPreviews(previews);

                            String authority = uri.getAuthority();

                            if (authority != null) {
                                if (authority.contains("gfycat.com")) {
                                    String gfycatId = url.substring(url.lastIndexOf("/") + 1).toLowerCase();
                                    post.setPostType(Post.VIDEO_TYPE);
                                    post.setIsGfycat(true);
                                    post.setVideoUrl(url);
                                    post.setGfycatId(gfycatId);
                                } else if (authority.contains("redgifs.com")) {
                                    String gfycatId = url.substring(url.lastIndexOf("/") + 1).toLowerCase();
                                    post.setPostType(Post.VIDEO_TYPE);
                                    post.setIsRedgifs(true);
                                    post.setVideoUrl(url);
                                    post.setGfycatId(gfycatId);
                                } else if (authority.equals("streamable.com")) {
                                    String shortCode = url.substring(url.lastIndexOf("/") + 1);
                                    post.setPostType(Post.VIDEO_TYPE);
                                    post.setIsStreamable(true);
                                    post.setVideoUrl(url);
                                    post.setStreamableShortCode(shortCode);
                                }
                            }
                        }
                    }
                }
            } else {
                if (path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".jpeg")) {
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
                } else if (path.endsWith(".mp4")) {
                    //Video post
                    int postType = Post.VIDEO_TYPE;

                    post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                            authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                            postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                            spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);
                    post.setPreviews(previews);
                    post.setVideoUrl(url);
                    post.setVideoDownloadUrl(url);
                } else {
                    //CP No Preview Link post
                    int postType = Post.NO_PREVIEW_LINK_TYPE;

                    post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                            authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                            postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                            spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);
                    //Need attention
                    if (data.isNull(JSONUtils.SELFTEXT_KEY)) {
                        post.setSelfText("");
                    } else {
                        post.setSelfText(Utils.modifyMarkdown(Utils.trimTrailingWhitespace(data.getString(JSONUtils.SELFTEXT_KEY))));
                    }

                    String authority = uri.getAuthority();

                    if (authority != null) {
                        if (authority.contains("gfycat.com")) {
                            String gfycatId = url.substring(url.lastIndexOf("/") + 1).toLowerCase();
                            post.setPostType(Post.VIDEO_TYPE);
                            post.setIsGfycat(true);
                            post.setVideoUrl(url);
                            post.setGfycatId(gfycatId);
                        } else if (authority.contains("redgifs.com")) {
                            String gfycatId = url.substring(url.lastIndexOf("/") + 1).toLowerCase();
                            post.setPostType(Post.VIDEO_TYPE);
                            post.setIsRedgifs(true);
                            post.setVideoUrl(url);
                            post.setGfycatId(gfycatId);
                        } else if (authority.equals("streamable.com")) {
                            String shortCode = url.substring(url.lastIndexOf("/") + 1);
                            post.setPostType(Post.VIDEO_TYPE);
                            post.setIsStreamable(true);
                            post.setVideoUrl(url);
                            post.setStreamableShortCode(shortCode);
                        }
                    }
                }
            }
        }

        if (post.getPostType() == Post.VIDEO_TYPE) {
            try {
                String authority = uri.getAuthority();
                if (authority != null) {
                    if (authority.contains("gfycat.com")) {
                        post.setIsGfycat(true);
                        post.setVideoUrl(url);
                        String gfycatId = url.substring(url.lastIndexOf("/") + 1);
                        if (gfycatId.contains("-")) {
                            gfycatId = gfycatId.substring(0, gfycatId.indexOf('-'));
                        }
                        post.setGfycatId(gfycatId.toLowerCase());
                    } else if (authority.contains("redgifs.com")) {
                        String gfycatId = url.substring(url.lastIndexOf("/") + 1);
                        if (gfycatId.contains("-")) {
                            gfycatId = gfycatId.substring(0, gfycatId.indexOf('-'));
                        }
                        post.setIsRedgifs(true);
                        post.setVideoUrl(url);
                        post.setGfycatId(gfycatId.toLowerCase());
                    } else if (authority.equals("streamable.com")) {
                        String shortCode = url.substring(url.lastIndexOf("/") + 1);
                        post.setPostType(Post.VIDEO_TYPE);
                        post.setIsStreamable(true);
                        post.setVideoUrl(url);
                        post.setStreamableShortCode(shortCode);
                    }
                }
            } catch (IllegalArgumentException ignore) { }
        } else if (post.getPostType() == Post.LINK_TYPE || post.getPostType() == Post.NO_PREVIEW_LINK_TYPE) {
            if (!data.isNull(JSONUtils.GALLERY_DATA_KEY)) {
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
            } else if (post.getPostType() == Post.LINK_TYPE) {
                String authority = uri.getAuthority();

                if (authority != null) {
                    if (authority.contains("gfycat.com")) {
                        String gfycatId = url.substring(url.lastIndexOf("/") + 1).toLowerCase();
                        post.setPostType(Post.VIDEO_TYPE);
                        post.setIsGfycat(true);
                        post.setVideoUrl(url);
                        post.setGfycatId(gfycatId);
                    } else if (authority.contains("redgifs.com")) {
                        String gfycatId = url.substring(url.lastIndexOf("/") + 1).toLowerCase();
                        post.setPostType(Post.VIDEO_TYPE);
                        post.setIsRedgifs(true);
                        post.setVideoUrl(url);
                        post.setGfycatId(gfycatId);
                    } else if (authority.equals("streamable.com")) {
                        String shortCode = url.substring(url.lastIndexOf("/") + 1);
                        post.setPostType(Post.VIDEO_TYPE);
                        post.setIsStreamable(true);
                        post.setVideoUrl(url);
                        post.setStreamableShortCode(shortCode);
                    }
                }
            }
        }

        if (post.getPostType() != Post.LINK_TYPE && post.getPostType() != Post.NO_PREVIEW_LINK_TYPE) {
            if (data.isNull(JSONUtils.SELFTEXT_KEY)) {
                post.setSelfText("");
            } else {
                String selfText = Utils.modifyMarkdown(Utils.trimTrailingWhitespace(data.getString(JSONUtils.SELFTEXT_KEY)));
                post.setSelfText(selfText);
                if (data.isNull(JSONUtils.SELFTEXT_HTML_KEY)) {
                    post.setSelfTextPlainTrimmed("");
                } else {
                    String selfTextPlain = Utils.trimTrailingWhitespace(
                            Html.fromHtml(data.getString(JSONUtils.SELFTEXT_HTML_KEY))).toString();
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
        }

        return post;
    }
    private static Post parseDataGQL(JSONObject data, String permalink, String id, String fullName,
                                  String subredditName, String subredditNamePrefixed, String author,
                                  String authorFlair, String authorFlairHTML,
                                  long postTimeMillis, String title, ArrayList<Post.Preview> previews,
                                  int score, int voteType, int nComments, int upvoteRatio, String flair,
                                  String awards, int nAwards, boolean hidden, boolean spoiler,
                                  boolean nsfw, boolean stickied, boolean archived, boolean locked,
                                  boolean saved, boolean deleted, boolean removed, boolean isCrosspost,
                                  String distinguished, String suggestedSort) throws JSONException {
        Post post;

        String url = Html.fromHtml(data.getString(JSONUtils.URL_KEY)).toString();

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
            setText(post, data);

        } else if(data.getString("postHint").equals("IMAGE")){
            int postType = Post.IMAGE_TYPE;

            if(!data.getJSONObject("media").isNull("typeHint")){
                if(data.getJSONObject(JSONUtils.MEDIA_KEY).getString("typeHint").equals("GIFVIDEO")){
                    postType = Post.VIDEO_TYPE;
                    JSONObject redditVideoObject = data.getJSONObject(JSONUtils.MEDIA_KEY).getJSONObject("animated").getJSONObject("mp4_source");

                    String videoUrl = Html.fromHtml(redditVideoObject.getString("url")).toString();

                    post = new Post(id, fullName, subredditName, subredditNamePrefixed, author, authorFlair,
                            authorFlairHTML, postTimeMillis, title, permalink, score, postType, voteType,
                            nComments, upvoteRatio, flair, awards, nAwards, hidden, spoiler, nsfw, stickied,
                            archived, locked, saved, isCrosspost, distinguished, suggestedSort);

                    post.setPreviews(previews);
                    post.setVideoUrl(videoUrl);
                    post.setVideoDownloadUrl(videoUrl);
                    return post;
                }
            }

            String srcUrl = url;

            if(data.getString("domain").contains("imgur")){
                srcUrl = data.getJSONObject("media").getJSONObject("still").getJSONObject("source").getString("url");
            }

            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                    authorFlair, authorFlairHTML, postTimeMillis, title, srcUrl, permalink, score,
                    postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                    spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);

            if (previews.isEmpty()) {
                previews.add(new Post.Preview(srcUrl, 0, 0, "", ""));
            }
            post.setPreviews(previews);
        } else if (data.getString("postHint").equals("HOSTED_VIDEO") || data.getString("postHint").equals("RICH_VIDEO")){
            if(data.getString("domain").contains("youtu") || data.getString("domain").contains("streamable")){
                int postType = Post.LINK_TYPE;
                post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                        authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                        postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                        spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);
                if (data.getBoolean("isSelfPost")) {
                    String selfText = data.getJSONObject("content").getString("markdown");
                    String selfTextModified = insertImages(selfText, data.getJSONObject("content").getJSONArray("richtextMedia"));
                    post.setSelfText(Utils.modifyMarkdown(Utils.trimTrailingWhitespace(selfTextModified)));
                } else {
                    post.setSelfText("");
                }
                if(!previews.isEmpty()){
                    post.setPreviews(previews);
                }
                return post;
            }

            // TODO check if streaming object is null and default to link type if so
            JSONObject redditVideoObject = data.getJSONObject(JSONUtils.MEDIA_KEY).getJSONObject("streaming");
            JSONObject download = data.getJSONObject(JSONUtils.MEDIA_KEY).getJSONObject("download");
            int postType = Post.VIDEO_TYPE;
            String videoUrl = Html.fromHtml(redditVideoObject.getString("dashUrl")).toString();
            String videoDownloadUrl = download.getString("url");

            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author, authorFlair,
                    authorFlairHTML, postTimeMillis, title, permalink, score, postType, voteType,
                    nComments, upvoteRatio, flair, awards, nAwards, hidden, spoiler, nsfw, stickied,
                    archived, locked, saved, isCrosspost, distinguished, suggestedSort);

            post.setPreviews(previews);
            post.setVideoUrl(videoUrl);
            post.setVideoDownloadUrl(videoDownloadUrl);
        } else if (data.getString("postHint").equals("LINK")){
            int postType = Post.LINK_TYPE;

            if(!data.isNull(JSONUtils.MEDIA_KEY) && !data.getJSONObject(JSONUtils.MEDIA_KEY).isNull("streaming")){
                postType = Post.VIDEO_TYPE;
                JSONObject redditVideoObject = data.getJSONObject(JSONUtils.MEDIA_KEY).getJSONObject("streaming");
                JSONObject download = data.getJSONObject(JSONUtils.MEDIA_KEY).getJSONObject("download");
                String videoUrl = Html.fromHtml(redditVideoObject.getString("dashUrl")).toString();
                String videoDownloadUrl = download.getString("url");

                post = new Post(id, fullName, subredditName, subredditNamePrefixed, author, authorFlair,
                        authorFlairHTML, postTimeMillis, title, permalink, score, postType, voteType,
                        nComments, upvoteRatio, flair, awards, nAwards, hidden, spoiler, nsfw, stickied,
                        archived, locked, saved, isCrosspost, distinguished, suggestedSort);

                post.setPreviews(previews);
                post.setVideoUrl(videoUrl);
                post.setVideoDownloadUrl(videoDownloadUrl);
                return post;
            }

            if(previews.isEmpty()){
                postType = Post.NO_PREVIEW_LINK_TYPE;
                post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                        authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                        postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                        spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);
                if (data.getBoolean("isSelfPost")) {
                    String selfText = data.getJSONObject("content").getString("markdown");
                    String selfTextModified = insertImages(selfText, data.getJSONObject("content").getJSONArray("richtextMedia"));
                    post.setSelfText(Utils.modifyMarkdown(Utils.trimTrailingWhitespace(selfTextModified)));
                } else {
                    post.setSelfText("");
                }
                return post;
            }

            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                    authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                    postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                    spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);
            if (data.getBoolean("isSelfPost")) {
                String selfText = data.getJSONObject("content").getString("markdown");
                String selfTextModified = insertImages(selfText, data.getJSONObject("content").getJSONArray("richtextMedia"));
                post.setSelfText(Utils.modifyMarkdown(Utils.trimTrailingWhitespace(selfTextModified)));
            } else {
                post.setSelfText("");
            }

            if(!previews.isEmpty()){
                post.setPreviews(previews);
            }
        }else if (!data.isNull("gallery")) {
            int postType = Post.LINK_TYPE;
            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                    authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                    postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                    spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);

            JSONArray galleryArray = data.getJSONObject("gallery").getJSONArray(JSONUtils.ITEMS_KEY);
            ArrayList<Post.Gallery> gallery = new ArrayList<>();
            for (int i = 0; i < galleryArray.length(); i++) {
                JSONObject galleryItem = galleryArray.getJSONObject(i);
                JSONObject mediaObject = galleryItem.getJSONObject("media");
                String galleryId = mediaObject.getString("id");
                String mimeType = mediaObject.getString("mimetype");
                String galleryItemUrl = mediaObject.getString("url");

                String galleryItemCaption = "";
                String galleryItemCaptionUrl = "";
                if (!galleryItem.isNull(JSONUtils.CAPTION_KEY)) {
                    galleryItemCaption = galleryItem.getString(JSONUtils.CAPTION_KEY).trim();
                }

                if (!galleryItem.isNull(JSONUtils.CAPTION_URL_KEY)) {
                    galleryItemCaptionUrl = galleryItem.getString("outboundUrl").trim();
                }

                // get preview data

                String[] resolutions = {"small", "medium", "large", "xlarge", "xxlarge", "xxxlarge"};
                for (String res : resolutions) {
                    if(mediaObject.isNull(res)){
                        continue;
                    }
                    JSONObject thumbnailPreview = mediaObject.getJSONObject(res);
                    String thumbnailPreviewUrl = thumbnailPreview.getString(JSONUtils.URL_KEY);
                    int thumbnailPreviewWidth = thumbnailPreview.getJSONObject("dimensions").getInt(JSONUtils.WIDTH_KEY);
                    int thumbnailPreviewHeight = thumbnailPreview.getJSONObject("dimensions").getInt(JSONUtils.HEIGHT_KEY);

                    previews.add(new Post.Preview(thumbnailPreviewUrl, thumbnailPreviewWidth, thumbnailPreviewHeight, "", ""));
                }

                if (previews.isEmpty() && (mimeType.contains("jpg") || mimeType.contains("png"))) {
                    previews.add(new Post.Preview(galleryItemUrl, mediaObject.getInt("width"),
                            mediaObject.getInt("heigth"), galleryItemCaption, galleryItemCaptionUrl));
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

        } else {
            int postType = Post.LINK_TYPE;

            if(previews.isEmpty()){
                postType = Post.NO_PREVIEW_LINK_TYPE;
            }

            post = new Post(id, fullName, subredditName, subredditNamePrefixed, author,
                    authorFlair, authorFlairHTML, postTimeMillis, title, url, permalink, score,
                    postType, voteType, nComments, upvoteRatio, flair, awards, nAwards, hidden,
                    spoiler, nsfw, stickied, archived, locked, saved, isCrosspost, distinguished, suggestedSort);
            if (data.getBoolean("isSelfPost")) {
                String selfText = data.getJSONObject("content").getString("markdown");
                String selfTextModified = insertImages(selfText, data.getJSONObject("content").getJSONArray("richtextMedia"));
                post.setSelfText(Utils.modifyMarkdown(Utils.trimTrailingWhitespace(selfTextModified)));
            } else {
                post.setSelfText("");
            }
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
                    String selfTextModified = insertImages(selfText, content.getJSONArray("richtextMedia"));
                    post.setSelfText(selfTextModified);
                    if (content.isNull("html")) {
                        post.setSelfTextPlainTrimmed("");
                    } else {
                        String selfTextPlain = Utils.trimTrailingWhitespace(
                                Html.fromHtml(content.getString("html"))).toString();
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
            post.setSelfTextPlainTrimmed("");
        }
    }

    public static String insertImages(String content, JSONArray richtextMedia) {

        Pattern patternImageLink = Pattern.compile("!\\[img\\]\\([^\\)]*\\)");
        Pattern patternLink = Pattern.compile("\\([^\\)]*\\)");
        Matcher matcher = patternImageLink.matcher(content);
        String value = content;

        Map replace = new HashMap<String, String>();

        int i = 0;
        while (matcher.find()) {
            String imageLink = matcher.group();
            Matcher matcher2 = patternLink.matcher(imageLink);

            if(matcher2.find()){
                String linkContent = matcher2.group().replaceAll("[()]", "");
                String[] parts = linkContent.split(" ");
                String caption = "";

                if(parts.length > 1){
                    caption = parts[1].replace("\"","");
                }
                try{
                    String url = richtextMedia.getJSONObject(i).getString("url");
                    replace.put(imageLink, String.format("![%s](%s)", caption, url));
                }catch (JSONException e){
                }
            }
            i++;
        }
        for(Object key : replace.keySet()){
            String k = key.toString();
            value = value.replace(k,replace.get(key).toString());
        }
        return value;
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
