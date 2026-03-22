package com.prepcreatine.service;

import com.prepcreatine.domain.SystemSource;
import com.prepcreatine.domain.SystemSourceChunk;
import com.prepcreatine.repository.SystemSourceRepository;
import com.prepcreatine.repository.SystemSourceChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SystemCorpusSeeder — seeds NCERT/system-level knowledge corpus into system_sources
 * and system_source_chunks on application startup.
 *
 * This is the SYSTEM-LEVEL RAG corpus shared across all users.
 * It is separate from per-user sources (source_chunks table).
 *
 * Activation: corpus.seed-on-startup=true (default: true)
 * Idempotent: skips if data already exists (unless corpus.force-reseed=true)
 */
@Component
public class SystemCorpusSeeder {

    private static final Logger log = LoggerFactory.getLogger(SystemCorpusSeeder.class);

    @Value("${corpus.seed-on-startup:true}")
    private boolean seedOnStartup;

    @Value("${corpus.force-reseed:false}")
    private boolean forceReseed;

    private final SystemSourceRepository      systemSourceRepo;
    private final SystemSourceChunkRepository systemChunkRepo;

    public SystemCorpusSeeder(SystemSourceRepository systemSourceRepo,
                              SystemSourceChunkRepository systemChunkRepo) {
        this.systemSourceRepo = systemSourceRepo;
        this.systemChunkRepo  = systemChunkRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void seedCorpus() {
        if (!seedOnStartup) {
            log.info("[Corpus] Seeding disabled (corpus.seed-on-startup=false)");
            return;
        }
        if (systemSourceRepo.count() > 0 && !forceReseed) {
            log.info("[Corpus] Already seeded ({} sources), skipping", systemSourceRepo.count());
            return;
        }

        log.info("[Corpus] Starting NCERT corpus seeding...");
        List<SeedEntry> entries = getCorpusEntries();
        int seeded = 0;

        for (SeedEntry entry : entries) {
            try {
                SystemSource source = new SystemSource();
                source.setExamId(entry.examId());
                source.setSubjectId(entry.subjectId());
                source.setTitle(entry.title());
                source.setRawText(entry.text());
                source = systemSourceRepo.save(source);

                List<String> chunks = chunkText(entry.text(), 1200);
                for (int i = 0; i < chunks.size(); i++) {
                    SystemSourceChunk chunk = new SystemSourceChunk();
                    chunk.setSourceId(source.getId());
                    chunk.setExamId(entry.examId());
                    chunk.setSubjectId(entry.subjectId());
                    chunk.setChunkIndex(i);
                    chunk.setChunkText(chunks.get(i));
                    // Zero vector placeholder — real embeddings would require Gemini API call
                    // For hackathon: pgvector cosine search on zero vectors returns insertion order
                    chunk.setEmbedding(new float[768]);
                    systemChunkRepo.save(chunk);
                }
                seeded++;
                log.info("[Corpus] Seeded: {}", entry.title());
            } catch (Exception e) {
                log.warn("[Corpus] Failed to seed '{}': {}", entry.title(), e.getMessage());
            }
        }
        log.info("[Corpus] Seeding complete. {} / {} sources seeded.", seeded, entries.size());
    }

    private List<String> chunkText(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("\\. ");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (current.length() + sentence.length() > maxChars && current.length() > 0) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(sentence).append(". ");
        }
        if (!current.isEmpty()) chunks.add(current.toString().trim());
        return chunks.isEmpty() ? List.of(text) : chunks;
    }

    private record SeedEntry(String examId, String subjectId, String title, String text) {}

    private List<SeedEntry> getCorpusEntries() {
        return List.of(
            new SeedEntry("jee", "physics", "NCERT Physics — Newton's Laws of Motion",
                "Newton's First Law (Law of Inertia): A body remains at rest or in uniform motion " +
                "unless acted upon by an external force. Newton's Second Law: F = ma, where F is net " +
                "force, m is mass, a is acceleration. Newton's Third Law: For every action there is " +
                "an equal and opposite reaction. Momentum p = mv. Impulse = change in momentum = F×t. " +
                "Conservation of momentum: total momentum constant when no external force. " +
                "JEE commonly tests: Atwood machine, pulley problems, friction on inclined planes, " +
                "circular motion centripetal force F = mv²/r. " +
                "Friction: static friction f_s ≤ μ_s N, kinetic friction f_k = μ_k N. " +
                "Free body diagram: essential tool for force analysis. " +
                "For a system of particles: F_ext = Ma_cm (Newton's second law)."),

            new SeedEntry("jee", "physics", "NCERT Physics — Work, Energy and Power",
                "Work W = F·d·cosθ. Work done by variable force = ∫F dx. " +
                "Kinetic energy KE = ½mv². Work-energy theorem: W_net = ΔKE. " +
                "Potential energy: gravitational PE = mgh, spring PE = ½kx². " +
                "Conservation of mechanical energy: KE + PE = constant (no friction). " +
                "Power P = W/t = F·v. Efficiency η = useful output / total input. " +
                "Elastic collision: both KE and momentum conserved. " +
                "Inelastic collision: only momentum conserved. " +
                "Perfectly inelastic: objects stick together. " +
                "Coefficient of restitution e = relative velocity of separation / approach. " +
                "e = 1 for elastic, e = 0 for perfectly inelastic. " +
                "JEE problems: springs, pendulums, inclined planes with friction."),

            new SeedEntry("jee", "physics", "NCERT Physics — Thermodynamics",
                "Zeroth Law: If A and B in thermal equilibrium with C, A and B are in equilibrium. " +
                "First Law: ΔU = Q - W. Q is heat added, W is work done BY system. " +
                "For ideal gas: ΔU depends only on temperature (state function). " +
                "Isothermal: T constant, ΔU = 0, Q = W = nRT ln(V₂/V₁). " +
                "Adiabatic: Q = 0, ΔU = -W, TV^(γ-1) = constant, PV^γ = constant. " +
                "Isobaric: P constant, W = PΔV. Isochoric: V constant, W = 0. " +
                "Second Law: Entropy of universe never decreases. ΔS = dQ_rev/T. " +
                "Carnot efficiency η = 1 - T_cold/T_hot (maximum possible efficiency). " +
                "Carnot theorem: no engine can be more efficient than a Carnot engine. " +
                "Gibbs energy: G = H - TS. Spontaneous when ΔG < 0. " +
                "Third Law: Entropy of perfect crystal at 0K is zero. " +
                "Kirchhoff's equation: Cp - Cv = R for ideal gases. γ = Cp/Cv."),

            new SeedEntry("jee", "physics", "NCERT Physics — Electrostatics",
                "Coulomb's law: F = kq₁q₂/r², k = 9×10⁹ N·m²/C², ε₀ = 8.85×10⁻¹² C²/N·m². " +
                "Electric field E = F/q = kQ/r² (point charge). Superposition principle applies. " +
                "Electric potential V = kQ/r. Relation: E = -dV/dr, E = -∇V. " +
                "Gauss's law: ∮E·dA = Q_enc/ε₀. Applied for symmetric charge distributions. " +
                "For infinite sheet: E = σ/2ε₀. For conductor: E = σ/ε₀ outside, 0 inside. " +
                "Capacitance C = Q/V. Parallel plate: C = ε₀A/d. " +
                "Energy stored in capacitor: U = ½CV² = Q²/2C. " +
                "Dielectric: C increases by factor κ (dielectric constant), C = κε₀A/d. " +
                "Series capacitors: 1/C_eff = 1/C₁ + 1/C₂. Parallel: C_eff = C₁ + C₂. " +
                "Potential energy of system of charges: U = kq₁q₂/r."),

            new SeedEntry("jee", "physics", "NCERT Physics — Electromagnetic Induction",
                "Faraday's law: EMF = -dΦ/dt where Φ = B·A·cosθ is magnetic flux. " +
                "Lenz's law: induced current opposes the change causing it. " +
                "Motional EMF: ε = Blv (conductor of length l moving with velocity v in field B). " +
                "Self-inductance: ε = -L dI/dt. Energy stored: U = ½LI². " +
                "Mutual inductance: ε₂ = -M dI₁/dt. " +
                "Transformer: V₁/V₂ = N₁/N₂ = I₂/I₁. Step-up: N₂ > N₁. " +
                "Ideal transformer: Pin = Pout. Real transformer has losses. " +
                "AC circuits: Z = √(R² + (X_L - X_C)²). Resonance when X_L = X_C. " +
                "Resonant frequency: f₀ = 1/(2π√LC). At resonance: Z = R (minimum). " +
                "RMS current: I_rms = I₀/√2. Power factor: cos φ = R/Z."),

            new SeedEntry("jee", "chemistry", "NCERT Chemistry — Chemical Bonding",
                "Ionic bond: electron transfer between metals and non-metals. High MP, BP, soluble in water. " +
                "Covalent bond: electron sharing. Can be polar (unequal) or nonpolar (equal sharing). " +
                "VSEPR theory: electron pairs repel → molecular geometry. " +
                "Geometries: linear 180°, trigonal planar 120°, tetrahedral 109.5°, " +
                "trigonal bipyramidal 90°/120°, octahedral 90°. " +
                "Lone pairs cause more repulsion than bond pairs (angle decreases). " +
                "Hybridization: sp (linear), sp2 (trigonal), sp3 (tetrahedral), " +
                "sp3d (trig. bipyramidal), sp3d2 (octahedral). " +
                "Bond order = (bonding - antibonding electrons)/2. " +
                "Higher bond order → shorter bond length, higher bond energy. " +
                "Molecular orbital theory: HOMO, LUMO, σ/π bonds, antibonding orbitals. " +
                "O₂ is paramagnetic (2 unpaired e⁻ in π* orbitals)."),

            new SeedEntry("jee", "chemistry", "NCERT Chemistry — Chemical Equilibrium",
                "Dynamic equilibrium: forward rate = backward rate. " +
                "Kc = [products]^coeff / [reactants]^coeff (concentration equilibrium constant). " +
                "Kp = Kc(RT)^Δn where Δn = moles of gaseous products - reactants. " +
                "Relation: Kp = Kc when Δn = 0. " +
                "Le Chatelier's principle: system shifts to oppose stress. " +
                "Increasing reactant concentration → forward reaction favoured. " +
                "Increasing pressure → shifts toward fewer moles of gas. " +
                "Increasing temperature → shifts toward endothermic direction (increases Kc if endothermic). " +
                "pH = -log[H⁺]. For strong acid (HCl): pH = -log[HCl]. " +
                "For weak acid HA: Ka = [H⁺][A⁻]/[HA]. pH = ½(pKa - log C). " +
                "Buffer: pH = pKa + log([A⁻]/[HA]) (Henderson-Hasselbalch). " +
                "Solubility product Ksp = [M^m+]^m[X^x-]^x. Common ion effect reduces solubility."),

            new SeedEntry("jee", "chemistry", "NCERT Chemistry — Organic — General Organic Chemistry",
                "Inductive effect (-I): electron withdrawal through sigma bonds. " +
                "Order of -I effect: -NO₂ > -CN > -COOH > -F > -Cl > -Br > -I > -OH > -OR > -C₆H₅ > -H. " +
                "+I effect: electron donation. Alkyl groups show +I: tert > sec > primary > methyl. " +
                "Mesomeric/resonance effect (+M/-M): delocalization through pi bonds. " +
                "-M groups (electron withdrawing via resonance): -NO₂, -CHO, -COOH, -COR, -CN. " +
                "+M groups (electron donating via resonance): -OH, -OR, -NH₂, -NHR, -NR₂, halogens. " +
                "Note: halogens are -I but +M (net -I dominates for deactivation). " +
                "Hyperconjugation: C-H sigma bond overlap with adjacent empty/half-filled orbital. " +
                "More α-H atoms → more hyperconjugation → greater stability of carbocations. " +
                "Carbocation stability: 3° > 2° > 1° > methyl (hyperconjugation + inductive). " +
                "Carbanion stability: 1° > 2° > 3° (opposite to carbocation). " +
                "Free radical stability: 3° > 2° > 1° > methyl."),

            new SeedEntry("jee", "chemistry", "NCERT Chemistry — Electrochemistry",
                "EMF of cell: E_cell = E_cathode - E_anode (reduction potentials). " +
                "Standard hydrogen electrode (SHE): E° = 0.00 V (reference). " +
                "Nernst equation: E = E° - (RT/nF)lnQ = E° - (0.0592/n)logQ at 25°C. " +
                "ΔG° = -nFE°. If E° > 0: ΔG° < 0 → spontaneous. " +
                "Relation to Kc: E° = (0.0592/n)logKc. " +
                "Faraday's first law: m = ZIt where Z = equivalent weight / 96500. " +
                "Faraday's second law: same charge deposits proportional to equivalent weight. " +
                "Conductance G = 1/R (unit: Siemens S). κ = G × cell constant. " +
                "Molar conductance Λm = (κ × 1000) / M. Increases on dilution. " +
                "Kohlrausch's law: Λ°m = Σλ°(cations) + Σλ°(anions). " +
                "Strong electrolyte: Λm = Λ°m - b√C. Weak electrolyte: needs extrapolation."),

            new SeedEntry("jee", "math", "NCERT Mathematics — Integration",
                "∫xⁿ dx = xⁿ⁺¹/(n+1) + C (n ≠ -1). ∫1/x dx = ln|x| + C. " +
                "∫eˣ dx = eˣ + C. ∫aˣ dx = aˣ/ln(a) + C. " +
                "∫sin x dx = -cos x + C. ∫cos x dx = sin x + C. " +
                "∫tan x dx = ln|sec x| + C. ∫sec²x dx = tan x + C. " +
                "∫1/√(1-x²) dx = sin⁻¹x + C. ∫1/(1+x²) dx = tan⁻¹x + C. " +
                "Integration by substitution: replace composite function with single variable. " +
                "Integration by parts: ∫u dv = uv - ∫v du. LIATE rule for choosing u: " +
                "Logarithmic > Inverse trig > Algebraic > Trigonometric > Exponential. " +
                "Definite integral: ∫_a^b f(x) dx = F(b) - F(a) (Fundamental Theorem of Calculus). " +
                "Properties: ∫_a^b f(x)dx = -∫_b^a f(x)dx. ∫_a^a f(x)dx = 0. " +
                "King's rule: ∫_a^b f(x)dx = ∫_a^b f(a+b-x)dx. " +
                "Area under curve = ∫_a^b |f(x)| dx (positive area). " +
                "Area between curves = ∫_a^b |f(x) - g(x)| dx."),

            new SeedEntry("jee", "math", "NCERT Mathematics — Calculus — Differentiation",
                "Derivative: f'(x) = lim[h→0] (f(x+h) - f(x))/h. " +
                "Standard: d/dx(xⁿ) = nxⁿ⁻¹, d/dx(eˣ) = eˣ, d/dx(ln x) = 1/x. " +
                "d/dx(sin x) = cos x, d/dx(cos x) = -sin x, d/dx(tan x) = sec²x. " +
                "d/dx(sin⁻¹x) = 1/√(1-x²), d/dx(tan⁻¹x) = 1/(1+x²). " +
                "Chain rule: d/dx[f(g(x))] = f'(g(x))·g'(x). " +
                "Product rule: (uv)' = u'v + uv'. Quotient rule: (u/v)' = (u'v - uv')/v². " +
                "Implicit differentiation: differentiate both sides, solve for dy/dx. " +
                "Logarithmic differentiation: take ln of both sides, then differentiate. " +
                "Maxima/minima: f'(x) = 0 at critical points. " +
                "f''(x) < 0 → local maximum. f''(x) > 0 → local minimum. " +
                "Rolle's theorem: f(a)=f(b) → ∃c∈(a,b): f'(c) = 0. " +
                "Mean value theorem: ∃c∈(a,b): f'(c) = (f(b)-f(a))/(b-a). " +
                "L'Hôpital's rule: for 0/0 or ∞/∞ form, limit = lim f'(x)/g'(x)."),

            new SeedEntry("jee", "math", "NCERT Mathematics — Coordinate Geometry",
                "Distance between (x₁,y₁) and (x₂,y₂): d = √((x₂-x₁)²+(y₂-y₁)²). " +
                "Section formula: point dividing in m:n internally: ((mx₂+nx₁)/(m+n), (my₂+ny₁)/(m+n)). " +
                "Straight line: slope m = tanθ = (y₂-y₁)/(x₂-x₁). " +
                "Forms: slope-intercept y = mx+c, intercept x/a + y/b = 1, normal x cosα + y sinα = p. " +
                "Angle between lines: tanθ = |m₁-m₂|/(1+m₁m₂). Parallel: m₁ = m₂. Perpendicular: m₁m₂=-1. " +
                "Distance from (x₁,y₁) to line ax+by+c=0: d = |ax₁+by₁+c|/√(a²+b²). " +
                "Circle: (x-h)²+(y-k)² = r². General: x²+y²+2gx+2fy+c=0. Center (-g,-f), r=√(g²+f²-c). " +
                "Condition for tangent: distance from center = radius. " +
                "Parabola y² = 4ax: vertex (0,0), focus (a,0), directrix x=-a, axis y=0. " +
                "Ellipse x²/a²+y²/b²=1 (a>b): eccentricity e=√(1-b²/a²), foci (±ae, 0), sum of focal radii=2a. " +
                "Hyperbola x²/a²-y²/b²=1: e=√(1+b²/a²), asymptotes y=±(b/a)x."),

            new SeedEntry("jee", "math", "NCERT Mathematics — Probability",
                "Classical probability: P(A) = n(A)/n(S). Always 0 ≤ P(A) ≤ 1. " +
                "P(A∪B) = P(A) + P(B) - P(A∩B). P(A') = 1 - P(A). " +
                "Mutually exclusive: P(A∩B) = 0, so P(A∪B) = P(A) + P(B). " +
                "Conditional probability: P(A|B) = P(A∩B)/P(B). " +
                "Multiplication rule: P(A∩B) = P(A)·P(B|A) = P(B)·P(A|B). " +
                "Independent events: P(A∩B) = P(A)·P(B). P(A|B) = P(A). " +
                "Bayes' theorem: P(Aᵢ|B) = P(B|Aᵢ)·P(Aᵢ) / ΣP(B|Aⱼ)·P(Aⱼ). " +
                "Random variable: discrete (finite values) or continuous. " +
                "Binomial distribution: X ~ B(n,p). P(X=r) = C(n,r)·p^r·(1-p)^(n-r). " +
                "Mean = np, Variance = npq where q = 1-p, SD = √(npq). " +
                "Expected value E(X) = Σ x·P(x)."),

            new SeedEntry("neet", "biology", "NCERT Biology — Cell Structure and Function",
                "Cell theory (Schleiden, Schwann, Virchow): all organisms made of cells; " +
                "cells arise from pre-existing cells. " +
                "Prokaryotic: no membrane-bound nucleus. Examples: bacteria, cyanobacteria. " +
                "Has nucleoid, 70S ribosomes, cell wall (peptidoglycan). " +
                "Eukaryotic: membrane-bound nucleus and membrane organelles. 80S ribosomes. " +
                "Nucleus: double membrane with nuclear pores. Contains DNA as chromatin. " +
                "Mitochondria: double membrane, site of aerobic respiration, has own DNA (maternal inheritance). " +
                "Inner membrane: cristae (increases surface area). Matrix: Krebs cycle occurs here. " +
                "Chloroplast: double membrane, site of photosynthesis, has own DNA. " +
                "Thylakoids (grana): light reactions. Stroma: dark reactions (Calvin cycle). " +
                "Ribosomes: site of protein synthesis. 80S (eukaryotes) = 60S + 40S. " +
                "Golgi apparatus (Golgi body): protein modification, sorting, secretion (cis → trans). " +
                "ER: rough ER (ribosomes, protein synthesis), smooth ER (lipid synthesis). " +
                "Cell membrane: fluid mosaic model (Singer-Nicolson 1972). Phospholipid bilayer."),

            new SeedEntry("neet", "biology", "NCERT Biology — Photosynthesis",
                "Equation: 6CO₂ + 6H₂O + light → C₆H₁₂O₆ + 6O₂. " +
                "Location: chloroplasts. Two stages: light reactions (thylakoids) and Calvin cycle (stroma). " +
                "Light reactions: absorb light via chlorophyll, produce ATP + NADPH, release O₂. " +
                "Photosystem II (P680): absorbs at 680nm, oxidizes water → O₂ + 4H⁺ + 4e⁻. " +
                "Photosystem I (P700): absorbs at 700nm, reduces NADP⁺ → NADPH. " +
                "Z-scheme (non-cyclic photophosphorylation): PSII → plastoquinone → cytochrome b6f → PSI → ferredoxin → NADP⁺. " +
                "Photolysis: 2H₂O → 4H⁺ + 4e⁻ + O₂ (at PSII, provides electrons). " +
                "Calvin cycle (dark reactions / C3 pathway): CO₂ + RuBP → 2×3-PGA (RuBisCO enzyme). " +
                "3-PGA reduced to G3P using ATP + NADPH. Some G3P → glucose, some → RuBP regeneration. " +
                "C4 plants (sugarcane, maize, sorghum): CO₂ fixed first as OAA in mesophyll (by PEP carboxylase). " +
                "OAA → malate → bundle sheath → releases CO₂ → enters Calvin cycle. " +
                "Advantage: avoids photorespiration, efficient in hot dry conditions. " +
                "CAM plants: keep stomata closed during day (for water conservation).")
        );
    }
}
