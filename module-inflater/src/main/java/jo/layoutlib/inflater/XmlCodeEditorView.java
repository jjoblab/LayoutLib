package jo.layoutlib.inflater;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;

import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Éditeur XML avec coloration syntaxique et numéros de ligne, inspiré de
 * l'éditeur de code d'Android Studio.
 *
 * <p>Fonctionnalités :</p>
 * <ul>
 *   <li>Coloration des tags, attributs, strings, commentaires, et markup XML</li>
 *   <li>Numéros de ligne dans la marge gauche</li>
 *   <li>Police monospace</li>
 *   <li>Surlignage de la ligne courante</li>
 *   <li>Highlight de lignes sélectionnées (ex: ligne de la vue sélectionnée)</li>
 * </ul>
 *
 * <h2>Couleurs par défaut (thème Material 3 dark)</h2>
 * <ul>
 *   <li>Tags : #F07178 (rouge corail)</li>
 *   <li>Attributs : #C792EA (violet)</li>
 *   <li>Strings : #C3E88D (vert clair)</li>
 *   <li>Commentaires : #54668A (bleu-gris, italique)</li>
 *   <li>Brackets {@code <?xml ?>} : #89DDFF (cyan)</li>
 *   <li>Numéros de ligne : #3A3D5C (gris foncé)</li>
 *   <li>Texte par défaut : #E6E6F0 (blanc cassé)</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.1
 */
public class XmlCodeEditorView extends EditText {

    // ---- Couleurs configurables ----
    private int colorTag = Color.parseColor("#F07178");
    private int colorAttr = Color.parseColor("#C792EA");
    private int colorString = Color.parseColor("#C3E88D");
    private int colorComment = Color.parseColor("#54668A");
    private int colorBracket = Color.parseColor("#89DDFF");
    private int colorDefault = Color.parseColor("#E6E6F0");
    private int colorLineNum = Color.parseColor("#3A3D5C");
    private int colorLineNumActive = Color.parseColor("#9B95AD");
    private int colorHighlightBg = Color.parseColor("#147C5CFF"); // 8% alpha
    private int colorActiveLineBg = Color.parseColor("#08FFFFFF"); // 3% alpha

    private Paint lineNumPaint;
    private Paint lineNumActivePaint;
    private Paint highlightPaint;
    private Paint activeLinePaint;
    private Rect rect = new Rect();

    private int gutterWidthPx = 0;
    private int highlightStartLine = -1;
    private int highlightEndLine = -1;
    private int lineHeightPx = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingHighlight;
    private static final long DEBOUNCE_MS = 200;

    public XmlCodeEditorView(Context context) {
        super(context);
        init();
    }

    public XmlCodeEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public XmlCodeEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        gutterWidthPx = (int) (42 * density);
        lineHeightPx = (int) (getPaint().getTextSize() * 1.7f);

        lineNumPaint = new Paint();
        lineNumPaint.setColor(colorLineNum);
        lineNumPaint.setTextSize(getPaint().getTextSize());
        lineNumPaint.setAntiAlias(true);
        lineNumPaint.setTypeface(android.graphics.Typeface.MONOSPACE);
        lineNumPaint.setTextAlign(Paint.Align.RIGHT);

        lineNumActivePaint = new Paint(lineNumPaint);
        lineNumActivePaint.setColor(colorLineNumActive);

        highlightPaint = new Paint();
        highlightPaint.setColor(colorHighlightBg);
        highlightPaint.setStyle(Paint.Style.FILL);

        activeLinePaint = new Paint();
        activeLinePaint.setColor(colorActiveLineBg);
        activeLinePaint.setStyle(Paint.Style.FILL);

        setTypeface(android.graphics.Typeface.MONOSPACE);
        setTextColor(colorDefault);
        setBackgroundColor(Color.parseColor("#0F1226"));

        // Padding gauche pour le gutter
        setPadding(gutterWidthPx + (int) (8 * density),
                (int) (14 * density),
                (int) (20 * density),
                (int) (80 * density));

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                scheduleHighlight();
            }
        });
    }

    private void scheduleHighlight() {
        if (pendingHighlight != null) mainHandler.removeCallbacks(pendingHighlight);
        pendingHighlight = () -> {
            applySyntaxHighlight();
            invalidate();
        };
        mainHandler.postDelayed(pendingHighlight, DEBOUNCE_MS);
    }

    /** Applique la coloration syntaxique sur tout le texte. */
    public void applySyntaxHighlight() {
        String text = getText().toString();
        if (text.isEmpty()) return;

        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        // Reset : on applique d'abord le défaut
        sb.setSpan(new ForegroundColorSpan(colorDefault),
                0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Commentaires <!-- ... -->
        applyPattern(sb, text, Pattern.compile("<!--.*?-->", Pattern.DOTALL),
                colorComment, true);

        // Brackets <?xml ... ?> ou <? ... ?>
        applyPattern(sb, text, Pattern.compile("<\\?.*?\\?>", Pattern.DOTALL),
                colorBracket, false);
        // Juste le <?xml, ?>, < et > markup
        applyPattern(sb, text, Pattern.compile("<\\?|\\?>"), colorBracket, false);

        // Strings entre guillemets (à l'intérieur des tags)
        applyPattern(sb, text, Pattern.compile("\"[^\"]*\""), colorString, false);

        // Attributs : mot:mot= ou mot= (avant le =)
        Matcher mAttr = Pattern.compile("(\\b[a-zA-Z_:][a-zA-Z0-9_:]*)(\\s*=)").matcher(text);
        while (mAttr.find()) {
            sb.setSpan(new ForegroundColorSpan(colorAttr),
                    mAttr.start(1), mAttr.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Tags : <TagName ou </TagName
        Matcher mTag = Pattern.compile("(</?)([a-zA-Z_][a-zA-Z0-9_\\-\\.]*)").matcher(text);
        while (mTag.find()) {
            sb.setSpan(new ForegroundColorSpan(colorTag),
                    mTag.start(2), mTag.end(2), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(colorBracket),
                    mTag.start(1), mTag.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // /> à la fin des tags auto-fermants
        applyPattern(sb, text, Pattern.compile("/>"), colorBracket, false);

        // Remplacer le texte sans perdre le curseur
        int cursor = getSelectionStart();
        setText(sb);
        if (cursor >= 0 && cursor <= length()) {
            setSelection(cursor);
        }
    }

    private void applyPattern(SpannableStringBuilder sb, String text,
                              Pattern pattern, int color, boolean italic) {
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            sb.setSpan(new ForegroundColorSpan(color),
                    m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (italic) {
                sb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC),
                        m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /** Surligne les lignes comprises entre startLine et endLine (1-indexed). */
    public void setHighlightLines(int startLine, int endLine) {
        this.highlightStartLine = startLine;
        this.highlightEndLine = endLine;
        invalidate();
    }

    public void clearHighlight() {
        this.highlightStartLine = -1;
        this.highlightEndLine = -1;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Background
        float density = getResources().getDisplayMetrics().density;

        // Dessiner les highlights de lignes avant le texte
        String text = getText().toString();
        int lineCount = getLineCount();
        float lineHeight = getLineHeight();
        int padTop = getPaddingTop() - getScrollY();

        // Active line (cursor line)
        int cursorLine = getLayout() != null
                ? getLayout().getLineForOffset(getSelectionStart()) : -1;

        // Highlight range
        if (highlightStartLine > 0 && highlightEndLine >= highlightStartLine) {
            float startY = padTop + (highlightStartLine - 1) * lineHeight;
            float endY = padTop + highlightEndLine * lineHeight;
            canvas.drawRect(0, startY, getWidth(), endY, highlightPaint);
        }

        // Active line background (léger)
        if (cursorLine >= 0) {
            float startY = padTop + cursorLine * lineHeight;
            canvas.drawRect(0, startY, getWidth(), startY + lineHeight, activeLinePaint);
        }

        super.onDraw(canvas);

        // Dessiner les numéros de ligne dans le gutter
        int padLeft = getPaddingLeft();
        int gutterRight = gutterWidthPx + (int) (4 * density);

        for (int i = 0; i < lineCount; i++) {
            float y = padTop + (i + 1) * lineHeight - (lineHeight - getPaint().getTextSize()) / 2f;
            int num = i + 1;
            String numStr = String.valueOf(num);
            Paint p = (i == cursorLine) ? lineNumActivePaint : lineNumPaint;
            canvas.drawText(numStr, gutterRight, y, p);
        }
    }

    // ---- Getters/Setters de couleurs ----

    public void setColorTag(int c) { colorTag = c; invalidate(); }
    public void setColorAttr(int c) { colorAttr = c; invalidate(); }
    public void setColorString(int c) { colorString = c; invalidate(); }
    public void setColorComment(int c) { colorComment = c; invalidate(); }
    public void setColorBracket(int c) { colorBracket = c; invalidate(); }
    public void setColorDefault(int c) { colorDefault = c; setTextColor(c); invalidate(); }
    public void setColorLineNum(int c) { colorLineNum = c; lineNumPaint.setColor(c); invalidate(); }
    public void setColorHighlightBg(int c) { colorHighlightBg = c; highlightPaint.setColor(c); invalidate(); }
}
