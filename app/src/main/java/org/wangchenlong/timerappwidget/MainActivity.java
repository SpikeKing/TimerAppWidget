package org.wangchenlong.timerappwidget;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static org.wangchenlong.timerappwidget.TimerAppWidget.CHANGE_STATE;
import static org.wangchenlong.timerappwidget.TimerAppWidget.IMAGE_COUNT;
import static org.wangchenlong.timerappwidget.TimerAppWidget.mAvatars;
import static org.wangchenlong.timerappwidget.TimerAppWidget.mNames;

/**
 * 主页, 直接启动通知栏
 *
 * @author C.L. Wang
 */
public class MainActivity extends AppCompatActivity {

    private static int sId = new AtomicInteger(0).incrementAndGet();

    private boolean sIsUpdate = false; // 是否启动更新时间
    private long mUpdateImageLastTime = 0L; // 上次更新图片的时间
    private int mImageIndex = 0; // 图片位置

    // 修改通知栏状态
    public static final String NOTIFICATION_STATE = "org.wangchenlong.timerappwidget.action.NOTIFICATION_STATE";

    private NotificationCompat.Builder mBuilder; // 构造器

    @Override public Intent getIntent() {
        return super.getIntent();
    }

    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            // 获取布局, 并设置关闭显示
            RemoteViews rv = new RemoteViews(getPackageName(), R.layout.notification_timer);

            Date date = new Date();

            // 设置文字
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
            rv.setTextViewText(R.id.widget_tv_text, sdf.format(date)); // 设置文字

            // 更换图片
            long seconds = TimeUnit.MILLISECONDS.toSeconds(date.getTime());
            long interval = seconds - mUpdateImageLastTime; // 每隔5秒更换图片与图片文字
            if (mUpdateImageLastTime == 0 || interval % 5 == 0) {
                mImageIndex++;
                rv.setImageViewResource(R.id.widget_tv_image, mAvatars[mImageIndex % IMAGE_COUNT]); // 设置图片
                rv.setTextViewText(R.id.widget_tv_image_text, context.getString(mNames[mImageIndex % IMAGE_COUNT]));
                mUpdateImageLastTime = seconds;
            }

            mBuilder.setCustomContentView(rv);
            Notification notification = mBuilder.build();
            NotificationManager manager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(sId, notification);
        }
    };

    private BroadcastReceiver mStartReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CHANGE_STATE)) {
                sIsUpdate = !sIsUpdate;
                if (sIsUpdate) {
                    startUpdate(context); // 开始更新
                } else {
                    stopUpdate(context); // 结束更新
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setNotification(getApplicationContext());

        registerReceiver(mUpdateReceiver, new IntentFilter(NOTIFICATION_STATE));
        registerReceiver(mStartReceiver, new IntentFilter(CHANGE_STATE));
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUpdateReceiver);
        unregisterReceiver(mStartReceiver);
    }

    /**
     * 启动更新小插件的时间
     *
     * @param context 上下文
     */
    public void startUpdate(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // 设置更新
        am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
                1000, getUpdateIntent(context, true));
    }

    /**
     * 停止更新小插件的时间
     *
     * @param context 上下文
     */
    public void stopUpdate(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // 取消更新
        am.cancel(getUpdateIntent(context, false));
    }

    /**
     * 设置通知栏
     *
     * @param context 上下文
     */
    private void setNotification(Context context) {
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(R.drawable.avatar_jessica);
        mBuilder.setWhen(System.currentTimeMillis());
        mBuilder.setOngoing(true); // 始终存在

        // 开启或关闭时间的控制
        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.notification_timer);
        Intent changeIntent = new Intent(CHANGE_STATE);
        PendingIntent changePi = PendingIntent.getBroadcast(context, 0, changeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_b_control, changePi);

        mBuilder.setCustomContentView(rv);
        Notification notification = mBuilder.build();
        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(sId, notification);
    }

    /**
     * 获取需要更新的延迟消息
     *
     * @param context 上下文
     * @return 延迟消息
     */
    private PendingIntent getUpdateIntent(Context context, boolean isStart) {
        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.notification_timer);
        if (isStart) {
            rv.setTextViewText(R.id.widget_b_control, context.getString(R.string.stop));
        } else {
            rv.setTextViewText(R.id.widget_b_control, context.getString(R.string.start));
        }

        mBuilder.setCustomContentView(rv);
        Notification notification = mBuilder.build();
        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(sId, notification);

        Intent alertIntent = new Intent(NOTIFICATION_STATE); // 设置更新活动
        return PendingIntent.getBroadcast(context, 0, alertIntent, FLAG_CANCEL_CURRENT); // 取消前一个更新
    }


}
