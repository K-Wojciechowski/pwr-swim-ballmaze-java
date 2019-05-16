package pl.krzysztofwojciechowski.ballmaze.java;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private LightSensorListener lightSensorListener;
    private Sensor accelerometer;
    private AccelerometerListener accelerometerListener;
    private GameView gameView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameView = new GameView(this);
        setContentView(gameView);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        assert sensorManager != null;

        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        lightSensorListener = new LightSensorListener();

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometerListener = new AccelerometerListener();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.w("X", "onResume");
        sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w("X", "onPause");
        sensorManager.unregisterListener(lightSensorListener);
        sensorManager.unregisterListener(accelerometerListener);
    }

    class LightSensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            gameView.handleLightSensor(event.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // We can't do much about this.
        }
    }

    class AccelerometerListener implements SensorEventListener {
        float lastVal = 0;

        @Override
        public void onSensorChanged(SensorEvent event) {
            // https://gamedev.stackexchange.com/a/73294
            lastVal = lastVal + Constants.ACCEL_ALPHA * (event.values[0] - lastVal);
            gameView.handleAccelerometer(-lastVal * Constants.ACCEL_SENSITIVITY);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Cannot handle meaningfully.
        }
    }
}
