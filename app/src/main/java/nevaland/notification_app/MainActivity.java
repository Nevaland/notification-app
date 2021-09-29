package nevaland.notification_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = getSharedPreferences("notification_settings", MODE_PRIVATE);
        long nextNotifyTimeMillis = sharedPreferences.getLong("nextNotifyTime", Calendar.getInstance().getTimeInMillis());
        long startStareTimeMillis = sharedPreferences.getLong("startStareTime", Calendar.getInstance().getTimeInMillis());
        long endStareTimeMillis = sharedPreferences.getLong("endStareTime", Calendar.getInstance().getTimeInMillis() + 600000);
        Boolean isEnable = sharedPreferences.getBoolean("isEnable", false);

        final TimePicker startTimePicker = (TimePicker) findViewById(R.id.timePicker_start);
        final TimePicker endTimePicker = (TimePicker) findViewById(R.id.timePicker_end);
        final Switch onOffSwitch = (Switch) findViewById(R.id.switch_onOff);
        Button saveButton = (Button) findViewById(R.id.btn_save);

        setTimePickers(startTimePicker, endTimePicker, startStareTimeMillis, endStareTimeMillis);

        onOffSwitch.setChecked(isEnable);
        onOffSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Getting onOff Switch value, Saving value.
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO: Check OnOff

                int hour_24, minute;
                if (Build.VERSION.SDK_INT >= 23 ){
                    hour_24 = startTimePicker.getHour();
                    minute = startTimePicker.getMinute();
                }
                else{
                    hour_24 = startTimePicker.getCurrentHour();
                    minute = startTimePicker.getCurrentMinute();
                }

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, hour_24);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                if (calendar.before(Calendar.getInstance())) {
                    calendar.add(Calendar.DATE, 1);
                }

                Date currentDateTime = calendar.getTime();
                String date_text = new SimpleDateFormat("yyyy년 MM월 dd일 EE요일 a hh시 mm분 ", Locale.getDefault()).format(currentDateTime);
                Toast.makeText(getApplicationContext(),date_text + "으로 알람이 설정되었습니다!", Toast.LENGTH_SHORT).show();

                //  Preference에 설정한 값 저장
                SharedPreferences.Editor editor = getSharedPreferences("notification_settings", MODE_PRIVATE).edit();
                editor.putLong("nextNotifyTime", (long) calendar.getTimeInMillis());
                editor.apply();

                diaryNotification(calendar);
            }
        });

    }

    private void setTimePickers(TimePicker startTimePicker, TimePicker endTimePicker, long startStareTimeMillis, long endStareTimeMillis) {
        startTimePicker.setIs24HourView(true);
        endTimePicker.setIs24HourView(true);

        Date startStareDate = getDateByMillis(startStareTimeMillis);
        Date endStareDate = getDateByMillis(endStareTimeMillis);

        int start_hour = getHour(startStareDate);
        int start_minute = getMinute(startStareDate);
        int end_hour = getHour(endStareDate);
        int end_minute = getMinute(endStareDate);

        if (Build.VERSION.SDK_INT >= 23){
            startTimePicker.setHour(start_hour);
            startTimePicker.setMinute(start_minute);
            endTimePicker.setHour(end_hour);
            endTimePicker.setMinute(end_minute);
        }
        else{
            startTimePicker.setCurrentHour(start_hour);
            startTimePicker.setCurrentMinute(start_minute);
            endTimePicker.setCurrentHour(end_hour);
            endTimePicker.setCurrentMinute(end_minute);
        }
    }

    private int getHour(Date startStareDate) {
        SimpleDateFormat hourFormat = new SimpleDateFormat("kk", Locale.getDefault());
        return Integer.parseInt(hourFormat.format(startStareDate));
    }

    private int getMinute(Date startStareDate) {
        SimpleDateFormat minuteFormat = new SimpleDateFormat("mm", Locale.getDefault());
        return Integer.parseInt(minuteFormat.format(startStareDate));
    }

    @NonNull
    private Date getDateByMillis(long millis) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(millis);
        return calendar.getTime();
    }

    void diaryNotification(Calendar calendar)
    {
//        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        Boolean dailyNotify = sharedPref.getBoolean(SettingsActivity.KEY_PREF_DAILY_NOTIFICATION, true);
        Boolean dailyNotify = true; // 무조건 알람을 사용

        PackageManager pm = this.getPackageManager();
        ComponentName receiver = new ComponentName(this, DeviceBootReceiver.class);
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);


        // 사용자가 매일 알람을 허용했다면
        if (dailyNotify) {


            if (alarmManager != null) {

                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, pendingIntent);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            }

            // 부팅 후 실행되는 리시버 사용가능하게 설정
            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

        }
//        else { // Disable Daily Notifications
//            if (PendingIntent.getBroadcast(this, 0, alarmIntent, 0) != null && alarmManager != null) {
//                alarmManager.cancel(pendingIntent);
//                //Toast.makeText(this,"Notifications were disabled",Toast.LENGTH_SHORT).show();
//            }
//            pm.setComponentEnabledSetting(receiver,
//                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//                    PackageManager.DONT_KILL_APP);
//        }
    }


//    public void NotificationSomethings() {
//        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
//
//        Intent notificationIntent = new Intent(this, ResultActivity.class);
//        notificationIntent.putExtra("notificationId", count); // 전달할 값
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK) ;
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE); // Can't Use FLAG_UPDATE_CURRENT on version 31 and above
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground)) // BitMap 이미지 요구
//                .setContentTitle("Notification Title")
//                .setContentText("Notification SubTitle")
//
//                // 더 많은 내용이라서 일부만 보여줘야 하는 경우 아래 주석을 제거하면 setContentText에 있는 문자열 대신 아래 문자열을 보여줌
//                // .setStyle(new NotificationCompat.BigTextStyle().bigText("더 많은 내용을 보여줘야 하는 경우..."))
//
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setContentIntent(pendingIntent) // 사용자가 Notification을 탭시 ResultActivity로 이동하도록 설정
//                .setAutoCancel(true);
//
//        // OREO API 26 이상에서는 채널 필요
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//
//            builder.setSmallIcon(R.drawable.ic_launcher_foreground); // mipmap 사용시 Oreo 이상에서 시스템 UI 에러남
//            CharSequence channelName  = "노티페케이션 채널";
//            String description = "오레오 이상을 위한 것임";
//            int importance = NotificationManager.IMPORTANCE_HIGH;
//
//            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName , importance);
//            channel.setDescription(description);
//
//            // Notification 채널을 시스템에 등록
//            assert notificationManager != null;
//            notificationManager.createNotificationChannel(channel);
//            Log.d("NEVA", "OVER OREO!");
//        } else {
//            builder.setSmallIcon(R.mipmap.ic_launcher); // Oreo 이하에서 mipmap 사용하지 않으면 Couldn't create icon: StatusBarIcon 에러남
//            Log.d("NEVA", "UNDER OREO!");
//        }
//
//        assert notificationManager != null;
//        notificationManager.notify(1234, builder.build()); // 고유숫자로 Notification 동작시킴
//    }
}