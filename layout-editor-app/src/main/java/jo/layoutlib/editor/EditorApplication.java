package jo.layoutlib.editor;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Application avec handler global d'exceptions non interceptées.
 *
 * <p>Intercepte tous les crashes non gérés et redirige vers
 * {@link CrashActivity} pour afficher le stack trace au lieu de
 * montrer le dialogue Android par défaut.</p>
 *
 * @author jo@Dev
 * @since 3.3
 */
public class EditorApplication extends Application {

    private static final String TAG = "LayoutEditor";

    @Override
    public void onCreate() {
        super.onCreate();
        setupCrashHandler();
        Log.i(TAG, "EditorApplication démarrée — handler de crash installé");
    }

    private void setupCrashHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Crash non intercepté sur thread " + thread.getName(), throwable);

            // Construit le stack trace
            StringBuilder sb = new StringBuilder();
            sb.append("Thread: ").append(thread.getName()).append("\n");
            sb.append("Time: ").append(new java.util.Date()).append("\n");
            sb.append("Device: ").append(Build.MANUFACTURER).append(" ")
                    .append(Build.MODEL).append("\n");
            sb.append("Android: ").append(Build.VERSION.RELEASE)
                    .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n\n");
            sb.append("Exception: ").append(throwable.getClass().getName()).append("\n");
            sb.append("Message: ").append(throwable.getMessage()).append("\n\n");
            sb.append("Stack trace:\n");

            for (StackTraceElement element : throwable.getStackTrace()) {
                sb.append("    at ").append(element.toString()).append("\n");
            }

            // Cause (si exception chaînée)
            Throwable cause = throwable.getCause();
            while (cause != null) {
                sb.append("\nCaused by: ").append(cause.getClass().getName()).append("\n");
                sb.append("Message: ").append(cause.getMessage()).append("\n");
                for (StackTraceElement element : cause.getStackTrace()) {
                    sb.append("    at ").append(element.toString()).append("\n");
                }
                cause = cause.getCause();
            }

            String stackTrace = sb.toString();

            // Lance CrashActivity dans un process séparé pour éviter les boucles
            Intent intent = new Intent(this, CrashActivity.class);
            intent.putExtra(CrashActivity.EXTRA_STACK_TRACE, stackTrace);
            intent.putExtra(CrashActivity.EXTRA_EXCEPTION_CLASS,
                    throwable.getClass().getName());
            intent.putExtra(CrashActivity.EXTRA_EXCEPTION_MESSAGE,
                    throwable.getMessage());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Termine le process courant
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
    }
}
