package com.travelstudio.bgmcutter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.OutputStream;

/** 음악 파일을 음악/벨소리/알람/알림 폴더에 저장하고, 원하면 기본 소리로 지정 */
@CapacitorPlugin(name = "Ringtone")
public class RingtonePlugin extends Plugin {

    @PluginMethod
    public void canWriteSettings(PluginCall call) {
        JSObject r = new JSObject();
        r.put("granted", Settings.System.canWrite(getContext()));
        call.resolve(r);
    }

    @PluginMethod
    public void openWriteSettings(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + getContext().getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }

    @PluginMethod
    public void save(PluginCall call) {
        String data = call.getString("data");
        String name = call.getString("name", "bgm.mp3");
        String mime = call.getString("mime", "audio/mpeg");
        String kind = call.getString("kind", "music");
        boolean setDefault = Boolean.TRUE.equals(call.getBoolean("setDefault", false));
        if (data == null) {
            call.reject("저장할 데이터가 없어요");
            return;
        }
        try {
            byte[] bytes = Base64.decode(data, Base64.DEFAULT);
            String dir;
            int type = -1;
            switch (kind) {
                case "ringtone":
                    dir = Environment.DIRECTORY_RINGTONES;
                    type = RingtoneManager.TYPE_RINGTONE;
                    break;
                case "alarm":
                    dir = Environment.DIRECTORY_ALARMS;
                    type = RingtoneManager.TYPE_ALARM;
                    break;
                case "notification":
                    dir = Environment.DIRECTORY_NOTIFICATIONS;
                    type = RingtoneManager.TYPE_NOTIFICATION;
                    break;
                default:
                    dir = Environment.DIRECTORY_MUSIC + "/BGM커터";
            }
            ContentResolver cr = getContext().getContentResolver();
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, dir);
            v.put(MediaStore.Audio.Media.IS_RINGTONE, type == RingtoneManager.TYPE_RINGTONE);
            v.put(MediaStore.Audio.Media.IS_ALARM, type == RingtoneManager.TYPE_ALARM);
            v.put(MediaStore.Audio.Media.IS_NOTIFICATION, type == RingtoneManager.TYPE_NOTIFICATION);
            v.put(MediaStore.Audio.Media.IS_MUSIC, type < 0);
            v.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = cr.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, v);
            if (uri == null) {
                call.reject("저장 공간에 파일을 만들 수 없어요");
                return;
            }
            try (OutputStream os = cr.openOutputStream(uri)) {
                os.write(bytes);
            }
            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            cr.update(uri, done, null, null);

            boolean applied = false;
            if (setDefault && type >= 0) {
                if (!Settings.System.canWrite(getContext())) {
                    call.reject("‘시스템 설정 변경’ 허용이 필요해요");
                    return;
                }
                RingtoneManager.setActualDefaultRingtoneUri(getContext(), type, uri);
                applied = true;
            }
            JSObject r = new JSObject();
            r.put("uri", uri.toString());
            r.put("applied", applied);
            call.resolve(r);
        } catch (Exception e) {
            call.reject("저장 실패: " + e.getMessage());
        }
    }
}
