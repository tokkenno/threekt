import { STLExporter } from 'threekt';
import { createCubeExporterExample } from './createCubeExporterExample';

export async function createExample(canvas: HTMLCanvasElement) {
    const exporter = new STLExporter();
    return createCubeExporterExample(canvas, false, [
        { label: 'Export STL ASCII', file: 'box.stl', serialize: mesh => exporter.parse(mesh) },
        { label: 'Export STL binary', file: 'box.stl', serialize: mesh => exporter.parseBinary(mesh) },
    ]);
}
