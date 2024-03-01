package mz.app.motocom.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import mz.app.motocom.MainActivity;
import mz.app.motocom.R;

import java.io.IOException;
import java.net.Socket;

public class ClientAudioService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "VoiceCallChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        // Create notification channel
        createNotificationChannel();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            final String ipAddress = intent.getStringExtra("IP_ADDRESS");
            final int port = intent.getIntExtra("PORT", 25000);

            // Build and display the notification for the foreground service
            try {
                Notification notification = buildNotification();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startForeground(NOTIFICATION_ID, notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                                    | ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }
            } catch (Exception ignored) {
            }
            // Audio setup
            // Initialize speakers
            // Audio streaming
            Thread clientThread = new Thread(new Runnable() {
                @Override
                @SuppressLint("MissingPermission")
                public void run() {
                    AudioRecord microphone = null;
                    AudioTrack speakers = null;
                    try (Socket socket = new Socket(ipAddress, port)) {
                        Intent successIntent = new Intent("mz.app.motocom.CLIENT_CONNECTED");
                        sendBroadcast(successIntent);
                        // Audio setup
                        int sampleRate = 44100;
                        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, audioFormat);


                        microphone = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, sampleRate, AudioFormat.CHANNEL_IN_STEREO, audioFormat, bufferSize);
                        microphone.startRecording();

                        // Initialize speakers
                        speakers = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
                        speakers.play();

                        byte[] buffer = new byte[bufferSize];

                        // Audio streaming
                        while (true) {
                            int bytesRead = microphone.read(buffer, 0, bufferSize);
                            socket.getOutputStream().write(buffer, 0, bytesRead);

                            int bytesReceived = socket.getInputStream().read(buffer, 0, bufferSize);
                            speakers.write(buffer, 0, bytesReceived);
                        }

                    } catch (IOException e) {
                        Intent clientExceptionIntent = new Intent("mz.app.motocom.CLIENT_EXCEPTION");
                        sendBroadcast(clientExceptionIntent);
                    } finally {
                        if(microphone != null){
                            microphone.release();
                        }
                        if(speakers != null){
                            speakers.release();
                        }
                    }

                }
            });
            clientThread.start();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Voice Call Service", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // Build the notification for the foreground service
    private Notification buildNotification() {
        // Create intent to open the MainActivity when the notification is clicked
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE); // Use FLAG_IMMUTABLE

        // Build the notification with the PendingIntent
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MotoCom Voice Service")
                .setContentText("Voice call in progress")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent) // Set the PendingIntent to open the app
                .setAutoCancel(true); // Automatically dismiss the notification when clicked

        return builder.build();
    }
}