package jo.layoutlib.editor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import jo.codeeditor.document.EditorDocument;
import jo.layoutlib.drawables.DrawableResolverImpl;
import jo.layoutlib.inflater.ComponentPalettePopup;
import jo.layoutlib.inflater.DeviceProfile;
import jo.layoutlib.inflater.OverlayView;
import jo.layoutlib.inflater.RenderService;
import jo.layoutlib.inflater.ViewInfoCollector;
import jo.layoutlib.inflater.BlueprintListView;
import jo.layoutlib.themes.ThemeResolverImpl;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceResolverImpl;
import jo.layoutlib.resources.ResourceTable;
import jo.codeeditor.session.EditorSession;
import jo.codeeditor.view.chrome.EditorTheme;
import jo.codeeditor.view.EditorView;

/**
 * Activité principale du Layout Editor — version 3.1.
 *
 * <p>Architecture :</p>
 * <ul>
 *   <li><strong>code-editor-lib</strong> : EditorView + EditorSession pour
 *       l'édition XML avec coloration syntaxique, gutter, folds, IME</li>
 *   <li><strong>mini-layoutlib</strong> : RenderService avec ResourceResolver
 *       pour le rendu automatique, OverlayView (sélection + 8 handles +
 *       drag-to-move/resize), BlueprintListView, ComponentPalettePopup,
 *       ColorSwatchPopup</li>
 *   <li><strong>Layout-editor-app custom</strong> : PropertiesPanel,
 *       PaletteAdapter, TreeAdapter, UndoRedoManager, XmlMutator</li>
 * </ul>
 *
 * <p>Le rendu est automatique : chaque modification du texte dans EditorView
 * déclenche un {@link RenderService#requestRender(String)} debouncé (400ms).</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
public class MainActivity extends AppCompatActivity {

    // ---- Services du mini-layoutlib ----
    private RenderService renderService;
    private ResourceResolverImpl resourceResolver;
    private ThemeResolverImpl themeResolver;
    private OverlayView overlayView;

    // ---- code-editor-lib ----
    private EditorView editorView;
    private EditorSession session;

    // ---- Panneaux custom (basés sur mini-layoutlib) ----
    private PropertiesPanel propertiesPanel;
    private LinearLayout propertiesContainer;

    // ---- Vues principales ----
    private FrameLayout designScroll;
    private ScrollView blueprintScroll;
    private FrameLayout codeViewContainer;
    private TextView screenBadge;
    private TextView statusText;
    private TextView zoomPercent;
    private FloatingActionButton fab;

    // ---- Mode tabs ----
    private MaterialButton tabDesign, tabBlueprint, tabCode;

    // ---- BS tabs ----
    private MaterialButton bsTabAttributes, bsTabPalette, bsTabTree;
    private TextView bsTitle;
    private TextView bsIdChip;
    private LinearLayout bottomSheet;
    private FrameLayout bsContentFrame;
    private NestedScrollView bsBodyScroll;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;

    // ---- Bottom nav (BottomNavigationView Material 3) ----
    private BottomNavigationView bottomNav;

    // ---- Blueprint ----
    private BlueprintListView blueprintList;

    // ---- État ----
    private ViewInfoCollector.ViewInfo currentRootInfo;
    private View renderedRoot; // le root rendu actuel (pour findRenderedView)
    private ViewInfoCollector.ViewInfo selectedView;
    private String currentMode = "design"; // "design" | "blueprint" | "code"
    private int zoom = 100;
    private ComponentPalettePopup palettePopup;
    private boolean suppressTextWatcher = false;
    private boolean fabOpen = false;

    // ---- Undo/Redo custom (au niveau XML complet) ----
    private UndoRedoManager undoRedoManager;
    private final Handler historyHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingHistoryPush;
    private static final long HISTORY_DEBOUNCE_MS = 1000;

    /** XML par défaut chargé au démarrage. */
    private static final String DEFAULT_XML =
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    android:orientation=\"vertical\"\n" +
            "    android:padding=\"24dp\"\n" +
            "    android:background=\"#FFFFFFFF\"\n" +
            "    android:gravity=\"center_horizontal\">\n" +
            "    <TextView android:text=\"Connexion\" android:textSize=\"24sp\"\n" +
            "        android:textColor=\"#FF6750A4\" android:textStyle=\"bold\"\n" +
            "        android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"\n" +
            "        android:layout_marginBottom=\"16dp\"/>\n" +
            "    <EditText android:hint=\"Adresse email\" android:inputType=\"textEmailAddress\"\n" +
            "        android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"\n" +
            "        android:layout_marginBottom=\"8dp\"/>\n" +
            "    <EditText android:hint=\"Mot de passe\" android:inputType=\"textPassword\"\n" +
            "        android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"\n" +
            "        android:layout_marginBottom=\"16dp\"/>\n" +
            "    <Button android:text=\"Se connecter\"\n" +
            "        android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>\n" +
            "    <TextView android:text=\"Mot de passe oublié ?\" android:textColor=\"#FF6750A4\"\n" +
            "        android:layout_marginTop=\"16dp\"\n" +
            "        android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>\n" +
            "</LinearLayout>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        bindViews();
        setupRenderService();
        setupEditor();
        setupModeTabs();
        setupBottomSheetTabs();
        setupBottomNav();
        setupFAB();
        setupZoomControls();
        setupTopAppBar();

        // Initialiser le gestionnaire undo/redo
        undoRedoManager = new UndoRedoManager(50, 800);

        // Charger le XML par défaut dans l'éditeur
        suppressTextWatcher = true;
        EditorDocument doc = EditorDocument.of(DEFAULT_XML);
        session = new EditorSession(doc);
        session.setLanguage("xml");
        editorView.setSession(session);
        editorView.setTheme(makeMaterialDarkTheme());
        editorView.setLanguage(null);
        editorView.setFileName("activity_login.xml");
        suppressTextWatcher = false;
        undoRedoManager.pushState(DEFAULT_XML);
        refreshUndoRedoButtons();

        // Wire text changes → render automatique
        session.setOnTextEditListener((start, end, inserted) -> {
            if (suppressTextWatcher) return;
            String xml = session.getText();
            if (xml.trim().isEmpty()) {
                // XML vide : afficher placeholder, vider le preview
                showEmptyPreview();
            } else {
                renderService.requestRender(xml);
                scheduleHistoryPush(xml);
            }
            refreshUndoRedoButtons();
        });

        // Render initial
        designScroll.post(() -> renderService.requestImmediateRender(DEFAULT_XML));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 1. Annuler le push d'historique debouncé en attente
        if (pendingHistoryPush != null) {
            historyHandler.removeCallbacks(pendingHistoryPush);
            pendingHistoryPush = null;
        }

        // 2. Arrêter toute animation en cours (FAB)
        if (fab != null) {
            fab.clearAnimation();
            fab.animate().cancel();
        }

        // 3. Refermer la palette de composants si ouverte
        if (palettePopup != null) {
            if (palettePopup.isShowing()) {
                palettePopup.dismiss();
            }
            palettePopup = null;
        }

        // 4. Libérer le RenderService : annule les rendus debouncés en
        //    attente, retire le callback (plus de notification vers une
        //    Activity détruite) et coupe la référence vers le Context.
        if (renderService != null) {
            renderService.release();
            renderService = null;
        }

        // 5. Couper les références restantes vers la hiérarchie rendue
        overlayView = null;
        renderedRoot = null;
        currentRootInfo = null;
        selectedView = null;
    }

    // ============================================================
    // BIND VIEWS
    // ============================================================

    private void bindViews() {
        editorView = findViewById(R.id.editorView);
        designScroll = findViewById(R.id.designScroll);
        blueprintScroll = findViewById(R.id.blueprintScroll);
        codeViewContainer = findViewById(R.id.codeViewContainer);
        screenBadge = findViewById(R.id.screenBadge);
        statusText = findViewById(R.id.statusText);
        zoomPercent = findViewById(R.id.zoomPercent);
        fab = findViewById(R.id.fab);

        tabDesign = findViewById(R.id.tabDesign);
        tabBlueprint = findViewById(R.id.tabBlueprint);
        tabCode = findViewById(R.id.tabCode);

        bsTabAttributes = findViewById(R.id.bsTabAttributes);
        bsTabPalette = findViewById(R.id.bsTabPalette);
        bsTabTree = findViewById(R.id.bsTabTree);
        bsTitle = findViewById(R.id.bsTitle);
        bsIdChip = findViewById(R.id.bsIdChip);
        bottomSheet = findViewById(R.id.bottomSheet);
        bsContentFrame = findViewById(R.id.bsContentFrame);
        bsBodyScroll = findViewById(R.id.bsBodyScroll);

        // TEMPORAIRE : cacher le bottom sheet pendant le debug des overlays
        if (bottomSheet != null) {
            bottomSheet.setVisibility(View.GONE);
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            bottomSheetBehavior.setDraggable(false);
        }

        blueprintList = findViewById(R.id.blueprintList);

        bottomNav = findViewById(R.id.bottomNav);

        // Container pour PropertiesPanel (LinearLayout créé programmatiquement)
        propertiesContainer = new LinearLayout(this);
        propertiesContainer.setOrientation(LinearLayout.VERTICAL);
        propertiesContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        bsContentFrame.addView(propertiesContainer);

        propertiesPanel = new PropertiesPanel(this, propertiesContainer);
        propertiesPanel.setOnPropertyChangeListener((key, value) -> {
            if (selectedView == null || selectedView.idName == null || session == null) return;
            // Mapper le label du champ vers le nom d'attribut XML réel
            String xmlAttrName = mapFieldToXmlAttribute(key);
            String xml = session.getText();
            String newXml = XmlMutator.setAttributeById(xml, selectedView.idName, xmlAttrName, value);
            if (!newXml.equals(xml)) {
                replaceSessionXml(newXml);
            }
        });
        propertiesPanel.showProperties(null, null);
    }

    // ============================================================
    // RENDER SERVICE — Connexion LayoutLib
    // ============================================================

    private void setupRenderService() {
        float density = getResources().getDisplayMetrics().density;
        float fontScale = getResources().getDisplayMetrics().scaledDensity;
        float xdpi = getResources().getDisplayMetrics().xdpi;

        renderService = new RenderService(this);
        renderService.setDebounceMs(400);
        // Fit le rendu au device profile (Pixel 4 par défaut)
        DeviceProfile dp = DeviceProfile.PIXEL_4;
        renderService.setTargetDimensions(
                dp.getWidthPx(density),
                dp.getHeightPx(density));
        renderService.setDimensionConverter(new DimensionConverter(density, fontScale, xdpi));

        // ═══ ResourceResolver — CRITIQUE ═══
        ResourceTable table = new ResourceTable();
        resourceResolver = new ResourceResolverImpl(table);
        resourceResolver.setDimensionConverter(new DimensionConverter(density, fontScale, xdpi));
        renderService.setResourceResolver(resourceResolver);

        // ═══ DrawableResolver — shapes, selectors, vectors @drawable/ ═══
        // (repli sur les Resources natives si la résolution échoue)
        renderService.setDrawableResolver(
                new DrawableResolverImpl(resourceResolver,
                        new DimensionConverter(density, fontScale, xdpi)));

        // ═══ ThemeResolver — ?attr/ depuis themes.xml/styles.xml ═══
        // (repli sur le thème natif si l'attribut n'y est pas défini)
        themeResolver = new ThemeResolverImpl(resourceResolver,
                new DimensionConverter(density, fontScale, xdpi));
        themeResolver.setTheme("Theme.MaterialComponents.DayNight");
        renderService.setThemeResolver(themeResolver);

        renderService.setRenderCallback(new RenderService.RenderCallback() {
            @Override
            public void onRenderSuccess(View root, long timeMs, int viewCount,
                                         int width, int height) {
                runOnUiThread(() -> {
                    // 1. Vider le designScroll (retirer l'ancien root + overlay)
                    designScroll.removeAllViews();
                    if (root.getParent() != null) {
                        ((ViewGroup) root.getParent()).removeView(root);
                    }

                    // 2. Créer l'overlay UNE SEULE FOIS
                    if (overlayView == null) {
                        overlayView = new OverlayView(MainActivity.this);
                        overlayView.setSelectionColor(Color.parseColor("#7C5CFF"));
                        overlayView.setBlueprintColor(Color.parseColor("#4AA3FF"));
                        overlayView.setShowHandles(true);
                        overlayView.setShowSelectionLabel(true);
                        overlayView.setShowDimensionTooltip(true);
                        overlayView.setShowMargins(true);
                        overlayView.setShowPadding(true);
                        overlayView.setSnapToGrid(true);
                        overlayView.setGridSizeDp(8);
                        overlayView.setDeviceProfile(DeviceProfile.PIXEL_4);

                        overlayView.setSelectionListener(info -> {
                            selectedView = info;
                            overlayView.setSelectedView(info);
                            updateBsIdChip(info);
                            if (currentBsPanel.equals("properties")) {
                                propertiesPanel.showProperties(info,
                                        session != null ? session.getText() : null);
                            }
                        });

                        overlayView.setHandleDragListener(new OverlayView.OnHandleDragListener() {
                            @Override
                            public void onHandleDrag(ViewInfoCollector.ViewInfo info,
                                                     OverlayView.HandleDirection dir,
                                                     int newWidth, int newHeight) {
                                View renderedView = findRenderedView(info);
                                if (renderedView != null) {
                                    ViewGroup.LayoutParams lp = renderedView.getLayoutParams();
                                    if (lp != null) {
                                        lp.width = newWidth;
                                        lp.height = newHeight;
                                        renderedView.setLayoutParams(lp);
                                    }
                                }
                            }
                            @Override
                            public void onHandleDragEnd(ViewInfoCollector.ViewInfo info,
                                                       int finalWidth, int finalHeight) {
                                // ── Utiliser les valeurs FINALES du drag (pas info.width/height
                                // qui sont immutables et pré-drag). Sinon l'édition utilisateur
                                // serait silencieusement annulée au prochain rendu.
                                if (info != null && info.idName != null) {
                                    updateViewDimensionsInXml(info.idName, finalWidth, finalHeight);
                                }
                            }
                        });

                        overlayView.setOnMoveListener(new OverlayView.OnMoveListener() {
                            @Override
                            public void onMove(ViewInfoCollector.ViewInfo info,
                                               int newLeft, int newTop) {
                                View renderedView = findRenderedView(info);
                                if (renderedView != null) {
                                    float dx = newLeft - info.left;
                                    float dy = newTop - info.top;
                                    renderedView.setTranslationX(dx);
                                    renderedView.setTranslationY(dy);
                                }
                            }
                            @Override
                            public void onMoveEnd(ViewInfoCollector.ViewInfo info,
                                                 int finalLeft, int finalTop) {
                                if (info != null && info.idName != null) {
                                    View renderedView = findRenderedView(info);
                                    if (renderedView != null) {
                                        renderedView.setTranslationX(0);
                                        renderedView.setTranslationY(0);
                                    }
                                    // ── Convertir la position finale (absolue, root-relative)
                                    // en marges par rapport au parent direct. On a besoin de
                                    // l'offset du parent dans le snapshot.
                                    int parentLeft = 0;
                                    int parentTop = 0;
                                    if (currentRootInfo != null) {
                                        ViewInfoCollector.ViewInfo parent = findParentInfo(
                                                currentRootInfo, info);
                                        if (parent != null) {
                                            parentLeft = parent.left;
                                            parentTop = parent.top;
                                        }
                                    }
                                    int newMarginLeft = finalLeft - parentLeft;
                                    int newMarginTop = finalTop - parentTop;
                                    updateViewMarginsInXml(info.idName,
                                            newMarginLeft, newMarginTop);
                                }
                            }
                        });

                        // ── Synchronisation overlay ↔ root rendu ──────────────────────
                        // L'overlay se scalle elle-même (pivot=center) dans applyZoom().
                        // On notifie le listener pour qu'il scalle le root rendu avec
                        // les MÊMES paramètres. Comme les deux ont les mêmes dimensions
                        // et la même position (gravity=CENTER dans designScroll), leurs
                        // pivots sont au même endroit écran → ils scalent symétriquement
                        // et restent alignés.
                        overlayView.setOnZoomPanListener((zoom, panX, panY) -> {
                            applyTransformToRoot(zoom, panX, panY);
                            // Mettre à jour le label % de zoom
                            if (zoomPercent != null) {
                                int pct = Math.round(zoom * 100);
                                zoomPercent.setText(pct + "%");
                            }
                        });
                    }

                    // 3. Ajouter le root et l'overlay comme FRÈRES dans designScroll
                    // (plus de previewContainer intermédiaire, plus de device frame).
                    // Les deux ont les MÊMES dimensions (rootW × rootH) et la MÊME
                    // position (gravity=CENTER). L'overlay est au-dessus du root.
                    int rootW = root.getMeasuredWidth();
                    int rootH = root.getMeasuredHeight();

                    // z=0 : root rendu
                    FrameLayout.LayoutParams rootLp = new FrameLayout.LayoutParams(rootW, rootH);
                    rootLp.gravity = android.view.Gravity.CENTER;
                    root.setLayoutParams(rootLp);
                    designScroll.addView(root);
                    renderedRoot = root;

                    // z=1 : overlay (au-dessus du root)
                    FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(rootW, rootH);
                    overlayLp.gravity = android.view.Gravity.CENTER;
                    overlayView.setLayoutParams(overlayLp);
                    designScroll.addView(overlayView);

                    // ── Réappliquer le zoom dès que l'overlay est laid out ──────────
                    // applyZoom() utilise getWidth()/2 comme pivot. Au premier appel
                    // (avant layout), getWidth()=0 → pivot=(0,0). Dès que l'overlay
                    // est laid out, il faut réappliquer le zoom pour utiliser le bon
                    // pivot (centre) et garder le rendu centré.
                    View.OnLayoutChangeListener layoutListener = (v, left, top, right, bottom,
                                                                 oldLeft, oldTop, oldRight, oldBottom) -> {
                        if (v.getWidth() > 0 && v.getHeight() > 0
                                && overlayView != null) {
                            // Réapplique le zoom courant → recalcule le pivot centré
                            overlayView.setZoom(overlayView.getZoom());
                        }
                    };
                    overlayView.addOnLayoutChangeListener(layoutListener);

                    // Collecter la hiérarchie et la passer à l'overlay
                    currentRootInfo = ViewInfoCollector.collect(root);
                    overlayView.setViewInfo(currentRootInfo);
                    // Le mode de l'overlay dépend du mode courant (design / blueprint)
                    if (currentMode.equals("blueprint")) {
                        overlayView.setMode(OverlayView.Mode.BLUEPRINT_BOXES);
                    } else {
                        overlayView.setMode(OverlayView.Mode.DESIGN);
                    }
                    // Restaurer la sélection précédente si possible
                    if (selectedView != null && currentRootInfo != null) {
                        ViewInfoCollector.ViewInfo refreshed = findViewInfoById(
                                currentRootInfo, selectedView.idName);
                        if (refreshed != null) {
                            overlayView.setSelectedView(refreshed);
                            selectedView = refreshed;
                        }
                    }

                    // Connecter le BlueprintListView
                    blueprintList.setViewInfo(currentRootInfo);
                    blueprintList.setOnSelectionListener(info -> {
                        selectedView = info;
                        if (overlayView != null) overlayView.setSelectedView(info);
                        updateBsIdChip(info);
                        if (currentBsPanel.equals("properties")) {
                            propertiesPanel.showProperties(info,
                                    session != null ? session.getText() : null);
                        }
                    });

                    screenBadge.setText(width + " × " + height + " · " + viewCount + " vues");
                    statusText.setText("OK : " + root.getClass().getSimpleName()
                            + " " + width + "×" + height + " (" + viewCount + " vues, "
                            + timeMs + " ms)");
                    statusText.setTextColor(Color.parseColor("#6FDC8C"));
                });
            }

            @Override
            public void onRenderError(String message, Throwable cause) {
                runOnUiThread(() -> {
                    statusText.setText("Erreur : " + message);
                    statusText.setTextColor(Color.parseColor("#F4757A"));
                });
            }

            @Override
            public void onXmlInvalid(String message) {
                runOnUiThread(() -> {
                    statusText.setText(R.string.status_xml_invalid);
                    statusText.setTextColor(Color.parseColor("#9B95AD"));
                });
            }
        });
    }

    // ============================================================
    // EDITOR (code-editor-lib)
    // ============================================================

    private void setupEditor() {
        // EditorView + EditorSession sont créés dans onCreate
    }

    /**
     * Affiche un placeholder quand le XML est vide (pas de racine).
     * Comme Android Studio qui montre "Nothing to show".
     */
    private void showEmptyPreview() {
        runOnUiThread(() -> {
            if (overlayView != null && overlayView.getChildCount() > 0) {
                overlayView.removeAllViews();
            }
            renderedRoot = null;
            currentRootInfo = null;
            selectedView = null;
            statusText.setText("XML vide");
            statusText.setTextColor(Color.parseColor("#9B95AD"));
            screenBadge.setText("— × — · 0 vues");
        });
    }

    private EditorTheme makeMaterialDarkTheme() {
        return new EditorTheme(
            0xFF0F1226, 0xFF0A0B1A, 0xFF3A3D5C, 0xFF252338,
            0xFFE6E6F0, 0x337C5CFF, 0x08FFFFFF,
            0xFFF4757A, 0xFFFFB74D, 0xFF4AA3FF,
            0xFFF07178, 0xFFC3E88D, 0xFF54668A, 0xFFF78C6C,
            0xFFC792EA, 0xFF82AAFF, 0xFFFFCB6B, 0xFF89DDFF,
            0xFF89DDFF, 0xFFF07178, 0xFFC792EA, 0xFFC792EA,
            0xFFE6E6F0, 0xFFF78C6C, 0xFFC3E88D,
            0x33C792EA, 0xFFC792EA, 0x33F07178,
            0xFF252338, 0xFFFFFFC8, 0xFFE6E6F0
        );
    }

    // ============================================================
    // MODE TABS
    // ============================================================

    private void setupModeTabs() {
        updateModeTabStyle("design");
        tabDesign.setOnClickListener(v -> switchMode("design"));
        tabBlueprint.setOnClickListener(v -> switchMode("blueprint"));
        tabCode.setOnClickListener(v -> switchMode("code"));
    }

    private void switchMode(String mode) {
        currentMode = mode;
        updateModeTabStyle(mode);
        switch (mode) {
            case "design":
                designScroll.setVisibility(View.VISIBLE);
                blueprintScroll.setVisibility(View.GONE);
                codeViewContainer.setVisibility(View.GONE);
                if (overlayView != null) overlayView.setMode(OverlayView.Mode.DESIGN);
                break;
            case "blueprint":
                designScroll.setVisibility(View.GONE);
                blueprintScroll.setVisibility(View.VISIBLE);
                codeViewContainer.setVisibility(View.GONE);
                if (overlayView != null) overlayView.setMode(OverlayView.Mode.BLUEPRINT_BOXES);
                break;
            case "code":
                designScroll.setVisibility(View.GONE);
                blueprintScroll.setVisibility(View.GONE);
                codeViewContainer.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateModeTabStyle(String activeMode) {
        for (MaterialButton btn : new MaterialButton[]{tabDesign, tabBlueprint, tabCode}) {
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setTextColor(Color.parseColor("#9B95AD"));
        }
        MaterialButton active = activeMode.equals("design") ? tabDesign
                : activeMode.equals("blueprint") ? tabBlueprint : tabCode;
        active.setBackgroundColor(Color.parseColor("#4F378B"));
        active.setTextColor(Color.parseColor("#D0BCFF"));
    }

    // ============================================================
    // BOTTOM SHEET TABS
    // ============================================================

    private String currentBsPanel = "properties";

    private void setupBottomSheetTabs() {
        if (bottomSheetBehavior == null) return; // debug mode
        bsTabAttributes.setOnClickListener(v -> setBsPanel("properties"));
        bsTabPalette.setOnClickListener(v -> setBsPanel("palette"));
        bsTabTree.setOnClickListener(v -> setBsPanel("tree"));

        // Grabber : au clic, cycle entre collapsed → half-expanded → expanded
        findViewById(R.id.bsGrabber).setOnClickListener(v -> {
            int state = bottomSheetBehavior.getState();
            switch (state) {
                case BottomSheetBehavior.STATE_COLLAPSED:
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                    break;
                case BottomSheetBehavior.STATE_HALF_EXPANDED:
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    break;
                case BottomSheetBehavior.STATE_EXPANDED:
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    break;
                default:
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    break;
            }
        });

        // Callback pour réagir aux changements d'état (optionnel : log, animation, etc.)
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                // Peut être utilisé pour logger ou ajuster l'UI selon l'état
            }
            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                // slideOffset : 0 = collapsed, 0.5 = half-expanded, 1 = expanded
            }
        });

        setBsPanel("properties");
    }

    private void setBsPanel(String panel) {
        if (bottomSheetBehavior == null) return; // debug mode
        currentBsPanel = panel;
        for (MaterialButton btn : new MaterialButton[]{bsTabAttributes, bsTabPalette, bsTabTree}) {
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setTextColor(Color.parseColor("#9B95AD"));
        }
        MaterialButton activeBtn = panel.equals("properties") ? bsTabAttributes
                : panel.equals("palette") ? bsTabPalette : bsTabTree;
        activeBtn.setBackgroundColor(Color.parseColor("#267C5CFF"));
        activeBtn.setTextColor(Color.parseColor("#D0BCFF"));

        if (panel.equals("properties")) {
            bsTitle.setText(R.string.bs_title_properties);
        } else if (panel.equals("palette")) {
            bsTitle.setText(R.string.bs_title_palette);
        } else {
            bsTitle.setText(R.string.bs_title_hierarchy);
        }

        switch (panel) {
            case "properties":
                propertiesPanel.showProperties(selectedView,
                        session != null ? session.getText() : null);
                break;
            case "palette":
                propertiesPanel.showPalette(name -> {
                    String snippet = PaletteAdapter.createSnippet(name);
                    insertSnippetIntoEditor(snippet);
                    Toast.makeText(this,
                            getString(R.string.toast_added_component, name),
                            Toast.LENGTH_SHORT).show();
                });
                break;
            case "tree":
                propertiesPanel.showTree(currentRootInfo, info -> {
                    selectedView = info;
                    if (overlayView != null) overlayView.setSelectedView(info);
                    updateBsIdChip(info);
                    setBsPanel("properties");
                });
                break;
        }

        // Expand le bottom sheet si collapsed, pour montrer le contenu du panel
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        }
    }

    /**
     * Insère un snippet XML dans l'éditeur à la position du caret.
     */
    private void insertSnippetIntoEditor(String snippet) {
        if (session == null) return;
        int caret = session.getSelection().start;
        suppressTextWatcher = true;
        session.replaceRange(caret, caret, snippet);
        editorView.notifyTextChanged();
        suppressTextWatcher = false;
        renderService.requestRender(session.getText());
        scheduleHistoryPush(session.getText());
    }

    private void updateBsIdChip(ViewInfoCollector.ViewInfo info) {
        if (info != null && info.idName != null) {
            bsIdChip.setText("@" + info.idName);
            bsIdChip.setVisibility(View.VISIBLE);
        } else if (info != null) {
            bsIdChip.setText(info.simpleName);
            bsIdChip.setVisibility(View.VISIBLE);
        } else {
            bsIdChip.setVisibility(View.GONE);
        }
    }

    /**
     * Cherche une ViewInfo par idName dans l'arbre de hiérarchie.
     * Utilisé pour restaurer la sélection après un re-rendu.
     */
    private ViewInfoCollector.ViewInfo findViewInfoById(
            ViewInfoCollector.ViewInfo root, String idName) {
        if (root == null || idName == null) return null;
        if (idName.equals(root.idName)) return root;
        for (ViewInfoCollector.ViewInfo child : root.children) {
            ViewInfoCollector.ViewInfo found = findViewInfoById(child, idName);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Mappe le label d'un champ du PropertiesPanel vers le nom d'attribut XML réel.
     * Par exemple : "marginTop" → "layout_marginTop", "paddingH" → "padding",
     * "gravity" → "layout_gravity", "textSize" → "textSize" (inchangé).
     *
     * @param fieldLabel le label du champ (ex: "marginTop", "text", "gravity")
     * @return le nom d'attribut XML (ex: "layout_marginTop", "text", "layout_gravity")
     */
    private String mapFieldToXmlAttribute(String fieldLabel) {
        switch (fieldLabel) {
            case "marginTop":      return "layout_marginTop";
            case "marginBottom":   return "layout_marginBottom";
            case "paddingH":       return "padding";
            case "gravity":        return "layout_gravity";
            // Les autres labels sont déjà des noms d'attributs valides :
            // id, text, hint, textSize, textStyle, textColor, hintTextColor,
            // layout_width, layout_height, inputType, imeOptions, maxLines,
            // background, backgroundTint
            default:               return fieldLabel;
        }
    }

    /**
     * Trouve la View Android réelle correspondant à une ViewInfo.
     * Utilise l'id de la ViewInfo pour la chercher dans l'arbre rendu
     * via {@code findViewById}. Retourne null si non trouvé (vue sans id
     * ou root non disponible).
     *
     * <p>Utilisé pour le live update pendant le drag : on resize/déplace
     * directement la vue rendue sans passer par le XML.</p>
     */
    private View findRenderedView(ViewInfoCollector.ViewInfo info) {
        if (info == null || renderedRoot == null) return null;
        if (info.id == View.NO_ID) return null;
        try {
            return renderedRoot.findViewById(info.id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Trouve le parent direct d'une ViewInfo dans le snapshot.
     *
     * <p>Utile pour convertir une position absolue (root-relative) en
     * marges (relatives au parent direct) lors d'un drag-to-move.</p>
     *
     * @param root la racine du snapshot
     * @param child la vue dont on cherche le parent
     * @return le parent direct, ou null si non trouvé / si {@code child} est la racine
     */
    private ViewInfoCollector.ViewInfo findParentInfo(
            ViewInfoCollector.ViewInfo root, ViewInfoCollector.ViewInfo child) {
        if (root == null || child == null) return null;
        for (ViewInfoCollector.ViewInfo c : root.children) {
            if (c == child) return root;
            ViewInfoCollector.ViewInfo found = findParentInfo(c, child);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Applique la transformation zoom/pan au root rendu, symétriquement à
     * l'OverlayView (qui se scalle elle-même dans applyZoom()).
     *
     * <p>Le root et l'overlay ont les MÊMES dimensions ({@code rootW × rootH})
     * et la MÊME position ({@code gravity=CENTER} dans {@code designScroll}).
     * Donc leurs pivots (au centre de chaque view) sont au même endroit
     * écran. En scalant les deux avec le même facteur et le même pivot, ils
     * grandissent symétriquement et restent alignés à toutes les échelles.</p>
     *
     * @param zoom le facteur de zoom (1.0 = 100%)
     * @param panX translation X en px (espace local du view)
     * @param panY translation Y en px (espace local du view)
     */
    private void applyTransformToRoot(float zoom, float panX, float panY) {
        if (renderedRoot == null) return;
        // Pivot au CENTRE du root rendu — symétrique à OverlayView.applyZoom()
        float pivotX = renderedRoot.getWidth() > 0
                ? renderedRoot.getWidth() / 2f : 0f;
        float pivotY = renderedRoot.getHeight() > 0
                ? renderedRoot.getHeight() / 2f : 0f;
        renderedRoot.setPivotX(pivotX);
        renderedRoot.setPivotY(pivotY);
        renderedRoot.setScaleX(zoom);
        renderedRoot.setScaleY(zoom);
        renderedRoot.setTranslationX(panX);
        renderedRoot.setTranslationY(panY);
    }

    // ============================================================
    // BOTTOM NAV (BottomNavigationView Material 3)
    // ============================================================

    private void setupBottomNav() {
        // Sélection initiale = Design
        bottomNav.setSelectedItemId(R.id.nav_design);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_design) {
                switchMode("design");
                setBsPanel("properties");
                return true;
            } else if (id == R.id.nav_palette) {
                setBsPanel("palette");
                return true;
            } else if (id == R.id.nav_properties) {
                setBsPanel("properties");
                return true;
            } else if (id == R.id.nav_tree) {
                setBsPanel("tree");
                return true;
            }
            return false;
        });
    }

    // ============================================================
    // FAB + PALETTE POPUP (mini-layoutlib)
    // ============================================================

    private void setupFAB() {
        fab.setOnClickListener(v -> togglePalettePopup());
    }

    private void togglePalettePopup() {
        if (palettePopup != null && palettePopup.isShowing()) {
            palettePopup.dismiss();
            palettePopup = null;
            animateFabRotation(false);
            return;
        }
        palettePopup = new ComponentPalettePopup(this);
        palettePopup.setOnDismissListener(() -> animateFabRotation(false));
        palettePopup.setOnComponentSelectedListener(name -> {
            String snippet = PaletteAdapter.createSnippet(name);
            insertSnippetIntoEditor(snippet);
            Toast.makeText(this,
                    getString(R.string.toast_added_component, name),
                    Toast.LENGTH_SHORT).show();
        });
        palettePopup.setAnimationStyle(R.style.PalettePopupAnimation);
        palettePopup.showAsDropDown(fab, -200, -260);
        animateFabRotation(true);
    }

    private void animateFabRotation(boolean open) {
        fabOpen = open;
        float from = open ? 0f : 135f;
        float to = open ? 135f : 0f;
        RotateAnimation rot = new RotateAnimation(from, to,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rot.setDuration(300);
        rot.setInterpolator(new AccelerateDecelerateInterpolator());
        rot.setFillAfter(true);
        fab.startAnimation(rot);
    }

    // ============================================================
    // ZOOM CONTROLS
    // ============================================================

    private void setupZoomControls() {
        findViewById(R.id.btnZoomIn).setOnClickListener(v -> {
            if (overlayView != null) {
                overlayView.zoomIn();
                updateZoomLabel();
            }
        });
        findViewById(R.id.btnZoomOut).setOnClickListener(v -> {
            if (overlayView != null) {
                overlayView.zoomOut();
                updateZoomLabel();
            }
        });
        findViewById(R.id.btnZoomFit).setOnClickListener(v -> {
            if (overlayView != null) {
                overlayView.resetZoom();
                updateZoomLabel();
            }
        });
    }

    private void updateZoomLabel() {
        if (overlayView != null && zoomPercent != null) {
            int pct = Math.round(overlayView.getZoom() * 100);
            zoomPercent.setText(pct + "%");
        }
    }

    // ============================================================
    // UNDO/REDO (custom, au niveau XML complet)
    // ============================================================

    private void scheduleHistoryPush(String xml) {
        if (pendingHistoryPush != null) historyHandler.removeCallbacks(pendingHistoryPush);
        pendingHistoryPush = () -> {
            undoRedoManager.pushState(xml);
            refreshUndoRedoButtons();
        };
        historyHandler.postDelayed(pendingHistoryPush, HISTORY_DEBOUNCE_MS);
    }

    private void refreshUndoRedoButtons() {
        ImageButton btnUndo = findViewById(R.id.btnUndo);
        ImageButton btnRedo = findViewById(R.id.btnRedo);
        if (btnUndo != null) {
            btnUndo.setEnabled(undoRedoManager.canUndo());
            btnUndo.setAlpha(undoRedoManager.canUndo() ? 1f : 0.35f);
        }
        if (btnRedo != null) {
            btnRedo.setEnabled(undoRedoManager.canRedo());
            btnRedo.setAlpha(undoRedoManager.canRedo() ? 1f : 0.35f);
        }
    }

    private void performUndo() {
        String previous = undoRedoManager.undo();
        if (previous != null) {
            replaceSessionXml(previous);
            refreshUndoRedoButtons();
            Toast.makeText(this, "Undo (" + undoRedoManager.getUndoCount() + " restants)",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void performRedo() {
        String next = undoRedoManager.redo();
        if (next != null) {
            replaceSessionXml(next);
            refreshUndoRedoButtons();
            Toast.makeText(this, "Redo (" + undoRedoManager.getRedoCount() + " restants)",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ============================================================
    // XML MUTATIONS (depuis overlay drag)
    // ============================================================

    private void updateViewDimensionsInXml(String idName, int widthPx, int heightPx) {
        if (session == null) return;
        float density = getResources().getDisplayMetrics().density;
        String widthDp = Math.round(widthPx / density) + "dp";
        String heightDp = Math.round(heightPx / density) + "dp";

        String xml = session.getText();
        String newXml = XmlMutator.setAttributeById(xml, idName, "layout_width", widthDp);
        newXml = XmlMutator.setAttributeById(newXml, idName, "layout_height", heightDp);
        if (!newXml.equals(xml)) {
            replaceSessionXml(newXml);
        }
    }

    private void updateViewMarginsInXml(String idName, int marginLeftPx, int marginTopPx) {
        if (session == null) return;
        float density = getResources().getDisplayMetrics().density;
        String marginLeftDp = Math.round(marginLeftPx / density) + "dp";
        String marginTopDp = Math.round(marginTopPx / density) + "dp";

        String xml = session.getText();
        String newXml = XmlMutator.setAttributeById(xml, idName, "layout_marginLeft", marginLeftDp);
        newXml = XmlMutator.setAttributeById(newXml, idName, "layout_marginTop", marginTopDp);
        if (!newXml.equals(xml)) {
            replaceSessionXml(newXml);
        }
    }

    /**
     * Remplace le XML de la session en créant un nouveau EditorDocument/Session.
     */
    private void replaceSessionXml(String newXml) {
        suppressTextWatcher = true;
        EditorDocument newDoc = EditorDocument.of(newXml);
        session = new EditorSession(newDoc);
        session.setLanguage("xml");
        editorView.setSession(session);
        editorView.setTheme(makeMaterialDarkTheme());
        editorView.setLanguage(null);
        editorView.setFileName("activity_login.xml");
        session.setOnTextEditListener((s, e, i) -> {
            if (suppressTextWatcher) return;
            String x = session.getText();
            if (!x.isEmpty()) {
                renderService.requestRender(x);
                scheduleHistoryPush(x);
            }
            refreshUndoRedoButtons();
        });
        suppressTextWatcher = false;
        renderService.requestRender(newXml);
        scheduleHistoryPush(newXml);
    }

    // ============================================================
    // TOP APP BAR
    // ============================================================

    private void setupTopAppBar() {
        ImageButton btnUndo = findViewById(R.id.btnUndo);
        ImageButton btnRedo = findViewById(R.id.btnRedo);
        ImageButton btnExport = findViewById(R.id.btnExport);
        ImageButton btnMore = findViewById(R.id.btnMore);

        btnUndo.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(150)
                    .withEndAction(() -> v.animate().scaleX(1).scaleY(1).setDuration(150).start())
                    .start();
            performUndo();
        });
        btnRedo.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(150)
                    .withEndAction(() -> v.animate().scaleX(1).scaleY(1).setDuration(150).start())
                    .start();
            performRedo();
        });
        btnExport.setOnClickListener(v -> {
            if (session == null) return;
            String xml = session.getText();
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("XML", xml);
                clipboard.setPrimaryClip(clip);
                if (currentRootInfo != null) {
                    int count = ViewInfoCollector.countViews(currentRootInfo);
                    Toast.makeText(this,
                            getString(R.string.toast_export_done, count, 0),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.toast_xml_copied, Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnMore.setOnClickListener(v ->
                Toast.makeText(this, "Plus d'options (TODO)", Toast.LENGTH_SHORT).show());
    }
}
