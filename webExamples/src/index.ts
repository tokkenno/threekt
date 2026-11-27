type Example = {
    render(seconds: number): void;
    dispose(): void;
};

type ExampleModule = {
    createExample(canvas: HTMLCanvasElement): Example | Promise<Example>;
};

const examples = new Map<string, () => Promise<ExampleModule>>([
    ['webgl_geometry_cube', () => import('./examples/webgl_geometry_cube')],
    ['webgl_materials_texture_canvas', () => import('./examples/webgl_materials_texture_canvas')],
    ['webgl_materials_texture_rotation', () => import('./examples/webgl_materials_texture_rotation')],
    ['webgl_buffergeometry_rawshader', () => import('./examples/webgl_buffergeometry_rawshader')],
    ['webgl_buffergeometry_attributes_none', () => import('./examples/webgl_buffergeometry_attributes_none')],
    ['webgl_buffergeometry_lines', () => import('./examples/webgl_buffergeometry_lines')],
    ['webgl_buffergeometry_lines_indexed', () => import('./examples/webgl_buffergeometry_lines_indexed')],
    ['webgl_buffergeometry_points', () => import('./examples/webgl_buffergeometry_points')],
    ['webgl_buffergeometry_drawrange', () => import('./examples/webgl_buffergeometry_drawrange')],
    ['webgl_buffergeometry', () => import('./examples/webgl_buffergeometry')],
    ['webgl_buffergeometry_uint', () => import('./examples/webgl_buffergeometry_uint')],
    ['webgl_buffergeometry_points_interleaved', () => import('./examples/webgl_buffergeometry_points_interleaved')],
    ['webgl_buffergeometry_selective_draw', () => import('./examples/webgl_buffergeometry_selective_draw')],
    ['webgl_buffergeometry_custom_attributes_particles', () => import('./examples/webgl_buffergeometry_custom_attributes_particles')],
    ['webgl_buffergeometry_indexed', () => import('./examples/webgl_buffergeometry_indexed')],
    ['webgl_points_waves', () => import('./examples/webgl_points_waves')],
    ['webgl_shader', () => import('./examples/webgl_shader')],
    ['webgl_custom_attributes', () => import('./examples/webgl_custom_attributes')],
    ['webgl_custom_attributes_lines', () => import('./examples/webgl_custom_attributes_lines')],
    ['webgl_shader_lava', () => import('./examples/webgl_shader_lava')],
    ['webgl_shaders_sky', () => import('./examples/webgl_shaders_sky')],
    ['webgl_custom_attributes_points', () => import('./examples/webgl_custom_attributes_points')],
    ['webgl_points_billboards', () => import('./examples/webgl_points_billboards')],
    ['webgl_points_sprites', () => import('./examples/webgl_points_sprites')],
    ['webgl_materials_texture_anisotropy', () => import('./examples/webgl_materials_texture_anisotropy')],
    ['webgl_materials_texture_filters', () => import('./examples/webgl_materials_texture_filters')],
    ['webgl_materials_texture_manualmipmap', () => import('./examples/webgl_materials_texture_manualmipmap')],
    ['webgl_buffergeometry_attributes_integer', () => import('./examples/webgl_buffergeometry_attributes_integer')],
    ['webgl_lines_colors', () => import('./examples/webgl_lines_colors')],
    ['webgl_lines_dashed', () => import('./examples/webgl_lines_dashed')],
    ['webgl_custom_attributes_points2', () => import('./examples/webgl_custom_attributes_points2')],
    ['webgl_custom_attributes_points3', () => import('./examples/webgl_custom_attributes_points3')],
    ['webgl_materials_blending', () => import('./examples/webgl_materials_blending')],
    ['webgl_materials_wireframe', () => import('./examples/webgl_materials_wireframe')],
    ['webgl_materials_toon', () => import('./examples/webgl_materials_toon')],
    ['webgl_panorama_equirectangular', () => import('./examples/webgl_panorama_equirectangular')],
    ['webgl_loader_obj', () => import('./examples/webgl_loader_obj')],
    ['webgl_loader_xyz', () => import('./examples/webgl_loader_xyz')],
    ['webgl_panorama_cube', () => import('./examples/webgl_panorama_cube')],
    ['webgl_loader_imagebitmap', () => import('./examples/webgl_loader_imagebitmap')],
    ['webgl_video_panorama_equirectangular', () => import('./examples/webgl_video_panorama_equirectangular')],
    ['webgl_materials_video_webcam', () => import('./examples/webgl_materials_video_webcam')],
    ['webgl_materials_blending_custom', () => import('./examples/webgl_materials_blending_custom')],
    ['webgl_geometry_colors', () => import('./examples/webgl_geometry_colors')],
    ['webgl_multiple_elements_text', () => import('./examples/webgl_multiple_elements_text')],
    ['webgl_loader_pcd', () => import('./examples/webgl_loader_pcd')],
    ['webgl_camera', () => import('./examples/webgl_camera')],
    ['threekt_calibrated_camera', () => import('./examples/threekt_calibrated_camera')],
    ['webgl_multiple_views', () => import('./examples/webgl_multiple_views')],
    ['webgl_morphtargets', () => import('./examples/webgl_morphtargets')],
    ['webgl_video_kinect', () => import('./examples/webgl_video_kinect')],
    ['webgl_loader_texture_tga', () => import('./examples/webgl_loader_texture_tga')],
    ['webgl_loader_stl', () => import('./examples/webgl_loader_stl')],
    ['misc_exporter_stl', () => import('./examples/misc_exporter_stl')],
    ['misc_exporter_ply', () => import('./examples/misc_exporter_ply')],
    ['misc_exporter_obj', () => import('./examples/misc_exporter_obj')],
    ['webgl_camera_array', () => import('./examples/webgl_camera_array')],
    ['webgl_interactive_cubes', () => import('./examples/webgl_interactive_cubes')],
    ['webgl_interactive_cubes_ortho', () => import('./examples/webgl_interactive_cubes_ortho')],
    ['webgl_interactive_lines', () => import('./examples/webgl_interactive_lines')],
    ['webgl_interactive_buffergeometry', () => import('./examples/webgl_interactive_buffergeometry')],
    ['webgl_interactive_points', () => import('./examples/webgl_interactive_points')],
    ['webgl_interactive_raycasting_points', () => import('./examples/webgl_interactive_raycasting_points')],
    ['webgl_interactive_voxelpainter', () => import('./examples/webgl_interactive_voxelpainter')],
    ['webgl_raycaster_texture', () => import('./examples/webgl_raycaster_texture')],
    ['webgl_math_orientation_transform', () => import('./examples/webgl_math_orientation_transform')],
    ['webgl_materials_video', () => import('./examples/webgl_materials_video')],
    ['webgl_geometry_terrain', () => import('./examples/webgl_geometry_terrain')],
    ['webgl_lod', () => import('./examples/webgl_lod')],
    ['webgl_multiple_elements', () => import('./examples/webgl_multiple_elements')],
    ['webgl_multiple_scenes_comparison', () => import('./examples/webgl_multiple_scenes_comparison')],
    ['misc_controls_orbit', () => import('./examples/misc_controls_orbit')],
    ['misc_controls_map', () => import('./examples/misc_controls_map')],
    ['misc_controls_drag', () => import('./examples/misc_controls_drag')],
    ['webgl_geometry_terrain_raycast', () => import('./examples/webgl_geometry_terrain_raycast')],
    ['webgl_geometry_spline_editor', () => import('./examples/webgl_geometry_spline_editor')],
    ['webgl_buffergeometry_instancing', () => import('./examples/webgl_buffergeometry_instancing')],
    ['webgl_buffergeometry_instancing_billboards', () => import('./examples/webgl_buffergeometry_instancing_billboards')],
    ['webgl_buffergeometry_instancing_interleaved', () => import('./examples/webgl_buffergeometry_instancing_interleaved')],
    ['webgl_instancing_performance', () => import('./examples/webgl_instancing_performance')],
    ['webgl_instancing_dynamic', () => import('./examples/webgl_instancing_dynamic')],
    ['webgl_instancing_raycast', () => import('./examples/webgl_instancing_raycast')],
    ['misc_controls_trackball', () => import('./examples/misc_controls_trackball')],
    ['webgl_geometries', () => import('./examples/webgl_geometries')],
    ['webgl_sprites', () => import('./examples/webgl_sprites')],
    ['webgl_raycaster_sprite', () => import('./examples/webgl_raycaster_sprite')],
    ['webgl_math_obb', () => import('./examples/webgl_math_obb')],
    ['webgl_geometry_shapes', () => import('./examples/webgl_geometry_shapes')],
    ['webgl_geometry_extrude_splines', () => import('./examples/webgl_geometry_extrude_splines')],
    ['webgl_geometry_text', () => import('./examples/webgl_geometry_text')],
    ['webgl_geometry_text_shapes', () => import('./examples/webgl_geometry_text_shapes')],
    ['webgl_geometry_extrude_shapes', () => import('./examples/webgl_geometry_extrude_shapes')],
    ['webgl_geometry_nurbs', () => import('./examples/webgl_geometry_nurbs')],
    ['webgl_geometry_minecraft', () => import('./examples/webgl_geometry_minecraft')],
    ['webgl_geometry_colors_lookuptable', () => import('./examples/webgl_geometry_colors_lookuptable')],
    ['webgl_instancing_scatter', () => import('./examples/webgl_instancing_scatter')],
    ['webgl_morphtargets_sphere', () => import('./examples/webgl_morphtargets_sphere')],
    ['webgl_effects_ascii', () => import('./examples/webgl_effects_ascii')],
    ['webgl_effects_stereo', () => import('./examples/webgl_effects_stereo')],
    ['webgl_effects_anaglyph', () => import('./examples/webgl_effects_anaglyph')],
    ['webgl_effects_parallaxbarrier', () => import('./examples/webgl_effects_parallaxbarrier')],
    ['webgl_loader_gcode', () => import('./examples/webgl_loader_gcode')],
    ['webgl_loader_pdb', () => import('./examples/webgl_loader_pdb')],
    ['misc_raycaster_helper', () => import('./examples/misc_raycaster_helper')],
    ['webgl_geometry_convex', () => import('./examples/webgl_geometry_convex')],
    ['webgl_clipping', () => import('./examples/webgl_clipping')],
    ['webgl_clipping_advanced', () => import('./examples/webgl_clipping_advanced')],
    ['webgl_clipping_intersection', () => import('./examples/webgl_clipping_intersection')],
    ['webgl_helpers', () => import('./examples/webgl_helpers')],
    ['misc_uv_tests', () => import('./examples/misc_uv_tests')],
    ['webgl_depth_texture', () => import('./examples/webgl_depth_texture')],
    ['webgl_framebuffer_texture', () => import('./examples/webgl_framebuffer_texture')],
    ['webgl_materials_texture_partialupdate', () => import('./examples/webgl_materials_texture_partialupdate')],
    ['webgl_postprocessing_procedural', () => import('./examples/webgl_postprocessing_procedural')],
    ['webgl_postprocessing_afterimage', () => import('./examples/webgl_postprocessing_afterimage')],
    ['webgl_postprocessing_sobel', () => import('./examples/webgl_postprocessing_sobel')],
    ['webgl_postprocessing', () => import('./examples/webgl_postprocessing')],
    ['webgl_postprocessing_backgrounds', () => import('./examples/webgl_postprocessing_backgrounds')],
    ['webgl_postprocessing_transition', () => import('./examples/webgl_postprocessing_transition')],
    ['webgl_postprocessing_fxaa', () => import('./examples/webgl_postprocessing_fxaa')],
    ['webgl_postprocessing_glitch', () => import('./examples/webgl_postprocessing_glitch')],
    ['webgl_postprocessing_rgb_halftone', () => import('./examples/webgl_postprocessing_rgb_halftone')],
    ['webgl_postprocessing_pixel', () => import('./examples/webgl_postprocessing_pixel')],
    ['webgl_rtt', () => import('./examples/webgl_rtt')],
    ['webgl_read_float_buffer', () => import('./examples/webgl_read_float_buffer')],
    ['webgl_test_memory', () => import('./examples/webgl_test_memory')],
    ['webgl_test_memory2', () => import('./examples/webgl_test_memory2')],
    ['misc_controls_pointerlock', () => import('./examples/misc_controls_pointerlock')],
    ['misc_controls_fly', () => import('./examples/misc_controls_fly')],
    ['misc_boxselection', () => import('./examples/misc_boxselection')],
    ['webgl_loader_ply', () => import('./examples/webgl_loader_ply')],
    ['webgl_loader_amf', () => import('./examples/webgl_loader_amf')],
    ['webgl_loader_vox', () => import('./examples/webgl_loader_vox')],
    ['webgl_loader_mdd', () => import('./examples/webgl_loader_mdd')],
    ['misc_animation_keys', () => import('./examples/misc_animation_keys')],
    ['misc_animation_groups', () => import('./examples/misc_animation_groups')],
    ['css2d_label', () => import('./examples/css2d_label')],
    ['css3d_mixed', () => import('./examples/css3d_mixed')],
    ['css3d_molecules', () => import('./examples/css3d_molecules')],
    ['css3d_orthographic', () => import('./examples/css3d_orthographic')],
    ['css3d_periodictable', () => import('./examples/css3d_periodictable')],
    ['css3d_sandbox', () => import('./examples/css3d_sandbox')],
    ['css3d_sprites', () => import('./examples/css3d_sprites')],
    ['css3d_youtube', () => import('./examples/css3d_youtube')],
    ['svg_lines', () => import('./examples/svg_lines')],
    ['svg_sandbox', () => import('./examples/svg_sandbox')],
    ['webaudio_orientation', () => import('./examples/webaudio_orientation')],
    ['webaudio_sandbox', () => import('./examples/webaudio_sandbox')],
    ['webaudio_timing', () => import('./examples/webaudio_timing')],
    ['webaudio_visualizer', () => import('./examples/webaudio_visualizer')],
    ['webgl_materials_cubemap', () => import('./examples/webgl_materials_cubemap')],
    ['webgl_materials_cubemap_refraction', () => import('./examples/webgl_materials_cubemap_refraction')],
    ['webgl_materials_cubemap_mipmaps', () => import('./examples/webgl_materials_cubemap_mipmaps')],
    ['webgl_postprocessing_3dlut', () => import('./examples/webgl_postprocessing_3dlut')],
    ['webgl_postprocessing_advanced', () => import('./examples/webgl_postprocessing_advanced')],
    ['webgl_postprocessing_dof', () => import('./examples/webgl_postprocessing_dof')],
    ['webgl_postprocessing_dof2', () => import('./examples/webgl_postprocessing_dof2')],
    ['webgl_postprocessing_masking', () => import('./examples/webgl_postprocessing_masking')],
    ['webgl_postprocessing_outline', () => import('./examples/webgl_postprocessing_outline')],
    ['webgl_postprocessing_ssaa', () => import('./examples/webgl_postprocessing_ssaa')],
    ['webgl_postprocessing_taa', () => import('./examples/webgl_postprocessing_taa')],
    ['webgl_materials_bumpmap', () => import('./examples/webgl_materials_bumpmap')],
    ['webgl_materials_displacementmap', () => import('./examples/webgl_materials_displacementmap')],
    ['webgl_materials_channels', () => import('./examples/webgl_materials_channels')],
    ['webgl_materials_normalmap', () => import('./examples/webgl_materials_normalmap')],
    ['webgl_materials_normalmap_object_space', () => import('./examples/webgl_materials_normalmap_object_space')],
    ['webgl_materials_matcap', () => import('./examples/webgl_materials_matcap')],
    ['webgl_materials_alphahash', () => import('./examples/webgl_materials_alphahash')],
    ['webgl_materials_modified', () => import('./examples/webgl_materials_modified')],
    ['webgl_postprocessing_unreal_bloom', () => import('./examples/webgl_postprocessing_unreal_bloom')],
    ['webgl_postprocessing_unreal_bloom_selective', () => import('./examples/webgl_postprocessing_unreal_bloom_selective')],
    ['webgl_mirror', () => import('./examples/webgl_mirror')],
    ['webgl_portal', () => import('./examples/webgl_portal')],
    ['webgl_geometry_teapot', () => import('./examples/webgl_geometry_teapot')],
    ['webgl_modifier_curve', () => import('./examples/webgl_modifier_curve')],
    ['webgl_modifier_curve_instanced', () => import('./examples/webgl_modifier_curve_instanced')],
]);

const gallery = document.querySelector<HTMLElement>('#gallery')!;
const menuToggle = document.querySelector<HTMLButtonElement>('#menu-toggle')!;
const compactLayout = window.matchMedia('(max-width: 44rem)');
const menuPreference = () => sessionStorage.getItem(compactLayout.matches ? 'compact-menu' : 'desktop-menu');

function setMenuExpanded(expanded: boolean) {
    gallery.hidden = !expanded;
    menuToggle.setAttribute('aria-expanded', String(expanded));
    menuToggle.setAttribute('aria-label', expanded ? 'Collapse examples menu' : 'Expand examples menu');
    if (expanded) gallery.querySelector('[aria-current="page"]')?.scrollIntoView({ block: 'nearest' });
}

function restoreMenu() {
    const saved = menuPreference();
    setMenuExpanded(saved === null ? !compactLayout.matches : saved === 'open');
}

function saveMenu(expanded: boolean) {
    sessionStorage.setItem(compactLayout.matches ? 'compact-menu' : 'desktop-menu', expanded ? 'open' : 'closed');
    setMenuExpanded(expanded);
}

menuToggle.addEventListener('click', () => saveMenu(gallery.hidden === true));
compactLayout.addEventListener('change', restoreMenu);
gallery.addEventListener('click', (event) => {
    if (compactLayout.matches && event.button === 0 && !event.ctrlKey && !event.metaKey &&
        !event.shiftKey && !event.altKey && event.target instanceof Element && event.target.closest('a')) {
        saveMenu(false);
    }
});
document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !gallery.hidden &&
        (gallery.contains(document.activeElement) || document.activeElement === menuToggle)) {
        saveMenu(false);
        menuToggle.focus();
    }
});

restoreMenu();

if (new URLSearchParams(window.location.search).get('embed') === '1' && window.parent !== window) {
    document.body.classList.add('gallery-embed');
}
const exampleId = new URLSearchParams(window.location.search).get('example');
const load = exampleId !== null ? examples.get(exampleId) : undefined;
const selected = Array.from(gallery.querySelectorAll<HTMLAnchorElement>('a[data-description]'))
    .find(link => new URL(link.href).searchParams.get('example') === exampleId);

if (load && selected) {
    const canvas = document.querySelector<HTMLCanvasElement>('#example-canvas')!;
    const errorMessage = document.querySelector<HTMLParagraphElement>('#example-error')!;
    const title = selected.textContent.trim();
    document.querySelector<HTMLElement>('#example-title')!.textContent = title;
    document.querySelector<HTMLElement>('#example-description')!.textContent = selected.dataset.description ?? '';
    document.querySelector<HTMLElement>('#welcome')!.hidden = true;
    document.querySelector<HTMLElement>('#example-view')!.hidden = false;
    document.title = `${title} · Threekt`;
    selected.setAttribute('aria-current', 'page');
    if (!gallery.hidden) selected.scrollIntoView({ block: 'nearest' });

    const showError = (error: unknown) => {
        errorMessage.textContent = `Could not run the example. ${String(error)}`;
        errorMessage.hidden = false;
    };

    let stop = () => {};
    let closed = false;
    window.addEventListener('pagehide', (event) => {
        if (!event.persisted) {
            closed = true;
            stop();
        }
    });

    load().then(async ({ createExample }) => {
        const example = await createExample(canvas);
        if (closed) {
            example.dispose();
            return;
        }

        let frame: number | undefined;
        const startedAt = performance.now();
        stop = () => {
            if (frame !== undefined) cancelAnimationFrame(frame);
            example.dispose();
            stop = () => {};
        };

        const draw = (now: number) => {
            try {
                example.render(Math.max(0, now - startedAt) / 1000);
                frame = requestAnimationFrame(draw);
            } catch (error) {
                stop();
                showError(error);
            }
        };
        frame = requestAnimationFrame(draw);
    }).catch(showError);
}
