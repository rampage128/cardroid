package de.jlab.cardroid.overlay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import de.jlab.cardroid.R;

public abstract class Overlay {

    private Context context;
    private WindowManager windowManager;

    private WindowManager.LayoutParams layoutParams;
    private View contentView;
    private boolean isVisible = false;

    private Handler uiHandler;

    public Overlay(@NonNull Context context) {
        this.context = context;
        this.layoutParams = this.createLayoutParams();
    }

    public void create() {
        if (this.contentView != null) {
            this.destroy();
        }

        this.uiHandler = new Handler(this.context.getMainLooper());
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        this.onCreate(this.layoutParams, this.context);
        if (this.contentView == null) {
            throw new IllegalStateException("onCreate() did not call setContentView(View) in Overlay " + this.getClass().getSimpleName());
        }
        this.updateLayoutFromContentView();
    }

    public void show() {
        this.runOnUiThread(() -> {
            if (this.isVisible) {
                return;
            }

            if (this.contentView == null) {
                throw new IllegalStateException("Can not show overlay " + this.getClass().getSimpleName() + " because it has no content. Did you call create(Context)?");
            }

            this.windowManager.addView(this.contentView, this.layoutParams);
            this.onShow();

            this.isVisible = true;
        });
    }

    public void hide() {
        //this.runOnUiThread(() -> {
            if (!this.isVisible) {
                return;
            }

            this.windowManager.removeView(this.contentView);
            this.onHide();

            this.isVisible = false;
        //});
    }

    public void fadeIn(long animationDuration) {
        this.show();
        this.runOnUiThread(() -> {
            this.contentView.setVisibility(View.VISIBLE);
            this.contentView.setAlpha(0f);
            this.contentView.animate()
                    .alpha(1f)
                    .setDuration(animationDuration)
                    .setListener(null);
        });
    }

    public void fadeOut(long animationDuration) {
        this.runOnUiThread(() -> {
            if (!this.isVisible) {
                return;
            }

            this.contentView.animate()
                    .alpha(0f)
                    .setDuration(animationDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            Overlay.this.contentView.setVisibility(View.GONE);
                            Overlay.this.hide();
                        }
                    });
        });
    }

    public void destroy() {
        if (this.contentView != null) {
            this.hide();
            this.onDestroy();

            this.uiHandler.removeCallbacksAndMessages(null);
            this.uiHandler = null;
            this.windowManager = null;
            this.contentView = null;
        }
    }

    protected Resources getResources() {
        return this.context.getResources();
    }

    protected String getString(@StringRes int resId) {
        return this.context.getString(resId);
    }

    protected void cancelOnUiThread(@NonNull Runnable runnable) {
        if (this.uiHandler != null) {
            this.uiHandler.removeCallbacks(runnable);
        }
    }

    protected void runOnUiThread(@NonNull Runnable runnable) {
        if (this.uiHandler != null) {
            this.uiHandler.post(runnable);
        }
    }

    protected void runOnUiThread(@NonNull Runnable runnable, long delay) {
        if (this.uiHandler != null) {
            this.uiHandler.postDelayed(runnable, delay);
        }
    }

    @Nullable
    protected <T extends View> T findViewById(@IdRes int id) {
        return this.contentView.findViewById(id);
    }

    protected View setContentView(@LayoutRes int layoutId) {
        this.context.setTheme(R.style.AppTheme);
        this.contentView = LayoutInflater.from(this.context).inflate(layoutId, null);
        return this.contentView;
    }

    protected abstract void onCreate(@NonNull WindowManager.LayoutParams windowParams, @NonNull Context context);
    protected abstract void onShow();
    protected abstract void onHide();
    protected abstract void onDestroy();

    public void move(int x, int y) {
        this.layoutParams.x = x;
        this.layoutParams.y = y;
        this.layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        this.windowManager.updateViewLayout(this.contentView, this.layoutParams);
    }

    public void scale(int width, int height) {
        this.layoutParams.width = width;
        this.layoutParams.height = height;
        this.windowManager.updateViewLayout(this.contentView, this.layoutParams);
    }

    public void anchor(int gravity) {
        this.layoutParams.gravity = gravity;
        this.windowManager.updateViewLayout(this.contentView, this.layoutParams);
    }

    private void updateLayoutFromContentView() {
        int width = this.layoutParams.width;
        int height = this.layoutParams.height;

        // Default to full screen layout if width or height are 0 (to prevent invisible overlays)
        if (width == 0 || height == 0) {
            width = WindowManager.LayoutParams.MATCH_PARENT;
            height = WindowManager.LayoutParams.MATCH_PARENT;
        }

        // If height is full screen, make sure we render behind the navigation bar as well
        if (height == WindowManager.LayoutParams.MATCH_PARENT) {
            this.layoutParams.gravity = Gravity.TOP | Gravity.LEFT;

            /* FIXME: Full size overlays do not draw behind navigation bar ...
                the code should work, but doesn't ...

            Point fullSize = new Point();
            Point usableSize = new Point();

            this.windowManager.getDefaultDisplay().getRealSize(fullSize);
            this.windowManager.getDefaultDisplay().getSize(usableSize);

            Log.e(this.getClass().getSimpleName(), "" + (usableSize.y - fullSize.y) + ", " + fullSize.y);

            this.layoutParams.y = usableSize.y - fullSize.y;
            this.layoutParams.height = fullSize.y;
            this.layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
             */
        }

        // Apply final width and height
        this.layoutParams.width = width;
        this.layoutParams.height = height;
    }

    private WindowManager.LayoutParams createLayoutParams() {
        // Create the right window type depending on the given android version
        int windowType = WindowManager.LayoutParams.TYPE_PHONE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            windowType = WindowManager.LayoutParams.TYPE_TOAST;
        }
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            windowType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        // Init default window behaviour (full-screen, allow overlap decorations,
        WindowManager.LayoutParams params =  new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        /*
        // DISABLE WINDOW ANIMATIONS
        try {
            Class layoutParamsClass = params.getClass();
            Field privateFlags = layoutParamsClass.getField("privateFlags");
            Field noAnim = layoutParamsClass.getField("PRIVATE_FLAG_NO_MOVE_ANIMATION");

            int privateFlagsValue = privateFlags.getInt(params);
            int noAnimFlag = noAnim.getInt(params);
            privateFlagsValue |= noAnimFlag;

            privateFlags.setInt(params, privateFlagsValue);
        } catch (Exception e) {
            //do nothing. Probably using other version of android
        }
        // END DISABLE WINDOW ANIMATIONS
         */

        return params;
    }

}
