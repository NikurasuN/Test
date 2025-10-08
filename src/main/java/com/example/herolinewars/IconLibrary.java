package com.example.herolinewars;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Centralised factory for lightweight vector icons used throughout the UI.
 */
public final class IconLibrary {
    private static final Color ATTACK_BADGE_COLOR = new Color(214, 76, 76);
    private static final Color DEFENSE_BADGE_COLOR = new Color(82, 142, 224);
    private static final Color STRENGTH_COLOR = new Color(204, 68, 68);
    private static final Color DEXTERITY_COLOR = new Color(74, 170, 110);
    private static final Color INTELLIGENCE_COLOR = new Color(92, 142, 214);

    private IconLibrary() {
    }

    /**
     * Types of high level HUD icons.
     */
    public enum Category {
        HERO_SUMMARY,
        ATTRIBUTES,
        COMBAT,
        PROGRESS,
        RESOURCES,
        PORTAL,
        HERO,
        ENEMY,
        KILLS,
        ECONOMY,
        QUEUE,
        INVENTORY,
        ACTION,
        SHOP,
        PAUSE
    }

    private static final Map<Category, Icon> CATEGORY_CACHE = new EnumMap<>(Category.class);

    /**
     * Creates an icon representing the provided attribute.
     */
    public static Icon createAttributeGlyph(Hero.PrimaryAttribute attribute) {
        switch (attribute) {
            case STRENGTH:
                return createIcon(20, 20, g2 -> {
                    paintBadgeBackground(g2, new Color(184, 52, 52), new Color(108, 18, 18));
                    g2.setColor(new Color(240, 230, 230));
                    g2.fillRoundRect(4, 8, 12, 4, 3, 3);
                    g2.fillOval(1, 6, 6, 8);
                    g2.fillOval(13, 6, 6, 8);
                    g2.setColor(new Color(90, 10, 10));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, 19, 19, 6, 6);
                });
            case DEXTERITY:
                return createIcon(20, 20, g2 -> {
                    paintBadgeBackground(g2, new Color(60, 160, 92), new Color(22, 88, 40));
                    g2.setColor(new Color(236, 248, 236));
                    GeneralPath arrow = new GeneralPath();
                    arrow.moveTo(5, 10);
                    arrow.lineTo(12, 4);
                    arrow.lineTo(12, 8);
                    arrow.lineTo(16, 8);
                    arrow.lineTo(16, 12);
                    arrow.lineTo(12, 12);
                    arrow.lineTo(12, 16);
                    arrow.closePath();
                    g2.fill(arrow);
                    g2.setColor(new Color(16, 66, 32));
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(0, 0, 19, 19, 6, 6);
                });
            case INTELLIGENCE:
            default:
                return createIcon(20, 20, g2 -> {
                    paintBadgeBackground(g2, new Color(74, 122, 210), new Color(36, 58, 126));
                    g2.setColor(new Color(232, 240, 255));
                    GeneralPath spark = new GeneralPath();
                    spark.moveTo(10, 3);
                    spark.lineTo(12, 8);
                    spark.lineTo(17, 10);
                    spark.lineTo(12, 12);
                    spark.lineTo(10, 17);
                    spark.lineTo(8, 12);
                    spark.lineTo(3, 10);
                    spark.lineTo(8, 8);
                    spark.closePath();
                    g2.fill(spark);
                    g2.setColor(new Color(28, 50, 110));
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(0, 0, 19, 19, 6, 6);
                });
        }
    }

    /**
     * Creates an icon representing an equipment slot or relic.
     */
    public static Icon createItemSlotGlyph(Item.EquipmentSlot slot) {
        int size = 24;
        return createIcon(size, size, g2 -> {
            switch (slot != null ? slot : Item.EquipmentSlot.ACCESSORY) {
                case HELMET:
                    paintHelmet(g2, size);
                    break;
                case CHESTPLATE:
                    paintChestplate(g2, size);
                    break;
                case WEAPON:
                    paintWeapon(g2, size);
                    break;
                case SHIELD:
                    paintShield(g2, size);
                    break;
                case RING:
                    paintRing(g2, size);
                    break;
                case ACCESSORY:
                default:
                    paintRelic(g2, size);
                    break;
            }
        });
    }

    /**
     * Creates an icon for a specific item including stat pips.
     */
    public static Icon createItemGlyph(Item item) {
        int size = 28;
        return createIcon(size, size, g2 -> {
            Item.EquipmentSlot slot = item.getSlot();
            switch (slot != null ? slot : Item.EquipmentSlot.ACCESSORY) {
                case HELMET:
                    paintHelmet(g2, size);
                    break;
                case CHESTPLATE:
                    paintChestplate(g2, size);
                    break;
                case WEAPON:
                    paintWeapon(g2, size);
                    break;
                case SHIELD:
                    paintShield(g2, size);
                    break;
                case RING:
                    paintRing(g2, size);
                    break;
                case ACCESSORY:
                default:
                    paintRelic(g2, size);
                    break;
            }

            int pipY = size - 7;
            if (item.getAttackBonus() > 0) {
                drawBadge(g2, 6, 6, ATTACK_BADGE_COLOR, createCrossedBlades());
            }
            if (item.getDefenseBonus() > 0) {
                drawBadge(g2, size - 6, 6, DEFENSE_BADGE_COLOR, createShieldEmblem());
            }
            int offset = 0;
            if (item.getStrengthBonus() > 0) {
                drawAttributePip(g2, size / 2 - 6 + offset, pipY, STRENGTH_COLOR);
                offset += 6;
            }
            if (item.getDexterityBonus() > 0) {
                drawAttributePip(g2, size / 2 - 6 + offset, pipY, DEXTERITY_COLOR);
                offset += 6;
            }
            if (item.getIntelligenceBonus() > 0) {
                drawAttributePip(g2, size / 2 - 6 + offset, pipY, INTELLIGENCE_COLOR);
            }
        });
    }

    /**
     * Returns a reusable category icon.
     */
    public static Icon createCategoryGlyph(Category category) {
        return CATEGORY_CACHE.computeIfAbsent(category, IconLibrary::buildCategoryIcon);
    }

    private static Icon buildCategoryIcon(Category category) {
        switch (category) {
            case HERO_SUMMARY:
                return createIcon(22, 22, g2 -> paintCrest(g2, new Color(210, 176, 68), new Color(132, 96, 22)));
            case ATTRIBUTES:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(72, 84, 114), new Color(40, 48, 66));
                    g2.setColor(STRENGTH_COLOR);
                    g2.fillRect(4, 6, 4, 10);
                    g2.setColor(DEXTERITY_COLOR);
                    g2.fillRect(10, 4, 4, 12);
                    g2.setColor(INTELLIGENCE_COLOR);
                    g2.fillRect(16, 8, 4, 8);
                    g2.setColor(new Color(24, 30, 46));
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(0, 0, 21, 21, 6, 6);
                });
            case COMBAT:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(140, 88, 62), new Color(82, 42, 28));
                    g2.setColor(new Color(245, 230, 210));
                    Path2D sword1 = new Path2D.Double();
                    sword1.moveTo(6, 15);
                    sword1.lineTo(10, 7);
                    sword1.lineTo(11, 8);
                    sword1.lineTo(7, 16);
                    sword1.closePath();
                    g2.fill(sword1);
                    Path2D sword2 = new Path2D.Double();
                    sword2.moveTo(16, 15);
                    sword2.lineTo(12, 7);
                    sword2.lineTo(11, 8);
                    sword2.lineTo(15, 16);
                    sword2.closePath();
                    g2.fill(sword2);
                    g2.setColor(new Color(60, 34, 20));
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(0, 0, 21, 21, 6, 6);
                });
            case PROGRESS:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(78, 118, 194), new Color(32, 58, 116));
                    g2.setColor(new Color(236, 246, 255));
                    Path2D arrow = new Path2D.Double();
                    arrow.moveTo(6, 16);
                    arrow.lineTo(6, 12);
                    arrow.lineTo(10, 12);
                    arrow.lineTo(10, 8);
                    arrow.lineTo(8, 8);
                    arrow.lineTo(11, 4);
                    arrow.lineTo(14, 8);
                    arrow.lineTo(12, 8);
                    arrow.lineTo(12, 16);
                    arrow.closePath();
                    g2.fill(arrow);
                    g2.setColor(new Color(26, 44, 86));
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(0, 0, 21, 21, 6, 6);
                });
            case RESOURCES:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(180, 66, 92), new Color(110, 28, 48));
                    g2.setColor(new Color(244, 218, 226));
                    g2.fillOval(4, 6, 7, 7);
                    g2.setColor(new Color(240, 236, 180));
                    g2.fillOval(11, 5, 7, 7);
                    g2.setColor(new Color(80, 20, 34));
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(0, 0, 21, 21, 6, 6);
                });
            case PORTAL:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(108, 80, 166), new Color(52, 30, 108));
                    g2.setColor(new Color(220, 208, 246));
                    g2.fillOval(5, 5, 12, 12);
                    g2.setColor(new Color(82, 48, 140));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(4, 4, 14, 14);
                });
            case HERO:
                return createIcon(22, 22, g2 -> paintCrest(g2, new Color(80, 142, 214), new Color(34, 72, 122)));
            case ENEMY:
                return createIcon(22, 22, g2 -> paintCrest(g2, new Color(200, 74, 74), new Color(112, 24, 24)));
            case KILLS:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(160, 86, 86), new Color(92, 36, 36));
                    g2.setColor(new Color(244, 228, 228));
                    Path2D blade = new Path2D.Double();
                    blade.moveTo(6, 6);
                    blade.lineTo(16, 16);
                    blade.lineTo(12, 16);
                    blade.lineTo(16, 12);
                    blade.closePath();
                    g2.fill(blade);
                    g2.setColor(new Color(70, 28, 28));
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(0, 0, 21, 21, 6, 6);
                });
            case ECONOMY:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(190, 152, 64), new Color(114, 76, 24));
                    g2.setColor(new Color(246, 234, 184));
                    g2.fillOval(5, 5, 12, 12);
                    g2.setColor(new Color(120, 78, 18));
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawOval(4, 4, 14, 14);
                    g2.drawLine(11, 6, 11, 14);
                    g2.drawLine(8, 10, 14, 10);
                });
            case QUEUE:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(82, 108, 156), new Color(42, 58, 92));
                    g2.setColor(new Color(220, 228, 244));
                    g2.fillRect(6, 6, 10, 2);
                    g2.fillRect(6, 10, 10, 2);
                    g2.fillRect(6, 14, 10, 2);
                    g2.setColor(new Color(34, 44, 68));
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(0, 0, 21, 21, 6, 6);
                });
            case INVENTORY:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(148, 108, 72), new Color(86, 48, 26));
                    g2.setColor(new Color(224, 198, 164));
                    g2.fillRoundRect(4, 7, 14, 10, 6, 6);
                    g2.setColor(new Color(240, 208, 172));
                    g2.fillOval(8, 4, 6, 6);
                    g2.setColor(new Color(70, 38, 20));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, 21, 21, 6, 6);
                });
            case ACTION:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(214, 176, 68), new Color(142, 102, 20));
                    g2.setColor(new Color(248, 238, 180));
                    GeneralPath bolt = new GeneralPath();
                    bolt.moveTo(11, 4);
                    bolt.lineTo(8, 12);
                    bolt.lineTo(12, 12);
                    bolt.lineTo(11, 18);
                    bolt.lineTo(15, 10);
                    bolt.lineTo(11, 10);
                    bolt.closePath();
                    g2.fill(bolt);
                    g2.setColor(new Color(96, 64, 10));
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(0, 0, 21, 21, 6, 6);
                });
            case SHOP:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(124, 174, 88), new Color(60, 110, 40));
                    g2.setColor(new Color(244, 234, 186));
                    g2.fillOval(6, 7, 10, 10);
                    g2.setColor(new Color(78, 98, 46));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawOval(5, 6, 12, 12);
                    g2.drawLine(11, 6, 11, 18);
                });
            case PAUSE:
            default:
                return createIcon(22, 22, g2 -> {
                    paintBadgeBackground(g2, new Color(92, 112, 140), new Color(38, 52, 74));
                    g2.setColor(new Color(224, 234, 246));
                    g2.fillRect(7, 6, 4, 10);
                    g2.fillRect(13, 6, 4, 10);
                    g2.setColor(new Color(28, 38, 58));
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(0, 0, 21, 21, 6, 6);
                });
        }
    }

    private static void paintHelmet(Graphics2D g2, int size) {
        paintBadgeBackground(g2, new Color(120, 138, 160), new Color(64, 74, 92));
        g2.setColor(new Color(220, 226, 236));
        g2.fillOval(5, 5, size - 10, size - 14);
        g2.setColor(new Color(66, 74, 88));
        g2.fillRect(6, size - 10, size - 12, 5);
        g2.setColor(new Color(38, 46, 60));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(1, 1, size - 2, size - 2, 8, 8);
    }

    private static void paintChestplate(Graphics2D g2, int size) {
        paintBadgeBackground(g2, new Color(128, 100, 78), new Color(76, 46, 30));
        g2.setColor(new Color(216, 196, 170));
        GeneralPath torso = new GeneralPath();
        torso.moveTo(6, 4);
        torso.lineTo(size - 6, 4);
        torso.lineTo(size - 4, size - 6);
        torso.lineTo(size / 2 + 4, size - 2);
        torso.lineTo(size / 2 - 4, size - 2);
        torso.lineTo(4, size - 6);
        torso.closePath();
        g2.fill(torso);
        g2.setColor(new Color(66, 44, 30));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(1, 1, size - 2, size - 2, 8, 8);
    }

    private static void paintWeapon(Graphics2D g2, int size) {
        paintBadgeBackground(g2, new Color(166, 122, 82), new Color(94, 62, 32));
        g2.setColor(new Color(236, 230, 224));
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(size / 2 - 3, size - 6, size - 6, 6);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(220, 210, 140));
        g2.drawLine(size / 2 - 1, size - 6, size - 6, size / 2 - 3);
        g2.setColor(new Color(74, 46, 24));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(1, 1, size - 2, size - 2, 8, 8);
    }

    private static void paintShield(Graphics2D g2, int size) {
        paintBadgeBackground(g2, new Color(92, 134, 182), new Color(46, 66, 96));
        g2.setColor(new Color(234, 244, 254));
        GeneralPath shield = new GeneralPath();
        shield.moveTo(size / 2, 4);
        shield.lineTo(size - 6, 8);
        shield.lineTo(size - 8, size - 6);
        shield.lineTo(size / 2, size - 2);
        shield.lineTo(6, size - 6);
        shield.lineTo(4, 8);
        shield.closePath();
        g2.fill(shield);
        g2.setColor(new Color(46, 68, 100));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(1, 1, size - 2, size - 2, 8, 8);
    }

    private static void paintRing(Graphics2D g2, int size) {
        paintBadgeBackground(g2, new Color(182, 142, 64), new Color(118, 84, 24));
        g2.setColor(new Color(246, 234, 188));
        g2.fillOval(5, 5, size - 10, size - 10);
        g2.setColor(new Color(150, 102, 24));
        g2.fillOval(8, 8, size - 16, size - 16);
        g2.setColor(new Color(94, 62, 14));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(1, 1, size - 2, size - 2, 8, 8);
    }

    private static void paintRelic(Graphics2D g2, int size) {
        paintBadgeBackground(g2, new Color(136, 92, 182), new Color(72, 44, 110));
        g2.setColor(new Color(234, 220, 250));
        GeneralPath gem = new GeneralPath();
        gem.moveTo(size / 2, 4);
        gem.lineTo(size - 6, size / 2);
        gem.lineTo(size / 2, size - 4);
        gem.lineTo(6, size / 2);
        gem.closePath();
        g2.fill(gem);
        g2.setColor(new Color(66, 30, 100));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(1, 1, size - 2, size - 2, 8, 8);
    }

    private static void paintBadgeBackground(Graphics2D g2, Color top, Color bottom) {
        int width = g2.getClipBounds().width;
        int height = g2.getClipBounds().height;
        g2.setPaint(new GradientPaint(0, 0, top, 0, height, bottom));
        g2.fillRoundRect(0, 0, width - 1, height - 1, 8, 8);
    }

    private static void drawBadge(Graphics2D g2, int centerX, int centerY, Color color, ShapePainter overlay) {
        Graphics2D copy = (Graphics2D) g2.create();
        enableQuality(copy);
        copy.translate(centerX - 5, centerY - 5);
        copy.setColor(color);
        copy.fillOval(0, 0, 10, 10);
        copy.setColor(color.darker());
        copy.setStroke(new BasicStroke(1f));
        copy.drawOval(0, 0, 10, 10);
        overlay.paint(copy);
        copy.dispose();
    }

    private static void drawAttributePip(Graphics2D g2, int centerX, int centerY, Color color) {
        g2.setColor(color);
        g2.fillOval(centerX - 3, centerY - 3, 6, 6);
        g2.setColor(color.darker());
        g2.drawOval(centerX - 3, centerY - 3, 6, 6);
    }

    private static ShapePainter createCrossedBlades() {
        return g2 -> {
            g2.setColor(new Color(252, 244, 240));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(2, 7, 7, 2);
            g2.drawLine(3, 2, 8, 7);
        };
    }

    private static ShapePainter createShieldEmblem() {
        return g2 -> {
            g2.setColor(new Color(236, 244, 252));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(2, 2, 6, 6);
        };
    }

    private static void paintCrest(Graphics2D g2, Color top, Color bottom) {
        paintBadgeBackground(g2, top, bottom);
        g2.setColor(new Color(248, 242, 230));
        GeneralPath crest = new GeneralPath();
        crest.moveTo(6, 6);
        crest.lineTo(16, 6);
        crest.lineTo(16, 11);
        crest.lineTo(11, 16);
        crest.lineTo(6, 11);
        crest.closePath();
        g2.fill(crest);
        g2.setColor(bottom.darker());
        g2.setStroke(new BasicStroke(1.1f));
        g2.drawRoundRect(0, 0, 21, 21, 6, 6);
    }

    private static Icon createIcon(int width, int height, Consumer<Graphics2D> painter) {
        return new PainterIcon(width, height, painter);
    }

    private static void enableQuality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private interface ShapePainter {
        void paint(Graphics2D g2);
    }

    private static final class PainterIcon implements Icon {
        private final int width;
        private final int height;
        private final Consumer<Graphics2D> painter;

        private PainterIcon(int width, int height, Consumer<Graphics2D> painter) {
            this.width = width;
            this.height = height;
            this.painter = painter;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setClip(0, 0, width, height);
            enableQuality(g2);
            painter.accept(g2);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }
}
