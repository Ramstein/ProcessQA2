package net.gotev.speechdemo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.RecognizerIntent;
import android.speech.tts.Voice;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SupportedLanguagesListener;
import net.gotev.speech.TextToSpeechCallback;
import net.gotev.speech.UnsupportedReason;
import net.gotev.speech.ui.SpeechProgressView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static net.gotev.speechdemo.Utils.SpeakPromptly;
import static net.gotev.speechdemo.Utils.isBluetoothHeadsetConnected;
import static net.gotev.speechdemo.Utils.onSpeechToTextQuestion;
import static net.gotev.speechdemo.Utils.startAnsweringTheQuestions;

public class MainActivity extends AppCompatActivity implements SpeechDelegate {

    @SuppressLint("StaticFieldLeak")
    public static EditText sub_code_editText;
    @SuppressLint("StaticFieldLeak")
    public static EditText n_que_layout;
    @SuppressLint("StaticFieldLeak")
    public static ImageButton mic_btn;
    public static SpeechProgressView progress;
    @SuppressLint("StaticFieldLeak")
    public static LinearLayout linearLayout;
    @SuppressLint("StaticFieldLeak")
    public static EditText textToSpeech;
    @SuppressLint("StaticFieldLeak")
    static TextView textViewCountDown;
    @SuppressLint("StaticFieldLeak")
    static TextView textView;
    private static TimePickerFragment timePickerFragment;
    public Handler backgroundHandler;
    @SuppressLint("StaticFieldLeak")
    private TextView topTextView;
    private HandlerThread backgroundThread;

    @SuppressLint("SetTextI18n")
    public static synchronized void startTimer(long ms) {
        if (Utils.isBluetoothConnected) {
            timePickerFragment.timerRunning = true;
            timePickerFragment.countDownTimer = new CountDownTimer(ms, 1) {

                @Override
                public void onTick(long millisUntilFinished) {
                    timePickerFragment.updateCountDownText(millisUntilFinished);
                }

                @RequiresApi(api = Build.VERSION_CODES.O)
                @SuppressLint("SetTextI18n")
                @Override
                public void onFinish() {
                    timePickerFragment.timerRunning = false;
                    if (Utils.isBluetoothConnected) {
                        if (sendGetAsyncRequest.n_que_answered < Utils.n_que) {
                            String temp_txt = n_que_layout.getText().toString();
                            if (!temp_txt.equals("") & temp_txt.matches("[0-9]+")) {
                                int n = Integer.parseInt(temp_txt);
                                if (n > 0 & n < 25) {
                                    Utils.n_que = n;
                                } else {
                                    textView.setText("Going with 20 questions.");
                                }
                            } else {
                                textView.setText("Going with 20 questions.");
                            }
                            if (Utils.TimerWithGoogleWindow) {
                                Utils.SpeechToText();  // launches speech to text with a google interface window
                                Utils.processQuestionToAnswer();
                            } else {
                                onSpeechToTextQuestion();
                            }
                            timePickerFragment.nextExecution();
                        } else { // all of the question answered from lambda, start speaking them
                            Speech.getInstance().say("Answering the questions now", new TextToSpeechCallback() {
                                @Override
                                public void onStart() {
                                }

                                @Override
                                public void onCompleted() {
                                    startAnsweringTheQuestions();
                                }

                                @Override
                                public void onError() {
                                }
                            });
                        }
                    } else {
                        Log.e("isBluetoothConnected: ", "startTimer.finish() : " + Utils.isBluetoothConnected);
                    }
                }
            }.start();
            timePickerFragment.timerRunning = true;
        } else {
            textView.setText("isBluetoothConnected: " + Utils.isBluetoothConnected);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Utils.context = this;
        timePickerFragment = new TimePickerFragment();
        Utils.preferencesHandler = new PreferencesHandler();
        Speech.init(this, getPackageName(), Utils.mTttsInitListener);

        n_que_layout = findViewById(R.id.n_que_layout);
        sub_code_editText = findViewById(R.id.sub_code_editText);
        textView = findViewById(R.id.textView);
        textViewCountDown = findViewById(R.id.textViewCountDown);
        linearLayout = findViewById(R.id.linearLayout);
        topTextView = findViewById(R.id.topTextView);
        textToSpeech = findViewById(R.id.textToSpeech);
        progress = findViewById(R.id.progress);


        mic_btn = findViewById(R.id.mic_btn);
        mic_btn.setOnClickListener(view -> {
            if (Utils.isBluetoothConnected) {
                Utils.test = true;
                onSpeechToTextQuestion();
            } else {
                textView.setText("Utils.isBluetoothConnected: " + Utils.isBluetoothConnected);
            }
        });

        Button speak = findViewById(R.id.speak);
        speak.setOnClickListener(view -> onSpeakClick());

        int[] colors = {
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.holo_blue_light),
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.holo_blue_dark),
                ContextCompat.getColor(this, android.R.color.holo_blue_bright)
        };
        progress.setColors(colors);

        Button btn_schedule = findViewById(R.id.btn_schedule);
        btn_schedule.setOnClickListener(v -> {
            if (timePickerFragment.timerRunning) {
                timePickerFragment.resetTimer();
            }
            DialogFragment timePicker = new TimePickerFragment();  // calling a different package TimePickerFragment.java
            timePicker.show(getSupportFragmentManager(), "time picker");
        });

        ////////////////////// Timer
        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> timePickerFragment.resetTimer());
        timePickerFragment.updateCountDownText(timePickerFragment.beginTimerMillis);

        Button sub_code_btn = findViewById(R.id.sub_code_btn);
        sub_code_btn.setOnClickListener(view -> {
            Utils.test = true;
            String sub_code = sub_code_editText.getText().toString().toUpperCase();
            String url = Utils.createUrl(1, sub_code, "As people have said, calling the Toast initiation within onResponse() works.");
            if (Utils.API_POST) {
                Utils.sendPostRequest(url);
            } else {
//                sendGetRequest(url);
                new sendGetAsyncRequest().execute(url);
            }
            if (Utils.isBluetoothConnected) {
                startAnsweringTheQuestions();
            }
        });

        //// BluetoothReciever updating the variable Utils.isBluetoothConnected to true or false
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(Utils.BluetoothReciever, filter);

        Utils.isBluetoothConnected = isBluetoothHeadsetConnected();
        Log.e("BluetoothHeadset", "isBluetoothHeadsetConnected: " + Utils.isBluetoothConnected);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Utils.RECOGNIZERINTENTREQUESTCODE) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                Utils.SpeechRecognizerInput.add(result.get(0).toUpperCase());
                Utils.mIslistening = false;
                Log.e("onActivityResult", String.valueOf(result));
                if (Utils.test) {
                    if (Utils.SpeechRecognizerInput.get((Utils.SpeechRecognizerInput.size() - 1)).equals("")) {
                        SpeakPromptly("Not able to hear you.");
                    }
                    Utils.SpeechRecognizerInput.set((Utils.SpeechRecognizerInput.size() - 1), "");
                }
            }
        }
    }


    private void onSetSpeechToTextLanguage() {
        Speech.getInstance().getSupportedSpeechToTextLanguages(new SupportedLanguagesListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onSupportedLanguages(List<String> supportedLanguages) {
                CharSequence[] items = new CharSequence[supportedLanguages.size()];
                supportedLanguages.toArray(items);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Current language: " + Speech.getInstance().getSpeechToTextLanguage())
                        .setItems(items, (dialogInterface, i) -> {
                            Locale locale = Locale.forLanguageTag(supportedLanguages.get(i));
                            Speech.getInstance().setLocale(locale);
                            Toast.makeText(MainActivity.this, "Selected: " + items[i], Toast.LENGTH_LONG).show();
                        })
                        .setPositiveButton("Cancel", null)
                        .create()
                        .show();
            }

            @Override
            public void onNotSupported(UnsupportedReason reason) {
                switch (reason) {
                    case GOOGLE_APP_NOT_FOUND:
                        Utils.showSpeechNotSupportedDialog();
                        break;

                    case EMPTY_SUPPORTED_LANGUAGES:
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.set_stt_langs)
                                .setMessage(R.string.no_langs)
                                .setPositiveButton("OK", null)
                                .show();
                        break;
                }
            }
        });
    }

    private void onSetTextToSpeechVoice() {
        List<Voice> supportedVoices = Speech.getInstance().getSupportedTextToSpeechVoices();

        if (supportedVoices.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.set_tts_voices)
                    .setMessage(R.string.no_tts_voices)
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        CharSequence[] items = new CharSequence[supportedVoices.size()];
        Iterator<Voice> iterator = supportedVoices.iterator();
        int i = 0;

        while (iterator.hasNext()) {
            Voice voice = iterator.next();

            items[i] = voice.toString();
            i++;
        }

        new AlertDialog.Builder(this)
                .setTitle("Current: " + Speech.getInstance().getTextToSpeechVoice())
                .setItems(items, (dialogInterface, i1) -> {
                    Speech.getInstance().setVoice(supportedVoices.get(i1));
                    Toast.makeText(this, "Selected: " + items[i1], Toast.LENGTH_LONG).show();
                })
                .setPositiveButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != Utils.RECORD_AUDIO_PERMISSIONS_REQUEST) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } else {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Utils.onRecordAudioPermissionGranted();
            } else {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onSpeakClick() {
        String txt = textToSpeech.getText().toString().trim();
        if (txt.isEmpty()) {
            Toast.makeText(this, R.string.input_something, Toast.LENGTH_LONG).show();
            Utils.SpeakAnswer("One way of doing this without changing Volley's source code is to check for the response data in the VolleyError and parse it your self..");
            return;
        }
        Utils.SpeakAnswer(txt);
    }

    @Override
    public void onStartOfSpeech() {
        Utils.mIslistening = true;
    }

    @Override
    public void onSpeechRmsChanged(float value) {
        //Log.d(getClass().getSimpleName(), "Speech recognition rms is now " + value +  "dB");
    }

    @Override
    public void onSpeechResult(String result) {
        mic_btn.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.GONE);
        if (!result.isEmpty()) {
            Utils.SpeechRecognizerInput.add(result);
            Utils.mIslistening = false;
            Log.e("onSpeechResult", result);
            if (Utils.test) {
                if (Utils.SpeechRecognizerInput.get((Utils.SpeechRecognizerInput.size() - 1)).equals("")) {
                    SpeakPromptly("Not able to hear you.");
                }
                Utils.SpeechRecognizerInput.set((Utils.SpeechRecognizerInput.size() - 1), "");
            }
            String url = Utils.createUrl(0, PreferencesHandler.sub_code, result);
            if (Utils.API_POST) {
                Utils.sendPostRequest(url);
            } else {
//                sendGetRequest(url);
                new sendGetAsyncRequest().execute(url);
            }
        }
    }

    @Override
    public void onSpeechPartialResults(List<String> results) {
        topTextView.setText("");
        for (String partial : results) {
            topTextView.append(partial + " ");
        }
    }

    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("millisLeft", timePickerFragment.timeLeftInMillis);
        outState.putBoolean("timerRunning", timePickerFragment.timerRunning);
        outState.putLong("endTime", timePickerFragment.endTimeInMillis);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        timePickerFragment.timeLeftInMillis = savedInstanceState.getLong("millisLeft");
        timePickerFragment.timerRunning = savedInstanceState.getBoolean("timerRunning");
        timePickerFragment.updateCountDownText(timePickerFragment.timeLeftInMillis);
        if (timePickerFragment.timerRunning) {
            timePickerFragment.endTimeInMillis = savedInstanceState.getLong("endTime");
            timePickerFragment.timeLeftInMillis = timePickerFragment.endTimeInMillis - System.currentTimeMillis();
            startTimer(timePickerFragment.timeLeftInMillis);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Speech.getInstance().shutdown();
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Quanification Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.supportedSTTLanguages:
                onSetSpeechToTextLanguage();
                return true;

            case R.id.supportedTTSLanguages:
                onSetTextToSpeechVoice();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class TimePickerFragment extends DialogFragment {
        static int idx_ms = 0;
        long beginTimerMillis;
        long timeLeftInMillis;
        long endTimeInMillis;
        boolean timerRunning;
        CountDownTimer countDownTimer;
        long beginMilliSec;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            return new TimePickerDialog(getActivity(), (view, hourOfDay, minute1) -> { //onTimeSet
                Calendar calender = Calendar.getInstance();
                calender.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calender.set(Calendar.MINUTE, minute1);
                calender.set(Calendar.SECOND, 0);
                if (calender.before(Calendar.getInstance())) {
                    calender.add(Calendar.DATE, 1);
                }

                updateTimeText(calender);
                beginTimerMillis = calender.getTimeInMillis() - System.currentTimeMillis();
                sendGetAsyncRequest.n_que_answered = 0;
                startTimer(beginTimerMillis);
                updateCountDownText(beginTimerMillis);
            }, hour, minute, DateFormat.is24HourFormat(getActivity()));
        }

        @SuppressLint("SetTextI18n")
        private synchronized void updateTimeText(Calendar calender) {
            textView.setText("Quanification set for: " + java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(calender.getTime()));
        }

        @SuppressLint("DefaultLocale")
        public synchronized void updateCountDownText(long timeLeftInMillis) {
            textViewCountDown.setText(String.format("%02d:%02d:%02d", (timeLeftInMillis / 3600000) % 24, (timeLeftInMillis / 60000) % 60, (timeLeftInMillis / 1000) % 60));
        }

        @SuppressLint("SetTextI18n")
        public synchronized void resetTimer() {
            if (countDownTimer != null) {  // stopping timer and rest processes
                beginMilliSec = 0;
                countDownTimer.cancel();
                updateCountDownText(0);
            }
            idx_ms = 0;
            textView.setText("Quanification cancelled.");
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @SuppressLint("SetTextI18n")
        public synchronized void nextExecution() {
            System.gc(); // collect the garbage before initiation of the next execution
            if (countDownTimer != null) {
                countDownTimer.cancel(); // first cancel previously running countdown timer
            }
            if (idx_ms <= Utils.TIMER_MILLIs.length) {
//                Utils.sleep(1);
                startTimer(Utils.TIMER_MILLIs[idx_ms]);
                updateCountDownText(Utils.TIMER_MILLIs[idx_ms]);
                idx_ms += 1;
            }
        }
    }
}
