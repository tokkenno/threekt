import { Uniform, Uniforms } from 'threekt';
import { loadImageTexture } from './loadImageTexture';

const urls = import.meta.glob('./assets/cube/{pisa,Park3Med,SwedishRoyalCastle}/*.{png,jpg}', { eager: true, query: '?url', import: 'default' }) as Record<string, string>;
export const cubeFaces = ['px', 'nx', 'py', 'ny', 'pz', 'nz'];

export async function loadExampleCubeMap(folder: 'pisa' | 'Park3Med' | 'SwedishRoyalCastle') {
    const uniforms = new Uniforms();
    const extension = folder === 'pisa' ? 'png' : 'jpg';
    await Promise.all(cubeFaces.map(async face => {
        const texture = await loadImageTexture(urls[`./assets/cube/${folder}/${face}.${extension}`]);
        uniforms.set(face, new Uniform(texture));
    }));
    return uniforms;
}

// WebGL texture coordinates run upward. Keep each face's orientation consistent with a cube sampler.
export const cubeLookup = `
    uniform sampler2D px, nx, py, ny, pz, nz;
    vec3 sampleCube(vec3 d) {
        vec3 a = abs(d);
        if (a.x >= max(a.y, a.z)) {
            if (d.x > 0.0) return texture2D(px, vec2(-d.z, d.y) / a.x * 0.5 + 0.5).rgb;
            return texture2D(nx, vec2(d.z, d.y) / a.x * 0.5 + 0.5).rgb;
        }
        if (a.y >= a.z) {
            if (d.y > 0.0) return texture2D(py, vec2(d.x, -d.z) / a.y * 0.5 + 0.5).rgb;
            return texture2D(ny, vec2(d.x, d.z) / a.y * 0.5 + 0.5).rgb;
        }
        if (d.z > 0.0) return texture2D(pz, vec2(d.x, d.y) / a.z * 0.5 + 0.5).rgb;
        return texture2D(nz, vec2(-d.x, d.y) / a.z * 0.5 + 0.5).rgb;
    }
`;
