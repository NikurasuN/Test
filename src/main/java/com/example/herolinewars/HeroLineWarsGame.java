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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

/**
 * A lightweight real-time version of Hero Line Wars where the action happens on the lane.
 */
public class HeroLineWarsGame extends JFrame {
    private static final int PANEL_WIDTH = 900;
    private static final int PANEL_HEIGHT = 440;
    private static final int HERO_WIDTH = 48;
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
    private static final int HERO_VERTICAL_MARGIN = 14;
    private static final int UNIT_VERTICAL_MARGIN = 8;
    private static final int UNIT_SIZE = 28;
    private static final int UNIT_ATTACK_COOLDOWN_TICKS = 24;
    private static final int UNIT_BASE_ATTACK_COOLDOWN_TICKS = 30;
    private static final int UNIT_KILL_REWARD = 6;
    private static final int SPAWN_SHIELD_TICKS = 60;

    private static final Item[] SHOP_ITEMS = new Item[] {
            new Item("Sharpened Arrows", 6, 0, 85, "Lightweight arrowheads that increase ranged damage."),
            new Item("Bulwark Shield", 0, 6, 90, "Sturdy shield that absorbs blows."),
            new Item("War Banner", 4, 3, 110, "Rallying banner granting balanced power."),
            new Item("Arcane Tome", 9, 0, 140, "Magical tome that empowers offensive spells."),
            new Item("Guardian Armor", 0, 9, 150, "Heavy armor that keeps you standing longer."),
            new Item("Heroic Relic", 6, 6, 185, "Relic of old heroes granting all-around strength.")
    };

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
    private final JLabel queueLabel = new JLabel("Next Wave: None queued.");
    private final JLabel inventoryLabel = new JLabel("Inventory: None");

    private final BattlefieldPanel battlefieldPanel = new BattlefieldPanel();
    private Timer gameTimer;

    private double heroX;
    private double heroTargetX;
    private double heroY;
    private double heroTargetY;
    private double enemyX;
    private double enemyTargetX;
    private double enemyY;
    private double enemyTargetY;
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
    private boolean paused;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HeroLineWarsGame game = new HeroLineWarsGame();
            game.setVisible(true);
        });
    }

    private void openShopDialog() {
        if (playerHero == null) {
            return;
        }
        JDialog dialog = new JDialog(this, "Hero Item Shop", true);
        dialog.setLayout(new BorderLayout());

        JLabel goldLabel = new JLabel();
        goldLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel messageLabel = new JLabel("Select an item to empower your hero.");
        messageLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 10, 10));

        updateShopGoldLabel(goldLabel);

        JPanel itemsPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        for (Item item : SHOP_ITEMS) {
            JButton button = new JButton(formatItemLabel(item));
            button.setToolTipText(item.getDescription());
            button.addActionListener(e -> handleItemPurchase(item, goldLabel, messageLabel));
            itemsPanel.add(button);
        }

        dialog.add(goldLabel, BorderLayout.NORTH);
        dialog.add(new javax.swing.JScrollPane(itemsPanel), BorderLayout.CENTER);
        dialog.add(messageLabel, BorderLayout.SOUTH);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                battlefieldPanel.requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                battlefieldPanel.requestFocusInWindow();
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String formatItemLabel(Item item) {
        return String.format("%s - %dG (+%d ATK, +%d DEF)", item.getName(), item.getCost(), item.getAttackBonus(), item.getDefenseBonus());
    }

    private void updateShopGoldLabel(JLabel label) {
        label.setText(String.format("Current Gold: %d", playerHero.getGold()));
    }

    private void handleItemPurchase(Item item, JLabel goldLabel, JLabel messageLabel) {
        if (playerHero == null) {
            return;
        }
        if (!playerHero.spendGold(item.getCost())) {
            messageLabel.setText(String.format("Not enough gold for %s.", item.getName()));
            return;
        }
        playerHero.applyItem(item);
        lastActionMessage = String.format("Purchased %s from the shop!", item.getName());
        updateShopGoldLabel(goldLabel);
        updateInventoryLabel();
        refreshHud();
        battlefieldPanel.repaint();
        messageLabel.setText(String.format("%s equipped!", item.getName()));
    }

    private void openPauseMenu() {
        if (gameOver || gameTimer == null) {
            return;
        }
        if (paused) {
            resumeGame();
            return;
        }
        pauseGame();

        JDialog dialog = new JDialog(this, "Paused", true);
        dialog.setLayout(new BorderLayout());

        JLabel infoLabel = new JLabel("Game paused. Choose an option.", SwingConstants.CENTER);
        infoLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 10, 10, 10));
        dialog.add(infoLabel, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel(new GridLayout(0, 1, 8, 8));
        JButton resumeButton = new JButton("Resume");
        resumeButton.addActionListener(e -> {
            dialog.dispose();
            resumeGame();
        });
        JButton restartButton = new JButton("Restart Battle");
        restartButton.addActionListener(e -> {
            dialog.dispose();
            startBattle();
        });
        JButton exitButton = new JButton("Exit Game");
        exitButton.addActionListener(e -> {
            dialog.dispose();
            dispose();
        });
        buttonsPanel.add(resumeButton);
        buttonsPanel.add(restartButton);
        buttonsPanel.add(exitButton);
        buttonsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 16, 16, 16));

        dialog.add(buttonsPanel, BorderLayout.CENTER);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                resumeGame();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                battlefieldPanel.requestFocusInWindow();
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void pauseGame() {
        if (paused || gameOver) {
            return;
        }
        paused = true;
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }
        lastActionMessage = "Game paused.";
        refreshHud();
        battlefieldPanel.repaint();
    }

    private void resumeGame() {
        if (!paused || gameOver) {
            return;
        }
        paused = false;
        if (gameTimer != null) {
            gameTimer.start();
        }
        lastActionMessage = "Battle resumed!";
        refreshHud();
        battlefieldPanel.requestFocusInWindow();
        battlefieldPanel.repaint();
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
        statusPanel.add(queueLabel);
        statusPanel.add(inventoryLabel);
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

        JPanel utilityPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        JButton shopButton = new JButton("Open Shop");
        shopButton.addActionListener(e -> openShopDialog());
        JButton pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> openPauseMenu());
        utilityPanel.add(shopButton);
        utilityPanel.add(pauseButton);
        utilityPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 10, 10));
        commandPanel.add(utilityPanel, BorderLayout.SOUTH);

        add(commandPanel, BorderLayout.SOUTH);
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
            playerHero = new Hero("Ranger", 95, 16, 4, 180, 12);
            dialog.dispose();
            startBattle();
        });
        heroPanel.add(rangerButton);

        JButton knightButton = new JButton("Knight - Heavily armored and built to tank waves.");
        knightButton.addActionListener(e -> {
            playerHero = new Hero("Knight", 125, 12, 7, 200, 10);
            dialog.dispose();
            startBattle();
        });
        heroPanel.add(knightButton);

        JButton mageButton = new JButton("Battle Mage - Fragile but deals heavy attacks.");
        mageButton.addActionListener(e -> {
            playerHero = new Hero("Battle Mage", 80, 20, 3, 160, 14);
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
        paused = false;

        playerHero.resetHealth();
        aiHero.resetHealth();

        heroX = getPlayerSpawnX();
        heroTargetX = heroX;
        heroY = getPlayerSpawnY();
        heroTargetY = heroY;
        enemyX = getEnemySpawnX();
        enemyTargetX = enemyX;
        enemyY = getEnemySpawnY();
        enemyTargetY = enemyY;

        updateQueueLabel();
        updateInventoryLabel();
        refreshHud();
        battlefieldPanel.repaint();

        if (gameTimer != null) {
            gameTimer.stop();
        }
        gameTimer = new Timer(TICK_MILLIS, e -> updateGame());
        gameTimer.start();

        SwingUtilities.invokeLater(() -> battlefieldPanel.requestFocusInWindow());
    }

    private void updateGame() {
        if (gameOver) {
            return;
        }

        if (paused) {
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

        heroTargetX = clampHorizontalTarget(heroTargetX);
        heroTargetY = clampHeroVerticalTarget(heroTargetY);
        enemyTargetX = clampHorizontalTarget(enemyTargetX);
        enemyTargetY = clampEnemyVerticalTarget(enemyTargetY);

        if (heroAlive) {
            heroX = approach(heroX, heroTargetX, HERO_SPEED);
            heroY = approach(heroY, heroTargetY, HERO_SPEED);
        } else {
            if (heroRespawnTimer > 0) {
                heroRespawnTimer--;
            }
            if (heroRespawnTimer <= 0) {
                heroAlive = true;
                playerHero.resetHealth();
                heroX = getPlayerSpawnX();
                heroTargetX = heroX;
                heroY = getPlayerSpawnY();
                heroTargetY = heroY;
            }
        }

        if (enemyAlive) {
            UnitInstance threat = findNearestUnit(playerUnits, enemyX + HERO_WIDTH / 2.0, enemyY + HERO_WIDTH / 2.0);
            if (threat != null) {
                double desired = threat.getCenterX() - HERO_WIDTH / 2.0 - 18;
                enemyTargetX = clampHorizontalTarget(desired);
                enemyTargetY = clampEnemyVerticalTarget(threat.getCenterY() - HERO_WIDTH / 2.0);
            } else {
                enemyTargetX = clampHorizontalTarget(getEnemyBaseX() - HERO_WIDTH - 20);
                enemyTargetY = clampEnemyVerticalTarget(getEnemyLaneCenterY() - HERO_WIDTH / 2.0);
            }
            enemyX = approach(enemyX, enemyTargetX, ENEMY_SPEED);
            enemyY = approach(enemyY, enemyTargetY, ENEMY_SPEED);
        } else {
            if (enemyRespawnTimer > 0) {
                enemyRespawnTimer--;
            }
            if (enemyRespawnTimer <= 0) {
                enemyAlive = true;
                aiHero.resetHealth();
                enemyX = getEnemySpawnX();
                enemyTargetX = enemyX;
                enemyY = getEnemySpawnY();
                enemyTargetY = enemyY;
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
        double heroCenterX = heroX + HERO_WIDTH / 2.0;
        double heroCenterY = heroY + HERO_WIDTH / 2.0;
        double enemyCenterX = enemyX + HERO_WIDTH / 2.0;
        double enemyCenterY = enemyY + HERO_WIDTH / 2.0;
        double distance = distance(heroCenterX, heroCenterY, enemyCenterX, enemyCenterY);
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
        double heroCenterY = heroY + HERO_WIDTH / 2.0;
        double enemyCenterY = enemyY + HERO_WIDTH / 2.0;
        int enemyBaseTop = getEnemyBaseTop();
        int enemyBaseBottom = getEnemyBaseBottom();
        int playerBaseTop = getPlayerBaseTop();
        int playerBaseBottom = getPlayerBaseBottom();

        if (heroAlive && !enemyAlive) {
            if (heroBaseAttackCooldown <= 0 && heroX + HERO_WIDTH >= getEnemyBaseX()
                    && heroCenterY >= enemyBaseTop && heroCenterY <= enemyBaseBottom) {
                enemyBaseHealth = Math.max(0, enemyBaseHealth - Math.max(BASE_DAMAGE_PER_TICK, playerHero.getAttack() * 3));
                heroBaseAttackCooldown = BASE_ATTACK_COOLDOWN_TICKS;
                checkVictoryConditions();
            }
        }
        if (enemyAlive && !heroAlive) {
            if (enemyBaseAttackCooldown <= 0 && enemyX <= getPlayerBaseX() + BASE_WIDTH
                    && enemyCenterY >= playerBaseTop && enemyCenterY <= playerBaseBottom) {
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
        lastActionMessage = "You were defeated! The enemy presses the attack.";
    }

    private void onEnemyHeroDefeated() {
        enemyAlive = false;
        enemyRespawnTimer = RESPAWN_TICKS;
        playerKills++;
        enemyAttackCooldown = ATTACK_COOLDOWN_TICKS;
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
            return;
        }
        baseLabel.setText(String.format("Base HP - You: %d | Enemy: %d", Math.max(0, playerBaseHealth), Math.max(0, enemyBaseHealth)));
        heroLabel.setText(String.format("Hero: %s | HP %d/%d | ATK %d | DEF %d", playerHero.getName(),
                Math.max(0, playerHero.getCurrentHealth()), playerHero.getMaxHealth(), playerHero.getAttack(), playerHero.getDefense()));
        aiLabel.setText(String.format("Enemy Hero: %s | HP %d/%d | ATK %d | DEF %d", aiHero.getName(),
                Math.max(0, aiHero.getCurrentHealth()), aiHero.getMaxHealth(), aiHero.getAttack(), aiHero.getDefense()));
        killsLabel.setText(String.format("Kills - You: %d | Enemy: %d", playerKills, enemyKills));
        economyLabel.setText(String.format("Economy - Gold %d (+%d) | Enemy Gold %d (+%d)",
                playerHero.getGold(), playerHero.getIncome(), aiHero.getGold(), aiHero.getIncome()));
        updateQueueLabel();
        updateInventoryLabel();
        double seconds = Math.max(0, waveCountdown) * TICK_MILLIS / 1000.0;
        String statusMessage = paused ? "Game paused." : lastActionMessage;
        actionLabel.setText(String.format("%s Next wave in %.1f s.", statusMessage, seconds));
    }

    private void updateQueueLabel() {
        if (playerTeam == null) {
            queueLabel.setText("Next Wave: None queued.");
            return;
        }
        java.util.List<UnitType> queued = playerTeam.getQueuedUnitsSnapshot();
        if (queued.isEmpty()) {
            queueLabel.setText("Next Wave: None queued.");
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < queued.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(queued.get(i).getDisplayName());
        }
        queueLabel.setText("Next Wave: " + builder);
    }

    private void updateInventoryLabel() {
        if (playerHero == null) {
            inventoryLabel.setText("Inventory: None");
            return;
        }
        java.util.List<Item> items = playerHero.getInventory();
        if (items.isEmpty()) {
            inventoryLabel.setText("Inventory: None");
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(items.get(i).getName());
        }
        inventoryLabel.setText("Inventory: " + builder);
    }

    private Hero createAiHero() {
        int roll = random.nextInt(3);
        switch (roll) {
            case 0:
                return new Hero("Sentinel", 110, 14, 6, 180, 12);
            case 1:
                return new Hero("Berserker", 85, 19, 4, 170, 13);
            default:
                return new Hero("Warlock", 90, 17, 5, 190, 11);
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
        return getMovementLeftLimit() + 40;
    }

    private double getEnemySpawnX() {
        return getMovementLeftLimit() + 40;
    }

    private double getPlayerSpawnY() {
        return getPlayerLaneCenterY() - HERO_WIDTH / 2.0;
    }

    private double getEnemySpawnY() {
        return getEnemyLaneCenterY() - HERO_WIDTH / 2.0;
    }

    private double getMovementLeftLimit() {
        return Math.max(0, BASE_MARGIN - HERO_WIDTH - 12);
    }

    private double getMovementRightLimit() {
        int width = battlefieldPanel.getWidth();
        if (width <= 0) {
            width = battlefieldPanel.getPreferredSize().width;
        }
        double limit = width - BASE_MARGIN - HERO_WIDTH * 0.5;
        return Math.max(getMovementLeftLimit(), limit);
    }

    private int getLaneHeight() {
        int height = battlefieldPanel.getHeight();
        if (height <= 0) {
            height = battlefieldPanel.getPreferredSize().height;
        }
        int available = height - 2 * LANE_MARGIN - LANE_GAP;
        if (available < 200) {
            available = 200;
        }
        return available / 2;
    }

    private int getPlayerLaneTop() {
        return LANE_MARGIN;
    }

    private int getEnemyLaneTop() {
        return getPlayerLaneTop() + getLaneHeight() + LANE_GAP;
    }

    private double getPlayerLaneCenterY() {
        return getPlayerLaneTop() + getLaneHeight() / 2.0;
    }

    private double getEnemyLaneCenterY() {
        return getEnemyLaneTop() + getLaneHeight() / 2.0;
    }

    private double getPlayerHeroTopLimit() {
        return getPlayerLaneTop() + HERO_VERTICAL_MARGIN;
    }

    private double getPlayerHeroBottomLimit() {
        return getPlayerLaneTop() + getLaneHeight() - HERO_WIDTH - HERO_VERTICAL_MARGIN;
    }

    private double getEnemyHeroTopLimit() {
        return getEnemyLaneTop() + HERO_VERTICAL_MARGIN;
    }

    private double getEnemyHeroBottomLimit() {
        return getEnemyLaneTop() + getLaneHeight() - HERO_WIDTH - HERO_VERTICAL_MARGIN;
    }

    private int getPlayerBaseTop() {
        return getPlayerLaneTop() + 20;
    }

    private int getPlayerBaseBottom() {
        return getPlayerBaseTop() + getLaneHeight() - 40;
    }

    private int getEnemyBaseTop() {
        return getEnemyLaneTop() + 20;
    }

    private int getEnemyBaseBottom() {
        return getEnemyBaseTop() + getLaneHeight() - 40;
    }

    private double getPlayerBaseCenterY() {
        return getPlayerLaneTop() + getLaneHeight() / 2.0;
    }

    private double getEnemyBaseCenterY() {
        return getEnemyLaneTop() + getLaneHeight() / 2.0;
    }

    private double clampHorizontalTarget(double value) {
        return clamp(value, getMovementLeftLimit(), getMovementRightLimit());
    }

    private double clampHeroVerticalTarget(double value) {
        double min = getPlayerHeroTopLimit();
        double max = getPlayerHeroBottomLimit();
        if (max < min) {
            return min;
        }
        return clamp(value, min, max);
    }

    private double clampEnemyVerticalTarget(double value) {
        double min = getEnemyHeroTopLimit();
        double max = getEnemyHeroBottomLimit();
        if (max < min) {
            return min;
        }
        return clamp(value, min, max);
    }

    private void updateHeroTargetFromMouse(int mouseX, int mouseY) {
        if (!heroAlive || paused || gameOver) {
            return;
        }
        double targetX = mouseX - HERO_WIDTH / 2.0;
        double targetY = mouseY - HERO_WIDTH / 2.0;
        heroTargetX = clampHorizontalTarget(targetX);
        heroTargetY = clampHeroVerticalTarget(targetY);
    }

    private void adjustHeroTargetX(double delta) {
        if (!heroAlive || paused || gameOver) {
            return;
        }
        heroTargetX = clampHorizontalTarget(heroTargetX + delta);
    }

    private void adjustHeroTargetY(double delta) {
        if (!heroAlive || paused || gameOver) {
            return;
        }
        heroTargetY = clampHeroVerticalTarget(heroTargetY + delta);
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

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.hypot(dx, dy);
    }

    private class BattlefieldPanel extends JPanel {
        BattlefieldPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(new Color(20, 30, 22));
            setFocusable(true);

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                        handleClick(e);
                        requestFocusInWindow();
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                        handleDrag(e);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                        handleRelease();
                    }
                }
            };
            addMouseListener(adapter);
            addMouseMotionListener(adapter);
            setupKeyBindings();
        }

        private void handleClick(MouseEvent e) {
            if (!heroAlive || gameOver || paused) {
                return;
            }
            updateHeroTargetFromMouse(e.getX(), e.getY());
        }

        private void handleDrag(MouseEvent e) {
            if (!heroAlive || gameOver || paused) {
                return;
            }
            updateHeroTargetFromMouse(e.getX(), e.getY());
        }

        private void handleRelease() {
            // No-op for now but kept for clarity and future interactions.
        }

        private void setupKeyBindings() {
            javax.swing.InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            javax.swing.ActionMap actionMap = getActionMap();

            javax.swing.KeyStroke left = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, 0);
            javax.swing.KeyStroke leftArrow = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, 0);
            javax.swing.KeyStroke right = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, 0);
            javax.swing.KeyStroke rightArrow = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, 0);
            javax.swing.KeyStroke up = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, 0);
            javax.swing.KeyStroke upArrow = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, 0);
            javax.swing.KeyStroke down = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, 0);
            javax.swing.KeyStroke downArrow = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, 0);
            javax.swing.KeyStroke pauseKey = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
            javax.swing.KeyStroke pauseKeyAlt = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, 0);

            inputMap.put(left, "moveLeft");
            inputMap.put(leftArrow, "moveLeft");
            inputMap.put(right, "moveRight");
            inputMap.put(rightArrow, "moveRight");
            inputMap.put(up, "moveUp");
            inputMap.put(upArrow, "moveUp");
            inputMap.put(down, "moveDown");
            inputMap.put(downArrow, "moveDown");
            inputMap.put(pauseKey, "pause");
            inputMap.put(pauseKeyAlt, "pause");

            actionMap.put("moveLeft", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    adjustHeroTargetX(-60);
                }
            });
            actionMap.put("moveRight", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    adjustHeroTargetX(60);
                }
            });
            actionMap.put("moveUp", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    adjustHeroTargetY(-60);
                }
            });
            actionMap.put("moveDown", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    adjustHeroTargetY(60);
                }
            });
            actionMap.put("pause", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    openPauseMenu();
                }
            });
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
            int laneHeight = getLaneHeight();
            int playerLaneTop = getPlayerLaneTop();
            int enemyLaneTop = getEnemyLaneTop();

            drawLaneSurface(g2, width, playerLaneTop, laneHeight);
            drawLaneSurface(g2, width, enemyLaneTop, laneHeight);

            drawBase(g2, getPlayerBaseX(), playerLaneTop, laneHeight, new Color(66, 135, 245));
            drawBase(g2, getEnemyBaseX(), playerLaneTop, laneHeight, new Color(200, 70, 70));
            for (UnitInstance unit : enemyUnits) {
                drawUnit(g2, unit, new Color(214, 68, 68));
            }
            if (heroAlive) {
                int heroDrawX = (int) Math.round(heroX);
                int heroDrawY = (int) Math.round(heroY);
                drawHeroRange(g2, heroDrawX + HERO_WIDTH / 2, heroDrawY + HERO_WIDTH / 2,
                        new Color(64, 144, 255, 90));
                drawHero(g2, heroDrawX, heroDrawY, playerHero.getName(),
                        playerHero.getCurrentHealth(), playerHero.getMaxHealth(), new Color(64, 144, 255));
            } else {
                drawRespawnIndicator(g2, (int) Math.round(heroX), (int) Math.round(heroY), heroRespawnTimer);
            }

            drawBase(g2, getPlayerBaseX(), enemyLaneTop, laneHeight, new Color(66, 135, 245));
            drawBase(g2, getEnemyBaseX(), enemyLaneTop, laneHeight, new Color(200, 70, 70));
            for (UnitInstance unit : playerUnits) {
                drawUnit(g2, unit, new Color(64, 144, 255));
            }
            if (enemyAlive) {
                int enemyDrawX = (int) Math.round(enemyX);
                int enemyDrawY = (int) Math.round(enemyY);
                drawHeroRange(g2, enemyDrawX + HERO_WIDTH / 2, enemyDrawY + HERO_WIDTH / 2,
                        new Color(214, 68, 68, 90));
                drawHero(g2, enemyDrawX, enemyDrawY, aiHero.getName(),
                        aiHero.getCurrentHealth(), aiHero.getMaxHealth(), new Color(214, 68, 68));
            } else {
                drawRespawnIndicator(g2, (int) Math.round(enemyX), (int) Math.round(enemyY), enemyRespawnTimer);
            }

            drawQueuedUnitsOverlay(g2, width);
            if (paused) {
                drawPauseOverlay(g2, width, getHeight());
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
            int laneTop = getPlayerLaneTop();
            int laneHeight = getLaneHeight();
            return y >= laneTop && y <= laneTop + laneHeight;
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

        private void drawHero(Graphics2D g2, int x, int y, String name, int currentHp, int maxHp, Color color) {
            int diameter = HERO_WIDTH;
            int drawX = x;
            int drawY = y;
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

        private void drawUnit(Graphics2D g2, UnitInstance unit, Color color) {
            int drawX = (int) Math.round(unit.x);
            int drawY = (int) Math.round(unit.y);
            g2.setColor(unit.engaged ? color.darker() : color);
            g2.fillOval(drawX, drawY, UNIT_SIZE, UNIT_SIZE);
            g2.setColor(Color.BLACK);
            g2.drawOval(drawX, drawY, UNIT_SIZE, UNIT_SIZE);
            if (unit.hasSpawnShield()) {
                g2.setColor(new Color(180, 220, 255, 120));
                int shieldSize = UNIT_SIZE + 12;
                g2.drawOval(drawX - 6, drawY - 6, shieldSize, shieldSize);
            }
            double ratio = Math.min(1.0, Math.max(0, unit.health) / (double) unit.type.getHealth());
            g2.setColor(new Color(45, 45, 45));
            g2.fillRoundRect(drawX, drawY - 8, UNIT_SIZE, 6, 6, 6);
            g2.setColor(new Color(80, 210, 100));
            g2.fillRoundRect(drawX, drawY - 8, (int) Math.round(UNIT_SIZE * ratio), 6, 6, 6);
        }

        private void drawHeroRange(Graphics2D g2, int centerX, int centerY, Color color) {
            int diameter = ATTACK_RANGE * 2;
            g2.setColor(color);
            g2.fillOval(centerX - ATTACK_RANGE, centerY - ATTACK_RANGE, diameter, diameter);
            g2.setColor(color.darker());
            g2.drawOval(centerX - ATTACK_RANGE, centerY - ATTACK_RANGE, diameter, diameter);
        }

        private void drawQueuedUnitsOverlay(Graphics2D g2, int width) {
            if (playerTeam == null) {
                return;
            }
            java.util.List<UnitType> queued = playerTeam.getQueuedUnitsSnapshot();
            StringBuilder builder = new StringBuilder("Next Wave: ");
            if (queued.isEmpty()) {
                builder.append("None");
            } else {
                for (int i = 0; i < queued.size(); i++) {
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(queued.get(i).getDisplayName());
                }
            }
            String text = builder.toString();
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            int textWidth = g2.getFontMetrics().stringWidth(text);
            int boxWidth = textWidth + 20;
            int boxHeight = 30;
            g2.setColor(new Color(10, 10, 10, 160));
            g2.fillRoundRect(14, 14, boxWidth, boxHeight, 14, 14);
            g2.setColor(Color.WHITE);
            g2.drawString(text, 24, 33);
        }

        private void drawPauseOverlay(Graphics2D g2, int width, int height) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, width, height);
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 32f));
            String title = "Paused";
            int titleWidth = g2.getFontMetrics().stringWidth(title);
            int titleX = (width - titleWidth) / 2;
            int titleY = height / 2 - 10;
            g2.drawString(title, titleX, titleY);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
            String hint = "Use the pause menu or press Esc to resume.";
            int hintWidth = g2.getFontMetrics().stringWidth(hint);
            g2.drawString(hint, (width - hintWidth) / 2, titleY + 28);
        }

        private void drawRespawnIndicator(Graphics2D g2, int x, int y, int timer) {
            int diameter = HERO_WIDTH;
            int drawX = x;
            int drawY = y;
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
            playerUnits.add(createUnitInstance(type, true));
        }
        for (UnitType type : enemyWave) {
            enemyUnits.add(createUnitInstance(type, false));
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

    private UnitInstance createUnitInstance(UnitType type, boolean fromPlayer) {
        double spawnX = fromPlayer ? getPlayerBaseX() + BASE_WIDTH + 8 : getEnemyBaseX() - UNIT_SIZE - 8;
        double laneTop = fromPlayer ? getEnemyLaneTop() : getPlayerLaneTop();
        int laneHeight = getLaneHeight();
        double topLimit = laneTop + UNIT_VERTICAL_MARGIN;
        double bottomLimit = laneTop + laneHeight - UNIT_SIZE - UNIT_VERTICAL_MARGIN;
        if (bottomLimit < topLimit) {
            bottomLimit = topLimit;
        }
        double spawnY = topLimit + (bottomLimit - topLimit) / 2.0;
        return new UnitInstance(type, spawnX, spawnY, topLimit, bottomLimit, fromPlayer);
    }

    private void resolveHeroUnitCombat() {
        if (gameOver) {
            return;
        }

        double heroCenterX = heroX + HERO_WIDTH / 2.0;
        double heroCenterY = heroY + HERO_WIDTH / 2.0;
        double enemyCenterX = enemyX + HERO_WIDTH / 2.0;
        double enemyCenterY = enemyY + HERO_WIDTH / 2.0;

        if (heroAlive) {
            UnitInstance target = findUnitInRange(enemyUnits, heroCenterX, heroCenterY, ATTACK_RANGE);
            if (target != null && heroAttackCooldown <= 0) {
                target.takeDamage(Math.max(1, playerHero.getAttack()));
                heroAttackCooldown = ATTACK_COOLDOWN_TICKS;
                if (target.isDead()) {
                    enemyUnits.remove(target);
                    playerHero.addGold(UNIT_KILL_REWARD);
                    lastActionMessage = String.format("%s defeated an enemy %s!", playerHero.getName(),
                            target.getType().getDisplayName());
                }
            }
        }

        if (enemyAlive) {
            UnitInstance target = findUnitInRange(playerUnits, enemyCenterX, enemyCenterY, ATTACK_RANGE);
            if (target != null && enemyAttackCooldown <= 0) {
                target.takeDamage(Math.max(1, aiHero.getAttack()));
                enemyAttackCooldown = ATTACK_COOLDOWN_TICKS;
                if (target.isDead()) {
                    playerUnits.remove(target);
                }
            }
        }

        if (heroAlive) {
            for (UnitInstance unit : enemyUnits) {
                if (unit.isInRange(heroCenterX, heroCenterY)) {
                    unit.engage();
                    unit.setTargetCenterY(heroCenterY);
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
                if (unit.isInRange(enemyCenterX, enemyCenterY)) {
                    unit.engage();
                    unit.setTargetCenterY(enemyCenterY);
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

    private UnitInstance findUnitInRange(java.util.List<UnitInstance> units, double referenceX, double referenceY, int range) {
        UnitInstance nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (UnitInstance unit : units) {
            double dist = distance(unit.getCenterX(), unit.getCenterY(), referenceX, referenceY);
            if (dist <= range && dist < bestDistance) {
                bestDistance = dist;
                nearest = unit;
            }
        }
        return nearest;
    }

    private UnitInstance findNearestUnit(java.util.List<UnitInstance> units, double referenceX, double referenceY) {
        UnitInstance nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (UnitInstance unit : units) {
            double dist = distance(unit.getCenterX(), unit.getCenterY(), referenceX, referenceY);
            if (dist < bestDistance) {
                bestDistance = dist;
                nearest = unit;
            }
        }
        return nearest;
    }

    private void updateUnits() {
        if (gameOver) {
            return;
        }
        int laneHeight = getLaneHeight();
        double playerUnitTop = getEnemyLaneTop() + UNIT_VERTICAL_MARGIN;
        double playerUnitBottom = getEnemyLaneTop() + laneHeight - UNIT_SIZE - UNIT_VERTICAL_MARGIN;
        double enemyUnitTop = getPlayerLaneTop() + UNIT_VERTICAL_MARGIN;
        double enemyUnitBottom = getPlayerLaneTop() + laneHeight - UNIT_SIZE - UNIT_VERTICAL_MARGIN;
        if (playerUnitBottom < playerUnitTop) {
            playerUnitBottom = playerUnitTop;
        }
        if (enemyUnitBottom < enemyUnitTop) {
            enemyUnitBottom = enemyUnitTop;
        }

        for (UnitInstance unit : playerUnits) {
            unit.updateLaneBounds(playerUnitTop, playerUnitBottom);
            unit.preUpdate();
            unit.advance(UNIT_SPEED, getEnemyBaseX() - UNIT_SIZE);
        }
        for (UnitInstance unit : enemyUnits) {
            unit.updateLaneBounds(enemyUnitTop, enemyUnitBottom);
            unit.preUpdate();
            unit.advance(-UNIT_SPEED, getPlayerBaseX() + BASE_WIDTH);
        }

        for (UnitInstance playerUnit : playerUnits) {
            for (UnitInstance enemyUnit : enemyUnits) {
                double separation = distance(playerUnit.getCenterX(), playerUnit.getCenterY(), enemyUnit.getCenterX(),
                        enemyUnit.getCenterY());
                boolean playerInRange = separation <= playerUnit.getRange();
                boolean enemyInRange = separation <= enemyUnit.getRange();
                if (!playerInRange && !enemyInRange) {
                    continue;
                }
                if (separation < UNIT_SIZE) {
                    double midpoint = (playerUnit.getCenterX() + enemyUnit.getCenterX()) / 2.0;
                    playerUnit.lockAt(midpoint - UNIT_SIZE);
                    enemyUnit.lockAt(midpoint);
                    playerUnit.setTargetCenterY(enemyUnit.getCenterY());
                    enemyUnit.setTargetCenterY(playerUnit.getCenterY());
                } else {
                    if (playerInRange) {
                        playerUnit.engage();
                        playerUnit.setTargetCenterY(enemyUnit.getCenterY());
                    }
                    if (enemyInRange) {
                        enemyUnit.engage();
                        enemyUnit.setTargetCenterY(playerUnit.getCenterY());
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
                unit.setTargetCenterY(getEnemyBaseCenterY());
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
                unit.setTargetCenterY(getPlayerBaseCenterY());
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
        private int spawnShield;
        private double y;
        private double targetY;
        private double topLimit;
        private double bottomLimit;
        private double laneCenter;

        UnitInstance(UnitType type, double x, double y, double topLimit, double bottomLimit, boolean fromPlayer) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.health = type.getHealth();
            this.attackCooldown = 0;
            this.baseAttackCooldown = 0;
            this.fromPlayer = fromPlayer;
            this.spawnShield = SPAWN_SHIELD_TICKS;
            updateLaneBounds(topLimit, bottomLimit);
            this.targetY = clamp(y, this.topLimit, this.bottomLimit);
        }

        void preUpdate() {
            engagedLastTick = engaged;
            engaged = false;
            y = clamp(y, topLimit, bottomLimit);
            targetY = clamp(targetY, topLimit, bottomLimit);
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            if (baseAttackCooldown > 0) {
                baseAttackCooldown--;
            }
            if (spawnShield > 0) {
                spawnShield--;
            }
            if (!engagedLastTick) {
                targetY = laneCenter;
            }
        }

        void advance(double speed, double clampPosition) {
            double verticalSpeed = Math.max(1.2, Math.abs(speed));
            y = clamp(approach(y, targetY, verticalSpeed), topLimit, bottomLimit);
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

        void updateLaneBounds(double newTopLimit, double newBottomLimit) {
            if (newBottomLimit < newTopLimit) {
                double temp = newTopLimit;
                newTopLimit = newBottomLimit;
                newBottomLimit = temp;
            }
            this.topLimit = newTopLimit;
            this.bottomLimit = newBottomLimit;
            this.laneCenter = this.topLimit + (this.bottomLimit - this.topLimit) / 2.0;
            this.y = clamp(this.y, this.topLimit, this.bottomLimit);
            this.targetY = clamp(this.targetY, this.topLimit, this.bottomLimit);
        }

        void takeDamage(int amount) {
            if (spawnShield > 0) {
                return;
            }
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

        double getCenterY() {
            return y + UNIT_SIZE / 2.0;
        }

        void setTargetCenterY(double centerY) {
            targetY = clamp(centerY - UNIT_SIZE / 2.0, topLimit, bottomLimit);
        }

        boolean isInRange(UnitInstance other) {
            return distance(getCenterX(), getCenterY(), other.getCenterX(), other.getCenterY()) <= getRange();
        }

        boolean isInRange(double targetX, double targetY) {
            return distance(getCenterX(), getCenterY(), targetX, targetY) <= getRange();
        }

        boolean hasSpawnShield() {
            return spawnShield > 0;
        }
    }
}
