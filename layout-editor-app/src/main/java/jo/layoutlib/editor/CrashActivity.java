package jo.layoutlib.editor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activité affichée quand l'app crash.
 *
 * <p>Au lieu d'afficher le dialogue Android par défaut ("Application has stopped"),
 * cette activité montre :</p>
 *
 * <ul>
 *   <li>Le type d'exception et le message</li>
 *   <li>Le stack trace complet dans une zone scrollable</li>
 *   <li>Un bouton "Copier" pour copier le stack trace</li>
 *   <li>Un bouton "Partager" pour partager le stack trace</li>
 *   <li>Un bouton "Redémarrer" pour relancer l'app</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 3.3
 */
public class CrashActivity extends AppCompatActivity {

    public static final String EXTRA_STACK_TRACE = "jo.layoutlib.editor.STACK_TRACE";
    public static final String EXTRA_EXCEPTION_CLASS = "jo.layoutlib.editor.EXCEPTION_CLASS";
    public static final String EXTRA_EXCEPTION_MESSAGE = "jo.layoutlib.editor.EXCEPTION_MESSAGE";

    private TextView exceptionClassText;
    private TextView exceptionMessageText;
    private TextView stackTraceText;
    private Button copyButton;
    private Button restartButton;
    private Button shareButton;

    private String stackTrace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);

        exceptionClassText = findViewById(R.id.exceptionClass);
        exceptionMessageText = findViewById(R.id.exceptionMessage);
        stackTraceText = findViewById(R.id.stackTraceText);
        copyButton = findViewById(R.id.btnCopy);
        restartButton = findViewById(R.id.btnRestart);
        shareButton = findViewById(R.id.btnShare);

        stackTraceText.setMovementMethod(new ScrollingMovementMethod());
        loadIntentExtras();
        setupListeners();
    }

    private void loadIntentExtras() {
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE);
        String exceptionClass = intent.getStringExtra(EXTRA_EXCEPTION_CLASS);
        String exceptionMessage = intent.getStringExtra(EXTRA_EXCEPTION_MESSAGE);

        if (stackTrace == null) {
            stackTrace = "Aucun stack trace disponible";
        }

        if (exceptionClass != null) {
            exceptionClassText.setText("Exception: " + exceptionClass);
        }

        if (exceptionMessage != null) {
            exceptionMessageText.setText("Message: " + exceptionMessage);
        } else {
            exceptionMessageText.setVisibility(View.GONE);
        }

        // Le stack trace ne s'affiche qu'en build debug — en release on ne
        // divulgue rien du fonctionnement interne (juste un message générique).
        if (BuildConfig.DEBUG) {
            stackTraceText.setText(stackTrace != null ? stackTrace
                    : "Aucun stack trace disponible");
        } else {
            stackTrace = null; // rien de copiable/partageable en release
            stackTraceText.setText("Détails techniques disponibles uniquement "
                    + "dans le build debug.");
            copyButton.setVisibility(View.GONE);
            shareButton.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && stackTrace != null) {
                ClipData clip = ClipData.newPlainText("Stack trace", stackTrace);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Stack trace copié", Toast.LENGTH_SHORT).show();
            }
        });

        shareButton.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Layout Editor");
            shareIntent.putExtra(Intent.EXTRA_TEXT, stackTrace);
            startActivity(Intent.createChooser(shareIntent, "Partager le stack trace"));
        });

        restartButton.setOnClickListener(v -> {
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(mainIntent);
            finish();
        });
    }
}
