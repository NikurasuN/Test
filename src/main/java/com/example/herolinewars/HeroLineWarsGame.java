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
    private static final int LANE_MARGIN = 20;
    private static final int LANE_GAP = 40;
    private static final double HERO_SPEED = 4.5;
    private static final double ENEMY_SPEED = 3.4;
    private static final int ATTACK_RANGE = 72;
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    private static final int BASE_ATTACK_COOLDOWN_TICKS = 28;
    private static final int RESPAWN_TICKS = 120;
    private static final int BASE_DAMAGE_PER_TICK = 55;
    private static final int TICK_MILLIS = 30;
    private static final int INCOME_INTERVAL_TICKS = 120;
    private static final int WAVE_INTERVAL_TICKS = 240;
    private static final double UNIT_SPEED = 2.6;
    private static final int UNIT_SIZE = 28;
    private static final int UNIT_ATTACK_COOLDOWN_TICKS = 24;
    private static final int UNIT_BASE_ATTACK_COOLDOWN_TICKS = 30;
    private static final int UNIT_KILL_REWARD = 6;

    private final Random random = new Random();

    private Hero playerHero;
    private Hero aiHero;
    private Team playerTeam;
    private Team enemyTeam;

    private final JLabel modeLabel = new JLabel("Hero Line Wars - Live Battle");
    private final JLabel baseLabel = new JLabel();
    private final JLabel heroLabel = new JLabel();
    private final JLabel aiLabel = new JLabel();
    private final JLabel killsLabel = new JLabel();
    private final JLabel economyLabel = new JLabel();
    private final JLabel actionLabel = new JLabel("Ready to launch units down the lane.");
    private final JLabel heroPrimaryLabel = new JLabel();
    private final JLabel heroStrengthLabel = new JLabel();
    private final JLabel heroDexterityLabel = new JLabel();
    private final JLabel heroIntelligenceLabel = new JLabel();

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
    private int incomeTickTimer;
    private int aiSendTimer;
    private int waveCountdown;
    private String lastActionMessage = "Ready to launch units down the lane.";
    private final java.util.List<UnitInstance> playerUnits = new java.util.ArrayList<>();
    private final java.util.List<UnitInstance> enemyUnits = new java.util.ArrayList<>();

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
        statusPanel.add(economyLabel);
        statusPanel.add(actionLabel);
        add(statusPanel, BorderLayout.NORTH);

        add(battlefieldPanel, BorderLayout.CENTER);

        JPanel commandPanel = new JPanel(new BorderLayout());
        JLabel helpLabel = new JLabel("Click the lane to reposition. Use the buttons to send reinforcements for more income.", SwingConstants.CENTER);
        helpLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 0, 6, 0));
        commandPanel.add(helpLabel, BorderLayout.NORTH);

        JPanel unitButtonPanel = new JPanel(new GridLayout(1, 0, 6, 6));
        for (UnitType type : UnitType.values()) {
            JButton button = new JButton(String.format("%s (%dG, +%d income)", type.getDisplayName(), type.getCost(), type.getIncomeBonus()));
            button.addActionListener(e -> attemptSendUnit(type));
            unitButtonPanel.add(button);
        }
        unitButtonPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 10, 10));
        commandPanel.add(unitButtonPanel, BorderLayout.CENTER);

        JPanel heroInterfacePanel = createHeroInterfacePanel();
        commandPanel.add(heroInterfacePanel, BorderLayout.SOUTH);

        add(commandPanel, BorderLayout.SOUTH);
        refreshHeroInterface();
    }

    private JPanel createHeroInterfacePanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 2, 2));
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Hero Interface"));
        panel.add(heroPrimaryLabel);
        panel.add(heroStrengthLabel);
        panel.add(heroDexterityLabel);
        panel.add(heroIntelligenceLabel);
        return panel;
    }

    private void showHeroSelectionDialog() {
        JDialog dialog = new JDialog(this, "Choose Your Hero", true);
        dialog.setLayout(new BorderLayout());

        JLabel description = new JLabel("Select a hero to begin the battle.");
        description.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.add(description, BorderLayout.NORTH);

        JPanel heroPanel = new JPanel(new GridLayout(0, 1, 10, 10));

        JButton rangerButton = new JButton("Ranger - Dexterity primary with swift volleys.");
        rangerButton.addActionListener(e -> {
            playerHero = new Hero("Ranger", 12, 16, 10, Hero.PrimaryAttribute.DEXTERITY, 40, 6, 2, 180, 12);
            dialog.dispose();
            startBattle();
        });
        heroPanel.add(rangerButton);

        JButton knightButton = new JButton("Knight - Strength primary and steadfast defender.");
        knightButton.addActionListener(e -> {
            playerHero = new Hero("Knight", 18, 8, 6, Hero.PrimaryAttribute.STRENGTH, 42, 4, 3, 200, 10);
            dialog.dispose();
            startBattle();
        });
        heroPanel.add(knightButton);

        JButton mageButton = new JButton("Battle Mage - Intelligence primary with explosive bursts.");
        mageButton.addActionListener(e -> {
            playerHero = new Hero("Battle Mage", 8, 10, 18, Hero.PrimaryAttribute.INTELLIGENCE, 40, 7, 1, 160, 14);
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
        playerTeam = new Team("Player", playerHero);
        enemyTeam = new Team("Enemy", aiHero);
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
        incomeTickTimer = 0;
        aiSendTimer = 60;
        waveCountdown = WAVE_INTERVAL_TICKS;
        playerUnits.clear();
        enemyUnits.clear();
        lastActionMessage = "Battle underway. Send units to pressure the enemy!";

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

        incomeTickTimer++;
        if (incomeTickTimer >= INCOME_INTERVAL_TICKS) {
            incomeTickTimer = 0;
            playerHero.earnIncome();
            aiHero.earnIncome();
        }

        if (aiSendTimer > 0) {
            aiSendTimer--;
        }
        if (aiSendTimer <= 0) {
            attemptAiSendUnit();
            aiSendTimer = 90 + random.nextInt(90);
        }

        if (waveCountdown > 0) {
            waveCountdown--;
        }
        if (waveCountdown <= 0) {
            launchNextWave();
            waveCountdown = WAVE_INTERVAL_TICKS;
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
            UnitInstance threat = findNearestUnit(playerUnits, enemyX + HERO_WIDTH / 2.0);
            if (threat != null) {
                double desired = threat.getCenterX() - HERO_WIDTH / 2.0 - 18;
                enemyTargetX = clamp(desired, leftBound, rightBound);
            } else {
                enemyTargetX = clamp(getEnemySpawnX(), leftBound, rightBound);
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
        updateUnits();
        resolveHeroUnitCombat();

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
            int damage = computeHeroDamage(playerHero, aiHero);
            if (aiHero.takeDamage(damage)) {
                onEnemyHeroDefeated();
                return;
            }
            heroAttackCooldown = playerHero.getAttackDelayTicks(ATTACK_COOLDOWN_TICKS);
        }
        if (enemyAttackCooldown <= 0) {
            int damage = computeHeroDamage(aiHero, playerHero);
            if (playerHero.takeDamage(damage)) {
                onPlayerHeroDefeated();
                return;
            }
            enemyAttackCooldown = aiHero.getAttackDelayTicks(ATTACK_COOLDOWN_TICKS);
        }
    }

    private int computeHeroDamage(Hero attacker, Hero defender) {
        int rawDamage = attacker.rollAttackDamage(random);
        return Math.max(1, rawDamage - defender.getDefense());
    }

    private void handleBasePressure() {
        if (heroAlive && !enemyAlive) {
            if (heroBaseAttackCooldown <= 0 && heroX + HERO_WIDTH >= getEnemyBaseX()) {
                enemyBaseHealth = Math.max(0, enemyBaseHealth - Math.max(BASE_DAMAGE_PER_TICK, playerHero.getAttack() * 3));
                heroBaseAttackCooldown = playerHero.getAttackDelayTicks(BASE_ATTACK_COOLDOWN_TICKS);
                checkVictoryConditions();
            }
        }
        if (enemyAlive && !heroAlive) {
            if (enemyBaseAttackCooldown <= 0 && enemyX <= getPlayerBaseX() + BASE_WIDTH) {
                playerBaseHealth = Math.max(0, playerBaseHealth - Math.max(BASE_DAMAGE_PER_TICK, aiHero.getAttack() * 3));
                enemyBaseAttackCooldown = aiHero.getAttackDelayTicks(BASE_ATTACK_COOLDOWN_TICKS);
                checkVictoryConditions();
            }
        }
    }

    private void onPlayerHeroDefeated() {
        heroAlive = false;
        heroRespawnTimer = RESPAWN_TICKS;
        enemyKills++;
        heroAttackCooldown = playerHero.getAttackDelayTicks(ATTACK_COOLDOWN_TICKS);
        lastActionMessage = "You were defeated! The enemy presses the attack.";
    }

    private void onEnemyHeroDefeated() {
        enemyAlive = false;
        enemyRespawnTimer = RESPAWN_TICKS;
        playerKills++;
        enemyAttackCooldown = aiHero.getAttackDelayTicks(ATTACK_COOLDOWN_TICKS);
        lastActionMessage = "Enemy hero defeated! Push the advantage.";
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
        lastActionMessage = playerWon ? "Victory! Enemy base destroyed." : "Defeat! Your base has fallen.";
        refreshHud();
        battlefieldPanel.repaint();
        JOptionPane.showMessageDialog(this, message, "Battle Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshHud() {
        if (playerHero == null || aiHero == null) {
            refreshHeroInterface();
            return;
        }
        baseLabel.setText(String.format("Base HP - You: %d | Enemy: %d", Math.max(0, playerBaseHealth), Math.max(0, enemyBaseHealth)));
        heroLabel.setText(String.format("Hero: %s | HP %d/%d | Shield %d/%d | ATK %d | DEF %d", playerHero.getName(),
                Math.max(0, playerHero.getCurrentHealth()), playerHero.getMaxHealth(),
                Math.max(0, playerHero.getCurrentEnergyShield()), playerHero.getMaxEnergyShield(),
                playerHero.getAttack(), playerHero.getDefense()));
        aiLabel.setText(String.format("Enemy Hero: %s | HP %d/%d | Shield %d/%d | ATK %d | DEF %d", aiHero.getName(),
                Math.max(0, aiHero.getCurrentHealth()), aiHero.getMaxHealth(),
                Math.max(0, aiHero.getCurrentEnergyShield()), aiHero.getMaxEnergyShield(),
                aiHero.getAttack(), aiHero.getDefense()));
        killsLabel.setText(String.format("Kills - You: %d | Enemy: %d", playerKills, enemyKills));
        economyLabel.setText(String.format("Economy - Gold %d (+%d) | Enemy Gold %d (+%d)",
                playerHero.getGold(), playerHero.getIncome(), aiHero.getGold(), aiHero.getIncome()));
        double seconds = Math.max(0, waveCountdown) * TICK_MILLIS / 1000.0;
        actionLabel.setText(String.format("%s Next wave in %.1f s.", lastActionMessage, seconds));
        refreshHeroInterface();
    }

    private void refreshHeroInterface() {
        if (playerHero == null) {
            heroPrimaryLabel.setText("Primary Attribute: -");
            heroStrengthLabel.setText("Strength: -");
            heroDexterityLabel.setText("Dexterity: -");
            heroIntelligenceLabel.setText("Intelligence: -");
            return;
        }

        int attackSpeedPercent = (int) Math.round((playerHero.getAttackSpeedMultiplier() - 1.0) * 100.0);
        heroPrimaryLabel.setText(String.format("Primary Attribute: %s | Damage %d | Attack Speed %+d%%",
                playerHero.getPrimaryAttribute().getDisplayName(),
                playerHero.getAttack(),
                attackSpeedPercent));

        heroStrengthLabel.setText(String.format("Strength %d → Max Health %d, Armor %d",
                playerHero.getStrength(),
                playerHero.getMaxHealth(),
                playerHero.getDefense()));

        int evasionPercent = (int) Math.round(playerHero.getEvasionChance() * 100.0);
        heroDexterityLabel.setText(String.format("Dexterity %d → Evasion %d%%",
                playerHero.getDexterity(),
                evasionPercent));

        int critPercent = (int) Math.round(playerHero.getCriticalChance() * 100.0);
        heroIntelligenceLabel.setText(String.format("Intelligence %d → Crit Chance %d%%, Shield %d/%d",
                playerHero.getIntelligence(),
                critPercent,
                Math.max(0, playerHero.getCurrentEnergyShield()),
                playerHero.getMaxEnergyShield()));
    }

    private Hero createAiHero() {
        int roll = random.nextInt(3);
        switch (roll) {
            case 0:
                return new Hero("Sentinel", 16, 9, 8, Hero.PrimaryAttribute.STRENGTH, 44, 5, 3, 180, 12);
            case 1:
                return new Hero("Berserker", 12, 18, 7, Hero.PrimaryAttribute.DEXTERITY, 40, 6, 2, 170, 13);
            default:
                return new Hero("Warlock", 9, 11, 17, Hero.PrimaryAttribute.INTELLIGENCE, 40, 7, 2, 190, 11);
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
            if (!isInPlayerLane(e.getY())) {
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
            if (width <= 0) {
                width = getPreferredSize().width;
            }
            int laneHeight = calculateLaneHeight();
            int playerLaneTop = LANE_MARGIN;
            int enemyLaneTop = playerLaneTop + laneHeight + LANE_GAP;

            drawLaneSurface(g2, width, playerLaneTop, laneHeight);
            drawLaneSurface(g2, width, enemyLaneTop, laneHeight);

            drawBase(g2, getPlayerBaseX(), playerLaneTop, laneHeight, new Color(66, 135, 245));
            drawBase(g2, getEnemyBaseX(), playerLaneTop, laneHeight, new Color(200, 70, 70));
            for (UnitInstance unit : enemyUnits) {
                drawUnit(g2, unit, playerLaneTop, laneHeight, new Color(214, 68, 68));
            }
            if (heroAlive) {
                drawHero(g2, (int) Math.round(heroX), playerLaneTop, laneHeight, playerHero.getName(),
                        playerHero.getCurrentHealth(), playerHero.getMaxHealth(), new Color(64, 144, 255));
            } else {
                drawRespawnIndicator(g2, (int) Math.round(heroX), playerLaneTop, laneHeight, heroRespawnTimer);
            }

            drawBase(g2, getPlayerBaseX(), enemyLaneTop, laneHeight, new Color(66, 135, 245));
            drawBase(g2, getEnemyBaseX(), enemyLaneTop, laneHeight, new Color(200, 70, 70));
            for (UnitInstance unit : playerUnits) {
                drawUnit(g2, unit, enemyLaneTop, laneHeight, new Color(64, 144, 255));
            }
            if (enemyAlive) {
                drawHero(g2, (int) Math.round(enemyX), enemyLaneTop, laneHeight, aiHero.getName(),
                        aiHero.getCurrentHealth(), aiHero.getMaxHealth(), new Color(214, 68, 68));
            } else {
                drawRespawnIndicator(g2, (int) Math.round(enemyX), enemyLaneTop, laneHeight, enemyRespawnTimer);
            }

            g2.dispose();
        }

        private void drawLaneSurface(Graphics2D g2, int width, int laneTop, int laneHeight) {
            g2.setColor(new Color(32, 46, 34));
            g2.fillRoundRect(0, laneTop, width, laneHeight, 30, 30);
            g2.setColor(new Color(28, 40, 30));
            for (int i = 0; i < width; i += 40) {
                g2.fillRect(i, laneTop + laneHeight / 2 - 2, 20, 4);
            }
        }

        private boolean isInPlayerLane(int y) {
            int laneTop = LANE_MARGIN;
            int laneHeight = calculateLaneHeight();
            return y >= laneTop && y <= laneTop + laneHeight;
        }

        private int calculateLaneHeight() {
            int height = getHeight();
            if (height <= 0) {
                height = getPreferredSize().height;
            }
            int available = height - 2 * LANE_MARGIN - LANE_GAP;
            if (available < 200) {
                available = 200;
            }
            return available / 2;
        }

        private void drawBase(Graphics2D g2, int baseX, int laneTop, int laneHeight, Color color) {
            int baseY = laneTop + 20;
            int baseHeight = laneHeight - 40;
            g2.setColor(color.darker());
            g2.fillRoundRect(baseX - 6, baseY - 6, BASE_WIDTH + 12, baseHeight + 12, 18, 18);
            g2.setColor(color);
            g2.fillRoundRect(baseX, baseY, BASE_WIDTH, baseHeight, 18, 18);

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
            g2.setColor(new Color(35, 35, 35, 200));
            g2.fillRoundRect(baseX, baseY - barHeight - 6, barWidth, barHeight, 8, 8);
            g2.setColor(new Color(70, 220, 90));
            g2.fillRoundRect(baseX, baseY - barHeight - 6, (int) Math.round(barWidth * ratio), barHeight, 8, 8);
        }

        private void drawHero(Graphics2D g2, int x, int laneTop, int laneHeight, String name, int currentHp, int maxHp, Color color) {
            int diameter = HERO_WIDTH;
            int centerY = laneTop + laneHeight / 2;
            int drawX = (int) Math.round(x);
            int drawY = centerY - diameter / 2;
            g2.setColor(color);
            g2.fillOval(drawX, drawY, diameter, diameter);
            g2.setColor(Color.BLACK);
            g2.drawOval(drawX, drawY, diameter, diameter);

            int barWidth = HERO_WIDTH + 10;
            int barHeight = 8;
            double ratio = Math.min(1.0, Math.max(0, currentHp) / (double) maxHp);
            int barX = drawX - (barWidth - HERO_WIDTH) / 2;
            int barY = drawY - barHeight - 6;
            g2.setColor(new Color(45, 45, 45));
            g2.fillRoundRect(barX, barY, barWidth, barHeight, 8, 8);
            g2.setColor(new Color(70, 220, 90));
            g2.fillRoundRect(barX, barY, (int) Math.round(barWidth * ratio), barHeight, 8, 8);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            g2.setColor(Color.WHITE);
            int textWidth = g2.getFontMetrics().stringWidth(name);
            g2.drawString(name, drawX + (HERO_WIDTH - textWidth) / 2, barY - 2);
        }

        private void drawUnit(Graphics2D g2, UnitInstance unit, int laneTop, int laneHeight, Color color) {
            int centerY = laneTop + laneHeight / 2;
            int drawX = (int) Math.round(unit.x);
            int drawY = centerY - UNIT_SIZE / 2;
            g2.setColor(unit.engaged ? color.darker() : color);
            g2.fillOval(drawX, drawY, UNIT_SIZE, UNIT_SIZE);
            g2.setColor(Color.BLACK);
            g2.drawOval(drawX, drawY, UNIT_SIZE, UNIT_SIZE);
            double ratio = Math.min(1.0, Math.max(0, unit.health) / (double) unit.type.getHealth());
            g2.setColor(new Color(45, 45, 45));
            g2.fillRoundRect(drawX, drawY - 8, UNIT_SIZE, 6, 6, 6);
            g2.setColor(new Color(80, 210, 100));
            g2.fillRoundRect(drawX, drawY - 8, (int) Math.round(UNIT_SIZE * ratio), 6, 6, 6);
        }

        private void drawRespawnIndicator(Graphics2D g2, int x, int laneTop, int laneHeight, int timer) {
            int diameter = HERO_WIDTH;
            int centerY = laneTop + laneHeight / 2;
            int drawX = (int) Math.round(x);
            int drawY = centerY - diameter / 2;
            g2.setColor(new Color(120, 120, 120, 150));
            g2.fillOval(drawX, drawY, diameter, diameter);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawOval(drawX, drawY, diameter, diameter);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.setColor(Color.WHITE);
            String text = "Respawn";
            int textWidth = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, drawX + (diameter - textWidth) / 2, drawY + diameter / 2 - 6);
            String countdown = String.format("%.1f s", Math.max(0, timer * TICK_MILLIS / 1000.0));
            int countdownWidth = g2.getFontMetrics().stringWidth(countdown);
            g2.drawString(countdown, drawX + (diameter - countdownWidth) / 2, drawY + diameter / 2 + 12);
        }
    }

    private void attemptSendUnit(UnitType type) {
        if (gameOver || playerHero == null) {
            return;
        }
        if (!playerHero.spendGold(type.getCost())) {
            lastActionMessage = String.format("Not enough gold to send a %s.", type.getDisplayName());
            refreshHud();
            battlefieldPanel.repaint();
            return;
        }
        playerHero.addIncome(type.getIncomeBonus());
        playerTeam.queueUnit(type);
        lastActionMessage = String.format("Queued a %s for the next wave.", type.getDisplayName());
        refreshHud();
        battlefieldPanel.repaint();
    }

    private void attemptAiSendUnit() {
        if (gameOver || aiHero == null) {
            return;
        }
        java.util.List<UnitType> affordable = new java.util.ArrayList<>();
        for (UnitType type : UnitType.values()) {
            if (aiHero.getGold() >= type.getCost()) {
                affordable.add(type);
            }
        }
        if (affordable.isEmpty()) {
            return;
        }
        UnitType choice = affordable.get(random.nextInt(affordable.size()));
        aiHero.spendGold(choice.getCost());
        aiHero.addIncome(choice.getIncomeBonus());
        enemyTeam.queueUnit(choice);
        battlefieldPanel.repaint();
    }

    private void launchNextWave() {
        if (gameOver || playerTeam == null || enemyTeam == null) {
            return;
        }
        java.util.List<UnitType> playerWave = playerTeam.drainQueuedUnits();
        java.util.List<UnitType> enemyWave = enemyTeam.drainQueuedUnits();

        for (UnitType type : playerWave) {
            playerUnits.add(new UnitInstance(type, getPlayerBaseX() + BASE_WIDTH + 8, true));
        }
        for (UnitType type : enemyWave) {
            enemyUnits.add(new UnitInstance(type, getEnemyBaseX() - UNIT_SIZE - 8, false));
        }

        if (playerWave.isEmpty() && enemyWave.isEmpty()) {
            lastActionMessage = "No reinforcements arrived this wave.";
        } else if (!playerWave.isEmpty() && !enemyWave.isEmpty()) {
            lastActionMessage = "Both teams unleashed reinforcements!";
        } else if (!playerWave.isEmpty()) {
            lastActionMessage = "Your units charge down the enemy lane!";
        } else {
            lastActionMessage = "Enemy forces surge onto your lane!";
        }
    }

    private void resolveHeroUnitCombat() {
        if (gameOver) {
            return;
        }

        if (heroAlive) {
            UnitInstance target = findUnitInRange(enemyUnits, heroX + HERO_WIDTH / 2.0, ATTACK_RANGE);
            if (target != null && heroAttackCooldown <= 0) {
                int damage = Math.max(1, playerHero.rollAttackDamage(random));
                target.takeDamage(damage);
                heroAttackCooldown = playerHero.getAttackDelayTicks(ATTACK_COOLDOWN_TICKS);
                if (target.isDead()) {
                    enemyUnits.remove(target);
                    playerHero.addGold(UNIT_KILL_REWARD);
                    lastActionMessage = String.format("%s defeated an enemy %s!", playerHero.getName(),
                            target.getType().getDisplayName());
                }
            }
        }

        if (enemyAlive) {
            UnitInstance target = findUnitInRange(playerUnits, enemyX + HERO_WIDTH / 2.0, ATTACK_RANGE);
            if (target != null && enemyAttackCooldown <= 0) {
                int damage = Math.max(1, aiHero.rollAttackDamage(random));
                target.takeDamage(damage);
                enemyAttackCooldown = aiHero.getAttackDelayTicks(ATTACK_COOLDOWN_TICKS);
                if (target.isDead()) {
                    playerUnits.remove(target);
                }
            }
        }

        if (heroAlive) {
            for (UnitInstance unit : enemyUnits) {
                if (unit.isInRange(heroX + HERO_WIDTH / 2.0)) {
                    unit.engage();
                    if (unit.tryAttackHero(playerHero)) {
                        lastActionMessage = String.format("%s was overwhelmed defending the lane!", playerHero.getName());
                        onPlayerHeroDefeated();
                        break;
                    }
                }
            }
        }

        if (enemyAlive) {
            for (UnitInstance unit : playerUnits) {
                if (unit.isInRange(enemyX + HERO_WIDTH / 2.0)) {
                    unit.engage();
                    if (unit.tryAttackHero(aiHero)) {
                        onEnemyHeroDefeated();
                        break;
                    }
                }
            }
        }

        enemyUnits.removeIf(UnitInstance::isDead);
        playerUnits.removeIf(UnitInstance::isDead);
    }

    private UnitInstance findUnitInRange(java.util.List<UnitInstance> units, double referenceX, int range) {
        UnitInstance nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (UnitInstance unit : units) {
            double distance = Math.abs(unit.getCenterX() - referenceX);
            if (distance <= range && distance < bestDistance) {
                bestDistance = distance;
                nearest = unit;
            }
        }
        return nearest;
    }

    private UnitInstance findNearestUnit(java.util.List<UnitInstance> units, double referenceX) {
        UnitInstance nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (UnitInstance unit : units) {
            double distance = Math.abs(unit.getCenterX() - referenceX);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = unit;
            }
        }
        return nearest;
    }

    private void updateUnits() {
        if (gameOver) {
            return;
        }
        for (UnitInstance unit : playerUnits) {
            unit.preUpdate();
            unit.advance(UNIT_SPEED, getEnemyBaseX() - UNIT_SIZE);
        }
        for (UnitInstance unit : enemyUnits) {
            unit.preUpdate();
            unit.advance(-UNIT_SPEED, getPlayerBaseX() + BASE_WIDTH);
        }

        for (UnitInstance playerUnit : playerUnits) {
            for (UnitInstance enemyUnit : enemyUnits) {
                double distance = Math.abs(playerUnit.getCenterX() - enemyUnit.getCenterX());
                boolean playerInRange = distance <= playerUnit.getRange();
                boolean enemyInRange = distance <= enemyUnit.getRange();
                if (!playerInRange && !enemyInRange) {
                    continue;
                }
                if (distance < UNIT_SIZE) {
                    double midpoint = (playerUnit.getCenterX() + enemyUnit.getCenterX()) / 2.0;
                    playerUnit.lockAt(midpoint - UNIT_SIZE);
                    enemyUnit.lockAt(midpoint);
                } else {
                    if (playerInRange) {
                        playerUnit.engage();
                    }
                    if (enemyInRange) {
                        enemyUnit.engage();
                    }
                }
                if (playerInRange) {
                    playerUnit.tryAttack(enemyUnit);
                }
                if (enemyInRange) {
                    enemyUnit.tryAttack(playerUnit);
                }
            }
        }

        java.util.Iterator<UnitInstance> iterator = playerUnits.iterator();
        while (iterator.hasNext()) {
            UnitInstance unit = iterator.next();
            if (unit.x + UNIT_SIZE >= getEnemyBaseX()) {
                unit.lockAt(getEnemyBaseX() - UNIT_SIZE);
                if (unit.tryAttackBase()) {
                    enemyBaseHealth = Math.max(0, enemyBaseHealth - unit.type.getDamage());
                    checkVictoryConditions();
                }
            }
            if (unit.isDead()) {
                iterator.remove();
            }
        }

        iterator = enemyUnits.iterator();
        while (iterator.hasNext()) {
            UnitInstance unit = iterator.next();
            if (unit.x <= getPlayerBaseX() + BASE_WIDTH) {
                unit.lockAt(getPlayerBaseX() + BASE_WIDTH);
                if (unit.tryAttackBase()) {
                    playerBaseHealth = Math.max(0, playerBaseHealth - unit.type.getDamage());
                    checkVictoryConditions();
                }
            }
            if (unit.isDead()) {
                iterator.remove();
            }
        }
    }

    private class UnitInstance {
        private final UnitType type;
        private double x;
        private int health;
        private int attackCooldown;
        private int baseAttackCooldown;
        private boolean engaged;
        private boolean engagedLastTick;
        private final boolean fromPlayer;

        UnitInstance(UnitType type, double x, boolean fromPlayer) {
            this.type = type;
            this.x = x;
            this.health = type.getHealth();
            this.attackCooldown = 0;
            this.baseAttackCooldown = 0;
            this.fromPlayer = fromPlayer;
        }

        void preUpdate() {
            engagedLastTick = engaged;
            engaged = false;
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            if (baseAttackCooldown > 0) {
                baseAttackCooldown--;
            }
        }

        void advance(double speed, double clampPosition) {
            if (engaged || engagedLastTick) {
                return;
            }
            x += speed;
            if (fromPlayer) {
                x = Math.min(x, clampPosition);
            } else {
                x = Math.max(x, clampPosition);
            }
        }

        void engage() {
            engaged = true;
        }

        void tryAttack(UnitInstance target) {
            if (attackCooldown <= 0 && isInRange(target)) {
                target.takeDamage(type.getDamage());
                attackCooldown = UNIT_ATTACK_COOLDOWN_TICKS;
            }
        }

        boolean tryAttackBase() {
            if (baseAttackCooldown <= 0) {
                baseAttackCooldown = UNIT_BASE_ATTACK_COOLDOWN_TICKS;
                return true;
            }
            return false;
        }

        boolean tryAttackHero(Hero hero) {
            if (attackCooldown <= 0) {
                int damage = Math.max(1, type.getDamage() - hero.getDefense());
                boolean defeated = hero.takeDamage(damage);
                attackCooldown = UNIT_ATTACK_COOLDOWN_TICKS;
                return defeated;
            }
            return false;
        }

        void lockAt(double newX) {
            x = newX;
            engaged = true;
            engagedLastTick = true;
        }

        void takeDamage(int amount) {
            health -= amount;
        }

        boolean isDead() {
            return health <= 0;
        }

        UnitType getType() {
            return type;
        }

        int getRange() {
            return type.getRange();
        }

        double getCenterX() {
            return x + UNIT_SIZE / 2.0;
        }

        boolean isInRange(UnitInstance other) {
            return Math.abs(getCenterX() - other.getCenterX()) <= getRange();
        }

        boolean isInRange(double targetX) {
            return Math.abs(getCenterX() - targetX) <= getRange();
        }
    }
}
