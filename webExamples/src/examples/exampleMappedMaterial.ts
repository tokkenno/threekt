import { Color, ColorSpace, Matrix4, ShaderMaterial, Side, Uniform, Uniforms, Vector2, type Texture } from 'threekt';
import { encodeSrgb } from './fullscreenPass';

type Maps = { normal?: Texture; bump?: Texture; diffuse?: Texture; specular?: Texture;
    displacement?: Texture; occlusion?: Texture; matcap?: Texture };
export function exampleMappedMaterial(maps: Maps, objectSpace = false) {
    for (const texture of [maps.normal, maps.bump, maps.specular, maps.displacement, maps.occlusion]) {
        if (texture) texture.colorSpace = ColorSpace.None;
    }
    const tint = new Color(1, 1, 1);
    const scale = new Vector2(1, 1);
    const normalEnabled = new Uniform(true), channel = new Uniform(0), displacement = new Uniform(0);
    const ambient = new Uniform(0.26), roughness = new Uniform(0.55), metalness = new Uniform(0);
    const aoIntensity = new Uniform(1), exposure = new Uniform(1), lightAngle = new Uniform(0);
    const previous = new Matrix4();
    const uniforms = new Uniforms()
        .set('tint', new Uniform(tint)).set('normalScale', new Uniform(scale)).set('normalEnabled', normalEnabled)
        .set('displacementScale', displacement).set('channel', channel).set('previousModelView', new Uniform(previous))
        .set('ambient', ambient).set('roughness', roughness).set('metalness', metalness).set('aoIntensity', aoIntensity)
        .set('exposure', exposure).set('lightAngle', lightAngle);
    const defines = Object.entries(maps).map(([name, texture]) => {
        uniforms.set(name + 'Map', new Uniform(texture));
        return '#define USE_' + name.toUpperCase();
    }).join('\n') + '\n';
    const material = new ShaderMaterial(defines + `
        varying vec2 texcoord;
        varying vec3 viewPosition, viewNormal;
        varying vec4 currentClip, previousClip;
        uniform sampler2D displacementMap;
        uniform float displacementScale;
        uniform mat4 previousModelView;
        void main() {
            texcoord = uv;
            vec3 p = position;
            #ifdef USE_DISPLACEMENT
            p += normal * (texture2D(displacementMap, uv).r * displacementScale - 0.428408);
            #endif
            vec4 mv = modelViewMatrix * vec4(p, 1.0);
            viewPosition = mv.xyz;
            viewNormal = normalMatrix * normal;
            currentClip = projectionMatrix * mv;
            previousClip = projectionMatrix * previousModelView * vec4(p, 1.0);
            gl_Position = currentClip;
        }
    `, defines + `
        varying vec2 texcoord;
        varying vec3 viewPosition, viewNormal;
        varying vec4 currentClip, previousClip;
        uniform vec3 tint;
        uniform vec2 normalScale;
        uniform bool normalEnabled;
        uniform int channel;
        uniform float ambient, roughness, metalness, aoIntensity, exposure, lightAngle;
        uniform mat3 normalMatrix;
        uniform sampler2D normalMap, bumpMap, diffuseMap, specularMap, occlusionMap, matcapMap;
        ${encodeSrgb}
        mat3 tangentFrame(vec3 n) {
            vec3 dx = dFdx(viewPosition), dy = dFdy(viewPosition);
            vec2 u = dFdx(texcoord), v = dFdy(texcoord);
            vec3 t = cross(dy, n) * u.x + cross(n, dx) * v.x;
            vec3 b = cross(dy, n) * u.y + cross(n, dx) * v.y;
            float inv = inversesqrt(max(1e-10, max(dot(t,t), dot(b,b))));
            return mat3(t * inv, b * inv, n);
        }
        void main() {
            vec3 n = normalize(viewNormal);
            #ifdef USE_NORMAL
            if (normalEnabled) {
                vec3 m = texture2D(normalMap, texcoord).rgb * 2.0 - 1.0;
                m.xy *= normalScale;
                n = normalize(${objectSpace ? 'normalMatrix' : 'tangentFrame(n)'} * m);
            }
            #endif
            #ifdef USE_BUMP
            if (normalEnabled) {
                float h = texture2D(bumpMap, texcoord).r * normalScale.x * 0.14;
                vec3 dx = dFdx(viewPosition), dy = dFdy(viewPosition);
                vec3 r1 = cross(dy,n), r2 = cross(n,dx);
                float determinant = dot(dx,r1);
                n = normalize(abs(determinant)*n - sign(determinant)*(dFdx(h)*r1+dFdy(h)*r2));
            }
            #endif
            float depth = gl_FragCoord.z;
            if (channel == 1) { gl_FragColor = vec4(n * 0.5 + 0.5, 1.0); return; }
            if (channel == 2) { gl_FragColor = vec4(vec3(1.0-depth), 1.0); return; }
            if (channel >= 3 && channel <= 5) {
                // Match the channel ordering of Three.js's depth packing.
                float integerPart;
                float alpha = 1.0, blue = 0.0, green;
                if (channel == 5) {
                    alpha = modf(depth * 16777216.0, integerPart);
                    blue = modf(integerPart / 256.0, integerPart) * (256.0 / 255.0);
                    green = modf(integerPart / 256.0, integerPart) * (256.0 / 255.0);
                } else if (channel == 3) {
                    blue = modf(depth * 65536.0, integerPart);
                    green = modf(integerPart / 256.0, integerPart) * (256.0 / 255.0);
                } else {
                    green = modf(depth * 256.0, integerPart);
                }
                gl_FragColor = vec4(integerPart / 255.0, green, blue, alpha);
                return;
            }
            if (channel == 6) {
                vec2 v = currentClip.xy / currentClip.w - previousClip.xy / previousClip.w;
                gl_FragColor = vec4(0.5 + v * 22.0, 0.05, 1.0);
                return;
            }
            vec3 viewDir = normalize(-viewPosition);
            vec3 base = tint;
            #ifdef USE_DIFFUSE
            base *= texture2D(diffuseMap, texcoord).rgb;
            #endif
            #ifdef USE_MATCAP
            vec3 x = normalize(vec3(viewDir.z, 0.0, -viewDir.x));
            vec3 y = cross(viewDir, x);
            vec2 matcapUV = vec2(dot(x,n), dot(y,n)) * 0.495 + 0.5;
            base *= texture2D(matcapMap, matcapUV).rgb;
            #else
            float ao = 1.0;
            #ifdef USE_OCCLUSION
            ao = mix(1.0, texture2D(occlusionMap, texcoord).r, aoIntensity);
            #endif
            vec3 l = normalize(vec3(0.78 * sin(lightAngle) + 0.25, 0.52, 0.8 * cos(lightAngle) + 0.36));
            vec3 h = normalize(l + viewDir);
            float ndl = max(dot(l,n),0.0);
            float specular = pow(max(dot(n,h),0.0), mix(110.0, 8.0, roughness));
            #ifdef USE_SPECULAR
            specular *= texture2D(specularMap,texcoord).r;
            #endif
            base = base * (ambient * ao + ndl * (1.0 - 0.55 * metalness))
                 + specular * mix(vec3(0.32), base, metalness);
            #endif
            gl_FragColor = vec4(encodeSrgb(base * exposure), 1.0);
        }
    `, uniforms);
    material.side = Side.Double;
    return { material, tint, scale, normalEnabled, channel, displacement, ambient, roughness, metalness,
        aoIntensity, exposure, lightAngle, previous };
}
