package patch.log;

import android.util.Log;

/**
 * Created by muditmathur on 24/10/16.
 */
public class DebugLog {
    private DebugLog() {}

    /**
     * Send a debug log message
     *
     * @param tag Source of a log message.
     * @param message The message you would like logged.
     */
    public static void log(String tag, String message) {
        Log.d(tag, message);
    }
}
