import { PLYExporter } from 'threekt';
import { createCubeExporterExample } from './createCubeExporterExample';

export async function createExample(canvas: HTMLCanvasElement) {
    const exporter = new PLYExporter();
    return createCubeExporterExample(canvas, true, [
        { label: 'Exportar PLY ASCII', file: 'box.ply', serialize: mesh => exporter.parse(mesh) },
        { label: 'Exportar PLY binario BE', file: 'box.ply', serialize: mesh => exporter.parseBinary(mesh, false) },
        { label: 'Exportar PLY binario LE', file: 'box.ply', serialize: mesh => exporter.parseBinary(mesh, true) },
    ]);
}
