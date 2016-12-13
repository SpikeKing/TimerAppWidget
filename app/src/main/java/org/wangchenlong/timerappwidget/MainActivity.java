package org.wangchenlong.timerappwidget;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;

import static org.wangchenlong.timerappwidget.NotificationReceiver.sId;

/**
 * 主页, 直接启动通知栏
 *
 * @author C.L. Wang
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        initNotification(context); // 初始化通知栏
    }

    /**
     * 初始化通知栏
     *
     * @param context 上下文
     */
    private void initNotification(Context context) {
        NotificationCompat.Builder builder = NotificationReceiver.getNotification(context);
        Notification notification = builder.build();
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(sId, notification);
    }
}
