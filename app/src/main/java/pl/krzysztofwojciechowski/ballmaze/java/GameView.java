package pl.krzysztofwojciechowski.ballmaze.java;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import androidx.core.content.ContextCompat;

import java.util.concurrent.ThreadLocalRandom;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private Canvas canvas;
    private Paint paint;
    private float lightValue = 0f;
    private GameMode mode = GameMode.START;
    private GameThread thread;
    private int UNIT;

    private int screenWidth = 0;
    private int screenHeight = 0;
    private int maxVisibleFloors = 0;

    private int[] gaps = new int[Constants.FLOORS];
    private int[] colors = new int[Constants.FLOORS];
    private int[] heights = new int[Constants.FLOORS];

    private float ballShift = 0f;

    private int ballSize_px = 0;
    private int ballX_px = 0;
    private int ballY_px = 0;
    private int topY_px = 0;
    private int ballCentered_px = 0;
    private int ballDistance_px = 0;
    private int scrollDist_px = 0;
    private int minLeftBallPosition_px = 0;
    private int maxRightBallPosition_px = 0;
    private int floorRectHeight_px = 0;
    private int gapWidth_px = 0;

    private int score = 0;

    GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        canvas = new Canvas();
        paint = new Paint();
        UNIT = (int)getDim(R.dimen._unit_);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode != GameMode.INGAME) startGame();
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        draw();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // No meaningful support.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopGame();
    }

    private void draw() {
        if (!getHolder().getSurface().isValid()) return;
        canvas = getHolder().lockCanvas();
        if (canvas == null) return;
        if (lightValue < Constants.DARKMODE_THRESHOLD) {
            canvas.drawColor(getColor(R.color.background_dark));
            paint.setColor(getColor(R.color.foreground_dark));
        } else {
            canvas.drawColor(getColor(R.color.background_light));
            paint.setColor(getColor(R.color.foreground_light));
        }
        switch (mode) {
            case START:
                drawStart(canvas);
                break;
            case INGAME:
                drawInGame(canvas);
                break;
            case LOSE:
                drawLose(canvas);
                break;
            case WIN:
                drawWin(canvas);
                break;
            default:
                mode = GameMode.START;
                drawStart(canvas);
        }
        getHolder().unlockCanvasAndPost(canvas);
    }

    private void drawStart(Canvas canvas) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        ballCentered_px = screenWidth / 2;
        ballSize_px = (int)getDim(R.dimen.ball_size);
        ballDistance_px = ballCentered_px - (int)(getDim(R.dimen.gap_margin_size) + ballSize_px);
        scrollDist_px = (int)getDim(R.dimen.scroll_distance);
        minLeftBallPosition_px = (int)(getDim(R.dimen.ball_margin_size) + ballSize_px);
        maxRightBallPosition_px = screenWidth - minLeftBallPosition_px;
        floorRectHeight_px = (int)getDim(R.dimen.floor_rect_height);
        gapWidth_px = (int)getDim(R.dimen.gap_width);

        // Game title
        paint.setTextSize(getDim(R.dimen.title_size));
        drawCenter(canvas, paint, getString(R.string.app_name), -1,
                getDim(R.dimen.title_position));

        // Objective: title
        paint.setTextSize(getDim(R.dimen.objective_title_size));
        drawCenter(canvas, paint, getString(R.string.objective_title), -1,
                getDim(R.dimen.objective_title_position));

        // Objective text content
        String objective = getString(R.string.objective);
        TextPaint tp = new TextPaint();
        tp.setTextSize(getDim(R.dimen.objective_text_size));
        tp.setColor(paint.getColor());
        StaticLayout sl = StaticLayout.Builder.obtain(
                objective, 0, objective.length(), tp,
                (int)(getWidth() - getDim(R.dimen.text_margin))
        ).build();

        float objPos = getDim(R.dimen.objective_position);
        canvas.translate(0, objPos);
        sl.draw(canvas);
        paint.setTextSize(getDim(R.dimen.big_text_size));
        canvas.translate(0, -objPos);

        // START prompt
        Typeface oldtf = paint.getTypeface();
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        drawCenter(canvas, paint, getString(R.string.tap_to_start), -1, -1);
        paint.setTypeface(oldtf);
    }

    private float getDim(int resId) {
        return getResources().getDimension(resId);
    }

    private String getString(int resId) {
        return getResources().getString(resId);
    }

    private void drawInGame(Canvas canvas) {
        float displayY = getResources().getDimension(R.dimen.ball_from_top);

        int foreColor = paint.getColor();
        canvas.drawCircle(ballX_px, displayY, getResources().getDimension(R.dimen.ball_size), paint);
        if (score > 0) {
            int scHeight = heights[score - 1];
            if (scHeight > (ballY_px - ballSize_px - floorRectHeight_px)) drawAt(score - 1, canvas);
        }
        int maxShown = Math.min(score + maxVisibleFloors, Constants.FLOORS - 1);
        for (int i = score; i <= maxShown; i++) {
            drawAt(i, canvas);
        }

        // Draw score with shadow
        paint.setColor(getColor(R.color.textshadow));
        canvas.drawText(
                Integer.toString(score),
                getDim(R.dimen.score_margin) + UNIT,
                (int)(screenHeight - getDim(R.dimen.score_margin)) + UNIT,
                paint);

        paint.setColor(foreColor);
        canvas.drawText(
                Integer.toString(score),
                getDim(R.dimen.score_margin),
                (int)(screenHeight - getDim(R.dimen.score_margin)),
                paint);
    }

    private void drawAt(int height, Canvas canvas) {
        int top = heights[height] - topY_px;
        paint.setColor(colors[height]);
        canvas.drawRect(0, top, gaps[height], top + floorRectHeight_px, paint);
        canvas.drawRect(gaps[height] + gapWidth_px, top, screenWidth, top + floorRectHeight_px, paint);
    }

    private void drawWin(Canvas canvas) {
        if (lightValue < Constants.DARKMODE_THRESHOLD) {
            canvas.drawColor(getColor(R.color.win_dark));
        } else {
            canvas.drawColor(getColor(R.color.win_light));
        }

        drawGameOver(canvas, getString(R.string.you_won));
    }

    private void drawLose(Canvas canvas) {
        if (lightValue < Constants.DARKMODE_THRESHOLD) {
            canvas.drawColor(getColor(R.color.lose_dark));
        } else {
            canvas.drawColor(getColor(R.color.lose_light));
        }

        drawGameOver(canvas, getString(R.string.you_lost));
    }

    private void drawGameOver(Canvas canvas, String wlText) {
        // Game title
        paint.setTextSize(getDim(R.dimen.title_size));
        drawCenter(canvas, paint, getString(R.string.app_name), -1,
                getDim(R.dimen.title_position));

        // YOU WON/LOST
        paint.setTextSize(getDim(R.dimen.big_text_size));
        drawCenter(canvas, paint, wlText, -1,
                getDim(R.dimen.gameover_title_position));

        // Score
        drawCenter(canvas, paint, getResources().getString(R.string.score, score), -1,
                getDim(R.dimen.gameover_score_position));

        // START prompt
        Typeface oldtf = paint.getTypeface();
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        drawCenter(canvas, paint, getString(R.string.play_again), -1, -1);
        paint.setTypeface(oldtf);
    }

    private void startGame() {
        mode = GameMode.INGAME;
        int range = (int)(screenWidth - (2 * getDim(R.dimen.gap_margin_size)) - gapWidth_px + 1);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        int[] colorArray = getResources().getIntArray(R.array.floor_colors);
        heights[0] = (int)getDim(R.dimen.first_floor_height);
        for (int i = 0; i < Constants.FLOORS; i++) {
            gaps[i] = (int)getDim(R.dimen.gap_margin_size) + rand.nextInt(range);
            colors[i] = colorArray[i % colorArray.length];
            if (i != 0) heights[i] = heights[i - 1] + (int)getDim(R.dimen.distance_between_floors);
        }

        maxVisibleFloors = (int)Math.ceil(screenHeight / getDim(R.dimen.distance_between_floors));

        ballX_px = ballCentered_px;
        ballY_px = 0;
        topY_px = -ballSize_px;
        score = 0;
        thread = new GameThread();
        thread.start();
    }

    public void stopGame() {
        mode = GameMode.START;
        try {
            if (thread != null) thread.join();
        } catch (InterruptedException ignored) {
            // That should not fail.
        }
    }

    // https://stackoverflow.com/a/32081250
    private void drawCenter(Canvas canvas, Paint paint, String text, float x, float y) {
        Rect r = canvas.getClipBounds();
        int cHeight = r.height();
        int cWidth = r.width();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), r);

        if (x == -1) x = cWidth / 2f - r.width() / 2f - r.left;
        if (y == -1) y = cHeight / 2f + r.height() / 2f - r.bottom;
        canvas.drawText(text, x, y, paint);
    }

    private int getColor(int colorId) {
        return ContextCompat.getColor(getContext(), colorId);
    }

    public void handleLightSensor(float value) {
        lightValue = value;
        if (mode != GameMode.INGAME) draw();
        // otherwise, the GameThread handles this
    }

    public void handleAccelerometer(float x) {
        ballShift = x;
    }

    class GameThread extends Thread {
        @Override
        public void run() {
            while (mode == GameMode.INGAME) {
                try {
                    runGameTick();
                    draw();
                    sleep(16);
                } catch (InterruptedException ignored) { }
            }
        }

        private void runGameTick() {
            ballX_px += (ballShift * ballDistance_px);
            if (ballX_px < minLeftBallPosition_px) { ballX_px = minLeftBallPosition_px; }
            if (ballX_px > maxRightBallPosition_px) { ballX_px = maxRightBallPosition_px; }

            ballY_px += scrollDist_px;
            topY_px += scrollDist_px;
            // TODO collision detection
            if (ballY_px - (2 * ballSize_px) > heights[score]) {
                score++;
            }
            if (score == Constants.FLOORS) {
                mode = GameMode.WIN;
            }
        }
    }
}
