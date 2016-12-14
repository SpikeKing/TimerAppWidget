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
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * 实时更新数据的小控件, 每隔2秒更换图片, 显示时间.
 * <p>
 * Created by wangchenlong on 16/12/2.
 */
public class TimerAppWidget extends AppWidgetProvider {

    // 更新小插件的广播
    public static final String CHANGE_STATE = "org.wangchenlong.timerappwidget.action.CHANGE_STATE";

    // 每次更新都会创建新的实例, 只能使用静态变量
    private static boolean sIsUpdate = false; // 是否启动更新时间
    private static int sImageIndex = 0;
    private static long sUpdateImageLastTime = 0L; // 上次更新图片的时间

    public static int IMAGE_COUNT = 4; // 图片数量

    @DrawableRes
    public static final int[] mAvatars = new int[]{
            R.drawable.avatar_tiffany,
            R.drawable.avatar_jessica,
            R.drawable.avatar_soo_young,
            R.drawable.avatar_yoo_na
    };

    @StringRes
    public static final int[] mNames = new int[]{
            R.string.tiffany,
            R.string.jessica,
            R.string.soo_young,
            R.string.yoo_na
    };

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
     * 获取需要更新的延迟消息
     *
     * @param context 上下文
     * @return 延迟消息
     */
    private PendingIntent getUpdateIntent(Context context, boolean isStart) {
        // 获取当前的组件
        ComponentName widget = new ComponentName(context, TimerAppWidget.class);

        // 获取布局, 并设置关闭显示
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.app_widget_timer);
        if (isStart) {
            rv.setTextViewText(R.id.widget_b_control, context.getString(R.string.stop));
        } else {
            rv.setTextViewText(R.id.widget_b_control, context.getString(R.string.start));
        }

        // 获取系统的AppWidgetManager
        AppWidgetManager awm = AppWidgetManager.getInstance(context);

        // 更新页面组件
        awm.updateAppWidget(widget, rv);
        int appWidgetIds[] = awm.getAppWidgetIds(widget);

        Intent alertIntent = new Intent(context, TimerAppWidget.class);
        alertIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE); // 设置更新活动
        alertIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds); // 设置当前插件的ID
        return PendingIntent.getBroadcast(context, 0, alertIntent,
                FLAG_CANCEL_CURRENT); // 取消前一个更新
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.app_widget_timer);
        ComponentName widget = new ComponentName(context, TimerAppWidget.class);

        Date date = new Date();

        // 设置文字
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
        rv.setTextViewText(R.id.widget_tv_text, sdf.format(date)); // 设置文字

        // 更换图片
        long seconds = TimeUnit.MILLISECONDS.toSeconds(date.getTime());
        long interval = seconds - sUpdateImageLastTime; // 每隔5秒更换图片与图片文字
        if (sUpdateImageLastTime == 0 || interval % 5 == 0) {
            sImageIndex++;
            rv.setImageViewResource(R.id.widget_tv_image, mAvatars[sImageIndex % IMAGE_COUNT]); // 设置图片
            rv.setTextViewText(R.id.widget_tv_image_text, context.getString(mNames[sImageIndex % IMAGE_COUNT]));
            sUpdateImageLastTime = seconds;
        }

        // 点击头像跳转主页
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPi = PendingIntent.getActivity(context, 0, mainIntent, FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_tv_image, mainPi); // 设置点击事件

        // 开启或关闭时间的控制
        Intent changeIntent = new Intent(CHANGE_STATE);
        PendingIntent changePi = PendingIntent.getBroadcast(context, 0, changeIntent, FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_b_control, changePi);

        // 更新插件
        appWidgetManager.updateAppWidget(widget, rv);
    }
}
