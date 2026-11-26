import {
    AmbientLight, Color, DirectionalLight, Fog, InstancedMesh, MeshPhongMaterial, Object3D, PerspectiveCamera,
    Scene, SphereGeometry,
} from 'threekt';

export function createPostprocessingCloud(colored: boolean) {
    const scene = new Scene();
    scene.fog = new Fog(Color.fromHex(0x000000), 1, 1000);
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.z = 400;
    const material = new MeshPhongMaterial(Color.fromHex(0xffffff));
    material.flatShading = true;
    const cloud = new InstancedMesh(new SphereGeometry(1, 4, 4), material, 100);
    const dummy = new Object3D();
    for (let i = 0; i < cloud.count; i++) {
        dummy.position.set(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5)
            .normalize().multiplyScalar(Math.random() * 400);
        dummy.rotation.set(Math.random() * 2, Math.random() * 2, Math.random() * 2);
        dummy.scale.setScalar(Math.random() * 49 + 1);
        dummy.updateMatrix();
        cloud.setMatrixAt(i, dummy.matrix);
        if (colored) cloud.setColorAt(i, Color.fromHex(Math.floor(Math.random() * 0x1000000)));
    }
    scene.add(cloud);
    scene.add(new AmbientLight(Color.fromHex(0xcccccc)));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(1, 1, 1);
    scene.add(light);
    return { scene, camera, cloud };
}
