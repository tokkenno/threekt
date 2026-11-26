package com.aitorgf.threekt.renderers.shaders

/**
 * GLSL ES 3.00 sources of the built-in materials, assembled from chunks condensed from the Three.js
 * `ShaderChunk`s with the same uniform names and `#define` switches. The renderer prepends
 * `#version 300 es` and the defines.
 */
internal object ShaderLib {
    private val commonVertex = """
        precision highp float;
        precision highp int;
        precision highp sampler2DArray;

        uniform mat4 modelViewMatrix;
        uniform mat4 projectionMatrix;

        in vec3 position;

        bool isPerspectiveMatrix( mat4 m ) {
            return m[ 2 ][ 3 ] == - 1.0;
        }
    """.trimIndent()

    private val colorParsVertex = """
        #if defined( USE_COLOR ) || defined( USE_INSTANCING_COLOR )
        out vec4 vColor;
        #endif
        #ifdef USE_COLOR
        // Three-component colors read 1.0 as their alpha.
        in vec4 color;
        #endif
        #ifdef USE_INSTANCING_COLOR
        in vec3 instanceColor;
        #endif
    """.trimIndent()

    private val colorVertex = """
        #if defined( USE_COLOR ) || defined( USE_INSTANCING_COLOR )
        vColor = vec4( 1.0 );
        #endif
        #ifdef USE_COLOR
        vColor *= color;
        #endif
        #ifdef USE_INSTANCING_COLOR
        vColor.xyz *= instanceColor.xyz;
        #endif
    """.trimIndent()

    /** Per-instance transform of `InstancedMesh`, applied before the model-view matrix. */
    private val instancingParsVertex = """
        #ifdef USE_INSTANCING
        in mat4 instanceMatrix;
        #endif
    """.trimIndent()

    /**
     * Transforms `objectNormal` by the instance matrix, dividing by the squared axis scales so that
     * non-uniform scaling keeps normals perpendicular, then by `normalMatrix`, as `defaultnormal_vertex`.
     */
    private val defaultnormalVertex = """
        vec3 objectNormal = vec3( normal );
        #ifdef USE_INSTANCING
        mat3 im = mat3( instanceMatrix );
        objectNormal /= vec3( dot( im[ 0 ], im[ 0 ] ), dot( im[ 1 ], im[ 1 ] ), dot( im[ 2 ], im[ 2 ] ) );
        objectNormal = im * objectNormal;
        #endif
        vec3 transformedNormal = normalMatrix * objectNormal;
        #ifdef FLIP_SIDED
        transformedNormal = - transformedNormal;
        #endif
    """.trimIndent()

    private val morphtargetParsVertex = """
        #ifdef USE_MORPHTARGETS
        uniform float morphTargetBaseInfluence;
        uniform float morphTargetInfluences[ MORPHTARGETS_COUNT ];
        uniform sampler2DArray morphTargetsTexture;
        uniform ivec2 morphTargetsTextureSize;

        vec4 getMorph( const in int vertexIndex, const in int morphTargetIndex, const in int offset ) {
            int texelIndex = vertexIndex * MORPHTARGETS_TEXTURE_STRIDE + offset;
            int y = texelIndex / morphTargetsTextureSize.x;
            int x = texelIndex - y * morphTargetsTextureSize.x;
            ivec3 morphUV = ivec3( x, y, morphTargetIndex );
            return texelFetch( morphTargetsTexture, morphUV, 0 );
        }
        #endif
    """.trimIndent()

    /** Begins `transformed` from the position and applies the morph targets. */
    private val beginMorphtargetVertex = """
        vec3 transformed = vec3( position );

        #ifdef USE_MORPHTARGETS
        // With absolute targets the base influence is 1 - sum(influences); with relative targets it is 1.
        transformed *= morphTargetBaseInfluence;
        for ( int i = 0; i < MORPHTARGETS_COUNT; i ++ ) {
            if ( morphTargetInfluences[ i ] != 0.0 ) transformed += getMorph( gl_VertexID, i, 0 ).xyz * morphTargetInfluences[ i ];
        }
        #endif
    """.trimIndent()

    private val projectVertex = """
        vec4 mvPosition = vec4( transformed, 1.0 );
        #ifdef USE_INSTANCING
        mvPosition = instanceMatrix * mvPosition;
        #endif
        mvPosition = modelViewMatrix * mvPosition;
        gl_Position = projectionMatrix * mvPosition;
    """.trimIndent()

    private val fogParsVertex = """
        #ifdef USE_FOG
        out float vFogDepth;
        #endif
    """.trimIndent()

    private val fogVertex = """
        #ifdef USE_FOG
        vFogDepth = - mvPosition.z;
        #endif
    """.trimIndent()

    private val commonFragment = """
        precision highp float;

        uniform vec3 diffuse;
        uniform float opacity;

        out vec4 pc_fragColor;

        vec4 sRGBTransferOETF( in vec4 value ) {
            return vec4( mix( pow( value.rgb, vec3( 0.41666 ) ) * 1.055 - vec3( 0.055 ), value.rgb * 12.92, vec3( lessThanEqual( value.rgb, vec3( 0.0031308 ) ) ) ), value.a );
        }
    """.trimIndent()

    private val colorParsFragment = """
        #if defined( USE_COLOR ) || defined( USE_INSTANCING_COLOR )
        in vec4 vColor;
        #endif
    """.trimIndent()

    private val colorFragment = """
        #if defined( USE_COLOR ) || defined( USE_INSTANCING_COLOR )
        diffuseColor *= vColor;
        #endif
    """.trimIndent()

    private val fogParsFragment = """
        #ifdef USE_FOG
        uniform vec3 fogColor;
        in float vFogDepth;
        #ifdef FOG_EXP2
        uniform float fogDensity;
        #else
        uniform float fogNear;
        uniform float fogFar;
        #endif
        #endif
    """.trimIndent()

    /**
     * Writes the color with full alpha for opaque materials, encodes it as sRGB, blends in the fog, whose
     * color is already sRGB, and premultiplies the alpha when requested, in the order Three.js uses.
     * Expects `outgoingLight` and `diffuseColor`.
     */
    private val outputFragment = """
        #ifdef USE_ALPHATEST
        if ( diffuseColor.a < alphaTest ) discard;
        #endif
        #ifdef OPAQUE
        diffuseColor.a = 1.0;
        #endif

        #ifdef LINEAR_OUTPUT
            pc_fragColor = vec4( outgoingLight, diffuseColor.a );
        #else
            pc_fragColor = sRGBTransferOETF( vec4( outgoingLight, diffuseColor.a ) );
        #endif

        #ifdef USE_FOG
        #ifdef FOG_EXP2
        float fogFactor = 1.0 - exp( -fogDensity * fogDensity * vFogDepth * vFogDepth );
        #else
        float fogFactor = smoothstep( fogNear, fogFar, vFogDepth );
        #endif
        pc_fragColor.rgb = mix( pc_fragColor.rgb, fogColor, fogFactor );
        #endif

        #ifdef PREMULTIPLIED_ALPHA
        pc_fragColor.rgb *= pc_fragColor.a;
        #endif
    """.trimIndent()

    private val bsdfs = """
        #define RECIPROCAL_PI 0.3183098861837907
        #define saturate( a ) clamp( a, 0.0, 1.0 )
        float pow2( const in float x ) { return x * x; }
        float pow4( const in float x ) { float x2 = x * x; return x2 * x2; }

        vec3 F_Schlick( const in vec3 f0, const in float f90, const in float dotVH ) {
            float fresnel = exp2( ( - 5.55473 * dotVH - 6.98316 ) * dotVH );
            return f0 * ( 1.0 - fresnel ) + ( f90 * fresnel );
        }

        vec3 BRDF_Lambert( const in vec3 diffuseColor ) {
            return RECIPROCAL_PI * diffuseColor;
        }

        float G_BlinnPhong_Implicit( /* const in float dotNL, const in float dotNV */ ) {
            // geometry term is (n dot l)(n dot v) / 4(n dot l)(n dot v)
            return 0.25;
        }

        float D_BlinnPhong( const in float shininess, const in float dotNH ) {
            return RECIPROCAL_PI * ( shininess * 0.5 + 1.0 ) * pow( dotNH, shininess );
        }

        vec3 BRDF_BlinnPhong( const in vec3 lightDir, const in vec3 viewDir, const in vec3 normal, const in vec3 specularColor, const in float shininess ) {
            vec3 halfDir = normalize( lightDir + viewDir );
            float dotNH = saturate( dot( normal, halfDir ) );
            float dotVH = saturate( dot( viewDir, halfDir ) );
            vec3 F = F_Schlick( specularColor, 1.0, dotVH );
            float G = G_BlinnPhong_Implicit( /* dotNL, dotNV */ );
            float D = D_BlinnPhong( shininess, dotNH );
            return F * ( G * D );
        }
    """.trimIndent()

    private val lightsParsBegin = """
        uniform bool isOrthographic;
        uniform vec3 ambientLightColor;

        #if NUM_DIR_LIGHTS > 0
        struct DirectionalLight {
            vec3 direction;
            vec3 color;
        };
        uniform DirectionalLight directionalLights[ NUM_DIR_LIGHTS ];
        #endif

        #if NUM_POINT_LIGHTS > 0
        struct PointLight {
            vec3 position;
            vec3 color;
            float distance;
            float decay;
        };
        uniform PointLight pointLights[ NUM_POINT_LIGHTS ];
        #endif

        float getDistanceAttenuation( const in float lightDistance, const in float cutoffDistance, const in float decayExponent ) {
            float distanceFalloff = 1.0 / max( pow( lightDistance, decayExponent ), 0.01 );
            if ( cutoffDistance > 0.0 ) {
                distanceFalloff *= pow2( saturate( 1.0 - pow4( lightDistance / cutoffDistance ) ) );
            }
            return distanceFalloff;
        }

        #if NUM_HEMI_LIGHTS > 0
        struct HemisphereLight {
            vec3 direction;
            vec3 skyColor;
            vec3 groundColor;
        };
        uniform HemisphereLight hemisphereLights[ NUM_HEMI_LIGHTS ];

        vec3 getHemisphereLightIrradiance( const in HemisphereLight light, const in vec3 normal ) {
            float dotNL = dot( normal, light.direction );
            float hemiDiffuseWeight = 0.5 * dotNL + 0.5;
            return mix( light.groundColor, light.skyColor, hemiDiffuseWeight );
        }
        #endif
    """.trimIndent()

    /** Vertex shader of `MeshBasicMaterial` and `LineBasicMaterial`. */
    val basicVertex = """
        |$commonVertex
        |
        |#ifdef USE_MAP
        |in vec2 uv;
        |uniform mat3 mapTransform;
        |out vec2 vMapUv;
        |#endif
        |
        |$colorParsVertex
        |$instancingParsVertex
        |$morphtargetParsVertex
        |$fogParsVertex
        |
        |#ifdef USE_DASH
        |in float lineDistance;
        |uniform float scale;
        |out float vLineDistance;
        |#endif
        |void main() {
        |    #ifdef USE_DASH
        |    vLineDistance = scale * lineDistance;
        |    #endif
        |    #ifdef USE_MAP
        |    vMapUv = ( mapTransform * vec3( uv, 1 ) ).xy;
        |    #endif
        |
        |    $colorVertex
        |    $beginMorphtargetVertex
        |    $projectVertex
        |    $fogVertex
        |}
    """.trimMargin()

    /** Fragment shader of `MeshBasicMaterial` and `LineBasicMaterial`. */
    val basicFragment = """
        |$commonFragment
        |
        |#ifdef USE_MAP
        |uniform sampler2D map;
        |in vec2 vMapUv;
        |#endif
        |
        |$colorParsFragment
        |$fogParsFragment
        |#ifdef USE_ALPHATEST
        |uniform float alphaTest;
        |#endif
        |
        |#ifdef USE_DASH
        |uniform float dashSize;
        |uniform float totalSize;
        |in float vLineDistance;
        |#endif
        |void main() {
        |    #ifdef USE_DASH
        |    if (totalSize > 0.0 && mod(vLineDistance, totalSize) > dashSize) discard;
        |    #endif
        |    vec4 diffuseColor = vec4( diffuse, opacity );
        |
        |    #ifdef USE_MAP
        |    diffuseColor *= texture( map, vMapUv );
        |    #endif
        |
        |    $colorFragment
        |
        |    vec3 outgoingLight = diffuseColor.rgb;
        |    $outputFragment
        |}
    """.trimMargin()

    /**
     * Vertex shader of `PointsMaterial`. Points are [size] pixels wide, scaled by distance when
     * `USE_SIZEATTENUATION` is defined and the projection is perspective.
     */
    val pointsVertex = """
        |$commonVertex
        |
        |uniform float size;
        |uniform float scale;
        |
        |$colorParsVertex
        |$morphtargetParsVertex
        |$fogParsVertex
        |
        |void main() {
        |    $colorVertex
        |    $beginMorphtargetVertex
        |    $projectVertex
        |
        |    gl_PointSize = size;
        |
        |    #ifdef USE_SIZEATTENUATION
        |    bool isPerspective = isPerspectiveMatrix( projectionMatrix );
        |    if ( isPerspective ) gl_PointSize *= ( scale / - mvPosition.z );
        |    #endif
        |
        |    $fogVertex
        |}
    """.trimMargin()

    /** Fragment shader of `PointsMaterial`. A map is sampled across each point through `gl_PointCoord`. */
    val pointsFragment = """
        |$commonFragment
        |
        |#ifdef USE_MAP
        |uniform mat3 uvTransform;
        |uniform sampler2D map;
        |#endif
        |
        |$colorParsFragment
        |$fogParsFragment
        |#ifdef USE_ALPHATEST
        |uniform float alphaTest;
        |#endif
        |
        |void main() {
        |    vec4 diffuseColor = vec4( diffuse, opacity );
        |
        |    #ifdef USE_MAP
        |    vec2 uv = ( uvTransform * vec3( gl_PointCoord.x, 1.0 - gl_PointCoord.y, 1 ) ).xy;
        |    diffuseColor *= texture( map, uv );
        |    #endif
        |
        |    $colorFragment
        |
        |    vec3 outgoingLight = diffuseColor.rgb;
        |    $outputFragment
        |}
    """.trimMargin()

    /**
     * Vertex shader of `MeshPhongMaterial`. Normals are transformed by `normalMatrix` and flipped for
     * back-side materials; flat shading derives them in the fragment shader instead. Morph targets move
     * positions only.
     */
    val phongVertex = """
        |$commonVertex
        |
        |uniform mat3 normalMatrix;
        |in vec3 normal;
        |out vec3 vViewPosition;
        |
        |#ifndef FLAT_SHADED
        |out vec3 vNormal;
        |#endif
        |
        |#ifdef USE_MAP
        |in vec2 uv;
        |uniform mat3 mapTransform;
        |out vec2 vMapUv;
        |#endif
        |
        |$colorParsVertex
        |$instancingParsVertex
        |$morphtargetParsVertex
        |$fogParsVertex
        |
        |void main() {
        |    #ifdef USE_MAP
        |    vMapUv = ( mapTransform * vec3( uv, 1 ) ).xy;
        |    #endif
        |
        |    $colorVertex
        |
        |    $defaultnormalVertex
        |    #ifndef FLAT_SHADED
        |    vNormal = normalize( transformedNormal );
        |    #endif
        |
        |    $beginMorphtargetVertex
        |    $projectVertex
        |    vViewPosition = - mvPosition.xyz;
        |    $fogVertex
        |}
    """.trimMargin()

    /**
     * Direct and indirect lighting of the Phong and Lambert models, as the Three.js `lights_fragment_*`
     * chunks with `RE_Direct_BlinnPhong` or `RE_Direct_Lambert`. Specular terms are added when
     * `USE_SPECULAR` is defined. Expects `diffuseColor`, `normal`, `vViewPosition`, and, with specular,
     * `specular`, `shininess` and `specularStrength`.
     */
    private val lightsFragment = """
        vec3 geometryPosition = - vViewPosition;
        vec3 geometryNormal = normal;
        vec3 geometryViewDir = ( isOrthographic ) ? vec3( 0, 0, 1 ) : normalize( vViewPosition );

        vec3 directDiffuse = vec3( 0.0 );
        vec3 directSpecular = vec3( 0.0 );
        vec3 indirectDiffuse = vec3( 0.0 );

        #if NUM_POINT_LIGHTS > 0
        for ( int i = 0; i < NUM_POINT_LIGHTS; i ++ ) {
            vec3 lVector = pointLights[ i ].position - geometryPosition;
            vec3 lightDirection = normalize( lVector );
            vec3 lightColor = pointLights[ i ].color * getDistanceAttenuation( length( lVector ), pointLights[ i ].distance, pointLights[ i ].decay );
            float dotNL = saturate( dot( geometryNormal, lightDirection ) );
            vec3 irradiance = dotNL * lightColor;
            directDiffuse += irradiance * BRDF_Lambert( diffuseColor.rgb );
            #ifdef USE_SPECULAR
            directSpecular += irradiance * BRDF_BlinnPhong( lightDirection, geometryViewDir, geometryNormal, specular, shininess ) * specularStrength;
            #endif
        }
        #endif

        #if NUM_DIR_LIGHTS > 0
        for ( int i = 0; i < NUM_DIR_LIGHTS; i ++ ) {
            vec3 lightDirection = directionalLights[ i ].direction;
            float dotNL = saturate( dot( geometryNormal, lightDirection ) );
            vec3 irradiance = dotNL * directionalLights[ i ].color;
            directDiffuse += irradiance * BRDF_Lambert( diffuseColor.rgb );
            #ifdef USE_SPECULAR
            directSpecular += irradiance * BRDF_BlinnPhong( lightDirection, geometryViewDir, geometryNormal, specular, shininess ) * specularStrength;
            #endif
        }
        #endif

        vec3 irradiance = ambientLightColor;
        #if NUM_HEMI_LIGHTS > 0
        for ( int i = 0; i < NUM_HEMI_LIGHTS; i++ ) {
            irradiance += getHemisphereLightIrradiance( hemisphereLights[ i ], geometryNormal );
        }
        #endif
        indirectDiffuse += irradiance * BRDF_Lambert( diffuseColor.rgb );
    """.trimIndent()

    /** Normal in view space, from flat-shading derivatives or the interpolated vertex normal. */
    private val normalFragmentBegin = """
        float faceDirection = gl_FrontFacing ? 1.0 : - 1.0;
        #ifdef FLAT_SHADED
        vec3 fdx = dFdx( vViewPosition );
        vec3 fdy = dFdy( vViewPosition );
        vec3 normal = normalize( cross( fdx, fdy ) );
        #else
        vec3 normal = normalize( vNormal );
        #ifdef DOUBLE_SIDED
        normal *= faceDirection;
        #endif
        #endif
    """.trimIndent()

    /**
     * Fragment shader of `MeshPhongMaterial` and, without `USE_SPECULAR`, of `MeshLambertMaterial`:
     * Lambertian diffuse lighting from point, directional, ambient and hemisphere lights, plus Blinn-Phong
     * highlights for Phong. The renderer defines `USE_SPECULAR` for Phong.
     */
    val phongFragment = """
        |$commonFragment
        |
        |uniform vec3 emissive;
        |#ifdef USE_SPECULAR
        |uniform vec3 specular;
        |uniform float shininess;
        |#endif
        |
        |in vec3 vViewPosition;
        |#ifndef FLAT_SHADED
        |in vec3 vNormal;
        |#endif
        |
        |#ifdef USE_MAP
        |uniform sampler2D map;
        |in vec2 vMapUv;
        |#endif
        |
        |$colorParsFragment
        |$fogParsFragment
        |#ifdef USE_ALPHATEST
        |uniform float alphaTest;
        |#endif
        |$bsdfs
        |$lightsParsBegin
        |
        |void main() {
        |    vec4 diffuseColor = vec4( diffuse, opacity );
        |    vec3 totalEmissiveRadiance = emissive;
        |
        |    #ifdef USE_MAP
        |    diffuseColor *= texture( map, vMapUv );
        |    #endif
        |
        |    $colorFragment
        |
        |    float specularStrength = 1.0;
        |
        |    $normalFragmentBegin
        |    $lightsFragment
        |
        |    vec3 outgoingLight = directDiffuse + indirectDiffuse + directSpecular + totalEmissiveRadiance;
        |    $outputFragment
        |}
    """.trimMargin()

    /** Fragment shader of `MeshNormalMaterial`: view-space normals packed into RGB, without color encoding. */
    val normalFragment = """
        |precision highp float;
        |
        |uniform float opacity;
        |in vec3 vViewPosition;
        |#ifndef FLAT_SHADED
        |in vec3 vNormal;
        |#endif
        |out vec4 pc_fragColor;
        |
        |void main() {
        |    vec4 diffuseColor = vec4( 0.0, 0.0, 0.0, opacity );
        |    $normalFragmentBegin
        |    pc_fragColor = vec4( normalize( normal ) * 0.5 + 0.5, diffuseColor.a );
        |    #ifdef OPAQUE
        |    pc_fragColor.a = 1.0;
        |    #endif
        |}
    """.trimMargin()

    /**
     * Vertex shader of `SpriteMaterial`. The quad faces the camera at the object's origin, scaled by the
     * object's X and Y scale, offset by `center` and turned by `rotation`. Without
     * `USE_SIZEATTENUATION` its size stays constant on screen under a perspective camera.
     */
    val spriteVertex = """
        |$commonVertex
        |
        |uniform mat4 modelMatrix;
        |uniform float rotation;
        |uniform vec2 center;
        |
        |#ifdef USE_MAP
        |in vec2 uv;
        |uniform mat3 mapTransform;
        |out vec2 vMapUv;
        |#endif
        |
        |$fogParsVertex
        |
        |void main() {
        |    #ifdef USE_MAP
        |    vMapUv = ( mapTransform * vec3( uv, 1 ) ).xy;
        |    #endif
        |
        |    vec4 mvPosition = modelViewMatrix[ 3 ];
        |    vec2 scale = vec2( length( modelMatrix[ 0 ].xyz ), length( modelMatrix[ 1 ].xyz ) );
        |
        |    #ifndef USE_SIZEATTENUATION
        |    bool isPerspective = isPerspectiveMatrix( projectionMatrix );
        |    if ( isPerspective ) scale *= - mvPosition.z;
        |    #endif
        |
        |    vec2 alignedPosition = ( position.xy - ( center - vec2( 0.5 ) ) ) * scale;
        |    vec2 rotatedPosition;
        |    rotatedPosition.x = cos( rotation ) * alignedPosition.x - sin( rotation ) * alignedPosition.y;
        |    rotatedPosition.y = sin( rotation ) * alignedPosition.x + cos( rotation ) * alignedPosition.y;
        |    mvPosition.xy += rotatedPosition;
        |
        |    gl_Position = projectionMatrix * mvPosition;
        |    $fogVertex
        |}
    """.trimMargin()

    /** Fragment shader of `SpriteMaterial`, which matches the unlit basic fragment shader. */
    val spriteFragment get() = basicFragment
}
