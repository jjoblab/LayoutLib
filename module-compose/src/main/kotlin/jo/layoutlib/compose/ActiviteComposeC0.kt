package jo.layoutlib.compose

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * v3.7.0 — S9 du plan PE14 (étude § 5.4) : le HARNAIS du spike Compose C0.
 *
 * <p>Interprète une `@Composable` triviale, la compose dans la session
 * LayoutLib, affiche le BITMAP rendu et les MESURES — le critère de
 * passage du C0 (« mesurer latence + mémoire » — PREVIEW.md § 4). Le
 * « relancer ×10 » chiffe la reproductibilité (médiane, min, max) : la
 * composition PREMIÈRE (entrée en session) et les SUIVANTES (le régime
 * live-edit visé par C1) ne coûtent pas pareil — le spike les SÉPARE.</p>
 *
 * <p>La « fenêtre » de la session : un conteneur INVISIBLE — la
 * `ComposeView` ne compose qu'attachée (ViewTreeLifecycleOwner de
 * l'activité), miroir du futur processus `:preview` possédant sa fenêtre
 * (PREVIEW.md § 4). Lancée depuis l'app démo :
 * {@code adb shell am start -n jo.layoutlib.editor/jo.layoutlib.compose.ActiviteComposeC0}.</p>
 *
 * @author jo@Dev
 * @since 3.7.0
 */
@SuppressLint("SetTextI18n")
class ActiviteComposeC0 :
    AppCompatActivity(),
    Runnable {
    private val session = SessionComposeC0(this)
    private lateinit var texteMesures: TextView
    private lateinit var rendu: ImageView
    private lateinit var fenetreSession: FrameLayout

    /** La source du spike — TRIVIALE par contrat (§ 5.4). */
    private val sourceKotlin =
        """
        @Composable
        fun BonjourCompose() {
            Text("Bonjour, Compose C0 !")
            Text("Composé dans la session LayoutLib.")
        }
        """.trimIndent()

    private var passesRestantes = 0
    private val latencesRendu = mutableListOf<Long>()
    private var mesuresInitiales: SessionComposeC0.MesuresC0? = null
    private var dernierBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val colonne = LinearLayout(this)
        colonne.orientation = LinearLayout.VERTICAL
        colonne.setPadding(24, 96, 24, 24)

        val titre = TextView(this)
        titre.text = "Compose C0 — spike § 5.4"
        titre.textSize = 18f
        colonne.addView(titre)

        rendu = ImageView(this)
        colonne.addView(
            rendu,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        texteMesures = TextView(this)
        texteMesures.typeface = android.graphics.Typeface.MONOSPACE
        texteMesures.textSize = 12f
        colonne.addView(texteMesures)

        val relancer = Button(this)
        relancer.text = "Relancer ×10"
        relancer.setOnClickListener {
            passesRestantes = 10
            latencesRendu.clear()
            relancer.isEnabled = false
            window.decorView.post(this)
        }
        colonne.addView(relancer)

        // La FENÊTRE de la session : conteneur INVISIBLE attaché (la
        // ComposeView y démarre sa composition), jamais dessiné à l'écran.
        fenetreSession = FrameLayout(this)
        fenetreSession.visibility = View.INVISIBLE
        setContentView(colonne)
        addContentView(
            fenetreSession,
            FrameLayout.LayoutParams(1, 1, Gravity.TOP or Gravity.START),
        )

        // Le premier rendu (entrée en composition) se poste : l'activité
        // est attachée, les propriétaires de cycle de vie sont posés.
        window.decorView.post { rendreUnePasse(enregistrerInitiales = true) }
    }

    /**
     * Une passe : rendu de la session ; les SUIVANTES (post-frame) rendent
     * la composition matérialisée — le régime live-edit mesuré par le C0.
     */
    private fun rendreUnePasse(enregistrerInitiales: Boolean = false) {
        dernierBitmap?.recycle()
        val mesures =
            session.rendre(
                sourceKotlin = sourceKotlin,
                hote = fenetreSession,
                largeurPx = 360 * 3,
                hauteurPx = 640 * 3,
            )
        if (mesures == null) {
            texteMesures.text = "Interprétation vide (source non triviale)."
            return
        }
        dernierBitmap = mesures.bitmap
        rendu.setImageBitmap(mesures.bitmap)
        if (enregistrerInitiales) {
            mesuresInitiales = mesures
            texteMesures.text =
                "1re passe — ${mesures.nom}()\n" +
                    "inflation host : ${mesures.latenceInflationMs} ms\n" +
                    "rendu+composition : ${mesures.latenceRenduMs} ms\n" +
                    "PSS total : ${mesures.pssTotalKo} Ko\n" +
                    "(« Relancer ×10 » chiffe le régime stationnaire)"
        } else {
            latencesRendu.add(mesures.latenceRenduMs)
        }
    }

    /** La boucle des passes suivantes — une PAR FRAME (recomposition matérialisée). */
    override fun run() {
        if (passesRestantes <= 0) {
            afficherStatistiques()
            return
        }
        passesRestantes--
        rendreUnePasse()
        window.decorView.post(this)
    }

    private fun afficherStatistiques() {
        val initiales = mesuresInitiales
        val triees = latencesRendu.sorted()
        if (triees.isEmpty() || initiales == null) return
        texteMesures.text =
            "1re passe : ${initiales.latenceRenduMs} ms (entrée en composition)\n" +
                "suivantes ×${triees.size} : médiane ${triees[triees.size / 2]} ms · " +
                "min ${triees.first()} · max ${triees.last()} ms\n" +
                "PSS total : ${initiales.pssTotalKo} Ko (runtime Compose embarqué)\n" +
                "bitmap ${initiales.largeurPx}×${initiales.hauteurPx}"
    }

    override fun onDestroy() {
        session.detacher()
        dernierBitmap?.recycle()
        dernierBitmap = null
        super.onDestroy()
    }
}
