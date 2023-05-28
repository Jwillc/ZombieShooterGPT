package com.appswithchatgpt.zombiezhootergpt;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.TimeUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameView implements Screen {
	private SpriteBatch batch;
	private Texture img;
	private Sprite player;
	private float playerSpeed = 10f;
	private int health = 100; // player starts with 100 health
	private Texture bulletImg;
	private Texture shootButtonImg;
	private Rectangle shootButtonBounds;
	float shootButtonWidth = 250; // replace with your desired width
	float shootButtonHeight = 250; // replace with your desired height
	private List<Sprite> bullets;
	private float bulletSpeed = 20f;
	private float shootDelay = 0.05f; // delay in seconds
	private float timeSinceLastShot = 0;
	private float lastZombieTime = 0;
	private Texture zombieImg;
	private List<Zombie> zombies;
	private float zombieSpeed = 2f;
	private ParticleEffectPool explosionEffectPool;
	private List<ParticleEffectPool.PooledEffect> activeExplosions;
	private Texture backgroundNear;
	private Texture backgroundFar;
	private float backgroundNearX;
	private float backgroundFarX;
	private float MAX_HEIGHT;
	private Texture fastZombieImg;
	private Texture crazyZombieImg;
	private ShapeRenderer shapeRenderer;
	private Rectangle playerHitbox;
	Sound shootingSound;
	private boolean isShooting;
	private int kills;
	private BitmapFont font;
	private GlyphLayout layout;
	private float gameStartTime;
	private Game game;

	public GameView(Game game) {
		this.game = game;
	}

	private class Zombie {
		Sprite sprite;
		int hitCount;
		float speed;
		Rectangle hitbox;

		Zombie(Texture texture, float x, float y, float minSpeed, float maxSpeed) {
			sprite = new Sprite(texture);
			sprite.setSize(150, 200); // set the size of the zombie sprite
			sprite.setPosition(x, y);
			hitCount = 0;
			this.speed = MathUtils.random(minSpeed, maxSpeed);
			hitbox = new Rectangle(x, y, sprite.getWidth(), sprite.getHeight());
		}

		void update() {
			hitbox.setPosition(sprite.getX(), sprite.getY());
		}

		void jitter() {
			float jitterAmount = MathUtils.random(-1f, 1f); // adjust this value to change the amount of jitter
			sprite.setY(sprite.getY() + jitterAmount);

			// Keep zombie within screen bounds
			if (sprite.getY() < 0) sprite.setY(0);
			if (sprite.getY() > MAX_HEIGHT - sprite.getHeight()) sprite.setY(MAX_HEIGHT - sprite.getHeight());
		}
	}

	private class FastZombie extends Zombie {
		FastZombie(Texture texture, float x, float y) {
			super(texture, x, y, 4f, 6f); // set a random speed between 4 and 6 for the fast zombie
		}
	}

	private class CrazyZombie extends Zombie {
		boolean movingUp;

		CrazyZombie(Texture texture, float x, float y) {
			super(texture, x, y, 3f, 5f); // set the speed of the crazy zombie
			movingUp = MathUtils.randomBoolean(); // randomly start moving up or down
		}

		void update() {
			if (movingUp) {
				sprite.setY(sprite.getY() + speed);
				if (sprite.getY() > MAX_HEIGHT) {
					movingUp = false;
				}
			} else {
				sprite.setY(sprite.getY() - speed);
				if (sprite.getY() < 0) {
					movingUp = true;
				}
			}
			super.update();
		}
	}

	private void checkShootingLogic() {

		/*if (Gdx.input.isTouched()) {
			int touchX = Gdx.input.getX();
			int touchY = Gdx.graphics.getHeight() - Gdx.input.getY(); // libGDX's y-coordinates are from bottom to top
			if (shootButtonBounds.contains(touchX, touchY)) {
				// Shoot button is pressed
				Sprite bullet = new Sprite(bulletImg);
				bullet.setPosition(player.getX() + player.getWidth(), player.getY() + player.getHeight() / 2);
				bullets.add(bullet);
			}
		}*/

		batch.begin();

		List<Sprite> bulletsToRemove = new ArrayList<>();

		for (Sprite bullet : bullets) {
			bullet.setX(bullet.getX() + bulletSpeed);
			if (bullet.getX() > Gdx.graphics.getWidth()) {
				// Bullet is off-screen
				bulletsToRemove.add(bullet);
			} else {
				bullet.draw(batch);
			}
		}

		// Move the zombies and check for collisions with bullets
		List<Zombie> zombiesToRemove = new ArrayList<>();
		for (Zombie zombie : zombies) {

			zombie.sprite.setX(zombie.sprite.getX() - zombie.speed);
			if (zombie instanceof CrazyZombie) {
				((CrazyZombie) zombie).update();
			} else {
				zombie.jitter();
			}

			for (Sprite bullet : bullets) {
				if (bullet.getBoundingRectangle().overlaps(zombie.sprite.getBoundingRectangle())) {
					// Bullet hit zombie
					bulletsToRemove.add(bullet);
					zombie.hitCount++;
					if (zombie.hitCount >= 10) {
						// Zombie has been hit 10 times and dies
						zombiesToRemove.add(zombie);
						kills++; // increment the kill counter
						for (Zombie mZombie : zombiesToRemove) {
							ParticleEffectPool.PooledEffect explosionEffect = explosionEffectPool.obtain();
							explosionEffect.setPosition(mZombie.sprite.getX() + mZombie.sprite.getWidth() / 2, zombie.sprite.getY() + zombie.sprite.getHeight() / 2);
							activeExplosions.add(explosionEffect);
						}
					}
					break;
				}
			}

			zombie.update();  // Update the zombie's hitbox position

			// Check for collisions between the player and the zombies
			if (playerHitbox.overlaps(zombie.hitbox)) {
				health -= 5; // decrease health by 5
				if (health < 0) {
					game.setScreen(new MenuScreen(game, kills));
					dispose();
				}
				System.out.println("Collision detected! Health is now: " + health);
				break; // exit the loop as soon as a collision is detected
			}
		}

		for (Iterator<ParticleEffectPool.PooledEffect> iter = activeExplosions.iterator(); iter.hasNext(); ) {
			ParticleEffectPool.PooledEffect explosionEffect = iter.next();
			explosionEffect.draw(batch, Gdx.graphics.getDeltaTime());
			if (explosionEffect.isComplete()) {
				explosionEffect.free();
				iter.remove();
			}
		}

		zombies.removeAll(zombiesToRemove);
		bullets.removeAll(bulletsToRemove);

		for (Zombie zombie : zombies) {
			zombie.sprite.draw(batch);
		}

		batch.end();

	}

	@Override
	public void show() {
		font = new BitmapFont();
		layout = new GlyphLayout();
		gameStartTime = TimeUtils.nanoTime();

		font.getData().setScale(4, 4);

		MAX_HEIGHT = Gdx.graphics.getHeight() * 0.25f;
		shapeRenderer = new ShapeRenderer();

		batch = new SpriteBatch();
		img = new Texture("player.png"); // replace with your player image
		player = new Sprite(img);
		player.setSize(200, 250); // set the size of the player sprite
		player.setPosition(0, Gdx.graphics.getHeight() / 2f); // player starts on the left side, middle of the screen
		playerHitbox = new Rectangle(0, Gdx.graphics.getHeight() / 2f, 200, 250); // Update this line

		bulletImg = new Texture("bullet.png"); // replace with your bullet image
		shootButtonImg = new Texture("shootButton.png"); // replace with your button image
		shootButtonBounds = new Rectangle(Gdx.graphics.getWidth() - shootButtonWidth, 0, shootButtonWidth, shootButtonHeight);
		bullets = new ArrayList<>();
		shootingSound = Gdx.audio.newSound(Gdx.files.internal("machinegunloop.mp3"));

		zombieImg = new Texture("zombie1.png"); // replace with your zombie image
		fastZombieImg = new Texture("zombieFast.png");
		crazyZombieImg = new Texture("crazyZombie.png");
		zombies = new ArrayList<>();

		ParticleEffect explosionEffectPrototype = new ParticleEffect();
		explosionEffectPrototype.load(Gdx.files.internal("explosion.p"), Gdx.files.internal(""));
		explosionEffectPool = new ParticleEffectPool(explosionEffectPrototype, 1, 10);
		activeExplosions = new ArrayList<>();

		backgroundNear = new Texture(Gdx.files.internal("backgroundNear.png"));
		backgroundFar = new Texture(Gdx.files.internal("backgroundFar.png"));
		backgroundNearX = 0;
		backgroundFarX = 0;
	}

	@Override
	public void render(float delta) {
		float deltaTime = Gdx.graphics.getDeltaTime(); // time since last frame
		timeSinceLastShot += deltaTime;

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.begin();

		float elapsedTime = (TimeUtils.nanoTime() - gameStartTime) / 1000000000.0f; // convert to seconds

		// Update and draw the backgrounds
		backgroundNearX -= 100 * Gdx.graphics.getDeltaTime(); // Near background moves faster
		backgroundFarX -= 50 * Gdx.graphics.getDeltaTime(); // Far background moves slower

		if (backgroundNearX + backgroundNear.getWidth() < 0) {
			backgroundNearX = 0;
		}
		if (backgroundFarX + backgroundFar.getWidth() < 0) {
			backgroundFarX = 0;
		}

		batch.draw(backgroundFar, backgroundFarX, 0);
		batch.draw(backgroundFar, backgroundFarX + backgroundFar.getWidth(), 0);
		batch.draw(backgroundNear, backgroundNearX, 0);
		batch.draw(backgroundNear, backgroundNearX + backgroundNear.getWidth(), 0);

		font.draw(batch, "Kills: " + kills, 250, Gdx.graphics.getHeight() - 25);

		player.draw(batch);
		batch.draw(shootButtonImg, shootButtonBounds.x, shootButtonBounds.y, shootButtonBounds.width, shootButtonBounds.height);
		batch.end();

		// Draw the health bar
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(Color.RED);
		shapeRenderer.rect(20, Gdx.graphics.getHeight() - 40, health * 2, 20); // health * 2 to make the health bar wider
		shapeRenderer.end();

		if (Gdx.input.isTouched()) {
			int touchX = Gdx.input.getX();
			int touchY = Gdx.graphics.getHeight() - Gdx.input.getY(); // libGDX's y-coordinates are from bottom to top
			if (shootButtonBounds.contains(touchX, touchY)) {
				// Shoot button is pressed
				if (!isShooting) {
					// Start the shooting sound loop if it's not already playing
					shootingSound.loop();
					isShooting = true;
				}
				if (timeSinceLastShot > shootDelay) {
					// Enough time has passed since the last shot
					Sprite bullet = new Sprite(bulletImg);
					bullet.setPosition(player.getX() + player.getWidth(), player.getY() + 30 + player.getHeight() / 2);
					bullets.add(bullet);
					timeSinceLastShot = 0;
				}
			} else {
				// Shoot button is not pressed
				// Touch is on the upper half of the screen
				if (touchY > Gdx.graphics.getHeight() / 2 && player.getY() < MAX_HEIGHT) {
					player.setY(player.getY() + playerSpeed);
				} else {
					// Touch is on the lower half of the screen
					player.setY(player.getY() - playerSpeed);
				}

				if (isShooting) {
					// Stop the shooting sound loop if it's currently playing
					shootingSound.stop();
					isShooting = false;
				}
			}

			// Keep player within screen bounds
			if (player.getY() < 0) player.setY(0);
			if (player.getY() > MAX_HEIGHT) player.setY(MAX_HEIGHT); // Limit player's upward movement
		} else {
			// Touch is not down
			if (isShooting) {
				// Stop the shooting sound loop if it's currently playing
				shootingSound.stop();
				isShooting = false;
			}
		}

		playerHitbox.setPosition(player.getX(), player.getY());
		// Keep player within screen bounds
		if (player.getY() < 0) player.setY(0);
		if (player.getY() > Gdx.graphics.getHeight() - player.getHeight()) player.setY(Gdx.graphics.getHeight() - player.getHeight());

		// Spawn a new zombie every second
		if (TimeUtils.nanoTime() - lastZombieTime > (1 - (elapsedTime / 100)) * 1000000000) {
			float rand = MathUtils.random();
			if (rand < 0.33f) { // 33% chance to spawn a fast zombie
				zombies.add(new FastZombie(fastZombieImg, Gdx.graphics.getWidth(), MathUtils.random(MAX_HEIGHT)));
			} else if (rand < 0.66f) { // 33% chance to spawn a crazy zombie
				zombies.add(new CrazyZombie(crazyZombieImg, Gdx.graphics.getWidth(), MathUtils.random(MAX_HEIGHT)));
			} else {
				zombies.add(new Zombie(zombieImg, Gdx.graphics.getWidth(), MathUtils.random(MAX_HEIGHT), 1.5f, 2.5f)); // set a random speed between 1.5 and 2.5 for the regular zombie
			}
			lastZombieTime = TimeUtils.nanoTime();
		}

		checkShootingLogic();
	}

	@Override
	public void resize(int width, int height) {
		//
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void hide() {
		//
	}

	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();

		bulletImg.dispose();
		shootButtonImg.dispose();
		fastZombieImg.dispose();
		crazyZombieImg.dispose();
		shapeRenderer.dispose();
		shootingSound.dispose();
		font.dispose();

		for (ParticleEffectPool.PooledEffect explosionEffect : activeExplosions) {
			explosionEffect.free();
		}
		activeExplosions.clear();
	}
}
