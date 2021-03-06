package com.piusvelte.sonet.social;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.piusvelte.okoauth.Helper;
import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.StatusImages;
import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.Statuses;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.piusvelte.sonet.Sonet.Simgur;
import static com.piusvelte.sonet.Sonet.Slink;
import static com.piusvelte.sonet.Sonet.sRFC822;
import static com.piusvelte.sonet.Sonet.sTimeZone;

/**
 * Created by bemmanuel on 2/15/15.
 */
abstract public class Client {

    String mTag;

    Context mContext;
    String mToken;
    String mSecret;
    String mAccountEsid;
    int mNetwork;
    Helper mOAuth10Helper;

    private SimpleDateFormat mSimpleDateFormat = null;

    public Client(Context context, String token, String secret, String accountEsid, int network) {
        mTag = getClass().getSimpleName();
        mContext = context;
        mToken = token;
        mSecret = secret;
        mAccountEsid = accountEsid;
        mNetwork = network;
    }

    public enum Network {
        Twitter {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                return new Twitter(context, token, secret, accountEntityId, this.ordinal());
            }

            @Override
            public boolean isLocationSupported() {
                return true;
            }

            @Override
            public int getIcon() {
                return R.drawable.twitter;
            }
        },

        Facebook {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                return new Facebook(context, token, secret, accountEntityId, this.ordinal());
            }

            @Override
            public boolean isLocationSupported() {
                return true;
            }

            @Override
            public int getIcon() {
                return R.drawable.facebook;
            }
        },

        MySpace {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                return new MySpace(context, token, secret, accountEntityId, this.ordinal());
            }

            @Override
            public boolean isLocationSupported() {
                return false;
            }

            @Override
            public int getIcon() {
                return R.drawable.myspace;
            }
        },

        Buzz {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                return null;
            }

            @Override
            public boolean isLocationSupported() {
                return false;
            }

            @Override
            public int getIcon() {
                return R.drawable.buzz;
            }
        },

        Foursquare {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                return new Foursquare(context, token, secret, accountEntityId, this.ordinal());
            }

            @Override
            public boolean isLocationSupported() {
                return true;
            }

            @Override
            public int getIcon() {
                return R.drawable.foursquare;
            }
        },

        LinkedIn {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                return new LinkedIn(context, token, secret, accountEntityId, this.ordinal());
            }

            @Override
            public boolean isLocationSupported() {
                return false;
            }

            @Override
            public int getIcon() {
                return R.drawable.linkedin;
            }
        },

        Sms {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                throw new IllegalArgumentException("SMS is not a SocialClient");
            }

            @Override
            public boolean isLocationSupported() {
                return false;
            }

            @Override
            public int getIcon() {
                return R.drawable.sms;
            }
        },

        Rss {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                return new Rss(context, token, secret, accountEntityId, this.ordinal());
            }

            @Override
            public boolean isLocationSupported() {
                return false;
            }

            @Override
            public int getIcon() {
                return R.drawable.rss;
            }
        },

        IdentiCa {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                return new IdentiCa(context, token, secret, accountEntityId, this.ordinal());
            }

            @Override
            public boolean isLocationSupported() {
                return false;
            }

            @Override
            public int getIcon() {
                return R.drawable.identica;
            }
        },

        GooglePlus {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                return new GooglePlus(context, token, secret, accountEntityId, this.ordinal());
            }

            @Override
            public boolean isLocationSupported() {
                return false;
            }

            @Override
            public int getIcon() {
                return R.drawable.googleplus;
            }
        },

        Pinterest {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                return new Pinterest(context, token, secret, accountEntityId, this.ordinal());
            }

            @Override
            public boolean isLocationSupported() {
                return false;
            }

            @Override
            public int getIcon() {
                return R.drawable.buzz;// TODO replace
            }
        },

        Chatter {
            @Override
            public Client getClient(Context context, String token, String secret, String accountEntityId) {
                return new Chatter(context, token, secret, accountEntityId, this.ordinal());
            }

            @Override
            public boolean isLocationSupported() {
                return false;
            }

            @Override
            public int getIcon() {
                return R.drawable.salesforce;
            }
        };

        public static Network get(int network) {
            return Network.values()[network];
        }

        abstract public Client getClient(Context context, String token, String secret, String accountEntityId);

        abstract public boolean isLocationSupported();

        @DrawableRes
        abstract public int getIcon();
    }

    public static class Builder {

        private Context mContext;
        private Network mNetwork;
        private String mToken;
        private String mSecret;
        private String mAccountEsid;

        public Builder(@NonNull Context context) {
            mContext = context.getApplicationContext();
        }

        public static Builder from(@NonNull Client client) {
            return new Builder(client.mContext)
                    .setNetwork(client.mNetwork)
                    .setCredentials(client.mToken, client.mSecret)
                    .setAccount(client.mAccountEsid);
        }

        public Builder setNetwork(Network network) {
            mNetwork = network;
            return this;
        }

        public Builder setNetwork(int network) {
            mNetwork = Network.get(network);
            return this;
        }

        public Builder setCredentials(String token, String secret) {
            mToken = token;
            mSecret = secret;
            return this;
        }

        public Builder setAccount(String accountEsid) {
            mAccountEsid = accountEsid;
            return this;
        }

        public Client build() {
            return mNetwork.getClient(mContext, mToken, mSecret, mAccountEsid);
        }
    }

    public String getFeed(int appWidgetId,
            String widget,
            long account,
            int status_count,
            boolean time24hr,
            boolean display_profile,
            int notifications) {
        String[] notificationMessage = new String[1];
        Set<String> notificationSids = null;
        boolean doNotify = notifications != 0;

        if (doNotify) {
            notificationSids = getNotificationStatusIds(account, notificationMessage);
        }

        String response = getFeedResponse(status_count);

        if (!TextUtils.isEmpty(response)) {
            JSONArray feedItems;
            int parseCount;

            try {
                feedItems = parseFeed(response);

                if (feedItems != null) {
                    parseCount = Math.min(feedItems.length(), status_count);

                    if (parseCount > 0) {
                        removeOldStatuses(widget, Long.toString(account));

                        for (int itemIdx = 0; itemIdx < parseCount; itemIdx++) {
                            JSONObject item = feedItems.getJSONObject(itemIdx);

                            if (item != null) {
                                addFeedItem(item,
                                        display_profile,
                                        time24hr,
                                        appWidgetId,
                                        account,
                                        notificationSids,
                                        notificationMessage,
                                        doNotify);
                            }
                        }
                    } else if (this instanceof MySpace) {
                        // warn about myspace permissions
                        addStatusItem(0,
                                getString(R.string.myspace_permissions_title),
                                null,
                                getString(R.string.myspace_permissions_message),
                                time24hr,
                                appWidgetId,
                                account,
                                "",
                                "",
                                new ArrayList<String[]>()
                        );
                    }
                }
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) {
                    Log.d(mTag, "error parsing feed response: " + response, e);
                }
            }
        } else if (this instanceof MySpace) {
            // warn about myspace permissions
            addStatusItem(0,
                    getString(R.string.myspace_permissions_title),
                    null,
                    getString(R.string.myspace_permissions_message),
                    time24hr,
                    appWidgetId,
                    account,
                    "",
                    "",
                    new ArrayList<String[]>()
            );
        }

        if (doNotify) {
            getNotificationMessage(account, notificationMessage);
            return notificationMessage[0];
        }

        return null;
    }

    @Nullable
    abstract public String getProfileUrl(@NonNull String esid);

    @Nullable
    abstract public String getProfilePhotoUrl();

    @Nullable
    abstract public String getProfilePhotoUrl(String esid);

    @Nullable
    abstract public Set<String> getNotificationStatusIds(long account, String[] notificationMessage);

    @Nullable
    abstract public String getFeedResponse(int status_count);

    @Nullable
    abstract public JSONArray parseFeed(@NonNull String response) throws JSONException;

    @Nullable
    abstract public void addFeedItem(@NonNull JSONObject item,
            boolean display_profile,
            boolean time24hr,
            int appWidgetId,
            long account,
            Set<String> notificationSids,
            String[] notificationMessage,
            boolean doNotify) throws JSONException;

    @Nullable
    abstract public void getNotificationMessage(long account, String[] notificationMessage);

    abstract public void getNotifications(long account, String[] notificationMessage);

    abstract public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags);

    abstract public boolean isLikeable(String statusId);

    abstract public boolean isLiked(String statusId, String accountId);

    abstract public boolean likeStatus(String statusId, String accountId, boolean doLike);

    abstract public String getLikeText(boolean isLiked);

    abstract public boolean isCommentable(String statusId);

    abstract public String getCommentPretext(String accountId);

    abstract public void onDelete();

    public List<HashMap<String, String>> getComments(@NonNull String statusId, boolean time24hr) {
        List<HashMap<String, String>> parsedComments = new ArrayList<>();

        String response = getCommentsResponse(statusId);

        if (!TextUtils.isEmpty(response)) {
            JSONArray jsonComments = null;

            try {
                jsonComments = parseComments(response);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.d(mTag, "exception parsing: " + response, e);
            }

            if (jsonComments != null) {
                for (int commentsIdx = 0, commentsLength = jsonComments.length(); commentsIdx < commentsLength; commentsIdx++) {
                    JSONObject comment = null;

                    try {
                        comment = jsonComments.getJSONObject(commentsIdx);
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) Log.d(mTag, "exception getting comment", e);
                    }

                    if (comment != null) {
                        HashMap<String, String> parsedComment = null;

                        try {
                            parsedComment = parseComment(statusId, comment, time24hr);
                        } catch (JSONException e) {
                            if (BuildConfig.DEBUG) Log.d(mTag, "exception parsing comment", e);
                        }

                        if (parsedComment != null) {
                            parsedComments.add(parsedComment);
                        }
                    }
                }
            }
        }

        return parsedComments;
    }

    @Nullable
    abstract public String getCommentsResponse(String statusId);

    @Nullable
    abstract public JSONArray parseComments(@NonNull String response) throws JSONException;

    @Nullable
    abstract public HashMap<String, String> parseComment(@NonNull String statusId, @NonNull JSONObject jsonComment, boolean time24hr)
            throws JSONException;

    abstract public LinkedHashMap<String, String> getLocations(String latitude, String longitude);

    abstract public boolean sendComment(@NonNull String statusId, @NonNull String message);

    abstract public List<HashMap<String, String>> getFriends();

    abstract String getApiKey();

    abstract String getApiSecret();

    @Nullable
    abstract public Uri getCallback();

    abstract String getRequestUrl();

    abstract String getAccessUrl();

    abstract String getAuthorizeUrl();

    abstract public String getCallbackUrl();

    abstract public MemberAuthentication getMemberAuthentication(@NonNull String authenticatedUrl);

    public boolean hasConnectionError() {
        return false;
    }

    /**
     * Attempt to resolve a connection error {@link #hasConnectionError()}
     *
     * @param activity
     * @param requestCode
     * @return {@code True} if resolution is attempted
     */
    public boolean resolveConnectionError(@NonNull Activity activity, int requestCode) {
        return false;
    }

    @Nullable
    public static String getParamValue(@Nullable String url, @NonNull String name) {
        if (TextUtils.isEmpty(url)) return null;

        name += "=";
        int nameIndex = url.indexOf(name);

        if (nameIndex < 0) return null;

        String value = url.substring(nameIndex + name.length());

        int nextParamIndex = value.indexOf("&");

        if (nextParamIndex >= 0) {
            value = value.substring(0, nextParamIndex);
        }

        return value;
    }

    @Nullable
    public String getAuthUrl() {
        return getOAuth10Helper().getTokenAuthorizationUrl(SonetHttpClient.getOkHttpClientInstance());
    }

    Helper getOAuth10Helper() {
        if (mOAuth10Helper == null) {
            mOAuth10Helper = new Helper(getApiKey(),
                    getApiSecret(),
                    mToken,
                    mSecret,
                    getRequestUrl(),
                    getAuthorizeUrl(),
                    getAccessUrl(),
                    getCallbackUrl());
        }

        return mOAuth10Helper;
    }

    String getString(int resId) {
        return mContext.getString(resId);
    }

    Resources getResources() {
        return mContext.getResources();
    }

    ContentResolver getContentResolver() {
        return mContext.getContentResolver();
    }

    String getFirstPhotoUrl(String[] parts) {
        if (parts.length > 1) {
            Uri uri = Uri.parse(parts[1]);

            if (uri.getHost().equals(Simgur)) {
                return parts[1];
            }
        }

        return null;
    }

    String getPostFriendOverride(String friend) {
        return null;
    }

    String getPostFriend(String friend) {
        return friend;
    }

    void formatLink(Matcher matcher, StringBuffer stringBuffer, String link) {
        matcher.appendReplacement(stringBuffer, "(" + Slink + ": " + Uri.parse(link).getHost() + ")");
    }

    void addStatusItem(long created,
            String friend,
            String url,
            String message,
            boolean time24hr,
            int appWidgetId,
            long accountId,
            String sid,
            String esid) {
        addStatusItem(created, friend, url, message, time24hr, appWidgetId, accountId, sid, esid, new ArrayList<String[]>());
    }

    void addStatusItem(long created,
            String friend,
            String url,
            String message,
            boolean time24hr,
            int appWidgetId,
            long accountId,
            String sid,
            String esid,
            ArrayList<String[]> links) {
        long id;
        String friend_override = getPostFriendOverride(friend);
        friend = getPostFriend(friend);

        ContentValues entityValues = new ContentValues();
        entityValues.put(Entity.FRIEND, friend);
        entityValues.put(Entity.PROFILE_URL, url);

        Cursor entity = getContentResolver().query(Entity.getContentUri(mContext),
                new String[] { Entity._ID },
                Entity.ACCOUNT + "=? and " + Entity.ESID + "=?",
                new String[] { Long.toString(accountId),
                        SonetCrypto.getInstance(mContext).Encrypt(esid) },
                null);

        if (entity.moveToFirst()) {
            id = entity.getLong(0);
            // update friend and profile_url if changed
            getContentResolver().update(Entity.getContentUri(mContext),
                    entityValues,
                    Entity.ACCOUNT + "=? and " + Entity.ESID + "=?",
                    new String[] { Long.toString(accountId),
                            SonetCrypto.getInstance(mContext).Encrypt(esid) });
        } else {
            entityValues.put(Entity.ESID, esid);
            entityValues.put(Entity.ACCOUNT, accountId);
            id = Long.parseLong(getContentResolver().insert(Entity.getContentUri(mContext), entityValues).getLastPathSegment());
        }

        entity.close();
        // facebook sid comes in as esid_sid, the esid_ may need to be removed
        //		if (serviceId == FACEBOOK) {
        //			int split = sid.indexOf("_");
        //			if ((split > 0) && (split < sid.length())) {
        //				sid = sid.substring(sid.indexOf("_") + 1);
        //			}
        //		}
        // update the account statuses

        // parse any links
        Matcher m = Pattern.compile("\\bhttp(s)?://\\S+\\b", Pattern.CASE_INSENSITIVE).matcher(message);
        StringBuffer sb = new StringBuffer(message.length());

        while (m.find()) {
            String link = m.group();
            // check existing links before adding
            boolean exists = false;

            for (String[] l : links) {
                if (l[1].equals(link)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                links.add(new String[] { Slink, link });
                formatLink(m, sb, link);
            }
        }

        m.appendTail(sb);
        message = sb.toString();
        ContentValues values = new ContentValues();
        values.put(Statuses.CREATED, created);
        values.put(Statuses.ENTITY, id);
        values.put(Statuses.MESSAGE, message);
        values.put(Statuses.SERVICE, mNetwork);
        values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(created, time24hr));
        values.put(Statuses.WIDGET, appWidgetId);
        values.put(Statuses.ACCOUNT, accountId);
        values.put(Statuses.SID, sid);
        values.put(Statuses.FRIEND_OVERRIDE, friend_override);
        long statusId = Long.parseLong(getContentResolver().insert(Statuses.getContentUri(mContext), values).getLastPathSegment());
        String imageUrl = null;

        for (String[] s : links) {
            // get the first photo
            if (imageUrl == null) {
                imageUrl = getFirstPhotoUrl(s);
            }

            ContentValues linkValues = new ContentValues();
            linkValues.put(StatusLinks.STATUS_ID, statusId);
            linkValues.put(StatusLinks.LINK_TYPE, s[0]);
            linkValues.put(StatusLinks.LINK_URI, s[1]);
            getContentResolver().insert(StatusLinks.getContentUri(mContext), linkValues);
        }

        if (!TextUtils.isEmpty(imageUrl)) {
            addStatusImage(statusId, imageUrl);
        }
    }

    private void addStatusImage(long statusId, @NonNull String imageUrl) {
        ContentValues imageValues = new ContentValues();
        imageValues.put(StatusImages.STATUS_ID, statusId);
        imageValues.put(StatusImages.URL, imageUrl);
        mContext.getContentResolver().insert(StatusImages.getContentUri(mContext),
                imageValues);
    }

    void removeOldStatuses(String widgetId, String accountId) {
        Cursor statuses = getContentResolver()
                .query(Statuses.getContentUri(mContext), new String[] { Statuses._ID }, Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?",
                        new String[] { widgetId, accountId }, null);

        if (statuses.moveToFirst()) {
            while (!statuses.isAfterLast()) {
                String id = Long.toString(statuses.getLong(0));
                getContentResolver().delete(StatusLinks.getContentUri(mContext), StatusLinks.STATUS_ID + "=?", new String[] { id });
                getContentResolver().delete(StatusImages.getContentUri(mContext), StatusImages.STATUS_ID + "=?", new String[] { id });
                statuses.moveToNext();
            }
        }

        statuses.close();
        getContentResolver().delete(Statuses.getContentUri(mContext), Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?",
                new String[] { widgetId, accountId });
        Cursor entities = getContentResolver()
                .query(Entity.getContentUri(mContext), new String[] { Entity._ID }, Entity.ACCOUNT + "=?", new String[] { accountId }, null);

        if (entities.moveToFirst()) {
            while (!entities.isAfterLast()) {
                Cursor s = getContentResolver().query(Statuses.getContentUri(mContext),
                        new String[] { Statuses._ID },
                        Statuses.ACCOUNT + "=? and " + Statuses.WIDGET + " !=?",
                        new String[] { accountId, widgetId },
                        null);

                if (!s.moveToFirst()) {
                    // not in use, remove it
                    getContentResolver()
                            .delete(Entity.getContentUri(mContext), Entity._ID + "=?", new String[] { Long.toString(entities.getLong(0)) });
                }

                s.close();
                entities.moveToNext();
            }
        }

        entities.close();
    }

    void addNotification(String sid, String esid, String friend, String message, long created, long accountId, String notification) {
        ContentValues values = new ContentValues();
        values.put(Notifications.SID, sid);
        values.put(Notifications.ESID, esid);
        values.put(Notifications.FRIEND, friend);
        values.put(Notifications.MESSAGE, message);
        values.put(Notifications.CREATED, created);
        values.put(Notifications.ACCOUNT, accountId);
        values.put(Notifications.NOTIFICATION, notification);
        values.put(Notifications.CLEARED, 0);
        values.put(Notifications.UPDATED, created);
        getContentResolver().insert(Notifications.getContentUri(mContext), values);
    }

    String updateNotification(long notificationId, long created_time, String accountEsid, String esid, String name, boolean cleared) {
        String message = null;
        // new comment
        ContentValues values = new ContentValues();
        values.put(Notifications.UPDATED, created_time);

        if (accountEsid.equals(esid)) {
            // user's own comment, clear the notification
            values.put(Notifications.CLEARED, 1);
        } else if (cleared) {
            values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), name));
            values.put(Notifications.CLEARED, 0);
            message = String.format(getString(R.string.friendcommented), name);
        } else {
            values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), name + " and others"));
            message = String.format(getString(R.string.friendcommented), name + " and others");
        }

        getContentResolver().update(Notifications.getContentUri(mContext),
                values,
                Notifications._ID + "=?",
                new String[] { Long.toString(notificationId) });
        return message;
    }

    void updateNotificationMessage(String[] originalMessage, String newMessage) {
        if (TextUtils.isEmpty(originalMessage[0])) {
            originalMessage[0] = newMessage;
        } else if (!TextUtils.isEmpty(newMessage)) {
            originalMessage[0] = mContext.getString(R.string.notify_multiple_updates);
        }
    }

    long parseDate(String date, String format) {
        if (date != null) {
            // hack for the literal 'Z'
            if (date.substring(date.length() - 1).equals("Z")) {
                date = date.substring(0, date.length() - 2) + "+0000";
            }

            Date created = null;

            if (format != null) {
                if (mSimpleDateFormat == null) {
                    mSimpleDateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
                    // all dates should be GMT/UTC
                    mSimpleDateFormat.setTimeZone(sTimeZone);
                }

                try {
                    created = mSimpleDateFormat.parse(date);
                    return created.getTime();
                } catch (ParseException e) {
                    Log.e(mTag, e.toString());
                }
            } else {
                // attempt to parse RSS date
                if (mSimpleDateFormat != null) {
                    try {
                        created = mSimpleDateFormat.parse(date);
                        return created.getTime();
                    } catch (ParseException e) {
                        Log.e(mTag, e.toString());
                    }
                }

                for (String rfc822 : sRFC822) {
                    mSimpleDateFormat = new SimpleDateFormat(rfc822, Locale.ENGLISH);
                    mSimpleDateFormat.setTimeZone(sTimeZone);

                    try {
                        if ((created = mSimpleDateFormat.parse(date)) != null) {
                            return created.getTime();
                        }
                    } catch (ParseException e) {
                        Log.e(mTag, e.toString());
                    }
                }
            }
        }

        return System.currentTimeMillis();
    }

    public static class MemberAuthentication {
        public String username;
        public String token;
        public String secret;
        public int expiry;
        public int network;
        public String id;
    }
}
