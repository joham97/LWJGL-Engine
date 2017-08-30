package org.lwjglb.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjglb.engine.graph.InstancedMesh;
import org.lwjglb.engine.graph.Mesh;
import org.lwjglb.engine.graph.particles.IParticleEmitter;
import org.lwjglb.engine.graph.weather.Fog;
import org.lwjglb.engine.items.GameItem;
import org.lwjglb.engine.items.SkyBox;

public class Scene {

    private final Map<Mesh, List<GameItem>> meshMap;

    private final Map<InstancedMesh, List<GameItem>> instancedMeshMap;
    
    private List<GameItem> currentGameItems;

    private SkyBox skyBox;

    private SceneLight sceneLight;

    private Fog fog;

    private boolean renderShadows;
    
    private IParticleEmitter[] particleEmitters;

    public Scene() {
        this.meshMap = new HashMap<>();
        this.instancedMeshMap = new HashMap<>();
        this.currentGameItems = new ArrayList<>();
        this.fog = Fog.NOFOG;
        this.renderShadows = true;
    }

    public Map<Mesh, List<GameItem>> getGameMeshes() {
        return this.meshMap;
    }

    public Map<InstancedMesh, List<GameItem>> getGameInstancedMeshes() {
        return this.instancedMeshMap;
    }

    public boolean isRenderShadows() {
        return this.renderShadows;
    }

	public void updateGameObjects(List<GameItem> updatedGameItems) {
		for (GameItem gameItem : this.currentGameItems) {
			if (!updatedGameItems.contains(gameItem)) {
				removeGameItem(gameItem);
			}
		}
		for (GameItem gameItem : updatedGameItems) {
			if (!this.currentGameItems.contains(gameItem)) {
				addGameItem(gameItem);
			}
		}
		this.currentGameItems.clear();
		this.currentGameItems.addAll(updatedGameItems);
	}
	
    public void addGameItems(List<GameItem> gameItems) {
        // Create a map of meshes to speed up rendering
        int numGameItems = gameItems != null ? gameItems.size() : 0;
        for (int i = 0; i < numGameItems; i++) {
        	addGameItem(gameItems.get(i));
        }
    }

	private void addGameItem(GameItem gameItem) {
        Mesh[] meshes = gameItem.getMeshes();
        for (Mesh mesh : meshes) {
            boolean instancedMesh = mesh instanceof InstancedMesh;
            List<GameItem> list = instancedMesh ? this.instancedMeshMap.get(mesh) : this.meshMap.get(mesh);
            if (list == null) {
                list = new ArrayList<>();
                if (instancedMesh) {
                    this.instancedMeshMap.put((InstancedMesh)mesh, list);
                } else {
                    this.meshMap.put(mesh, list);
                }
            }
            list.add(gameItem);
        }
    }
    
    private void removeGameItem(GameItem gameItemToRemove){
    	if(gameItemToRemove != null){ 
    		for(Entry<Mesh, List<GameItem>> entry : this.meshMap.entrySet()){
    			List<GameItem> items = entry.getValue();
    			if(items.contains(gameItemToRemove)){
    				items.remove(gameItemToRemove);
    			}
    		}
    	}
    }
        
    public void cleanup() {
        for (Mesh mesh : this.meshMap.keySet()) {
            mesh.cleanUp();
        }
        for (Mesh mesh : this.instancedMeshMap.keySet()) {
            mesh.cleanUp();
        }
        if (this.particleEmitters != null) {
            for (IParticleEmitter particleEmitter : this.particleEmitters) {
                particleEmitter.cleanup();
            }
        }
    }

    public SkyBox getSkyBox() {
        return this.skyBox;
    }

    public void setRenderShadows(boolean renderShadows) {
        this.renderShadows = renderShadows;
    }

    public void setSkyBox(SkyBox skyBox) {
        this.skyBox = skyBox;
    }

    public SceneLight getSceneLight() {
        return this.sceneLight;
    }

    public void setSceneLight(SceneLight sceneLight) {
        this.sceneLight = sceneLight;
    }

    /**
     * @return the fog
     */
    public Fog getFog() {
        return this.fog;
    }

    /**
     * @param fog the fog to set
     */
    public void setFog(Fog fog) {
        this.fog = fog;
    }

    public IParticleEmitter[] getParticleEmitters() {
        return this.particleEmitters;
    }

    public void setParticleEmitters(IParticleEmitter[] particleEmitters) {
        this.particleEmitters = particleEmitters;
    }

    @Override
    public String toString() {
    	StringBuilder stringBuilder = new StringBuilder();
    	stringBuilder.append("Mesh Count: " + this.meshMap.size());
    	stringBuilder.append("\n");
    	int i = 0;
    	for(Entry<Mesh, List<GameItem>> entry : this.meshMap.entrySet()){
        	stringBuilder.append("Mesh[");
        	stringBuilder.append(i);
        	stringBuilder.append("]: ");
        	stringBuilder.append(entry.getValue().size());
        	stringBuilder.append(" Game Items");
        	stringBuilder.append("\n");
		}
    	return stringBuilder.toString();
    }
}
