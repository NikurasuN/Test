package com.example.herolinewars;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

/**
 * A lightweight real-time version of Hero Line Wars where the action happens on the lane.
 */
public class HeroLineWarsGame extends JFrame {
    private static final int PANEL_WIDTH = 900;
    private static final int PANEL_HEIGHT = 440;
    private static final int HERO_WIDTH = 48;
    private static final int HERO_HEIGHT = 90;
    private static final int BASE_WIDTH = 80;
    private static final int BASE_HEIGHT = 190;
    private static final int BASE_MARGIN = 32;
    private static final double HERO_SPEED = 4.5;
    private static final double ENEMY_SPEED = 3.4;
    private static final int ATTACK_RANGE = 72;
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    private static final int BASE_ATTACK_COOLDOWN_TICKS = 28;
    private static final int RESPAWN_TICKS = 120;
    private static final int BASE_DAMAGE_PER_TICK = 55;
    private static final int TICK_MILLIS = 30;

    private final Random random = new Random();

    private Hero playerHero;
    private Hero aiHero;

    private final JLabel modeLabel = new JLabel("Hero Line Wars - Live Battle");
    private final JLabel baseLabel = new JLabel();
    private final JLabel heroLabel = new JLabel();
    private final JLabel aiLabel = new JLabel();
    private final JLabel killsLabel = new JLabel();

    private final BattlefieldPanel battlefieldPanel = new BattlefieldPanel();
    private Timer gameTimer;

    private double heroX;
    private double heroTargetX;
    private double enemyX;
    private double enemyTargetX;
    private boolean heroAlive;
    private boolean enemyAlive;
    private int heroAttackCooldown;
    private int enemyAttackCooldown;
    private int heroBaseAttackCooldown;
    private int enemyBaseAttackCooldown;
    private int heroRespawnTimer;
    private int enemyRespawnTimer;
    private int playerBaseHealth;
    private int enemyBaseHealth;
    private int playerKills;
    private int enemyKills;
    private boolean gameOver;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HeroLineWarsGame game = new HeroLineWarsGame();
            game.setVisible(true);
        });
    }

    public HeroLineWarsGame() {
        super("Hero Line Wars");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(960, 640));

        buildInterface();
        pack();
        setLocationRelativeTo(null);

        showHeroSelectionDialog();
    }

    private void buildInterface() {
        JPanel statusPanel = new JPanel(new GridLayout(0, 1));
        modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD, 16f));
        statusPanel.add(modeLabel);
        statusPanel.add(baseLabel);
        statusPanel.add(heroLabel);
        statusPanel.add(aiLabel);
        statusPanel.add(killsLabel);
        add(statusPanel, BorderLayout.NORTH);

        add(battlefieldPanel, BorderLayout.CENTER);

        JLabel helpLabel = new JLabel("Click the lane to move your hero. Heroes auto-attack when in range.", SwingConstants.CENTER);
        helpLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 0, 6, 0));
        add(helpLabel, BorderLayout.SOUTH);
    }

    private void showHeroSelectionDialog() {
        JDialog dialog = new JDialog(this, "Choose Your Hero", true);
        dialog.setLayout(new BorderLayout());

        JLabel description = new JLabel("Select a hero to begin the battle.");
        description.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.add(description, BorderLayout.NORTH);

        JPanel heroPanel = new JPanel(new GridLayout(0, 1, 10, 10));

        JButton rangerButton = new JButton("Ranger - Balanced stats and reliable damage.");
        rangerButton.addActionListener(e -> {
            playerHero = new Hero("Ranger", 95, 16, 4, 0, 0);
            dialog.dispose();
            startBattle();
        });
        heroPanel.add(rangerButton);

        JButton knightButton = new JButton("Knight - Heavily armored and built to tank waves.");
        knightButton.addActionListener(e -> {
            playerHero = new Hero("Knight", 125, 12, 7, 0, 0);
            dialog.dispose();
            startBattle();
        });
        heroPanel.add(knightButton);

        JButton mageButton = new JButton("Battle Mage - Fragile but deals heavy attacks.");
        mageButton.addActionListener(e -> {
            playerHero = new Hero("Battle Mage", 80, 20, 3, 0, 0);
            dialog.dispose();
            startBattle();
        });
        heroPanel.add(mageButton);

        dialog.add(heroPanel, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void startBattle() {
        aiHero = createAiHero();
        playerBaseHealth = 1000;
        enemyBaseHealth = 1000;
        playerKills = 0;
        enemyKills = 0;
        heroAlive = true;
        enemyAlive = true;
        heroRespawnTimer = 0;
        enemyRespawnTimer = 0;
        heroAttackCooldown = 0;
        enemyAttackCooldown = 0;
        heroBaseAttackCooldown = 0;
        enemyBaseAttackCooldown = 0;
        gameOver = false;

        playerHero.resetHealth();
        aiHero.resetHealth();

        heroX = getPlayerSpawnX();
        heroTargetX = heroX;
        enemyX = getEnemySpawnX();
        enemyTargetX = enemyX;

        refreshHud();
        battlefieldPanel.repaint();

        if (gameTimer != null) {
            gameTimer.stop();
        }
        gameTimer = new Timer(TICK_MILLIS, e -> updateGame());
        gameTimer.start();
    }

    private void updateGame() {
        if (gameOver) {
            return;
        }

        double leftBound = getLaneLeftBound();
        double rightBound = getLaneRightBound();

        heroTargetX = clamp(heroTargetX, leftBound, rightBound);
        enemyTargetX = clamp(enemyTargetX, leftBound, rightBound);

        if (heroAlive) {
            heroX = approach(heroX, heroTargetX, HERO_SPEED);
        } else {
            if (heroRespawnTimer > 0) {
                heroRespawnTimer--;
            }
            if (heroRespawnTimer <= 0) {
                heroAlive = true;
                playerHero.resetHealth();
                heroX = getPlayerSpawnX();
                heroTargetX = heroX;
            }
        }

        if (enemyAlive) {
            if (heroAlive) {
                enemyTargetX = clamp(heroX + HERO_WIDTH + 25, leftBound, rightBound);
            } else {
                enemyTargetX = clamp(getPlayerBaseX() + BASE_WIDTH + 16, leftBound, rightBound);
            }
            enemyX = approach(enemyX, enemyTargetX, ENEMY_SPEED);
        } else {
            if (enemyRespawnTimer > 0) {
                enemyRespawnTimer--;
            }
            if (enemyRespawnTimer <= 0) {
                enemyAlive = true;
                aiHero.resetHealth();
                enemyX = getEnemySpawnX();
                enemyTargetX = enemyX;
            }
        }

        if (heroAttackCooldown > 0) {
            heroAttackCooldown--;
        }
        if (enemyAttackCooldown > 0) {
            enemyAttackCooldown--;
        }
        if (heroBaseAttackCooldown > 0) {
            heroBaseAttackCooldown--;
        }
        if (enemyBaseAttackCooldown > 0) {
            enemyBaseAttackCooldown--;
        }

        handleHeroCombat();
        handleBasePressure();

        refreshHud();
        battlefieldPanel.repaint();
    }

    private void handleHeroCombat() {
        if (!heroAlive || !enemyAlive) {
            return;
        }
        double heroCenter = heroX + HERO_WIDTH / 2.0;
        double enemyCenter = enemyX + HERO_WIDTH / 2.0;
        double distance = Math.abs(heroCenter - enemyCenter);
        if (distance > ATTACK_RANGE) {
            return;
        }
        if (heroAttackCooldown <= 0) {
            int damage = Math.max(1, playerHero.getAttack() - aiHero.getDefense());
            if (aiHero.takeDamage(damage)) {
                onEnemyHeroDefeated();
                return;
            }
            heroAttackCooldown = ATTACK_COOLDOWN_TICKS;
        }
        if (enemyAttackCooldown <= 0) {
            int damage = Math.max(1, aiHero.getAttack() - playerHero.getDefense());
            if (playerHero.takeDamage(damage)) {
                onPlayerHeroDefeated();
                return;
            }
            enemyAttackCooldown = ATTACK_COOLDOWN_TICKS;
        }
    }

    private void handleBasePressure() {
        if (heroAlive && !enemyAlive) {
            if (heroBaseAttackCooldown <= 0 && heroX + HERO_WIDTH >= getEnemyBaseX()) {
                enemyBaseHealth = Math.max(0, enemyBaseHealth - Math.max(BASE_DAMAGE_PER_TICK, playerHero.getAttack() * 3));
                heroBaseAttackCooldown = BASE_ATTACK_COOLDOWN_TICKS;
                checkVictoryConditions();
            }
        }
        if (enemyAlive && !heroAlive) {
            if (enemyBaseAttackCooldown <= 0 && enemyX <= getPlayerBaseX() + BASE_WIDTH) {
                playerBaseHealth = Math.max(0, playerBaseHealth - Math.max(BASE_DAMAGE_PER_TICK, aiHero.getAttack() * 3));
                enemyBaseAttackCooldown = BASE_ATTACK_COOLDOWN_TICKS;
                checkVictoryConditions();
            }
        }
    }

    private void onPlayerHeroDefeated() {
        heroAlive = false;
        heroRespawnTimer = RESPAWN_TICKS;
        enemyKills++;
        heroAttackCooldown = ATTACK_COOLDOWN_TICKS;
    }

    private void onEnemyHeroDefeated() {
        enemyAlive = false;
        enemyRespawnTimer = RESPAWN_TICKS;
        playerKills++;
        enemyAttackCooldown = ATTACK_COOLDOWN_TICKS;
    }

    private void checkVictoryConditions() {
        if (gameOver) {
            return;
        }
        if (enemyBaseHealth <= 0) {
            finishBattle(true);
        } else if (playerBaseHealth <= 0) {
            finishBattle(false);
        }
    }

    private void finishBattle(boolean playerWon) {
        gameOver = true;
        if (gameTimer != null) {
            gameTimer.stop();
        }
        String message = playerWon ? "Victory! The enemy base has fallen." : "Defeat! Your base has been destroyed.";
        JOptionPane.showMessageDialog(this, message, "Battle Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshHud() {
        if (playerHero == null || aiHero == null) {
            return;
        }
        baseLabel.setText(String.format("Base HP - You: %d | Enemy: %d", Math.max(0, playerBaseHealth), Math.max(0, enemyBaseHealth)));
        heroLabel.setText(String.format("Hero: %s | HP %d/%d | ATK %d | DEF %d", playerHero.getName(),
                Math.max(0, playerHero.getCurrentHealth()), playerHero.getMaxHealth(), playerHero.getAttack(), playerHero.getDefense()));
        aiLabel.setText(String.format("Enemy Hero: %s | HP %d/%d | ATK %d | DEF %d", aiHero.getName(),
                Math.max(0, aiHero.getCurrentHealth()), aiHero.getMaxHealth(), aiHero.getAttack(), aiHero.getDefense()));
        killsLabel.setText(String.format("Kills - You: %d | Enemy: %d", playerKills, enemyKills));
    }

    private Hero createAiHero() {
        int roll = random.nextInt(3);
        switch (roll) {
            case 0:
                return new Hero("Sentinel", 110, 14, 6, 0, 0);
            case 1:
                return new Hero("Berserker", 85, 19, 4, 0, 0);
            default:
                return new Hero("Warlock", 90, 17, 5, 0, 0);
        }
    }

    private double getLaneLeftBound() {
        return BASE_MARGIN + BASE_WIDTH + 8;
    }

    private double getLaneRightBound() {
        int width = battlefieldPanel.getWidth();
        if (width <= 0) {
            width = battlefieldPanel.getPreferredSize().width;
        }
        return width - BASE_MARGIN - BASE_WIDTH - HERO_WIDTH - 8;
    }

    private int getPlayerBaseX() {
        return BASE_MARGIN;
    }

    private int getEnemyBaseX() {
        int width = battlefieldPanel.getWidth();
        if (width <= 0) {
            width = battlefieldPanel.getPreferredSize().width;
        }
        return width - BASE_MARGIN - BASE_WIDTH;
    }

    private double getPlayerSpawnX() {
        return getLaneLeftBound() + 20;
    }

    private double getEnemySpawnX() {
        return getLaneRightBound() - 20;
    }

    private static double approach(double current, double target, double speed) {
        if (Math.abs(target - current) <= speed) {
            return target;
        }
        return current + Math.signum(target - current) * speed;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private class BattlefieldPanel extends JPanel {
        BattlefieldPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(new Color(20, 30, 22));

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleClick(e);
                }
            };
            addMouseListener(adapter);
        }

        private void handleClick(MouseEvent e) {
            if (!heroAlive || gameOver) {
                return;
            }
            double target = e.getX() - HERO_WIDTH / 2.0;
            heroTargetX = clamp(target, getLaneLeftBound(), getLaneRightBound());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int laneTop = height / 2 - 120;
            int laneHeight = 240;
            g2.setColor(new Color(32, 48, 34));
            g2.fillRoundRect(0, laneTop, width, laneHeight, 25, 25);

            int floorY = laneTop + laneHeight - 20;
            g2.setColor(new Color(24, 38, 26));
            g2.fillRect(0, floorY, width, 20);

            drawBase(g2, getPlayerBaseX(), floorY, new Color(66, 135, 245));
            drawBase(g2, getEnemyBaseX(), floorY, new Color(200, 70, 70));

            if (enemyAlive) {
                drawHero(g2, (int) Math.round(enemyX), floorY - HERO_HEIGHT, aiHero.getName(),
                        aiHero.getCurrentHealth(), aiHero.getMaxHealth(), new Color(214, 68, 68));
            } else {
                drawRespawnIndicator(g2, (int) Math.round(enemyX), floorY - HERO_HEIGHT, enemyRespawnTimer);
            }

            if (heroAlive) {
                drawHero(g2, (int) Math.round(heroX), floorY - HERO_HEIGHT, playerHero.getName(),
                        playerHero.getCurrentHealth(), playerHero.getMaxHealth(), new Color(64, 144, 255));
            } else {
                drawRespawnIndicator(g2, (int) Math.round(heroX), floorY - HERO_HEIGHT, heroRespawnTimer);
            }

            g2.dispose();
        }

        private void drawBase(Graphics2D g2, int baseX, int floorY, Color color) {
            int baseTop = floorY - BASE_HEIGHT;
            g2.setColor(color.darker());
            g2.fillRoundRect(baseX - 4, baseTop - 6, BASE_WIDTH + 8, BASE_HEIGHT + 12, 18, 18);
            g2.setColor(color);
            g2.fillRoundRect(baseX, baseTop, BASE_WIDTH, BASE_HEIGHT, 16, 16);
            int barWidth = BASE_WIDTH;
            int barHeight = 10;
            int currentHp;
            int maxHp = 1000;
            if (baseX <= BASE_MARGIN + 1) {
                currentHp = Math.max(0, playerBaseHealth);
            } else {
                currentHp = Math.max(0, enemyBaseHealth);
            }
            double ratio = Math.min(1.0, currentHp / (double) maxHp);
            g2.setColor(Color.DARK_GRAY);
            g2.fillRoundRect(baseX, baseTop - barHeight - 4, barWidth, barHeight, 10, 10);
            g2.setColor(new Color(30, 200, 90));
            g2.fillRoundRect(baseX, baseTop - barHeight - 4, (int) Math.round(barWidth * ratio), barHeight, 10, 10);
        }

        private void drawHero(Graphics2D g2, int x, int y, String name, int currentHp, int maxHp, Color color) {
            g2.setColor(color);
            g2.fillRoundRect(x, y, HERO_WIDTH, HERO_HEIGHT, 18, 18);
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(x, y, HERO_WIDTH, HERO_HEIGHT, 18, 18);

            int barWidth = HERO_WIDTH;
            int barHeight = 8;
            double ratio = Math.min(1.0, Math.max(0, currentHp) / (double) maxHp);
            int barX = x;
            int barY = y - barHeight - 4;
            g2.setColor(new Color(45, 45, 45));
            g2.fillRoundRect(barX, barY, barWidth, barHeight, 8, 8);
            g2.setColor(new Color(70, 220, 90));
            g2.fillRoundRect(barX, barY, (int) Math.round(barWidth * ratio), barHeight, 8, 8);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            g2.setColor(Color.WHITE);
            int textY = y - barHeight - 8;
            g2.drawString(name, x - 4, textY - 4);
        }

        private void drawRespawnIndicator(Graphics2D g2, int x, int y, int timer) {
            g2.setColor(new Color(120, 120, 120, 150));
            g2.fillRoundRect(x, y, HERO_WIDTH, HERO_HEIGHT, 18, 18);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawRoundRect(x, y, HERO_WIDTH, HERO_HEIGHT, 18, 18);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.setColor(Color.WHITE);
            String text = "Respawn";
            int textWidth = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, x + (HERO_WIDTH - textWidth) / 2, y + HERO_HEIGHT / 2);
            String countdown = String.format("%.1f s", Math.max(0, timer * TICK_MILLIS / 1000.0));
            int countdownWidth = g2.getFontMetrics().stringWidth(countdown);
            g2.drawString(countdown, x + (HERO_WIDTH - countdownWidth) / 2, y + HERO_HEIGHT / 2 + 16);
        }
    }
}
