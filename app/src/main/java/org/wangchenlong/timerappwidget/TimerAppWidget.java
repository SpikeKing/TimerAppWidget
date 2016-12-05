package org.wangchenlong.timerappwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;

/**
 * 实时更新数据的小控件, 每隔2秒更换图片, 显示时间.
 * <p>
 * Created by wangchenlong on 16/12/2.
 */
public class TimerAppWidget extends AppWidgetProvider {

    // 更新小部件的广播
    private static final String CHANGE_STATE = "org.wangchenlong.timerappwidget.action.CHANGE_STATE";

    @DrawableRes
    private static final int[] mAvatars = new int[]{
            R.drawable.avatar_tiffany,
            R.drawable.avatar_jessica,
            R.drawable.avatar_soo_young,
            R.drawable.avatar_yoo_na
    };

    @StringRes
    private static final int[] mNames = new int[]{
            R.string.tiffany,
            R.string.jessica,
            R.string.soo_young,
            R.string.yoo_na
    };

    private long mLastTime = 0L;
    private static boolean sIsUpdate = false;

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals(CHANGE_STATE)) {
            sIsUpdate = !sIsUpdate;

            if (sIsUpdate) {
                startUpdate(context); // 开始更新
            } else {
                stopUpdate(context); // 结束更新
            }
        }
    }

    /**
     * 启动更新小部件的时间
     *
     * @param context 上下文
     */
    public void startUpdate(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // 获取当前的组件
        ComponentName widget = new ComponentName(context, TimerAppWidget.class);
        // 获取系统的AppWidgetManager
        AppWidgetManager awm = AppWidgetManager.getInstance(context);

        // 设置关闭显示
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.app_widget_timer);
        rv.setTextViewText(R.id.widget_b_control, context.getString(R.string.stop));

        // 更新页面组件
        awm.updateAppWidget(widget, rv);
        int appWidgetIds[] = awm.getAppWidgetIds(widget);
        // 更新数据
        awm.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_tv_text);

        Intent alertIntent = new Intent(context, TimerAppWidget.class);
        alertIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE); // 设置更新活动
        alertIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds); // 设置当前部件的ID
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, alertIntent,
                FLAG_CANCEL_CURRENT); // 取消前一个更新

        // 设置更新
        am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 1000, pi);
    }

    /**
     * 停止更新小部件的时间
     *
     * @param context 上下文
     */
    public void stopUpdate(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // 获取当前的组件
        ComponentName widget = new ComponentName(context, TimerAppWidget.class);
        // 获取系统的AppWidgetManager
        AppWidgetManager awm = AppWidgetManager.getInstance(context);

        // 设置关闭显示
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.app_widget_timer);
        rv.setTextViewText(R.id.widget_b_control, context.getString(R.string.start));

        // 更新页面组件
        awm.updateAppWidget(widget, rv);
        int appWidgetIds[] = awm.getAppWidgetIds(widget);
        // 更新数据
        awm.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_tv_text);

        Intent alertIntent = new Intent(context, TimerAppWidget.class);
        alertIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE); // 设置更新活动
        alertIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds); // 设置当前部件的ID
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, alertIntent,
                FLAG_CANCEL_CURRENT); // 取消前一个更新

        // 取消更新
        am.cancel(pi);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.app_widget_timer);
        ComponentName widget = new ComponentName(context, TimerAppWidget.class);

        Date date = new Date();

        // 设置文字
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
        rv.setTextViewText(R.id.widget_tv_text, sdf.format(date));

        // 更换图片
        long seconds = TimeUnit.MILLISECONDS.toSeconds(date.getTime());
        long interval = (seconds - mLastTime) / 5L; // 每隔5秒更换图片与图片文字
        if (mLastTime == 0L || interval == 0L) {
            rv.setImageViewResource(R.id.widget_tv_image, mAvatars[(int) interval % 4]);
            rv.setTextViewText(R.id.widget_tv_image_text, context.getString(mNames[(int) interval % 4]));

            mLastTime = seconds;
        }

        // 开启或关闭时间的控制
        Intent intent = new Intent(CHANGE_STATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, FLAG_CANCEL_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_b_control, pi);

        // 更新部件
        appWidgetManager.updateAppWidget(widget, rv);
    }
}
