package com.piusvelte.sonet.social;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.Statuses;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static com.piusvelte.sonet.Sonet.S_total;
import static com.piusvelte.sonet.Sonet.Sbody;
import static com.piusvelte.sonet.Sonet.Scomment;
import static com.piusvelte.sonet.Sonet.Sconnections;
import static com.piusvelte.sonet.Sonet.ScurrentShare;
import static com.piusvelte.sonet.Sonet.SfirstName;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.SisCommentable;
import static com.piusvelte.sonet.Sonet.Sjob;
import static com.piusvelte.sonet.Sonet.SlastName;
import static com.piusvelte.sonet.Sonet.SmemberGroups;
import static com.piusvelte.sonet.Sonet.Sname;
import static com.piusvelte.sonet.Sonet.Sperson;
import static com.piusvelte.sonet.Sonet.SpersonActivities;
import static com.piusvelte.sonet.Sonet.SpictureUrl;
import static com.piusvelte.sonet.Sonet.Sposition;
import static com.piusvelte.sonet.Sonet.SrecommendationSnippet;
import static com.piusvelte.sonet.Sonet.SrecommendationsGiven;
import static com.piusvelte.sonet.Sonet.Srecommendee;
import static com.piusvelte.sonet.Sonet.Stimestamp;
import static com.piusvelte.sonet.Sonet.Stitle;
import static com.piusvelte.sonet.Sonet.SupdateComments;
import static com.piusvelte.sonet.Sonet.SupdateContent;
import static com.piusvelte.sonet.Sonet.SupdateKey;
import static com.piusvelte.sonet.Sonet.SupdateType;
import static com.piusvelte.sonet.Sonet.Svalues;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class LinkedIn extends Client {

    private static final String LINKEDIN_BASE_URL = "https://api.linkedin.com/v1/people/~";
    private static final String LINKEDIN_URL_ACCESS = "https://www.linkedin.com/uas/oauth2/accessToken";
    private static final String LINKEDIN_URL_ME = "%s:(id,first-name,last-name,picture-url)";
    private static final String LINKEDIN_URL_USER = "https://api.linkedin.com/v1/people/id=%s";
    @Deprecated
    private static final String LINKEDIN_UPDATES = "%s/network/updates?type=APPS&type=CMPY&type=CONN&type=JOBS&type=JGRP&type=PICT"
            + "&type=PRFU&type=RECU&type=PRFX&type=ANSW&type=QSTN&type=SHAR&type=VIRL&format=json";
    // TODO append &format=json instead
    private static final String[][] LINKEDIN_HEADERS = new String[][] { { "x-li-format", "json" } };
    private static final String LINKEDIN_IS_LIKED = "%s/network/updates/key=%s/is-liked";
    private static final String LINKEDIN_UPDATE = "%s/network/updates/key=%s";
    private static final String LINKEDIN_UPDATE_COMMENTS = "%s/network/updates/key=%s/update-comments";
    private static final String LINKEDIN_POST = "%s/shares?format-json";
    private static final String LINKEDIN_POST_BODY = "{\"comment\":\"%s\",\"visibility\":{\"code\":\"anyone\"}}";
    private static final String LINKEDIN_COMMENT_BODY = "<?xml version='1.0' " +
            "encoding='UTF-8'?><update-comment><comment>%s</comment></update-comment>";
    private static final String LINKEDIN_LIKE_BODY = "<?xml version='1.0' encoding='UTF-8'?><is-liked>%s</is-liked>";
    private static final String IS_LIKABLE = "isLikable";
    private static final String IS_LIKED = "isLiked";
    private static final String IS_COMMENTABLE = "isCommentable";

    @Deprecated
    public enum LinkedIn_UpdateTypes {
        ANSW("updated an answer"),
        APPS("updated the application "),
        CMPY("company update"),
        CONN("is now connected to "),
        JOBP("posted the job "),
        JGRP("joined the group "),
        PRFX("updated their extended profile"),
        PREC("recommends "),
        PROF("changed their profile"),
        QSTN("updated a question"),
        SHAR("shared something"),
        VIRL("updated the viral "),
        PICU("updated their profile picture");

        public String message = null;

        LinkedIn_UpdateTypes(String message) {
            this.message = message;
        }

        public static boolean contains(String type) {
            for (LinkedIn_UpdateTypes t : LinkedIn_UpdateTypes.values()) {
                if (t.name().equals(type)) {
                    return true;
                }
            }
            return false;
        }

        public static String getMessage(String type) {
            for (LinkedIn_UpdateTypes t : LinkedIn_UpdateTypes.values()) {
                if (t.name().equals(type)) {
                    return t.message;
                }
            }
            return null;
        }
    }

    public LinkedIn(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    @Nullable
    @Override
    public String getProfileUrl(@NonNull String esid) {
        Request request = signRequest(new Request.Builder()
                .url(String.format(LINKEDIN_URL_USER, esid)));
        String response = SonetHttpClient.getResponse(request);

        if (!TextUtils.isEmpty(response)) {
            try {
                return new JSONObject(response).getJSONObject("siteStandardProfileRequest").getString("url").replaceAll("\\\\", "");
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(mTag, "Error parsing: " + response, e);
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public String getProfilePhotoUrl() {
        return getProfilePhotoUrl(mAccountEsid);
    }

    @Nullable
    @Override
    public String getProfilePhotoUrl(String esid) {
        Request request = signRequest(new Request.Builder()
                .url(String.format(LINKEDIN_URL_ME, LINKEDIN_BASE_URL)));
        String httpResponse = SonetHttpClient.getResponse(request);

        if (!TextUtils.isEmpty(httpResponse)) {
            try {
                JSONObject jobj = new JSONObject(httpResponse);
                return jobj.getString(SpictureUrl);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) {
                    Log.d(mTag, "error parsing me response: " + httpResponse, e);
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public Uri getCallback() {
        return Uri.parse("http://www.piusvelte.com/sonet/linkedin/callback");
    }

    @Override
    String getRequestUrl() {
        return null;
    }

    @Override
    String getAccessUrl() {
        return null;
    }

    @Override
    String getAuthorizeUrl() {
        return null;
    }

    @Override
    public String getCallbackUrl() {
        return getCallback().toString();
    }

    @Nullable
    @Override
    public String getAuthUrl() {
        return "https://www.linkedin.com/uas/oauth2/authorization?"
                + "response_type=code&client_id=" + BuildConfig.LINKEDIN_KEY
                + "&redirect_uri=" + getCallbackUrl()
                + "&state=" + Long.toString(System.currentTimeMillis())
                + "&scope=r_basicprofile%20r_emailaddress%20w_share";
    }

    @Override
    public MemberAuthentication getMemberAuthentication(@NonNull String authenticatedUrl) {
        String code = getParamValue(authenticatedUrl, "code");

        if (!TextUtils.isEmpty(code)) {
            Request request = new Request.Builder()
                    .url(LINKEDIN_URL_ACCESS)
                    .post(new FormEncodingBuilder()
                            .add("grant_type", "authorization_code")
                            .add("code", code)
                            .add("redirect_uri", getCallbackUrl())
                            .add("client_id", BuildConfig.LINKEDIN_KEY)
                            .add("client_secret", BuildConfig.LINKEDIN_SECRET)
                            .build())
                    .build();

            String accessTokenResponse = SonetHttpClient.getResponse(request);

            if (!TextUtils.isEmpty(accessTokenResponse)) {

                try {
                    JSONObject accessTokenResponseJobj = new JSONObject(accessTokenResponse);

                    if (accessTokenResponseJobj.has("access_token") && accessTokenResponseJobj.has("expires_in")) {
                        mToken = accessTokenResponseJobj.getString("access_token");
                        int expiry = accessTokenResponseJobj.getInt("expires_in");

                        if (!TextUtils.isEmpty(mToken)) {
                            Request meRequest = signRequest(new Request.Builder()
                                    .url(String.format(LINKEDIN_URL_ME, LINKEDIN_BASE_URL)));

                            String meResponse = SonetHttpClient.getResponse(meRequest);

                            if (!TextUtils.isEmpty(meResponse)) {
                                try {
                                    JSONObject jobj = new JSONObject(meResponse);

                                    if (jobj.has("firstName") && jobj.has(Sid)) {
                                        MemberAuthentication memberAuthentication = new MemberAuthentication();
                                        memberAuthentication.username = jobj.getString("firstName") + " " + jobj.getString("lastName");
                                        memberAuthentication.token = mToken;
                                        memberAuthentication.secret = "";
                                        memberAuthentication.expiry = expiry;
                                        memberAuthentication.network = mNetwork;
                                        memberAuthentication.id = jobj.getString(Sid);
                                        return memberAuthentication;
                                    }
                                } catch (JSONException e) {
                                    if (BuildConfig.DEBUG) {
                                        Log.d(mTag, "error parsing me response: " + meResponse, e);
                                    }
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    if (BuildConfig.DEBUG) {
                        Log.d(mTag, "error parsing access token response: " + accessTokenResponse, e);
                    }
                }
            }
        }

        return null;
    }

    private Request signRequest(Request.Builder builder) {
        for (String[] header : LINKEDIN_HEADERS) {
            builder.addHeader(header[0], header[1]);
        }

        builder.addHeader("Authorization", "Bearer " + mToken);

        return builder.build();
    }

    @Nullable
    @Override
    public Set<String> getNotificationStatusIds(long account, String[] notificationMessage) {
        Set<String> notificationSids = new HashSet<>();
        Cursor currentNotifications = getContentResolver().query(Notifications.getContentUri(mContext),
                new String[] { Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID },
                Notifications.ACCOUNT + "=?", new String[] { Long.toString(account) }, null);

        // loop over notifications
        if (currentNotifications.moveToFirst()) {
            while (!currentNotifications.isAfterLast()) {
                long notificationId = currentNotifications.getLong(0);
                String sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(1));
                long updated = currentNotifications.getLong(2);
                boolean cleared = currentNotifications.getInt(3) == 1;

                // store sids, to avoid duplicates when requesting the latest feed
                notificationSids.add(sid);

                // get comments for current notifications
                Request request = signRequest(new Request.Builder()
                        .url(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, sid)));

                String response = SonetHttpClient.getResponse(request);

                if (!TextUtils.isEmpty(response)) {
                    // check for a newer post, if it's the user's own, then set CLEARED=0
                    try {
                        JSONObject jsonResponse = new JSONObject(response);

                        if (jsonResponse.has(S_total) && (jsonResponse.getInt(S_total) != 0)) {
                            JSONArray commentsArray = jsonResponse.getJSONArray(Svalues);
                            int i2 = commentsArray.length();

                            if (i2 > 0) {
                                for (int i = 0; i < i2; i++) {
                                    JSONObject commentObj = commentsArray.getJSONObject(i);
                                    long created_time = commentObj.getLong(Stimestamp);

                                    if (created_time > updated) {
                                        JSONObject friendObj = commentObj.getJSONObject(Sperson);
                                        updateNotificationMessage(notificationMessage,
                                                updateNotification(notificationId, created_time, mAccountEsid, friendObj.getString(Sid),
                                                        friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName), cleared));
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) Log.d(mTag, "error parsing: " + response, e);
                    }
                }

                currentNotifications.moveToNext();
            }
        }

        currentNotifications.close();
        return notificationSids;
    }

    @Nullable
    @Override
    public String getFeedResponse(int status_count) {
        Request request = signRequest(new Request.Builder()
                .url(String.format(LINKEDIN_UPDATES, LINKEDIN_BASE_URL)));
        return SonetHttpClient.getResponse(request);
    }

    @Nullable
    @Override
    public JSONArray parseFeed(@NonNull String response) throws JSONException {
        return new JSONObject(response).getJSONArray(Svalues);
    }

    @Nullable
    @Override
    public void addFeedItem(@NonNull JSONObject item,
            boolean display_profile,
            boolean time24hr,
            int appWidgetId,
            long account,
            Set<String> notificationSids,
            String[] notificationMessage,
            boolean doNotify) throws JSONException {
        Log.d("Bryan", "item=" + item.toString(4));
        String updateType = item.getString(SupdateType);
        JSONObject updateContent = item.getJSONObject(SupdateContent);
        String update = LinkedIn_UpdateTypes.getMessage(updateType);

        if (update != null && updateContent.has(Sperson)) {
            JSONObject friendObj = updateContent.getJSONObject(Sperson);

            if (LinkedIn_UpdateTypes.APPS.name().equals(updateType)) {
                if (friendObj.has(SpersonActivities)) {
                    JSONObject personActivities = friendObj.getJSONObject(SpersonActivities);

                    if (personActivities.has(Svalues)) {
                        JSONArray updates = personActivities.getJSONArray(Svalues);

                        for (int u = 0, u2 = updates.length(); u < u2; u++) {
                            update += updates.getJSONObject(u).getString(Sbody);
                            if (u < (updates.length() - 1)) {
                                update += ", ";
                            }
                        }
                    }
                }
            } else if (LinkedIn_UpdateTypes.CONN.name().equals(updateType)) {
                if (friendObj.has(Sconnections)) {
                    JSONObject connections = friendObj.getJSONObject(Sconnections);

                    if (connections.has(Svalues)) {
                        JSONArray updates = connections.getJSONArray(Svalues);

                        for (int u = 0, u2 = updates.length(); u < u2; u++) {
                            update += updates.getJSONObject(u).getString(SfirstName) + " " + updates.getJSONObject(u).getString(SlastName);

                            if (u < (updates.length() - 1)) {
                                update += ", ";
                            }
                        }
                    }
                }
            } else if (LinkedIn_UpdateTypes.JOBP.name().equals(updateType)) {
                if (updateContent.has(Sjob) && updateContent.getJSONObject(Sjob).has(Sposition) && updateContent.getJSONObject(Sjob)
                        .getJSONObject(Sposition).has(Stitle)) {
                    update += updateContent.getJSONObject(Sjob).getJSONObject(Sposition).getString(Stitle);
                }
            } else if (LinkedIn_UpdateTypes.JGRP.name().equals(updateType)) {
                if (friendObj.has(SmemberGroups)) {
                    JSONObject memberGroups = friendObj.getJSONObject(SmemberGroups);

                    if (memberGroups.has(Svalues)) {
                        JSONArray updates = memberGroups.getJSONArray(Svalues);

                        for (int u = 0, u2 = updates.length(); u < u2; u++) {
                            update += updates.getJSONObject(u).getString(Sname);

                            if (u < (updates.length() - 1)) {
                                update += ", ";
                            }
                        }
                    }
                }
            } else if (LinkedIn_UpdateTypes.PREC.name().equals(updateType)) {
                if (friendObj.has(SrecommendationsGiven)) {
                    JSONObject recommendationsGiven = friendObj.getJSONObject(SrecommendationsGiven);

                    if (recommendationsGiven.has(Svalues)) {
                        JSONArray updates = recommendationsGiven.getJSONArray(Svalues);
                        for (int u = 0, u2 = updates.length(); u < u2; u++) {
                            JSONObject recommendation = updates.getJSONObject(u);
                            JSONObject recommendee = recommendation.getJSONObject(Srecommendee);
                            if (recommendee.has(SfirstName)) {
                                update += recommendee.getString(SfirstName);
                            }
                            if (recommendee.has(SlastName)) {
                                update += recommendee.getString(SlastName);
                            }
                            if (recommendation.has(SrecommendationSnippet)) {
                                update += ":" + recommendation.getString(SrecommendationSnippet);
                            }

                            if (u < (updates.length() - 1)) {
                                update += ", ";
                            }
                        }
                    }
                }
            } else if (LinkedIn_UpdateTypes.SHAR.name().equals(updateType) && friendObj.has(ScurrentShare)) {
                JSONObject currentShare = friendObj.getJSONObject(ScurrentShare);

                if (currentShare.has(Scomment)) {
                    update = currentShare.getString(Scomment);
                }
            }

            long date = item.getLong(Stimestamp);
            String sid = item.has(SupdateKey) ? item.getString(SupdateKey) : null;
            String esid = friendObj.getString(Sid);
            String friend = friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName);
            int commentCount = 0;
            String notification = null;

            if (item.has(SupdateComments)) {
                JSONObject updateComments = item.getJSONObject(SupdateComments);

                if (updateComments.has(Svalues)) {
                    JSONArray commentsArray = updateComments.getJSONArray(Svalues);
                    commentCount = commentsArray.length();

                    if (!notificationSids.contains(sid) && (commentCount > 0)) {
                        // default hasCommented to whether or not these comments are for the own user's status
                        boolean hasCommented = notification != null || esid.equals(mAccountEsid);

                        for (int c2 = 0; c2 < commentCount; c2++) {
                            JSONObject commentObj = commentsArray.getJSONObject(c2);

                            if (commentObj.has(Sperson)) {
                                JSONObject c4 = commentObj.getJSONObject(Sperson);

                                if (c4.getString(Sid).equals(mAccountEsid)) {
                                    if (!hasCommented) {
                                        // the user has commented on this thread, notify any updates
                                        hasCommented = true;
                                    }

                                    // clear any notifications, as the user is already aware
                                    if (notification != null) {
                                        notification = null;
                                    }
                                } else if (hasCommented) {
                                    // don't notify about user's own comments
                                    // send the parent comment sid
                                    notification = String
                                            .format(getString(R.string.friendcommented), c4.getString(SfirstName) + " " + c4.getString(SlastName));
                                }
                            }
                        }
                    }
                }
            }

            if (doNotify && notification != null) {
                // new notification
                addNotification(sid, esid, friend, update, date, account, notification);
                updateNotificationMessage(notificationMessage, notification);
            }

            addStatusItem(date,
                    friend,
                    display_profile && friendObj.has(SpictureUrl) ? friendObj.getString(SpictureUrl) : null,
                    (item.has(SisCommentable) && item.getBoolean(SisCommentable) ? String
                            .format(getString(R.string.messageWithCommentCount), update, commentCount) : update),
                    time24hr,
                    appWidgetId,
                    account,
                    sid,
                    esid,
                    new ArrayList<String[]>()
            );
        }
    }

    @Nullable
    @Override
    public void getNotificationMessage(long account, String[] notificationMessage) {
        // NO-OP
    }

    @Override
    public void getNotifications(long account, String[] notificationMessage) {
        Cursor currentNotifications = getContentResolver().query(Notifications.getContentUri(mContext),
                new String[] { Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID },
                Notifications.ACCOUNT + "=?", new String[] { Long.toString(account) }, null);

        if (currentNotifications.moveToFirst()) {
            Set<String> notificationSids = new HashSet<>();

            // loop over notifications
            while (!currentNotifications.isAfterLast()) {
                long notificationId = currentNotifications.getLong(0);
                String sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(1));
                long updated = currentNotifications.getLong(2);
                boolean cleared = currentNotifications.getInt(3) == 1;
                // store sids, to avoid duplicates when requesting the latest feed
                notificationSids.add(sid);
                // get comments for current notifications
                Request request = signRequest(new Request.Builder()
                        .url(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, sid)));
                String response = SonetHttpClient.getResponse(request);

                if (!TextUtils.isEmpty(response)) {
                    // check for a newer post, if it's the user's own, then set CLEARED=0
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.has(S_total) && (jsonResponse.getInt(S_total) != 0)) {
                            JSONArray comments = jsonResponse.getJSONArray(Svalues);
                            int i2 = comments.length();
                            if (i2 > 0) {
                                for (int i = 0; i < i2; i++) {
                                    JSONObject comment = comments.getJSONObject(i);
                                    long created_time = comment.getLong(Stimestamp);
                                    if (created_time > updated) {
                                        // new comment
                                        ContentValues values = new ContentValues();
                                        values.put(Notifications.UPDATED, created_time);
                                        JSONObject person = comment.getJSONObject(Sperson);
                                        if (mAccountEsid.equals(person.getString(Sid))) {
                                            // user's own comment, clear the notification
                                            values.put(Notifications.CLEARED, 1);
                                        } else if (cleared) {
                                            values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented),
                                                    person.getString(SfirstName) + " " + person.getString(SlastName)));
                                            values.put(Notifications.CLEARED, 0);
                                        } else {
                                            values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented),
                                                    person.getString(SfirstName) + " " + person.getString(SlastName) + " and others"));
                                        }
                                        getContentResolver().update(Notifications.getContentUri(mContext), values, Notifications._ID + "=?",
                                                new String[] { Long.toString(notificationId) });
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
                    }
                }
                currentNotifications.moveToNext();
            }
            // check the latest feed
            Request request = signRequest(new Request.Builder()
                    .url(String.format(LINKEDIN_UPDATES, LINKEDIN_BASE_URL)));
            String response = SonetHttpClient.getResponse(request);

            if (!TextUtils.isEmpty(response)) {
                try {
                    JSONArray jarr = new JSONObject(response).getJSONArray(Svalues);
                    // if there are updates, clear the cache
                    int d2 = jarr.length();

                    if (d2 > 0) {
                        for (int d = 0; d < d2; d++) {
                            JSONObject o = jarr.getJSONObject(d);
                            String sid = o.getString(SupdateKey);

                            // if already notified, ignore
                            if (!notificationSids.contains(sid)) {
                                String updateType = o.getString(SupdateType);
                                JSONObject updateContent = o.getJSONObject(SupdateContent);

                                if (LinkedIn_UpdateTypes.contains(updateType) && updateContent.has(Sperson)) {
                                    JSONObject f = updateContent.getJSONObject(Sperson);

                                    if (f.has(SfirstName) && f.has(SlastName) && f.has(Sid) && o.has(SupdateComments)) {
                                        JSONObject updateComments = o.getJSONObject(SupdateComments);

                                        if (updateComments.has(Svalues)) {
                                            String notification = null;
                                            String esid = f.getString(Sid);
                                            JSONArray comments = updateComments.getJSONArray(Svalues);
                                            int commentCount = comments.length();

                                            // notifications
                                            if (commentCount > 0) {
                                                // default hasCommented to whether or not these comments are for the own user's status
                                                boolean hasCommented = notification != null || esid.equals(mAccountEsid);

                                                for (int c2 = 0; c2 < commentCount; c2++) {
                                                    JSONObject c3 = comments.getJSONObject(c2);

                                                    if (c3.has(Sperson)) {
                                                        JSONObject c4 = c3.getJSONObject(Sperson);

                                                        if (c4.getString(Sid).equals(mAccountEsid)) {
                                                            if (!hasCommented) {
                                                                // the user has commented on this thread, notify any updates
                                                                hasCommented = true;
                                                            }

                                                            // clear any notifications, as the user is already aware
                                                            if (notification != null) {
                                                                notification = null;
                                                            }
                                                        } else if (hasCommented) {
                                                            // don't notify about user's own comments
                                                            // send the parent comment sid
                                                            notification = String.format(getString(R.string.friendcommented),
                                                                    c4.getString(SfirstName) + " " + c4.getString(SlastName));
                                                        }
                                                    }
                                                }
                                            }

                                            if (notification != null) {
                                                String update = LinkedIn_UpdateTypes.getMessage(updateType);

                                                if (LinkedIn_UpdateTypes.APPS.name().equals(updateType)) {
                                                    if (f.has(SpersonActivities)) {
                                                        JSONObject personActivities = f.getJSONObject(SpersonActivities);

                                                        if (personActivities.has(Svalues)) {
                                                            JSONArray updates = personActivities.getJSONArray(Svalues);

                                                            for (int u = 0, u2 = updates.length(); u < u2; u++) {
                                                                update += updates.getJSONObject(u).getString(Sbody);

                                                                if (u < (updates.length() - 1)) {
                                                                    update += ", ";
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else if (LinkedIn_UpdateTypes.CONN.name().equals(updateType)) {
                                                    if (f.has(Sconnections)) {
                                                        JSONObject connections = f.getJSONObject(Sconnections);

                                                        if (connections.has(Svalues)) {
                                                            JSONArray updates = connections.getJSONArray(Svalues);

                                                            for (int u = 0, u2 = updates.length(); u < u2; u++) {
                                                                update += updates.getJSONObject(u).getString(SfirstName) + " " + updates
                                                                        .getJSONObject(u).getString(SlastName);

                                                                if (u < (updates.length() - 1)) {
                                                                    update += ", ";
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else if (LinkedIn_UpdateTypes.JOBP.name().equals(updateType)) {
                                                    if (updateContent.has(Sjob) && updateContent.getJSONObject(Sjob).has(Sposition) && updateContent
                                                            .getJSONObject(Sjob).getJSONObject(Sposition).has(Stitle)) {
                                                        update += updateContent.getJSONObject(Sjob).getJSONObject(Sposition).getString(Stitle);
                                                    }
                                                } else if (LinkedIn_UpdateTypes.JGRP.name().equals(updateType)) {
                                                    if (f.has(SmemberGroups)) {
                                                        JSONObject memberGroups = f.getJSONObject(SmemberGroups);

                                                        if (memberGroups.has(Svalues)) {
                                                            JSONArray updates = memberGroups.getJSONArray(Svalues);

                                                            for (int u = 0, u2 = updates.length(); u < u2; u++) {
                                                                update += updates.getJSONObject(u).getString(Sname);

                                                                if (u < (updates.length() - 1)) {
                                                                    update += ", ";
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else if (LinkedIn_UpdateTypes.PREC.name().equals(updateType)) {
                                                    if (f.has(SrecommendationsGiven)) {
                                                        JSONObject recommendationsGiven = f.getJSONObject(SrecommendationsGiven);

                                                        if (recommendationsGiven.has(Svalues)) {
                                                            JSONArray updates = recommendationsGiven.getJSONArray(Svalues);

                                                            for (int u = 0, u2 = updates.length(); u < u2; u++) {
                                                                JSONObject recommendation = updates.getJSONObject(u);
                                                                JSONObject recommendee = recommendation.getJSONObject(Srecommendee);

                                                                if (recommendee.has(SfirstName)) {
                                                                    update += recommendee.getString(SfirstName);
                                                                }

                                                                if (recommendee.has(SlastName)) {
                                                                    update += recommendee.getString(SlastName);
                                                                }

                                                                if (recommendation.has(SrecommendationSnippet)) {
                                                                    update += ":" + recommendation.getString(SrecommendationSnippet);
                                                                }

                                                                if (u < (updates.length() - 1)) {
                                                                    update += ", ";
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else if (LinkedIn_UpdateTypes.SHAR.name().equals(updateType) && f.has(ScurrentShare)) {
                                                    JSONObject currentShare = f.getJSONObject(ScurrentShare);

                                                    if (currentShare.has(Scomment)) {
                                                        update = currentShare.getString(Scomment);
                                                    }
                                                }

                                                // new notification
                                                addNotification(sid, esid, f.getString(SfirstName) + " " + f.getString(SlastName), update,
                                                        o.getLong(Stimestamp), account, notification);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
                }
            }
        }

        currentNotifications.close();
    }

    @Override
    public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags) {
        Request request = signRequest(new Request.Builder()
                .url(String.format(LINKEDIN_POST, LINKEDIN_BASE_URL))
                .addHeader("Content-Type", "application/xml")
                .post(RequestBody.create(MediaType.parse("application/json"), String.format(LINKEDIN_POST_BODY, "", message))));
        return SonetHttpClient.request(request);
    }

    @Nullable
    private JSONObject getStatus(String statusId) {
        Request request = signRequest(new Request.Builder()
                .url(String.format(LINKEDIN_UPDATE, LINKEDIN_BASE_URL, statusId)));
        String response = SonetHttpClient.getResponse(request);

        if (response != null) {
            try {
                return new JSONObject(response);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }
        }

        return null;
    }

    @Override
    public boolean isLikeable(String statusId) {
        return isLikeable(getStatus(statusId));
    }

    private boolean isLikeable(@Nullable JSONObject jsonStatus) {
        return jsonStatus != null && jsonStatus.has(IS_LIKABLE);
    }

    @Override
    public boolean isLiked(String statusId, String accountId) {
        JSONObject jsonStatus = getStatus(statusId);

        if (jsonStatus != null && isLikeable(jsonStatus)) {
            try {
                return jsonStatus.has(IS_LIKED) && jsonStatus.getBoolean(IS_LIKED);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }
        }

        return false;
    }

    @Override
    public boolean likeStatus(String statusId, String accountId, boolean doLike) {
        Request request = signRequest(new Request.Builder()
                .url(String.format(LINKEDIN_IS_LIKED, LINKEDIN_BASE_URL, statusId))
                .addHeader("Content-Type", "application/xml")
                .put(RequestBody.create(MediaType.parse("application/xml"), String.format(LINKEDIN_LIKE_BODY, Boolean.toString(doLike)))));
        return SonetHttpClient.request(request);
    }

    @Override
    public String getLikeText(boolean isLiked) {
        return getString(isLiked ? R.string.unlike : R.string.like);
    }

    @Override
    public boolean isCommentable(String statusId) {
        JSONObject jsonStatus = getStatus(statusId);

        if (jsonStatus != null && jsonStatus.has(IS_COMMENTABLE)) {
            try {
                return jsonStatus.getBoolean(IS_COMMENTABLE);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }
        }

        return false;
    }

    @Override
    public String getCommentPretext(String accountId) {
        return null;
    }

    @Override
    public void onDelete() {
    }

    @Nullable
    @Override
    public String getCommentsResponse(String statusId) {
        Request request = signRequest(new Request.Builder()
                .url(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, statusId)));
        return SonetHttpClient.getResponse(request);
    }

    @Nullable
    @Override
    public JSONArray parseComments(@NonNull String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);

        if (jsonResponse.has(S_total) && (jsonResponse.getInt(S_total) > 0)) {
            return jsonResponse.getJSONArray(Svalues);
        }

        return null;
    }

    @Nullable
    @Override
    public HashMap<String, String> parseComment(@NonNull String statusId, @NonNull JSONObject jsonComment, boolean time24hr) throws JSONException {
        JSONObject person = jsonComment.getJSONObject(Sperson);
        HashMap<String, String> commentMap = new HashMap<>();
        commentMap.put(Statuses.SID, jsonComment.getString(Sid));
        commentMap.put(Entity.FRIEND, person.getString(SfirstName) + " " + person.getString(SlastName));
        commentMap.put(Statuses.MESSAGE, jsonComment.getString(Scomment));
        commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(jsonComment.getLong(Stimestamp), time24hr));
        commentMap.put(getString(R.string.like), "");
        return commentMap;
    }

    @Override
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
        return null;
    }

    @Override
    public boolean sendComment(@NonNull String statusId, @NonNull String message) {
        Request request = signRequest(new Request.Builder()
                .url(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, statusId))
                .addHeader("Content-Type", "application/xml")
                .post(RequestBody.create(MediaType.parse("application/xml"), String.format(LINKEDIN_COMMENT_BODY, message))));
        return SonetHttpClient.request(request);
    }

    @Override
    public List<HashMap<String, String>> getFriends() {
        return null;
    }

    @Override
    String getApiKey() {
        return BuildConfig.LINKEDIN_KEY;
    }

    @Override
    String getApiSecret() {
        return BuildConfig.LINKEDIN_SECRET;
    }
}
