package com.example.herolinewars;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A small Swing adaptation of the classic Hero Line Wars custom map.
 */
public class HeroLineWarsGame extends JFrame {
    private final Random random = new Random();

    private final List<Item> shopItems = List.of(
            new Item("Sharpened Arrows", 6, 0, 75, "Improves your hero's ranged attacks."),
            new Item("Steel Shield", 0, 5, 70, "Extra protection against incoming waves."),
            new Item("Mystic Tome", 4, 2, 65, "Knowledge that increases both attack and defense."),
            new Item("War Banner", 3, 3, 55, "A balanced boost for any hero."),
            new Item("Champion's Blade", 9, 0, 110, "High risk, high reward damage upgrade.")
    );

    private Hero playerHero;
    private Hero aiHero;
    private Team playerTeam;
    private Team aiTeam;
    private int round;
    private boolean planningPhase;

    private final JTextArea logArea = new JTextArea();
    private final JLabel roundLabel = new JLabel("Round: -");
    private final JLabel baseLabel = new JLabel("Base HP - You: 0 | Enemy: 0");
    private final JLabel heroLabel = new JLabel("Hero: -");
    private final JLabel aiLabel = new JLabel("Enemy Hero: -");
    private final JLabel queuedUnitsLabel = new JLabel("Queued Units: none");

    private final JButton viewInventoryButton = new JButton("View Inventory");
    private final JButton buyItemButton = new JButton("Buy Item");
    private final JButton sendUnitsButton = new JButton("Send Units");
    private final JButton previewWaveButton = new JButton("Preview Wave");
    private final JButton finishPlanningButton = new JButton("Finish Planning");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HeroLineWarsGame game = new HeroLineWarsGame();
            game.setVisible(true);
        });
    }

    public HeroLineWarsGame() {
        super("Hero Line Wars");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(900, 650));
        setLayout(new BorderLayout());

        buildInterface();
        pack();
        setLocationRelativeTo(null);

        appendLog("Welcome to Hero Line Wars (Swing Edition)!");
        appendLog("Hold your lane, build a stronger economy than your opponent, and destroy their base!");
        showHeroSelectionDialog();
    }

    private void buildInterface() {
        JPanel statusPanel = new JPanel(new GridLayout(0, 1));
        statusPanel.add(roundLabel);
        statusPanel.add(baseLabel);
        statusPanel.add(heroLabel);
        statusPanel.add(aiLabel);
        statusPanel.add(queuedUnitsLabel);
        add(statusPanel, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(viewInventoryButton);
        buttonPanel.add(buyItemButton);
        buttonPanel.add(sendUnitsButton);
        buttonPanel.add(previewWaveButton);
        buttonPanel.add(finishPlanningButton);
        add(buttonPanel, BorderLayout.SOUTH);

        viewInventoryButton.addActionListener(e -> showInventoryDialog());
        buyItemButton.addActionListener(e -> showShopDialog());
        sendUnitsButton.addActionListener(e -> showSendUnitsDialog());
        previewWaveButton.addActionListener(e -> showQueuedUnitsDialog());
        finishPlanningButton.addActionListener(e -> handleFinishPlanning());

        setPlanningControlsEnabled(false);
    }

    private void showHeroSelectionDialog() {
        JDialog dialog = new JDialog(this, "Choose Your Hero", true);
        dialog.setLayout(new BorderLayout());

        JTextArea description = new JTextArea();
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setText("Select a hero to begin the battle. Each hero offers a unique playstyle.");
        description.setBorder(null);
        dialog.add(description, BorderLayout.NORTH);

        JPanel heroPanel = new JPanel(new GridLayout(0, 1, 10, 10));

        JButton rangerButton = new JButton("Ranger - Balanced stats and reliable damage.");
        rangerButton.addActionListener(e -> {
            playerHero = new Hero("Ranger", 95, 16, 4, 120, 50);
            dialog.dispose();
            startGame();
        });
        heroPanel.add(rangerButton);

        JButton knightButton = new JButton("Knight - Heavily armored and built to tank waves.");
        knightButton.addActionListener(e -> {
            playerHero = new Hero("Knight", 125, 12, 7, 120, 50);
            dialog.dispose();
            startGame();
        });
        heroPanel.add(knightButton);

        JButton mageButton = new JButton("Battle Mage - Fragile but deals heavy attacks.");
        mageButton.addActionListener(e -> {
            playerHero = new Hero("Battle Mage", 80, 20, 3, 120, 55);
            dialog.dispose();
            startGame();
        });
        heroPanel.add(mageButton);

        dialog.add(heroPanel, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void startGame() {
        aiHero = createAiHero();
        playerTeam = new Team("Player", playerHero);
        aiTeam = new Team("Legion", aiHero);
        round = 0;
        planningPhase = false;

        appendLog("\nYour hero: " + playerHero.getName());
        appendLog("Facing the opposing legion: " + aiHero.getName());
        updateStatusLabels();
        startNextRound();
    }

    private void startNextRound() {
        if (playerTeam.isDefeated() || aiTeam.isDefeated()) {
            return;
        }
        round++;
        planningPhase = true;
        appendLog("\n==============================");
        appendLog("Round " + round);
        appendLog("==============================");

        startOfRoundIncome();
        updateStatusLabels();
        updateQueuedUnitsLabel();
        setPlanningControlsEnabled(true);
    }

    private void setPlanningControlsEnabled(boolean enabled) {
        viewInventoryButton.setEnabled(enabled);
        buyItemButton.setEnabled(enabled);
        sendUnitsButton.setEnabled(enabled);
        previewWaveButton.setEnabled(enabled);
        finishPlanningButton.setEnabled(enabled);
    }

    private void showInventoryDialog() {
        if (!planningPhase || playerHero == null) {
            return;
        }
        List<Item> inventory = playerHero.getInventory();
        StringBuilder builder = new StringBuilder();
        builder.append(playerHero.getName()).append("'s Inventory:\n");
        if (inventory.isEmpty()) {
            builder.append("  (empty)\n");
        } else {
            for (Item item : inventory) {
                builder.append("  • ").append(item).append("\n");
            }
        }
        JOptionPane.showMessageDialog(this, builder.toString(), "Inventory", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showShopDialog() {
        if (!planningPhase || playerHero == null) {
            return;
        }

        String[] itemDescriptions = new String[shopItems.size()];
        for (int i = 0; i < shopItems.size(); i++) {
            Item item = shopItems.get(i);
            itemDescriptions[i] = String.format("%s (Cost: %d, +%d ATK, +%d DEF) - %s",
                    item.getName(), item.getCost(), item.getAttackBonus(), item.getDefenseBonus(), item.getDescription());
        }

        JList<String> list = new JList<>(itemDescriptions);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(400, 180));

        int option = JOptionPane.showConfirmDialog(this, scrollPane, "Shop", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            int index = list.getSelectedIndex();
            if (index >= 0) {
                Item selected = shopItems.get(index);
                if (playerHero.spendGold(selected.getCost())) {
                    playerHero.applyItem(selected);
                    appendLog(String.format("Purchased %s! New stats -> ATK: %d, DEF: %d", selected.getName(),
                            playerHero.getAttack(), playerHero.getDefense()));
                } else {
                    JOptionPane.showMessageDialog(this, "Not enough gold for that item.", "Insufficient Gold",
                            JOptionPane.WARNING_MESSAGE);
                }
                updateStatusLabels();
            }
        }
    }

    private void showSendUnitsDialog() {
        if (!planningPhase || playerHero == null) {
            return;
        }

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        JComboBox<UnitType> unitComboBox = new JComboBox<>(UnitType.values());
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));

        panel.add(new JLabel("Unit:"));
        panel.add(unitComboBox);
        panel.add(new JLabel("Quantity:"));
        panel.add(quantitySpinner);

        int option = JOptionPane.showConfirmDialog(this, panel, "Send Units", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            UnitType selected = (UnitType) unitComboBox.getSelectedItem();
            int quantity = (int) quantitySpinner.getValue();
            if (selected != null) {
                queueUnits(selected, quantity);
            }
        }
    }

    private void queueUnits(UnitType selected, int quantity) {
        Hero hero = playerTeam.getHero();
        int totalCost = selected.getCost() * quantity;
        if (hero.getGold() < totalCost) {
            JOptionPane.showMessageDialog(this, "Not enough gold to send that many units.",
                    "Insufficient Gold", JOptionPane.WARNING_MESSAGE);
            return;
        }
        hero.spendGold(totalCost);
        hero.addIncome(selected.getIncomeBonus() * quantity);
        for (int i = 0; i < quantity; i++) {
            playerTeam.queueUnit(selected);
        }
        appendLog(String.format("Queued %d %s for the next wave. Income is now %d.",
                quantity, selected.getDisplayName(), hero.getIncome()));
        updateStatusLabels();
        updateQueuedUnitsLabel();
    }

    private void showQueuedUnitsDialog() {
        if (!planningPhase || playerTeam == null) {
            return;
        }
        List<UnitType> queuedUnits = playerTeam.getQueuedUnitsSnapshot();
        if (queuedUnits.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No units queued yet for the next wave.",
                    "Queued Units", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Map<UnitType, Integer> counts = new EnumMap<>(UnitType.class);
        for (UnitType type : UnitType.values()) {
            counts.put(type, 0);
        }
        for (UnitType unit : queuedUnits) {
            counts.put(unit, counts.get(unit) + 1);
        }
        StringBuilder builder = new StringBuilder("Units ready for the next wave:\n");
        for (Map.Entry<UnitType, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 0) {
                builder.append("  ").append(entry.getKey().getDisplayName())
                        .append(" x").append(entry.getValue()).append('\n');
            }
        }
        JOptionPane.showMessageDialog(this, builder.toString(), "Queued Units", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleFinishPlanning() {
        if (!planningPhase) {
            return;
        }
        planningPhase = false;
        setPlanningControlsEnabled(false);

        appendLog("\nResolving the round...");
        aiPlanningPhase(aiTeam, round);

        List<UnitType> playerWave = playerTeam.drainQueuedUnits();
        List<UnitType> aiWave = aiTeam.drainQueuedUnits();
        updateQueuedUnitsLabel();

        resolveWave("Player", playerWave, aiTeam);
        resolveWave(aiTeam.getName(), aiWave, playerTeam);

        printRoundSummary();
        updateStatusLabels();

        if (checkForWinner()) {
            return;
        }
        if (round >= 30) {
            determineEconomicWinner();
            return;
        }
        startNextRound();
    }

    private boolean checkForWinner() {
        boolean playerDefeated = playerTeam.isDefeated();
        boolean aiDefeated = aiTeam.isDefeated();

        if (playerDefeated && aiDefeated) {
            appendLog("Both bases fell at the same time! It's a draw.");
            endGame("Draw");
            return true;
        } else if (aiDefeated) {
            appendLog("Congratulations! You defended your base and destroyed the opposing legion.");
            endGame("Victory");
            return true;
        } else if (playerDefeated) {
            appendLog("Your base was overwhelmed. Better luck next time!");
            endGame("Defeat");
            return true;
        }
        return false;
    }

    private void determineEconomicWinner() {
        appendLog("Time is up! The stronger economy wins...");
        if (playerTeam.getHero().getIncome() >= aiTeam.getHero().getIncome()) {
            appendLog("Your team generated more income and is declared the winner!");
            endGame("Victory by economy");
        } else {
            appendLog("The legion amassed the greater economy. They win this time.");
            endGame("Defeat by economy");
        }
    }

    private void endGame(String result) {
        setPlanningControlsEnabled(false);
        JOptionPane.showMessageDialog(this, result, "Game Over", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startOfRoundIncome() {
        playerTeam.getHero().earnIncome();
        aiTeam.getHero().earnIncome();
        appendLog(String.format("Income received! You now have %d gold (Income: %d).",
                playerTeam.getHero().getGold(), playerTeam.getHero().getIncome()));
    }

    private Hero createAiHero() {
        int roll = random.nextInt(3);
        switch (roll) {
            case 0:
                appendLog("The opposing legion fields a disciplined Sentinel.");
                return new Hero("Sentinel", 110, 14, 6, 120, 52);
            case 1:
                appendLog("The opposing legion fields a ferocious Berserker.");
                return new Hero("Berserker", 85, 19, 4, 120, 54);
            default:
                appendLog("The opposing legion fields an arcane Warlock.");
                return new Hero("Warlock", 90, 17, 5, 120, 50);
        }
    }

    private void aiPlanningPhase(Team aiTeam, int currentRound) {
        Hero hero = aiTeam.getHero();
        appendLog("The opposing legion is preparing its strategy...");

        if (hero.getGold() >= 80 && hero.getInventory().size() < 4 && random.nextBoolean()) {
            Item chosen = shopItems.get(random.nextInt(shopItems.size()));
            if (hero.spendGold(chosen.getCost())) {
                hero.applyItem(chosen);
                appendLog(String.format("The legion equips %s for their hero.", chosen.getName()));
            }
        }

        int minCost = getMinimumUnitCost();
        int unitsToSend = Math.max(1, currentRound / 2);
        while (hero.getGold() >= minCost && unitsToSend > 0) {
            UnitType type = chooseUnitForAi(hero, currentRound);
            if (!hero.spendGold(type.getCost())) {
                break;
            }
            aiTeam.queueUnit(type);
            hero.addIncome(type.getIncomeBonus());
            unitsToSend--;
            appendLog(String.format("The legion queues a %s for the coming wave.", type.getDisplayName()));
        }
    }

    private UnitType chooseUnitForAi(Hero hero, int currentRound) {
        UnitType[] types = UnitType.values();
        if (currentRound < 4) {
            return types[random.nextInt(Math.min(types.length, 3))];
        }
        if (hero.getIncome() > 120) {
            return types[types.length - 1];
        }
        return types[random.nextInt(types.length)];
    }

    private int getMinimumUnitCost() {
        int min = Integer.MAX_VALUE;
        for (UnitType type : UnitType.values()) {
            min = Math.min(min, type.getCost());
        }
        return min;
    }

    private void resolveWave(String attackerName, List<UnitType> wave, Team defenderTeam) {
        Hero defender = defenderTeam.getHero();
        if (wave.isEmpty()) {
            appendLog(String.format("%s sends no units this round.", attackerName));
            return;
        }

        defender.resetHealth();
        int goldEarned = 0;
        boolean heroFell = false;
        int baseDamage = 0;
        int unitsProcessed = 0;

        for (int i = 0; i < wave.size(); i++) {
            UnitType unit = wave.get(i);
            int unitHealth = unit.getHealth();
            while (unitHealth > 0) {
                unitHealth -= defender.getAttack();
                if (unitHealth <= 0) {
                    goldEarned += 8;
                    break;
                }
                int damageToHero = Math.max(1, unit.getDamage() - defender.getDefense());
                if (defender.takeDamage(damageToHero)) {
                    heroFell = true;
                    baseDamage += Math.max(6, unit.getDamage());
                    unitsProcessed = i + 1;
                    break;
                }
            }
            if (heroFell) {
                break;
            }
            unitsProcessed = i + 1;
        }

        if (heroFell) {
            for (int i = unitsProcessed; i < wave.size(); i++) {
                UnitType remaining = wave.get(i);
                baseDamage += Math.max(6, remaining.getDamage());
            }
            defenderTeam.damageBase(baseDamage);
            appendLog(String.format("%s's hero was overwhelmed! The base takes %d damage (Base HP: %d).",
                    defenderTeam.getName(), baseDamage, Math.max(0, defenderTeam.getBaseHealth())));
        } else {
            int survivingHealth = defender.getCurrentHealth();
            appendLog(String.format("%s defended the wave with %d HP remaining and earned %d bonus gold.",
                    defender.getName(), survivingHealth, goldEarned));
            defender.addGold(goldEarned);
        }
        defender.resetHealth();
    }

    private void printRoundSummary() {
        appendLog("\n--- Round Summary ---");
        appendLog(String.format("Your base HP: %d", Math.max(0, playerTeam.getBaseHealth())));
        appendLog(String.format("Enemy base HP: %d", Math.max(0, aiTeam.getBaseHealth())));
        appendLog(String.format("Your hero income: %d, Gold: %d", playerTeam.getHero().getIncome(),
                playerTeam.getHero().getGold()));
        appendLog(String.format("Enemy hero income: %d", aiTeam.getHero().getIncome()));
    }

    private void updateStatusLabels() {
        roundLabel.setText("Round: " + round);
        if (playerTeam != null && aiTeam != null) {
            baseLabel.setText(String.format("Base HP - You: %d | Enemy: %d",
                    Math.max(0, playerTeam.getBaseHealth()), Math.max(0, aiTeam.getBaseHealth())));
            Hero hero = playerTeam.getHero();
            heroLabel.setText(String.format("Hero: %s | ATK %d | DEF %d | Gold %d | Income %d",
                    hero.getName(), hero.getAttack(), hero.getDefense(), hero.getGold(), hero.getIncome()));
            Hero enemyHero = aiTeam.getHero();
            aiLabel.setText(String.format("Enemy Hero: %s | ATK %d | DEF %d | Gold %d | Income %d",
                    enemyHero.getName(), enemyHero.getAttack(), enemyHero.getDefense(), enemyHero.getGold(),
                    enemyHero.getIncome()));
        }
    }

    private void updateQueuedUnitsLabel() {
        if (playerTeam == null) {
            queuedUnitsLabel.setText("Queued Units: none");
            return;
        }
        List<UnitType> queued = playerTeam.getQueuedUnitsSnapshot();
        if (queued.isEmpty()) {
            queuedUnitsLabel.setText("Queued Units: none");
            return;
        }
        Map<UnitType, Integer> counts = new EnumMap<>(UnitType.class);
        for (UnitType type : UnitType.values()) {
            counts.put(type, 0);
        }
        for (UnitType unit : queued) {
            counts.put(unit, counts.get(unit) + 1);
        }
        StringBuilder builder = new StringBuilder("Queued Units: ");
        boolean first = true;
        for (Map.Entry<UnitType, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 0) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(entry.getKey().getDisplayName()).append(" x").append(entry.getValue());
                first = false;
            }
        }
        queuedUnitsLabel.setText(builder.toString());
    }

    private void appendLog(String text) {
        logArea.append(text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
