package jo.layoutlib.validation;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Générateur de rapport HTML pour les tests de validation.
 *
 * <p>Cette classe transforme une liste de {@link LayoutTestResult} en un
 * rapport HTML lisible présentant :</p>
 *
 * <ul>
 *   <li>Statistiques globales (taux de succès, temps moyen, etc.)</li>
 *   <li>Tableau détaillé par layout (résultat, temps, nombre de vues)</li>
 *   <li>Groupement par catégorie</li>
 *   <li>Mise en évidence des tests en échec</li>
 * </ul>
 *
 * <p>Le rapport généré est un fichier HTML autonome (CSS inline) qui peut
 * être ouvert dans n'importe quel navigateur.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ValidationReportGenerator {

    /** Seuil de performance acceptable (ms). */
    private static final long PERF_THRESHOLD_MS = 200;

    /**
     * Constructeur privé : classe utilitaire.
     */
    private ValidationReportGenerator() {
    }

    /**
     * Génère un rapport HTML complet à partir d'une liste de résultats.
     *
     * @param results la liste des résultats
     * @return le HTML du rapport
     */
    public static String generateHtml(List<LayoutTestResult> results) {
        if (results == null || results.isEmpty()) {
            return "<html><body><h1>Aucun résultat</h1></body></html>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"fr\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<title>Rapport de validation — Mini-LayoutLib</title>\n");
        sb.append("<style>").append(getCss()).append("</style>\n");
        sb.append("</head>\n<body>\n");

        sb.append("<h1>Rapport de validation — Mini-LayoutLib</h1>\n");
        sb.append("<p class='generated'>Généré le ")
                .append(new java.util.Date()).append("</p>\n");

        // Statistiques globales
        sb.append(generateGlobalStats(results));

        // Tableau détaillé
        sb.append(generateDetailedTable(results));

        // Par catégorie
        sb.append(generateByCategory(results));

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    /**
     * Génère la section des statistiques globales.
     */
    private static String generateGlobalStats(List<LayoutTestResult> results) {
        int total = results.size();
        long success = results.stream().filter(LayoutTestResult::isSuccess).count();
        long failure = total - success;
        double successRate = (double) success / total * 100;
        double avgTime = results.stream()
                .filter(LayoutTestResult::isSuccess)
                .mapToLong(LayoutTestResult::getTotalTimeMs)
                .average()
                .orElse(0);
        long maxTime = results.stream()
                .filter(LayoutTestResult::isSuccess)
                .mapToLong(LayoutTestResult::getTotalTimeMs)
                .max()
                .orElse(0);
        long slowTests = results.stream()
                .filter(LayoutTestResult::isSuccess)
                .filter(r -> !r.meetsPerformanceTarget())
                .count();
        int totalViews = results.stream()
                .filter(LayoutTestResult::isSuccess)
                .mapToInt(LayoutTestResult::getViewCount)
                .sum();

        StringBuilder sb = new StringBuilder();
        sb.append("<section class='stats'>\n");
        sb.append("<h2>Statistiques globales</h2>\n");
        sb.append("<div class='stat-grid'>\n");
        sb.append(statCard("Total tests", String.valueOf(total), "neutral"));
        sb.append(statCard("Succès", String.valueOf(success), "success"));
        sb.append(statCard("Échecs", String.valueOf(failure), failure > 0 ? "failure" : "neutral"));
        sb.append(statCard("Taux de succès",
                String.format("%.1f%%", successRate),
                successRate >= 95 ? "success" : "warning"));
        sb.append(statCard("Temps moyen",
                String.format("%.1f ms", avgTime), "neutral"));
        sb.append(statCard("Temps max", maxTime + " ms",
                maxTime > PERF_THRESHOLD_MS ? "warning" : "success"));
        sb.append(statCard("Tests lents (>200ms)",
                String.valueOf(slowTests), slowTests > 0 ? "warning" : "success"));
        sb.append(statCard("Vues créées (total)",
                String.valueOf(totalViews), "neutral"));
        sb.append("</div>\n");
        sb.append("<div class='target'>");
        sb.append("<strong>Cible :</strong> ≥ 95% de succès, &lt; 200ms par layout");
        sb.append("</div>\n");
        sb.append("</section>\n");
        return sb.toString();
    }

    /**
     * Génère une carte de statistique.
     */
    private static String statCard(String label, String value, String cssClass) {
        return String.format(
                "<div class='stat-card %s'><div class='stat-value'>%s</div>"
                        + "<div class='stat-label'>%s</div></div>\n",
                cssClass, value, label);
    }

    /**
     * Génère le tableau détaillé des résultats.
     */
    private static String generateDetailedTable(List<LayoutTestResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("<section>\n<h2>Détail par layout</h2>\n");
        sb.append("<table>\n<thead><tr>");
        sb.append("<th>#</th><th>Nom</th><th>Catégorie</th><th>Résultat</th>");
        sb.append("<th>Vues</th><th>Profondeur</th><th>Inflation</th>");
        sb.append("<th>Layout</th><th>Total</th><th>Dimensions</th>");
        sb.append("</tr></thead>\n<tbody>\n");
        int i = 1;
        for (LayoutTestResult r : results) {
            sb.append("<tr class='").append(r.isSuccess() ? "ok" : "fail").append("'>");
            sb.append("<td>").append(i++).append("</td>");
            sb.append("<td>").append(r.getLayoutName()).append("</td>");
            sb.append("<td>").append(extractCategory(r.getLayoutName())).append("</td>");
            sb.append("<td>").append(r.isSuccess() ? "✓" : "✗").append("</td>");
            sb.append("<td>").append(r.getViewCount()).append("</td>");
            sb.append("<td>").append(r.getMaxDepth()).append("</td>");
            sb.append("<td>").append(r.getInflationTimeMs()).append(" ms</td>");
            sb.append("<td>").append(r.getLayoutTimeMs()).append(" ms</td>");
            sb.append("<td>").append(r.getTotalTimeMs()).append(" ms</td>");
            if (r.isSuccess()) {
                sb.append("<td>").append(r.getMeasuredWidth()).append("×")
                        .append(r.getMeasuredHeight()).append("</td>");
            } else {
                sb.append("<td colspan='1' class='error'>")
                        .append(escape(r.getErrorMessage())).append("</td>");
            }
            sb.append("</tr>\n");
        }
        sb.append("</tbody>\n</table>\n</section>\n");
        return sb.toString();
    }

    /**
     * Génère la section groupée par catégorie.
     */
    private static String generateByCategory(List<LayoutTestResult> results) {
        Map<String, List<LayoutTestResult>> byCategory = results.stream()
                .collect(Collectors.groupingBy(
                        r -> extractCategory(r.getLayoutName()),
                        TreeMap::new,
                        Collectors.toList()));

        StringBuilder sb = new StringBuilder();
        sb.append("<section>\n<h2>Par catégorie</h2>\n");
        sb.append("<table>\n<thead><tr>");
        sb.append("<th>Catégorie</th><th>Total</th><th>Succès</th>");
        sb.append("<th>Échecs</th><th>Taux</th><th>Temps moyen</th>");
        sb.append("</tr></thead>\n<tbody>\n");
        for (Map.Entry<String, List<LayoutTestResult>> entry : byCategory.entrySet()) {
            String cat = entry.getKey();
            List<LayoutTestResult> catResults = entry.getValue();
            int total = catResults.size();
            long success = catResults.stream().filter(LayoutTestResult::isSuccess).count();
            long failure = total - success;
            double rate = (double) success / total * 100;
            double avgTime = catResults.stream()
                    .filter(LayoutTestResult::isSuccess)
                    .mapToLong(LayoutTestResult::getTotalTimeMs)
                    .average()
                    .orElse(0);
            sb.append("<tr>");
            sb.append("<td>").append(cat).append("</td>");
            sb.append("<td>").append(total).append("</td>");
            sb.append("<td>").append(success).append("</td>");
            sb.append("<td>").append(failure).append("</td>");
            sb.append("<td>").append(String.format("%.0f%%", rate)).append("</td>");
            sb.append("<td>").append(String.format("%.1f ms", avgTime)).append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</tbody>\n</table>\n</section>\n");
        return sb.toString();
    }

    /**
     * Extrait la catégorie depuis le nom du layout (avant le premier _).
     */
    private static String extractCategory(String layoutName) {
        // Les noms sont au format "01_textview_simple" — on retourne
        // un identifiant de catégorie basé sur le numéro
        if (layoutName == null) return "unknown";
        try {
            int num = Integer.parseInt(layoutName.substring(0, 2));
            if (num <= 10) return "basic";
            if (num <= 20) return "nested";
            if (num <= 25) return "resources";
            if (num <= 33) return "drawables";
            if (num <= 37) return "theme";
            if (num <= 42) return "special";
            if (num <= 45) return "material";
            return "complex";
        } catch (NumberFormatException e) {
            return "unknown";
        }
    }

    /**
     * Échappe les caractères HTML spéciaux.
     */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * @return le CSS inline pour le rapport
     */
    private static String getCss() {
        return "body { font-family: -apple-system, 'Segoe UI', Roboto, sans-serif; "
                + "margin: 24px; color: #222; background: #f7f7f7; }"
                + "h1 { color: #6750A4; border-bottom: 2px solid #6750A4; padding-bottom: 8px; }"
                + "h2 { color: #625B71; margin-top: 32px; }"
                + ".generated { color: #888; font-size: 0.9em; }"
                + "section { background: white; padding: 16px 24px; margin-bottom: 24px; "
                + "border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }"
                + ".stat-grid { display: grid; "
                + "grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); "
                + "gap: 12px; margin: 16px 0; }"
                + ".stat-card { padding: 16px; border-radius: 8px; text-align: center; "
                + "border-left: 4px solid #999; }"
                + ".stat-card.success { background: #E8F5E9; border-color: #4CAF50; }"
                + ".stat-card.failure { background: #FFEBEE; border-color: #F44336; }"
                + ".stat-card.warning { background: #FFF8E1; border-color: #FF9800; }"
                + ".stat-card.neutral { background: #F5F5F5; border-color: #999; }"
                + ".stat-value { font-size: 28px; font-weight: bold; color: #333; }"
                + ".stat-label { font-size: 13px; color: #666; margin-top: 4px; }"
                + ".target { margin-top: 12px; padding: 12px; background: #E3F2FD; "
                + "border-radius: 4px; color: #1565C0; }"
                + "table { border-collapse: collapse; width: 100%; margin-top: 12px; "
                + "font-size: 14px; }"
                + "th, td { padding: 8px 12px; text-align: left; border-bottom: 1px solid #eee; }"
                + "th { background: #6750A4; color: white; font-weight: 600; }"
                + "tr.ok { background: #C8E6C9; }"
                + "tr.fail { background: #FFCDD2; }"
                + "td.error { color: #C62828; font-family: monospace; font-size: 12px; }"
                + "tr:hover { background: #fff9c4; }";
    }
}
