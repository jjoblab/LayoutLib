package jo.layoutlib.compose

/**
 * v3.7.0 — S9 du plan PE14 (étude § 5.4) : l'INTERPRÉTEUR d'une
 * `@Composable` TRIVIALE, moitié « arbre résolu » du spike Compose C0.
 *
 * <h2>Le périmètre « triviale » (l'objet du C0)</h2>
 *
 * <p>Le C0 PRouve le côté RENDU (un `ComposeView` composé dans la session
 * LayoutLib existante) — pas le compilateur Kotlin. L'interprétation se
 * limite à la forme la plus étroite :</p>
 *
 * <pre>`@Composable
 * fun NomEcran() {
 *     Text("Un littéral")
 * }`</pre>
 *
 * <p>Extraire : le NOM de la fonction (l'identité de la preview, miroir
 * du FQN de la `@Composable` de la référence) et les LITTÉRAUX des appels
 * `Text("…")` du corps (ordre du document). C'est le placeholder ÉTALON
 * du modèle de symboles du LSP Kotlin (K3+ — PREVIEW.md § 4.2) : la
 * grammaire vendorée du frère résout les vrais arbres dès C1, CE lecteur
 * n'existe que pour MESURER la machinerie de composition, jamais pour
 * produire.</p>
 *
 * <h2>Honnêteté contractuelle (philosophie X4)</h2>
 *
 * <p>Le lecteur SOUS-RAPPORTE : annotée mais sans `fun`, `@Composable`
 * absente, corps vide, littéral non fermé — [rien] (jamais de devinette,
 * jamais d'échec). Un `Text` à variable (`Text(nom)`) est IGNORÉ : sans
 * modèle de symboles, sa valeur est inconnue du C0.</p>
 */
object InterpreteurComposeC0 {
    /**
     * Une `@Composable` triviale interprétée : le nom de la fonction et
     * ses littéraux de `Text`, dans l'ordre du document.
     *
     * @property nom le nom de la fonction `@Composable`.
     * @property textes les littéraux des `Text("…")` du corps.
     */
    data class ComposableTriviale(
        val nom: String,
        val textes: List<String>,
    )

    /** Interprète la source Kotlin — `null` si rien ne se laisse lire (honnête). */
    fun interpreter(source: String): ComposableTriviale? {
        val annotation = source.indexOf("@Composable")
        if (annotation < 0) return null
        val apresAnnotation = source.substring(annotation)
        // Le `fun` qui suit l'annotation (même ligne ou ligne suivante) —
        // borné : un @Composable orphelin sans fonction sous-rapporte.
        val funOffset = apresAnnotation.indexOf("fun ")
        if (funOffset < 0) return null
        val apresFun = apresAnnotation.substring(funOffset + "fun ".length)
        val nom = apresFun.takeWhile { it.isLetterOrDigit() || it == '_' }
        if (nom.isEmpty() || nom.first().isDigit()) return null

        // Le CORPS : de la première '{' après la signature au '}' qui la
        // ferme (compteur d'accolades, chaînes et commentaires sautés —
        // un littéral peut en contenir).
        val ouvertureCorps = apresFun.indexOf('{')
        if (ouvertureCorps < 0) return null
        val corps = corpsEntreAccolades(apresFun.substring(ouvertureCorps))
            ?: return null
        val textes = extraireTextes(corps)
        if (textes.isEmpty()) return null
        return ComposableTriviale(nom = nom, textes = textes)
    }

    /**
     * Le corps entre la '{' d'ouverture [texteOr] et le '}' qui la ferme :
     * compteur d'accolades, littéraux `"…"` et commentaires `//`/`/* */`
     * traversés sans les compter. `null` si jamais fermé (frappe en cours).
     */
    private fun corpsEntreAccolades(texteOr: String): String? {
        var profondeur = 0
        var indice = 0
        while (indice < texteOr.length) {
            when {
                texteOr.startsWith("\"", indice) -> {
                    val fin = fermetureDeChaine(texteOr, indice)
                    if (fin < 0) return null
                    indice = fin
                }

                texteOr.startsWith("//", indice) -> {
                    val fin = texteOr.indexOf('\n', indice)
                    if (fin < 0) return null
                    indice = fin
                }

                texteOr.startsWith("/*", indice) -> {
                    val fin = texteOr.indexOf("*/", indice)
                    if (fin < 0) return null
                    indice = fin + 2
                }

                texteOr[indice] == '{' -> profondeur++

                texteOr[indice] == '}' -> {
                    profondeur--
                    if (profondeur == 0) {
                        return texteOr.substring(1, indice)
                    }
                }
            }
            indice++
        }
        return null
    }

    /** Les littéraux des `Text("…")` du corps, dans l'ordre. */
    private fun extraireTextes(corps: String): List<String> {
        val textes = mutableListOf<String>()
        var reste = corps
        while (true) {
            val appel = reste.indexOf("Text(")
            if (appel < 0) break
            val apres = reste.substring(appel + "Text(".length)
            // L'appel attend un littéral DIRECT (trivial) : Text(  "…"  .
            val guillemet = apres.indexOf('"')
            if (guillemet < 0 || apres.take(guillemet).any { !it.isWhitespace() }) {
                // Text(variable) ou Text(autose) : ignoré, on passe au
                // Text suivant APRÈS cette parenthèse (sous-rapport).
                reste = if (apres.indexOf(')') >= 0) apres.substring(apres.indexOf(')')) else ""
                continue
            }
            val fin = fermetureDeChaine(apres, guillemet)
            if (fin < 0) break
            textes.add(apres.substring(guillemet + 1, fin - 1))
            reste = apres.substring(fin)
        }
        return textes
    }

    /**
     * L'indice JUSTE APRÈS la chaîne ouverte à [debut] (le `"` échappé
     * `\"` traversé) ; -1 si jamais fermée.
     */
    private fun fermetureDeChaine(
        texte: String,
        depart: Int,
    ): Int {
        var indice = depart + 1
        while (indice < texte.length) {
            if (texte[indice] == '\\') {
                indice += 2
                continue
            }
            if (texte[indice] == '"') return indice + 1
            indice++
        }
        return -1
    }
}
