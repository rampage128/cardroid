package de.jlab.cardroid.utils.ui;

import android.text.Html;
import android.text.Spanned;

public final class UnitValues {

    public static Spanned getFancyDecimalValue(float value) {
        String[] parts = Float.toString(value).split("\\.");
        String decimals = parts.length > 1 ? "<small>." + parts[1] + "</small>" : "";
        return Html.fromHtml(parts[0] + decimals);
    }

    public static int constrainToRange(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public static float constrainToRange(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

}
