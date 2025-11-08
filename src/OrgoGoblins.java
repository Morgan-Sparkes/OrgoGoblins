import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.List;
import java.util.Optional;

public class OrgoGoblins extends JFrame {

    public OrgoGoblins() {

        System.out.println("Reading app usage Data");
        List<AppUsageReader.AppInfo> apps = AppUsageReader.readAppUsage("app_usage.json");

        System.out.println("Loaded apps");
        for (AppUsageReader.AppInfo app : apps) {
            System.out.println("-" + app);
        }

        ImageIcon hutIcon = new ImageIcon("hut.png");
        Image scaledImage = hutIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
        JButton hutButton = new JButton(new ImageIcon(scaledImage));
        hutButton.setPreferredSize(new Dimension(120, 120));

        hutButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                List<AppUsageReader.AppInfo> refreshedApps = AppUsageReader.readAppUsage("app_usage.json");
                Optional<AppUsageReader.AppInfo> leastUsed = refreshedApps.stream()
                        .min((a, b) -> Long.compare(a.usageCount, b.usageCount));

                Point hutCenter = getComponentCenterOnScreen(hutButton);
                Point targetPoint = computeTargetPoint(leastUsed.orElse(null), hutCenter);

                System.out.println("Summoning creature from " + hutCenter + " to " + targetPoint +
                        leastUsed.map(info -> " (target app: " + info.name + ", usage " + info.usageCount + ")")
                                .orElse(""));

                CreatureWindow creature = new CreatureWindow(hutCenter, targetPoint);
                creature.showWindow();
            });
        });

        setLayout(new BorderLayout());
        add(hutButton, BorderLayout.CENTER);
        setTitle("OrgoGoblins Hut");
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OrgoGoblins goblins = new OrgoGoblins();
            goblins.setVisible(true);
        });
    }

    private static Point getComponentCenterOnScreen(JButton button) {
        try {
            Point location = button.getLocationOnScreen();
            return new Point(
                    location.x + button.getWidth() / 2,
                    location.y + button.getHeight() / 2
            );
        } catch (IllegalComponentStateException ex) {
            Container root = button.getTopLevelAncestor();
            if (root != null) {
                try {
                    Point rootLocation = root.getLocationOnScreen();
                    return new Point(
                            rootLocation.x + button.getWidth() / 2,
                            rootLocation.y + button.getHeight() / 2
                    );
                } catch (IllegalComponentStateException ignored) {
                    // fall through to default
                }
            }
            return new Point(button.getWidth() / 2, button.getHeight() / 2);
        }
    }

    private static Point computeTargetPoint(AppUsageReader.AppInfo appInfo, Point hutCenter) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (appInfo == null) {
            int x = Math.min(screen.width - 100, hutCenter.x + 300);
            int y = Math.min(screen.height - 100, hutCenter.y);
            return new Point(x, y);
        }

        int hash = Math.abs(appInfo.name.hashCode());
        double angle = (hash % 360) * Math.PI / 180.0;
        int radius = 250 + (hash % 200); // Distance between 250-449 pixels.

        int targetX = hutCenter.x + (int) (Math.cos(angle) * radius);
        int targetY = hutCenter.y + (int) (Math.sin(angle) * radius);

        int margin = 80;
        targetX = Math.max(margin, Math.min(screen.width - margin, targetX));
        targetY = Math.max(margin, Math.min(screen.height - margin, targetY));

        return new Point(targetX, targetY);
    }
}
