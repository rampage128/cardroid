package de.jlab.cardroid.utils.ui;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import de.jlab.cardroid.R;

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

    public static String getStatisticString(int count, int average, @StringRes int unit, @NonNull Context context) {
        return context.getString(
            R.string.status_statistics_value,
            count,
            context.getString(unit),
            average
        );
    }

}
