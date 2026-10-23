package jo.layoutlib.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Debug
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.sp
import jo.layoutlib.inflater.BridgeInflater
import jo.layoutlib.inflater.ViewTagRegistry
import jo.layoutlib.layout.LayoutEngineImpl

/**
 * v3.7.0 — S9 du plan PE14 (étude § 5.4) : la SESSION Compose C0 —
 * « interpréter une @Composable triviale, la composer dans un ComposeView
 * DANS LA SESSION LayoutLib EXISTANTE, mesurer latence + mémoire ».
 *
 * <h2>La machinerie (miroir de l'étude § 5.2, précision 1)</h2>
 *
 * <p>« Un `ComposeView` dans la session remplace le `ComposeViewAdapter` de
 * la référence » : l'HÔTE est un layout XML gonflé par le VRAI
 * [BridgeInflater] (la `ComposeView` s'y instancie par RÉFLEXION, comme
 * toute vue — le moteur n'a AUCUNE connaissance de Compose), le contenu
 * composé est l'INTERPRÉTATION de [InterpreteurComposeC0], la
 * mesure/layout passe par le VRAI [LayoutEngineImpl], le bitmap par le
 * vrai `Canvas`. Une seule machinerie, deux dialectes (XML, Compose).</p>
 *
 * <h2>L'attache (le « propriétaire de fenêtre » du C0)</h2>
 *
 * <p>Une `ComposeView` ne compose qu'ATTACHÉE (`onAttachedToWindow` →
 * `ensureCompositionCreated` — il lui faut un `ViewTreeLifecycleOwner`) :
 * le futur processus `:preview` de CodeIDE possédera sa fenêtre (PREVIEW.md
 * § 4) ; le harnais du spike l'incarne par un conteneur INVISIBLE de l'app
 * démo — le [hote] passé à [rendre]. La composition démarre à l'attache,
 * la PREMIÈRE mesure la matérialise (le rendu du moteur la consomme).</p>
 *
 * <h2>Les mesures (l'objet du spike)</h2>
 *
 * <ul>
 *   <li>latence d'INFLATION (le host XML par le moteur) ;</li>
 *   <li>latence de RENDU (interprétation + composition première + mesure
 *       + layout + dessin bitmap — de l'appel au bitmap) ;</li>
 *   <li>mémoire : PSS natif+dalvik (`Debug.MemoryInfo`) autour du rendu —
 *       le Compose runtime embarqué a un coût, le spike le chiffrera.</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 3.7.0
 */
class SessionComposeC0(
    private val contexte: Context,
) {
    /**
     * Les mesures d'un rendu C0 (le RÉSULTAT du spike — PREVIEW.md § 4,
     * critère de passage C1 : latence + mémoire réelles sur appareil).
     *
     * @property nom la composable interprétée (miroir du FQN de la référence).
     * @property textes les littéraux composés.
     * @property latenceInflationMs l'inflation du host par le moteur.
     * @property latenceRenduMs composition première + mesure/layout/dessin.
     * @property pssTotalKo le PSS total du process APRÈS rendu (Ko).
     * @property largeurPx la largeur du bitmap (le profil de la session).
     * @property hauteurPx la hauteur du bitmap.
     * @property bitmap le rendu (pour l'affichage du harnais).
     */
    data class MesuresC0(
        val nom: String,
        val textes: List<String>,
        val latenceInflationMs: Long,
        val latenceRenduMs: Long,
        val pssTotalKo: Long,
        val largeurPx: Int,
        val hauteurPx: Int,
        val bitmap: Bitmap,
    )

    /**
     * Le host XML gonflé par le moteur — `ComposeView` par son nom PLEIN
     * (la résolution par réflexion du ViewFactory, AUCUNE connaissance
     * moteur de Compose) ; le décor ConfigChrome reste OFF par défaut
     * (miroir `showSystemUi` opt-in de l'étude § 5.4).
     */
    private val hostXml: String =
        "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"" +
            " android:layout_width=\"match_parent\"" +
            " android:layout_height=\"match_parent\">" +
            "<androidx.compose.ui.platform.ComposeView" +
            " android:layout_width=\"match_parent\"" +
            " android:layout_height=\"match_parent\" />" +
            "</FrameLayout>"

    /** La dernière racine gonflée (détacheable par le harnais). */
    private var racineCourante: View? = null

    /**
     * Interprète [sourceKotlin], compose le contenu dans la `ComposeView`
     * de la session et rend le bitmap au profil [largeurPx]×[hauteurPx].
     *
     * @param sourceKotlin la source d'une `@Composable` triviale
     * @param hote le conteneur ATTACHÉ (fenêtre du harnais) qui reçoit la
     *        racine — la composition y démarre ; détachez-le entre rendus
     * @param largeurPx la largeur du profil de la session
     * @param hauteurPx la hauteur du profil de la session
     * @return les mesures, ou `null` si la source ne se laisse pas
     *         interpréter (honnête, jamais de composition devinée)
     */
    fun rendre(
        sourceKotlin: String,
        hote: ViewGroup,
        largeurPx: Int,
        hauteurPx: Int,
    ): MesuresC0? {
        val interpretee = InterpreteurComposeC0.interpreter(sourceKotlin) ?: return null

        detacher()
        val inflater = BridgeInflater(contexte, ViewTagRegistry(false))
        val debutInflation = System.nanoTime()
        val racine = inflater.inflate(hostXml)
        val latenceInflation = (System.nanoTime() - debutInflation) / 1_000_000
        racineCourante = racine

        val composeView = chercherComposeView(racine) ?: return null
        composeView.setContent { contenuCompose(interpretee) }

        // L'attache démarre la composition ; la première mesure du moteur
        // (via LayoutEngineImpl) la matérialise — une seule machinerie.
        hote.addView(
            racine,
            FrameLayout.LayoutParams(largeurPx, hauteurPx),
        )

        val moteur = LayoutEngineImpl()
        val debutRendu = System.nanoTime()
        moteur.render(racine, largeurPx, hauteurPx)
        val bitmap = Bitmap.createBitmap(largeurPx, hauteurPx, Bitmap.Config.ARGB_8888)
        racine.draw(Canvas(bitmap))
        val latenceRendu = (System.nanoTime() - debutRendu) / 1_000_000

        val memoire = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoire)
        val pss = memoire.totalPss.toLong()

        return MesuresC0(
            nom = interpretee.nom,
            textes = interpretee.textes,
            latenceInflationMs = latenceInflation,
            latenceRenduMs = latenceRendu,
            pssTotalKo = pss,
            largeurPx = largeurPx,
            hauteurPx = hauteurPx,
            bitmap = bitmap,
        )
    }

    /** Détache la racine courante du harnais (entre rendus). */
    fun detacher() {
        racineCourante?.let { racine ->
            (racine.parent as? ViewGroup)?.removeView(racine)
        }
        racineCourante = null
    }

    /** La `ComposeView` du host (traversée d'instance — AUCUN id résolu). */
    private fun chercherComposeView(racine: View): ComposeView? {
        if (racine is ComposeView) return racine
        if (racine is ViewGroup) {
            for (enfant in racine.children()) {
                chercherComposeView(enfant)?.let { return it }
            }
        }
        return null
    }

    private fun ViewGroup.children(): List<View> =
        List(childCount) { getChildAt(it) }

    /**
     * Le CONTENU composé — TRIVIAL par contrat : le nom de la composable
     * (titre de la preview, miroir du panneau AS) puis ses littéraux, sous
     * le thème Material3 (la session thème de C1 branchera le thème du
     * PROJET par les résolveurs existants).
     */
    @Composable
    private fun contenuCompose(interpretee: InterpreteurComposeC0.ComposableTriviale) {
        MaterialTheme {
            androidx.compose.foundation.layout.Column {
                Text(
                    text = interpretee.nom + "()",
                    fontSize = 12.sp,
                )
                interpretee.textes.forEach { litteral ->
                    Text(text = litteral, fontSize = 20.sp)
                }
            }
        }
    }
}
