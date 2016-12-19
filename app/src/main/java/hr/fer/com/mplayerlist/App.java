package hr.fer.com.mplayerlist;

import android.app.Activity;
import android.app.Application;

/**
 * Created by Igor on 17/12/16.
 */
public class App extends Application{
    private static Activity currActivity;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static Activity getCurrentActivity(){
        return currActivity;
    }

    public static void registerActivity(Activity activity) {
        currActivity = activity;
    }
}
