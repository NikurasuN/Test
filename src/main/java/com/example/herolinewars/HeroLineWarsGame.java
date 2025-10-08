package com.example.herolinewars;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A lightweight real-time version of Hero Line Wars where the action happens on the lane.
 */
public class HeroLineWarsGame extends JFrame {
    private enum GameMode {
        SOLO,
        LOCAL_VERSUS
    }
    private static final int PANEL_WIDTH = 1440;
    private static final int PANEL_HEIGHT = 840;
    private static final int WORLD_WIDTH = 2400;
    private static final int WORLD_HEIGHT = 1560;
    private static final int HERO_WIDTH = 36;
    private static final int BASE_WIDTH = 80;
    private static final int BASE_MARGIN = 32;
    private static final int LANE_MARGIN = 30;
    private static final int LANE_GAP = 80;
    private static final int LANES_PER_SIDE = 2;
    private static final int INTRA_LANE_GAP = 24;
    private static final int LANE_CONNECTOR_WIDTH = 140;
    private static final int LANE_SWITCH_ZONE_WIDTH = 200;
    private static final double HERO_SPEED = 4.5;
    private static final double ENEMY_SPEED = 3.4;
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    private static final int PORTAL_ATTACK_COOLDOWN_TICKS = 28;
    private static final int RESPAWN_TICKS = 120;
    private static final int TICK_MILLIS = 30;
    private static final int INCOME_INTERVAL_TICKS = 120;
    private static final int WAVE_INTERVAL_TICKS = 240;
    private static final double UNIT_SPEED = 2.6;
    private static final int HERO_VERTICAL_MARGIN = 10;
    private static final int UNIT_VERTICAL_MARGIN = 6;
    private static final int UNIT_SIZE = 22;
    private static final int ATTRIBUTE_UPGRADE_COST = 120;
    private static final int UNIT_ATTACK_COOLDOWN_TICKS = 24;
    private static final int UNIT_KILL_REWARD = 6;
    private static final int SPAWN_SHIELD_TICKS = 60;
    private static final int EXPERIENCE_PER_UNIT_KILL = 18;
    private static final int EXPERIENCE_PER_HERO_KILL = 150;
    private static final int PORTAL_BREACH_THRESHOLD = 12;
    private static final double CAMERA_PAN_DELTA = 120;
    private static final double CAMERA_ZOOM_STEP = 0.15;
    private static final double CAMERA_MIN_ZOOM = 0.6;
    private static final double CAMERA_MAX_ZOOM = 1.8;
    private static final boolean LOCK_HERO_TO_LANE = true;
    private static final boolean LOCK_ENEMY_TO_LANE = true;

    private static final Item[] SHOP_ITEMS = new Item[] {
            new Item("Sharpened Arrows", 6, 0, 85,
                    "Lightweight arrowheads that increase ranged damage.", null, 0, 0, 0),
            new Item("Bulwark Shield", 0, 6, 90,
                    "Sturdy shield that absorbs blows.", Item.EquipmentSlot.SHIELD, 1, 0, 0),
            new Item("War Banner", 4, 3, 110,
                    "Rallying banner granting balanced power.", Item.EquipmentSlot.ACCESSORY, 1, 1, 0),
            new Item("Arcane Tome", 9, 0, 140,
                    "Magical tome that empowers offensive spells.", Item.EquipmentSlot.WEAPON, 0, 0, 2),
            new Item("Guardian Armor", 0, 9, 150,
                    "Heavy armor that keeps you standing longer.", Item.EquipmentSlot.CHESTPLATE, 1, 0, 0),
            new Item("Heroic Relic", 6, 6, 185,
                    "Relic of old heroes granting all-around strength.", Item.EquipmentSlot.ACCESSORY, 2, 2, 2),
            new Item("Steel Helm", 0, 4, 160,
                    "Fortified helmet that hardens resolve.", Item.EquipmentSlot.HELMET, 2, 0, 0),
            new Item("Knight's Chestplate", 0, 7, 210,
                    "Immovable armor that protects the torso.", Item.EquipmentSlot.CHESTPLATE, 3, 0, 0),
            new Item("Ring of Fortitude", 0, 0, 125,
                    "Enchanted band that bolsters physical might.", Item.EquipmentSlot.RING, 3, 0, 0),
            new Item("Ring of Swiftness", 0, 0, 125,
                    "A nimble ring that heightens agility.", Item.EquipmentSlot.RING, 0, 3, 0),
            new Item("Ring of Insight", 0, 0, 125,
                    "A crystalline ring that sharpens arcane focus.", Item.EquipmentSlot.RING, 0, 0, 3)
    };

    private final Random random = new Random();
    private final EnumMap<UnitType, UnitBalance> unitBalances = new EnumMap<>(UnitType.class);
    private final EnumMap<UnitType, UnitBalance> defaultUnitBalances = new EnumMap<>(UnitType.class);
    private final EnumMap<UnitType, JButton> unitButtons = new EnumMap<>(UnitType.class);

    private Hero playerHero;
    private Hero aiHero;
    private Hero secondPlayerSelection;
    private GameMode gameMode = GameMode.SOLO;
    private Team playerTeam;
    private Team enemyTeam;

    private final JLabel modeLabel = new JLabel("Hero Line Wars - Live Battle");
    private final JLabel baseLabel = new JLabel();
    private final JLabel heroLabel = new JLabel();
    private final JLabel aiLabel = new JLabel();
    private final JLabel killsLabel = new JLabel();
    private final JLabel economyLabel = new JLabel();
    private final JLabel actionLabel = new JLabel("Ready to launch units down the lane.");
    private final JLabel controlsHintLabel = new JLabel();
    private final JLabel queueLabel = new JLabel("Next Wave: None queued.");
    private final JLabel inventoryLabel = new JLabel("Inventory: None");
    private final JLabel heroSummaryLabel = new JLabel("Hero interface locked until a hero is chosen.");
    private final JLabel heroAttributesLabel = new JLabel();
    private final JLabel heroCombatLabel = new JLabel();
    private final JLabel heroProgressLabel = new JLabel();
    private final JLabel heroResourceLabel = new JLabel();
    private final JLabel upgradeSummaryLabel = new JLabel("Invest gold to train attributes.");
    private final JButton upgradeStrengthButton = new JButton();
    private final JButton upgradeDexterityButton = new JButton();
    private final JButton upgradeIntelligenceButton = new JButton();
    private final javax.swing.JComboBox<String> playerSelector = new javax.swing.JComboBox<>();

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
    private int playerPortalBreaches;
    private int enemyPortalBreaches;
    private int playerKills;
    private int enemyKills;
    private boolean gameOver;
    private int incomeTickTimer;
    private int aiSendTimer;
    private int waveCountdown;
    private String lastActionMessage = "Ready to launch units down the lane.";
    private final java.util.List<UnitInstance> playerUnits = new java.util.ArrayList<>();
    private final java.util.List<UnitInstance> enemyUnits = new java.util.ArrayList<>();
    private final java.util.List<Projectile> projectiles = new java.util.ArrayList<>();
    private boolean paused;
    private int nextPlayerLaneIndex;
    private int nextEnemyLaneIndex;
    private int heroLaneIndex;
    private int enemyLaneIndex;
    private boolean enemyIsHuman;
    private int activeHeroIndex;
    private double cameraX;
    private double cameraY;
    private double cameraZoom = 1.0;

    private static final int PLAYER_DEFAULT_LANE = 0;
    private static final int ENEMY_DEFAULT_LANE = LANES_PER_SIDE - 1;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HeroLineWarsGame game = new HeroLineWarsGame();
            game.setVisible(true);
        });
    }

    private void openShopDialog(Hero targetHero) {
        if (targetHero == null) {
            return;
        }
        JDialog dialog = new JDialog(this, "Hero Item Shop", true);
        dialog.setLayout(new BorderLayout());

        JLabel goldLabel = new JLabel();
        goldLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel messageLabel = new JLabel("Select an item to empower your hero.");
        messageLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 10, 10));

        updateShopGoldLabel(goldLabel, targetHero);

        JPanel itemsPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        for (Item item : SHOP_ITEMS) {
            JButton button = new JButton(formatItemLabel(item));
            button.setToolTipText(item.getDescription());
            button.setIcon(IconLibrary.createItemGlyph(item));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setHorizontalTextPosition(SwingConstants.RIGHT);
            button.setIconTextGap(10);
            button.addActionListener(e -> handleItemPurchase(item, goldLabel, messageLabel, targetHero));
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

    private void openInventoryDialog(Hero targetHero) {
        if (targetHero == null) {
            return;
        }
        JDialog dialog = new JDialog(this, String.format("%s Inventory", targetHero.getName()), true);
        dialog.setLayout(new BorderLayout());

        JLabel header = new JLabel(String.format("%s's Equipment", targetHero.getName()), SwingConstants.CENTER);
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));
        dialog.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        JPanel equipmentGrid = new JPanel(new GridLayout(0, 2, 8, 6));
        equipmentGrid.setBorder(javax.swing.BorderFactory.createTitledBorder("Equipped Gear"));

        java.util.Map<Item.EquipmentSlot, java.util.List<Item>> equipped = targetHero.getEquippedItemsBySlot();
        for (Item.EquipmentSlot slot : Item.EquipmentSlot.values()) {
            StringBuilder slotLabel = new StringBuilder(slot.getDisplayName());
            if (slot == Item.EquipmentSlot.RING) {
                slotLabel.append(String.format(" (%d/%d)", targetHero.getEquippedCount(Item.EquipmentSlot.RING),
                        targetHero.getMaxRings()));
            }
            JLabel slotName = new JLabel(slotLabel + ":");
            slotName.setIcon(IconLibrary.createItemSlotGlyph(slot));
            slotName.setIconTextGap(6);
            slotName.setHorizontalTextPosition(SwingConstants.RIGHT);
            equipmentGrid.add(slotName);
            java.util.List<Item> slotItems = equipped.get(slot);
            String text;
            if (slotItems == null || slotItems.isEmpty()) {
                text = "Empty";
            } else {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < slotItems.size(); i++) {
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(slotItems.get(i).getName());
                }
                text = builder.toString();
            }
            equipmentGrid.add(new JLabel(text));
        }
        content.add(equipmentGrid, BorderLayout.NORTH);

        java.util.List<Item> slotless = new java.util.ArrayList<>();
        for (Item item : targetHero.getInventory()) {
            if (item.getSlot() == null) {
                slotless.add(item);
            }
        }
        if (!slotless.isEmpty()) {
            JPanel relicPanel = new JPanel(new BorderLayout());
            relicPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Relics"));
            JTextArea relicArea = new JTextArea();
            relicArea.setEditable(false);
            relicArea.setLineWrap(true);
            relicArea.setWrapStyleWord(true);
            StringBuilder builder = new StringBuilder();
            for (Item item : slotless) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(item.getName()).append(" - ").append(item.getDescription());
            }
            relicArea.setText(builder.toString());
            relicArea.setBackground(new Color(248, 248, 248));
            relicPanel.add(new javax.swing.JScrollPane(relicArea), BorderLayout.CENTER);
            content.add(relicPanel, BorderLayout.CENTER);
        }

        dialog.add(new javax.swing.JScrollPane(content), BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel footer = new JPanel();
        footer.add(closeButton);
        dialog.add(footer, BorderLayout.SOUTH);

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

        dialog.setSize(new Dimension(420, 420));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String formatItemLabel(Item item) {
        java.util.List<String> stats = new java.util.ArrayList<>();
        if (item.getAttackBonus() != 0) {
            stats.add(String.format("+%d ATK", item.getAttackBonus()));
        }
        if (item.getDefenseBonus() != 0) {
            stats.add(String.format("+%d DEF", item.getDefenseBonus()));
        }
        if (item.getStrengthBonus() != 0) {
            stats.add(String.format("+%d STR", item.getStrengthBonus()));
        }
        if (item.getDexterityBonus() != 0) {
            stats.add(String.format("+%d DEX", item.getDexterityBonus()));
        }
        if (item.getIntelligenceBonus() != 0) {
            stats.add(String.format("+%d INT", item.getIntelligenceBonus()));
        }
        String statsText = stats.isEmpty() ? "No bonuses" : String.join(", ", stats);
        String slotText = item.getSlot() != null ? String.format(" [%s]", item.getSlot().getDisplayName()) : "";
        return String.format("%s - %dG (%s)%s", item.getName(), item.getCost(), statsText, slotText);
    }

    private void updateShopGoldLabel(JLabel label, Hero hero) {
        if (hero == null) {
            label.setText("Current Gold: 0");
            return;
        }
        label.setText(String.format("Current Gold: %d", hero.getGold()));
    }

    private void handleItemPurchase(Item item, JLabel goldLabel, JLabel messageLabel, Hero hero) {
        if (hero == null) {
            return;
        }
        Item.EquipmentSlot slot = item.getSlot();
        if (slot == Item.EquipmentSlot.RING
                && hero.getEquippedCount(Item.EquipmentSlot.RING) >= hero.getMaxRings()) {
            messageLabel.setText(String.format("Ring slots are full (%d/%d).",
                    hero.getEquippedCount(Item.EquipmentSlot.RING), hero.getMaxRings()));
            lastActionMessage = "Ring slots are already filled.";
            refreshHud();
            battlefieldPanel.repaint();
            return;
        }
        Item previous = hero.getEquippedItem(slot);

        if (!hero.spendGold(item.getCost())) {
            messageLabel.setText(String.format("Not enough gold for %s.", item.getName()));
            return;
        }
        if (!hero.applyItem(item)) {
            // Defensive guard: if equipping fails, refund the purchase.
            hero.addGold(item.getCost());
            messageLabel.setText(String.format("Unable to equip %s.", item.getName()));
            return;
        }
        if (previous != null && slot != null && slot.isUnique()) {
            lastActionMessage = String.format("Replaced %s with %s.", previous.getName(), item.getName());
            messageLabel.setText(String.format("%s replaced %s.", item.getName(), previous.getName()));
        } else {
            lastActionMessage = String.format("Purchased %s from the shop!", item.getName());
            messageLabel.setText(String.format("%s equipped!", item.getName()));
        }
        updateShopGoldLabel(goldLabel, hero);
        updateInventoryLabel();
        refreshHud();
        battlefieldPanel.repaint();
    }

    private void openPauseMenu() {
        boolean battleEnded = gameOver;
        if (gameTimer == null && !battleEnded) {
            return;
        }
        if (!battleEnded) {
            if (paused) {
                resumeGame();
                return;
            }
            pauseGame();
        }

        String title = battleEnded ? "Battle Menu" : "Paused";
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new BorderLayout());

        String infoText = battleEnded ? "The battle has concluded. Choose what to do next." :
                "Game paused. Choose an option.";
        JLabel infoLabel = new JLabel(infoText, SwingConstants.CENTER);
        infoLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 10, 10, 10));
        dialog.add(infoLabel, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel(new GridLayout(0, 1, 8, 8));
        JButton restartButton = new JButton("Restart Battle");
        restartButton.addActionListener(e -> {
            dialog.dispose();
            restartToHeroSelection();
        });
        JButton exitButton = new JButton("Exit Game");
        exitButton.addActionListener(e -> {
            dialog.dispose();
            dispose();
        });
        JButton keybindsButton = new JButton("Keybinds & Controls");
        keybindsButton.addActionListener(e -> openKeybindsDialog());
        JButton developerButton = new JButton("Developer Settings");
        developerButton.addActionListener(e -> openDeveloperSettingsDialog());
        if (!battleEnded) {
            JButton resumeButton = new JButton("Resume");
            resumeButton.addActionListener(e -> {
                dialog.dispose();
                resumeGame();
            });
            buttonsPanel.add(resumeButton);
        }
        buttonsPanel.add(keybindsButton);
        buttonsPanel.add(developerButton);
        buttonsPanel.add(restartButton);
        buttonsPanel.add(exitButton);
        buttonsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 16, 16, 16));

        dialog.add(buttonsPanel, BorderLayout.CENTER);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!battleEnded) {
                    resumeGame();
                }
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

    private void openKeybindsDialog() {
        JDialog dialog = new JDialog(this, "Controls & Keybinds", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());

        JLabel header = new JLabel("Review the default controls for solo and versus play.", SwingConstants.CENTER);
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 16, 8, 16));
        dialog.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16));

        content.add(createKeybindSection("General", new String[][] {
                {"Pause / Menu", "Esc or P"},
                {"Center Camera", "C"},
                {"Zoom In", "+ / = or Mouse Wheel Up"},
                {"Zoom Out", "- or Mouse Wheel Down"}
        }));
        content.add(javax.swing.Box.createVerticalStrut(12));
        content.add(createKeybindSection("Solo Play", new String[][] {
                {"Hero Movement", "W / A / S / D"},
                {"Camera Pan", "Arrow Keys or Q / E / R / F"}
        }));
        content.add(javax.swing.Box.createVerticalStrut(12));
        content.add(createKeybindSection("Local Versus", new String[][] {
                {"Player 1 Hero", "W / A / S / D"},
                {"Player 2 Hero", "Arrow Keys"},
                {"Camera Pan", "Q / E / R / F or Shift + Arrow Keys"}
        }));
        content.add(javax.swing.Box.createVerticalStrut(12));
        content.add(createKeybindSection("Mouse", new String[][] {
                {"Issue Hero Move", "Left Click or Drag"},
                {"Issue Enemy Move", "Right Click or Drag (Versus)"},
                {"Zoom", "Mouse Wheel"}
        }));

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel footer = new JPanel();
        footer.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 16, 12, 16));
        footer.add(closeButton);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                battlefieldPanel.requestFocusInWindow();
            }
        });

        dialog.pack();
        if (dialog.getWidth() < 420) {
            dialog.setSize(new Dimension(420, dialog.getHeight()));
        }
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openDeveloperSettingsDialog() {
        JDialog dialog = new JDialog(this, "Developer Settings", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());

        JLabel header = new JLabel("Tweak gameplay values to iterate quickly.", SwingConstants.CENTER);
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 16, 8, 16));
        dialog.add(header, BorderLayout.NORTH);

        List<UnitBalanceEditor> unitEditors = new ArrayList<>();
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Units", createUnitBalanceTab(unitEditors));
        dialog.add(tabs, BorderLayout.CENTER);

        JButton applyButton = new JButton("Apply Changes");
        applyButton.addActionListener(e -> {
            for (UnitBalanceEditor editor : unitEditors) {
                editor.applyTo(getUnitBalance(editor.getType()));
            }
            onUnitBalanceUpdated("Developer unit tuning applied.");
        });

        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.addActionListener(e -> {
            for (UnitBalanceEditor editor : unitEditors) {
                UnitBalance defaults = defaultUnitBalances.get(editor.getType());
                if (defaults != null) {
                    editor.loadFrom(defaults);
                    editor.applyTo(getUnitBalance(editor.getType()));
                }
            }
            onUnitBalanceUpdated("Unit stats reset to defaults.");
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 8));
        footer.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 12, 12));
        footer.add(resetButton);
        footer.add(applyButton);
        footer.add(closeButton);
        dialog.add(footer, BorderLayout.SOUTH);

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
        if (dialog.getWidth() < 560 || dialog.getHeight() < 520) {
            dialog.setSize(Math.max(560, dialog.getWidth()), Math.max(520, dialog.getHeight()));
        }
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JScrollPane createUnitBalanceTab(List<UnitBalanceEditor> editors) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 12, 10, 12));

        for (UnitType type : UnitType.values()) {
            UnitBalanceEditor editor = new UnitBalanceEditor(type);
            editor.loadFrom(getUnitBalance(type));
            editors.add(editor);
            panel.add(editor.getPanel());
            panel.add(javax.swing.Box.createVerticalStrut(8));
        }
        panel.add(javax.swing.Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private void onUnitBalanceUpdated(String message) {
        refreshUnitButtons();
        clampExistingUnitsToBalance();
        lastActionMessage = message;
        refreshHud();
        battlefieldPanel.repaint();
    }

    private void clampExistingUnitsToBalance() {
        for (UnitInstance unit : playerUnits) {
            unit.clampHealthToBalance();
        }
        for (UnitInstance unit : enemyUnits) {
            unit.clampHealthToBalance();
        }
    }

    private JPanel createKeybindSection(String title, String[][] entries) {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize() + 1f));
        titleLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 4, 0));
        container.add(titleLabel, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(entries.length, 2, 12, 6));
        grid.setOpaque(false);
        for (String[] entry : entries) {
            JLabel action = new JLabel(entry[0]);
            action.setFont(action.getFont().deriveFont(Font.BOLD));
            grid.add(action);
            JLabel binding = new JLabel(entry[1]);
            grid.add(binding);
        }

        container.add(grid, BorderLayout.CENTER);
        return container;
    }

    private void restartToHeroSelection() {
        if (gameTimer != null) {
            gameTimer.stop();
            gameTimer = null;
        }
        paused = false;
        gameOver = false;
        heroAlive = false;
        enemyAlive = false;
        heroRespawnTimer = 0;
        enemyRespawnTimer = 0;
        heroAttackCooldown = 0;
        enemyAttackCooldown = 0;
        heroBaseAttackCooldown = 0;
        enemyBaseAttackCooldown = 0;
        playerUnits.clear();
        enemyUnits.clear();
        projectiles.clear();
        playerTeam = null;
        enemyTeam = null;
        nextPlayerLaneIndex = 0;
        nextEnemyLaneIndex = 0;
        heroLaneIndex = PLAYER_DEFAULT_LANE;
        enemyLaneIndex = ENEMY_DEFAULT_LANE;
        playerHero = null;
        aiHero = null;
        enemyIsHuman = false;
        activeHeroIndex = 0;
        playerPortalBreaches = 0;
        enemyPortalBreaches = 0;
        playerKills = 0;
        enemyKills = 0;
        lastActionMessage = "Select a hero to begin the battle.";
        baseLabel.setText(String.format("Portal Breaches - Player 1: 0/%d | Enemy: 0/%d", PORTAL_BREACH_THRESHOLD,
                PORTAL_BREACH_THRESHOLD));
        heroLabel.setText("Hero: None selected");
        aiLabel.setText("Enemy Hero: Unknown");
        killsLabel.setText("Kills - You: 0 | Enemy: 0");
        economyLabel.setText("Economy - Gold 0 (+0) | Enemy Gold 0 (+0)");
        actionLabel.setText(lastActionMessage);
        queueLabel.setText("Next Wave: None queued.");
        inventoryLabel.setText("Inventory: None");
        refreshPlayerSelector();
        refreshControlsHint();
        updateHeroInterface();
        battlefieldPanel.repaint();
        showStartupFlow();
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
        setPreferredSize(new Dimension(1580, 980));
        setMinimumSize(new Dimension(1380, 860));
        getContentPane().setBackground(new Color(10, 14, 22));
        setLocationByPlatform(true);

        initializeUnitBalances();
        buildInterface();
        pack();
        setLocationRelativeTo(null);
        setResizable(true);

        showStartupFlow();
    }

    private void initializeUnitBalances() {
        unitBalances.clear();
        defaultUnitBalances.clear();
        for (UnitType type : UnitType.values()) {
            UnitBalance defaults = UnitBalance.from(type);
            defaultUnitBalances.put(type, defaults);
            unitBalances.put(type, defaults.copy());
        }
    }

    private void showStartupFlow() {
        Runnable flow = () -> {
            if (!showModeSelectionDialog()) {
                dispose();
                return;
            }
            if (gameMode == GameMode.SOLO) {
                showHeroSelectionDialogForPlayer("Choose Your Hero", hero -> {
                    if (hero == null) {
                        dispose();
                        return;
                    }
                    playerHero = hero;
                    aiHero = createAiHero();
                    enemyIsHuman = false;
                    activeHeroIndex = 0;
                    refreshControlsHint();
                    startBattle();
                });
            } else {
                showHeroSelectionDialogForPlayer("Player 1 - Choose Hero", hero -> {
                    if (hero == null) {
                        dispose();
                        return;
                    }
                    playerHero = hero;
                    showHeroSelectionDialogForPlayer("Player 2 - Choose Hero", hero2 -> {
                        if (hero2 == null) {
                            dispose();
                            return;
                        }
                        secondPlayerSelection = hero2;
                        aiHero = secondPlayerSelection;
                        enemyIsHuman = true;
                        activeHeroIndex = 0;
                        refreshControlsHint();
                        startBattle();
                    });
                });
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            flow.run();
        } else {
            SwingUtilities.invokeLater(flow);
        }
    }

    private boolean showModeSelectionDialog() {
        Object[] options = {"Solo vs AI", "Local Versus"};
        int choice = JOptionPane.showOptionDialog(this,
                "Select how you want to play.",
                "Select Mode",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        gameMode = choice == 1 ? GameMode.LOCAL_VERSUS : GameMode.SOLO;
        return true;
    }

    private void showHeroSelectionDialogForPlayer(String title, Consumer<Hero> onSelection) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new BorderLayout());

        JLabel description = new JLabel("Select a hero to begin the battle.");
        description.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.add(description, BorderLayout.NORTH);

        JPanel heroPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        heroPanel.add(createHeroSelectionButton(dialog, "Ranger - Fires arrows from long range (7).",
                () -> new Hero("Ranger", 63, 10, 2, 8, 12, 6, Hero.PrimaryAttribute.DEXTERITY, 180, 12, 7), onSelection));
        heroPanel.add(createHeroSelectionButton(dialog, "Berserker - Close combat specialist with brutal swings (2).",
                () -> new Hero("Berserker", 65, 4, 2, 15, 6, 5, Hero.PrimaryAttribute.STRENGTH, 200, 10, 2), onSelection));
        heroPanel.add(createHeroSelectionButton(dialog, "Mage - Launches arcane bolts from mid range (4).",
                () -> new Hero("Mage", 56, 13, 1, 6, 8, 14, Hero.PrimaryAttribute.INTELLIGENCE, 160, 14, 4), onSelection));

        dialog.add(heroPanel, BorderLayout.CENTER);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
                onSelection.accept(null);
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JButton createHeroSelectionButton(JDialog dialog, String label, Supplier<Hero> supplier,
            Consumer<Hero> onSelection) {
        JButton button = new JButton(label);
        button.addActionListener(e -> {
            dialog.dispose();
            onSelection.accept(supplier.get());
        });
        return button;
    }

    private void buildInterface() {
        styleStatusLabel(modeLabel, Font.BOLD, 26f, new Color(215, 235, 255));
        styleStatusLabel(baseLabel, Font.BOLD, 16f, new Color(230, 235, 245));
        styleStatusLabel(heroLabel, Font.PLAIN, 15f, new Color(200, 215, 240));
        styleStatusLabel(aiLabel, Font.PLAIN, 15f, new Color(200, 215, 240));
        styleStatusLabel(killsLabel, Font.PLAIN, 15f, new Color(200, 215, 240));
        styleStatusLabel(economyLabel, Font.PLAIN, 15f, new Color(200, 215, 240));
        styleStatusLabel(queueLabel, Font.PLAIN, 14f, new Color(190, 210, 235));
        styleStatusLabel(inventoryLabel, Font.PLAIN, 14f, new Color(190, 210, 235));
        styleStatusLabel(controlsHintLabel, Font.PLAIN, 13f, new Color(205, 220, 235));
        styleStatusLabel(actionLabel, Font.BOLD, 15f, new Color(240, 220, 160));
        applyCategoryIcon(baseLabel, IconLibrary.Category.PORTAL);
        applyCategoryIcon(heroLabel, IconLibrary.Category.HERO);
        applyCategoryIcon(aiLabel, IconLibrary.Category.ENEMY);
        applyCategoryIcon(killsLabel, IconLibrary.Category.KILLS);
        applyCategoryIcon(economyLabel, IconLibrary.Category.ECONOMY);
        applyCategoryIcon(queueLabel, IconLibrary.Category.QUEUE);
        applyCategoryIcon(inventoryLabel, IconLibrary.Category.INVENTORY);
        applyCategoryIcon(actionLabel, IconLibrary.Category.ACTION);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 18, 12, 18));
        topPanel.setBackground(new Color(16, 24, 36));
        modeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(modeLabel, BorderLayout.NORTH);

        JPanel infoGrid = new JPanel(new GridLayout(2, 3, 16, 6));
        infoGrid.setOpaque(false);
        infoGrid.add(baseLabel);
        infoGrid.add(heroLabel);
        infoGrid.add(aiLabel);
        infoGrid.add(killsLabel);
        infoGrid.add(economyLabel);
        infoGrid.add(queueLabel);
        topPanel.add(infoGrid, BorderLayout.CENTER);

        JPanel infoFooter = new JPanel(new BorderLayout());
        infoFooter.setOpaque(false);
        infoFooter.add(inventoryLabel, BorderLayout.WEST);
        controlsHintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoFooter.add(controlsHintLabel, BorderLayout.CENTER);
        actionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        actionLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        infoFooter.add(actionLabel, BorderLayout.EAST);
        topPanel.add(infoFooter, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        add(battlefieldPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 14, 8, 14));
        bottomPanel.setBackground(new Color(12, 18, 28));

        bottomPanel.add(createHeroInterfacePanel(), BorderLayout.WEST);
        bottomPanel.add(createCommandPanel(), BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        updateHeroInterface();
        refreshControlsHint();
    }

    private void refreshPlayerSelector() {
        playerSelector.removeAllItems();
        playerSelector.addItem("Player 1");
        if (enemyIsHuman) {
            playerSelector.addItem("Player 2");
            playerSelector.setEnabled(true);
        } else {
            playerSelector.setEnabled(false);
        }
        if (playerSelector.getItemCount() > 0) {
            int targetIndex = Math.min(activeHeroIndex, playerSelector.getItemCount() - 1);
            playerSelector.setSelectedIndex(targetIndex);
        }
        updateHeroInterface();
    }

    private void refreshControlsHint() {
        final String bullet = " • ";
        StringBuilder builder = new StringBuilder();
        if (enemyIsHuman) {
            builder.append("P1 Hero: WASD").append(bullet).append("P2 Hero: Arrow Keys").append(bullet);
            builder.append("Camera: Q/E/R/F or Shift+Arrow Keys");
        } else {
            builder.append("Hero Movement: WASD").append(bullet);
            builder.append("Camera: Arrow Keys or Q/E/R/F");
        }
        builder.append(bullet).append("Center: C");
        builder.append(bullet).append("Zoom: +/- or Mouse Wheel");
        builder.append(bullet).append("Pause: Esc/P");
        controlsHintLabel.setText(builder.toString());
    }

    private Hero getActiveHero() {
        return getHeroForIndex(activeHeroIndex);
    }

    private Hero getHeroForIndex(int index) {
        if (index <= 0 || !enemyIsHuman) {
            return playerHero;
        }
        return aiHero;
    }

    private Team getTeamForIndex(int index) {
        if (index <= 0 || !enemyIsHuman) {
            return playerTeam;
        }
        return enemyTeam;
    }

    private JPanel createCommandPanel() {
        JPanel commandPanel = new JPanel(new BorderLayout());
        commandPanel.setOpaque(true);
        commandPanel.setBackground(new Color(14, 20, 30));
        commandPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(32, 44, 60)),
                javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        JLabel helpLabel = new JLabel(
                "Click to move. Use the selector to issue orders for each commander.");
        helpLabel.setForeground(new Color(210, 220, 235));
        helpLabel.setFont(helpLabel.getFont().deriveFont(11.5f));
        header.add(helpLabel, BorderLayout.CENTER);

        playerSelector.setFocusable(false);
        playerSelector.addActionListener(e -> {
            int index = playerSelector.getSelectedIndex();
            if (index >= 0) {
                activeHeroIndex = index;
                updateHeroInterface();
            }
        });
        header.add(playerSelector, BorderLayout.EAST);
        commandPanel.add(header, BorderLayout.NORTH);

        JPanel unitButtonPanel = new JPanel(new GridLayout(1, 0, 4, 4));
        unitButtonPanel.setOpaque(false);
        unitButtons.clear();
        for (UnitType type : UnitType.values()) {
            JButton button = new JButton(formatUnitButtonLabel(type));
            button.setToolTipText(createUnitTooltip(type));
            button.addActionListener(e -> attemptSendUnit(type));
            stylePrimaryButton(button);
            unitButtons.put(type, button);
            unitButtonPanel.add(button);
        }
        unitButtonPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 0, 2, 0));
        commandPanel.add(unitButtonPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JPanel utilityPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        utilityPanel.setOpaque(false);
        JButton inventoryButton = new JButton("Inventory");
        inventoryButton.addActionListener(e -> openInventoryDialog(getActiveHero()));
        JButton shopButton = new JButton("Open Shop");
        shopButton.addActionListener(e -> openShopDialog(getActiveHero()));
        JButton pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> openPauseMenu());
        styleSecondaryButton(inventoryButton);
        styleSecondaryButton(shopButton);
        styleSecondaryButton(pauseButton);
        applyCategoryIcon(inventoryButton, IconLibrary.Category.INVENTORY);
        applyCategoryIcon(shopButton, IconLibrary.Category.SHOP);
        applyCategoryIcon(pauseButton, IconLibrary.Category.PAUSE);
        utilityPanel.add(inventoryButton);
        utilityPanel.add(shopButton);
        utilityPanel.add(pauseButton);
        footer.add(utilityPanel, BorderLayout.EAST);

        commandPanel.add(footer, BorderLayout.SOUTH);

        refreshPlayerSelector();

        return commandPanel;
    }

    private String formatUnitButtonLabel(UnitType type) {
        UnitBalance balance = getUnitBalance(type);
        return String.format("%s (%dG, +%d income)", type.getDisplayName(), balance.getCost(),
                balance.getIncomeBonus());
    }

    private String createUnitTooltip(UnitType type) {
        UnitBalance balance = getUnitBalance(type);
        return String.format("<html>%s<br/>Cost: %dG &nbsp; Income: +%d<br/>Health: %d &nbsp; Damage: %d &nbsp; Range: %d</html>",
                type.getDescription(), balance.getCost(), balance.getIncomeBonus(), balance.getHealth(),
                balance.getDamage(), balance.getRange());
    }

    private void refreshUnitButtons() {
        for (UnitType type : unitButtons.keySet()) {
            JButton button = unitButtons.get(type);
            if (button != null) {
                button.setText(formatUnitButtonLabel(type));
                button.setToolTipText(createUnitTooltip(type));
            }
        }
    }

    private UnitBalance getUnitBalance(UnitType type) {
        UnitBalance balance = unitBalances.get(type);
        if (balance == null) {
            UnitBalance defaults = UnitBalance.from(type);
            unitBalances.put(type, defaults);
            defaultUnitBalances.putIfAbsent(type, defaults.copy());
            return defaults;
        }
        return balance;
    }

    private void styleStatusLabel(JLabel label, int style, float size, Color color) {
        label.setFont(label.getFont().deriveFont(style, size));
        label.setForeground(color);
        label.setOpaque(false);
    }

    private void stylePrimaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setBackground(new Color(40, 64, 92));
        button.setForeground(Color.WHITE);
        button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(56, 92, 130)),
                javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        button.setFont(button.getFont().deriveFont(12f));
    }

    private void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setBackground(new Color(54, 64, 80));
        button.setForeground(new Color(235, 240, 250));
        button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(70, 88, 110)),
                javax.swing.BorderFactory.createEmptyBorder(3, 7, 3, 7)));
        button.setFont(button.getFont().deriveFont(11.5f));
    }

    private void applyCategoryIcon(JLabel label, IconLibrary.Category category) {
        label.setIcon(IconLibrary.createCategoryGlyph(category));
        label.setIconTextGap(8);
        label.setHorizontalTextPosition(SwingConstants.RIGHT);
    }

    private void applyCategoryIcon(AbstractButton button, IconLibrary.Category category) {
        button.setIcon(IconLibrary.createCategoryGlyph(category));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        button.setIconTextGap(6);
    }

    private void applyAttributeIcon(JButton button, Hero.PrimaryAttribute attribute) {
        button.setIcon(IconLibrary.createAttributeGlyph(attribute));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        button.setIconTextGap(6);
    }

    private JPanel createHeroInterfacePanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 3, 3));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10));
        panel.setBackground(new Color(18, 24, 32));

        Font bold = heroSummaryLabel.getFont().deriveFont(Font.BOLD, 14f);
        heroSummaryLabel.setFont(bold);
        heroSummaryLabel.setForeground(Color.WHITE);

        heroAttributesLabel.setForeground(new Color(200, 220, 255));
        heroCombatLabel.setForeground(new Color(200, 220, 255));
        heroProgressLabel.setForeground(new Color(200, 220, 255));
        heroResourceLabel.setForeground(new Color(200, 220, 255));
        upgradeSummaryLabel.setForeground(new Color(210, 220, 255));
        applyCategoryIcon(heroSummaryLabel, IconLibrary.Category.HERO_SUMMARY);
        applyCategoryIcon(heroAttributesLabel, IconLibrary.Category.ATTRIBUTES);
        applyCategoryIcon(heroCombatLabel, IconLibrary.Category.COMBAT);
        applyCategoryIcon(heroProgressLabel, IconLibrary.Category.PROGRESS);
        applyCategoryIcon(heroResourceLabel, IconLibrary.Category.RESOURCES);
        applyCategoryIcon(upgradeSummaryLabel, IconLibrary.Category.ATTRIBUTES);

        configureUpgradeButton(upgradeStrengthButton, "Strength", Hero.PrimaryAttribute.STRENGTH);
        configureUpgradeButton(upgradeDexterityButton, "Dexterity", Hero.PrimaryAttribute.DEXTERITY);
        configureUpgradeButton(upgradeIntelligenceButton, "Intelligence", Hero.PrimaryAttribute.INTELLIGENCE);

        panel.add(heroSummaryLabel);
        panel.add(heroAttributesLabel);
        panel.add(heroCombatLabel);
        panel.add(heroProgressLabel);
        panel.add(heroResourceLabel);
        JPanel upgradePanel = new JPanel(new GridLayout(0, 1, 4, 4));
        upgradePanel.setOpaque(false);
        upgradePanel.add(upgradeSummaryLabel);
        JPanel upgradeButtonsRow = new JPanel(new GridLayout(1, 0, 4, 4));
        upgradeButtonsRow.setOpaque(false);
        upgradeButtonsRow.add(upgradeStrengthButton);
        upgradeButtonsRow.add(upgradeDexterityButton);
        upgradeButtonsRow.add(upgradeIntelligenceButton);
        upgradePanel.add(upgradeButtonsRow);
        panel.add(upgradePanel);
        panel.setOpaque(true);
        updateUpgradeButtons();
        return panel;
    }

    private void configureUpgradeButton(JButton button, String attributeName, Hero.PrimaryAttribute attribute) {
        button.setFocusPainted(false);
        button.setBackground(new Color(36, 46, 60));
        button.setForeground(Color.WHITE);
        button.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        button.setFont(button.getFont().deriveFont(12.5f));
        button.setToolTipText(String.format("Spend %d gold to increase %s by 1.", ATTRIBUTE_UPGRADE_COST,
                attributeName.toLowerCase()));
        applyAttributeIcon(button, attribute);
        button.addActionListener(e -> attemptAttributeUpgrade(attribute, attributeName));
    }

    private void updateUpgradeButtons() {
        Hero hero = getHeroForIndex(activeHeroIndex);
        if (hero == null) {
            upgradeSummaryLabel.setText("Invest gold to train attributes.");
            upgradeStrengthButton.setText(String.format("Strength +1 (%dG)", ATTRIBUTE_UPGRADE_COST));
            upgradeDexterityButton.setText(String.format("Dexterity +1 (%dG)", ATTRIBUTE_UPGRADE_COST));
            upgradeIntelligenceButton.setText(String.format("Intelligence +1 (%dG)", ATTRIBUTE_UPGRADE_COST));
            upgradeStrengthButton.setEnabled(false);
            upgradeDexterityButton.setEnabled(false);
            upgradeIntelligenceButton.setEnabled(false);
            return;
        }

        upgradeSummaryLabel.setText(String.format("Spend %d gold to gain +1 attribute point.", ATTRIBUTE_UPGRADE_COST));
        updateUpgradeButton(upgradeStrengthButton, "Strength", hero.getStrength(), hero);
        updateUpgradeButton(upgradeDexterityButton, "Dexterity", hero.getDexterity(), hero);
        updateUpgradeButton(upgradeIntelligenceButton, "Intelligence", hero.getIntelligence(), hero);
    }

    private void updateUpgradeButton(JButton button, String attributeName, int currentValue, Hero hero) {
        button.setText(String.format("%s %d (+1) - %dG", attributeName, currentValue, ATTRIBUTE_UPGRADE_COST));
        boolean affordable = hero != null && hero.getGold() >= ATTRIBUTE_UPGRADE_COST;
        button.setEnabled(affordable);
        button.setToolTipText(String.format("Spend %d gold to increase %s by 1 (current %d).", ATTRIBUTE_UPGRADE_COST,
                attributeName.toLowerCase(), currentValue));
    }

    private void showHeroSelectionDialog() {
        JDialog dialog = new JDialog(this, "Choose Your Hero", true);
        dialog.setLayout(new BorderLayout());

        JLabel description = new JLabel("Select a hero to begin the battle.");
        description.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.add(description, BorderLayout.NORTH);

        JPanel heroPanel = new JPanel(new GridLayout(0, 1, 10, 10));

        JButton rangerButton = new JButton("Ranger - Fires arrows from long range (7).");
        rangerButton.addActionListener(e -> {
            playerHero = new Hero("Ranger", 63, 10, 2, 8, 12, 6, Hero.PrimaryAttribute.DEXTERITY, 180, 12, 7);
            dialog.dispose();
            startBattle();
        });
        heroPanel.add(rangerButton);

        JButton berserkerButton = new JButton("Berserker - Close combat specialist with brutal swings (2).");
        berserkerButton.addActionListener(e -> {
            playerHero = new Hero("Berserker", 65, 4, 2, 15, 6, 5, Hero.PrimaryAttribute.STRENGTH, 200, 10, 2);
            dialog.dispose();
            startBattle();
        });
        heroPanel.add(berserkerButton);

        JButton mageButton = new JButton("Mage - Launches arcane bolts from mid range (4).");
        mageButton.addActionListener(e -> {
            playerHero = new Hero("Mage", 56, 13, 1, 6, 8, 14, Hero.PrimaryAttribute.INTELLIGENCE, 160, 14, 4);
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
        if (playerHero == null) {
            return;
        }
        if (!enemyIsHuman) {
            aiHero = createAiHero();
        } else if (aiHero == null) {
            aiHero = new Hero("Guest", 60, 9, 2, 8, 9, 8, Hero.PrimaryAttribute.DEXTERITY, 160, 12, 4);
        }
        modeLabel.setText(enemyIsHuman ? "Hero Line Wars - Local Versus" : "Hero Line Wars - Live Battle");
        playerTeam = new Team(enemyIsHuman ? "Player 1" : "Player", playerHero);
        enemyTeam = new Team(enemyIsHuman ? "Player 2" : "Enemy", aiHero);
        refreshControlsHint();
        playerPortalBreaches = 0;
        enemyPortalBreaches = 0;
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
        projectiles.clear();
        nextPlayerLaneIndex = 0;
        nextEnemyLaneIndex = 0;
        heroLaneIndex = PLAYER_DEFAULT_LANE;
        enemyLaneIndex = ENEMY_DEFAULT_LANE;
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

        resetCamera();

        refreshPlayerSelector();
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

        if (!enemyIsHuman) {
            if (aiSendTimer > 0) {
                aiSendTimer--;
            }
            if (aiSendTimer <= 0) {
                attemptAiSendUnit();
                aiSendTimer = 90 + random.nextInt(90);
            }
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
            updateHeroLaneLock();
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
                heroLaneIndex = PLAYER_DEFAULT_LANE;
            }
        }

        if (enemyAlive) {
            if (enemyIsHuman) {
                enemyX = approach(enemyX, enemyTargetX, ENEMY_SPEED);
                enemyY = approach(enemyY, enemyTargetY, ENEMY_SPEED);
                updateEnemyLaneLock();
            } else {
                UnitInstance threat = findNearestUnit(playerUnits, enemyX + HERO_WIDTH / 2.0, enemyY + HERO_WIDTH / 2.0);
                if (threat != null) {
                    double desired = threat.getCenterX() - HERO_WIDTH / 2.0 - 18;
                    int targetLane = threat.getLaneIndex();
                    double candidateX = clampHorizontalTarget(desired);
                    if (targetLane != enemyLaneIndex && !shouldAllowEnemyLaneSwitch(candidateX)) {
                        enemyTargetX = clampHorizontalTarget(getEnemySwitchAnchorX() - HERO_WIDTH / 2.0);
                        enemyTargetY = clampEnemyVerticalTarget(getEnemyLaneCenterY(enemyLaneIndex) - HERO_WIDTH / 2.0);
                    } else {
                        enemyTargetX = candidateX;
                        enemyTargetY = clampEnemyVerticalTarget(threat.getCenterY() - HERO_WIDTH / 2.0);
                        if (shouldAllowEnemyLaneSwitch(enemyTargetX)) {
                            enemyLaneIndex = clampEnemyLaneIndex(targetLane);
                        }
                    }
                } else {
                    enemyTargetX = clampHorizontalTarget(getEnemyBaseX() - HERO_WIDTH - 20);
                    enemyTargetY = clampEnemyVerticalTarget(getEnemyLaneCenterY(enemyLaneIndex) - HERO_WIDTH / 2.0);
                }
                enemyX = approach(enemyX, enemyTargetX, ENEMY_SPEED);
                enemyY = approach(enemyY, enemyTargetY, ENEMY_SPEED);
                updateEnemyLaneLock();
            }
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
                enemyLaneIndex = ENEMY_DEFAULT_LANE;
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

        handleHeroAttacks();
        updateProjectiles();
        handleBasePressure();
        updateUnits();
        resolveHeroUnitCombat();

        refreshHud();
        battlefieldPanel.repaint();
    }

    private void handleHeroAttacks() {
        processHeroAttack(true);
        processHeroAttack(false);
    }

    private void processHeroAttack(boolean fromPlayer) {
        Hero attacker = fromPlayer ? playerHero : aiHero;
        if (attacker == null) {
            return;
        }
        boolean attackerAlive = fromPlayer ? heroAlive : enemyAlive;
        if (!attackerAlive) {
            return;
        }
        double centerX = (fromPlayer ? heroX : enemyX) + HERO_WIDTH / 2.0;
        double centerY = (fromPlayer ? heroY : enemyY) + HERO_WIDTH / 2.0;
        int laneIndex = fromPlayer ? heroLaneIndex : enemyLaneIndex;
        int range = attacker.getAttackRangePixels();

        HeroTarget target = selectHeroTarget(fromPlayer, centerX, centerY, range, laneIndex);
        if (target == null) {
            return;
        }

        if (getAttackCooldown(fromPlayer) > 0) {
            return;
        }

        ProjectileType projectileType = determineProjectileType(attacker);
        if (projectileType != null) {
            if (hasActiveProjectile(fromPlayer)) {
                return;
            }
            launchProjectile(fromPlayer, projectileType, centerX, centerY, attacker, target);
            setAttackCooldown(fromPlayer, getHeroAttackCooldownTicks(attacker));
        } else {
            applyDirectHeroAttack(fromPlayer, attacker, target);
            setAttackCooldown(fromPlayer, getHeroAttackCooldownTicks(attacker));
        }
    }

    private void applyDirectHeroAttack(boolean fromPlayer, Hero attacker, HeroTarget target) {
        if (target.isHeroTarget()) {
            if (fromPlayer) {
                int damage = Math.max(1, attacker.rollAttackDamage() - aiHero.getDefense());
                if (aiHero.takeDamage(damage)) {
                    onEnemyHeroDefeated();
                }
            } else {
                int damage = Math.max(1, attacker.rollAttackDamage() - playerHero.getDefense());
                if (playerHero.takeDamage(damage)) {
                    onPlayerHeroDefeated();
                }
            }
            return;
        }

        UnitInstance unit = target.getUnit();
        if (unit == null) {
            return;
        }
        unit.takeDamage(Math.max(1, attacker.rollAttackDamage()));
        if (unit.isDead()) {
            handleUnitDefeatedByHero(unit, fromPlayer);
        }
    }

    private HeroTarget selectHeroTarget(boolean fromPlayer, double centerX, double centerY, int range, int laneIndex) {
        if (fromPlayer) {
            if (enemyAlive) {
                double enemyCenterX = enemyX + HERO_WIDTH / 2.0;
                double enemyCenterY = enemyY + HERO_WIDTH / 2.0;
                if (!isSeparatedByLaneWall(centerY, enemyCenterY) && laneIndex == enemyLaneIndex) {
                    if (distance(centerX, centerY, enemyCenterX, enemyCenterY) <= range) {
                        return HeroTarget.enemyHero();
                    }
                }
            }
            UnitInstance unit = findUnitInLaneRange(enemyUnits, centerX, centerY, range, laneIndex);
            if (unit != null) {
                return HeroTarget.unit(unit);
            }
        } else {
            if (heroAlive) {
                double heroCenterX = heroX + HERO_WIDTH / 2.0;
                double heroCenterY = heroY + HERO_WIDTH / 2.0;
                if (!isSeparatedByLaneWall(centerY, heroCenterY) && laneIndex == heroLaneIndex) {
                    if (distance(centerX, centerY, heroCenterX, heroCenterY) <= range) {
                        return HeroTarget.playerHero();
                    }
                }
            }
            UnitInstance unit = findUnitInLaneRange(playerUnits, centerX, centerY, range, laneIndex);
            if (unit != null) {
                return HeroTarget.unit(unit);
            }
        }
        return null;
    }

    private boolean isSeparatedByLaneWall(double sourceY, double targetY) {
        int wallTop = getPlayerClusterBottom();
        int wallBottom = getEnemyClusterTop();
        return (sourceY < wallTop && targetY > wallBottom) || (targetY < wallTop && sourceY > wallBottom);
    }

    private UnitInstance findUnitInLaneRange(java.util.List<UnitInstance> units, double referenceX, double referenceY,
            int range, int laneIndex) {
        UnitInstance nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (UnitInstance unit : units) {
            if (unit.getLaneIndex() != laneIndex) {
                continue;
            }
            double dist = distance(unit.getCenterX(), unit.getCenterY(), referenceX, referenceY);
            if (dist <= range && dist < bestDistance) {
                bestDistance = dist;
                nearest = unit;
            }
        }
        return nearest;
    }

    private int getAttackCooldown(boolean fromPlayer) {
        return fromPlayer ? heroAttackCooldown : enemyAttackCooldown;
    }

    private void setAttackCooldown(boolean fromPlayer, int value) {
        if (fromPlayer) {
            heroAttackCooldown = value;
        } else {
            enemyAttackCooldown = value;
        }
    }

    private ProjectileType determineProjectileType(Hero hero) {
        if (hero == null || hero.getAttackRangeUnits() <= 3) {
            return null;
        }
        if (hero.getPrimaryAttribute() == Hero.PrimaryAttribute.INTELLIGENCE) {
            return ProjectileType.MAGIC_BOLT;
        }
        return ProjectileType.ARROW;
    }

    private boolean hasActiveProjectile(boolean fromPlayer) {
        for (Projectile projectile : projectiles) {
            if (projectile.isFromPlayer() == fromPlayer) {
                return true;
            }
        }
        return false;
    }

    private void launchProjectile(boolean fromPlayer, ProjectileType type, double centerX, double centerY, Hero attacker,
            HeroTarget target) {
        projectiles.add(new Projectile(fromPlayer, type, centerX, centerY, attacker, target));
    }

    private void handleUnitDefeatedByHero(UnitInstance unit, boolean byPlayerHero) {
        java.util.List<UnitInstance> sourceList = byPlayerHero ? enemyUnits : playerUnits;
        if (!sourceList.remove(unit)) {
            return;
        }
        if (byPlayerHero) {
            playerHero.addGold(UNIT_KILL_REWARD);
            StringBuilder builder = new StringBuilder(String.format("%s defeated an enemy %s!",
                    playerHero.getName(), unit.getType().getDisplayName()));
            int levels = playerHero.gainExperience(EXPERIENCE_PER_UNIT_KILL);
            if (levels > 0) {
                builder.append(String.format(" %s reached level %d!", playerHero.getName(), playerHero.getLevel()));
            }
            lastActionMessage = builder.toString();
        } else {
            int levels = aiHero.gainExperience(EXPERIENCE_PER_UNIT_KILL);
            if (levels > 0) {
                lastActionMessage = String.format("Enemy %s grew stronger and reached level %d!", aiHero.getName(),
                        aiHero.getLevel());
            }
        }
    }

    private void registerPortalBreach(boolean byPlayer, String description) {
        if (gameOver) {
            return;
        }
        if (byPlayer) {
            enemyPortalBreaches = Math.min(PORTAL_BREACH_THRESHOLD, enemyPortalBreaches + 1);
        } else {
            playerPortalBreaches = Math.min(PORTAL_BREACH_THRESHOLD, playerPortalBreaches + 1);
        }
        int breaches = byPlayer ? enemyPortalBreaches : playerPortalBreaches;
        String context;
        if (description != null && !description.isBlank()) {
            context = description;
        } else {
            context = byPlayer ? "Your units breached the enemy portal!" : "Enemy units slipped through your portal!";
        }
        lastActionMessage = String.format("%s (%d/%d)", context, breaches, PORTAL_BREACH_THRESHOLD);
        checkVictoryConditions();
    }

    private void updateProjectiles() {
        if (projectiles.isEmpty()) {
            return;
        }
        java.util.Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            if (projectile.update()) {
                iterator.remove();
            }
        }
    }

    private void handleProjectileImpact(Projectile projectile) {
        if (projectile.isHeroTarget()) {
            if (projectile.isTargetingEnemyHero()) {
                if (!enemyAlive) {
                    return;
                }
                if (aiHero.takeDamage(projectile.getDamage())) {
                    onEnemyHeroDefeated();
                }
            } else {
                if (!heroAlive) {
                    return;
                }
                if (playerHero.takeDamage(projectile.getDamage())) {
                    onPlayerHeroDefeated();
                }
            }
            return;
        }

        UnitInstance unit = projectile.getTargetUnit();
        if (unit == null) {
            return;
        }
        unit.takeDamage(projectile.getDamage());
        if (unit.isDead()) {
            handleUnitDefeatedByHero(unit, projectile.isFromPlayer());
        }
    }

    private int getHeroAttackCooldownTicks(Hero hero) {
        return Math.max(8, (int) Math.round(ATTACK_COOLDOWN_TICKS / hero.getAttackSpeedMultiplier()));
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
                registerPortalBreach(true,
                        String.format("%s forced a breach in the enemy portal!", playerHero.getName()));
                heroBaseAttackCooldown = getHeroBaseAttackCooldownTicks(playerHero);
            }
        }
        if (enemyAlive && !heroAlive) {
            if (enemyBaseAttackCooldown <= 0 && enemyX <= getPlayerBaseX() + BASE_WIDTH
                    && enemyCenterY >= playerBaseTop && enemyCenterY <= playerBaseBottom) {
                registerPortalBreach(false,
                        String.format("Enemy %s ruptured your portal defenses!", aiHero.getName()));
                enemyBaseAttackCooldown = getHeroBaseAttackCooldownTicks(aiHero);
            }
        }
    }

    private int getHeroBaseAttackCooldownTicks(Hero hero) {
        return Math.max(12, (int) Math.round(PORTAL_ATTACK_COOLDOWN_TICKS / hero.getAttackSpeedMultiplier()));
    }

    private void onPlayerHeroDefeated() {
        heroAlive = false;
        heroRespawnTimer = RESPAWN_TICKS;
        enemyKills++;
        heroAttackCooldown = getHeroAttackCooldownTicks(playerHero);
        StringBuilder builder = new StringBuilder("You were defeated! The enemy presses the attack.");
        int levels = aiHero.gainExperience(EXPERIENCE_PER_HERO_KILL);
        if (levels > 0) {
            builder.append(String.format(" Enemy %s reached level %d!", aiHero.getName(), aiHero.getLevel()));
        }
        lastActionMessage = builder.toString();
    }

    private void onEnemyHeroDefeated() {
        enemyAlive = false;
        enemyRespawnTimer = RESPAWN_TICKS;
        playerKills++;
        enemyAttackCooldown = getHeroAttackCooldownTicks(aiHero);
        StringBuilder builder = new StringBuilder("Enemy hero defeated! Push the advantage.");
        int levels = playerHero.gainExperience(EXPERIENCE_PER_HERO_KILL);
        if (levels > 0) {
            builder.append(String.format(" %s reached level %d!", playerHero.getName(), playerHero.getLevel()));
        }
        lastActionMessage = builder.toString();
    }

    private void checkVictoryConditions() {
        if (gameOver) {
            return;
        }
        if (enemyPortalBreaches >= PORTAL_BREACH_THRESHOLD) {
            finishBattle(true);
        } else if (playerPortalBreaches >= PORTAL_BREACH_THRESHOLD) {
            finishBattle(false);
        }
    }

    private void finishBattle(boolean playerWon) {
        gameOver = true;
        if (gameTimer != null) {
            gameTimer.stop();
        }
        String message = playerWon ? "Victory! The enemy portal has been overrun." :
                "Defeat! Too many foes breached your portal.";
        lastActionMessage = playerWon ? "Victory! Enemy portal sealed." : "Defeat! Your portal has collapsed.";
        refreshHud();
        battlefieldPanel.repaint();
        JOptionPane.showMessageDialog(this, message, "Battle Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshHud() {
        if (playerHero == null || aiHero == null) {
            return;
        }
        String enemyName = enemyIsHuman ? "Player 2" : "Enemy";
        baseLabel.setText(String.format("Portal Breaches - Player 1: %d/%d | %s: %d/%d",
                playerPortalBreaches, PORTAL_BREACH_THRESHOLD, enemyName, enemyPortalBreaches,
                PORTAL_BREACH_THRESHOLD));
        heroLabel.setText(String.format("Player 1 Hero: %s (Lv %d) | HP %d/%d | Shield %d/%d | ATK %d | DEF %d",
                playerHero.getName(), playerHero.getLevel(),
                Math.max(0, playerHero.getCurrentHealth()), playerHero.getMaxHealth(), playerHero.getCurrentShield(), playerHero.getMaxEnergyShield(),
                playerHero.getAttackPower(), playerHero.getDefense()));
        aiLabel.setText(String.format("%s Hero: %s (Lv %d) | HP %d/%d | Shield %d/%d | ATK %d | DEF %d",
                enemyName, aiHero.getName(), aiHero.getLevel(),
                Math.max(0, aiHero.getCurrentHealth()), aiHero.getMaxHealth(), aiHero.getCurrentShield(), aiHero.getMaxEnergyShield(),
                aiHero.getAttackPower(), aiHero.getDefense()));
        killsLabel.setText(String.format("Kills - Player 1: %d | %s: %d", playerKills, enemyName, enemyKills));
        economyLabel.setText(String.format("Economy - Gold %d (+%d) | %s Gold %d (+%d)",
                playerHero.getGold(), playerHero.getIncome(), enemyName, aiHero.getGold(), aiHero.getIncome()));
        updateQueueLabel();
        updateInventoryLabel();
        double seconds = Math.max(0, waveCountdown) * TICK_MILLIS / 1000.0;
        String statusMessage = paused ? "Game paused." : lastActionMessage;
        actionLabel.setText(String.format("%s Next wave in %.1f s.", statusMessage, seconds));
        updateHeroInterface();
    }

    private void updateHeroInterface() {
        Hero hero = getHeroForIndex(activeHeroIndex);
        if (hero == null) {
            heroSummaryLabel.setText("Hero interface locked until a hero is chosen.");
            heroAttributesLabel.setText("Attributes: --");
            heroCombatLabel.setText("Combat data unavailable.");
            heroProgressLabel.setText("Progress: --");
            heroResourceLabel.setText("Vitals: --");
            updateUpgradeButtons();
            return;
        }

        String primary = hero.getPrimaryAttribute().name().substring(0, 1)
                + hero.getPrimaryAttribute().name().substring(1).toLowerCase();
        heroSummaryLabel.setText(String.format("%s - Level %d (%s)", hero.getName(), hero.getLevel(), primary));
        heroAttributesLabel.setText(String.format("Attributes: STR %d | DEX %d | INT %d",
                hero.getStrength(), hero.getDexterity(), hero.getIntelligence()));

        double attackSpeedBonus = (hero.getAttackSpeedMultiplier() - 1.0) * 100.0;
        double critChance = hero.getCriticalChance() * 100.0;
        double evasion = hero.getEvasionChance() * 100.0;
        heroCombatLabel.setText(String.format("Combat: Damage %d | Crit %.1f%% | Attack Speed %+.1f%% | Evasion %.1f%% | Range %d",
                hero.getAttackPower(), critChance, attackSpeedBonus, evasion, hero.getAttackRangeUnits()));

        heroProgressLabel.setText(String.format("Progress: XP %d / %d",
                hero.getExperience(), hero.getExperienceToNextLevel()));

        heroResourceLabel.setText(String.format("Vitals: Health %d/%d | Shield %d/%d | Armor %d",
                Math.max(0, hero.getCurrentHealth()), hero.getMaxHealth(),
                hero.getCurrentShield(), hero.getMaxEnergyShield(),
                hero.getDefense()));
        updateUpgradeButtons();
    }

    private void updateQueueLabel() {
        if (playerTeam == null) {
            queueLabel.setText("Next Wave: None queued.");
            return;
        }
        StringBuilder builder = new StringBuilder();
        java.util.List<UnitType> playerQueued = playerTeam.getQueuedUnitsSnapshot();
        if (playerQueued.isEmpty()) {
            builder.append("Player 1: None");
        } else {
            builder.append("Player 1: ");
            for (int i = 0; i < playerQueued.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(playerQueued.get(i).getDisplayName());
            }
        }
        if (enemyTeam != null) {
            builder.append("  |  ");
            java.util.List<UnitType> enemyQueued = enemyTeam.getQueuedUnitsSnapshot();
            String enemyName = enemyIsHuman ? "Player 2" : "Enemy";
            if (enemyQueued.isEmpty()) {
                builder.append(enemyName).append(": None");
            } else {
                builder.append(enemyName).append(": ");
                for (int i = 0; i < enemyQueued.size(); i++) {
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(enemyQueued.get(i).getDisplayName());
                }
            }
        }
        queueLabel.setText("Next Wave - " + builder);
    }

    private void updateInventoryLabel() {
        Hero hero = getHeroForIndex(activeHeroIndex);
        if (hero == null) {
            inventoryLabel.setText("Inventory: None");
            return;
        }
        java.util.List<Item> items = hero.getInventory();
        if (items.isEmpty()) {
            inventoryLabel.setText("Inventory: None");
            return;
        }
        java.util.Map<Item.EquipmentSlot, java.util.List<Item>> equipped = hero.getEquippedItemsBySlot();
        Team team = getTeamForIndex(activeHeroIndex);
        String ownerName = team != null ? team.getName() : hero.getName();
        StringBuilder builder = new StringBuilder(String.format("%s Inventory: ", ownerName));
        boolean appended = false;
        for (Item.EquipmentSlot slot : Item.EquipmentSlot.values()) {
            if (appended) {
                builder.append(" | ");
            }
            appended = true;
            builder.append(slot.getDisplayName());
            if (slot == Item.EquipmentSlot.RING) {
                builder.append(String.format(" (%d/%d)", hero.getEquippedCount(Item.EquipmentSlot.RING),
                        hero.getMaxRings()));
            }
            java.util.List<Item> slotItems = equipped.get(slot);
            if (slotItems == null || slotItems.isEmpty()) {
                builder.append(": Empty");
            } else {
                builder.append(": ");
                for (int i = 0; i < slotItems.size(); i++) {
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(slotItems.get(i).getName());
                }
            }
        }
        java.util.List<Item> slotless = new java.util.ArrayList<>();
        for (Item item : items) {
            if (item.getSlot() == null) {
                slotless.add(item);
            }
        }
        if (!slotless.isEmpty()) {
            if (appended) {
                builder.append(" | ");
            }
            builder.append("Relics: ");
            for (int i = 0; i < slotless.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(slotless.get(i).getName());
            }
        }
        inventoryLabel.setText(builder.toString());
    }

    private Hero createAiHero() {
        int roll = random.nextInt(3);
        switch (roll) {
            case 0:
                return new Hero("Sentinel", 68, 8, 3, 12, 9, 6, Hero.PrimaryAttribute.STRENGTH, 180, 12, 3);
            case 1:
                return new Hero("Berserker", 60, 12, 2, 7, 14, 5, Hero.PrimaryAttribute.DEXTERITY, 170, 13, 2);
            default:
                return new Hero("Warlock", 58, 11, 2, 6, 7, 15, Hero.PrimaryAttribute.INTELLIGENCE, 190, 11, 5);
        }
    }

    private int getPlayerBaseX() {
        return BASE_MARGIN;
    }

    private int getEnemyBaseX() {
        return WORLD_WIDTH - BASE_MARGIN - BASE_WIDTH;
    }

    private double getPlayerSpawnX() {
        return getMovementLeftLimit() + 40;
    }

    private double getEnemySpawnX() {
        return getMovementRightLimit() - 40;
    }

    private double getPlayerSpawnY() {
        return getPlayerLaneCenterY(PLAYER_DEFAULT_LANE) - HERO_WIDTH / 2.0;
    }

    private double getEnemySpawnY() {
        return getEnemyLaneCenterY(ENEMY_DEFAULT_LANE) - HERO_WIDTH / 2.0;
    }

    private double getMovementLeftLimit() {
        return Math.max(0, BASE_MARGIN - HERO_WIDTH - 12);
    }

    private double getMovementRightLimit() {
        double limit = WORLD_WIDTH - BASE_MARGIN - HERO_WIDTH * 0.5;
        return Math.max(getMovementLeftLimit(), limit);
    }

    private int getLaneHeight() {
        int height = WORLD_HEIGHT;
        int laneCount = LANES_PER_SIDE * 2;
        int gapTotal = 2 * LANE_MARGIN + LANE_GAP + (LANES_PER_SIDE - 1) * INTRA_LANE_GAP * 2;
        int available = height - gapTotal;
        int minimum = laneCount * 80;
        if (available < minimum) {
            available = minimum;
        }
        return Math.max(60, available / laneCount);
    }

    private int getClusterHeight() {
        return LANES_PER_SIDE * getLaneHeight() + (LANES_PER_SIDE - 1) * INTRA_LANE_GAP;
    }

    private int getPlayerClusterTop() {
        return LANE_MARGIN;
    }

    private int getPlayerClusterBottom() {
        return getPlayerClusterTop() + getClusterHeight();
    }

    private int getEnemyClusterTop() {
        return getPlayerClusterBottom() + LANE_GAP;
    }

    private int getEnemyClusterBottom() {
        return getEnemyClusterTop() + getClusterHeight();
    }

    private int getPlayerLaneTop(int laneIndex) {
        return getPlayerClusterTop() + laneIndex * (getLaneHeight() + INTRA_LANE_GAP);
    }

    private int getEnemyLaneTop(int laneIndex) {
        return getEnemyClusterTop() + laneIndex * (getLaneHeight() + INTRA_LANE_GAP);
    }

    private double getPlayerLaneCenterY(int laneIndex) {
        return getPlayerLaneTop(laneIndex) + getLaneHeight() / 2.0;
    }

    private double getEnemyLaneCenterY(int laneIndex) {
        return getEnemyLaneTop(laneIndex) + getLaneHeight() / 2.0;
    }

    private double getPlayerClusterCenterY() {
        return getPlayerClusterTop() + getClusterHeight() / 2.0;
    }

    private double getEnemyClusterCenterY() {
        return getEnemyClusterTop() + getClusterHeight() / 2.0;
    }

    private double getPlayerHeroTopLimit() {
        return getPlayerClusterTop() + HERO_VERTICAL_MARGIN;
    }

    private double getPlayerHeroBottomLimit() {
        return getPlayerClusterBottom() - HERO_WIDTH - HERO_VERTICAL_MARGIN;
    }

    private double getEnemyHeroTopLimit() {
        return getEnemyClusterTop() + HERO_VERTICAL_MARGIN;
    }

    private double getEnemyHeroBottomLimit() {
        return getEnemyClusterBottom() - HERO_WIDTH - HERO_VERTICAL_MARGIN;
    }

    private int getPlayerBaseTop() {
        return getPlayerClusterTop() + 20;
    }

    private int getPlayerBaseBottom() {
        return getPlayerClusterBottom() - 20;
    }

    private int getEnemyBaseTop() {
        return getEnemyClusterTop() + 20;
    }

    private int getEnemyBaseBottom() {
        return getEnemyClusterBottom() - 20;
    }

    private double getPlayerBaseCenterY() {
        return getPlayerClusterCenterY();
    }

    private double getEnemyBaseCenterY() {
        return getEnemyClusterCenterY();
    }

    private double getPortalProgress(boolean playerPortal) {
        int breaches = playerPortal ? playerPortalBreaches : enemyPortalBreaches;
        return Math.min(1.0, Math.max(0.0, breaches / (double) PORTAL_BREACH_THRESHOLD));
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
        if (!shouldAllowPlayerLaneSwitch()) {
            int lane = clampPlayerLaneIndex(heroLaneIndex);
            double laneMin = getPlayerLaneTopLimit(lane);
            double laneMax = getPlayerLaneBottomLimit(lane);
            min = Math.max(min, laneMin);
            max = Math.min(max, laneMax);
        }
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
        if (!shouldAllowEnemyLaneSwitch()) {
            int lane = clampEnemyLaneIndex(enemyLaneIndex);
            double laneMin = getEnemyLaneTopLimit(lane);
            double laneMax = getEnemyLaneBottomLimit(lane);
            min = Math.max(min, laneMin);
            max = Math.min(max, laneMax);
        }
        if (max < min) {
            return min;
        }
        return clamp(value, min, max);
    }

    private void updateHeroTargetFromMouse(int mouseX, int mouseY) {
        if (!heroAlive || paused || gameOver) {
            return;
        }
        double worldX = screenToWorldX(mouseX);
        double worldY = screenToWorldY(mouseY);
        double targetX = worldX - HERO_WIDTH / 2.0;
        double targetY = worldY - HERO_WIDTH / 2.0;
        double candidateX = clampHorizontalTarget(targetX);
        int desiredLane = resolvePlayerLaneIndex(worldY);
        if (desiredLane != heroLaneIndex && !shouldAllowPlayerLaneSwitch(candidateX)) {
            candidateX = clampHorizontalTarget(getPlayerSwitchAnchorX() - HERO_WIDTH / 2.0);
        }
        heroTargetX = candidateX;
        heroTargetY = clampHeroVerticalTarget(targetY);
        refreshHeroLaneSelectionFromTarget();
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
        double candidateY = heroTargetY + delta;
        int desiredLane = resolvePlayerLaneIndex(candidateY + HERO_WIDTH / 2.0);
        if (desiredLane != heroLaneIndex && !shouldAllowPlayerLaneSwitch(heroTargetX)) {
            heroTargetX = clampHorizontalTarget(getPlayerSwitchAnchorX() - HERO_WIDTH / 2.0);
        }
        heroTargetY = clampHeroVerticalTarget(candidateY);
        refreshHeroLaneSelectionFromTarget();
    }

    private void updateEnemyTargetFromMouse(int mouseX, int mouseY) {
        if (!enemyIsHuman || !enemyAlive || paused || gameOver) {
            return;
        }
        double worldX = screenToWorldX(mouseX);
        double worldY = screenToWorldY(mouseY);
        double targetX = worldX - HERO_WIDTH / 2.0;
        double targetY = worldY - HERO_WIDTH / 2.0;
        double candidateX = clampHorizontalTarget(targetX);
        int desiredLane = resolveEnemyLaneIndex(worldY);
        if (desiredLane != enemyLaneIndex && !shouldAllowEnemyLaneSwitch(candidateX)) {
            candidateX = clampHorizontalTarget(getEnemySwitchAnchorX() - HERO_WIDTH / 2.0);
        }
        enemyTargetX = candidateX;
        enemyTargetY = clampEnemyVerticalTarget(targetY);
        if (shouldAllowEnemyLaneSwitch(enemyTargetX)) {
            enemyLaneIndex = clampEnemyLaneIndex(desiredLane);
        }
    }

    private void adjustEnemyTargetX(double delta) {
        if (!enemyIsHuman || !enemyAlive || paused || gameOver) {
            return;
        }
        enemyTargetX = clampHorizontalTarget(enemyTargetX + delta);
    }

    private void adjustEnemyTargetY(double delta) {
        if (!enemyIsHuman || !enemyAlive || paused || gameOver) {
            return;
        }
        double candidateY = enemyTargetY + delta;
        int desiredLane = resolveEnemyLaneIndex(candidateY + HERO_WIDTH / 2.0);
        if (desiredLane != enemyLaneIndex && !shouldAllowEnemyLaneSwitch(enemyTargetX)) {
            enemyTargetX = clampHorizontalTarget(getEnemySwitchAnchorX() - HERO_WIDTH / 2.0);
        }
        enemyTargetY = clampEnemyVerticalTarget(candidateY);
        if (shouldAllowEnemyLaneSwitch(enemyTargetX)) {
            enemyLaneIndex = clampEnemyLaneIndex(desiredLane);
        }
    }

    private void refreshHeroLaneSelectionFromTarget() {
        if (shouldAllowPlayerLaneSwitch()) {
            heroLaneIndex = clampPlayerLaneIndex(resolvePlayerLaneIndex(heroTargetY + HERO_WIDTH / 2.0));
        }
    }

    private void updateHeroLaneLock() {
        if (!heroAlive) {
            return;
        }
        double heroCenterX = heroX + HERO_WIDTH / 2.0;
        if (!isInPlayerSwitchZone(heroCenterX)) {
            heroLaneIndex = clampPlayerLaneIndex(resolvePlayerLaneIndex(heroY + HERO_WIDTH / 2.0));
            heroTargetY = clampHeroVerticalTarget(heroTargetY);
        }
    }

    private void updateEnemyLaneLock() {
        if (!enemyAlive) {
            return;
        }
        double enemyCenterX = enemyX + HERO_WIDTH / 2.0;
        if (!isInEnemySwitchZone(enemyCenterX)) {
            enemyLaneIndex = clampEnemyLaneIndex(resolveEnemyLaneIndex(enemyY + HERO_WIDTH / 2.0));
            enemyTargetY = clampEnemyVerticalTarget(enemyTargetY);
        }
    }

    private void panCamera(double deltaX, double deltaY) {
        cameraX = clamp(cameraX + deltaX, 0, getMaxCameraX());
        cameraY = clamp(cameraY + deltaY, 0, getMaxCameraY());
        battlefieldPanel.repaint();
    }

    private void centerCameraOn(double worldX, double worldY) {
        double viewWidth = getCurrentViewWidthInWorldUnits();
        double viewHeight = getCurrentViewHeightInWorldUnits();
        int viewWidth = getCurrentViewWidth();
        int viewHeight = getCurrentViewHeight();
        double targetX = worldX - viewWidth / 2.0;
        double targetY = worldY - viewHeight / 2.0;
        cameraX = clamp(targetX, 0, getMaxCameraX());
        cameraY = clamp(targetY, 0, getMaxCameraY());
        battlefieldPanel.repaint();
    }

    private void resetCamera() {
        cameraZoom = clamp(1.0, CAMERA_MIN_ZOOM, CAMERA_MAX_ZOOM);
        centerCameraOn(heroX + HERO_WIDTH / 2.0, heroY + HERO_WIDTH / 2.0);
    }

    private void ensureCameraWithinBounds() {
        double newCameraX = clamp(cameraX, 0, getMaxCameraX());
        double newCameraY = clamp(cameraY, 0, getMaxCameraY());
        if (newCameraX != cameraX || newCameraY != cameraY) {
            cameraX = newCameraX;
            cameraY = newCameraY;
            battlefieldPanel.repaint();
        }
    }

    private int getCurrentViewWidth() {
        int width = battlefieldPanel.getWidth();
        if (width <= 0) {
            width = battlefieldPanel.getPreferredSize().width;
        }
        return Math.max(1, width);
    }

    private int getCurrentViewHeight() {
        int height = battlefieldPanel.getHeight();
        if (height <= 0) {
            height = battlefieldPanel.getPreferredSize().height;
        }
        return Math.max(1, height);
    }

    private double getCurrentViewWidthInWorldUnits() {
        return getCurrentViewWidth() / cameraZoom;
    }

    private double getCurrentViewHeightInWorldUnits() {
        return getCurrentViewHeight() / cameraZoom;
    }

    private double getMaxCameraX() {
        return Math.max(0, WORLD_WIDTH - getCurrentViewWidthInWorldUnits());
    }

    private double getMaxCameraY() {
        return Math.max(0, WORLD_HEIGHT - getCurrentViewHeightInWorldUnits());
    }

    private double screenToWorldX(double screenX) {
        return cameraX + screenX / cameraZoom;
    }

    private double screenToWorldY(double screenY) {
        return cameraY + screenY / cameraZoom;
    }

    private double getCameraCenterX() {
        return cameraX + getCurrentViewWidthInWorldUnits() / 2.0;
    }

    private double getCameraCenterY() {
        return cameraY + getCurrentViewHeightInWorldUnits() / 2.0;
    }

    private void adjustCameraZoom(double delta) {
        adjustCameraZoomAtPoint(delta, getCameraCenterX(), getCameraCenterY());
    }

    private void adjustCameraZoomAtPoint(double delta, double pivotWorldX, double pivotWorldY) {
        setCameraZoom(cameraZoom + delta, pivotWorldX, pivotWorldY);
    }

    private void setCameraZoom(double targetZoom, double pivotWorldX, double pivotWorldY) {
        double clampedZoom = clamp(targetZoom, CAMERA_MIN_ZOOM, CAMERA_MAX_ZOOM);
        cameraZoom = clampedZoom;
        double viewWidth = getCurrentViewWidthInWorldUnits();
        double viewHeight = getCurrentViewHeightInWorldUnits();
        double desiredX = pivotWorldX - viewWidth / 2.0;
        double desiredY = pivotWorldY - viewHeight / 2.0;
        cameraX = clamp(desiredX, 0, getMaxCameraX());
        cameraY = clamp(desiredY, 0, getMaxCameraY());
        battlefieldPanel.repaint();
    private double getMaxCameraX() {
        return Math.max(0, WORLD_WIDTH - getCurrentViewWidth());
    }

    private double getMaxCameraY() {
        return Math.max(0, WORLD_HEIGHT - getCurrentViewHeight());
    }

    private double screenToWorldX(double screenX) {
        return cameraX + screenX;
    }

    private double screenToWorldY(double screenY) {
        return cameraY + screenY;
    }

    private boolean shouldAllowPlayerLaneSwitch() {
        return shouldAllowPlayerLaneSwitch(heroTargetX);
    }

    private boolean shouldAllowPlayerLaneSwitch(double candidateTargetX) {
        if (LOCK_HERO_TO_LANE) {
            return false;
        }
        double heroCenterX = heroX + HERO_WIDTH / 2.0;
        double candidateCenterX = candidateTargetX + HERO_WIDTH / 2.0;
        return isInPlayerSwitchZone(heroCenterX) || isInPlayerSwitchZone(candidateCenterX);
    }

    private boolean shouldAllowEnemyLaneSwitch() {
        return shouldAllowEnemyLaneSwitch(enemyTargetX);
    }

    private boolean shouldAllowEnemyLaneSwitch(double candidateTargetX) {
        if (LOCK_ENEMY_TO_LANE) {
            return false;
        }
        double enemyCenterX = enemyX + HERO_WIDTH / 2.0;
        double candidateCenterX = candidateTargetX + HERO_WIDTH / 2.0;
        return isInEnemySwitchZone(enemyCenterX) || isInEnemySwitchZone(candidateCenterX);
    }

    private boolean isInPlayerSwitchZone(double centerX) {
        return centerX <= getPlayerBaseX() + BASE_WIDTH + LANE_SWITCH_ZONE_WIDTH;
    }

    private boolean isInEnemySwitchZone(double centerX) {
        return centerX >= getEnemyBaseX() - LANE_SWITCH_ZONE_WIDTH;
    }

    private double getPlayerSwitchAnchorX() {
        return getPlayerBaseX() + BASE_WIDTH + LANE_SWITCH_ZONE_WIDTH / 2.0;
    }

    private double getEnemySwitchAnchorX() {
        return getEnemyBaseX() - LANE_SWITCH_ZONE_WIDTH / 2.0;
    }

    private int resolvePlayerLaneIndex(double centerY) {
        int lane = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < LANES_PER_SIDE; i++) {
            double laneCenter = getPlayerLaneCenterY(i);
            double distance = Math.abs(centerY - laneCenter);
            if (distance < bestDistance) {
                bestDistance = distance;
                lane = i;
            }
        }
        return lane;
    }

    private int resolveEnemyLaneIndex(double centerY) {
        int lane = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < LANES_PER_SIDE; i++) {
            double laneCenter = getEnemyLaneCenterY(i);
            double distance = Math.abs(centerY - laneCenter);
            if (distance < bestDistance) {
                bestDistance = distance;
                lane = i;
            }
        }
        return lane;
    }

    private int clampPlayerLaneIndex(int laneIndex) {
        return Math.max(0, Math.min(LANES_PER_SIDE - 1, laneIndex));
    }

    private int clampEnemyLaneIndex(int laneIndex) {
        return Math.max(0, Math.min(LANES_PER_SIDE - 1, laneIndex));
    }

    private double getPlayerLaneTopLimit(int laneIndex) {
        double top = getPlayerLaneTop(laneIndex) + HERO_VERTICAL_MARGIN;
        return Math.max(top, getPlayerHeroTopLimit());
    }

    private double getPlayerLaneBottomLimit(int laneIndex) {
        double bottom = getPlayerLaneTop(laneIndex) + getLaneHeight() - HERO_WIDTH - HERO_VERTICAL_MARGIN;
        return Math.min(bottom, getPlayerHeroBottomLimit());
    }

    private double getEnemyLaneTopLimit(int laneIndex) {
        double top = getEnemyLaneTop(laneIndex) + HERO_VERTICAL_MARGIN;
        return Math.max(top, getEnemyHeroTopLimit());
    }

    private double getEnemyLaneBottomLimit(int laneIndex) {
        double bottom = getEnemyLaneTop(laneIndex) + getLaneHeight() - HERO_WIDTH - HERO_VERTICAL_MARGIN;
        return Math.min(bottom, getEnemyHeroBottomLimit());
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
            setBackground(new Color(8, 12, 18));
            setFocusable(true);

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleClick(e);
                    requestFocusInWindow();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    handleDrag(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleRelease();
                }
            };
            addMouseListener(adapter);
            addMouseMotionListener(adapter);
            addMouseWheelListener(e -> {
                double rotation = e.getPreciseWheelRotation();
                if (rotation == 0) {
                    return;
                }
                double pivotX = screenToWorldX(e.getX());
                double pivotY = screenToWorldY(e.getY());
                if (rotation < 0) {
                    adjustCameraZoomAtPoint(CAMERA_ZOOM_STEP, pivotX, pivotY);
                } else {
                    adjustCameraZoomAtPoint(-CAMERA_ZOOM_STEP, pivotX, pivotY);
                }
            });
            setupKeyBindings();
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    ensureCameraWithinBounds();
                }
            });
        }

        private void handleClick(MouseEvent e) {
            if (gameOver || paused) {
                return;
            }
            if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                if (!heroAlive) {
                    return;
                }
                updateHeroTargetFromMouse(e.getX(), e.getY());
            } else if (javax.swing.SwingUtilities.isRightMouseButton(e) && enemyIsHuman) {
                if (!enemyAlive) {
                    return;
                }
                updateEnemyTargetFromMouse(e.getX(), e.getY());
            }
        }

        private void handleDrag(MouseEvent e) {
            if (gameOver || paused) {
                return;
            }
            if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                if (!heroAlive) {
                    return;
                }
                updateHeroTargetFromMouse(e.getX(), e.getY());
            } else if (javax.swing.SwingUtilities.isRightMouseButton(e) && enemyIsHuman) {
                if (!enemyAlive) {
                    return;
                }
                updateEnemyTargetFromMouse(e.getX(), e.getY());
            }
        }

        private void handleRelease() {
            // No-op for now but kept for clarity and future interactions.
        }

        private void setupKeyBindings() {
            javax.swing.InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            javax.swing.ActionMap actionMap = getActionMap();

            javax.swing.KeyStroke left = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, 0);
            javax.swing.KeyStroke right = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, 0);
            javax.swing.KeyStroke up = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, 0);
            javax.swing.KeyStroke down = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, 0);
            javax.swing.KeyStroke enemyLeft = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, 0);
            javax.swing.KeyStroke enemyRight = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, 0);
            javax.swing.KeyStroke enemyUp = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, 0);
            javax.swing.KeyStroke enemyDown = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, 0);
            javax.swing.KeyStroke pauseKey = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
            javax.swing.KeyStroke pauseKeyAlt = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, 0);
            javax.swing.KeyStroke panLeft = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, 0);
            javax.swing.KeyStroke panRight = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, 0);
            javax.swing.KeyStroke panUp = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, 0);
            javax.swing.KeyStroke panDown = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, 0);
            javax.swing.KeyStroke centerKey = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, 0);
            javax.swing.KeyStroke panLeftShift = javax.swing.KeyStroke
                    .getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK);
            javax.swing.KeyStroke panRightShift = javax.swing.KeyStroke
                    .getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK);
            javax.swing.KeyStroke panUpShift = javax.swing.KeyStroke
                    .getKeyStroke(java.awt.event.KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK);
            javax.swing.KeyStroke panDownShift = javax.swing.KeyStroke
                    .getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK);
            javax.swing.KeyStroke zoomIn = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_EQUALS, 0);
            javax.swing.KeyStroke zoomInShift = javax.swing.KeyStroke
                    .getKeyStroke(java.awt.event.KeyEvent.VK_EQUALS, InputEvent.SHIFT_DOWN_MASK);
            javax.swing.KeyStroke zoomInNumpad = javax.swing.KeyStroke
                    .getKeyStroke(java.awt.event.KeyEvent.VK_ADD, 0);
            javax.swing.KeyStroke zoomOut = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS, 0);
            javax.swing.KeyStroke zoomOutShift = javax.swing.KeyStroke
                    .getKeyStroke(java.awt.event.KeyEvent.VK_MINUS, InputEvent.SHIFT_DOWN_MASK);
            javax.swing.KeyStroke zoomOutNumpad = javax.swing.KeyStroke
                    .getKeyStroke(java.awt.event.KeyEvent.VK_SUBTRACT, 0);

            inputMap.put(left, "moveLeft");
            inputMap.put(right, "moveRight");
            inputMap.put(up, "moveUp");
            inputMap.put(down, "moveDown");
            inputMap.put(enemyLeft, "enemyMoveLeft");
            inputMap.put(enemyRight, "enemyMoveRight");
            inputMap.put(enemyUp, "enemyMoveUp");
            inputMap.put(enemyDown, "enemyMoveDown");
            inputMap.put(pauseKey, "pause");
            inputMap.put(pauseKeyAlt, "pause");
            inputMap.put(panLeft, "cameraLeft");
            inputMap.put(panRight, "cameraRight");
            inputMap.put(panUp, "cameraUp");
            inputMap.put(panDown, "cameraDown");
            inputMap.put(panLeftShift, "cameraLeft");
            inputMap.put(panRightShift, "cameraRight");
            inputMap.put(panUpShift, "cameraUp");
            inputMap.put(panDownShift, "cameraDown");
            inputMap.put(centerKey, "cameraCenter");
            inputMap.put(zoomIn, "cameraZoomIn");
            inputMap.put(zoomInShift, "cameraZoomIn");
            inputMap.put(zoomInNumpad, "cameraZoomIn");
            inputMap.put(zoomOut, "cameraZoomOut");
            inputMap.put(zoomOutShift, "cameraZoomOut");
            inputMap.put(zoomOutNumpad, "cameraZoomOut");
            inputMap.put(centerKey, "cameraCenter");

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
            actionMap.put("enemyMoveLeft", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (enemyIsHuman) {
                        adjustEnemyTargetX(-60);
                    } else {
                        panCamera(-CAMERA_PAN_DELTA, 0);
                    }
                }
            });
            actionMap.put("enemyMoveRight", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (enemyIsHuman) {
                        adjustEnemyTargetX(60);
                    } else {
                        panCamera(CAMERA_PAN_DELTA, 0);
                    }
                }
            });
            actionMap.put("enemyMoveUp", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (enemyIsHuman) {
                        adjustEnemyTargetY(-60);
                    } else {
                        panCamera(0, -CAMERA_PAN_DELTA);
                    }
                }
            });
            actionMap.put("enemyMoveDown", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (enemyIsHuman) {
                        adjustEnemyTargetY(60);
                    } else {
                        panCamera(0, CAMERA_PAN_DELTA);
                    }
                }
            });
            actionMap.put("cameraLeft", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    panCamera(-CAMERA_PAN_DELTA, 0);
                }
            });
            actionMap.put("cameraRight", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    panCamera(CAMERA_PAN_DELTA, 0);
                }
            });
            actionMap.put("cameraUp", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    panCamera(0, -CAMERA_PAN_DELTA);
                }
            });
            actionMap.put("cameraDown", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    panCamera(0, CAMERA_PAN_DELTA);
                }
            });
            actionMap.put("cameraCenter", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    centerCameraOn(heroX + HERO_WIDTH / 2.0, heroY + HERO_WIDTH / 2.0);
                }
            });
            actionMap.put("cameraZoomIn", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    adjustCameraZoom(CAMERA_ZOOM_STEP);
                }
            });
            actionMap.put("cameraZoomOut", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    adjustCameraZoom(-CAMERA_ZOOM_STEP);
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

            int viewWidth = getWidth();
            if (viewWidth <= 0) {
                viewWidth = getPreferredSize().width;
            }
            int viewHeight = getHeight();
            if (viewHeight <= 0) {
                viewHeight = getPreferredSize().height;
            }
            }
            int viewHeight = getHeight();
            if (viewHeight <= 0) {
                viewHeight = getPreferredSize().height;
            }
            GradientPaint background = new GradientPaint(0, 0, new Color(16, 28, 32), 0, viewHeight,
                    new Color(6, 14, 18));
            g2.setPaint(background);
            g2.fillRect(0, 0, viewWidth, viewHeight);
            g2.setPaint(null);

            AffineTransform originalTransform = g2.getTransform();
            g2.scale(cameraZoom, cameraZoom);
            g2.translate(-cameraX, -cameraY);

            int width = WORLD_WIDTH;
            int height = WORLD_HEIGHT;
            int laneHeight = getLaneHeight();
            int clusterHeight = getClusterHeight();
            int playerClusterTop = getPlayerClusterTop();
            int enemyClusterTop = getEnemyClusterTop();

            drawSwitchZones(g2, width, playerClusterTop, enemyClusterTop, clusterHeight);
            for (int lane = 0; lane < LANES_PER_SIDE; lane++) {
                drawLaneSurface(g2, width, getPlayerLaneTop(lane), laneHeight);
            }
            for (int lane = 0; lane < LANES_PER_SIDE; lane++) {
                drawLaneSurface(g2, width, getEnemyLaneTop(lane), laneHeight);
            }

            drawLaneWalls(g2, width, laneHeight);

            drawLaneConnector(g2, getPlayerBaseX() + BASE_WIDTH, playerClusterTop, clusterHeight);
            drawLaneConnector(g2, getEnemyBaseX() - LANE_CONNECTOR_WIDTH, enemyClusterTop, clusterHeight);

            drawBase(g2, getPlayerBaseX(), playerClusterTop, clusterHeight, new Color(66, 135, 245));
            drawBase(g2, getEnemyBaseX(), enemyClusterTop, clusterHeight, new Color(200, 70, 70));
            drawSpawnZone(g2, getPlayerSpawnX() + HERO_WIDTH / 2.0, getPlayerSpawnY() + HERO_WIDTH / 2.0,
                    new Color(80, 190, 90));
            for (UnitInstance unit : enemyUnits) {
                drawUnit(g2, unit, new Color(214, 68, 68));
            }
            if (playerHero != null) {
                if (heroAlive) {
                    int heroDrawX = (int) Math.round(heroX);
                    int heroDrawY = (int) Math.round(heroY);
                    drawHeroRange(g2, heroDrawX + HERO_WIDTH / 2, heroDrawY + HERO_WIDTH / 2,
                            playerHero.getAttackRangePixels(), new Color(64, 144, 255, 90));
                    drawHero(g2, heroDrawX, heroDrawY, playerHero.getName(),
                            playerHero.getCurrentHealth(), playerHero.getMaxHealth(), new Color(64, 144, 255));
                } else if (heroRespawnTimer > 0) {
                    drawRespawnIndicator(g2, (int) Math.round(heroX), (int) Math.round(heroY), heroRespawnTimer);
                }
            }

            drawSpawnZone(g2, getEnemySpawnX() + HERO_WIDTH / 2.0, getEnemySpawnY() + HERO_WIDTH / 2.0,
                    new Color(80, 190, 90));
            for (UnitInstance unit : playerUnits) {
                drawUnit(g2, unit, new Color(64, 144, 255));
            }
            if (aiHero != null) {
                if (enemyAlive) {
                    int enemyDrawX = (int) Math.round(enemyX);
                    int enemyDrawY = (int) Math.round(enemyY);
                    drawHeroRange(g2, enemyDrawX + HERO_WIDTH / 2, enemyDrawY + HERO_WIDTH / 2,
                            aiHero.getAttackRangePixels(), new Color(214, 68, 68, 90));
                    drawHero(g2, enemyDrawX, enemyDrawY, aiHero.getName(),
                            aiHero.getCurrentHealth(), aiHero.getMaxHealth(), new Color(214, 68, 68));
                } else if (enemyRespawnTimer > 0) {
                    drawRespawnIndicator(g2, (int) Math.round(enemyX), (int) Math.round(enemyY), enemyRespawnTimer);
                }
            }

            for (Projectile projectile : projectiles) {
                drawProjectile(g2, projectile);
            }

            g2.setTransform(originalTransform);

            drawQueuedUnitsOverlay(g2, viewWidth);
            if (paused) {
                drawPauseOverlay(g2, viewWidth, viewHeight);
            }

            g2.dispose();
        }

        private int projectX(int x, int y, int width, int height) {
            double center = width / 2.0;
            double normalized = height <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, y / (double) height));
            double scale = 0.7 + 0.3 * normalized;
            return (int) Math.round(center + (x - center) * scale);
        }

        private void drawSwitchZones(Graphics2D g2, int width, int playerClusterTop, int enemyClusterTop, int clusterHeight) {
            int playerZoneLeft = Math.max(0, getPlayerBaseX() - 16);
            int playerZoneWidth = BASE_WIDTH + LANE_SWITCH_ZONE_WIDTH + 32;
            Color playerColor = new Color(60, 120, 160, 90);
            g2.setColor(playerColor);
            g2.fillRoundRect(playerZoneLeft, playerClusterTop, playerZoneWidth, clusterHeight, 28, 28);
            java.awt.Stroke previous = g2.getStroke();
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(110, 170, 210, 120));
            g2.drawRoundRect(playerZoneLeft, playerClusterTop, playerZoneWidth, clusterHeight, 28, 28);

            int enemyZoneLeft = getEnemyBaseX() - LANE_SWITCH_ZONE_WIDTH - 16;
            int enemyZoneWidth = BASE_WIDTH + LANE_SWITCH_ZONE_WIDTH + 32;
            Color enemyColor = new Color(160, 80, 80, 90);
            g2.setColor(enemyColor);
            g2.fillRoundRect(enemyZoneLeft, enemyClusterTop, enemyZoneWidth, clusterHeight, 28, 28);
            g2.setColor(new Color(210, 120, 120, 120));
            g2.drawRoundRect(enemyZoneLeft, enemyClusterTop, enemyZoneWidth, clusterHeight, 28, 28);
            g2.setStroke(previous);
        }

        private void drawLaneSurface(Graphics2D g2, int width, int laneTop, int laneHeight) {
            int height = WORLD_HEIGHT;
            int leftBoundary = getPlayerBaseX() + BASE_WIDTH + 18;
            int rightBoundary = getEnemyBaseX() - 18;
            int topY = laneTop;
            int bottomY = laneTop + laneHeight;

            int leftTop = projectX(leftBoundary, topY, width, height);
            int rightTop = projectX(rightBoundary, topY, width, height);
            int leftBottom = projectX(leftBoundary, bottomY, width, height);
            int rightBottom = projectX(rightBoundary, bottomY, width, height);

            java.awt.geom.Path2D laneShape = new java.awt.geom.Path2D.Double();
            laneShape.moveTo(leftTop, topY);
            laneShape.lineTo(rightTop, topY);
            laneShape.lineTo(rightBottom, bottomY);
            laneShape.lineTo(leftBottom, bottomY);
            laneShape.closePath();

            GradientPaint paint = new GradientPaint(leftTop, topY, new Color(46, 66, 72),
                    leftBottom, bottomY, new Color(18, 26, 32));
            g2.setPaint(paint);
            g2.fill(laneShape);

            GradientPaint highlight = new GradientPaint(leftTop, topY, new Color(80, 110, 120, 120),
                    leftTop, topY + laneHeight / 3, new Color(0, 0, 0, 0));
            g2.setPaint(highlight);
            g2.fill(laneShape);

            g2.setPaint(null);
            g2.setColor(new Color(20, 30, 36, 180));
            g2.setStroke(new BasicStroke(3f));
            g2.draw(laneShape);

            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(32, 48, 54, 160));
            int segments = 4;
            for (int i = 1; i < segments; i++) {
                double progress = i / (double) segments;
                int sampleY = (int) Math.round(topY + laneHeight * progress);
                int leftX = projectX(leftBoundary, sampleY, width, height);
                int rightX = projectX(rightBoundary, sampleY, width, height);
                g2.drawLine(leftX, sampleY, rightX, sampleY);
            }

            java.awt.geom.Path2D leftShadow = new java.awt.geom.Path2D.Double();
            leftShadow.moveTo(leftTop, topY);
            leftShadow.lineTo(leftBottom, bottomY);
            leftShadow.lineTo(leftBottom - 26, bottomY + 22);
            leftShadow.lineTo(leftTop - 26, topY + 22);
            leftShadow.closePath();
            g2.setColor(new Color(8, 12, 16, 160));
            g2.fill(leftShadow);

            java.awt.geom.Path2D rightShadow = new java.awt.geom.Path2D.Double();
            rightShadow.moveTo(rightTop, topY);
            rightShadow.lineTo(rightBottom, bottomY);
            rightShadow.lineTo(rightBottom + 26, bottomY + 22);
            rightShadow.lineTo(rightTop + 26, topY + 22);
            rightShadow.closePath();
            g2.setColor(new Color(6, 8, 12, 180));
            g2.fill(rightShadow);
        }

        private void drawLaneWalls(Graphics2D g2, int width, int laneHeight) {
            int wallThickness = Math.max(12, INTRA_LANE_GAP - 6);
            int wallArc = Math.min(18, wallThickness);
            g2.setColor(new Color(20, 28, 40, 220));

            int playerWallStart = getPlayerBaseX() + BASE_WIDTH + LANE_SWITCH_ZONE_WIDTH - 8;
            int playerWallWidth = width - playerWallStart - 36;
            for (int lane = 0; lane < LANES_PER_SIDE - 1; lane++) {
                int gapTop = getPlayerLaneTop(lane) + laneHeight;
                int wallY = gapTop + Math.max(2, (INTRA_LANE_GAP - wallThickness) / 2);
                if (playerWallWidth > 0) {
                    g2.fillRoundRect(playerWallStart, wallY, playerWallWidth, wallThickness, wallArc, wallArc);
                }
            }

            int enemyWallWidth = getEnemyBaseX() - LANE_SWITCH_ZONE_WIDTH - 24;
            for (int lane = 0; lane < LANES_PER_SIDE - 1; lane++) {
                int gapTop = getEnemyLaneTop(lane) + laneHeight;
                int wallY = gapTop + Math.max(2, (INTRA_LANE_GAP - wallThickness) / 2);
                if (enemyWallWidth > 40) {
                    g2.fillRoundRect(24, wallY, enemyWallWidth, wallThickness, wallArc, wallArc);
                }
            }
        }

        private void drawLaneConnector(Graphics2D g2, int connectorLeftX, int clusterTop, int clusterHeight) {
            int connectorY = clusterTop + 18;
            int connectorHeight = Math.max(20, clusterHeight - 36);
            GradientPaint paint = new GradientPaint(connectorLeftX, connectorY, new Color(46, 62, 70),
                    connectorLeftX, connectorY + connectorHeight, new Color(32, 44, 52));
            g2.setPaint(paint);
            g2.fillRoundRect(connectorLeftX, connectorY, LANE_CONNECTOR_WIDTH, connectorHeight, 20, 20);
            g2.setPaint(null);
            g2.setColor(new Color(80, 110, 140, 140));
            g2.drawRoundRect(connectorLeftX, connectorY, LANE_CONNECTOR_WIDTH, connectorHeight, 20, 20);
            g2.setColor(new Color(24, 36, 44));
            for (int x = connectorLeftX + 6; x < connectorLeftX + LANE_CONNECTOR_WIDTH - 6; x += 22) {
                g2.fillRoundRect(x, connectorY + connectorHeight / 2 - 3, 14, 6, 6, 6);
            }
        }

        private void drawProjectile(Graphics2D g2, Projectile projectile) {
            ProjectileType type = projectile.getType();
            int drawX = (int) Math.round(projectile.getX());
            int drawY = (int) Math.round(projectile.getY());
            g2.setColor(type.getTrailColor());
            int trailSize = Math.max(2, type.getHitRadius());
            g2.fillOval(drawX - trailSize / 2, drawY - trailSize / 2, trailSize, trailSize);
            g2.setColor(type.getPrimaryColor());
            double angle = projectile.getAngle();
            int length = type.getLength();
            int endX = (int) Math.round(drawX + Math.cos(angle) * length);
            int endY = (int) Math.round(drawY + Math.sin(angle) * length);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(drawX, drawY, endX, endY);
            int headSize = Math.max(4, type.getHitRadius() + 2);
            g2.fillOval(endX - headSize / 2, endY - headSize / 2, headSize, headSize);
        }

        private void drawBase(Graphics2D g2, int baseX, int clusterTop, int clusterHeight, Color color) {
            int baseY = clusterTop + 20;
            int baseHeight = clusterHeight - 40;
            GradientPaint glow = new GradientPaint(baseX, baseY, color.brighter(), baseX, baseY + baseHeight,
                    color.darker());
            g2.setPaint(glow);
            g2.fillRoundRect(baseX - 6, baseY - 6, BASE_WIDTH + 12, baseHeight + 12, 18, 18);
            g2.setPaint(null);
            g2.setColor(new Color(28, 36, 48));
            g2.fillRoundRect(baseX, baseY, BASE_WIDTH, baseHeight, 18, 18);

            boolean playerPortal = baseX <= BASE_MARGIN + 1;
            double progress = getPortalProgress(playerPortal);
            int portalDiameter = Math.min(BASE_WIDTH - 18, baseHeight - 30);
            int portalX = baseX + (BASE_WIDTH - portalDiameter) / 2;
            int portalY = baseY + (baseHeight - portalDiameter) / 2;

            GradientPaint portalPaint = new GradientPaint(portalX, portalY, color.brighter(), portalX + portalDiameter,
                    portalY + portalDiameter, color.darker());
            g2.setPaint(portalPaint);
            g2.fillOval(portalX, portalY, portalDiameter, portalDiameter);
            g2.setPaint(null);

            int innerDiameter = (int) Math.round(portalDiameter * 0.55);
            int innerOffset = (portalDiameter - innerDiameter) / 2;
            g2.setColor(new Color(12, 18, 28, 220));
            g2.fillOval(portalX + innerOffset, portalY + innerOffset, innerDiameter, innerDiameter);

            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
            g2.drawOval(portalX, portalY, portalDiameter, portalDiameter);

            int ringDiameter = portalDiameter + 14;
            int ringX = portalX - 7;
            int ringY = portalY - 7;
            g2.setColor(new Color(255, 255, 255, 70));
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(ringX, ringY, ringDiameter, ringDiameter);

            if (progress > 0) {
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 220));
                int arcAngle = (int) Math.round(progress * -360);
                g2.drawArc(ringX, ringY, ringDiameter, ringDiameter, 90, arcAngle);
            }

            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(255, 255, 255, 160));
            for (int i = 0; i < 3; i++) {
                double angle = (System.currentTimeMillis() / 40.0 + i * 80) % 360;
                double radians = Math.toRadians(angle);
                int swirlRadius = innerDiameter / 2;
                int centerX = portalX + portalDiameter / 2;
                int centerY = portalY + portalDiameter / 2;
                int endX = centerX + (int) Math.round(Math.cos(radians) * swirlRadius * 0.9);
                int endY = centerY + (int) Math.round(Math.sin(radians) * swirlRadius * 0.9);
                g2.drawLine(centerX, centerY, endX, endY);
            }
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
            double ratio = Math.min(1.0, Math.max(0, unit.health) / (double) unit.getMaxHealth());
            g2.setColor(new Color(45, 45, 45));
            g2.fillRoundRect(drawX, drawY - 8, UNIT_SIZE, 6, 6, 6);
            g2.setColor(new Color(80, 210, 100));
            g2.fillRoundRect(drawX, drawY - 8, (int) Math.round(UNIT_SIZE * ratio), 6, 6, 6);
        }

        private void drawHeroRange(Graphics2D g2, int centerX, int centerY, int radius, Color color) {
            int safeRadius = Math.max(12, radius);
            int diameter = safeRadius * 2;
            g2.setColor(color);
            g2.fillOval(centerX - safeRadius, centerY - safeRadius, diameter, diameter);
            g2.setColor(color.darker());
            g2.drawOval(centerX - safeRadius, centerY - safeRadius, diameter, diameter);
        }

        private void drawSpawnZone(Graphics2D g2, double centerX, double centerY, Color color) {
            int radius = 26;
            int diameter = radius * 2;
            int drawX = (int) Math.round(centerX) - radius;
            int drawY = (int) Math.round(centerY) - radius;
            Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 90);
            g2.setColor(fill);
            g2.fillOval(drawX, drawY, diameter, diameter);
            java.awt.Stroke previous = g2.getStroke();
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(color.darker());
            g2.drawOval(drawX, drawY, diameter, diameter);
            g2.setStroke(previous);
        }

        private void drawQueuedUnitsOverlay(Graphics2D g2, int width) {
            if (playerTeam == null) {
                return;
            }
            StringBuilder builder = new StringBuilder();
            java.util.List<UnitType> playerQueued = playerTeam.getQueuedUnitsSnapshot();
            if (playerQueued.isEmpty()) {
                builder.append("P1: None");
            } else {
                builder.append("P1: ");
                for (int i = 0; i < playerQueued.size(); i++) {
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(playerQueued.get(i).getDisplayName());
                }
            }
            if (enemyTeam != null) {
                builder.append("  |  ");
                java.util.List<UnitType> enemyQueued = enemyTeam.getQueuedUnitsSnapshot();
                String enemyName = enemyIsHuman ? "P2" : "AI";
                if (enemyQueued.isEmpty()) {
                    builder.append(enemyName).append(": None");
                } else {
                    builder.append(enemyName).append(": ");
                    for (int i = 0; i < enemyQueued.size(); i++) {
                        if (i > 0) {
                            builder.append(", ");
                        }
                        builder.append(enemyQueued.get(i).getDisplayName());
                    }
                }
            }
            String text = "Next Wave - " + builder;
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
        if (gameOver) {
            return;
        }
        Hero hero = getHeroForIndex(activeHeroIndex);
        Team team = getTeamForIndex(activeHeroIndex);
        if (hero == null || team == null) {
            return;
        }
        UnitBalance balance = getUnitBalance(type);
        if (!hero.spendGold(balance.getCost())) {
            lastActionMessage = String.format("%s lacks the gold to send a %s.",
                    team.getName(), type.getDisplayName());
            refreshHud();
            battlefieldPanel.repaint();
            return;
        }
        hero.addIncome(balance.getIncomeBonus());
        team.queueUnit(type);
        lastActionMessage = String.format("%s queued a %s for the next wave.",
                team.getName(), type.getDisplayName());
        refreshHud();
        battlefieldPanel.repaint();
    }

    private void attemptAiSendUnit() {
        if (gameOver || aiHero == null) {
            return;
        }
        java.util.List<UnitType> affordable = new java.util.ArrayList<>();
        for (UnitType type : UnitType.values()) {
            UnitBalance balance = getUnitBalance(type);
            if (aiHero.getGold() >= balance.getCost()) {
                affordable.add(type);
            }
        }
        if (affordable.isEmpty()) {
            return;
        }
        UnitType choice = affordable.get(random.nextInt(affordable.size()));
        UnitBalance choiceBalance = getUnitBalance(choice);
        aiHero.spendGold(choiceBalance.getCost());
        aiHero.addIncome(choiceBalance.getIncomeBonus());
        enemyTeam.queueUnit(choice);
        battlefieldPanel.repaint();
    }

    private void attemptAttributeUpgrade(Hero.PrimaryAttribute attribute, String attributeName) {
        Hero hero = getHeroForIndex(activeHeroIndex);
        if (gameOver || hero == null) {
            return;
        }
        boolean upgraded = hero.upgradeAttribute(attribute, ATTRIBUTE_UPGRADE_COST);
        if (upgraded) {
            Team team = getTeamForIndex(activeHeroIndex);
            String ownerName = team != null ? team.getName() : hero.getName();
            lastActionMessage = String.format("%s invested %d gold to train %s.",
                    ownerName, ATTRIBUTE_UPGRADE_COST, attributeName.toLowerCase());
        } else {
            lastActionMessage = String.format("Not enough gold to train %s.", attributeName.toLowerCase());
        }
        refreshHud();
        battlefieldPanel.repaint();
    }

    private void launchNextWave() {
        if (gameOver || playerTeam == null || enemyTeam == null) {
            return;
        }
        java.util.List<UnitType> playerWave = playerTeam.drainQueuedUnits();
        java.util.List<UnitType> enemyWave = enemyTeam.drainQueuedUnits();

        int playerLane = nextPlayerLaneIndex;
        for (UnitType type : playerWave) {
            playerUnits.add(createUnitInstance(type, true, playerLane));
            playerLane = (playerLane + 1) % LANES_PER_SIDE;
        }
        nextPlayerLaneIndex = playerLane;
        int enemyLane = nextEnemyLaneIndex;
        for (UnitType type : enemyWave) {
            enemyUnits.add(createUnitInstance(type, false, enemyLane));
            enemyLane = (enemyLane + 1) % LANES_PER_SIDE;
        }
        nextEnemyLaneIndex = enemyLane;

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

    private UnitInstance createUnitInstance(UnitType type, boolean fromPlayer, int laneIndex) {
        double spawnXMin;
        double spawnXMax;
        if (fromPlayer) {
            spawnXMin = getPlayerBaseX() + BASE_WIDTH + 6;
            spawnXMax = Math.min(getEnemyBaseX() - UNIT_SIZE - 12, spawnXMin + 90);
        } else {
            spawnXMax = getEnemyBaseX() - UNIT_SIZE - 6;
            spawnXMin = Math.max(getPlayerBaseX() + BASE_WIDTH + 12, spawnXMax - 90);
        }
        double horizontalSpan = Math.max(0, spawnXMax - spawnXMin);
        double spawnX = spawnXMin + (horizontalSpan <= 0 ? 0 : random.nextDouble() * horizontalSpan);
        double laneTop = fromPlayer ? getEnemyLaneTop(laneIndex) : getPlayerLaneTop(laneIndex);
        int laneHeight = getLaneHeight();
        double topLimit = laneTop + UNIT_VERTICAL_MARGIN;
        double bottomLimit = laneTop + laneHeight - UNIT_SIZE - UNIT_VERTICAL_MARGIN;
        if (bottomLimit < topLimit) {
            bottomLimit = topLimit;
        }
        double verticalSpan = Math.max(0, bottomLimit - topLimit);
        double spawnY = topLimit + (verticalSpan <= 0 ? 0 : random.nextDouble() * verticalSpan);
        return new UnitInstance(type, getUnitBalance(type), spawnX, spawnY, topLimit, bottomLimit, fromPlayer, laneIndex);
        return new UnitInstance(type, spawnX, spawnY, topLimit, bottomLimit, fromPlayer, laneIndex);
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
            for (UnitInstance unit : enemyUnits) {
                if (unit.getLaneIndex() != heroLaneIndex) {
                    continue;
                }
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
                if (unit.getLaneIndex() != enemyLaneIndex) {
                    continue;
                }
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
        for (UnitInstance unit : playerUnits) {
            int laneIndex = unit.getLaneIndex();
            double laneTop = getEnemyLaneTop(laneIndex) + UNIT_VERTICAL_MARGIN;
            double laneBottom = getEnemyLaneTop(laneIndex) + getLaneHeight() - UNIT_SIZE - UNIT_VERTICAL_MARGIN;
            if (laneBottom < laneTop) {
                laneBottom = laneTop;
            }
            unit.updateLaneBounds(laneTop, laneBottom);
            unit.preUpdate();
            unit.advance(UNIT_SPEED, getEnemyBaseX() - UNIT_SIZE);
        }
        for (UnitInstance unit : enemyUnits) {
            int laneIndex = unit.getLaneIndex();
            double laneTop = getPlayerLaneTop(laneIndex) + UNIT_VERTICAL_MARGIN;
            double laneBottom = getPlayerLaneTop(laneIndex) + getLaneHeight() - UNIT_SIZE - UNIT_VERTICAL_MARGIN;
            if (laneBottom < laneTop) {
                laneBottom = laneTop;
            }
            unit.updateLaneBounds(laneTop, laneBottom);
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
                registerPortalBreach(true,
                        String.format("%s slipped through the enemy portal!", unit.getType().getDisplayName()));
                iterator.remove();
                continue;
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
                registerPortalBreach(false,
                        String.format("Enemy %s reached your portal!", unit.getType().getDisplayName()));
                iterator.remove();
                continue;
            }
            if (unit.isDead()) {
                iterator.remove();
            }
        }
    }

    private static final class UnitBalance {
        private int cost;
        private int health;
        private int damage;
        private int incomeBonus;
        private int range;

        private UnitBalance(int cost, int health, int damage, int incomeBonus, int range) {
            setCost(cost);
            setHealth(health);
            setDamage(damage);
            setIncomeBonus(incomeBonus);
            setRange(range);
        }

        static UnitBalance from(UnitType type) {
            return new UnitBalance(type.getCost(), type.getHealth(), type.getDamage(), type.getIncomeBonus(),
                    type.getRange());
        }

        UnitBalance copy() {
            return new UnitBalance(cost, health, damage, incomeBonus, range);
        }

        void apply(UnitBalance other) {
            setCost(other.cost);
            setHealth(other.health);
            setDamage(other.damage);
            setIncomeBonus(other.incomeBonus);
            setRange(other.range);
        }

        int getCost() {
            return cost;
        }

        void setCost(int value) {
            this.cost = Math.max(0, value);
        }

        int getHealth() {
            return health;
        }

        void setHealth(int value) {
            this.health = Math.max(1, value);
        }

        int getDamage() {
            return damage;
        }

        void setDamage(int value) {
            this.damage = Math.max(1, value);
        }

        int getIncomeBonus() {
            return incomeBonus;
        }

        void setIncomeBonus(int value) {
            this.incomeBonus = Math.max(0, value);
        }

        int getRange() {
            return range;
        }

        void setRange(int value) {
            this.range = Math.max(UNIT_SIZE, value);
        }
    }

    private static final class UnitBalanceEditor {
        private final UnitType type;
        private final JPanel panel;
        private final JSpinner costSpinner;
        private final JSpinner healthSpinner;
        private final JSpinner damageSpinner;
        private final JSpinner incomeSpinner;
        private final JSpinner rangeSpinner;

        UnitBalanceEditor(UnitType type) {
            this.type = type;
            this.panel = new JPanel(new BorderLayout(8, 6));
            panel.setOpaque(false);
            panel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(new Color(45, 62, 88)),
                    javax.swing.BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel title = new JLabel(type.getDisplayName());
            title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
            panel.add(title, BorderLayout.NORTH);

            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

            JLabel description = new JLabel(type.getDescription());
            description.setForeground(new Color(200, 210, 230));
            description.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 6, 0));
            center.add(description);

            java.awt.FlowLayout flow = new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 4);
            JPanel row = new JPanel(flow);
            row.setOpaque(false);

            costSpinner = createSpinner(0, 999, 1);
            healthSpinner = createSpinner(1, 1500, 1);
            damageSpinner = createSpinner(1, 600, 1);
            incomeSpinner = createSpinner(0, 250, 1);
            rangeSpinner = createSpinner(UNIT_SIZE, 480, 1);

            row.add(makeLabeledField("Cost", costSpinner));
            row.add(makeLabeledField("Health", healthSpinner));
            row.add(makeLabeledField("Damage", damageSpinner));
            row.add(makeLabeledField("Income", incomeSpinner));
            row.add(makeLabeledField("Range", rangeSpinner));

            center.add(row);
            panel.add(center, BorderLayout.CENTER);
        }

        private static JPanel makeLabeledField(String label, JSpinner spinner) {
            spinner.setPreferredSize(new Dimension(70, spinner.getPreferredSize().height));
            JPanel container = new JPanel(new BorderLayout(4, 0));
            container.setOpaque(false);
            JLabel name = new JLabel(label + ":");
            container.add(name, BorderLayout.WEST);
            container.add(spinner, BorderLayout.CENTER);
            return container;
        }

        private static JSpinner createSpinner(int min, int max, int step) {
            return new JSpinner(new SpinnerNumberModel(min, min, max, Math.max(1, step)));
        }

        void loadFrom(UnitBalance balance) {
            costSpinner.setValue(balance.getCost());
            healthSpinner.setValue(balance.getHealth());
            damageSpinner.setValue(balance.getDamage());
            incomeSpinner.setValue(balance.getIncomeBonus());
            rangeSpinner.setValue(balance.getRange());
        }

        void applyTo(UnitBalance balance) {
            balance.setCost((Integer) costSpinner.getValue());
            balance.setHealth((Integer) healthSpinner.getValue());
            balance.setDamage((Integer) damageSpinner.getValue());
            balance.setIncomeBonus((Integer) incomeSpinner.getValue());
            balance.setRange((Integer) rangeSpinner.getValue());
        }

        JPanel getPanel() {
            return panel;
        }

        UnitType getType() {
            return type;
        }
    }

    private static final class HeroTarget {
        private final HeroLineWarsGame.UnitInstance unit;
        private final boolean heroTarget;
        private final boolean targetEnemyHero;

        private HeroTarget(HeroLineWarsGame.UnitInstance unit, boolean heroTarget, boolean targetEnemyHero) {
            this.unit = unit;
            this.heroTarget = heroTarget;
            this.targetEnemyHero = targetEnemyHero;
        }

        static HeroTarget enemyHero() {
            return new HeroTarget(null, true, true);
        }

        static HeroTarget playerHero() {
            return new HeroTarget(null, true, false);
        }

        static HeroTarget unit(HeroLineWarsGame.UnitInstance unit) {
            return new HeroTarget(unit, false, false);
        }

        boolean isHeroTarget() {
            return heroTarget;
        }

        boolean isTargetingEnemyHero() {
            return targetEnemyHero;
        }

        HeroLineWarsGame.UnitInstance getUnit() {
            return unit;
        }
    }

    private enum ProjectileType {
        ARROW(12.0, 18, 6, new Color(230, 220, 180), new Color(140, 110, 70)),
        MAGIC_BOLT(9.5, 14, 8, new Color(140, 200, 255), new Color(70, 140, 220));

        private final double speed;
        private final int length;
        private final int hitRadius;
        private final Color primaryColor;
        private final Color trailColor;

        ProjectileType(double speed, int length, int hitRadius, Color primaryColor, Color trailColor) {
            this.speed = speed;
            this.length = length;
            this.hitRadius = hitRadius;
            this.primaryColor = primaryColor;
            this.trailColor = trailColor;
        }

        double getSpeed() {
            return speed;
        }

        int getLength() {
            return length;
        }

        int getHitRadius() {
            return hitRadius;
        }

        Color getPrimaryColor() {
            return primaryColor;
        }

        Color getTrailColor() {
            return trailColor;
        }
    }

    private class Projectile {
        private final boolean fromPlayer;
        private final ProjectileType type;
        private final HeroTarget target;
        private final int damage;
        private double x;
        private double y;
        private double angle;

        Projectile(boolean fromPlayer, ProjectileType type, double startX, double startY, Hero attacker, HeroTarget target) {
            this.fromPlayer = fromPlayer;
            this.type = type;
            this.target = target;
            this.x = startX;
            this.y = startY;
            int rolledDamage = Math.max(1, attacker.rollAttackDamage());
            if (target.isHeroTarget()) {
                int defense = fromPlayer ? aiHero.getDefense() : playerHero.getDefense();
                this.damage = Math.max(1, rolledDamage - defense);
            } else {
                this.damage = rolledDamage;
            }
        }

        boolean update() {
            if (!isTargetAlive()) {
                return true;
            }
            double targetX = getTargetX();
            double targetY = getTargetY();
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.hypot(dx, dy);
            if (distance <= type.getHitRadius()) {
                handleProjectileImpact(this);
                return true;
            }
            angle = Math.atan2(dy, dx);
            double step = Math.min(distance, type.getSpeed());
            x += Math.cos(angle) * step;
            y += Math.sin(angle) * step;
            if (Math.hypot(targetX - x, targetY - y) <= type.getHitRadius()) {
                handleProjectileImpact(this);
                return true;
            }
            return false;
        }

        boolean isFromPlayer() {
            return fromPlayer;
        }

        boolean isHeroTarget() {
            return target.isHeroTarget();
        }

        boolean isTargetingEnemyHero() {
            return target.isTargetingEnemyHero();
        }

        HeroLineWarsGame.UnitInstance getTargetUnit() {
            return target.getUnit();
        }

        int getDamage() {
            return damage;
        }

        ProjectileType getType() {
            return type;
        }

        double getX() {
            return x;
        }

        double getY() {
            return y;
        }

        double getAngle() {
            return angle;
        }

        private boolean isTargetAlive() {
            if (isHeroTarget()) {
                return isTargetingEnemyHero() ? enemyAlive : heroAlive;
            }
            HeroLineWarsGame.UnitInstance unit = target.getUnit();
            return unit != null && !unit.isDead();
        }

        private double getTargetX() {
            if (isHeroTarget()) {
                double baseX = isTargetingEnemyHero() ? enemyX : heroX;
                return baseX + HERO_WIDTH / 2.0;
            }
            HeroLineWarsGame.UnitInstance unit = target.getUnit();
            return unit != null ? unit.getCenterX() : x;
        }

        private double getTargetY() {
            if (isHeroTarget()) {
                double baseY = isTargetingEnemyHero() ? enemyY : heroY;
                return baseY + HERO_WIDTH / 2.0;
            }
            HeroLineWarsGame.UnitInstance unit = target.getUnit();
            return unit != null ? unit.getCenterY() : y;
        }
    }

    private class UnitInstance {
        private final UnitType type;
        private final UnitBalance balance;
        private double x;
        private int health;
        private int attackCooldown;
        private boolean engaged;
        private boolean engagedLastTick;
        private final boolean fromPlayer;
        private final int laneIndex;
        private int spawnShield;
        private double y;
        private double targetY;
        private double topLimit;
        private double bottomLimit;
        private double laneCenter;

        UnitInstance(UnitType type, UnitBalance balance, double x, double y, double topLimit, double bottomLimit,
                boolean fromPlayer, int laneIndex) {
            this.type = type;
            this.balance = balance;
            this.x = x;
            this.y = y;
            this.health = balance.getHealth();
            this.attackCooldown = 0;
            this.fromPlayer = fromPlayer;
            this.laneIndex = laneIndex;
            this.spawnShield = SPAWN_SHIELD_TICKS;
            updateLaneBounds(topLimit, bottomLimit);
            this.targetY = clamp(y, this.topLimit, this.bottomLimit);
        }

        int getLaneIndex() {
            return laneIndex;
        }

        void preUpdate() {
            engagedLastTick = engaged;
            engaged = false;
            y = clamp(y, topLimit, bottomLimit);
            targetY = clamp(targetY, topLimit, bottomLimit);
            if (attackCooldown > 0) {
                attackCooldown--;
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
                target.takeDamage(balance.getDamage());
                attackCooldown = UNIT_ATTACK_COOLDOWN_TICKS;
            }
        }

        boolean tryAttackHero(Hero hero) {
            if (attackCooldown <= 0) {
                int damage = Math.max(1, balance.getDamage() - hero.getDefense());
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
            return balance.getRange();
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

        int getMaxHealth() {
            return balance.getHealth();
        }

        void clampHealthToBalance() {
            health = Math.min(health, getMaxHealth());
        }
    }
}
