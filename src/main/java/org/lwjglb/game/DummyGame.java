package org.lwjglb.game;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;

import org.joml.Vector3f;
import org.lwjgl.openal.AL11;
import org.lwjglb.engine.IGameLogic;
import org.lwjglb.engine.MouseInput;
import org.lwjglb.engine.Scene;
import org.lwjglb.engine.SceneLight;
import org.lwjglb.engine.Window;
import org.lwjglb.engine.graph.Camera;
import org.lwjglb.engine.graph.Renderer;
import org.lwjglb.engine.items.GameItem;
import org.lwjglb.engine.sound.SoundManager;

public class DummyGame implements IGameLogic {

    private final Vector3f cameraInc;

    private final Renderer renderer;

    private final SoundManager soundMgr;

    private final Camera camera;

    private Scene scene;
    
    private GameItem[] gameItems;

    public DummyGame() {
        this.renderer = new Renderer();
        this.soundMgr = new SoundManager();
        this.camera = new Camera();
        this.cameraInc = new Vector3f(0.0f, 0.0f, 0.0f);
    }

    @Override
    public void init(Window window) throws Exception {
        this.renderer.init(window);
        this.soundMgr.init();

        this.scene = new Scene();

        this.gameItems = new GameItem[0];
       
        this.scene.setGameItems(this.gameItems);

        // Shadows
        this.scene.setRenderShadows(false);

        // Setup Lights
        setupLights();

        this.camera.getPosition().x = 0f;
        this.camera.getPosition().y = 0f;
        this.camera.getPosition().z = 0f;
        this.camera.getRotation().x = 0;
        this.camera.getRotation().y = 0;

        // Sounds
        this.soundMgr.init();
        this.soundMgr.setAttenuationModel(AL11.AL_EXPONENT_DISTANCE);
        setupSounds();
    }

    private void setupLights() {
        SceneLight sceneLight = new SceneLight();
        this.scene.setSceneLight(sceneLight);

        // Ambient Light
        sceneLight.setAmbientLight(new Vector3f(0.3f, 0.3f, 0.3f));
    }

    private void setupSounds() throws Exception {
        
    }

    @Override
    public void input(Window window, MouseInput mouseInput) {
        this.cameraInc.set(0, 0, 0);
        if (window.isKeyPressed(GLFW_KEY_W)) {
        	
        } 
        if (window.isKeyPressed(GLFW_KEY_S)) {
        	
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
        	
        } 
        if (window.isKeyPressed(GLFW_KEY_D)) {
        	
        }
        if (window.isKeyPressed(GLFW_KEY_LEFT)) {

        } else if (window.isKeyPressed(GLFW_KEY_RIGHT)) {

        }
    }

    @Override
    public void update(float interval, MouseInput mouseInput, Window window) {
        
    }

    @Override
    public void render(Window window) {
        this.renderer.render(window, this.camera, this.scene);
    }

    @Override
    public void cleanup() {
        this.renderer.cleanup();
        this.soundMgr.cleanup();

        this.scene.cleanup();
    }
}
