/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.ux.beta.cameracore.widget.cameracontrols.exposuresettingsindicator;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.ExposureMode;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.FrameLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.OnStateChangeCallback;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;

/**
 * Widget indicates the current exposure mode.
 * Tapping on the widget can be linked to open exposure settings
 */
public class ExposureSettingsIndicatorWidget extends FrameLayoutWidget implements View.OnClickListener {

    //region Fields
    private static final String TAG = "ExposureSetIndicWidget";
    private ImageView foregroundImageView;
    private ExposureSettingsIndicatorWidgetModel widgetModel;
    private Map<ExposureMode, Drawable> exposureModeDrawableHashMap;
    private OnStateChangeCallback<Object> stateChangeCallback = null;
    private int stateChangeResourceId;
    //endregion

    //region Lifecycle
    public ExposureSettingsIndicatorWidget(@NonNull Context context) {
        super(context);
    }

    public ExposureSettingsIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExposureSettingsIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_exposure_settings_indicator, this);
        foregroundImageView = findViewById(R.id.image_view_exposure_settings_indicator);
        setOnClickListener(this);

        if (!isInEditMode()) {
            initDefaults();
            widgetModel = new ExposureSettingsIndicatorWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
        }

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getExposureMode()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateUI));
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    @Override
    public void onClick(View v) {
        if (stateChangeCallback != null) {
            stateChangeCallback.onStateChange(null);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
        initializeListener();
    }

    @Override
    protected void onDetachedFromWindow() {
        destroyListener();
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }
    //endregion

    //region private methods
    private void checkAndUpdateUI() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.getExposureMode()
                    .firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(this::updateUI, logErrorConsumer(TAG, "get exposure mode")));
        }
    }

    private void initializeListener() {
        if (stateChangeResourceId != INVALID_RESOURCE && this.getRootView() != null) {
            View widgetView = this.getRootView().findViewById(stateChangeResourceId);
            // TODO when panel implemented
        }
    }

    private void destroyListener() {
        stateChangeCallback = null;
    }

    private void initDefaults() {
        exposureModeDrawableHashMap = new HashMap<>();

        exposureModeDrawableHashMap.put(ExposureMode.APERTURE_PRIORITY, getResources().getDrawable(R.drawable.uxsdk_ic_exposure_settings_aperture));
        exposureModeDrawableHashMap.put(ExposureMode.SHUTTER_PRIORITY, getResources().getDrawable(R.drawable.uxsdk_ic_exposure_settings_shutter));
        exposureModeDrawableHashMap.put(ExposureMode.MANUAL, getResources().getDrawable(R.drawable.uxsdk_ic_exposure_settings_manual));
        exposureModeDrawableHashMap.put(ExposureMode.PROGRAM, getResources().getDrawable(R.drawable.uxsdk_ic_exposure_settings_program));
        exposureModeDrawableHashMap.put(ExposureMode.UNKNOWN, getResources().getDrawable(R.drawable.uxsdk_ic_exposure_settings_normal));
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ExposureSettingsIndicatorWidget);
        setCameraIndex(CameraIndex.find(typedArray.getInt(R.styleable.ExposureSettingsIndicatorWidget_uxsdk_cameraIndex, 0)));
        if (!isInEditMode()){
            setLensType(SettingsDefinitions.LensType.find(typedArray.getInt(R.styleable.ExposureSettingsIndicatorWidget_uxsdk_lensType, 0)));
        }
        stateChangeResourceId =
                typedArray.getResourceId(R.styleable.ExposureSettingsIndicatorWidget_uxsdk_onStateChange, INVALID_RESOURCE);
        Drawable drawable = typedArray.getDrawable(R.styleable.ExposureSettingsIndicatorWidget_uxsdk_aperturePriorityModeDrawable);
        if (drawable != null) {
            setIconByMode(ExposureMode.APERTURE_PRIORITY, drawable);
        }
        drawable = typedArray.getDrawable(R.styleable.ExposureSettingsIndicatorWidget_uxsdk_shutterPriorityModeDrawable);
        if (drawable != null) {
            setIconByMode(ExposureMode.SHUTTER_PRIORITY, drawable);
        }
        drawable = typedArray.getDrawable(R.styleable.ExposureSettingsIndicatorWidget_uxsdk_programModeDrawable);
        if (drawable != null) {
            setIconByMode(ExposureMode.PROGRAM, drawable);
        }
        drawable = typedArray.getDrawable(R.styleable.ExposureSettingsIndicatorWidget_uxsdk_manualModeDrawable);
        if (drawable != null) {
            setIconByMode(ExposureMode.MANUAL, drawable);
        }
        drawable = typedArray.getDrawable(R.styleable.ExposureSettingsIndicatorWidget_uxsdk_unknownModeDrawable);
        if (drawable != null) {
            setIconByMode(ExposureMode.UNKNOWN, drawable);
        }
        setIconBackground(typedArray.getDrawable(R.styleable.ExposureSettingsIndicatorWidget_uxsdk_iconBackground));
        typedArray.recycle();
    }

    private void updateUI(ExposureMode exposureMode) {
        switch (exposureMode) {
            case APERTURE_PRIORITY:
                foregroundImageView.setImageDrawable(exposureModeDrawableHashMap.get(ExposureMode.APERTURE_PRIORITY));
                break;
            case SHUTTER_PRIORITY:
                foregroundImageView.setImageDrawable(exposureModeDrawableHashMap.get(ExposureMode.SHUTTER_PRIORITY));
                break;
            case MANUAL:
                foregroundImageView.setImageDrawable(exposureModeDrawableHashMap.get(ExposureMode.MANUAL));
                break;
            case PROGRAM:
                foregroundImageView.setImageDrawable(exposureModeDrawableHashMap.get(ExposureMode.PROGRAM));
                break;
            case UNKNOWN:
            default:
                foregroundImageView.setImageDrawable(exposureModeDrawableHashMap.get(ExposureMode.UNKNOWN));
        }
    }
    //endregion

    //region customizations

    /**
     * Get the index of the camera to which the widget is reacting
     *
     * @return instance of {@link CameraIndex}
     */
    @NonNull
    public CameraIndex getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    /**
     * Set the index of camera to which the widget should react
     *
     * @param cameraIndex index of the camera.
     */
    public void setCameraIndex(@NonNull CameraIndex cameraIndex) {
        if (!isInEditMode()) {
            widgetModel.setCameraIndex(cameraIndex);
        }
    }

    /**
     * Get the current type of the lens the widget is reacting to
     *
     * @return current lens type
     */
    @NonNull
    public SettingsDefinitions.LensType getLensType() {
        return widgetModel.getLensType();
    }

    /**
     * Set the type of the lens for which the widget should react
     *
     * @param lensType lens type
     */
    public void setLensType(@NonNull SettingsDefinitions.LensType lensType) {
        if (!isInEditMode()) {
            widgetModel.setLensType(lensType);
        }
    }

    /**
     * Set the icon for exposure mode
     *
     * @param exposureMode instance of {@link ExposureMode} for which icon should be used
     * @param resourceId   to be used
     */
    public void setIconByMode(@NonNull ExposureMode exposureMode, @DrawableRes int resourceId) {
        setIconByMode(exposureMode, getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for exposure mode
     *
     * @param exposureMode instance of {@link ExposureMode} for which icon should be used
     * @param drawable     to be used
     */
    public void setIconByMode(@NonNull ExposureMode exposureMode, @Nullable Drawable drawable) {
        exposureModeDrawableHashMap.put(exposureMode, drawable);
        checkAndUpdateUI();
    }

    /**
     * Get the icon used for the exposure mode
     *
     * @param exposureMode instance of {@link ExposureMode} for which icon is used
     * @return Drawable
     */
    @Nullable
    public Drawable getIconByMode(@NonNull ExposureMode exposureMode) {
        return exposureModeDrawableHashMap.get(exposureMode);
    }

    /**
     * Get current background of icon
     *
     * @return Drawable resource of the background
     */
    public Drawable getIconBackground() {
        return foregroundImageView.getBackground();
    }

    /**
     * Set background to icon
     *
     * @param resourceId resource id of background
     */
    public void setIconBackground(@DrawableRes int resourceId) {
        setIconBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to icon
     *
     * @param drawable Drawable to be used as background
     */
    public void setIconBackground(@Nullable Drawable drawable) {
        foregroundImageView.setBackground(drawable);
    }

    /**
     * Set callback for when the widget is tapped.
     * This can be used to link the widget to //TODO exposure settings panel
     *
     * @param stateChangeCallback listener to handle callback
     */
    public void setStateChangeCallback(@NonNull OnStateChangeCallback<Object> stateChangeCallback) {
        this.stateChangeCallback = stateChangeCallback;
    }
    //endregion
}
