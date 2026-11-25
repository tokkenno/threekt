import { PLYExporter } from 'threekt';
import { createCubeExporterExample } from './createCubeExporterExample';

export async function createExample(canvas: HTMLCanvasElement) {
    const exporter = new PLYExporter();
    return createCubeExporterExample(canvas, true, [
        { label: 'Export PLY ASCII', file: 'box.ply', serialize: mesh => exporter.parse(mesh) },
        { label: 'Export PLY binary BE', file: 'box.ply', serialize: mesh => exporter.parseBinary(mesh, false) },
        { label: 'Export PLY binary LE', file: 'box.ply', serialize: mesh => exporter.parseBinary(mesh, true) },
    ]);
}
