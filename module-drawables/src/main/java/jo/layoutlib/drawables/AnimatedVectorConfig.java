package jo.layoutlib.drawables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration d'un drawable XML de type {@code <animated-vector>}
 * (ADR 0116 — P5/L5).
 *
 * <p>Un animated-vector enveloppe un {@code <vector>} et lui attache des
 * animations par cibles. Deux formes existent dans l'AOSP :</p>
 * <ul>
 *   <li><strong>API 24+</strong> : le vector est désigné par référence
 *       {@code <animated-vector android:drawable="@drawable/icone">} ;</li>
 *   <li><strong>API 21-23</strong> : le vector est embarqué comme premier
 *       enfant inline du {@code <animated-vector>}.</li>
 * </ul>
 *
 * <p>Les cibles {@code <target android:name="…" android:animation="@anim/…"/>}
 * sont capturées pour introspection, mais jamais résolues : en preview
 * statique, seule la <strong>première frame</strong> (le vector tel quel)
 * importe — l'animation n'a pas de sens hors runtime.</p>
 *
 * <p>Les deux représentations du vector sont capturées (référence ET XML
 * inline) ; la résolution donne la priorité à la référence — même contrat que
 * {@link AdaptiveIconConfig}/{@link LayerListConfig}.</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
public class AnimatedVectorConfig {

    /** Référence du vector sous-jacent (ex. {@code @drawable/icone}). */
    private String drawableRef;

    /** XML inline du vector sous-jacent (format API 21-23), ou d'un drawable toléré. */
    private String drawableXml;

    /** Cibles animées capturées (jamais résolues en preview statique). */
    private final List<AnimatedTarget> targets = new ArrayList<>();

    /**
     * @return la référence du vector sous-jacent, ou {@code null}
     */
    public String getDrawableRef() {
        return drawableRef;
    }

    /**
     * @param drawableRef la référence (ex. {@code @drawable/icone})
     */
    public void setDrawableRef(String drawableRef) {
        this.drawableRef = drawableRef;
    }

    /**
     * @return le XML inline du vector sous-jacent, ou {@code null}
     */
    public String getDrawableXml() {
        return drawableXml;
    }

    /**
     * @param drawableXml le XML inline (élément drawable complet)
     */
    public void setDrawableXml(String drawableXml) {
        this.drawableXml = drawableXml;
    }

    /**
     * Ajoute une cible animée capturée.
     *
     * @param target la cible
     */
    public void addTarget(AnimatedTarget target) {
        if (target != null) {
            targets.add(target);
        }
    }

    /**
     * @return les cibles animées capturées (non modifiable)
     */
    public List<AnimatedTarget> getTargets() {
        return Collections.unmodifiableList(targets);
    }

    /**
     * @return le nombre de cibles capturées
     */
    public int getTargetCount() {
        return targets.size();
    }

    /**
     * @return true si un vector sous-jacent est désigné (référence ou inline)
     */
    public boolean hasDrawable() {
        return (drawableRef != null && !drawableRef.isEmpty())
                || (drawableXml != null && !drawableXml.isEmpty());
    }

    /**
     * Une cible {@code <target>} d'animated-vector : associe le nom d'un path
     * du vector à une animation {@code @anim/}.
     *
     * <p>Capture purement informative en preview statique — la première frame
     * n'en dépend pas.</p>
     */
    public static class AnimatedTarget {

        /** Nom du path/group ciblé dans le vector (attribut {@code android:name}). */
        private final String name;

        /** Référence de l'animation (attribut {@code android:animation}). */
        private final String animationRef;

        /**
         * Construit une cible.
         *
         * @param name         le nom ciblé (peut être {@code null})
         * @param animationRef la référence d'animation (peut être {@code null})
         */
        public AnimatedTarget(String name, String animationRef) {
            this.name = name;
            this.animationRef = animationRef;
        }

        /**
         * @return le nom ciblé, ou {@code null}
         */
        public String getName() {
            return name;
        }

        /**
         * @return la référence d'animation, ou {@code null}
         */
        public String getAnimationRef() {
            return animationRef;
        }
    }
}
