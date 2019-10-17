package de.jlab.cardroid.utils.ui;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import de.jlab.cardroid.R;

public final class DialogUtils {

    private DialogUtils() {}

    @SuppressLint("InflateParams")
    public static void showEditTextDialog(@NonNull LayoutInflater inflater, @StringRes int title, @StringRes int positiveButtonText, @NonNull TextDialogListener listener) {
        View dialogLayout = inflater.inflate(R.layout.dialog_text, null);
        EditText input = dialogLayout.findViewById(R.id.text);
        listener.initialize(input);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                input.setError(listener.validate(s.toString()));
            }
        });

        new AlertDialog.Builder(inflater.getContext())
                .setTitle(title)
                .setView(dialogLayout)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    String value = input.getText().toString();
                    if (listener.validate(value) == null) {
                        listener.onSuccess(value);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    dialog.cancel();
                })
                .show();
    }

    public interface TextDialogListener {
        void initialize(EditText input);
        String validate(String text);
        void onSuccess(String text);
    }

}
