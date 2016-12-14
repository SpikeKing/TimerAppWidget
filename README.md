# 实现同步更新的通知与插件

> 欢迎Follow我的[GitHub](https://github.com/SpikeKing)

在Android中, 除了本应用的视图外, 还允许操作**远程视图(RemoteView)**. 其中包含两类实例, 一类是**通知(Notification)**, 一类是**插件(Widget)**, 这些都是附着于系统中, 通过广播更新页面. 系统为了避免频繁更新, 规定最低频率, 如果需要饶过这个机制, 则必须使用**定时器(Alarm)**. 本文连接定时器与远程视图, 达到实时更新的目的, 两者都会涉及PendingIntent的使用.

---

## 插件

**插件(Widget)**实现两种功能, 一种是更新当前时间, 一种是更新启动状态, 需要注册两个**IntentFilter**. 使用Alarm定时器更新时间, 通过按钮控制启动状态.

### 注册

**AppWidget**(插件)在本质上是**Receiver**, 注册在**AndroidManifest**中也是, 不同的是定义特定的``intent-filter``, 即``APPWIDGET_UPDATE``. 

``` xml
<action android:name="android.appwidget.action.APPWIDGET_UPDATE"/>
```

并指定``meta-data``的``name``与``resource``. ``name``是固定的, ``resource``指明所使用的资源信息.

``` xml
<receiver android:name=".TimerAppWidget">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE"/>
        <action android:name="org.wangchenlong.timerappwidget.action.CHANGE_STATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/timer_widget_provider"/>
</receiver>
```

AppWidget的描述信息.

``` xml
<appwidget-provider
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:initialLayout="@layout/app_widget_timer"
    android:minHeight="52dp"
    android:minWidth="260dp"
    android:previewImage="@drawable/preview_widget"
    android:resizeMode="horizontal|vertical"/>
```

**initialLayout**: Widget的布局(Layout)文件.

**minWidth** & **minHeight**: 定义Widget的最小宽度和高度, 当数值不是桌面cell的整数倍时, 宽高会被增至最接近的cell大小.

**previewImage**: 当用户选择添加Widget时的预览图片. 如果未定义, 则展示应用的登录图标.

**resizeMode**: 在水平和竖直方向是否允许调整大小, 值可选: horizontal(水平方向), vertical(竖直方向), none(不允许调整). 

### 定义

**AppWidget**继承于``AppWidgetProvider``, 而最终继承于``BroadcastReceiver``. ``onUpdate``负责更新显示数据.

``` java
public class TimerAppWidget extends AppWidgetProvider {
    // 每次更新都会创建新的实例, 只能使用静态变量
    private static boolean sIsUpdate = false; // 是否启动更新时间
    private static int sImageIndex = 0;
    private static long sUpdateImageLastTime = 0L; // 上次更新图片的时间

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

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
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
```

获取当前的远程视图(``RemoteViews``)和组件信息(``ComponentName``), 最终用于更新插件, 即``updateAppWidget``.

``` java
RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.app_widget_timer);
ComponentName widget = new ComponentName(context, TimerAppWidget.class);
// ...
appWidgetManager.updateAppWidget(widget, rv);
```

在``RemoteViews``中设置显示文字(``TextView``)与图片(``ImageView``)的控件, 或者点击事件. 注意, 事件触发使用``PendingIntent``广播.

``` java
rv.setTextViewText(R.id.widget_tv_text, sdf.format(date)); // 设置文字
rv.setImageViewResource(R.id.widget_tv_image, mAvatars[(int) interval % 4]); // 设置图片

Intent mainIntent = new Intent(context, MainActivity.class);
PendingIntent mainPi = PendingIntent.getBroadcast(context, 0, mainIntent, FLAG_CANCEL_CURRENT);
rv.setOnClickPendingIntent(R.id.widget_tv_image, mainPi); // 设置点击事件
```

### 更新

更新的**延迟消息**(``PendingIntent``), 获取组件与布局, 系统管理器更新插件, 获取插件ID组; 使用系统默认的插件活动``ACTION_APPWIDGET_UPDATE``与参数``EXTRA_APPWIDGET_IDS``设置延迟消息, 供**定时器**(``AlarmManager``)使用.

``` java
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
```

启动或关闭定时器, 每秒更新一次.

``` java
public void startUpdate(Context context) {
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    // 设置更新
    am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
            1000, getUpdateIntent(context, true));
}

public void stopUpdate(Context context) {
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    // 取消更新
    am.cancel(getUpdateIntent(context, false));
}
```

---

## 通知

通知实现与插件类似, 使用相同的控制广播, 保持同步.

### 定义

通知栏使用自定义布局, 设置头像跳转事件与状态修改事件, 发送相应的**PendingIntent**广播, 注意状态使用**FLAG_UPDATE_CURRENT**, 更新当前重复的Intent. 当与Widget的PendingIntent重复时, 进行相应的替换.

``` java
public static NotificationCompat.Builder getNotification(Context context) {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
    builder.setSmallIcon(R.drawable.avatar_jessica);
    builder.setWhen(System.currentTimeMillis());
    builder.setOngoing(true); // 始终存在

    // 开启或关闭时间的控制
    RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.notification_timer);

    // 点击更新状态按钮
    Intent changeIntent = new Intent(CHANGE_STATE);
    PendingIntent changePi = PendingIntent.getBroadcast(context, 0, changeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);

    // 点击头像跳转主页
    Intent mainIntent = new Intent(context, MainActivity.class);
    PendingIntent mainPi = PendingIntent.getActivity(context, 0, mainIntent, FLAG_UPDATE_CURRENT);

    rv.setOnClickPendingIntent(R.id.widget_tv_image, mainPi); // 设置头像
    rv.setOnClickPendingIntent(R.id.widget_b_control, changePi); // 设置更新

    builder.setCustomContentView(rv);

    return builder;
}
```

### 更新

收到通知后, 实时更新当前时间, 每隔5秒循环更新图片, 由于远程视图, 每次都会创建实例, 因此参数需要使用静态变量.

``` java
private void updateData(Context context) {
    NotificationCompat.Builder builder = getNotification(context);

    // 获取布局, 并设置关闭显示
    RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.notification_timer);

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

    builder.setCustomContentView(rv);
    Notification notification = builder.build();
    NotificationManager manager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);
    manager.notify(sId, notification);
}
```

---

至此, 定时更新的插件与通知已经完成, 注意PendingIntent与定时器的使用方式. 这个实例对于开发插件与通知而言, 已经完全足够, 抛砖引玉.

OK, that's all! Enjoy it!
