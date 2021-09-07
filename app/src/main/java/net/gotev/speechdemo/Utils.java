package net.gotev.speechdemo;

//import android.app.AlertDialog;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Logger;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;
import net.gotev.speech.SpeechUtil;
import net.gotev.speech.TextToSpeechCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Utils extends AppCompatActivity {
    public static final int RECORD_AUDIO_PERMISSIONS_REQUEST = 1;
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final Integer RECOGNIZERINTENTREQUESTCODE = 10;
    public static final float SPEECHRATE = (float) 0.9;
    public static final float ANSWER_SPEECHRATE = (float) 1;  // SpeechRate 0.0 < x < 2.0
    public static final boolean API_POST = false;
    public static final long[] TIMER_MILLIs = {
            30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000,
            30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000,
            30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000,
            30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000,
            30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000,
            30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000,
            30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000,
            30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000, 30000};
    public static final boolean TimerWithGoogleWindow = false;
    public static final TextToSpeech.OnInitListener mTttsInitListener = status -> {
        switch (status) {
            case TextToSpeech.SUCCESS:
                Logger.info(LOG_TAG, "TextToSpeech engine successfully started");
                break;

            case TextToSpeech.ERROR:
                Logger.error(LOG_TAG, "Error while initializing TextToSpeech engine!");
                break;

            default:
                Logger.error(LOG_TAG, "Unknown TextToSpeech status: " + status);
                break;
        }
    };
    public static final String GET_URL = "https://0yh5imhg3m.execute-api.ap-south-1.amazonaws.com/prod";
    private static final String POST_URL = "https://89t84kai7b.execute-api.ap-south-1.amazonaws.com/prod";
    private static final String[] params = new String[3];
    public static List<String> SpeechRecognizerInput = new ArrayList<>();
    public static boolean isBluetoothConnected = false;
    //The BroadcastReceiver that listens for bluetooth broadcasts
    public static final BroadcastReceiver BluetoothReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                isBluetoothConnected = true;
                Log.e("BluetoothDevice", "isBluetoothConnected = true;");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                isBluetoothConnected = false;
                Speech.getInstance().stopTextToSpeech();
                Log.e("BluetoothDevice", "isBluetoothConnected = false;");
                //Device has disconnected
            }
        }
    };
    public static boolean test;
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static Integer n_que_answer_spoken = 0;
    public static boolean mIslistening = false;
    public static PreferencesHandler preferencesHandler;
    public static Integer n_que = 20; //10 + 3 + 5 + more 2 extra // you can set the number // 0 < n_que > 24

    public static AlertDialog.Builder alertDialogBuilder() {
        return new AlertDialog.Builder(context);
    }

    public static boolean requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSIONS_REQUEST);
        }
        return false;
    }

    public static RequestQueue Queue() {
        return Volley.newRequestQueue(context);
    }

    @SuppressLint("QueryPermissionsNeeded")
    public static ComponentName ResolveRecognizerIntent(Intent intent) {
        return intent.resolveActivity(context.getPackageManager());
    }


    public static synchronized void SpeakPromptly(String text) {
        Speech.getInstance().setTextToSpeechRate(SPEECHRATE);
        if (!Speech.getInstance().isSpeaking() & isBluetoothHeadsetConnected()) {
            Speech.getInstance().say(text, new TextToSpeechCallback() {
                @Override
                public void onStart() {
                }

                @Override
                public void onCompleted() {
                    isBluetoothHeadsetConnected();
                }

                @Override
                public void onError() {
                    Speech.getInstance().stopTextToSpeech();
                }
            });
            if (!isBluetoothHeadsetConnected()) {
                Speech.getInstance().stopTextToSpeech();
            }
            sleep(2);
        } else {
            Log.e("SpeakPromptly", "isBluetoothConnected: " + isBluetoothConnected + ", Speech.getInstance().isSpeaking(): " + Speech.getInstance().isSpeaking());
        }
    }

    @SuppressLint({"QueryPermissionsNeeded", "SetTextI18n"})
    public static boolean getSpeechInput() {
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        recognizerIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        if (ResolveRecognizerIntent(recognizerIntent) != null & isBluetoothConnected) {
            ((Activity) context).startActivityForResult(recognizerIntent, RECOGNIZERINTENTREQUESTCODE);
            Log.e("getSpeechInput", "StartRecognizerActivityForResult");
            return true;
        } else {
            Log.e("getSpeechInput", "Your device don't support speech input");
            return false;
        }
    }

    static void sleep(long second) {
        try {
            SECONDS.sleep(second);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        isBluetoothConnected = mBluetoothAdapter != null & mBluetoothAdapter.isEnabled()
                & mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
        return isBluetoothConnected;
    }

    public static synchronized void SpeakAnswer(String text) {
        Speech.getInstance().setTextToSpeechRate(ANSWER_SPEECHRATE);
        String[] s_arr;
        try {
            s_arr = text.substring(0, 2750).split(" "); // 2750 characters= 500 words
        } catch (Exception e) {
            s_arr = text.split(" ");
            e.printStackTrace();
        }
        String words = "";
        for (int idx_space = 1; idx_space <= s_arr.length; idx_space++) {
            words += s_arr[idx_space - 1] + " ";
            if (idx_space % 5 == 0 || idx_space == s_arr.length) {
                for (int i = 0; i < 3; ) {
                    if (!Speech.getInstance().isSpeaking() & isBluetoothHeadsetConnected()) {
                        sleep(1);
                        Speech.getInstance().say(words, new TextToSpeechCallback() {
                            @Override
                            public void onStart() {
                            }

                            @Override
                            public void onCompleted() {
                                isBluetoothHeadsetConnected();
                            }

                            @Override
                            public void onError() {
                                Speech.getInstance().stopTextToSpeech();
                            }
                        });
                        i += 1;
                        if (!isBluetoothHeadsetConnected()) {
                            Speech.getInstance().stopTextToSpeech();
                        }
                    }
                }
                words = "";
                System.gc();
            }
        }
    }

    public static void onSpeechToTextExec() {
        if (Speech.getInstance().isListening()) {
            Speech.getInstance().stopListening();
        } else {
            if (requestAudioPermission()) {
                onRecordAudioPermissionGranted();
            }
        }
    }

    public static void onRecordAudioPermissionGranted() {

        MainActivity.mic_btn.setVisibility(View.GONE);
        MainActivity.linearLayout.setVisibility(View.VISIBLE);

        try {
            Speech.getInstance().stopTextToSpeech();
            Log.e("STT", "Speech.getInstance().startListening(progress, (SpeechDelegate) context);.");
            Speech.getInstance().startListening(MainActivity.progress, (SpeechDelegate) context);
        } catch (SpeechRecognitionNotAvailable exc) {
            showSpeechNotSupportedDialog();
        } catch (GoogleVoiceTypingDisabledException exc) {
            showEnableGoogleVoiceTyping();
        }
    }

    public static void showSpeechNotSupportedDialog() {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    SpeechUtil.redirectUserToGoogleAppOnPlayStore(context);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };

        AlertDialog.Builder builder = alertDialogBuilder();
        builder.setMessage(R.string.speech_not_available)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.no, dialogClickListener)
                .show();
    }

    private static void showEnableGoogleVoiceTyping() {
        AlertDialog.Builder builder = alertDialogBuilder();
        builder.setMessage(R.string.enable_google_voice_typing)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    // do nothing
                })
                .show();
    }


    public static void onSpeechToTextQuestion() {
        PreferencesHandler.sub_code = MainActivity.sub_code_editText.getText().toString().trim().toUpperCase();
        mIslistening = false;
        if (PreferencesHandler.sub_code.length() > 3) {
            if (MainActivity.TimePickerFragment.idx_ms > 15) {
                SpeakPromptly("critical r" + (sendGetAsyncRequest.n_que_answered + 1));
            } else {
                SpeakPromptly("r" + (sendGetAsyncRequest.n_que_answered + 1));
            }
//            sleep(2);
            test = false;
            onSpeechToTextExec();
        } else {
            for (int i = 0; i < 3; i++) {
                SpeakPromptly("Not a valid subject code " + PreferencesHandler.sub_code);
            }
        }
    }

    public static void processQuestionToAnswer() {
        String speech = "";
        try {
            Log.e("processQuestionToAnswer", String.valueOf(sendGetAsyncRequest.n_que_answered));
            speech = SpeechRecognizerInput.get(sendGetAsyncRequest.n_que_answered);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        if (speech.equals("")) {
            SpeakPromptly("Not a question.");
//            sleep(2);
        } else {
            String url = createUrl(0, PreferencesHandler.sub_code, speech);
            if (API_POST) {
                sendPostRequest(url);
            } else {
//                sendGetRequest(url);
//                AsyncTask<String, Void, Integer> response_code = new sendGetAsyncRequest().execute(url);
                Log.e("sendGetAsyncRequest: ",
                        "urlConnection.getResponseCode()): " + new sendGetAsyncRequest().execute(url));
            }
        }
    }

    public static void startAnsweringTheQuestions() {
        for (int i = 0; i < sendGetAsyncRequest.n_que_answered; i++) {
            String[] que_ans = preferencesHandler.getQueAnsFromPreferences("question" + (i + 1), "answer" + (i + 1));
            Log.e("startAnswering", Arrays.toString(que_ans));
            if (!que_ans[0].equals("") & !que_ans[1].equals("")) {
                String temp_q;
                try {
                    temp_q = que_ans[0].substring(0, 100);
                } catch (Exception e) {
                    temp_q = que_ans[0];
                    e.printStackTrace();
                }
                sleep(10);
                SpeakPromptly("Question " + (i + 1) + " " + temp_q + "  Answering now");
                SpeakAnswer(que_ans[1]);
                preferencesHandler.removeQueAnsFromPreferences("question" + (i + 1), "answer" + (i + 1));
            } else {
                SpeakPromptly("Negative " + (i + 1));
            }
            n_que_answer_spoken += 1;
        }
    }

    private static synchronized void sendGetRequest(String url) {
        RequestQueue queue = Queue();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            response = response.replace("\n", "");
            response = response.replace("\\", "");
            String que = response.split("\": \"")[5].split("\", \"")[0]; // "\ " and ", "
            String ans = response.split("\": \"")[6].split("\", \"")[0]; // "\ " and ", "
            Log.e("sendGetRequest", "Volley Request succeed: " + response);
            if (preferencesHandler.putQueAnsInPreferences((sendGetAsyncRequest.n_que_answered + 1), que, ans)) {
                sendGetAsyncRequest.n_que_answered += 1;
            } else {
                SpeakPromptly((sendGetAsyncRequest.n_que_answered + 1) + " negative.");
            }
            Log.e("sendGetRequest", "Volley Request succeed: sendGetAsyncRequest.n_que_answered: " + sendGetAsyncRequest.n_que_answered);
        }, error -> {
            Log.e("sendGetRequest", "answer Request failed:" + error.getMessage());
            SpeakPromptly((sendGetAsyncRequest.n_que_answered + 1) + " negative.");
//            sleep(2);
            MainActivity.textView.setText(("answer Request failed: " + error.getMessage()));
        });
        queue.add(stringRequest);
    }

    public static synchronized void sendPostRequest(String url) {
        Map<String, String> url_params = new HashMap<>();
        RequestQueue queue = Queue();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, POST_URL, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                Log.e("sendPostRequest", "Volley Request succeed: " + jsonObject);

            } catch (JSONException e) {
                e.printStackTrace();
            }
//                // JSONObject > JsonArray
//                String val = "{\"statusCode\":200,\n" +
//                        "\"headers\":{\"Content-type\":\"application\\/json\"}," +
//                        "\"body\":\"{\\\"launch_time\\\": \\\"21-02-06 22:36:46\\\", \\\"sub_code\\\": \\\"KCE051\\\", \\\"question\\\": \\\"what is magnetic confinement?\\\", \\\"answer\\\": \\\"Magnetic confinement fusion is an approach to generate thermonuclear fusion power that uses magnetic fields to confine fusion fuel in the form of a plasma. Magnetic confinement is one of two major branches of fusion energy research, along with inertial confinement fusion\\\\\\\\n'\\\", \\\"score\\\": 100}\"}";
//            Log.e("sendPostRequest", "Volley Request succeed: " + response);
//            response = response.replace("\n", "");
//            response = response.replace("\\", "");
//            Log.e("sendPostRequest", "Volley Request succeed: " + response);
//            String que = response.split("\": \"")[5].split("\", \"")[0]; // "\ " and ", "
//            String ans = response.split("\": \"")[6].split("\", \"")[0]; // "\ " and ", "
//            Log.e("sendPostRequest", "Volley Request succeed: " + response);
//            if (preferencesHandler.putQueAnsInPreferences((sendGetAsyncRequest.n_que_answered + 1), que, ans)) {
//                sendGetAsyncRequest.n_que_answered += 1;
//            } else {
//                SpeakPromptly((sendGetAsyncRequest.n_que_answered + 1) + " negative.");
//            }
//            Log.e("sendPostRequest", "Volley Request succeed: sendGetAsyncRequest.n_que_answered: " + sendGetAsyncRequest.n_que_answered);
        }, error -> {
            Log.e("sendPostRequest", "answer Request failed:" + error.getMessage());
            SpeakPromptly((sendGetAsyncRequest.n_que_answered + 1) + " negative.");
//            sleep(2);
            MainActivity.textView.setText(("answer Request failed: " + error.getMessage()));

        }) {
            @Override
            protected Map<String, String> getParams() {
                url_params.put("t", params[0]);
                url_params.put("s", params[1]);
                url_params.put("q", params[2]);
                return url_params;
            }

//            @Override
//            public Map<String, String> getHeaders() {
//                HashMap<String, String> headers = new HashMap<>();
//                headers.put("Content-Type", "application/json; charset=utf-8");
//                return headers;
//            }
//            @Override
//            public String getBodyContentType() {
//                return "application/json; charset=utf-8";
//            }
//            @Override
//            public byte[] getBody() {
//                return url_params == null ? null : url_params.toString().getBytes(StandardCharsets.UTF_8);
//            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {
                    responseString = String.valueOf(response.statusCode);
                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }

        };
//        stringRequest.setRetryPolicy(new DefaultRetryPolicy(20000, 2, 1.0f));
        queue.add(stringRequest);
    }

    ///////////////////////////  api request
    @SuppressLint({"WrongConstant", "ShowToast"})
    public static synchronized String createUrl(Integer test, String sub_code, String question) {
        params[0] = test.toString();
        params[1] = sub_code;
        params[2] = question;
        String url = "";
        if (API_POST) {
            url = POST_URL + "?t" + "=" + test +
                    "&s" + "=" + sub_code +
                    "&q" + "=" + question;
            Log.e("params: ", "POST_URL: " + url);
        } else {
//            try {
//                url += GET_URL + "?" + URLEncoder.encode("t", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(test), "UTF-8");
//                url += "&" + URLEncoder.encode("s", "UTF-8") + "=" + URLEncoder.encode(sub_code, "UTF-8");
//                url += "&" + URLEncoder.encode("q", "UTF-8") + "=" + URLEncoder.encode(question, "UTF-8");
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
            url = GET_URL + "?t" + "=" + test +
                    "&s" + "=" + sub_code +
                    "&q" + "=" + question;
            Log.e("params: ", "GET_URL: " + url);
        }
        return url;
    }


    public static void SpeechToText() {
        PreferencesHandler.sub_code = MainActivity.sub_code_editText.getText().toString().toUpperCase();
        mIslistening = false;
        if (PreferencesHandler.sub_code.length() > 3) {
            SpeakPromptly("r" + (sendGetAsyncRequest.n_que_answered + 1));
//            sleep(2);
            test = false;
            getSpeechInput();
        } else {
            for (int i = 0; i < 3; i++) {
                SpeakPromptly("Not a valid subject code " + PreferencesHandler.sub_code);
            }
        }
    }


    private void onSpeakClick() {
        String txt = MainActivity.textToSpeech.getText().toString().trim();
        if (txt.isEmpty()) {
            Toast.makeText(this, R.string.input_something, Toast.LENGTH_LONG).show();
            SpeakAnswer("One way of doing this without changing Volley's source code is to check for the response data in the VolleyError and parse it your self..");
            return;
        }
        SpeakAnswer(txt);
    }

}


