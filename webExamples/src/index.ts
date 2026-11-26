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
    ['webgl_instancing_raycast', () => import('./examples/webgl_instancing_raycast')],
    ['misc_controls_trackball', () => import('./examples/misc_controls_trackball')],
    ['webgl_geometries', () => import('./examples/webgl_geometries')],
    ['webgl_sprites', () => import('./examples/webgl_sprites')],
    ['webgl_raycaster_sprite', () => import('./examples/webgl_raycaster_sprite')],
    ['webgl_math_obb', () => import('./examples/webgl_math_obb')],
    ['webgl_geometry_shapes', () => import('./examples/webgl_geometry_shapes')],
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
                example.render((now - startedAt) / 1000);
                frame = requestAnimationFrame(draw);
            } catch (error) {
                stop();
                showError(error);
            }
        };
        frame = requestAnimationFrame(draw);
    }).catch(showError);
}
