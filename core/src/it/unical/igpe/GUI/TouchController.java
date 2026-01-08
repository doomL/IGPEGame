package it.unical.igpe.GUI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class TouchController {
    // Virtual joystick for movement (left side)
    private Circle joystickBounds;
    private Circle joystickKnobBounds;
    private Vector2 joystickKnobPosition;
    private Vector2 joystickCenter;
    private int joystickPointerId = -1;
    private boolean joystickActive = false;
    
    // Virtual joystick for aiming (right side)
    private Circle aimJoystickBounds;
    private Circle aimJoystickKnobBounds;
    private Vector2 aimJoystickKnobPosition;
    private Vector2 aimJoystickCenter;
    private int aimJoystickPointerId = -1;
    private boolean aimJoystickActive = false;

    // Action buttons
    private Rectangle shootButton;
    private Rectangle reloadButton;
    private Rectangle slowMotionButton;
    private Rectangle pauseButton;

    // Weapon switch buttons
    private Rectangle pistolButton;
    private Rectangle shotgunButton;
    private Rectangle rifleButton;

    // Touch state
    private boolean shootPressed = false;
    private boolean reloadPressed = false;
    private boolean slowMotionPressed = false;
    private boolean pausePressed = false;
    private int shootButtonPointerId = -1;
    private int reloadButtonPointerId = -1;
    private int slowMotionButtonPointerId = -1;
    private int pauseButtonPointerId = -1;

    private OrthographicCamera camera;
    private OrthographicCamera uiCamera;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    // Visual parameters - made much larger for better visibility and touch
    private static final float JOYSTICK_RADIUS = 150f; // Bigger movement joystick
    private static final float JOYSTICK_KNOB_RADIUS = 75f;
    private static final float AIM_JOYSTICK_RADIUS = 150f; // Bigger aim joystick
    private static final float AIM_JOYSTICK_KNOB_RADIUS = 75f;
    private static final float BUTTON_SIZE = 100f;
    private static final float LARGE_BUTTON_SIZE = 120f;
    private static final float PADDING = 50f; // More padding from screen edges
    private static final float MARGIN_LEFT = 80f; // Left margin for movement joystick
    private static final float MARGIN_BOTTOM = 80f; // Bottom margin for movement joystick

    public TouchController(OrthographicCamera camera) {
        this.camera = camera;
        // Create UI camera for screen-space rendering
        this.uiCamera = new OrthographicCamera();
        updateUICamera();
        this.shapeRenderer = new ShapeRenderer();
        this.font = new BitmapFont();

        // Initialize buttons (top of screen, with proper spacing)
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        
        // Pause button (top right corner, with padding)
        pauseButton = new Rectangle(
            screenWidth - BUTTON_SIZE - PADDING,
            screenHeight - BUTTON_SIZE - PADDING,
            BUTTON_SIZE,
            BUTTON_SIZE
        );

        // Weapon buttons (top center, with spacing between them)
        float weaponButtonSpacing = BUTTON_SIZE + 20f;
        float weaponButtonsStartX = screenWidth / 2 - (weaponButtonSpacing * 1.5f);
        
        pistolButton = new Rectangle(
            weaponButtonsStartX,
            screenHeight - BUTTON_SIZE - PADDING,
            BUTTON_SIZE,
            BUTTON_SIZE
        );

        shotgunButton = new Rectangle(
            weaponButtonsStartX + weaponButtonSpacing,
            screenHeight - BUTTON_SIZE - PADDING,
            BUTTON_SIZE,
            BUTTON_SIZE
        );

        rifleButton = new Rectangle(
            weaponButtonsStartX + weaponButtonSpacing * 2,
            screenHeight - BUTTON_SIZE - PADDING,
            BUTTON_SIZE,
            BUTTON_SIZE
        );
        
        // Reload button (below weapon buttons, right side)
        reloadButton = new Rectangle(
            screenWidth - BUTTON_SIZE - PADDING,
            screenHeight - BUTTON_SIZE - PADDING - BUTTON_SIZE - 20f,
            BUTTON_SIZE,
            BUTTON_SIZE
        );
        
        // Shoot button (large, below reload button, right side)
        shootButton = new Rectangle(
            screenWidth - LARGE_BUTTON_SIZE - PADDING,
            reloadButton.y - LARGE_BUTTON_SIZE - 20f,
            LARGE_BUTTON_SIZE,
            LARGE_BUTTON_SIZE
        );

        // Slow motion button (to the left of shoot, same row)
        slowMotionButton = new Rectangle(
            shootButton.x - BUTTON_SIZE - 20f,
            shootButton.y,
            BUTTON_SIZE,
            BUTTON_SIZE
        );
        
        // Initialize movement joystick (bottom left, with margins)
        joystickCenter = new Vector2(JOYSTICK_RADIUS + MARGIN_LEFT, JOYSTICK_RADIUS + MARGIN_BOTTOM);
        joystickBounds = new Circle(joystickCenter.x, joystickCenter.y, JOYSTICK_RADIUS);
        joystickKnobPosition = new Vector2(joystickCenter);
        joystickKnobBounds = new Circle(joystickKnobPosition.x, joystickKnobPosition.y, JOYSTICK_KNOB_RADIUS);
        
        // Initialize aiming joystick (bottom right, with margins to avoid buttons)
        // Position it at the bottom right, ensuring it doesn't overlap with buttons above
        float aimJoystickBottom = AIM_JOYSTICK_RADIUS + MARGIN_BOTTOM;
        float aimJoystickRight = screenWidth - AIM_JOYSTICK_RADIUS - MARGIN_LEFT;
        aimJoystickCenter = new Vector2(aimJoystickRight, aimJoystickBottom);
        aimJoystickBounds = new Circle(aimJoystickCenter.x, aimJoystickCenter.y, AIM_JOYSTICK_RADIUS);
        aimJoystickKnobPosition = new Vector2(aimJoystickCenter);
        aimJoystickKnobBounds = new Circle(aimJoystickKnobPosition.x, aimJoystickKnobPosition.y, AIM_JOYSTICK_KNOB_RADIUS);

    }
    
    private void updateUICamera() {
        this.uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.uiCamera.update();
    }

    public void update() {
        // Update UI camera if screen size changed
        if (uiCamera.viewportWidth != Gdx.graphics.getWidth() || 
            uiCamera.viewportHeight != Gdx.graphics.getHeight()) {
            updateUICamera();
            // Update positions if screen size changed
            float screenWidth = Gdx.graphics.getWidth();
            float screenHeight = Gdx.graphics.getHeight();
            
            // Update movement joystick position
            joystickCenter.set(JOYSTICK_RADIUS + MARGIN_LEFT, JOYSTICK_RADIUS + MARGIN_BOTTOM);
            joystickBounds.setPosition(joystickCenter.x, joystickCenter.y);
            if (!joystickActive) {
                joystickKnobPosition.set(joystickCenter);
            }
            
            // Update button positions
            pauseButton.setPosition(screenWidth - BUTTON_SIZE - PADDING, screenHeight - BUTTON_SIZE - PADDING);
            float weaponButtonSpacing = BUTTON_SIZE + 20f;
            float weaponButtonsStartX = screenWidth / 2 - (weaponButtonSpacing * 1.5f);
            pistolButton.setPosition(weaponButtonsStartX, screenHeight - BUTTON_SIZE - PADDING);
            shotgunButton.setPosition(weaponButtonsStartX + weaponButtonSpacing, screenHeight - BUTTON_SIZE - PADDING);
            rifleButton.setPosition(weaponButtonsStartX + weaponButtonSpacing * 2, screenHeight - BUTTON_SIZE - PADDING);
            reloadButton.setPosition(screenWidth - BUTTON_SIZE - PADDING, screenHeight - BUTTON_SIZE - PADDING - BUTTON_SIZE - 20f);
            shootButton.setPosition(screenWidth - LARGE_BUTTON_SIZE - PADDING, reloadButton.y - LARGE_BUTTON_SIZE - 20f);
            slowMotionButton.setPosition(shootButton.x - BUTTON_SIZE - 20f, shootButton.y);
            
            // Update aiming joystick position (bottom right)
            float aimJoystickBottom = AIM_JOYSTICK_RADIUS + MARGIN_BOTTOM;
            float aimJoystickRight = screenWidth - AIM_JOYSTICK_RADIUS - MARGIN_LEFT;
            aimJoystickCenter.set(aimJoystickRight, aimJoystickBottom);
            aimJoystickBounds.setPosition(aimJoystickCenter.x, aimJoystickCenter.y);
            if (!aimJoystickActive) {
                aimJoystickKnobPosition.set(aimJoystickCenter);
            }
        }
        
        // Reset button states at start of each frame
        reloadPressed = false;
        slowMotionPressed = false;
        pausePressed = false;
        pistolButtonJustPressed = false;
        shotgunButtonJustPressed = false;
        rifleButtonJustPressed = false;
        shootPressed = false; // Reset shoot button state

        // Handle multi-touch - use SCREEN coordinates, not world coordinates
        float screenWidth = Gdx.graphics.getWidth();
        for (int i = 0; i < 5; i++) {
            if (Gdx.input.isTouched(i)) {
                // Get screen coordinates directly (not world coordinates)
                float screenX = Gdx.input.getX(i);
                float screenY = Gdx.graphics.getHeight() - Gdx.input.getY(i); // Flip Y axis
                Vector2 touch = new Vector2(screenX, screenY);

                // Check movement joystick (left side, screen coordinates)
                if (joystickPointerId == -1 && joystickBounds.contains(touch) && touch.x < screenWidth / 2) {
                    joystickPointerId = i;
                    joystickActive = true;
                }

                if (joystickPointerId == i) {
                    updateJoystick(touch);
                    continue; // Skip button checks if this is the movement joystick
                }
                
                // Check aiming joystick (right side, screen coordinates)
                if (aimJoystickPointerId == -1 && aimJoystickBounds.contains(touch) && touch.x > screenWidth / 2) {
                    aimJoystickPointerId = i;
                    aimJoystickActive = true;
                }

                if (aimJoystickPointerId == i) {
                    updateAimJoystick(touch);
                    continue; // Skip button checks if this is the aim joystick
                }

                // Check buttons (screen coordinates) - only if not already assigned to another pointer
                if (shootButton.contains(touch.x, touch.y)) {
                    if (shootButtonPointerId == -1) {
                        shootButtonPointerId = i;
                    }
                    if (shootButtonPointerId == i) {
                        shootPressed = true;
                    }
                }
                if (reloadButton.contains(touch.x, touch.y)) {
                    if (reloadButtonPointerId == -1) {
                        reloadButtonPointerId = i;
                    }
                    if (reloadButtonPointerId == i) {
                        reloadPressed = true;
                    }
                }
                if (slowMotionButton.contains(touch.x, touch.y)) {
                    if (slowMotionButtonPointerId == -1) {
                        slowMotionButtonPointerId = i;
                    }
                    if (slowMotionButtonPointerId == i) {
                        slowMotionPressed = true;
                    }
                }
                if (pauseButton.contains(touch.x, touch.y)) {
                    if (pauseButtonPointerId == -1) {
                        pauseButtonPointerId = i;
                    }
                    if (pauseButtonPointerId == i) {
                        pausePressed = true;
                    }
                }
                // Check weapon buttons (only on new touches, not held)
                if (pistolButton.contains(touch.x, touch.y) && joystickPointerId != i) {
                    pistolButtonJustPressed = true;
                }
                if (shotgunButton.contains(touch.x, touch.y) && joystickPointerId != i) {
                    shotgunButtonJustPressed = true;
                }
                if (rifleButton.contains(touch.x, touch.y) && joystickPointerId != i) {
                    rifleButtonJustPressed = true;
                }
            } else {
                // Reset pointer IDs when touch is released
                if (shootButtonPointerId == i) {
                    shootButtonPointerId = -1;
                }
                if (reloadButtonPointerId == i) {
                    reloadButtonPointerId = -1;
                }
                if (slowMotionButtonPointerId == i) {
                    slowMotionButtonPointerId = -1;
                }
                if (pauseButtonPointerId == i) {
                    pauseButtonPointerId = -1;
                }
            }
        }

        // Reset joysticks if released
        if (joystickPointerId != -1 && !Gdx.input.isTouched(joystickPointerId)) {
            joystickPointerId = -1;
            joystickActive = false;
            joystickKnobPosition.set(joystickCenter);
        }
        
        if (aimJoystickPointerId != -1 && !Gdx.input.isTouched(aimJoystickPointerId)) {
            aimJoystickPointerId = -1;
            aimJoystickActive = false;
            aimJoystickKnobPosition.set(aimJoystickCenter);
        }
    }

    private void updateJoystick(Vector2 touch) {
        Vector2 direction = new Vector2(touch).sub(joystickCenter);
        float distance = direction.len();

        if (distance > JOYSTICK_RADIUS - JOYSTICK_KNOB_RADIUS) {
            direction.setLength(JOYSTICK_RADIUS - JOYSTICK_KNOB_RADIUS);
        }

        joystickKnobPosition.set(joystickCenter).add(direction);
        joystickKnobBounds.setPosition(joystickKnobPosition.x, joystickKnobPosition.y);
    }
    
    private void updateAimJoystick(Vector2 touch) {
        Vector2 direction = new Vector2(touch).sub(aimJoystickCenter);
        float distance = direction.len();

        if (distance > AIM_JOYSTICK_RADIUS - AIM_JOYSTICK_KNOB_RADIUS) {
            direction.setLength(AIM_JOYSTICK_RADIUS - AIM_JOYSTICK_KNOB_RADIUS);
        }

        aimJoystickKnobPosition.set(aimJoystickCenter).add(direction);
        aimJoystickKnobBounds.setPosition(aimJoystickKnobPosition.x, aimJoystickKnobPosition.y);
    }
    
    public Vector2 getAimDirection() {
        if (!aimJoystickActive) {
            return null; // No aiming input
        }

        Vector2 direction = new Vector2(aimJoystickKnobPosition).sub(aimJoystickCenter);
        if (direction.len() < 10f) {
            return null; // Too small to aim
        }

        // Invert Y direction to match screen coordinates
        direction.y = -direction.y;
        // Rotate -90 degrees to fix the 90° offset (same as desktop mouse aiming)
        direction.rotate90(-1);
        return direction.nor();
    }
    
    public Vector2 getMovementDirection() {
        if (!joystickActive) {
            return Vector2.Zero;
        }

        Vector2 direction = new Vector2(joystickKnobPosition).sub(joystickCenter);
        if (direction.len() < 10f) {
            return Vector2.Zero;
        }

        // Invert Y direction to fix opposite movement
        direction.y = -direction.y;
        return direction.nor();
    }

    public void render(SpriteBatch batch) {
        // Update UI camera to match screen size
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCamera.update();
        
        // Render in screen coordinates (not world coordinates)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeType.Filled);
        
        // Render movement joystick base (bottom left) - screen coordinates
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.7f);
        shapeRenderer.circle(joystickCenter.x, joystickCenter.y, JOYSTICK_RADIUS);
        
        // Render movement joystick knob
        if (joystickActive) {
            shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 0.9f);
        } else {
            shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 0.7f);
        }
        shapeRenderer.circle(joystickKnobPosition.x, joystickKnobPosition.y, JOYSTICK_KNOB_RADIUS);
        
        // Render aiming joystick base (bottom right) - screen coordinates
        shapeRenderer.setColor(0.3f, 0.5f, 0.3f, 0.7f);
        shapeRenderer.circle(aimJoystickCenter.x, aimJoystickCenter.y, AIM_JOYSTICK_RADIUS);
        
        // Render aiming joystick knob
        if (aimJoystickActive) {
            shapeRenderer.setColor(0.5f, 0.7f, 0.5f, 0.9f);
        } else {
            shapeRenderer.setColor(0.4f, 0.6f, 0.4f, 0.7f);
        }
        shapeRenderer.circle(aimJoystickKnobPosition.x, aimJoystickKnobPosition.y, AIM_JOYSTICK_KNOB_RADIUS);
        
        // Render buttons (right side) - screen coordinates
        // Shoot button (large, bottom right)
        shapeRenderer.setColor(shootPressed ? 0.8f : 0.6f, 0.2f, 0.2f, 0.8f);
        shapeRenderer.rect(shootButton.x, shootButton.y, shootButton.width, shootButton.height);
        
        // Reload button
        shapeRenderer.setColor(reloadPressed ? 0.2f : 0.4f, 0.6f, 0.2f, 0.8f);
        shapeRenderer.rect(reloadButton.x, reloadButton.y, reloadButton.width, reloadButton.height);
        
        // Slow motion button
        shapeRenderer.setColor(slowMotionPressed ? 0.2f : 0.4f, 0.2f, 0.6f, 0.8f);
        shapeRenderer.rect(slowMotionButton.x, slowMotionButton.y, slowMotionButton.width, slowMotionButton.height);
        
        // Pause button (top right corner)
        shapeRenderer.setColor(pausePressed ? 0.6f : 0.4f, 0.4f, 0.4f, 0.8f);
        shapeRenderer.rect(pauseButton.x, pauseButton.y, pauseButton.width, pauseButton.height);
        
        // Weapon buttons (top of screen)
        shapeRenderer.setColor(0.3f, 0.3f, 0.5f, 0.8f);
        shapeRenderer.rect(pistolButton.x, pistolButton.y, pistolButton.width, pistolButton.height);
        shapeRenderer.rect(shotgunButton.x, shotgunButton.y, shotgunButton.width, shotgunButton.height);
        shapeRenderer.rect(rifleButton.x, rifleButton.y, rifleButton.width, rifleButton.height);
        
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        // Render text labels on buttons
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(0.8f);
        
        // Shoot button label
        font.draw(batch, "SHOOT", shootButton.x + 5, shootButton.y + shootButton.height/2 + 5);
        
        // Reload button label
        font.draw(batch, "R", reloadButton.x + reloadButton.width/2 - 5, reloadButton.y + reloadButton.height/2 + 5);
        
        // Slow motion label
        font.draw(batch, "SLOW", slowMotionButton.x + 5, slowMotionButton.y + slowMotionButton.height/2 + 5);
        
        // Pause label
        font.draw(batch, "||", pauseButton.x + pauseButton.width/2 - 5, pauseButton.y + pauseButton.height/2 + 5);
        
        // Weapon labels
        font.draw(batch, "1", pistolButton.x + pistolButton.width/2 - 3, pistolButton.y + pistolButton.height/2 + 5);
        font.draw(batch, "2", shotgunButton.x + shotgunButton.width/2 - 3, shotgunButton.y + shotgunButton.height/2 + 5);
        font.draw(batch, "3", rifleButton.x + rifleButton.width/2 - 3, rifleButton.y + rifleButton.height/2 + 5);
        
        batch.end();
    }
    
    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }


    public boolean isShootPressed() {
        return shootPressed;
    }

    public boolean isReloadPressed() {
        return reloadPressed;
    }

    public boolean isSlowMotionPressed() {
        return slowMotionPressed;
    }

    public boolean isPausePressed() {
        return pausePressed;
    }

    public boolean isTouched(int pointer) {
        return Gdx.input.isTouched(pointer);
    }

    public Vector2 getTouchPosition(int pointer) {
        if (!Gdx.input.isTouched(pointer)) {
            return null;
        }

        Vector3 touchPos = new Vector3(Gdx.input.getX(pointer), Gdx.input.getY(pointer), 0);
        camera.unproject(touchPos);
        return new Vector2(touchPos.x, touchPos.y);
    }

    // Check weapon button presses - use screen coordinates
    private boolean pistolButtonJustPressed = false;
    private boolean shotgunButtonJustPressed = false;
    private boolean rifleButtonJustPressed = false;
    
    public boolean isPistolButtonPressed() {
        if (pistolButtonJustPressed) {
            pistolButtonJustPressed = false;
            return true;
        }
        return false;
    }

    public boolean isShotgunButtonPressed() {
        if (shotgunButtonJustPressed) {
            shotgunButtonJustPressed = false;
            return true;
        }
        return false;
    }

    public boolean isRifleButtonPressed() {
        if (rifleButtonJustPressed) {
            rifleButtonJustPressed = false;
            return true;
        }
        return false;
    }

    public Rectangle getShootButton() { return shootButton; }
    public Rectangle getReloadButton() { return reloadButton; }
    public Rectangle getSlowMotionButton() { return slowMotionButton; }
    public Rectangle getPauseButton() { return pauseButton; }
    public Rectangle getPistolButton() { return pistolButton; }
    public Rectangle getShotgunButton() { return shotgunButton; }
    public Rectangle getRifleButton() { return rifleButton; }
    public Circle getJoystickBounds() { return joystickBounds; }
    public Circle getJoystickKnobBounds() { return joystickKnobBounds; }
    public int getJoystickPointerId() { return joystickPointerId; }
    
    // Check if a screen coordinate is on any UI element
    public boolean isTouchOnUI(Vector2 screenTouch) {
        // Check joysticks
        if (joystickBounds.contains(screenTouch) || aimJoystickBounds.contains(screenTouch)) {
            return true;
        }
        // Check buttons
        if (shootButton.contains(screenTouch.x, screenTouch.y) ||
            reloadButton.contains(screenTouch.x, screenTouch.y) ||
            slowMotionButton.contains(screenTouch.x, screenTouch.y) ||
            pauseButton.contains(screenTouch.x, screenTouch.y) ||
            pistolButton.contains(screenTouch.x, screenTouch.y) ||
            shotgunButton.contains(screenTouch.x, screenTouch.y) ||
            rifleButton.contains(screenTouch.x, screenTouch.y)) {
            return true;
        }
        return false;
    }
    
    public int getAimJoystickPointerId() { return aimJoystickPointerId; }
}
