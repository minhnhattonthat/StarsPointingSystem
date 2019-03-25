package com.celestial.gps.Sliders;

import android.content.Context;

import com.celestial.gps.CameraManager.Camera;
import com.celestial.gps.Converters.ExposureTimeConverter;

/**
 * Gemaakt door ruurd op 11-3-2017.
 */

public class ExposureSlider extends CameraStringSlider {
    public ExposureSlider(Context context, Camera camera) {
        super(context, camera, ExposureTimeConverter.exposureTimeFractions);
    }

    public void applyToCamera(String value) {
        float time = stringToValue(value);
        camera.state.setExposureTime(time);
    }
}
