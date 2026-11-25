const examples = new Map([
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
    ['webgl_buffergeometry_attributes_integer', () => import('./examples/webgl_buffergeometry_attributes_integer')],
    ['webgl_shader', () => import('./examples/webgl_shader')],
    ['webgl_points_waves', () => import('./examples/webgl_points_waves')],
    ['webgl_points_billboards', () => import('./examples/webgl_points_billboards')],
    ['webgl_points_sprites', () => import('./examples/webgl_points_sprites')],
    ['webgl_buffergeometry_indexed', () => import('./examples/webgl_buffergeometry_indexed')],
    ['webgl_panorama_equirectangular', () => import('./examples/webgl_panorama_equirectangular')],
    ['webgl_materials_texture_anisotropy', () => import('./examples/webgl_materials_texture_anisotropy')],
    ['webgl_materials_texture_filters', () => import('./examples/webgl_materials_texture_filters')],
    ['webgl_materials_texture_manualmipmap', () => import('./examples/webgl_materials_texture_manualmipmap')],
    ['webgl_materials_blending', () => import('./examples/webgl_materials_blending')],
    ['webgl_loader_obj', () => import('./examples/webgl_loader_obj')],
    ['webgl_loader_xyz', () => import('./examples/webgl_loader_xyz')],
    ['webgl_panorama_cube', () => import('./examples/webgl_panorama_cube')],
    ['webgl_loader_imagebitmap', () => import('./examples/webgl_loader_imagebitmap')],
    ['webgl_video_panorama_equirectangular', () => import('./examples/webgl_video_panorama_equirectangular')],
    ['webgl_materials_video_webcam', () => import('./examples/webgl_materials_video_webcam')],
    ['webgl_lines_colors', () => import('./examples/webgl_lines_colors')],
    ['webgl_materials_blending_custom', () => import('./examples/webgl_materials_blending_custom')],
    ['webgl_geometry_colors', () => import('./examples/webgl_geometry_colors')],
    ['webgl_multiple_elements_text', () => import('./examples/webgl_multiple_elements_text')],
]);

const exampleId = new URLSearchParams(window.location.search).get('example');

if (exampleId !== null) {
    const load = examples.get(exampleId);
    const canvas = document.querySelector<HTMLCanvasElement>('#example-canvas');
    const errorMessage = document.querySelector<HTMLParagraphElement>('#example-error');
    const gallery = document.querySelector<HTMLElement>('#gallery');
    const view = document.querySelector<HTMLElement>('#example-view');

    if (load && canvas && errorMessage && gallery && view) {
        const showError = (error: unknown) => {
            errorMessage.textContent = `No se pudo iniciar el ejemplo. ${String(error)}`;
            errorMessage.hidden = false;
        };

        gallery.hidden = true;
        view.hidden = false;
        document.title = `${exampleId} · Threekt`;

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
}
