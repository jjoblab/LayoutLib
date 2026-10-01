package jo.layoutlib.validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Programme principal pour générer un rapport de validation à partir des
 * 50 layouts de test.
 *
 * <p>Usage :</p>
 * <pre>{@code
 * java -cp validation.jar jo.layoutlib.validation.ReportMain <output.html>
 * }</pre>
 *
 * <p>Ce programme crée un rapport HTML statique listant les 50 layouts
 * du catalogue avec leurs catégories et les métriques attendues. Il ne
 * réalise pas l'exécution réelle des tests (qui nécessite un device Android)
 * mais sert de template pour le rapport final.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class ReportMain {

    /**
     * Constructeur privé.
     */
    private ReportMain() {
    }

    /**
     * Point d'entrée du programme.
     *
     * @param args args[0] = chemin du fichier HTML de sortie
     */
    public static void main(String[] args) {
        Path outputPath;
        if (args.length >= 1) {
            outputPath = Paths.get(args[0]);
        } else {
            outputPath = Paths.get("validation-report.html");
        }

        List<LayoutTestCase> tests = LayoutTestCatalog.getAllTests();
        System.out.println("Génération du rapport pour " + tests.size() + " layouts...");

        // Génération d'un rapport "template" — les résultats réels doivent
        // être collectés en exécutant les tests instrumentés sur device.
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang='fr'>\n<head>\n");
        sb.append("<meta charset='UTF-8'>\n");
        sb.append("<title>Catalogue de validation — Mini-LayoutLib</title>\n");
        sb.append("<style>\n");
        sb.append("body { font-family: -apple-system, 'Segoe UI', Roboto, sans-serif; ");
        sb.append("margin: 24px; color: #222; background: #f7f7f7; }\n");
        sb.append("h1 { color: #6750A4; border-bottom: 2px solid #6750A4; padding-bottom: 8px; }\n");
        sb.append("h2 { color: #625B71; margin-top: 32px; }\n");
        sb.append("table { border-collapse: collapse; width: 100%; ");
        sb.append("background: white; border-radius: 8px; overflow: hidden; }\n");
        sb.append("th, td { padding: 8px 12px; text-align: left; border-bottom: 1px solid #eee; }\n");
        sb.append("th { background: #6750A4; color: white; }\n");
        sb.append("tr:hover { background: #fff9c4; }\n");
        sb.append(".category { color: #6750A4; font-weight: bold; }\n");
        sb.append(".xml { font-family: monospace; font-size: 12px; color: #666; ");
        sb.append("white-space: pre-wrap; word-break: break-all; max-width: 600px; }\n");
        sb.append("</style>\n</head>\n<body>\n");

        sb.append("<h1>Catalogue de validation — Mini-LayoutLib</h1>\n");
        sb.append("<p>Ce catalogue liste les ").append(tests.size());
        sb.append(" layouts XML de test utilisés pour valider le mini-layoutlib. ");
        sb.append("Chaque layout est exécuté via le ");
        sb.append("<code>LayoutTestHarness</code> et son résultat est collecté ");
        sb.append("dans un <code>LayoutTestResult</code>.</p>\n");

        sb.append("<h2>Liste des tests</h2>\n");
        sb.append("<table>\n<thead><tr>");
        sb.append("<th>#</th><th>Nom</th><th>Catégorie</th><th>XML source</th>");
        sb.append("</tr></thead>\n<tbody>\n");

        int i = 1;
        for (LayoutTestCase test : tests) {
            sb.append("<tr>");
            sb.append("<td>").append(i++).append("</td>");
            sb.append("<td>").append(test.getName()).append("</td>");
            sb.append("<td class='category'>").append(test.getCategory()).append("</td>");
            sb.append("<td class='xml'>")
                    .append(escape(test.getXml())).append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</tbody>\n</table>\n");

        sb.append("<h2>Procédure d'exécution</h2>\n");
        sb.append("<ol>\n");
        sb.append("<li>Lancer un émulateur Android ou connecter un device</li>\n");
        sb.append("<li>Exécuter <code>./gradlew :mini-layoutlib:validation:connectedAndroidTest</code></li>\n");
        sb.append("<li>Le rapport HTML final est généré dans le cache de l'app de test</li>\n");
        sb.append("</ol>\n");

        sb.append("<h2>Métriques cibles</h2>\n");
        sb.append("<ul>\n");
        sb.append("<li><strong>Taux de succès</strong> ≥ 95%</li>\n");
        sb.append("<li><strong>Temps moyen</strong> &lt; 200 ms par layout</li>\n");
        sb.append("<li><strong>Temps max</strong> &lt; 500 ms</li>\n");
        sb.append("<li><strong>Taille APK ajoutée</strong> &lt; 500 KB</li>\n");
        sb.append("</ul>\n");

        sb.append("</body>\n</html>");

        try {
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Rapport généré : " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erreur d'écriture : " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Échappe les caractères HTML spéciaux.
     *
     * @param s la chaîne à échapper
     * @return la chaîne échappée
     */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
