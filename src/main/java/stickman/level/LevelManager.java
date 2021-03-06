package stickman.level;

import stickman.entity.*;
import stickman.entity.moving.MovingEntity;
import stickman.entity.moving.other.Bullet;
import stickman.entity.moving.other.Projectile;
import stickman.entity.moving.player.Controllable;
import stickman.entity.moving.player.StickMan;
import stickman.model.GameEngine;
import stickman.model.GameManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the Level interface. Manages the running of
 * the level and all the entities within it.
 */
public class LevelManager implements Level {

    /**
     * The player character.
     */
    private Controllable hero;

    /**
     * A list of all the entities in the level.
     */
    private List<Entity> entities;

    /**
     * A list of all the moving entities in the level.
     */
    private List<MovingEntity> movingEntities;

    /**
     * A list of all the entities that can interact with the player.
     */
    private List<Interactable> interactables;

    /**
     * A list of all the projectiles (bullets) in the level.
     */
    private List<Projectile> projectiles;

    /**
     * The height of the level.
     */
    private double height;

    /**
     * The width of the level.
     */
    private double width;

    /**
     * The height of the floor in the level.
     */
    private double floorHeight;

    /**
     * The target time of current level
     */
    private double targetTime;

    /**
     * Whether the entities should update, or the player has reached the flag.
     */
    private boolean active;

    /**
     * The name of the file the level is from.
     */
    private String filename;

    /**
     * The GameEngine the level is running inside of.
     */
    private GameEngine model;

    /**
     * Whether player won the game or not
     */
    private boolean won;

    /**
     * Whether player lose the game or not
     */
    private boolean lose;

    /**
     * Creates a new LevelManager object.
     * @param model The GameEngine the level is in
     * @param filename The file the level is based off of
     * @param height The height of the level
     * @param width The width of the level
     * @param floorHeight The height of the floor
     * @param targetTime The target time of the level
     * @param heroX The starting x of the hero
     * @param heroSize The size of the hero
     * @param entities The list of entities in the level
     * @param movingEntities The list of moving entities in the level
     * @param interactables The list of entities that can interact with the hero in the level
     */
    public LevelManager(GameEngine model, String filename, double height, double width, double floorHeight,
                        double targetTime, double heroX, String heroSize, List<Entity> entities,
                        List<MovingEntity> movingEntities, List<Interactable> interactables) {
        this.model = model;
        this.filename = filename;
        this.height = height;
        this.width = width;
        this.floorHeight = floorHeight;
        this.targetTime = targetTime;

        this.entities = entities;
        this.movingEntities = movingEntities;
        this.interactables = interactables;
        this.projectiles = new ArrayList<>();

        // Create new hero
        this.hero = new StickMan(heroX, floorHeight, heroSize,this);
        this.movingEntities.add(this.hero);

        // Ensure entities has all entities (including moving ones)
        this.entities.addAll(movingEntities);
        this.entities = new ArrayList<>(new HashSet<>(entities));
        this.active = true;
    }

    /**
     * Create a new LevelManager object for save current level operation.
     * @param model The GameEngine the level is in
     * @param filename The file the level is based off of
     * @param height The height of the level
     * @param width The width of the level
     * @param floorHeight The height of the floor
     * @param targetTime The target time of the level
     * @param active The active states of level
     * @param hero The hero that can be controlled
     * @param entities The list of entities in the level
     * @param movingEntities The list of moving entities in the level
     * @param interactables The list of entities that can interact with the hero in the level
     */
    private LevelManager(GameEngine model, String filename, double height, double width, double floorHeight,
                         double targetTime, boolean active, Entity hero, List<Entity> entities,
                         List<MovingEntity> movingEntities, List<Interactable> interactables) {
        this.model = model;
        this.filename = filename;
        this.height = height;
        this.width = width;
        this.floorHeight = floorHeight;
        this.targetTime = targetTime;
        this.active = active;

        this.entities = entities;
        this.movingEntities = movingEntities;
        this.interactables = interactables;
        this.projectiles = new ArrayList<>();

        // Create new hero
        this.hero = (Controllable) hero;
        this.movingEntities.add(this.hero);

        // Ensure entities has all entities (including moving ones)
        this.entities.addAll(movingEntities);
        this.entities = new ArrayList<>(new HashSet<>(entities));
        this.active = true;
    }

    /**
     * Create a copy version of LevelManager object that contains all the information
     * @return The LevelManager object contains all information
     */
    public LevelManager deepCopy() {

        List<Entity> copiedEntities = new ArrayList<>();
        List<MovingEntity> copiedMovingEntities = new ArrayList<>();
        List<Interactable> copiedInteractables = new ArrayList<>();

        // current copiedEntities contains all platforms
        for (Entity e : entities) {
            if (e.getImagePath().equals("foot_tile.png")) {
                copiedEntities.add(e.deepCopy());
            }
        }

        // current copiedMovingEntities contains all enemies
        for (MovingEntity m: movingEntities) {
            if (m.getImagePath().startsWith("slime")) {
                copiedMovingEntities.add((MovingEntity) m.deepCopy());
            }
        }

        // current copiedInteractables contains mushroom and flag.
        for (Interactable i: interactables) {
            if (!(i.getImagePath().startsWith("slime"))) {
                copiedInteractables.add((Interactable) i.deepCopy());
            }
        }

        // copy mushroom and flag in copiedInteractables to copiedEntities
        copiedEntities.addAll(copiedInteractables);

        // copy enemies in copiedMovingEntities to copiedInteractables
        for (MovingEntity m: copiedMovingEntities) {
            copiedInteractables.add((Interactable) m);
        }

        return new LevelManager(model, filename, height, width, floorHeight, targetTime, active,
                this.hero.deepCopy(), copiedEntities, copiedMovingEntities, copiedInteractables);
    }

    @Override
    public List<Entity> getEntities() {
        return this.entities;
    }

    @Override
    public double getHeight() {
        return this.height;
    }

    @Override
    public double getWidth() {
        return this.width;
    }

    @Override
    public void tick() {

        if (!active) {
            return;
        }

        for (MovingEntity entity : this.movingEntities) {
            entity.tick(this.entities, this.hero.getXPos(), this.floorHeight);
        }

        this.manageCollisions();

        // Remove inactive entities
        this.clearOutInactive();
    }

    /**
     * Removes inactive entities from all the lists.
     */
    private void clearOutInactive() {
        this.entities.removeIf(x -> !x.isActive());
        this.movingEntities.removeIf(x -> !this.entities.contains(x));
        this.interactables.removeIf(x -> !this.entities.contains(x));
        this.projectiles.removeIf(x -> !this.entities.contains(x));
    }

    /**
     * Calls interact methods on interactables and projectiles.
     */
    private void manageCollisions() {

        if (!entities.contains(this.hero)) {
            return;
        }

        // Collision between hero and other entity
        for (Interactable interactable : this.interactables) {
            if (interactable.checkCollide(hero)) {
                interactable.interact(hero);
            }
        }

        // Collision between bullet and moving entity (not hero)
        for (Projectile projectile : this.projectiles) {
            boolean collide = projectile.movingCollision(
                    this.movingEntities.stream().filter(x -> x != hero).collect(Collectors.toList())
            );
            if (collide) {
                ((GameManager) model).changeCurrentScore(100);
            }
        }

        // Collision between bullet and other entity
        for (Projectile projectile : this.projectiles) {
            projectile.staticCollision(this.entities.stream().filter(x -> x != hero).collect(Collectors.toList()));
        }
    }

    @Override
    public double getFloorHeight() {
        return this.floorHeight;
    }

    @Override
    public double getHeroX() {
        return this.hero.getXPos();
    }

    @Override
    public double getHeroY() {
        return this.hero.getYPos();
    }

    @Override
    public boolean jump() {
        if (!active) {
            return false;
        }
        return this.hero.jump();
    }

    @Override
    public boolean moveLeft() {
        if (!active) {
            return false;
        }
        return this.hero.moveLeft();
    }

    @Override
    public boolean moveRight() {
        if (!active) {
            return false;
        }
        return this.hero.moveRight();
    }

    @Override
    public boolean stopMoving() {
        if (!active) {
            return false;
        }
        return this.hero.stop();
    }

    @Override
    public void shoot() {
        if (!this.hero.upgraded() || !active) {
            return;
        }

        double x = this.hero.getXPos() + this.hero.getWidth();

        if (this.hero.isLeftFacing()) {
            x = this.hero.getXPos();
        }

        Projectile bullet = new Bullet(x, this.hero.getYPos() + (2 * this.hero.getWidth() / 3), this.hero.isLeftFacing());

        this.entities.add(bullet);
        this.movingEntities.add(bullet);
        this.projectiles.add(bullet);
    }

    /**
     * Check current is won or not
     * @return boolean to indicate player is won or not
     */
    public boolean isWon() {
        return won;
    }

    /**
     * Set current won state to be a value
     * @param value boolean to be set
     */
    public void setWon(boolean value) {
        won = value;
    }

    /**
     * Check current is lose or not
     * @return boolean to indicate player is lose or not
     */
    public boolean isLose() {
        return lose;
    }

    /**
     * Decrease the lives of stickman, when the live is 0, set the player lose the game
     */
    public void decreaseLives() {
        ((GameManager) model).decreaseLives();
        double currentLives = ((GameManager) model).getLives();
        if (currentLives == 0.0) {
            lose = true;
        }
    }

    /**
     * Load the next level
     */
    public void nextLevel() {
        ((GameManager) this.model).nextLevel();
    }

    /**
     * Change the current score to a certain value
     * @param value integer that score set to be
     */
    public void changeCurrentScore(int value) {
        ((GameManager) model).changeCurrentScore(value);
    }

    /**
     * Retrive target time
     * @return the target time
     */
    public double getTargetTime() {
        return targetTime;
    }
}
