package org.lwjglb.engine.graph;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glVertex3f;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjglb.engine.Scene;
import org.lwjglb.engine.SceneLight;
import org.lwjglb.engine.Utils;
import org.lwjglb.engine.Window;
import org.lwjglb.engine.graph.anim.AnimGameItem;
import org.lwjglb.engine.graph.anim.AnimatedFrame;
import org.lwjglb.engine.graph.lights.DirectionalLight;
import org.lwjglb.engine.graph.lights.PointLight;
import org.lwjglb.engine.graph.lights.SpotLight;
import org.lwjglb.engine.graph.particles.IParticleEmitter;
import org.lwjglb.engine.items.GameItem;
import org.lwjglb.engine.items.SkyBox;

public class Renderer {

    private static final int MAX_POINT_LIGHTS = 5;

    private static final int MAX_SPOT_LIGHTS = 5;

	private static final boolean RENDER_AXES = false;

	private static final boolean RENDER_CROSS_HAIR = false;

    private final Transformation transformation;

    private ShadowMap shadowMap;

    private ShaderProgram depthShaderProgram;

    private ShaderProgram sceneShaderProgram;

    private ShaderProgram skyBoxShaderProgram;

    private ShaderProgram particlesShaderProgram;

    private final float specularPower;

    public Renderer() {
        this.transformation = new Transformation();
        this.specularPower = 10f;
    }

    public void init(Window window) throws Exception {
        this.shadowMap = new ShadowMap();

        setupDepthShader();
        setupSkyBoxShader();
        setupSceneShader();
        setupParticlesShader();
    }

    public void render(Window window, Camera camera, Scene scene) {
        clear();

        // Render depth map before view ports has been set up
        renderDepthMap(window, camera, scene);

        glViewport(0, 0, window.getWidth(), window.getHeight());

        // Update projection matrix once per render cycle
        window.updateProjectionMatrix();

        renderScene(window, camera, scene);
        renderSkyBox(window, camera, scene);
        renderParticles(window, camera, scene);

        if(RENDER_AXES){
        	renderAxes(window, camera);
        }
        if(RENDER_CROSS_HAIR){
        	renderCrossHair(window);
        }
    }

    private void setupParticlesShader() throws Exception {
        this.particlesShaderProgram = new ShaderProgram();
        this.particlesShaderProgram.createVertexShader(Utils.loadResource("/shaders/particles_vertex.vs"));
        this.particlesShaderProgram.createFragmentShader(Utils.loadResource("/shaders/particles_fragment.fs"));
        this.particlesShaderProgram.link();

        this.particlesShaderProgram.createUniform("projectionMatrix");
        this.particlesShaderProgram.createUniform("texture_sampler");

        this.particlesShaderProgram.createUniform("numCols");
        this.particlesShaderProgram.createUniform("numRows");
    }

    private void setupDepthShader() throws Exception {
        this.depthShaderProgram = new ShaderProgram();
        this.depthShaderProgram.createVertexShader(Utils.loadResource("/shaders/depth_vertex.vs"));
        this.depthShaderProgram.createFragmentShader(Utils.loadResource("/shaders/depth_fragment.fs"));
        this.depthShaderProgram.link();

        this.depthShaderProgram.createUniform("isInstanced");
        this.depthShaderProgram.createUniform("jointsMatrix");
        this.depthShaderProgram.createUniform("modelLightViewNonInstancedMatrix");
        this.depthShaderProgram.createUniform("orthoProjectionMatrix");
    }

    private void setupSkyBoxShader() throws Exception {
        this.skyBoxShaderProgram = new ShaderProgram();
        this.skyBoxShaderProgram.createVertexShader(Utils.loadResource("/shaders/sb_vertex.vs"));
        this.skyBoxShaderProgram.createFragmentShader(Utils.loadResource("/shaders/sb_fragment.fs"));
        this.skyBoxShaderProgram.link();

        // Create uniforms for projection matrix
        this.skyBoxShaderProgram.createUniform("projectionMatrix");
        this.skyBoxShaderProgram.createUniform("modelViewMatrix");
        this.skyBoxShaderProgram.createUniform("texture_sampler");
        this.skyBoxShaderProgram.createUniform("ambientLight");
        this.skyBoxShaderProgram.createUniform("colour");
        this.skyBoxShaderProgram.createUniform("hasTexture");
    }

    private void setupSceneShader() throws Exception {
        // Create shader
        this.sceneShaderProgram = new ShaderProgram();
        this.sceneShaderProgram.createVertexShader(Utils.loadResource("/shaders/scene_vertex.vs"));
        this.sceneShaderProgram.createFragmentShader(Utils.loadResource("/shaders/scene_fragment.fs"));
        this.sceneShaderProgram.link();

        // Create uniforms for modelView and projection matrices
        this.sceneShaderProgram.createUniform("projectionMatrix");
        this.sceneShaderProgram.createUniform("modelViewNonInstancedMatrix");
        this.sceneShaderProgram.createUniform("texture_sampler");
        this.sceneShaderProgram.createUniform("normalMap");
        // Create uniform for material
        this.sceneShaderProgram.createMaterialUniform("material");
        // Create lighting related uniforms
        this.sceneShaderProgram.createUniform("specularPower");
        this.sceneShaderProgram.createUniform("ambientLight");
        this.sceneShaderProgram.createPointLightListUniform("pointLights", MAX_POINT_LIGHTS);
        this.sceneShaderProgram.createSpotLightListUniform("spotLights", MAX_SPOT_LIGHTS);
        this.sceneShaderProgram.createDirectionalLightUniform("directionalLight");
        this.sceneShaderProgram.createFogUniform("fog");

        // Create uniforms for shadow mapping
        this.sceneShaderProgram.createUniform("shadowMap");
        this.sceneShaderProgram.createUniform("orthoProjectionMatrix");
        this.sceneShaderProgram.createUniform("modelLightViewNonInstancedMatrix");
        this.sceneShaderProgram.createUniform("renderShadow");

        // Create uniform for joint matrices
        this.sceneShaderProgram.createUniform("jointsMatrix");

        this.sceneShaderProgram.createUniform("isInstanced");
        this.sceneShaderProgram.createUniform("numCols");
        this.sceneShaderProgram.createUniform("numRows");

        this.sceneShaderProgram.createUniform("selectedNonInstanced");
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    private void renderParticles(Window window, Camera camera, Scene scene) {
        this.particlesShaderProgram.bind();

        this.particlesShaderProgram.setUniform("texture_sampler", 0);
        Matrix4f projectionMatrix = window.getProjectionMatrix();
        this.particlesShaderProgram.setUniform("projectionMatrix", projectionMatrix);

        Matrix4f viewMatrix = camera.getViewMatrix();
        IParticleEmitter[] emitters = scene.getParticleEmitters();
        int numEmitters = emitters != null ? emitters.length : 0;

        glDepthMask(false);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        for (int i = 0; i < numEmitters; i++) {
            IParticleEmitter emitter = emitters[i];
            InstancedMesh mesh = (InstancedMesh) emitter.getBaseParticle().getMesh();

            Texture text = mesh.getMaterial().getTexture();
            this.particlesShaderProgram.setUniform("numCols", text.getNumCols());
            this.particlesShaderProgram.setUniform("numRows", text.getNumRows());

            mesh.renderListInstanced(emitter.getParticles(), true, this.transformation, viewMatrix, null);
        }

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(true);

        this.particlesShaderProgram.unbind();
    }

    private void renderDepthMap(Window window, Camera camera, Scene scene) {
        if (scene.isRenderShadows()) {
            // Setup view port to match the texture size
            glBindFramebuffer(GL_FRAMEBUFFER, this.shadowMap.getDepthMapFBO());
            glViewport(0, 0, ShadowMap.SHADOW_MAP_WIDTH, ShadowMap.SHADOW_MAP_HEIGHT);
            glClear(GL_DEPTH_BUFFER_BIT);

            this.depthShaderProgram.bind();

            DirectionalLight light = scene.getSceneLight().getDirectionalLight();
            Vector3f lightDirection = light.getDirection();

            float lightAngleX = (float) Math.toDegrees(Math.acos(lightDirection.z));
            float lightAngleY = (float) Math.toDegrees(Math.asin(lightDirection.x));
            float lightAngleZ = 0;
            Matrix4f lightViewMatrix = this.transformation.updateLightViewMatrix(new Vector3f(lightDirection).mul(light.getShadowPosMult()), new Vector3f(lightAngleX, lightAngleY, lightAngleZ));
            DirectionalLight.OrthoCoords orthCoords = light.getOrthoCoords();
            Matrix4f orthoProjMatrix = this.transformation.updateOrthoProjectionMatrix(orthCoords.left, orthCoords.right, orthCoords.bottom, orthCoords.top, orthCoords.near, orthCoords.far);

            this.depthShaderProgram.setUniform("orthoProjectionMatrix", orthoProjMatrix);

            renderNonInstancedMeshes(scene, this.depthShaderProgram, null, lightViewMatrix);

            renderInstancedMeshes(scene, this.depthShaderProgram, null, lightViewMatrix);

            // Unbind
            this.depthShaderProgram.unbind();
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    private void renderSkyBox(Window window, Camera camera, Scene scene) {
        SkyBox skyBox = scene.getSkyBox();
        if (skyBox != null) {
            this.skyBoxShaderProgram.bind();

            this.skyBoxShaderProgram.setUniform("texture_sampler", 0);

            Matrix4f projectionMatrix = window.getProjectionMatrix();
            this.skyBoxShaderProgram.setUniform("projectionMatrix", projectionMatrix);
            Matrix4f viewMatrix = camera.getViewMatrix();
            float m30 = viewMatrix.m30();
            viewMatrix.m30(0);
            float m31 = viewMatrix.m31();
            viewMatrix.m31(0);
            float m32 = viewMatrix.m32();
            viewMatrix.m32(0);

            Mesh mesh = skyBox.getMesh();
            Matrix4f modelViewMatrix = this.transformation.buildModelViewMatrix(skyBox, viewMatrix);
            this.skyBoxShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
            this.skyBoxShaderProgram.setUniform("ambientLight", scene.getSceneLight().getSkyBoxLight());
            this.skyBoxShaderProgram.setUniform("colour", mesh.getMaterial().getAmbientColour());
            this.skyBoxShaderProgram.setUniform("hasTexture", mesh.getMaterial().isTextured() ? 1 : 0);

            mesh.render();

            viewMatrix.m30(m30);
            viewMatrix.m31(m31);
            viewMatrix.m32(m32);
            this.skyBoxShaderProgram.unbind();
        }
    }

    public void renderScene(Window window, Camera camera, Scene scene) {
        this.sceneShaderProgram.bind();

        Matrix4f projectionMatrix = window.getProjectionMatrix();
        this.sceneShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        Matrix4f orthoProjMatrix = this.transformation.getOrthoProjectionMatrix();
        this.sceneShaderProgram.setUniform("orthoProjectionMatrix", orthoProjMatrix);
        Matrix4f lightViewMatrix = this.transformation.getLightViewMatrix();
        Matrix4f viewMatrix = camera.getViewMatrix();

        SceneLight sceneLight = scene.getSceneLight();
        renderLights(viewMatrix, sceneLight);

        this.sceneShaderProgram.setUniform("fog", scene.getFog());
        this.sceneShaderProgram.setUniform("texture_sampler", 0);
        this.sceneShaderProgram.setUniform("normalMap", 1);
        this.sceneShaderProgram.setUniform("shadowMap", 2);
        this.sceneShaderProgram.setUniform("renderShadow", scene.isRenderShadows() ? 1 : 0);

        renderNonInstancedMeshes(scene, this.sceneShaderProgram, viewMatrix, lightViewMatrix);

        renderInstancedMeshes(scene, this.sceneShaderProgram, viewMatrix, lightViewMatrix);

        this.sceneShaderProgram.unbind();
    }

    private void renderNonInstancedMeshes(Scene scene, ShaderProgram shader, Matrix4f viewMatrix, Matrix4f lightViewMatrix) {
        this.sceneShaderProgram.setUniform("isInstanced", 0);

        // Render each mesh with the associated game Items
        Map<Mesh, List<GameItem>> mapMeshes = scene.getGameMeshes();
        for (Mesh mesh : mapMeshes.keySet()) {
            if (viewMatrix != null) {
                shader.setUniform("material", mesh.getMaterial());
                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, this.shadowMap.getDepthMapTexture().getId());
            }

            Texture text = mesh.getMaterial().getTexture();
            if (text != null) {
                this.sceneShaderProgram.setUniform("numCols", text.getNumCols());
                this.sceneShaderProgram.setUniform("numRows", text.getNumRows());
            }

            mesh.renderList(mapMeshes.get(mesh), (GameItem gameItem) -> {
                this.sceneShaderProgram.setUniform("selectedNonInstanced", gameItem.isSelected() ? 1.0f : 0.0f);
                Matrix4f modelMatrix = this.transformation.buildModelMatrix(gameItem);
                if (viewMatrix != null) {
                    Matrix4f modelViewMatrix = this.transformation.buildModelViewMatrix(modelMatrix, viewMatrix);
                    this.sceneShaderProgram.setUniform("modelViewNonInstancedMatrix", modelViewMatrix);
                }
                Matrix4f modelLightViewMatrix = this.transformation.buildModelLightViewMatrix(modelMatrix, lightViewMatrix);
                this.sceneShaderProgram.setUniform("modelLightViewNonInstancedMatrix", modelLightViewMatrix);

                if (gameItem instanceof AnimGameItem) {
                    AnimGameItem animGameItem = (AnimGameItem) gameItem;
                    AnimatedFrame frame = animGameItem.getCurrentFrame();
                    shader.setUniform("jointsMatrix", frame.getJointMatrices());
                }
            }
            );
        }
    }

    private void renderInstancedMeshes(Scene scene, ShaderProgram shader, Matrix4f viewMatrix, Matrix4f lightViewMatrix) {
        shader.setUniform("isInstanced", 1);

        // Render each mesh with the associated game Items
        Map<InstancedMesh, List<GameItem>> mapMeshes = scene.getGameInstancedMeshes();
        for (InstancedMesh mesh : mapMeshes.keySet()) {
            Texture text = mesh.getMaterial().getTexture();
            if (text != null) {
                this.sceneShaderProgram.setUniform("numCols", text.getNumCols());
                this.sceneShaderProgram.setUniform("numRows", text.getNumRows());
            }

            if (viewMatrix != null) {
                shader.setUniform("material", mesh.getMaterial());
                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, this.shadowMap.getDepthMapTexture().getId());
            }
            mesh.renderListInstanced(mapMeshes.get(mesh), this.transformation, viewMatrix, lightViewMatrix);
        }
    }

    private void renderLights(Matrix4f viewMatrix, SceneLight sceneLight) {

        this.sceneShaderProgram.setUniform("ambientLight", sceneLight.getAmbientLight());
        this.sceneShaderProgram.setUniform("specularPower", this.specularPower);

        // Process Point Lights
        PointLight[] pointLightList = sceneLight.getPointLightList();
        int numLights = pointLightList != null ? pointLightList.length : 0;
        for (int i = 0; i < numLights; i++) {
            // Get a copy of the point light object and transform its position to view coordinates
            PointLight currPointLight = new PointLight(pointLightList[i]);
            Vector3f lightPos = currPointLight.getPosition();
            Vector4f aux = new Vector4f(lightPos, 1);
            aux.mul(viewMatrix);
            lightPos.x = aux.x;
            lightPos.y = aux.y;
            lightPos.z = aux.z;
            this.sceneShaderProgram.setUniform("pointLights", currPointLight, i);
        }

        // Process Spot Ligths
        SpotLight[] spotLightList = sceneLight.getSpotLightList();
        numLights = spotLightList != null ? spotLightList.length : 0;
        for (int i = 0; i < numLights; i++) {
            // Get a copy of the spot light object and transform its position and cone direction to view coordinates
            SpotLight currSpotLight = new SpotLight(spotLightList[i]);
            Vector4f dir = new Vector4f(currSpotLight.getConeDirection(), 0);
            dir.mul(viewMatrix);
            currSpotLight.setConeDirection(new Vector3f(dir.x, dir.y, dir.z));

            Vector3f lightPos = currSpotLight.getPointLight().getPosition();
            Vector4f aux = new Vector4f(lightPos, 1);
            aux.mul(viewMatrix);
            lightPos.x = aux.x;
            lightPos.y = aux.y;
            lightPos.z = aux.z;

            this.sceneShaderProgram.setUniform("spotLights", currSpotLight, i);
        }

        if(sceneLight.getDirectionalLight() != null){
	        // Get a copy of the directional light object and transform its position to view coordinates
	        DirectionalLight currDirLight = new DirectionalLight(sceneLight.getDirectionalLight());
	        Vector4f dir = new Vector4f(currDirLight.getDirection(), 0);
	        dir.mul(viewMatrix);
	        currDirLight.setDirection(new Vector3f(dir.x, dir.y, dir.z));
	        this.sceneShaderProgram.setUniform("directionalLight", currDirLight);
        }
    }

    private void renderCrossHair(Window window) {
        if (window.getWindowOptions().compatibleProfile) {
            glPushMatrix();
            glLoadIdentity();

            float inc = 0.05f;
            glLineWidth(2.0f);

            glBegin(GL_LINES);

            glColor3f(1.0f, 1.0f, 1.0f);

            // Horizontal line
            glVertex3f(-inc, 0.0f, 0.0f);
            glVertex3f(+inc, 0.0f, 0.0f);
            glEnd();

            // Vertical line
            glBegin(GL_LINES);
            glVertex3f(0.0f, -inc, 0.0f);
            glVertex3f(0.0f, +inc, 0.0f);
            glEnd();

            glPopMatrix();
        }
    }

    /**
     * Renders the three axis in space (For debugging purposes only
     *
     * @param camera
     */
    private void renderAxes(Window window, Camera camera) {
        Window.WindowOptions opts = window.getWindowOptions();
        if (opts.compatibleProfile) {
            glPushMatrix();
            glLoadIdentity();
            float rotX = camera.getRotation().x;
            float rotY = camera.getRotation().y;
            float rotZ = 0;
            glRotatef(rotX, 1.0f, 0.0f, 0.0f);
            glRotatef(rotY, 0.0f, 1.0f, 0.0f);
            glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
            glLineWidth(2.0f);

            glBegin(GL_LINES);
            // X Axis
            glColor3f(1.0f, 0.0f, 0.0f);
            glVertex3f(0.0f, 0.0f, 0.0f);
            glVertex3f(1.0f, 0.0f, 0.0f);
            // Y Axis
            glColor3f(0.0f, 1.0f, 0.0f);
            glVertex3f(0.0f, 0.0f, 0.0f);
            glVertex3f(0.0f, 1.0f, 0.0f);
            // Z Axis
            glColor3f(1.0f, 1.0f, 1.0f);
            glVertex3f(0.0f, 0.0f, 0.0f);
            glVertex3f(0.0f, 0.0f, 1.0f);
            glEnd();

            glPopMatrix();
        }
    }

    public void cleanup() {
        if (this.shadowMap != null) {
            this.shadowMap.cleanup();
        }
        if (this.depthShaderProgram != null) {
            this.depthShaderProgram.cleanup();
        }
        if (this.skyBoxShaderProgram != null) {
            this.skyBoxShaderProgram.cleanup();
        }
        if (this.sceneShaderProgram != null) {
            this.sceneShaderProgram.cleanup();
        }
        if (this.particlesShaderProgram != null) {
            this.particlesShaderProgram.cleanup();
        }
    }
}
