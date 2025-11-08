import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreatureWindow extends JWindow {

    private static final int TIMER_DELAY_MS = 16;
    private static final double WALK_SPEED_PX_PER_SEC = 220.0;
    private static final double IDLE_FRAME_SECONDS = 0.1;
    private static final int PAUSE_AT_TARGET_MS = 800;

    private final Point startPoint;
    private final Point targetPoint;
    private final Timer timer;
    private final JLabel creatureLabel;
    private final List<ImageIcon> idleFrames = new ArrayList<>();
    private int currentFrameIndex = 0;
    private double posX;
    private double posY;
    private boolean headingToTarget = true;
    private long lastTickNanos;
    private double animationAccumulatorSeconds = 0.0;

    public CreatureWindow(Point startPoint, Point targetPoint) {
        this.startPoint = startPoint;
        this.targetPoint = targetPoint;

        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));

        creatureLabel = new JLabel();
        loadIdleFrames();
        if (!idleFrames.isEmpty()) {
            creatureLabel.setIcon(idleFrames.get(currentFrameIndex));
        } else {
            ImageIcon fallbackIcon = new ImageIcon("creature.png");
            Image scaledImage = fallbackIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            creatureLabel.setIcon(new ImageIcon(scaledImage));
        }

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(creatureLabel, BorderLayout.CENTER);
        pack();

        posX = startPoint.x - getWidth() / 2.0;
        posY = startPoint.y - getHeight() / 2.0;
        setLocation((int) Math.round(posX), (int) Math.round(posY));

        timer = new Timer(TIMER_DELAY_MS, e -> onTick());
    }

    public void showWindow() {
        setVisible(true);
        lastTickNanos = System.nanoTime();
        timer.start();
    }

    @Override
    public void dispose() {
        timer.stop();
        super.dispose();
    }

    private void onTick() {
        long now = System.nanoTime();
        double deltaSeconds = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;

        if (deltaSeconds <= 0) {
            return;
        }

        stepTowardsGoal(deltaSeconds);
        advanceIdleFrame(deltaSeconds);
    }

    private void stepTowardsGoal(double deltaSeconds) {
        Point goal = headingToTarget ? targetPoint : startPoint;
        double centerX = posX + getWidth() / 2.0;
        double centerY = posY + getHeight() / 2.0;
        double dx = goal.x - centerX;
        double dy = goal.y - centerY;
        double distance = Math.hypot(dx, dy);

        double stepDistance = WALK_SPEED_PX_PER_SEC * deltaSeconds;
        if (distance <= stepDistance || distance == 0) {
            posX = goal.x - getWidth() / 2.0;
            posY = goal.y - getHeight() / 2.0;
            setLocation((int) Math.round(posX), (int) Math.round(posY));
            handleArrival();
            return;
        }

        double ratio = stepDistance / distance;
        posX += dx * ratio;
        posY += dy * ratio;
        setLocation((int) Math.round(posX), (int) Math.round(posY));
    }

    private void handleArrival() {
        if (headingToTarget) {
            headingToTarget = false;
            timer.stop();

            Timer pauseTimer = new Timer(PAUSE_AT_TARGET_MS, e -> {
                ((Timer) e.getSource()).stop();
                timer.start();
            });
            pauseTimer.setRepeats(false);
            pauseTimer.start();
        } else {
            timer.stop();
            Timer pauseTimer = new Timer(PAUSE_AT_TARGET_MS / 2, e -> {
                ((Timer) e.getSource()).stop();
                dispose();
            });
            pauseTimer.setRepeats(false);
            pauseTimer.start();
        }
    }

    private void loadIdleFrames() {
        File spriteSheet = new File("spr_idle_strip9.png");
        if (!spriteSheet.exists()) {
            return;
        }

        try {
            BufferedImage sheet = ImageIO.read(spriteSheet);
            int frameWidth = 64;
            int frameHeight = 64;
            int frameCount = sheet.getWidth() / frameWidth;
            for (int i = 0; i < frameCount; i++) {
                BufferedImage frame = sheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
                Image scaled = frame.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                idleFrames.add(new ImageIcon(scaled));
            }
            currentFrameIndex = 0;
        } catch (IOException e) {
            System.out.println("Unable to load idle sprite sheet: " + e.getMessage());
        }
    }

    private void advanceIdleFrame(double deltaSeconds) {
        if (idleFrames.isEmpty()) {
            return;
        }
        animationAccumulatorSeconds += deltaSeconds;
        if (animationAccumulatorSeconds < IDLE_FRAME_SECONDS) {
            return;
        }
        animationAccumulatorSeconds %= IDLE_FRAME_SECONDS;
        currentFrameIndex = (currentFrameIndex + 1) % idleFrames.size();
        creatureLabel.setIcon(idleFrames.get(currentFrameIndex));
    }
}
