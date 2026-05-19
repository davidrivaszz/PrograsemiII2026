package com.example.proyectof;

import java.util.List;

/**
 * Clasifica señas del Lenguaje de Señas de El Salvador (LSEN)
 * usando los 21 puntos landmarks de MediaPipe Hands.
 *
 * Índices de landmarks:
 *   0=WRIST
 *   1=THUMB_CMC, 2=THUMB_MCP, 3=THUMB_IP,  4=THUMB_TIP
 *   5=INDEX_MCP, 6=INDEX_PIP, 7=INDEX_DIP,  8=INDEX_TIP
 *   9=MIDDLE_MCP,10=MIDDLE_PIP,11=MIDDLE_DIP,12=MIDDLE_TIP
 *  13=RING_MCP, 14=RING_PIP, 15=RING_DIP,  16=RING_TIP
 *  17=PINKY_MCP,18=PINKY_PIP,19=PINKY_DIP, 20=PINKY_TIP
 */
public class SignClassifier {

    private static final float TOUCH_THRESHOLD = 0.07f;

    public static class Point {
        public float x, y;
        public Point(float x, float y) { this.x = x; this.y = y; }
    }

    public static String classify(List<Point> lm) {
        if (lm == null || lm.size() < 21) return null;

        // ── Puntos clave ──────────────────────────────────────────────────
        Point wrist     = lm.get(0);
        Point thumbMcp  = lm.get(2);
        Point thumbIp   = lm.get(3);
        Point thumbTip  = lm.get(4);
        Point indexMcp  = lm.get(5);
        Point indexPip  = lm.get(6);
        Point indexTip  = lm.get(8);
        Point middleMcp = lm.get(9);
        Point middlePip = lm.get(10);
        Point middleTip = lm.get(12);
        Point ringMcp   = lm.get(13);
        Point ringPip   = lm.get(14);
        Point ringTip   = lm.get(16);
        Point pinkyMcp  = lm.get(17);
        Point pinkyPip  = lm.get(18);
        Point pinkyTip  = lm.get(20);

        // ── Dedos extendidos ──────────────────────────────────────────────
        // Un dedo está extendido si su punta está claramente por encima de su PIP
        boolean indexUp  = indexTip.y  < indexPip.y  - 0.02f;
        boolean middleUp = middleTip.y < middlePip.y - 0.02f;
        boolean ringUp   = ringTip.y   < ringPip.y   - 0.02f;
        boolean pinkyUp  = pinkyTip.y  < pinkyPip.y  - 0.02f;
        boolean thumbUp  = thumbTip.y  < thumbIp.y   - 0.02f;

        // Pulgar extendido lateralmente (diferencia horizontal)
        boolean thumbOut = Math.abs(thumbTip.x - thumbMcp.x) > 0.08f;

        // Dedos totalmente doblados (punta por debajo del MCP)
        boolean indexFolded  = indexTip.y  > indexMcp.y;
        boolean middleFolded = middleTip.y > middleMcp.y;
        boolean ringFolded   = ringTip.y   > ringMcp.y;
        boolean pinkyFolded  = pinkyTip.y  > pinkyMcp.y;

        // ── Distancias entre puntas ───────────────────────────────────────
        float dThumbIndex  = dist(thumbTip, indexTip);
        float dThumbMiddle = dist(thumbTip, middleTip);
        float dIndexMiddle = dist(indexTip, middleTip);
        float dMiddleRing  = dist(middleTip, ringTip);
        float dRingPinky   = dist(ringTip, pinkyTip);

        // ════════════════════════════════════════════════════════════════════
        // REGLAS — orden de MÁS ESPECÍFICO a MÁS GENERAL
        // Cada seña tiene condiciones únicas que la diferencian de las demás.
        // ════════════════════════════════════════════════════════════════════

        // ── "Hola" — TODOS los dedos extendidos incluyendo pulgar ──────────
        // Diferencia con B y Casa: thumbUp también es true
        if (thumbUp && indexUp && middleUp && ringUp && pinkyUp) {
            return "Hola";
        }

        // ── "No" — puño COMPLETAMENTE cerrado, pulgar encima ───────────────
        // Diferencia con A: en A el pulgar está al lado (thumbOut), aquí no
        if (!thumbUp && !thumbOut && indexFolded && middleFolded && ringFolded && pinkyFolded) {
            return "No";
        }

        // ── "A" — puño cerrado con pulgar extendido al LADO ────────────────
        // Diferencia con No: thumbOut es true
        if (!indexUp && !middleUp && !ringUp && !pinkyUp && thumbOut && !thumbUp) {
            return "A";
        }

        // ── "Gracias" — solo PULGAR hacia ARRIBA, resto doblados ───────────
        // Diferencia con A: thumbUp (arriba), no lateral
        if (thumbUp && !thumbOut && indexFolded && middleFolded && ringFolded && pinkyFolded) {
            return "Gracias";
        }

        // ── "I" — solo MEÑIQUE arriba, resto doblados ──────────────────────
        // Debe ir antes de Y para evitar confusión
        if (!thumbUp && !thumbOut && !indexUp && !middleUp && !ringUp && pinkyUp
                && indexFolded && middleFolded && ringFolded) {
            return "I";
        }

        // ── "Y" — PULGAR LATERAL + MEÑIQUE arriba ──────────────────────────
        // Diferencia con I: thumbOut es true
        // Diferencia con Ayuda: misma forma, pero Ayuda se usa en contexto diferente
        if (thumbOut && !indexUp && !middleUp && !ringUp && pinkyUp
                && indexFolded && middleFolded && ringFolded) {
            return "Y";
        }

        // ── "Ayuda" — igual forma que Y pero se prioriza Y en alfabeto ─────
        // Para distinguir: Ayuda requiere que el meñique apunte más horizontal
        // Por ahora se mapea a Y si quieres el alfabeto, o ajusta aquí
        // if (thumbOut && !indexUp && !middleUp && !ringUp && pinkyUp) return "Ayuda";

        // ── "L" — ÍNDICE arriba + PULGAR LATERAL, forma de L ───────────────
        // CRÍTICO: thumbOut debe ser true. Diferencia con Sí: en Sí thumbOut es false
        if (thumbOut && indexUp && !middleUp && !ringUp && !pinkyUp
                && middleFolded && ringFolded && pinkyFolded) {
            return "L";
        }

        // ── "Sí" — solo ÍNDICE arriba, pulgar NO lateral ───────────────────
        // Diferencia con L: thumbOut es false
        if (!thumbOut && !thumbUp && indexUp && !middleUp && !ringUp && !pinkyUp
                && middleFolded && ringFolded && pinkyFolded) {
            return "Sí";
        }

        // ── "D" — índice arriba, pulgar TOCA medio, otros doblados ─────────
        // Diferencia con Sí: dThumbMiddle < TOUCH_THRESHOLD
        if (indexUp && !middleUp && !ringUp && !pinkyUp
                && dThumbMiddle < TOUCH_THRESHOLD
                && middleFolded && ringFolded && pinkyFolded) {
            return "D";
        }

        // ── "Suerte" — índice y medio JUNTOS (cruzados) ─────────────────────
        // Diferencia con Bien y V: dIndexMiddle MUY pequeño (dedos pegados/cruzados)
        if (!thumbUp && indexUp && middleUp && !ringUp && !pinkyUp
                && dIndexMiddle < 0.03f
                && ringFolded && pinkyFolded) {
            return "Suerte";
        }

        // ── "V" — índice y medio en V, MUY SEPARADOS ────────────────────────
        // Diferencia con Bien: dIndexMiddle > 0.07f (más separados que Bien)
        // Diferencia con Suerte: dIndexMiddle grande
        if (!thumbUp && indexUp && middleUp && !ringUp && !pinkyUp
                && dIndexMiddle > 0.07f
                && ringFolded && pinkyFolded) {
            return "V";
        }

        // ── "Bien" — índice y medio arriba, SEPARACIÓN MEDIA ────────────────
        // Diferencia con V: dIndexMiddle entre 0.03f y 0.07f
        // Diferencia con Suerte: no están cruzados
        if (!thumbUp && indexUp && middleUp && !ringUp && !pinkyUp
                && dIndexMiddle >= 0.03f && dIndexMiddle <= 0.07f
                && ringFolded && pinkyFolded) {
            return "Bien";
        }

        // ── "Amor" — PULGAR + ÍNDICE + MEÑIQUE ───────────────────────────────
        // Diferencia con Y: indexUp también es true
        if (thumbUp && indexUp && !middleUp && !ringUp && pinkyUp
                && middleFolded && ringFolded) {
            return "Amor";
        }

        // ── "Perfecto" (OK) — PULGAR e ÍNDICE forman círculo ─────────────────
        if (dThumbIndex < TOUCH_THRESHOLD && middleUp && ringUp && pinkyUp) {
            return "Perfecto";
        }

        // ── "Por favor" — PULGAR toca MEDIO, índice anular meñique arriba ────
        if (dThumbMiddle < TOUCH_THRESHOLD && indexUp && ringUp && pinkyUp && !middleUp) {
            return "Por favor";
        }

        // ── "Agua" — índice, medio y anular extendidos ────────────────────────
        if (!thumbUp && indexUp && middleUp && ringUp && !pinkyUp
                && pinkyFolded) {
            return "Agua";
        }

        // ── "B" — cuatro dedos juntos arriba, pulgar doblado ─────────────────
        // Diferencia con Casa: dedos muy juntos entre sí
        if (!thumbUp && !thumbOut && indexUp && middleUp && ringUp && pinkyUp
                && dIndexMiddle < 0.04f && dMiddleRing < 0.04f && dRingPinky < 0.04f) {
            return "B";
        }

        // ── "Casa" — cuatro dedos arriba con algo de separación ───────────────
        // Diferencia con B: algo más de separación entre dedos
        if (!thumbUp && indexUp && middleUp && ringUp && pinkyUp
                && dIndexMiddle < 0.06f && dMiddleRing < 0.06f) {
            return "Casa";
        }

        // ── "Familia" — cuatro dedos arriba muy separados ─────────────────────
        if (indexUp && middleUp && ringUp && pinkyUp && dMiddleRing > 0.06f) {
            return "Familia";
        }

        // ── "C" — mano curvada, ningún dedo extendido, pulgar forma arco ──────
        if (!indexUp && !middleUp && !ringUp && !pinkyUp
                && dThumbIndex > 0.06f && dThumbIndex < 0.14f
                && thumbTip.y < wrist.y) {
            return "C";
        }

        // ── "E" — todos curvados, puntas a nivel PIP ──────────────────────────
        if (!indexUp && !middleUp && !ringUp && !pinkyUp && !thumbUp && !thumbOut
                && indexTip.y > indexPip.y && middleTip.y > middlePip.y) {
            return "E";
        }

        // ── "O" — pulgar e índice forman O, resto doblados ───────────────────
        if (dThumbIndex < 0.05f && !middleUp && !ringUp && !pinkyUp
                && middleFolded && ringFolded && pinkyFolded) {
            return "O";
        }

        // ── "Buenos días" — igual que Hola pero no llegó porque Hola ya capturó
        // Se puede agregar un gesto de movimiento, aquí se deja como texto alternativo
        // return "Buenos días";

        return null; // No reconocido
    }

    private static float dist(Point a, Point b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}