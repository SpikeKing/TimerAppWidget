package org.wangchenlong.timerappwidget;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.RemoteViews;

import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private static int sId = new AtomicInteger(0).incrementAndGet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setNotification(getApplicationContext());
    }

    private void setNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.avatar_jessica);
        builder.setWhen(System.currentTimeMillis());

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_timer);
        builder.setCustomContentView(remoteViews);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL; // FLAG_AUTO_CANCEL表明当通知被用户点击时, 通知将被清除

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(sId, notification);
    }
}
