package net.gotev.speechdemo;


import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static net.gotev.speechdemo.Utils.SpeakPromptly;
import static net.gotev.speechdemo.Utils.preferencesHandler;

public class sendGetAsyncRequest extends AsyncTask<String, Void, Integer> {
    public static Integer n_que_answered = 0;

    String response;

    @Override
    protected Integer doInBackground(String... strings) {

        URL url;
        HttpURLConnection urlConnection;

        try {
            url = new URL(strings[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
//            urlConnection.setDoOutput(true);
//            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(urlConnection.getOutputStream());
//            outputStreamWriter.write(strings[0]);
//            outputStreamWriter.flush();

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                response = readStream(urlConnection.getInputStream());
            } else {
                Log.e("sendGetAsyncRequest", "answer Request failed: " + urlConnection.getResponseCode());
                SpeakPromptly((n_que_answered + 1) + " negative.");
//                Utils.sleep(1);
            }
            return urlConnection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onPostExecute(Integer s) {
        super.onPostExecute(s);
        if (response != null) {
            try {
//                Log.e("sendGetAsyncRequest", "Async Request succeed: response= " + response);
                JSONObject jsonObject = new JSONObject(response);
                String sub = jsonObject.getString("sub_code");
                String que = jsonObject.getString("question");
                String ans = jsonObject.getString("answer");
                int score = jsonObject.getInt("score");

                Log.e("sendGetAsyncRequest", "Async Request succeed: sub_code=" + sub);
                Log.e("sendGetAsyncRequest", "Async Request succeed: que=" + que);
                Log.e("sendGetAsyncRequest", "Async Request succeed: ans=" + ans);
                Log.e("sendGetAsyncRequest", "Async Request succeed: score=" + score);

                if (!Utils.test) {
//                response = response.replace("\n", "");
//                response = response.replace("\\", "");
//                Log.e("sendGetAsyncRequest", "Async Request succeed: response= " + response);
//                String que = response.split("\": \"")[5].split("\", \"")[0]; // "\ " and ", "
//                String ans = response.split("\": \"")[6].split("\", \"")[0]; // "\ " and ", "
//                int score = Integer.parseInt(response.substring(response.length() - 5, response.length() - 3).trim());
                    if (!ans.equals("") & score > 86) { // score > 86, less then 86 is always wrong
                        if (preferencesHandler.putQueAnsInPreferences((n_que_answered + 1), que, ans)) {
                            n_que_answered += 1;
                        }
                    } else {
                        if (MainActivity.TimePickerFragment.idx_ms > 15 & score >= 85) {
                            if (preferencesHandler.putQueAnsInPreferences((n_que_answered + 1), que, ans)) {
                                n_que_answered += 1;
                            }
                        }
                        SpeakPromptly("s" + score);
//                        Utils.sleep(1);
                    }
                    Log.e("sendGetAsyncRequest", "Async Request succeed: n_que_answered= " + n_que_answered);
                } else {
                    if (que.equals("")) {
                        MainActivity.textView.setText("sub_code: " + sub + " unavailable.");
                    } else {
                        MainActivity.textView.setText("sub_code: " + sub + " available.");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                SpeakPromptly((n_que_answered + 1) + " negative.");
            }

        }

    }

    // Converting InputStream to String
    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }
}

