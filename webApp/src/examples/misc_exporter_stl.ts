import { STLExporter } from 'threekt';
import { createCubeExporterExample } from './createCubeExporterExample';

export async function createExample(canvas: HTMLCanvasElement) {
    const exporter = new STLExporter();
    return createCubeExporterExample(canvas, false, [
        { label: 'Exportar STL ASCII', file: 'box.stl', serialize: mesh => exporter.parse(mesh) },
        { label: 'Exportar STL binario', file: 'box.stl', serialize: mesh => exporter.parseBinary(mesh) },
    ]);
}
