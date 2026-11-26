// Adapted from Three.js (MIT), examples/jsm/objects/Sky.js.
#ifdef REFLECTION
varying vec3 vWorldNormal;
#endif
uniform float exposure;
varying vec3 vWorldPosition;
varying vec3 vSunDirection;
varying vec3 vBetaR;
varying vec3 vBetaM;
varying float vSunE;

uniform float mieDirectionalG;
uniform float cloudScale;
uniform float cloudSpeed;
uniform float cloudCoverage;
uniform float cloudDensity;
uniform float cloudElevation;
uniform float showSunDisc;
uniform float time;

// gradient at a lattice corner; sinless hash so every GPU produces the same clouds
vec2 gradient( vec2 i ) {
    vec3 p = fract( i.xyx * vec3( 0.1031, 0.1030, 0.0973 ) );
    p += dot( p, p.yzx + 33.33 );
    return fract( ( p.xx + p.yz ) * p.zy ) * 2.0 - 1.0;
}

// 2D gradient noise: isotropic lobes like Perlin at value-noise cost
float noise( vec2 p ) {
    vec2 i = floor( p );
    vec2 f = fract( p );
    vec2 u = f * f * f * ( f * ( f * 6.0 - 15.0 ) + 10.0 ); // quintic fade
    float a = dot( gradient( i ), f );
    float b = dot( gradient( i + vec2( 1.0, 0.0 ) ), f - vec2( 1.0, 0.0 ) );
    float c = dot( gradient( i + vec2( 0.0, 1.0 ) ), f - vec2( 0.0, 1.0 ) );
    float d = dot( gradient( i + vec2( 1.0, 1.0 ) ), f - vec2( 1.0, 1.0 ) );
    return mix( mix( a, b, u.x ), mix( c, d, u.x ), u.y ) * 1.6; // ~[-1,1]
}

// fbm; per-octave drift makes clouds billow instead of scrolling as a rigid stamp
float fbm( vec2 p, float drift ) {
    float result = 0.0;
    float amplitude = 1.0;
    for ( int i = 0; i < 4; i ++ ) {
        result += amplitude * noise( p );
        amplitude *= 0.5;
        p = p * 2.0 + drift;
    }
    return result;
}

// constants for atmospheric scattering
const float pi = 3.141592653589793238462643383279502884197169;

const float n = 1.0003; // refractive index of air
const float N = 2.545E25; // number of molecules per unit volume for air at 288.15K and 1013mb (sea level -45 celsius)

// optical length at zenith for molecules
const float rayleighZenithLength = 8.4E3;
const float mieZenithLength = 1.25E3;
// 66 arc seconds -> degrees, and the cosine of that
const float sunAngularDiameterCos = 0.999956676946448443553574619906976478926848692873900859324;

// 3.0 / ( 16.0 * pi )
const float THREE_OVER_SIXTEENPI = 0.05968310365946075;
// 1.0 / ( 4.0 * pi )
const float ONE_OVER_FOURPI = 0.07957747154594767;

float rayleighPhase( float cosTheta ) {
    return THREE_OVER_SIXTEENPI * ( 1.0 + pow( cosTheta, 2.0 ) );
}

float hgPhase( float cosTheta, float g ) {
    float g2 = pow( g, 2.0 );
    float inverse = 1.0 / pow( 1.0 - 2.0 * g * cosTheta + g2, 1.5 );
    return ONE_OVER_FOURPI * ( ( 1.0 - g2 ) * inverse );
}

void main() {

    vec3 direction = normalize( vWorldPosition - cameraPosition );
            #ifdef REFLECTION
            direction = reflect(direction, normalize(vWorldNormal));
            #endif

    // optical length
    // cutoff angle at 90 to avoid singularity in next formula.
    float zenithAngle = acos( max( 0.0, direction.y ) );
    float inverse = 1.0 / ( cos( zenithAngle ) + 0.15 * pow( 93.885 - ( ( zenithAngle * 180.0 ) / pi ), -1.253 ) );
    float sR = rayleighZenithLength * inverse;
    float sM = mieZenithLength * inverse;

    // combined extinction factor
    vec3 Fex = exp( -( vBetaR * sR + vBetaM * sM ) );

    // in scattering
    float cosTheta = dot( direction, vSunDirection );

    float rPhase = rayleighPhase( cosTheta * 0.5 + 0.5 );
    vec3 betaRTheta = vBetaR * rPhase;

    float mPhase = hgPhase( cosTheta, mieDirectionalG );
    vec3 betaMTheta = vBetaM * mPhase;

    vec3 Lin = pow( vSunE * ( ( betaRTheta + betaMTheta ) / max( vBetaR + vBetaM, vec3( 1e-9 ) ) ) * ( 1.0 - Fex ), vec3( 1.5 ) );
    Lin *= mix( vec3( 1.0 ), pow( vSunE * ( ( betaRTheta + betaMTheta ) / max( vBetaR + vBetaM, vec3( 1e-9 ) ) ) * Fex, vec3( 1.0 / 2.0 ) ), clamp( pow( 1.0 - vSunDirection.y, 5.0 ), 0.0, 1.0 ) );

    // nightsky
    float theta = acos( direction.y ); // elevation --> y-axis, [-pi/2, pi/2]
    float phi = atan( direction.z, direction.x ); // azimuth --> x-axis [-pi/2, pi/2]
    vec2 uv = vec2( phi, theta ) / vec2( 2.0 * pi, pi ) + vec2( 0.5, 0.0 );
    vec3 L0 = vec3( 0.1 ) * Fex;

    // composition + solar disc
    float sundisc = clamp( ( cosTheta - sunAngularDiameterCos ) * 50000.0, 0.0, 1.0 ) * showSunDisc;
    vec3 sundiscColor = ( 760.0 * sundisc ) * min( vSunE * Fex, 80.0 );

    vec3 texColor = ( Lin + L0 ) * 0.04 + sundiscColor + vec3( 0.0, 0.0003, 0.00075 );

    // Clouds
    if ( direction.y > 0.0 && cloudCoverage > 0.0 ) {

        // Project to cloud plane (higher elevation = clouds appear lower/closer)
        float elevation = mix( 1.0, 0.1, cloudElevation );
        vec2 cloudUV = direction.xz / ( direction.y * elevation );
        cloudUV *= cloudScale;
        cloudUV += time * cloudSpeed;

        // Cloud density field
        float evolve = time * cloudSpeed * 300.0;
        float cloudNoise = clamp( fbm( cloudUV * 1000.0, evolve ) * 0.7 + 0.5, 0.0, 1.0 );

        // Large-scale coverage variation: clear gaps next to dense banks
        float region = noise( cloudUV * 300.0 ) * 0.37 + 0.5;
        float cov = clamp( cloudCoverage + ( region - 0.5 ) * 0.6, 0.0, 1.0 );

        // Carve clouds where noise rises above the coverage level
        float threshold = 1.0 - cov;
        float cloudMask = smoothstep( threshold, threshold + 0.3, cloudNoise );

        // Fade clouds near horizon (adjusted by elevation)
        float horizonFade = smoothstep( 0.0, 0.03 + 0.06 * cloudElevation, direction.y );
        cloudMask *= horizonFade;

        // Cloud lighting from the sky's own radiance
        float dayFactor = smoothstep( -0.08, 0.3, vSunDirection.y );
        vec3 sunColor = vSunE * Fex * 0.22 * 0.04; // 0.22 ~ albedo/pi, 0.04 = exposure; the aerial composite adds the eye-leg extinction
        vec3 skyAmbient = Lin * 0.04 + vec3( 0.0, 0.0003, 0.00075 );

        // Beer-powder self-shadow from the sampled density
        float depth = max( 0.0, cloudNoise - threshold );
        float beer = exp( depth * -4.0 );
        float powder = 1.0 - beer * beer; // beer*beer == exp(-8*depth)
        float shade = mix( 0.45, 1.0, clamp( beer * powder * 2.6, 0.0, 1.0 ) ); // 2.6 = 1/0.385, normalizes beer*powder peak to 1

        // Henyey-Greenstein forward lobe ( g = 0.7 ): silver lining on rims toward the sun
        float silver = clamp( 0.51 / pow( 1.49 - cosTheta * 1.4, 1.5 ), 0.0, 3.0 ); // 0.51=1-g^2, 1.49=1+g^2, 1.4=2g
        float edge = cloudMask * ( 1.0 - cloudMask ) * 4.0;

        vec3 cloudColor = skyAmbient + sunColor * shade;
        cloudColor += sunColor * silver * edge * 0.6;
        cloudColor *= max( dayFactor, 0.03 );

        // Cloud opacity via Beer's law: density sets how solid the clouds get
        float alpha = ( 1.0 - exp( depth * cloudDensity * -12.0 ) ) * horizonFade;

        // Occlude the sun disc/glow behind opaque cloud
        texColor -= L0 * 0.04 * alpha;

        // Composite through the atmosphere so distant clouds dissolve into haze
        vec3 cloudAerial = mix( texColor, cloudColor, Fex );
        texColor = mix( texColor, cloudAerial, alpha );

    }

    gl_FragColor = vec4( texColor, 1.0 );

    vec3 color = max(gl_FragColor.rgb * exposure, 0.0);
            gl_FragColor.rgb = encodeSrgb(clamp((color * (2.51 * color + 0.03)) /
                (color * (2.43 * color + 0.59) + 0.14), 0.0, 1.0));

}
