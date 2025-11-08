import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
            loadFallbackIcon();
        }

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(creatureLabel, BorderLayout.CENTER);
        pack();

        posX = startPoint.x - getWidth() / 2.0;
        posY = startPoint.y - getHeight() / 2.0;
        setLocation((int) Math.round(posX), (int) Math.round(posY));

        timer = new Timer(TIMER_DELAY_MS, e -> onTick());
        timer.setCoalesce(false);
        timer.setInitialDelay(0);
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
        if (loadFramesFromFiles()) {
            currentFrameIndex = 0;
            return;
        }
        loadFramesFromSpriteSheet();
    }

    private boolean loadFramesFromFiles() {
        boolean loaded = false;
        String[] fileNames = {
                "GoblinIdle1.png",
                "GoblinIdle2.png",
                "GoblinIdle3.png",
                "GoblinIdle4.png",
                "GoblinIdle5.png",
                "GoblinIdle6.png",
                "GoblinIdle7.png",
                "GoblinIdle8.png"
        };

        for (String name : fileNames) {
            File frameFile = new File(name);
            if (!frameFile.exists()) {
                continue;
            }
            try {
                BufferedImage frame = ImageIO.read(frameFile);
                idleFrames.add(new ImageIcon(scaleImage(frame, 80, 80)));
                loaded = true;
            } catch (IOException e) {
                System.out.println("Unable to load idle frame " + name + ": " + e.getMessage());
            }
        }
        return loaded;
    }

    private void loadFramesFromSpriteSheet() {
        File spriteSheet = new File("spr_idle_strip9.png");
        if (!spriteSheet.exists()) {
            return;
        }

        try {
            BufferedImage sheet = ImageIO.read(spriteSheet);
            int frameWidth = 64;
            int frameHeight = 64;

            int contentStart = findFirstOpaqueColumn(sheet);
            int contentEnd = findLastOpaqueColumn(sheet);
            if (contentStart == -1 || contentEnd == -1 || contentStart > contentEnd) {
                System.out.println("Idle sprite sheet contains no opaque pixels.");
                return;
            }

            int contentWidth = contentEnd - contentStart + 1;
            int frameCount = contentWidth / frameWidth;
            if (frameCount <= 0) {
                System.out.println("Sprite sheet content too narrow for frame width.");
                return;
            }

            for (int i = 0; i < frameCount; i++) {
                int frameX = contentStart + i * frameWidth;
                if (frameX + frameWidth > sheet.getWidth()) {
                    break;
                }
                BufferedImage frame = sheet.getSubimage(frameX, 0, frameWidth, frameHeight);
                BufferedImage scaled = scaleImage(frame, 80, 80);
                idleFrames.add(new ImageIcon(scaled));
            }
            currentFrameIndex = 0;
        } catch (IOException e) {
            System.out.println("Unable to load idle sprite sheet: " + e.getMessage());
        }
    }

    private void loadFallbackIcon() {
        File fallbackFile = new File("creature.png");
        if (!fallbackFile.exists()) {
            return;
        }
        try {
            BufferedImage fallback = ImageIO.read(fallbackFile);
            creatureLabel.setIcon(new ImageIcon(scaleImage(fallback, 80, 80)));
        } catch (IOException e) {
            System.out.println("Unable to load fallback creature icon: " + e.getMessage());
        }
    }

    private BufferedImage scaleImage(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(source, 0, 0, width, height, null);
        g2.dispose();
        return scaled;
    }

    private int findFirstOpaqueColumn(BufferedImage image) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (((image.getRGB(x, y) >> 24) & 0xFF) > 0) {
                    return x;
                }
            }
        }
        return -1;
    }

    private int findLastOpaqueColumn(BufferedImage image) {
        for (int x = image.getWidth() - 1; x >= 0; x--) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (((image.getRGB(x, y) >> 24) & 0xFF) > 0) {
                    return x;
                }
            }
        }
        return -1;
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
