package com.aitorgf.threekt.renderers.webgl

@ExperimentalJsExport
@JsExport
class WebGLLights(
    private val extensions: WebGLExtensions,
    private val capabilities: WebGLStateCapabilities
) {
    val cache = UniformsCache()
    val shadowCache = ShadowUniformsCache()

    const state = {

        version: 0,

        hash: {
            directionalLength: - 1,
            pointLength: - 1,
            spotLength: - 1,
            rectAreaLength: - 1,
            hemiLength: - 1,

            numDirectionalShadows: - 1,
            numPointShadows: - 1,
            numSpotShadows: - 1
        },

        ambient: [ 0, 0, 0 ],
        probe: [],
        directional: [],
        directionalShadow: [],
        directionalShadowMap: [],
        directionalShadowMatrix: [],
        spot: [],
        spotShadow: [],
        spotShadowMap: [],
        spotShadowMatrix: [],
        rectArea: [],
        rectAreaLTC1: null,
        rectAreaLTC2: null,
        point: [],
        pointShadow: [],
        pointShadowMap: [],
        pointShadowMatrix: [],
        hemi: []

    };

    for ( let i = 0; i < 9; i ++ ) state.probe.push( new Vector3() );

    const vector3 = new Vector3();
    const matrix4 = new Matrix4();
    const matrix42 = new Matrix4();

    function setup( lights ) {

        let r = 0, g = 0, b = 0;

        for ( let i = 0; i < 9; i ++ ) state.probe[ i ].set( 0, 0, 0 );

        let directionalLength = 0;
        let pointLength = 0;
        let spotLength = 0;
        let rectAreaLength = 0;
        let hemiLength = 0;

        let numDirectionalShadows = 0;
        let numPointShadows = 0;
        let numSpotShadows = 0;

        com.aitorgf.threekt.lights.sort( shadowCastingLightsFirst );

        for ( let i = 0, l = lights.length; i < l; i ++ ) {

            const light = lights[ i ];

            const color = light.color;
            const intensity = light.intensity;
            const distance = light.distance;

            const shadowMap = ( light.shadow && light.shadow.map ) ? light.shadow.map.texture : null;

            if ( light.isAmbientLight ) {

                r += color.r * intensity;
                g += color.g * intensity;
                b += color.b * intensity;

            } else if ( light.isLightProbe ) {

                for ( let j = 0; j < 9; j ++ ) {

                    state.probe[ j ].addScaledVector( light.sh.coefficients[ j ], intensity );

                }

            } else if ( light.isDirectionalLight ) {

                const uniforms = cache.get( light );

                uniforms.color.copy( light.color ).multiplyScalar( light.intensity );

                if ( light.castShadow ) {

                    const shadow = light.shadow;

                    const shadowUniforms = shadowCache.get( light );

                    shadowUniforms.shadowBias = shadow.bias;
                    shadowUniforms.shadowNormalBias = shadow.normalBias;
                    shadowUniforms.shadowRadius = shadow.radius;
                    shadowUniforms.shadowMapSize = shadow.mapSize;

                    state.directionalShadow[ directionalLength ] = shadowUniforms;
                    state.directionalShadowMap[ directionalLength ] = shadowMap;
                    state.directionalShadowMatrix[ directionalLength ] = light.shadow.matrix;

                    numDirectionalShadows ++;

                }

                state.directional[ directionalLength ] = uniforms;

                directionalLength ++;

            } else if ( light.isSpotLight ) {

                const uniforms = cache.get( light );

                uniforms.position.setFromMatrixPosition( light.matrixWorld );

                uniforms.color.copy( color ).multiplyScalar( intensity );
                uniforms.distance = distance;

                uniforms.coneCos = Math.cos( light.angle );
                uniforms.penumbraCos = Math.cos( light.angle * ( 1 - light.penumbra ) );
                uniforms.decay = light.decay;

                if ( light.castShadow ) {

                    const shadow = light.shadow;

                    const shadowUniforms = shadowCache.get( light );

                    shadowUniforms.shadowBias = shadow.bias;
                    shadowUniforms.shadowNormalBias = shadow.normalBias;
                    shadowUniforms.shadowRadius = shadow.radius;
                    shadowUniforms.shadowMapSize = shadow.mapSize;

                    state.spotShadow[ spotLength ] = shadowUniforms;
                    state.spotShadowMap[ spotLength ] = shadowMap;
                    state.spotShadowMatrix[ spotLength ] = light.shadow.matrix;

                    numSpotShadows ++;

                }

                state.spot[ spotLength ] = uniforms;

                spotLength ++;

            } else if ( light.isRectAreaLight ) {

                const uniforms = cache.get( light );

                // (a) intensity is the total visible light emitted
                //uniforms.color.copy( color ).multiplyScalar( intensity / ( light.width * light.height * Math.PI ) );

                // (b) intensity is the brightness of the light
                uniforms.color.copy( color ).multiplyScalar( intensity );

                uniforms.halfWidth.set( light.width * 0.5, 0.0, 0.0 );
                uniforms.halfHeight.set( 0.0, light.height * 0.5, 0.0 );

                state.rectArea[ rectAreaLength ] = uniforms;

                rectAreaLength ++;

            } else if ( light.isPointLight ) {

                const uniforms = cache.get( light );

                uniforms.color.copy( light.color ).multiplyScalar( light.intensity );
                uniforms.distance = light.distance;
                uniforms.decay = light.decay;

                if ( light.castShadow ) {

                    const shadow = light.shadow;

                    const shadowUniforms = shadowCache.get( light );

                    shadowUniforms.shadowBias = shadow.bias;
                    shadowUniforms.shadowNormalBias = shadow.normalBias;
                    shadowUniforms.shadowRadius = shadow.radius;
                    shadowUniforms.shadowMapSize = shadow.mapSize;
                    shadowUniforms.shadowCameraNear = shadow.camera.near;
                    shadowUniforms.shadowCameraFar = shadow.camera.far;

                    state.pointShadow[ pointLength ] = shadowUniforms;
                    state.pointShadowMap[ pointLength ] = shadowMap;
                    state.pointShadowMatrix[ pointLength ] = light.shadow.matrix;

                    numPointShadows ++;

                }

                state.point[ pointLength ] = uniforms;

                pointLength ++;

            } else if ( light.isHemisphereLight ) {

                const uniforms = cache.get( light );

                uniforms.skyColor.copy( light.color ).multiplyScalar( intensity );
                uniforms.groundColor.copy( light.groundColor ).multiplyScalar( intensity );

                state.hemi[ hemiLength ] = uniforms;

                hemiLength ++;

            }

        }

        if ( rectAreaLength > 0 ) {

            if ( capabilities.isWebGL2 ) {

                // WebGL 2

                state.rectAreaLTC1 = UniformsLib.LTC_FLOAT_1;
                state.rectAreaLTC2 = UniformsLib.LTC_FLOAT_2;

            } else {

                // WebGL 1

                if ( extensions.has( 'OES_texture_float_linear' ) === true ) {

                    state.rectAreaLTC1 = UniformsLib.LTC_FLOAT_1;
                    state.rectAreaLTC2 = UniformsLib.LTC_FLOAT_2;

                } else if ( extensions.has( 'OES_texture_half_float_linear' ) === true ) {

                    state.rectAreaLTC1 = UniformsLib.LTC_HALF_1;
                    state.rectAreaLTC2 = UniformsLib.LTC_HALF_2;

                } else {

                    console.error( 'THREE.WebGLRenderer: Unable to use RectAreaLight. Missing WebGL extensions.' );

                }

            }

        }

        state.ambient[ 0 ] = r;
        state.ambient[ 1 ] = g;
        state.ambient[ 2 ] = b;

        const hash = state.hash;

        if ( hash.directionalLength !== directionalLength ||
            hash.pointLength !== pointLength ||
            hash.spotLength !== spotLength ||
            hash.rectAreaLength !== rectAreaLength ||
            hash.hemiLength !== hemiLength ||
            hash.numDirectionalShadows !== numDirectionalShadows ||
            hash.numPointShadows !== numPointShadows ||
            hash.numSpotShadows !== numSpotShadows ) {

            state.directional.length = directionalLength;
            state.spot.length = spotLength;
            state.rectArea.length = rectAreaLength;
            state.point.length = pointLength;
            state.hemi.length = hemiLength;

            state.directionalShadow.length = numDirectionalShadows;
            state.directionalShadowMap.length = numDirectionalShadows;
            state.pointShadow.length = numPointShadows;
            state.pointShadowMap.length = numPointShadows;
            state.spotShadow.length = numSpotShadows;
            state.spotShadowMap.length = numSpotShadows;
            state.directionalShadowMatrix.length = numDirectionalShadows;
            state.pointShadowMatrix.length = numPointShadows;
            state.spotShadowMatrix.length = numSpotShadows;

            hash.directionalLength = directionalLength;
            hash.pointLength = pointLength;
            hash.spotLength = spotLength;
            hash.rectAreaLength = rectAreaLength;
            hash.hemiLength = hemiLength;

            hash.numDirectionalShadows = numDirectionalShadows;
            hash.numPointShadows = numPointShadows;
            hash.numSpotShadows = numSpotShadows;

            state.version = nextVersion ++;

        }

    }
